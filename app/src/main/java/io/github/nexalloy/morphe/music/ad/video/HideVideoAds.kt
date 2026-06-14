package io.github.nexalloy.morphe.music.ad.video

import app.morphe.extension.music.patches.HideVideoAdsPatch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val HideVideoAds = patch(
    name = "Hide music video ads",
    description = "Adds an option to hide ads that appear while listening to or streaming music videos, podcasts, or songs.",
) {
    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_music_hide_video_ads"),
    )

    ::showVideoAdsParentFingerprint.hookMethod(scopedHook(::showVideoAds.member) {
        before { param ->
            param.args[0] = HideVideoAdsPatch.showVideoAds(param.args[0] as Boolean)
        }
    })
}