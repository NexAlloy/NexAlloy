package io.github.nexalloy.revanced.twitch.ad.audio

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val BlockAudioAds = patch(
    name = "Block audio ads",
    description = "Blocks client-side Twitch audio ad playback calls.",
) {
    ::audioAdsPresenterPlayFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
