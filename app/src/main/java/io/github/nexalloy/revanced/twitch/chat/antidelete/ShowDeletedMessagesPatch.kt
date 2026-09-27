package io.github.nexalloy.revanced.twitch.chat.antidelete

import io.github.nexalloy.patch
import io.github.nexalloy.setBooleanField

val ShowDeletedMessages = patch(
    name = "Show deleted messages",
    description = "Keeps deleted Twitch chat messages accessible through the app's clickable spoiler behavior.",
) {
    ::deletedMessageClickableSpanCtorFingerprint.hookMethod {
        after { param ->
            val span = param.thisObject ?: return@after
            runCatching { span.setBooleanField("hasModAccess", true) }
        }
    }

    ::setHasModAccessFingerprint.hookMethod {
        before { param ->
            if (param.args.isNotEmpty()) param.args[0] = true
        }
    }
}
