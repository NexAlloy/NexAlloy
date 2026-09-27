package io.github.nexalloy.revanced.twitch

import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture
import io.github.nexalloy.revanced.twitch.ad.audio.BlockAudioAds
import io.github.nexalloy.revanced.twitch.ad.display.HideDisplayAds
import io.github.nexalloy.revanced.twitch.ad.video.BlockVideoAds
import io.github.nexalloy.revanced.twitch.chat.antidelete.ShowDeletedMessages
import io.github.nexalloy.revanced.twitch.chat.autoclaim.AutoClaimChannelPoints
import io.github.nexalloy.revanced.twitch.debug.EnableDebugMode

val TwitchPatches = arrayOf(
    AllowScreenCapture,
    BlockAudioAds,
    BlockVideoAds,
    HideDisplayAds,
    AutoClaimChannelPoints,
    ShowDeletedMessages,
    EnableDebugMode,
    DisableCommonAnalytics,
)
