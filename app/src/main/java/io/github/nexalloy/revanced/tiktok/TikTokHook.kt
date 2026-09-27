package io.github.nexalloy.revanced.tiktok

import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture
import io.github.nexalloy.revanced.tiktok.ad.HideAds
import io.github.nexalloy.revanced.tiktok.interaction.antirecording.DisableScreenCaptureDetection
import io.github.nexalloy.revanced.tiktok.interaction.cleardisplay.RememberClearDisplay
import io.github.nexalloy.revanced.tiktok.interaction.downloads.EnableDownloads
import io.github.nexalloy.revanced.tiktok.interaction.looping.StopVideoLooping
import io.github.nexalloy.revanced.tiktok.interaction.quickactions.DisableLongPressQuickShare
import io.github.nexalloy.revanced.tiktok.interaction.quickactions.DisableLongPressRepost
import io.github.nexalloy.revanced.tiktok.interaction.quickactions.HideQuickCommentReactions
import io.github.nexalloy.revanced.tiktok.interaction.seekbar.ShowSeekbar
import io.github.nexalloy.revanced.tiktok.interaction.speed.EnablePlaybackSpeed
import io.github.nexalloy.revanced.tiktok.misc.externalbrowser.OpenExternalLinks
import io.github.nexalloy.revanced.tiktok.misc.login.DisableLoginRequirement
import io.github.nexalloy.revanced.tiktok.misc.login.FixGoogleLogin
import io.github.nexalloy.revanced.tiktok.misc.share.SanitizeShareUrls

val TikTokPatches = arrayOf(
    AllowScreenCapture,
    HideAds,
    ShowSeekbar,
    EnablePlaybackSpeed,
    EnableDownloads,
    DisableLoginRequirement,
    FixGoogleLogin,
    SanitizeShareUrls,
    RememberClearDisplay,
    DisableScreenCaptureDetection,
    OpenExternalLinks,
    StopVideoLooping,
    HideQuickCommentReactions,
    DisableLongPressQuickShare,
    DisableLongPressRepost,
    DisableCommonAnalytics,
)
