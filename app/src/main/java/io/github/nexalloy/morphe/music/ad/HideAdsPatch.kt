package io.github.nexalloy.morphe.music.ad

import android.view.View
import android.view.ViewGroup
import app.morphe.extension.music.patches.HideAdsPatch
import app.morphe.extension.music.patches.components.AdsFilter
import app.morphe.extension.shared.ResourceUtils
import io.github.nexalloy.morphe.music.misc.litho.filter.lithoFilterPatch
import io.github.nexalloy.morphe.music.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.ad.HideFullscreenAds
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val hideAdsPatch = patch(
    name = "Hide ads",
    description = "Adds options to hide fullscreen ads, Premium promotions and video ads."
) {
    dependsOn(
        HideFullscreenAds(PreferenceScreen.ADS),
        lithoFilterPatch,
        elementProtoParserHookPatch,
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_music_hide_get_premium_label"),
        SwitchPreference("morphe_music_hide_music_premium_promotions"),
        SwitchPreference("morphe_music_hide_video_ads"),
    )

    addLithoFilter(AdsFilter())

    // Hide 'Get Music Premium' label
    val premiumView = ::getPremiumViewField.field
    HideGetPremiumFingerprint.hookMethod(scopedHook(::getPremiumSetVisibilityMethod.method) {
        before {
            if (it.thisObject === premiumView.get(outerParam.thisObject)) {
                it.args[0] = View.GONE
            }
        }
    })

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

    FloatingLayoutFingerprint.hookMethod {
        after { HideAdsPatch.hidePremiumPromotionBottomSheet(it.result as View) }
    }

    // Hide the ad overlay drawn over the player while an ad plays on a cast device.
    val skipAd = ResourceUtils.getIdIdentifier("skip_ad_button")
    AdOverlayInflateFingerprint.hookMethod(scopedHook(::adOverlayViewLookupMethod.method) {
        before {
            if (it.args[0] == skipAd) HideAdsPatch.hideAdOverlay(it.thisObject as ViewGroup)
        }
    })

    // Hide Music Premium promotions.
    hookElement(HideAdsPatch::hideMenuItemPremiumPromo)
}
