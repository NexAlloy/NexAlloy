package io.github.nexalloy.revanced.tiktok.interaction.antirecording

import android.app.Activity
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableScreenCaptureDetection = patch(
    name = "Disable TikTok screen capture detection",
    description = "Prevents TikTok from reacting to screenshot and screen-recording callbacks inside its process.",
    use = false,
) {
    listOf(
        ::antiRecordingAddedFingerprint,
        ::antiRecordingRemovedFingerprint,
    ).forEach { fingerprint ->
        fingerprint.memberOrNull?.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    Activity::class.java.declaredMethods
        .filter {
            it.name == "registerScreenCaptureCallback" ||
                it.name == "unregisterScreenCaptureCallback"
        }
        .forEach { method ->
            method.hookMethod(XC_MethodReplacement.DO_NOTHING)
        }
}
