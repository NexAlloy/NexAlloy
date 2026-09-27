package io.github.nexalloy.revanced.telegram.privacy

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun Any.peerUserIdOrNull(): Long? {
    val peer = runCatching { javaClass.getField("peer").get(this) }
        .recoverCatching {
            javaClass.getDeclaredField("peer").apply { isAccessible = true }.get(this)
        }
        .getOrNull() ?: return null

    return runCatching { peer.javaClass.getField("user_id").getLong(peer) }
        .recoverCatching {
            peer.javaClass.getDeclaredField("user_id").apply { isAccessible = true }.getLong(peer)
        }
        .getOrNull()
        ?.takeIf { it != 0L }
}

private fun ClassLoader.isGhostException(request: Any): Boolean {
    val userId = request.peerUserIdOrNull() ?: return false
    val context = runCatching {
        val app = loadClass("org.telegram.messenger.ApplicationLoader")
        val field = runCatching { app.getField("applicationContext") }
            .getOrElse { app.getDeclaredField("applicationContext").apply { isAccessible = true } }
        field.get(null) as android.content.Context
    }.getOrNull() ?: return false

    return context
        .getSharedPreferences("nexalloy.telegram.ghost", android.content.Context.MODE_PRIVATE)
        .getStringSet("exceptions", emptySet())
        ?.contains(userId.toString()) == true
}

private fun ClassLoader.blockOutgoingRequests(
    predicate: (Any) -> Boolean,
) {
    val connectionsManager =
        findClassOrNull("org.telegram.tgnet.ConnectionsManager") ?: return

    connectionsManager.declaredMethods
        .filter {
            it.name == "sendRequest" &&
                it.parameterTypes.isNotEmpty() &&
                it.returnType == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val request = param.args.firstOrNull() ?: return@before
                    if (!isGhostException(request) && predicate(request)) {
                        // Returning 0 matches Telegram's no-request / cancelled token convention.
                        param.result = 0
                    }
                }
            }
        }
}

private fun Any.classNameEndsWith(vararg names: String): Boolean {
    val name = javaClass.name
    return names.any(name::endsWith)
}

private fun Any.fieldOrNull(name: String): Any? =
    runCatching { javaClass.getField(name).get(this) }
        .recoverCatching {
            javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)
        }
        .getOrNull()

val HideReadReceipts = patch(
    name = "Hide message read receipts",
    description = "Prevents Telegram from sending normal, channel, and secret-chat read-history acknowledgements.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        request.classNameEndsWith(
            "\$TL_messages_readHistory",
            "\$TL_channels_readHistory",
            "\$TL_messages_readEncryptedHistory",
        )
    }
}

val HideContentReadReceipts = patch(
    name = "Hide listened / content-read receipts",
    description = "Prevents read-content acknowledgements used for voice notes and other content-specific read state.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        request.classNameEndsWith(
            "\$TL_messages_readMessageContents",
            "\$TL_channels_readMessageContents",
        )
    }
}

val HideTypingStatus = patch(
    name = "Hide typing status",
    description = "Prevents the typing indicator from being sent to chats.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        if (!request.classNameEndsWith("\$TL_messages_setTyping")) {
            return@blockOutgoingRequests false
        }
        val actionName = request.fieldOrNull("action")?.javaClass?.name.orEmpty()
        actionName.contains("TypingAction")
    }
}

val HideRecordingAndUploadStatus = patch(
    name = "Hide recording and upload status",
    description = "Hides voice/video recording and media/file upload activity indicators.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        if (!request.classNameEndsWith("\$TL_messages_setTyping")) {
            return@blockOutgoingRequests false
        }
        val actionName = request.fieldOrNull("action")?.javaClass?.name.orEmpty()
        actionName.contains("Record") || actionName.contains("Upload")
    }
}

val HideEmojiInteractions = patch(
    name = "Hide emoji interaction status",
    description = "Prevents Telegram from broadcasting animated-emoji interaction activity.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        if (!request.classNameEndsWith("\$TL_messages_setTyping")) {
            return@blockOutgoingRequests false
        }
        request.fieldOrNull("action")
            ?.javaClass
            ?.name
            ?.contains("EmojiInteraction") == true
    }
}

val HideGroupCallSpeakingStatus = patch(
    name = "Hide speaking status in group calls",
    description = "Prevents the speaking activity indicator from being sent through Telegram's typing/action channel.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        if (!request.classNameEndsWith("\$TL_messages_setTyping")) {
            return@blockOutgoingRequests false
        }
        request.fieldOrNull("action")
            ?.javaClass
            ?.name
            ?.contains("speakingInGroupCallAction") == true
    }
}

val HideStoryViews = patch(
    name = "Hide story views",
    description = "Prevents story read and story-view increment requests from being sent.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        request.classNameEndsWith(
            "\$TL_stories_readStories",
            "\$TL_stories_incrementStoryViews",
        )
    }
}

val HideScreenshotNotifications = patch(
    name = "Hide screenshot notifications",
    description = "Prevents Telegram from sending screenshot-notification requests from supported secret-chat flows.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        request.classNameEndsWith("\$TL_messages_sendScreenshotNotification")
    }
}

val HideAllChatActivity = patch(
    name = "Hide all chat activity (master)",
    description = "Blocks every Telegram setTyping/activity request, including typing, recording, uploads, sticker/contact/location selection, emoji interactions, and group-call speaking indicators.",
    use = false,
) {
    classLoader.blockOutgoingRequests { request ->
        request.classNameEndsWith("\$TL_messages_setTyping")
    }
}

val HideOnlineStatus = patch(
    name = "Hide online status",
    description = "Keeps this Telegram session from announcing online presence by converting account.updateStatus online updates into offline updates. Other logged-in sessions can still expose online state.",
    use = false,
) {
    val connectionsManager =
        classLoader.findClassOrNull("org.telegram.tgnet.ConnectionsManager")
            ?: return@patch

    connectionsManager.declaredMethods
        .filter {
            it.name == "sendRequest" &&
                it.parameterTypes.isNotEmpty() &&
                it.returnType == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val request = param.args.firstOrNull() ?: return@before
                    if (!request.javaClass.name.endsWith("\$updateStatus")) {
                        return@before
                    }
                    if (!request.javaClass.name.contains("TL_account")) {
                        return@before
                    }

                    runCatching {
                        request.javaClass.getField("offline").setBoolean(request, true)
                    }.recoverCatching {
                        request.javaClass.getDeclaredField("offline").apply {
                            isAccessible = true
                            setBoolean(request, true)
                        }
                    }
                }
            }
        }
}
