package io.github.nexalloy.revanced.pixel

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics

val ForcePixelSystemFeature = patch(
    name = "Force Pixel system-feature gate",
    description = "Forces the app's narrow PackageManager.hasSystemFeature utility check to report the required Pixel feature.",
    use = false,
) {
    ::hasSystemFeatureCheckerFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(true))
}

val ForcePixelModelCheck = patch(
    name = "Force Pixel model gate",
    description = "Forces the app's narrow public static Pixel model utility check to report a Pixel device.",
    use = false,
) {
    ::pixelModelCheckerFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(true))
}

val StripPixelAppRootDetection = patch(
    name = "Disable Pixel app root check",
    description = "Forces the narrow public static Magisk-check utility method to return false when present.",
    use = false,
) {
    ::rootDetectionCheckerFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}

val PixelDeviceGatePatches = arrayOf(
    ForcePixelSystemFeature,
    ForcePixelModelCheck,
    StripPixelAppRootDetection,
    DisableCommonAnalytics,
)
