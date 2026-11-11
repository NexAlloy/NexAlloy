package io.github.nexalloy.morphe.youtube.interaction.downloads

import android.net.Uri
import app.morphe.extension.shared.settings.preference.ExternalDownloaderPreference
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch
import app.morphe.extension.shared.spoof.requests.StreamingDataRequest
import app.morphe.extension.youtube.patches.DownloadsPatch
import app.morphe.extension.youtube.patches.InAppDownloadsPatch
import app.morphe.extension.youtube.settings.preference.ExportFolderPreference
import app.morphe.extension.youtube.videoplayer.ExportDownloadButton
import app.morphe.extension.youtube.videoplayer.ExternalDownloadButton
import app.morphe.extension.youtube.videoplayer.ShortsDownloadButton
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import io.github.nexalloy.R
import io.github.nexalloy.callMethod
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.request.buildRequestPatch
import io.github.nexalloy.morphe.shared.misc.request.hookBuildRequest
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.misc.spoof.plainStreamRequestsPatch
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.misc.auth.authHookPatch
import io.github.nexalloy.morphe.youtube.misc.commandrouter.commandRouterHookPatch
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addTopControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeTopControl
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val Downloads = patch(
    name = "Downloads",
    description = "Adds support to download videos with an external downloader app " +
            "using the in-app download button or a video player action button, " +
            "adds an option to download videos in-app, and exports saved videos to the device.",
) {
    dependsOn(
        LegacyPlayerControls,
        VideoInformationPatch,
        buildRequestPatch,
        plainStreamRequestsPatch,
        authHookPatch,
        elementProtoParserHookPatch,
        flyoutPatch,
        commandRouterHookPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_external_downloader_screen",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                ListPreference("morphe_download_mode"),
                SwitchPreference("morphe_shorts_download_flyout_button", summary = true),
                NonInteractivePreference(
                    "morphe_export_download_folder",
                    summaryKey = null,
                    tag = ExportFolderPreference::class.java,
                    selectable = true
                ),
                SwitchPreference("morphe_external_downloader_overlay_button", summary = true),
                TextPreference(
                    "morphe_external_downloader_name",
                    tag = ExternalDownloaderPreference::class.java
                ),
                SwitchPreference("morphe_external_downloader_quick_menu", summary = true),
                SwitchPreference("morphe_external_downloader_mark_watched", summary = true),
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
            ExternalDownloadButton::initializeLegacyButton
        )
    )

    OfflineVideoEndpointFingerprint.hookMethod {
        before {
            if (DownloadsPatch.inAppDownloadButtonOnClick(it.args[2] as String)) it.result = null
        }
    }

    setExtensionIsPatchIncluded(DownloadsPatch::class.java)
    setExtensionIsPatchIncluded(ShortsDownloadButton::class.java)
    setExtensionIsPatchIncluded(ExportDownloadButton::class.java)

    // Commands run by the extension have no interaction logger.
    InAppDownloadsPatch::class.java.getDeclaredMethod("getNoOpInteractionLogger")
        .hookMethod(returnConstant(::noOpInteractionLogger.field.get(null)))

    // Open the external downloader with the playlist link, instead of saving the playlist.
    OfflinePlaylistEndpointFingerprint.hookMethod {
        before {
            if (DownloadsPatch.offlinePlaylistEndpointOnClick(it.args[2] as String, it.args[3] as Enum<*>)) {
                it.result = null
            }
        }
    }

    // Add the Download button to the playlist page headers, which only Premium accounts get.
    hookElement(InAppDownloadsPatch::addPlaylistDownloadButton)

    // region In-app downloads.

    // The module and host use separate protobuf implementations.
    InAppDownloadsPatch.setMessageSerializer { it.callMethod("toByteArray") as ByteArray }
    val parseFrom = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    fun parse(defaultInstance: Any, bytes: ByteArray): Any =
        parseFrom.invoke(null, defaultInstance, bytes, registry)!!

    // Save the video instead of asking the server for the download action, which answers with the Premium upsell.
    val commandDefault = ::offlineCommandDefaultInstance.field.get(null)!!
    ResolveCommandFingerprint.hookMethod {
        before {
            InAppDownloadsPatch.replaceDownloadActionCommand(it.args[0])?.let { bytes ->
                it.args[0] = parse(commandDefault, bytes)
            }
        }
    }

    // Unlock the qualities of the video, and save the video instead of showing the Premium upsell.
    val offlineabilityDefault = ::offlineabilityDefaultInstance.field.get(null)!!
    ::saveOfflineVideoMethod.hookMethod {
        before {
            InAppDownloadsPatch.unlockOfflineability(it.args[1])?.let { bytes ->
                it.args[1] = parse(offlineabilityDefault, bytes)
            }
        }
    }

    // Save the video instead of showing the Premium upsell of the offline upsell endpoint.
    val commandField = ::offlineUpsellCommandField.field
    val upsellDefault = ::offlineUpsellDefaultInstance.field.get(null)!!
    OfflineUpsellResponseFingerprint.hookMethod {
        before {
            InAppDownloadsPatch.replaceOfflineUpsell(commandField.get(it.thisObject), it.args[0])?.let { bytes ->
                it.args[0] = parse(upsellDefault, bytes)
            }
        }
    }

    // Keep saved videos playable, if the server disables them when they are refreshed.
    val offlineStateDefault = ::offlineStateDefaultInstance.field.get(null)!!
    ::updateOfflineVideoPolicyMethods.dexMethodList.forEach { method ->
        method.hookMethod {
            before {
                InAppDownloadsPatch.replaceDisabledOfflineState(it.args[2])?.let { bytes ->
                    it.args[2] = parse(offlineStateDefault, bytes)
                }
            }
        }
    }

    // Keep download authentication headers without starting playback spoof requests.
    hookBuildRequest { url, headers ->
        if (url != null && headers != null && Uri.parse(url).path?.startsWith("/youtubei/") == true) {
            StreamingDataRequest.setAppHeaders(headers)
        }
        headers
    }

    // Save the video with streams of another client, if the server declines to save the video.
    val getter = ::playerResponseGetterMethod.method
    val fromBytes = ::playerResponseModelFromBytesMethod.method
    OfflinePlayerResponseFingerprint.hookMethod(scopedHook(::offlinePlayerResponseFactoryMethod.method) {
        after {
            if (it.hasThrowable()) return@after
            val model = it.result ?: return@after
            val bytes = InAppDownloadsPatch.replaceDeclinedOfflinePlayerResponse(getter.invoke(model))
                ?: return@after
            // The playback stream override would replace the streams that can be saved.
            SpoofVideoStreamsPatch.setKeepStreamsOnThread(true)
            val replacement = try {
                // The response time, used for the expiration of the stream urls.
                fromBytes.invoke(null, bytes, System.currentTimeMillis())
            } finally {
                SpoofVideoStreamsPatch.setKeepStreamsOnThread(false)
            }
            InAppDownloadsPatch.onOfflinePlayerResponseReplaced(replacement)
            if (replacement != null) it.result = replacement
        }
    })

    // endregion
}
