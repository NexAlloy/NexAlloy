/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3982
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.video.speed.shorts

import app.morphe.extension.youtube.patches.playback.speed.ShortsPlaybackSpeedFlyoutPatch
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.speed.custom.CustomPlaybackSpeed
import io.github.nexalloy.morphe.youtube.video.speed.settingsMenuShortsSpeedGroup
import io.github.nexalloy.patch

internal val ShortsPlaybackSpeedFlyout = patch(
    description = "Adds a playback speed item to the flyout menu of the Shorts player.",
) {
    dependsOn(
        flyoutPatch,
        VideoInformationPatch,
        CustomPlaybackSpeed,
    )

    settingsMenuShortsSpeedGroup.add(
        SwitchPreference("morphe_shorts_playback_speed_flyout_button", summary = true)
    )

    setExtensionIsPatchIncluded(ShortsPlaybackSpeedFlyoutPatch::class.java)
}
