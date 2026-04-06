package io.github.nexalloy.revanced.meta.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
) {
    ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}