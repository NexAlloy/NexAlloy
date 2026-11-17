package io.github.nexalloy.morphe.youtube.layout.captions

import app.morphe.extension.youtube.patches.AutoCaptionsPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.hookVideoStarted
import io.github.nexalloy.morphe.youtube.shared.startVideoInformerPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val AutoCaptions = patch(
    name = "Auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
) {
    dependsOn(
        VersionCheck,
        startVideoInformerPatch,
        VideoInformationPatch
    )

    PreferenceScreen.PLAYER.addPreferences(
        ListPreference("morphe_auto_captions_style")
    )

    val reason = ::captionSelectionReasonField.field
    val forced = ::forcedCaptionTrackField.field
    val owners = ::forcedCaptionOwnerFields.fieldList
    SubtitleManagerFingerprint.hookMethod {
        before {
            AutoCaptionsPatch.setCaptionSelectionReason(reason.getInt(it.args[0]))
            val owner = owners.fold(it.thisObject as Any?) { value, field -> value?.let(field::get) }
            if (owner != null) {
                val original = forced.getBoolean(owner)
                val shown = AutoCaptionsPatch.showForcedCaptions(original)
                if (shown != original) {
                    it.setObjectExtra("forcedCaptionOwner", owner)
                    it.setObjectExtra("forcedCaptionValue", original)
                    forced.setBoolean(owner, shown)
                }
            }
        }
        after {
            it.getObjectExtra("forcedCaptionOwner")?.let { owner ->
                forced.setBoolean(owner, it.getObjectExtra("forcedCaptionValue") as Boolean)
            }
        }
    }

    SubtitleManagerFingerprint.hookMethod(scopedHook(::subtitleTrackIsDisableOptionMethod.method) {
        after {
            if (outerParam.getObjectExtra("autoCaptionsHandled") == true) return@after
            outerParam.setObjectExtra("autoCaptionsHandled", true)
            it.result = AutoCaptionsPatch.disableAutoCaptions(it.result as Boolean)
        }
    })

    onCreateHook(AutoCaptionsPatch::newVideoStarted)

    hookVideoStarted(AutoCaptionsPatch::videoInformationLoaded)

    // Disable mute auto captions feature flag.
    insertLiteralOverride(45692436L, AutoCaptionsPatch::disableMuteAutoCaptions)
}
