package io.github.nexalloy.morphe.youtube.video.playerresponse

import io.github.nexalloy.patch

@Volatile
private var beforeVideoIdHooks = emptyArray<Hook.ProtoBufferParameterBeforeVideoId>()

@Volatile
private var videoIdHooks = emptyArray<Hook.VideoId>()

@Volatile
private var playlistIdHooks = emptyArray<Hook.PlaylistId>()

@Volatile
private var afterVideoIdHooks = emptyArray<Hook.ProtoBufferParameter>()

private val hookRegistrationLock = Any()

fun addPlayerResponseMethodHook(hook: Hook<*>) {
    synchronized(hookRegistrationLock) {
        when (hook) {
            is Hook.ProtoBufferParameterBeforeVideoId -> {
                if (beforeVideoIdHooks.none { it === hook }) {
                    beforeVideoIdHooks = beforeVideoIdHooks + hook
                }
            }

            is Hook.VideoId -> {
                if (videoIdHooks.none { it === hook }) {
                    videoIdHooks = videoIdHooks + hook
                }
            }

            is Hook.PlaylistId -> {
                if (playlistIdHooks.none { it === hook }) {
                    playlistIdHooks = playlistIdHooks + hook
                }
            }

            is Hook.ProtoBufferParameter -> {
                if (afterVideoIdHooks.none { it === hook }) {
                    afterVideoIdHooks = afterVideoIdHooks + hook
                }
            }
        }
    }
}

val PlayerResponseMethodHook = patch {
    val PARAMETER_VIDEO_ID = 0
    val PARAMETER_PROTO_BUFFER = 2
    val PARAMETER_PLAYLIST_ID = 3
    val parameterIsShortAndOpeningOrPlaying =
        ::playerParameterBuilderFingerprint.dexMethod.paramTypeNames
            .withIndex()
            .indexOfFirst { (index, type) -> index >= 10 && type == "boolean" }

    require(parameterIsShortAndOpeningOrPlaying >= 0) {
        "Player parameter builder is missing the Shorts state boolean parameter"
    }

    ::playerParameterBuilderFingerprint.hookMethod {
        before { param ->
            val videoId = param.args[PARAMETER_VIDEO_ID] as String
            var protobuf = param.args[PARAMETER_PROTO_BUFFER] as String
            val playlistId = param.args[PARAMETER_PLAYLIST_ID] as String?
            val isShortAndOpeningOrPlaying =
                param.args[parameterIsShortAndOpeningOrPlaying] as Boolean

            val beforeHooks = beforeVideoIdHooks
            val currentVideoIdHooks = videoIdHooks
            val currentPlaylistIdHooks = playlistIdHooks
            val afterHooks = afterVideoIdHooks

            for (hook in beforeHooks) {
                protobuf = hook(protobuf, videoId, isShortAndOpeningOrPlaying)
            }
            for (hook in currentVideoIdHooks) {
                hook(videoId, isShortAndOpeningOrPlaying)
            }
            for (hook in currentPlaylistIdHooks) {
                hook(playlistId, isShortAndOpeningOrPlaying)
            }
            for (hook in afterHooks) {
                protobuf = hook(protobuf, videoId, isShortAndOpeningOrPlaying)
            }
            param.args[PARAMETER_PROTO_BUFFER] = protobuf
        }
    }
}

sealed class Hook<T> {
    class PlaylistId(val hook: (String?, Boolean) -> Unit) : Hook<(String?, Boolean) -> Unit>() {
        operator fun invoke(p1: String?, p2: Boolean) = hook(p1, p2)
    }

    class VideoId(val hook: (String, Boolean) -> Unit) : Hook<(String, Boolean) -> Unit>() {
        operator fun invoke(p1: String, p2: Boolean) = hook(p1, p2)
    }

    class ProtoBufferParameter(val hook: (String, String, Boolean) -> String) :
        Hook<(String, String, Boolean) -> String>() {
        operator fun invoke(p1: String, p2: String, p3: Boolean): String = hook(p1, p2, p3)
    }

    class ProtoBufferParameterBeforeVideoId(val hook: (String, String, Boolean) -> String) :
        Hook<(String, String, Boolean) -> String>() {
        operator fun invoke(p1: String, p2: String, p3: Boolean): String = hook(p1, p2, p3)
    }
}
