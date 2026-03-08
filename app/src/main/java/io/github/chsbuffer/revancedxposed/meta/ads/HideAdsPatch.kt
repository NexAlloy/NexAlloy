package io.github.chsbuffer.revancedxposed.meta.ads

import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XC_MethodReplacement
import io.github.chsbuffer.revancedxposed.patch

val HideAds = patch(
    name = "Hide ads",
) {
    // Block ads ở feed/trang chủ
    ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)

    // Block ads ở story/reels
    ::adSponsoredContentFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
