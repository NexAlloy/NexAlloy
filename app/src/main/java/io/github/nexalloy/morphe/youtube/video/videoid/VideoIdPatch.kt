package io.github.nexalloy.morphe.youtube.video.videoid

import app.morphe.extension.shared.Logger
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.morphe.youtube.video.playerresponse.Hook
import io.github.nexalloy.morphe.youtube.video.playerresponse.PlayerResponseMethodHook
import io.github.nexalloy.morphe.youtube.video.playerresponse.addPlayerResponseMethodHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private val videoIdHooks = mutableListOf<(String) -> Unit>()
private val backgroundVideoIdHooks = mutableListOf<(String) -> Unit>()

/** Supports changing regular videos during background playback when no video is visible. */
fun hookBackgroundPlayVideoId(hook: (String) -> Unit) {
    backgroundVideoIdHooks.add(hook)
}

/**
 * Hooks the new video id when the video changes.
 *
 * Supports all videos (regular videos and Shorts).
 */
fun hookVideoId(hook: (videoId: String) -> Unit) {
    videoIdHooks.add(hook)
}

/**
 * Hooks the playlist ID of every video when loaded.
 * Supports all videos and functions in all situations.
 *
 * First parameter is the playlist ID.
 * Second parameter is if the video is a Short AND it is being opened or is currently playing.
 *
 * Hook is always called off the main thread.
 *
 * This hook is called as soon as the player response is parsed,
 * and called before many other hooks are updated such as [playerTypeHookPatch].
 *
 * Note: The playlist ID returned here may not be the current video that's being played.
 * It's common for multiple Shorts to load at once in preparation
 * for the user swiping to the next Short.
 *
 * Be aware, this can be called multiple times for the same playlist ID.
 *
 * @param methodDescriptor which method to call. Params must be `Ljava/lang/String;Z`
 */
fun hookPlayerResponsePlaylistId(hook: (playlistId: String?, isShortAndOpeningOrPlaying: Boolean) -> Unit) = addPlayerResponseMethodHook(
    Hook.PlaylistId(
        hook,
    ),
)

/**
 * Hooks the video ID of every video when loaded.
 * Supports all videos and functions in all situations.
 *
 * First parameter is the video ID.
 * Second parameter is if the video is a Short AND it is being opened or is currently playing.
 *
 * Hook is always called off the main thread.
 *
 * This hook is called as soon as the player response is parsed,
 * and called before many other hooks are updated such as [playerTypeHookPatch].
 *
 * Note: The video ID returned here may not be the current video that's being played.
 * It's common for multiple Shorts to load at once in preparation
 * for the user swiping to the next Short.
 *
 * For most use cases, you probably want to use
 * [hookVideoId] or [hookBackgroundPlayVideoId] instead.
 *
 * Be aware, this can be called multiple times for the same video ID.
 *
 * @param methodDescriptor which method to call. Params must be `Ljava/lang/String;Z`
 */
fun hookPlayerResponseVideoId(method :(videoId: String, isShortAndOpeningOrPlaying: Boolean) -> Unit) = addPlayerResponseMethodHook(
    Hook.VideoId(
        method,
    ),
)

val VideoId = patch(
    description = "Hooks to detect when the video id changes.",
) {
    dependsOn(
        PlayerResponseMethodHook
    )

    ::videoIdFingerprint.hookMethod {
        val videoIdMethod = ::PlayerResponseModel_getVideoId.method
        before { param ->
            val videoId = videoIdMethod(param.args[0]) as String
            Logger.printDebug { "setCurrentVideoId: $videoId" }
            videoIdHooks.forEach { it(videoId) }
        }
    }

    val notifyBackgroundId: HookDsl<IScopedHookCallback>.() -> Unit = {
        after {
            val videoId = it.result as? String ?: return@after
            backgroundVideoIdHooks.forEach { hook -> hook(videoId) }
        }
    }
    ::backgroundVideoIdMethod.hookMethod(scopedHook(*::backgroundVideoIdGetterMethods.dexMethodList.map {
        it.toMethod() to notifyBackgroundId
    }.toTypedArray()))

    // TODO Addon
}
