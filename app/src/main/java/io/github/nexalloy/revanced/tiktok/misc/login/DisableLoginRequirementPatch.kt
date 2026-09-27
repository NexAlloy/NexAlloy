package io.github.nexalloy.revanced.tiktok.misc.login

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val DisableLoginRequirement = patch(
    name = "Disable forced login",
    description = "Prevents TikTok from forcing the login screen when the matching service methods are present.",
) {
    ::enableForcedLoginFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    ::shouldShowForcedLoginFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
