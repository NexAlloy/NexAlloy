package io.github.nexalloy.revanced.instagram.privacy

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableScreenshotDetection = patch(
    name = "Disable Instagram screenshot detection",
    description = "Disables known Instagram DM/story screenshot detector entry points and FLAG_SECURE consistency handling.",
    use = false,
) {
    listOf(
        ::screenshotDetectorFingerprint,
        ::addFlagsToWindowFingerprint,
        ::directScreenshotCaptureTriggerFingerprint,
    ).forEach { fingerprint ->
        fingerprint.memberOrNull?.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
}
