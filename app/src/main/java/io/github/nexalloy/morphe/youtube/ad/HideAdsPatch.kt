package io.github.nexalloy.morphe.youtube.ad

import android.view.View
import app.morphe.extension.music.patches.HideAdsPatch
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.youtube.patches.components.AdsFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.morphe.shared.ad.HideFullscreenAds
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.layout.hide.general.HideHorizontalShelves
import io.github.nexalloy.morphe.youtube.misc.engagement.EngagementPanelHook
import io.github.nexalloy.morphe.youtube.misc.engagement.addEngagementPanelIdHook
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide general ads, Premium promotions and video ads.",
) {
    dependsOn(
        LithoFilter,
        EngagementPanelHook,
        HideHorizontalShelves,

        HideFullscreenAds(PreferenceScreen.ADS),
        VersionCheck,
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_hide_creator_store_shelf"),
//        SwitchPreference("morphe_hide_end_screen_store_banner"),
        SwitchPreference("morphe_hide_general_ads"),
        SwitchPreference("morphe_hide_merchandise_banners"),
        SwitchPreference("morphe_hide_paid_promotion_label"),
        SwitchPreference("morphe_hide_player_popup_ads"),
        SwitchPreference("morphe_hide_self_sponsor_ads"),
        SwitchPreference("morphe_hide_shopping_links"),
        SwitchPreference("morphe_hide_video_ads"),
        SwitchPreference("morphe_hide_youtube_premium_promotions"),
    )

    addLithoFilter(AdsFilter())
    addEngagementPanelIdHook(AdsFilter::hidePlayerPopupAds)

    // Hide video ads

    setOf(
        LoadVideoAdsFingerprint,
        PlayerBytesAdLayoutFingerprint,
    ).forEach { fingerprint ->
        fingerprint.hookMethod {
            before {
                if(AdsFilter.hideVideoAds())
                    it.result = null
            }
        }
    }

    // TODO: Hide YouTube Premium promotions

    // TODO: Hide end screen store banner

    // Hide get premium
    GetPremiumViewFingerprint.hookMethod {
        after {
            if (AdsFilter.hideGetPremiumView()) {
                val view = it.thisObject as View
                XposedHelpers.callMethod(view, "setMeasuredDimension", 0, 0)
            }
        }
    }

    // Hide player overlay view. This can be hidden with a regular litho filter
    // but an empty space remains.

    PlayerOverlayTimelyShelfFingerprint.hookMethod {
        val playerOverlayEventClass = ::PlayerOverlayEventType.clazz
        val playerOverlayIdField = ::PlayerOverlayIdField.field
        before {
            val obj = it.args[0]
            if (playerOverlayEventClass.isInstance(obj)) {
                val id = playerOverlayIdField.get(obj) as String

                if (!AdsFilter.allowAds(id == "player_overlay_timely_shelf"))
                    it.result = null
            }
        }
    }

    // Hide ad views
    val adAttributionId = ResourceUtils.getIdIdentifier("ad_attribution")

    XposedHelpers.findAndHookMethod(
        View::class.java.name,
        lpparam.classLoader,
        "findViewById",
        Int::class.java.name,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.args[0] == adAttributionId) {
                    Logger.printDebug { "Hide Ad Attribution View" }
                    AdsFilter.hideAdAttributionView(param.result as View)
                }
            }
        })

    // TODO Hide paid promotion label in miniplayer

    /**
     * TODO [AdsFilter.hideAds] OsNameHook
     */
    /**
     * TODO [AdsFilter.hideVideoAds] OsNameHook
     */
}