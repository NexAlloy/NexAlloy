package io.github.nexalloy.morphe.shared.misc.audio.tracks

import app.morphe.extension.shared.patches.ForceOriginalAudioPatch
import app.morphe.extension.shared.settings.preference.ForceOriginalAudioSwitchPreference
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun forceOriginalAudioPatch(
    block: PatchExecutor.() -> Unit = {},
    executeBlock: PatchExecutor.() -> Unit = {},
    fixUseLocalizedAudioTrackFlag: PatchExecutor.() -> Boolean,
    mainActivityOnCreateFingerprint: Fingerprint,
    subclassExtensionSetEnabled: () -> Unit,
    preferenceScreen: BasePreferenceScreen.Screen
) = patch(
    name = "Force original audio",
    description = "Adds an option to always use the original audio track.",
) {
    block()

    preferenceScreen.addPreferences(
        SwitchPreference(
            key = "morphe_force_original_audio",
            tag = ForceOriginalAudioSwitchPreference::class.java
        )
    )

    mainActivityOnCreateFingerprint.hookMethod {
        before {
            subclassExtensionSetEnabled()
        }
    }

    // Disable feature flag that ignores the default track flag
    // and instead overrides to the user region language.
    if (fixUseLocalizedAudioTrackFlag()) {
        ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
            after {
                if (it.args[1] == AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG) {
                    it.result =
                        ForceOriginalAudioPatch.ignoreDefaultAudioStream(it.result as Boolean)
                }
            }
        }
    }

    val getFormatStreamModelGetter = ::getFormatStreamModelGetter.dexMethodList
    val getIsDefaultAudioTrackFingerprint = getFormatStreamModelGetter[0]
    val getAudioTrackIdFingerprint = getFormatStreamModelGetter[1]
    val getAudioTrackDisplayNameFingerprint = getFormatStreamModelGetter[2]

    getIsDefaultAudioTrackFingerprint.hookMethod {
        val getAudioTrackIdMethod = getAudioTrackIdFingerprint.toMethod()
        val getAudioTrackDisplayNameMethod = getAudioTrackDisplayNameFingerprint.toMethod()
        after {
            it.result = ForceOriginalAudioPatch.isDefaultAudioStream(
                it.result as Boolean,
                getAudioTrackIdMethod(it.thisObject) as String?,
                getAudioTrackDisplayNameMethod(it.thisObject) as String?
            )
        }
    }

    executeBlock()
}