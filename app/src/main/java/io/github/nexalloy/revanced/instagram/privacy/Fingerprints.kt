package io.github.nexalloy.revanced.instagram.privacy

import io.github.nexalloy.morphe.fingerprint

val screenshotDetectorFingerprint = fingerprint {
    returns("V")
    strings(
        "ig_android_story_screenshot_directory",
        "screenshot_detector",
    )
}

val addFlagsToWindowFingerprint = fingerprint {
    returns("V")
    strings("Inconsistency in window FLAG_SECURE state detected! window state: ")
}

val directScreenshotCaptureTriggerFingerprint = fingerprint {
    returns("V")
    strings("igd_screenshot_capture")
}
