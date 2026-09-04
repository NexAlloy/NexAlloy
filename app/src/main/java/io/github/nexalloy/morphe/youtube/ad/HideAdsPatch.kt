package io.github.nexalloy.morphe.youtube.ad

import android.view.View
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.youtube.patches.components.AdsFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.morphe.shared.ad.HideFullscreenAds
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.layout.hide.general.HideHorizontalShelves
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.BROWSE
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.GET_WATCH
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.GUIDE
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.NEXT
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.PLAYER
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.REEL
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.SEARCH
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextHook
import io.github.nexalloy.morphe.youtube.misc.contexthook.addOSNameHook
import io.github.nexalloy.morphe.youtube.misc.engagement.EngagementPanelHook
import io.github.nexalloy.morphe.youtube.misc.engagement.addEngagementPanelIdHook
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

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
        ClientContextHook,
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_hide_creator_store_shelf"),
        SwitchPreference("morphe_hide_end_screen_store_banner"),
        SwitchPreference("morphe_hide_general_ads"),
        SwitchPreference("morphe_hide_merchandise_banners"),
        SwitchPreference("morphe_hide_paid_promotion_labels"),
        SwitchPreference("morphe_hide_player_popup_ads"),
        SwitchPreference("morphe_hide_self_sponsor_ads"),
        SwitchPreference("morphe_hide_shopping_links"),
        SwitchPreference("morphe_hide_video_ads"),
        SwitchPreference("morphe_hide_youtube_premium_promotions"),
    )

    addLithoFilter(AdsFilter())
    addEngagementPanelIdHook(AdsFilter::hidePlayerPopupAds)

    listOf(BROWSE, SEARCH, NEXT).forEach { endpoint ->
        addOSNameHook(endpoint, AdsFilter::hideAds)
    }
    listOf(GET_WATCH, PLAYER, REEL).forEach { endpoint ->
        addOSNameHook(endpoint, AdsFilter::hideVideoAds)
    }
    addOSNameHook(GUIDE, AdsFilter::overrideGuideOSName)

    setOf(
        LoadVideoAdsFingerprint,
        PlayerBytesAdLayoutFingerprint,
    ).forEach { fingerprint ->
        fingerprint.hookMethod {
            before {
                if (AdsFilter.hideVideoAds()) it.result = null
            }
        }
    }

    FullScreenEngagementAdContainerFingerprint.hookMethod(
        scopedHook(::fullScreenEngagementAdAddMethod.member) {
            before { param ->
                if (innerDepth != 0) return@before
                @Suppress("UNCHECKED_CAST")
                val elements = param.thisObject as MutableList<Any?>
                AdsFilter.hideEndScreenStoreBanner(elements, param.args[0])
                param.result = true
            }
        }
    )

    GetPremiumViewFingerprint.hookMethod {
        after {
            if (AdsFilter.hideGetPremiumView()) {
                val view = it.thisObject as View
                XposedHelpers.callMethod(view, "setMeasuredDimension", 0, 0)
            }
        }
    }

    PlayerOverlayTimelyShelfFingerprint.hookMethod {
        val playerOverlayEventClass = ::PlayerOverlayEventType.clazz
        val playerOverlayIdField = ::PlayerOverlayIdField.field
        before {
            val obj = it.args[0]
            if (playerOverlayEventClass.isInstance(obj)) {
                val id = playerOverlayIdField.get(obj) as String
                if (!AdsFilter.allowAds(id == "player_overlay_timely_shelf")) it.result = null
            }
        }
    }

    val adAttributionId = ResourceUtils.getIdIdentifier("ad_attribution")
    XposedHelpers.findAndHookMethod(
        View::class.java.name,
        lpparam.classLoader,
        "findViewById",
        Int::class.java.name,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.args[0] == adAttributionId) {
                    val view = param.result as? View ?: return
                    Logger.printDebug { "Hide Ad Attribution View" }
                    AdsFilter.hideAdAttributionView(view)
                }
            }
        })

    val miniplayerSubtitleId = ResourceUtils.getIdIdentifier("modern_miniplayer_subtitle_text")
    MiniplayerPaidPromotionLabelFingerprint.hookMethod(
        scopedHook(::miniplayerPaidPromotionViewMethod.member) {
            after { param ->
                if (param.args[0] != miniplayerSubtitleId) return@after
                (param.result as? View)?.let(AdsFilter::hideMiniplayerPaidPromotionLabelView)
            }
        }
    )
}
