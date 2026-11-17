package io.github.nexalloy.morphe.youtube.interaction.seekbar

import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import app.morphe.extension.youtube.patches.TapToSeekPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val enableTapToSeekPatch = patch(
    description = "Adds an option to enable tap to seek on the seekbar of the video player.",
) {
    PreferenceScreen.SEEKBAR.addPreferences(
        SwitchPreference("morphe_tap_to_seek", summary = true),
    )

    val isScrubbing = ::timeBarIsScrubbingMethod.method
    val completeScrub = ::timeBarCompleteScrubMethod.method
    TimeBarCompleteHiddenScrubFingerprint.hookMethod {
        before {
            if (TapToSeekPatch.shouldCompleteScrub(it.thisObject as View, it.args[0] as MotionEvent)
                && isScrubbing.invoke(it.thisObject) as Boolean) {
                completeScrub.invoke(it.thisObject)
            }
        }
    }

    // Find the required methods to tap the seekbar.
    val startSeek = ::tapSeekStartMethod.method
    val updateSeek = ::tapSeekUpdateMethod.method
    val pointField = ::tapSeekPointField.field
    val getPoint = pointField.type.getMethod("get")

    TapToSeekFingerprint.hookMethod(scopedHook(::tapSeekTouchDownMethod.method) {
        after {
            if (it.hasThrowable() || it.thisObject !== outerParam.thisObject ||
                (outerParam.args[0] as MotionEvent).action != MotionEvent.ACTION_DOWN ||
                !TapToSeekPatch.tapToSeekEnabled()) return@after
            val point = getPoint(pointField.get(outerParam.thisObject)) as Point
            startSeek(outerParam.thisObject, point.x)
            updateSeek(outerParam.thisObject, point.x)
        }
    })
}
