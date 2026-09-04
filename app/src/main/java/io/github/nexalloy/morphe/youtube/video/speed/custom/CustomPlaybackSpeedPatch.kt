package io.github.nexalloy.morphe.youtube.video.speed.custom

import app.morphe.extension.youtube.patches.components.PlaybackSpeedMenuFilter
import app.morphe.extension.youtube.patches.playback.speed.CustomPlaybackSpeedPatch
import app.morphe.extension.youtube.patches.playback.speed.CustomPlaybackSpeedPatch.customPlaybackSpeeds
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.invokeOriginalMethod
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_34_or_greater
import io.github.nexalloy.morphe.youtube.misc.recyclerviewtree.addRecyclerViewTreeHook
import io.github.nexalloy.morphe.youtube.misc.recyclerviewtree.recyclerViewTreeHook
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterFingerprint
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterParentFingerprint
import io.github.nexalloy.morphe.youtube.video.speed.settingsMenuVideoSpeedGroup
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val CustomPlaybackSpeed = patch(
    description = "Adds custom playback speed options.",
) {
    dependsOn(
        LithoFilter, recyclerViewTreeHook
    )

    settingsMenuVideoSpeedGroup.addAll(
        listOf(
            SwitchPreference("morphe_custom_speed_menu"),
            SwitchPreference("morphe_enable_playback_audio_pitch_controls"),
            SwitchPreference("morphe_playback_audio_time_stretching", summary = true),
            TextPreference(
                "morphe_custom_playback_speeds",
                inputType = InputType.TEXT_MULTI_LINE
            ),
        )
    )

    setOf(
        SpeedLimiterFingerprint,
        SpeedLimiterParentFingerprint
    ).forEach { fingerprint ->
        fingerprint.hookMethod(scopedHook(::clampFloatFingerprint.member) {
            before {
                if (it.args[1] == 0.25f && it.args[2] == 4.0f) {
                    it.args[1] = 0.0f
                    it.args[2] = 8.0f
                }
            }
        })
    }

    if (is_20_34_or_greater) {
        ServerSideMaxSpeedFeatureFlagFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }

    ::speedArrayGeneratorFingerprint.hookMethod {
        val source = ::speedsFloatArrayField.field.get(null) as FloatArray
        val chunkSize = source.size
        require(chunkSize > 0)

        before {
            synchronized(source) {
                val originalSource = source.copyOf()
                try {
                    val result = customPlaybackSpeeds.asIterable().chunked(chunkSize).flatMap { chunk ->
                        chunk.forEachIndexed { index, value -> source[index] = value }
                        val generated = it.invokeOriginalMethod() as Array<*>
                        require(generated.size >= chunk.size)
                        generated.take(chunk.size)
                    }

                    val first = requireNotNull(result.firstOrNull())
                    val arr = java.lang.reflect.Array.newInstance(first.javaClass, result.size)
                    result.forEachIndexed { index, value ->
                        java.lang.reflect.Array.set(arr, index, value)
                    }
                    it.result = arr
                } finally {
                    originalSource.copyInto(source)
                }
            }
        }
    }

    addRecyclerViewTreeHook.add { CustomPlaybackSpeedPatch.onFlyoutMenuCreate(it) }
    addLithoFilter(PlaybackSpeedMenuFilter())
}
