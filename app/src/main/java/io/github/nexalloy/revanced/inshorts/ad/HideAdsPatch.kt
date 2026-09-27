package io.github.nexalloy.revanced.inshorts.ad

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Stops Inshorts' Google ad loader request method.",
) {
    ::inshortsAdsFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
