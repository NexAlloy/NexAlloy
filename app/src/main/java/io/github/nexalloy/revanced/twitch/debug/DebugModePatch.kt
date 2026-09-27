package io.github.nexalloy.revanced.twitch.debug

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val EnableDebugMode = patch(
    name = "Enable debug mode",
    description = "Enables Twitch's internal debug configuration and debug options.",
    use = false,
) {
    ::isDebugConfigEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    ::isOmVerificationEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    ::shouldShowDebugOptionsFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}
