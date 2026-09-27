package io.github.nexalloy.revanced.telegram.privacy

import android.view.View
import android.view.ViewGroup
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findFieldRecursive
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.hookTelegramRequests
import io.github.nexalloy.revanced.telegram.runtime.messageDialogId
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField
import io.github.nexalloy.revanced.telegram.runtime.requestDialogId
import io.github.nexalloy.revanced.telegram.runtime.telegramPrefs
import io.github.nexalloy.revanced.telegram.runtime.writeTelegramField
import java.util.Locale

private const val PREF_FORCE_STEALTH = "force_stealth_dialogs"
private const val PREF_ALLOW_ALL = "ghost_allow_all"
private const val PREF_ALLOW_READ = "ghost_allow_read"
private const val PREF_ALLOW_ACTIVITY = "ghost_allow_activity"
private const val PREF_ALLOW_STORIES = "ghost_allow_stories"
private const val PREF_ALLOW_CONTENT = "ghost_allow_content"

private val contentReadException = ThreadLocal.withInitial { false }

private fun Any.classNameEndsWith(vararg names: String): Boolean {
    val name = javaClass.name
    return names.any(name::endsWith)
}

private fun Any.actionName(): String =
    readTelegramField("action")?.javaClass?.simpleName?.lowercase(Locale.US).orEmpty()

private fun ClassLoader.containsDialog(prefKey: String, dialogId: Long): Boolean =
    telegramPrefs()
        ?.getStringSet(prefKey, emptySet())
        ?.contains(dialogId.toString()) == true

private fun ClassLoader.toggleDialog(prefKey: String, dialogId: Long): Boolean {
    val prefs = telegramPrefs() ?: return false
    val values = prefs.getStringSet(prefKey, emptySet()).orEmpty().toMutableSet()
    val key = dialogId.toString()
    val enabled = if (values.remove(key)) {
        false
    } else {
        values.add(key)
        true
    }
    prefs.edit().putStringSet(prefKey, values).apply()
    return enabled
}

private fun ClassLoader.isException(request: Any, category: String): Boolean {
    val dialogId = requestDialogId(request) ?: return false
    if (containsDialog(PREF_ALLOW_ALL, dialogId)) return true
    return containsDialog(category, dialogId)
}

private fun ClassLoader.blockRequests(
    exceptionCategory: String?,
    matcher: (Any) -> Boolean,
) {
    hookTelegramRequests { request, param ->
        if (!matcher(request)) return@hookTelegramRequests
        if (exceptionCategory != null && isException(request, exceptionCategory)) {
            return@hookTelegramRequests
        }
        param.result = 0
    }
}

private fun ClassLoader.blockActivity(
    matcher: (String) -> Boolean,
) {
    blockRequests(PREF_ALLOW_ACTIVITY) { request ->
        request.classNameEndsWith("\$TL_messages_setTyping") &&
            matcher(request.actionName())
    }
}

val PerChatStealthAndExceptions = patch(
    name = "Per-chat stealth and Ghost exceptions",
    description = "Adds profile controls for forcing stealth in one chat or exempting that chat from global read, activity, story, and content-read privacy controls.",
) {
    val profile = classLoader.findTelegramClassOrNull("org.telegram.ui.ProfileActivity2")
        ?: return@patch
    val menuClass = classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenu")
        ?: return@patch
    val menuItemClass = classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenuItem")
        ?: return@patch
    val subItemClass = classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenuSubItem")
        ?: return@patch

    val menuItemId = 0x4E580701
    val rows = listOf(
        Triple(0x4E580711, PREF_FORCE_STEALTH, "Stealth this chat"),
        Triple(0x4E580712, PREF_ALLOW_ALL, "Ghost exception: allow all"),
        Triple(0x4E580713, PREF_ALLOW_READ, "Ghost exception: read receipts"),
        Triple(0x4E580714, PREF_ALLOW_ACTIVITY, "Ghost exception: chat activity"),
        Triple(0x4E580715, PREF_ALLOW_STORIES, "Ghost exception: story views"),
        Triple(0x4E580716, PREF_ALLOW_CONTENT, "Ghost exception: listened/content read"),
    )

    profile.declaredMethods
        .filter { it.name == "createView" && it.parameterTypes.size == 1 }
        .forEach { method ->
            method.hookMethod {
                after { param ->
                    val profileObject = param.thisObject
                    val root = param.result as? View ?: return@after
                    val dialogId = runCatching {
                        (profileObject.javaClass.getMethod("getDialogId").invoke(profileObject) as Number).toLong()
                    }.getOrNull() ?: return@after
                    if (dialogId == 0L) return@after

                    val actionBar = profileObject.findFieldRecursive("actionBar")
                        ?.get(profileObject) ?: return@after
                    val menu = actionBar.javaClass.getMethod("createMenu").invoke(actionBar)

                    if (menu is ViewGroup) {
                        for (i in 0 until menu.childCount) {
                            if (menu.getChildAt(i).tag == menuItemId) return@after
                        }
                    }

                    val icon = runCatching {
                        classLoader.loadClass("org.telegram.messenger.R\$drawable")
                            .getField("msg_stories_stealth")
                            .getInt(null)
                    }.getOrDefault(0)

                    val item = menuClass
                        .getMethod("addItem", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        .invoke(menu, menuItemId, icon)

                    rows.forEach { (rowId, prefKey, label) ->
                        val subItem = menuItemClass.getMethod(
                            "addSubItem",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            CharSequence::class.java,
                            Boolean::class.javaPrimitiveType,
                        ).invoke(item, rowId, 0, label, true) as View

                        fun updateChecked() {
                            subItemClass.getMethod(
                                "setChecked",
                                Boolean::class.javaPrimitiveType,
                            ).invoke(subItem, classLoader.containsDialog(prefKey, dialogId))
                        }

                        updateChecked()
                        subItem.setOnClickListener {
                            classLoader.toggleDialog(prefKey, dialogId)
                            updateChecked()
                        }
                    }
                }
            }
        }

    // Per-chat forced stealth remains useful even when the global privacy patches are off.
    classLoader.hookTelegramRequests { request, param ->
        val dialogId = requestDialogId(request) ?: return@hookTelegramRequests
        if (!classLoader.containsDialog(PREF_FORCE_STEALTH, dialogId)) return@hookTelegramRequests

        val shouldBlock =
            request.classNameEndsWith(
                "\$TL_messages_readHistory",
                "\$TL_channels_readHistory",
                "\$TL_messages_readEncryptedHistory",
                "\$TL_stories_readStories",
                "\$TL_stories_incrementStoryViews",
                "\$TL_messages_sendScreenshotNotification",
                "\$TL_messages_setTyping",
            )
        if (shouldBlock) param.result = 0
    }

    // Content-read requests do not always carry their peer. Block them one level earlier.
    classLoader.findTelegramClassOrNull("org.telegram.messenger.MessagesController")
        ?.declaredMethods
        ?.filter {
            it.name == "markMessageContentAsRead" &&
                it.parameterTypes.isNotEmpty()
        }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    val dialogId = messageDialogId(param.args.firstOrNull()) ?: return@before
                    if (classLoader.containsDialog(PREF_FORCE_STEALTH, dialogId)) {
                        param.result = null
                    } else if (
                        classLoader.containsDialog(PREF_ALLOW_ALL, dialogId) ||
                        classLoader.containsDialog(PREF_ALLOW_CONTENT, dialogId)
                    ) {
                        contentReadException.set(true)
                    }
                }
                after {
                    contentReadException.set(false)
                }
            }
        }
}

val HideReadReceipts = patch(
    name = "Hide message read receipts",
    description = "Prevents Telegram from sending normal, channel, and secret-chat read-history acknowledgements. Per-chat Ghost exceptions are respected.",
    use = false,
) {
    classLoader.blockRequests(PREF_ALLOW_READ) { request ->
        request.classNameEndsWith(
            "\$TL_messages_readHistory",
            "\$TL_channels_readHistory",
            "\$TL_messages_readEncryptedHistory",
        )
    }
}

val HideContentReadReceipts = patch(
    name = "Hide listened / content-read receipts",
    description = "Prevents content-read acknowledgements used by voice notes and other content-specific read state. Per-chat Ghost exceptions are respected where Telegram exposes the active message.",
    use = false,
) {
    classLoader.hookTelegramRequests { request, param ->
        if (!request.classNameEndsWith(
                "\$TL_messages_readMessageContents",
                "\$TL_channels_readMessageContents",
            )
        ) return@hookTelegramRequests
        if (contentReadException.get() == true) return@hookTelegramRequests
        param.result = 0
    }
}

val HideTypingStatus = patch(
    name = "Hide typing status",
    description = "Prevents the typing indicator from being sent to chats.",
    use = false,
) {
    classLoader.blockActivity { it.contains("typing") }
}

val HideVoiceRecordingStatus = patch(
    name = "Hide voice recording status",
    description = "Hides the recording-voice activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("recordaudio") || it.contains("recordvoice") }
}

val HideVoiceUploadStatus = patch(
    name = "Hide voice upload status",
    description = "Hides the uploading-voice activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("uploadaudio") || it.contains("uploadvoice") }
}

val HideVideoRecordingStatus = patch(
    name = "Hide video recording status",
    description = "Hides the standard video recording activity indicator.",
    use = false,
) {
    classLoader.blockActivity {
        (it.contains("recordvideo") && !it.contains("round")) ||
            it.contains("recordingvideo")
    }
}

val HideVideoUploadStatus = patch(
    name = "Hide video upload status",
    description = "Hides the standard video upload activity indicator.",
    use = false,
) {
    classLoader.blockActivity {
        (it.contains("uploadvideo") && !it.contains("round")) ||
            it.contains("uploadingvideo")
    }
}

val HidePhotoUploadStatus = patch(
    name = "Hide photo upload status",
    description = "Hides the photo upload activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("uploadphoto") }
}

val HideFileUploadStatus = patch(
    name = "Hide file upload status",
    description = "Hides file/document upload activity indicators.",
    use = false,
) {
    classLoader.blockActivity { it.contains("uploaddocument") || it.contains("uploadfile") }
}

val HideRoundVideoStatus = patch(
    name = "Hide round-video activity",
    description = "Hides recording and uploading indicators for round video messages.",
    use = false,
) {
    classLoader.blockActivity { it.contains("round") && (it.contains("record") || it.contains("upload")) }
}

val HideLocationSelectionStatus = patch(
    name = "Hide location selection status",
    description = "Hides the choosing-location activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("geolocation") || it.contains("chooselocation") }
}

val HideContactSelectionStatus = patch(
    name = "Hide contact selection status",
    description = "Hides the choosing-contact activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("choosecontact") }
}

val HideStickerSelectionStatus = patch(
    name = "Hide sticker selection status",
    description = "Hides the choosing-sticker activity indicator.",
    use = false,
) {
    classLoader.blockActivity { it.contains("choosesticker") || it.contains("choosingsticker") }
}

val HideGameStatus = patch(
    name = "Hide game activity",
    description = "Hides Telegram game-play activity indicators.",
    use = false,
) {
    classLoader.blockActivity { it.contains("gameplay") || it.contains("playinggame") }
}

val HideRecordingAndUploadStatus = patch(
    name = "Hide all recording and upload status",
    description = "Hides all voice/video/media/file recording and upload activity indicators.",
    use = false,
) {
    classLoader.blockActivity { it.contains("record") || it.contains("upload") }
}

val HideEmojiInteractions = patch(
    name = "Hide emoji interaction status",
    description = "Prevents Telegram from broadcasting animated-emoji interaction activity.",
    use = false,
) {
    classLoader.blockActivity { it.contains("emojiinteraction") && !it.contains("seen") }
}

val HideEmojiAcknowledgement = patch(
    name = "Hide emoji acknowledgement status",
    description = "Hides acknowledgements sent when animated emoji interactions are seen.",
    use = false,
) {
    classLoader.blockActivity {
        (it.contains("emoji") && it.contains("seen")) ||
            it.contains("emojiack")
    }
}

val HideGroupCallSpeakingStatus = patch(
    name = "Hide speaking status in group calls",
    description = "Prevents the speaking activity indicator from being sent through Telegram's activity channel.",
    use = false,
) {
    classLoader.blockActivity { it.contains("speakingingroupcall") }
}

val HideStoryReadReceipts = patch(
    name = "Hide story read receipts",
    description = "Blocks stories.readStories while leaving the separate story-view increment control independent.",
    use = false,
) {
    classLoader.blockRequests(PREF_ALLOW_STORIES) {
        it.classNameEndsWith("\$TL_stories_readStories")
    }
}

val HideStoryViewIncrements = patch(
    name = "Hide story view increments",
    description = "Blocks stories.incrementStoryViews independently of story read receipts.",
    use = false,
) {
    classLoader.blockRequests(PREF_ALLOW_STORIES) {
        it.classNameEndsWith("\$TL_stories_incrementStoryViews")
    }
}

val HideStoryViews = patch(
    name = "Hide all story view receipts",
    description = "Blocks both story-read and story-view increment requests.",
    use = false,
) {
    classLoader.blockRequests(PREF_ALLOW_STORIES) { request ->
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
    classLoader.blockRequests(null) {
        it.classNameEndsWith("\$TL_messages_sendScreenshotNotification")
    }
}

val HideAllChatActivity = patch(
    name = "Hide all chat activity (master)",
    description = "Blocks every Telegram setTyping/activity request, including typing, recording, uploads, selection actions, emoji interactions, and group-call speaking indicators.",
    use = false,
) {
    classLoader.blockRequests(PREF_ALLOW_ACTIVITY) {
        it.classNameEndsWith("\$TL_messages_setTyping")
    }
}

val HideOnlineStatus = patch(
    name = "Hide online status",
    description = "Keeps this Telegram session from announcing online presence. Other logged-in sessions can still expose online state.",
    use = false,
) {
    classLoader.hookTelegramRequests { request, _ ->
        if (!request.javaClass.name.endsWith("\$updateStatus")) {
            return@hookTelegramRequests
        }
        if (!request.javaClass.name.contains("TL_account")) {
            return@hookTelegramRequests
        }
        request.writeTelegramField("offline", true)
    }
}
