package io.github.nexalloy.morphe.youtube

import android.app.Activity
import app.morphe.extension.shared.Utils
import io.github.libxposed.api.XposedInterface
import io.github.nexalloy.ExtensionResourceHook
import io.github.nexalloy.addModuleAssets
import io.github.nexalloy.injectHostClassLoaderToSelf
import io.github.nexalloy.injectSelfClassLoaderToHost
import io.github.nexalloy.morphe.shared.misc.CheckRecycleBitmapMediaSession
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.youtube.ad.HideAds
import io.github.nexalloy.morphe.youtube.interaction.copyvideolink.CopyVideoLinkButtonPatch
import io.github.nexalloy.morphe.youtube.interaction.downloads.Downloads
import io.github.nexalloy.morphe.youtube.interaction.swipecontrols.SwipeControls
import io.github.nexalloy.morphe.youtube.layout.buttons.action.HideVideoActionButtons
import io.github.nexalloy.morphe.youtube.layout.buttons.navigation.NavigationBar
import io.github.nexalloy.morphe.youtube.layout.captions.AutoCaptions
import io.github.nexalloy.morphe.youtube.layout.hide.general.HideLayoutComponents
import io.github.nexalloy.morphe.youtube.layout.hide.shorts.HideShortsComponents
import io.github.nexalloy.morphe.youtube.layout.shortsnoresume.DisableShortsResumingOnStartup
import io.github.nexalloy.morphe.youtube.layout.sponsorblock.SponsorBlock
import io.github.nexalloy.morphe.youtube.layout.thumbnails.AlternativeThumbnailsPatch
import io.github.nexalloy.morphe.youtube.layout.thumbnails.BypassImageRegionRestrictionsPatch
import io.github.nexalloy.morphe.youtube.misc.backgroundplayback.BackgroundPlayback
import io.github.nexalloy.morphe.youtube.misc.debugging.EnableDebugging
import io.github.nexalloy.morphe.youtube.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.youtube.misc.settings.SettingsHook
import io.github.nexalloy.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.morphe.youtube.video.audio.ForceOriginalAudio
import io.github.nexalloy.morphe.youtube.video.codecs.DisableVideoCodecs
import io.github.nexalloy.morphe.youtube.video.quality.VideoQuality
import io.github.nexalloy.morphe.youtube.video.speed.PlaybackSpeed
import io.github.nexalloy.patch
import io.github.nexalloy.atLast
import org.luckypray.dexkit.wrap.DexMethod

val ExtensionHook = patch(name = "<ExtensionHook>") {
    injectHostClassLoaderToSelf(this::class.java.classLoader!!, classLoader)
    injectSelfClassLoaderToHost(this::class.java.classLoader!!, classLoader)
    DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onCreate(Landroid/os/Bundle;)V").hookMethod {
        before {
            val mainActivity = it.thisObject as Activity
            mainActivity.addModuleAssets()
            // MainHook already installs the Application as the process-wide extension context.
            // Keep the Activity weakly referenced for UI/resource helpers without retaining its
            // view hierarchy for the lifetime of the process.
            Utils.setActivity(mainActivity)
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
    ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
        priority = XposedInterface.PRIORITY_DEFAULT - 1
        after {
            val id = it.args.atLast(2) as Long
            val orig = it.result as Boolean
            val match = overrides[id] ?: return@after
            it.result = match(orig)
        }
    }
}

val YouTubePatches = arrayOf(
    ExtensionHook,
    BackgroundPlayback,
    SanitizeSharingLinks,
    HideAds,
    SponsorBlock,
    CopyVideoLinkButtonPatch,
    Downloads,
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
    AlternativeThumbnailsPatch,
    BypassImageRegionRestrictionsPatch,
    CheckRecycleBitmapMediaSession,
    // make sure settingsHook at end to build preferences
    SettingsHook,
    FeatureOverride
)
