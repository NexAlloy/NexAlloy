package io.github.chsbuffer.revancedxposed.instagram.tracking

import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.revancedxposed.hookMethod
import io.github.chsbuffer.revancedxposed.patch

val SanitizeTrackingLinks = patch(
    name = "Sanitize tracking links",
    description = "Removes tracking parameters from Instagram links when copying.",
) {
    XposedHelpers.findMethodExact(
        "android.content.ClipboardManager",
        classLoader,
        "setPrimaryClip",
        android.content.ClipData::class.java
    ).hookMethod {
        before {
            val clipData = it.args[0] as? android.content.ClipData ?: return@before
            if (clipData.itemCount == 0) return@before
            val item = clipData.getItemAt(0) ?: return@before
            var string = item.text?.toString() ?: return@before
            if (!string.contains("https://www.instagram.com/")) return@before
            Logger.printDebug { "Sanitize tracking link: $string" }
            string = string.replace(Regex("\\?igsh=.*"), "")
            string = string.replace(Regex("\\?ig_rid=.*"), "")
            string = string.replace(Regex("\\?utm_source=.*"), "")
            string = string.replace(Regex("\\?story_media_id=.*"), "")
            string = string.replace(Regex("(?i).*saved[-_]by.*"), "")
            it.args[0] = android.content.ClipData.newPlainText("URL", string)
        }
    }
}
