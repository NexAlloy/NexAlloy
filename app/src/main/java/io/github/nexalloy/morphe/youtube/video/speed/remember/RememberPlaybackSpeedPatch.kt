package io.github.nexalloy.morphe.youtube.video.speed.remember

import app.morphe.extension.youtube.patches.playback.speed.RememberPlaybackSpeedPatch
import app.morphe.extension.youtube.settings.preference.CustomVideoSpeedListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_11_or_greater
import io.github.nexalloy.morphe.youtube.shared.InitializePlaybackSpeedValuesFingerprint
import io.github.nexalloy.morphe.youtube.video.information.PlaybackSpeedMenu
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.morphe.youtube.video.information.userSelectedPlaybackSpeedHook
import io.github.nexalloy.morphe.youtube.video.speed.custom.CustomPlaybackSpeed
import io.github.nexalloy.morphe.youtube.video.speed.settingsMenuShortsSpeedGroup
import io.github.nexalloy.morphe.youtube.video.speed.settingsMenuVideoSpeedGroup
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val RememberPlaybackSpeed = patch {
    dependsOn(
        VideoId,
        VideoInformationPatch,
        CustomPlaybackSpeed,
        VersionCheck
    )

    settingsMenuVideoSpeedGroup.addAll(
        listOf(
            ListPreference(
                key = "morphe_playback_speed_default",
                // Entries and values are set by the extension code based on the actual speeds available.
                entriesKey = null,
                entryValuesKey = null,
                tag = CustomVideoSpeedListPreference::class.java
            ),
            ListPreference(
                key = "morphe_playback_audio_pitch_default",
                // List is shared with video speeds.
                entriesKey = null,
                entryValuesKey = null,
                tag = CustomVideoSpeedListPreference::class.java
            ),
            SwitchPreference("morphe_remember_playback_speed_last_selected", summary = true),
            SwitchPreference("morphe_remember_playback_speed_last_selected_toast", summary = true),
            SwitchPreference("morphe_disable_playback_speed_music", summary = true)
        )
    )

    settingsMenuShortsSpeedGroup.addAll(
        listOf(
            ListPreference(
                key = "morphe_shorts_playback_speed_default",
                entriesKey = null,
                entryValuesKey = null,
                tag = CustomVideoSpeedListPreference::class.java
            ),
            SwitchPreference("morphe_shorts_remember_playback_speed", summary = true)
        )
    )

    onCreateHook(RememberPlaybackSpeedPatch::newVideoStarted)

    userSelectedPlaybackSpeedHook(RememberPlaybackSpeedPatch::userSelectedPlaybackSpeed)

    hookPlayerResponseVideoId(RememberPlaybackSpeedPatch::preloadMusicVideoFetch)

    // The faster speed of a held Short is not replaced with the Shorts speed.
    if (is_21_11_or_greater) {
        val setSpeed = ::speedmasterSetSpeedMethod.method
        SpeedmasterStartFingerprint.hookMethod(scopedHook(setSpeed) {
            before { RememberPlaybackSpeedPatch.onSpeedmasterStarted() }
        })
    }

    /*
     * Hook the code that is called when the playback speeds are initialized, and sets the playback speed
     */
    InitializePlaybackSpeedValuesFingerprint.hookMethod {
        before {
            RememberPlaybackSpeedPatch.setDefaultPlaybackSpeed(PlaybackSpeedMenu(it.thisObject))
        }
    }
}
