package io.github.nexalloy.morphe.shared.misc.audio.tracks

import app.morphe.extension.shared.patches.ForceOriginalAudioPatch
import app.morphe.extension.shared.settings.preference.ForceOriginalAudioSwitchPreference
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch

internal fun forceOriginalAudioPatch(
    block: PatchExecutor.() -> Unit = {},
    executeBlock: PatchExecutor.() -> Unit = {},
    fixUseLocalizedAudioTrackFlag: PatchExecutor.() -> Boolean,
    forcedServerAdaptiveStreaming: PatchExecutor.() -> Boolean,
    mainActivityOnCreateFingerprint: Fingerprint,
    subclassExtensionClassDescriptor: String,
    preferenceScreen: BasePreferenceScreen.Screen
) = patch(
    name = "Force original audio",
    description = "Adds an option to always use the original audio track.",
) {
    block()

    preferenceScreen.addPreferences(
        SwitchPreference(
            key = "morphe_force_original_audio",
            tag = ForceOriginalAudioSwitchPreference::class.java,
            summary = true
        )
    )

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

    val subclassExtensionClassName = subclassExtensionClassDescriptor
        .removePrefix("L")
        .removeSuffix(";")
        .replace('/', '.')
    val setEnabledMethod = classLoader
        .loadClass(subclassExtensionClassName)
        .getDeclaredMethod("setEnabled")
    mainActivityOnCreateFingerprint.hookMethod {
        before {
            setEnabledMethod.invoke(null)
        }
    }

    if (fixUseLocalizedAudioTrackFlag()) {
        insertLiteralOverride(
            AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG,
            ForceOriginalAudioPatch::ignoreDefaultAudioStream
        )
    }

    if (forcedServerAdaptiveStreaming()) {
        val idField = ::audioTrackIdField.field.apply { isAccessible = true }
        val displayNameField = ::audioTrackDisplayNameField.field.apply { isAccessible = true }
        val isDefaultField = ::audioTrackIsDefaultField.field.apply { isAccessible = true }
        val trackArrayField = ::audioTrackRecordArrayField.field.apply { isAccessible = true }
        val setAudioTrack = ::setAudioTrackMethod.method.apply { isAccessible = true }
        val setVideoQualityList = ::setVideoQualityListMethod.method
        val playerControllerField = setVideoQualityList.declaringClass.declaredFields
            .filter { it.type == setAudioTrack.declaringClass }
            .single()
            .apply { isAccessible = true }

        class AudioTrackProxy(private val record: Any) : ForceOriginalAudioPatch.AudioTrackInterface {
            override fun patch_getDisplayName(): String = displayNameField.get(record) as String

            override fun patch_getId(): String = idField.get(record) as String

            override fun patch_getIsDefault(): Boolean = isDefaultField.getBoolean(record)

            override fun equals(other: Any?): Boolean =
                other is AudioTrackProxy && record == other.record

            override fun hashCode(): Int = record.hashCode()
        }

        ::setVideoQualityListMethod.hookMethod {
            before { param ->
                val audioVideoFormat = param.args.firstOrNull() ?: return@before
                val rawTracks = trackArrayField.get(audioVideoFormat) as? Array<*> ?: return@before
                if (rawTracks.isEmpty() || rawTracks.any { it == null }) return@before

                val tracks = Array<ForceOriginalAudioPatch.AudioTrackInterface>(rawTracks.size) { index ->
                    AudioTrackProxy(rawTracks[index]!!)
                }
                val trackId = ForceOriginalAudioPatch.getDefaultAudioTrackId(tracks) ?: return@before
                val playerController = playerControllerField.get(param.thisObject) ?: return@before
                setAudioTrack.invoke(playerController, trackId)
            }
        }
    }

    executeBlock()
}
