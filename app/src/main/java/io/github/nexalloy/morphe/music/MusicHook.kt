package io.github.nexalloy.morphe.music

import io.github.nexalloy.ExtensionResourceHook
import io.github.nexalloy.morphe.music.ad.hideAdsPatch
import io.github.nexalloy.morphe.music.misc.audio.EnableExclusiveAudioPlayback
import io.github.nexalloy.morphe.music.layout.buttons.navigation.navigationBarPatch
import io.github.nexalloy.morphe.music.misc.backgroundplayback.BackgroundPlayback
import io.github.nexalloy.morphe.music.misc.debugging.EnableDebugging
import io.github.nexalloy.morphe.music.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.music.misc.settings.SettingsHook
import io.github.nexalloy.morphe.shared.misc.CheckRecycleBitmapMediaSession

val YTMusicPatches = arrayOf(
    ExtensionResourceHook,
    BackgroundPlayback,
    navigationBarPatch,
    hideAdsPatch,
    EnableExclusiveAudioPlayback,
    CheckRecycleBitmapMediaSession,
    EnableDebugging,
    SanitizeSharingLinks,
    SettingsHook
)
