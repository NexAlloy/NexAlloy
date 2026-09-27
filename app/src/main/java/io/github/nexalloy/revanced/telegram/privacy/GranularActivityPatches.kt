package io.github.nexalloy.revanced.telegram.privacy

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun ClassLoader.findClassOrNull2(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun Any.actionNameOrEmpty(): String =
    runCatching { javaClass.getField("action").get(this)?.javaClass?.name.orEmpty() }
        .recoverCatching {
            javaClass.getDeclaredField("action").apply { isAccessible = true }
                .get(this)?.javaClass?.name.orEmpty()
        }
        .getOrDefault("")

private fun Any.peerUserIdOrNull2(): Long? {
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

private fun ClassLoader.isGhostException2(request: Any): Boolean {
    val userId = request.peerUserIdOrNull2() ?: return false
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

private fun ClassLoader.blockSetTypingAction(match: (String) -> Boolean) {
    val connections =
        findClassOrNull2("org.telegram.tgnet.ConnectionsManager") ?: return

    connections.declaredMethods
        .filter {
            it.name == "sendRequest" &&
                it.parameterTypes.isNotEmpty() &&
                it.returnType == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val request = param.args.firstOrNull() ?: return@before
                    if (!request.javaClass.name.endsWith("\$TL_messages_setTyping")) {
                        return@before
                    }
                    if (!isGhostException2(request) && match(request.actionNameOrEmpty())) {
                        param.result = 0
                    }
                }
            }
        }
}

val HideVoiceRecordingStatus = patch(
    name = "Hide voice recording status",
    description = "Hides the recording-voice activity indicator.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("RecordAudio") }
}

val HideVideoRecordingStatus = patch(
    name = "Hide video recording status",
    description = "Hides video and round-video recording activity indicators.",
    use = false,
) {
    classLoader.blockSetTypingAction {
        it.contains("RecordVideo") || it.contains("RecordRound")
    }
}

val HideVoiceUploadStatus = patch(
    name = "Hide voice upload status",
    description = "Hides voice/audio upload activity.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("UploadAudio") }
}

val HideVideoUploadStatus = patch(
    name = "Hide video upload status",
    description = "Hides video and round-video upload activity.",
    use = false,
) {
    classLoader.blockSetTypingAction {
        it.contains("UploadVideo") || it.contains("UploadRound")
    }
}

val HidePhotoUploadStatus = patch(
    name = "Hide photo upload status",
    description = "Hides photo upload activity.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("UploadPhoto") }
}

val HideFileUploadStatus = patch(
    name = "Hide file upload status",
    description = "Hides generic file upload activity.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("UploadDocument") || it.contains("UploadFile") }
}

val HideChoosingLocationStatus = patch(
    name = "Hide choosing location status",
    description = "Hides Telegram's choosing-location activity indicator.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("GeoLocation") || it.contains("Location") }
}

val HideChoosingContactStatus = patch(
    name = "Hide choosing contact status",
    description = "Hides Telegram's choosing-contact activity indicator.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("ChooseContact") }
}

val HideChoosingStickerStatus = patch(
    name = "Hide choosing sticker status",
    description = "Hides Telegram's choosing-sticker activity indicator.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("ChooseSticker") }
}

val HidePlayingGameStatus = patch(
    name = "Hide playing game status",
    description = "Hides Telegram's playing-game activity indicator.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("GamePlay") || it.contains("PlayingGame") }
}

val HideEmojiAcknowledgementStatus = patch(
    name = "Hide emoji acknowledgement status",
    description = "Hides emoji acknowledgement activity in supported Telegram versions.",
    use = false,
) {
    classLoader.blockSetTypingAction { it.contains("EmojiAcknowledgement") }
}
