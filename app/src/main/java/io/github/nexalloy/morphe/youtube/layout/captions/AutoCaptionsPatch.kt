package io.github.nexalloy.morphe.youtube.layout.captions

import app.morphe.extension.youtube.patches.AutoCaptionsPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private const val NO_VOLUME_CAPTIONS_FEATURE_FLAG = 45692436L

val AutoCaptions = patch(
    name = "Auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
) {
    dependsOn(
        VersionCheck,
        VideoInformationPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        if (is_20_26_or_greater) {
            ListPreference("morphe_auto_captions_style")
        } else {
            ListPreference(
                key = "morphe_auto_captions_style",
                entriesKey = "morphe_auto_captions_style_legacy_entries",
                entryValuesKey = "morphe_auto_captions_style_legacy_entry_values"
            )
        }
    )

    SubtitleManagerFingerprint.hookMethod(
        scopedHook(::subtitleManagerBooleanMethod.member) {
            after {
                it.result = AutoCaptionsPatch.disableAutoCaptions(it.result as Boolean)
            }
        }
    )

    onCreateHook.add { AutoCaptionsPatch.newVideoStarted(it) }

    StartVideoInformerFingerprint.hookMethod {
        before { AutoCaptionsPatch.videoInformationLoaded() }
    }

    if (is_20_26_or_greater) {
        if (is_21_30_or_greater) {
            ::noVolumeCaptionsFeatureFlagGetters.dexMethodList.forEach { method ->
                method.hookMethod {
                    after { param ->
                        if (param.hasThrowable()) return@after
                        if (param.args.none { (it as? Number)?.toLong() == NO_VOLUME_CAPTIONS_FEATURE_FLAG }) {
                            return@after
                        }
                        param.result = AutoCaptionsPatch.disableMuteAutoCaptions(param.result as Boolean)
                    }
                }
            }
        } else {
            insertLiteralOverride(
                NO_VOLUME_CAPTIONS_FEATURE_FLAG,
                AutoCaptionsPatch::disableMuteAutoCaptions
            )
        }
    }
}
