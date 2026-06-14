package io.github.nexalloy.morphe.youtube.interaction.copyvideolink

import app.morphe.extension.youtube.videoplayer.CopyVideoLinkButton
import io.github.nexalloy.R
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import io.github.nexalloy.morphe.youtube.layout.player.buttons.addPlayerBottomButton
import io.github.nexalloy.morphe.youtube.layout.player.buttons.playerOverlayButtonsHook
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addLegacyBottomControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeLegacyBottomControl
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val CopyVideoLinkButtonPatch = patch(
    name = "Copy video link",
    description = "Adds options to display buttons in the video player to copy video links.",
) {
    dependsOn(
        LegacyPlayerControls,
        playerOverlayButtonsHook,
        VideoInformationPatch,
    )

    addPlayerOverlayPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_copy_video_link_button", summary = true),
            SwitchPreference("morphe_copy_video_link_with_timestamp_button", summary = true)
        )
    )
    addPlayerBottomButton(CopyVideoLinkButton::initializeButton)

    addLegacyBottomControl(R.layout.morphe_copy_video_url_button)
    initializeLegacyBottomControl(
        ControlInitializer(
            R.id.morphe_copy_video_url_button,
            CopyVideoLinkButton::initializeLegacyButton,
            CopyVideoLinkButton::setVisibility,
            CopyVideoLinkButton::setVisibilityImmediate,
            CopyVideoLinkButton::setVisibilityNegatedImmediate
        )
    )
}