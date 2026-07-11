package io.github.nexalloy.morphe.music

import io.github.nexalloy.ExtensionResourceHook
import io.github.nexalloy.morphe.music.ad.HideAds
import io.github.nexalloy.morphe.music.audio.exclusiveaudio.EnableExclusiveAudioPlayback
import io.github.nexalloy.morphe.music.layout.upgradebutton.HideUpgradeButton
import io.github.nexalloy.morphe.music.misc.backgroundplayback.BackgroundPlayback
import io.github.nexalloy.morphe.music.misc.debugging.EnableDebugging
import io.github.nexalloy.morphe.music.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.music.misc.settings.SettingsHook
import io.github.nexalloy.morphe.shared.misc.CheckRecycleBitmapMediaSession

val YTMusicPatches = arrayOf(
    ExtensionResourceHook,
    BackgroundPlayback,
    HideUpgradeButton,
    HideAds,
    EnableExclusiveAudioPlayback,
    CheckRecycleBitmapMediaSession,
    EnableDebugging,
    SanitizeSharingLinks,
    SettingsHook
)