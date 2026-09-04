package io.github.nexalloy.morphe.music.ad

import android.view.View
import app.morphe.extension.music.patches.HideAdsPatch
import app.morphe.extension.shared.ResourceUtils
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.ad.HideFullscreenAds
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide fullscreen ads, Premium promotions and video ads."
) {
    dependsOn(
        HideFullscreenAds(PreferenceScreen.ADS),
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_music_hide_get_premium_label"),
        SwitchPreference("morphe_music_hide_music_premium_promotions"),
        SwitchPreference("morphe_music_hide_video_ads"),
    )

    HideGetPremiumFingerprint.hookMethod(
        scopedHook(::hideGetPremiumSetVisibility.member) {
            before { param ->
                param.args[0] = View.GONE
            }
        }
    )

    MembershipSettingsFingerprint.hookMethod {
        before {
            if (HideAdsPatch.hideGetPremiumLabel()) it.result = null
        }
    }

    ::showVideoAds.hookMethod {
        before { param ->
            param.args[0] = HideAdsPatch.hideVideoAds(param.args[0] as Boolean)
        }
    }

    val floatingLayoutId = ResourceUtils.getIdIdentifier("floating_layout")
    FloatingLayoutFingerprint.hookMethod(
        scopedHook(::floatingLayoutFindViewById.member) {
            after { param ->
                if (param.args[0] != floatingLayoutId) return@after
                (param.result as? View)?.let(HideAdsPatch::hidePremiumPromotionBottomSheet)
            }
        }
    )
}
