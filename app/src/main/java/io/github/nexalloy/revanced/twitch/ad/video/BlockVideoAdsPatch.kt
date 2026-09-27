package io.github.nexalloy.revanced.twitch.ad.video

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val BlockVideoAds = patch(
    name = "Block video ads",
    description = "Blocks client-side Twitch video ad playback and reports that ads should not be shown.",
) {
    ::contentConfigShowAdsFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    ::adsManagerPlayAdsFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
