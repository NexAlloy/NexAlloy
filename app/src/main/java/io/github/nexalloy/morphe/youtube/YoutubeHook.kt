package io.github.nexalloy.morphe.youtube

import android.app.Activity
import app.morphe.extension.shared.Utils
import io.github.libxposed.api.XposedInterface
import io.github.nexalloy.ExtensionResourceHook
import io.github.nexalloy.addModuleAssets
import io.github.nexalloy.atLast
import io.github.nexalloy.hookExtensionClassLoading
import io.github.nexalloy.hookMethod
import io.github.nexalloy.injectHostClassLoaderToSelf
import io.github.nexalloy.morphe.shared.misc.CheckRecycleBitmapMediaSession
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalIndexedBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_42_or_greater
import io.github.nexalloy.morphe.youtube.ad.HideAds
import io.github.nexalloy.morphe.youtube.interaction.channelsearch.channelSearchPatch
import io.github.nexalloy.morphe.youtube.interaction.copytext.copyTextPatch
import io.github.nexalloy.morphe.youtube.interaction.copyvideolink.CopyVideoLinkButtonPatch
import io.github.nexalloy.morphe.youtube.interaction.downloads.Downloads
import io.github.nexalloy.morphe.youtube.interaction.postimages.postImageDownloadPatch
import io.github.nexalloy.morphe.youtube.interaction.posttranslate.postTranslatePatch
import io.github.nexalloy.morphe.youtube.interaction.seekbar.seekbarPatch
import io.github.nexalloy.morphe.youtube.interaction.swipecontrols.SwipeControls
import io.github.nexalloy.morphe.youtube.layout.branding.header.changeHeaderPatch
import io.github.nexalloy.morphe.youtube.layout.buttons.action.HideVideoActionButtons
import io.github.nexalloy.morphe.youtube.layout.buttons.navigation.NavigationBar
import io.github.nexalloy.morphe.youtube.layout.captions.AutoCaptions
import io.github.nexalloy.morphe.youtube.layout.dearrow.deArrowPatch
import io.github.nexalloy.morphe.youtube.layout.flyout.addToQueuePatch
import io.github.nexalloy.morphe.youtube.layout.hide.general.HideLayoutComponents
import io.github.nexalloy.morphe.youtube.layout.hide.shorts.HideShortsComponents
import io.github.nexalloy.morphe.youtube.layout.returnyoutubedislike.returnYouTubeDislikePatch
import io.github.nexalloy.morphe.youtube.layout.sharesheet.openSystemShareSheetPatch
import io.github.nexalloy.morphe.youtube.layout.shortsnoresume.DisableShortsResumingOnStartup
import io.github.nexalloy.morphe.youtube.layout.sponsorblock.SponsorBlock
import io.github.nexalloy.morphe.youtube.layout.startpage.changeStartPagePatch
import io.github.nexalloy.morphe.youtube.layout.thumbnails.BypassImageRegionRestrictionsPatch
import io.github.nexalloy.morphe.youtube.misc.backgroundplayback.BackgroundPlayback
import io.github.nexalloy.morphe.youtube.misc.debugging.EnableDebugging
import io.github.nexalloy.morphe.youtube.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.youtube.misc.settings.SettingsHook
import io.github.nexalloy.morphe.youtube.misc.whitelist.channelWhitelistPatch
import io.github.nexalloy.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.morphe.youtube.video.audio.ForceOriginalAudio
import io.github.nexalloy.morphe.youtube.video.codecs.DisableVideoCodecs
import io.github.nexalloy.morphe.youtube.video.quality.VideoQuality
import io.github.nexalloy.morphe.youtube.video.speed.PlaybackSpeed
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

val ExtensionHook = patch(name = "<ExtensionHook>") {
    injectHostClassLoaderToSelf(this::class.java.classLoader!!, classLoader)
    hookExtensionClassLoading(this::class.java.classLoader!!)
    DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onCreate(Landroid/os/Bundle;)V").hookMethod {
        before {
            val mainActivity = it.thisObject as Activity
            mainActivity.addModuleAssets()
            Utils.setContext(mainActivity)
        }
    }

    ExtensionResourceHook.run(this)
}

private val overrides = mutableMapOf<Long, (Boolean) -> Boolean>()

fun insertLiteralOverride(id: Long, override: Boolean = false) {
    overrides[id] = { override }
}

fun insertLiteralOverride(id: Long, override: (Boolean) -> Boolean) {
    overrides[id] = override
}

val FeatureOverride = patch {
    dependsOn(VersionCheck)

    val methods = mutableListOf(::experimentalBooleanFeatureFlagFingerprint.method)
    if (is_21_42_or_greater) {
        methods += ::experimentalIndexedBooleanFeatureFlagFingerprint.method
    }
    methods.forEach { method ->
        method.hookMethod {
            priority = XposedInterface.PRIORITY_DEFAULT - 1
            after {
                val id = it.args.atLast(2) as Long
                val orig = it.result as Boolean
                val match = overrides[id] ?: return@after
                it.result = match(orig)
            }
        }
    }
}

val YouTubePatches = arrayOf(
    ExtensionHook,
    BackgroundPlayback,
    changeHeaderPatch,
    SanitizeSharingLinks,
    HideAds,
    SponsorBlock,
    copyTextPatch,
    returnYouTubeDislikePatch,
    CopyVideoLinkButtonPatch,
    Downloads,
    postImageDownloadPatch,
    postTranslatePatch,
    HideShortsComponents,
    DisableShortsResumingOnStartup,
    NavigationBar,
    SwipeControls,
    VideoQuality,
    HideLayoutComponents,
    HideVideoActionButtons,
    PlaybackSpeed,
    AutoCaptions,
    EnableDebugging,
    ForceOriginalAudio,
    DisableVideoCodecs,
    deArrowPatch,
    openSystemShareSheetPatch,
    seekbarPatch,
    addToQueuePatch,
    BypassImageRegionRestrictionsPatch,
    CheckRecycleBitmapMediaSession,
    changeStartPagePatch,
    channelSearchPatch,
    channelWhitelistPatch,
    // make sure settingsHook at end to build preferences
    SettingsHook,
    FeatureOverride
)
