package io.github.nexalloy.morphe.youtube.interaction.downloads

import android.app.Activity
import app.morphe.extension.youtube.patches.DownloadsPatch
import app.morphe.extension.youtube.settings.preference.ExternalDownloaderPreference
import app.morphe.extension.youtube.videoplayer.ExternalDownloadButton
import io.github.nexalloy.R
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addTopControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeTopControl
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.mainActivityOnCreateFingerprint
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val Downloads = patch(
    name = "Downloads",
    description = "Adds support to download videos with an external downloader app " +
            "using the in-app download button or a video player action button.",
) {
    dependsOn(
        LegacyPlayerControls,
        VideoInformationPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_external_downloader_screen",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_external_downloader", summary = true),
                SwitchPreference("morphe_external_downloader_action_button", summary = true),
                TextPreference(
                    "morphe_external_downloader_name",
                    tag = ExternalDownloaderPreference::class.java
                ),
            ),
        ),
    )

    addTopControl(
        R.layout.morphe_external_download_button,
        R.id.morphe_external_download_button,
        R.id.morphe_external_download_button
    )

    initializeTopControl(
        ControlInitializer(
            R.id.morphe_external_download_button,
            ExternalDownloadButton::initializeLegacyButton,
            ExternalDownloadButton::setVisibility,
            ExternalDownloadButton::setVisibilityImmediate,
            ExternalDownloadButton::setVisibilityNegatedImmediate,
        )
    )

    ::mainActivityOnCreateFingerprint.hookMethod {
        before { DownloadsPatch.setMainActivity(it.thisObject as Activity) }
    }

    OfflineVideoEndpointFingerprint.hookMethod {
        before {
            if (DownloadsPatch.inAppDownloadButtonOnClick(it.args[2] as String)) it.result = null
        }
    }
}