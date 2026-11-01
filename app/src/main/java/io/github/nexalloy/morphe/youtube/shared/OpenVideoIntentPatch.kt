package io.github.nexalloy.morphe.youtube.shared

import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_20_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private val shortsIntentHooks = mutableListOf<(Map<Any, Any>, String) -> Unit>()
private val videoIntentHooks = mutableListOf<(Map<Any, Any>, String) -> Unit>()

val openVideoIntentPatch = patch(
    description = "Provides hooks into intent components before videos open."
) {
    dependsOn(VersionCheck)
    val getVideoId = ::playbackStartVideoIdMethod.method

    VideoPlaybackIntentFingerprint.hookMethod(scopedHook(::regularVideoPlaybackStartDescriptorGetter.method) {
        after {
            if (innerDepth == 0 && !it.hasThrowable() && it.result != null) {
                @Suppress("UNCHECKED_CAST")
                val map = outerParam.args[1] as Map<Any, Any>
                val videoId = getVideoId.invoke(it.result) as String
                videoIntentHooks.forEach { callback -> callback(map, videoId) }
            }
        }
    })

    (if (is_21_20_or_greater) ShortsPlaybackIntentFingerprint else ShortsPlaybackIntentFingerprintLegacy)
        .hookMethod {
            before {
                @Suppress("UNCHECKED_CAST")
                val map = it.args[1] as Map<Any, Any>
                val videoId = getVideoId.invoke(it.args[0]) as String
                shortsIntentHooks.forEach { callback -> callback(map, videoId) }
            }
        }
}

fun hookVideoIntent(
    detectVideo: Boolean = true,
    detectShorts: Boolean = true,
    callback: (Map<Any, Any>, String) -> Unit
) {
    if (detectVideo) videoIntentHooks += callback
    if (detectShorts) shortsIntentHooks += callback
}
