package io.github.nexalloy.morphe.youtube.video.speed.button

import app.morphe.extension.youtube.videoplayer.PlaybackSpeedDialogButton
import io.github.nexalloy.R
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import io.github.nexalloy.morphe.youtube.layout.player.buttons.addPlayerBottomButton
import io.github.nexalloy.morphe.youtube.layout.player.buttons.playerOverlayButtonsHook
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addLegacyBottomControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeLegacyBottomControl
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.userSelectedPlaybackSpeedHook
import io.github.nexalloy.morphe.youtube.video.information.videoSpeedChangedHook
import io.github.nexalloy.morphe.youtube.video.speed.custom.CustomPlaybackSpeed
import io.github.nexalloy.patch

val PlaybackSpeedButton = patch(
    description = "Adds the option to display playback speed dialog button in the video player.",
) {
    dependsOn(
        CustomPlaybackSpeed,
        LegacyPlayerControls,
        playerOverlayButtonsHook,
        VideoInformationPatch,
    )

    addPlayerOverlayPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_playback_speed_dialog_button", summary = true),
            SwitchPreference("morphe_playback_speed_button_cycle", summary = true),
            TextPreference(
                "morphe_playback_speed_button_cycle_speeds",
                inputType = InputType.TEXT_MULTI_LINE
            )
        ),
    )

    addPlayerBottomButton(PlaybackSpeedDialogButton::initializeButton)

    addLegacyBottomControl(R.layout.morphe_playback_speed_dialog_button)
    initializeLegacyBottomControl(
        ControlInitializer(
            R.id.morphe_playback_speed_dialog_button_container,
            PlaybackSpeedDialogButton::initializeLegacyButton
        )
    )

    videoSpeedChangedHook(PlaybackSpeedDialogButton::videoSpeedChanged)
    userSelectedPlaybackSpeedHook(PlaybackSpeedDialogButton::videoSpeedChanged)
}

