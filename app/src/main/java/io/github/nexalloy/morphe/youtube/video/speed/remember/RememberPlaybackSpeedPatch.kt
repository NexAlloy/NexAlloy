package io.github.nexalloy.morphe.youtube.video.speed.remember

import app.morphe.extension.youtube.patches.playback.speed.RememberPlaybackSpeedPatch
import app.morphe.extension.youtube.settings.preference.CustomVideoSpeedListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.morphe.youtube.video.information.setPlaybackSpeedClassField
import io.github.nexalloy.morphe.youtube.video.information.setPlaybackSpeedContainerClassField
import io.github.nexalloy.morphe.youtube.video.information.setPlaybackSpeedMethod
import io.github.nexalloy.morphe.youtube.video.information.userSelectedPlaybackSpeedHook
import io.github.nexalloy.morphe.youtube.video.speed.custom.CustomPlaybackSpeed
import io.github.nexalloy.morphe.youtube.video.speed.settingsMenuVideoSpeedGroup
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.patch

val RememberPlaybackSpeed = patch {
    dependsOn(
        VideoId,
        VideoInformationPatch,
        CustomPlaybackSpeed
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
            SwitchPreference("morphe_remember_playback_speed_last_selected", summary = true),
            SwitchPreference("morphe_remember_playback_speed_last_selected_toast", summary = true),
            SwitchPreference("morphe_disable_playback_speed_music", summary = true)
        )
    )

    onCreateHook.add { RememberPlaybackSpeedPatch.newVideoStarted(it) }

    userSelectedPlaybackSpeedHook.add { RememberPlaybackSpeedPatch.userSelectedPlaybackSpeed(it) }

    hookPlayerResponseVideoId(RememberPlaybackSpeedPatch::preloadMusicVideoFetch)

    /*
     * Hook the code that is called when the playback speeds are initialized, and sets the playback speed
     */
    ::initializePlaybackSpeedValuesFingerprint.hookMethod {
        val onItemClickListenerClassField = ::onItemClickListenerClassFieldReference.field
        before {
            val playbackSpeedOverride = RememberPlaybackSpeedPatch.getPlaybackSpeedOverride()
            if (playbackSpeedOverride > 0.0f) {
                onItemClickListenerClassField.get(it.thisObject)
                    .let { setPlaybackSpeedContainerClassField.get(it) }
                    .let { setPlaybackSpeedClassField.get(it) }
                    .let { setPlaybackSpeedMethod(it, playbackSpeedOverride) }
            }
        }
    }
}
