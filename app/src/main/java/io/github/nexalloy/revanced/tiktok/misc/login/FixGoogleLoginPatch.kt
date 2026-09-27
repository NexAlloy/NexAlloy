package io.github.nexalloy.revanced.tiktok.misc.login

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val FixGoogleLogin = patch(
    name = "Fix Google login",
    description = "Uses TikTok's fallback Google authentication path when the app reports the incompatible auth flow as available.",
) {
    ::googleAuthAvailableFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    ::googleOneTapAuthAvailableFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
