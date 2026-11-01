package io.github.nexalloy.morphe.youtube.video.speed

import app.morphe.extension.shared.settings.preference.NoTitlePreferenceCategory
import app.morphe.extension.youtube.patches.VideoInformation
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.speed.button.PlaybackSpeedButton
import io.github.nexalloy.morphe.youtube.video.speed.custom.CustomPlaybackSpeed
import io.github.nexalloy.morphe.youtube.video.speed.remember.RememberPlaybackSpeed
import io.github.nexalloy.morphe.youtube.video.speed.shorts.ShortsPlaybackSpeedFlyout
import io.github.nexalloy.patch

/**
 * Speed menu settings.  Used to organize all speed related settings together.
 */
internal val settingsMenuVideoSpeedGroup = mutableSetOf<BasePreference>()
internal val settingsMenuShortsSpeedGroup = mutableSetOf<BasePreference>()

@Suppress("unused")
val PlaybackSpeed = patch(
    name = "Playback speed",
    description = "Adds options to customize available playback speeds, set a default playback speed, " +
            "and show a speed dialog button in the video player.",
) {
    dependsOn(
        CustomPlaybackSpeed,
        RememberPlaybackSpeed,
        PlaybackSpeedButton,
        ShortsPlaybackSpeedFlyout,
    )

    setExtensionIsPatchIncluded(VideoInformation::class.java)

    PreferenceScreen.VIDEO.addPreferences(
        PreferenceCategory(
            key = "morphe_zz_video_key", // Dummy key to force the speed settings last.
            titleKey = null,
            sorting = Sorting.UNSORTED,
            tag = NoTitlePreferenceCategory::class.java,
            preferences = settingsMenuVideoSpeedGroup
        )
    )

    PreferenceScreen.SHORTS.addPreferences(
        noTitleUnsortedPreferenceCategory(settingsMenuShortsSpeedGroup)
    )
}

