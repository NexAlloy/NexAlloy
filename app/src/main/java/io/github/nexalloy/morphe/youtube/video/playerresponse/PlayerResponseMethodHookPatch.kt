package io.github.nexalloy.morphe.youtube.video.playerresponse

import io.github.nexalloy.patch

private val hooks = mutableSetOf<Hook<*>>()

fun addPlayerResponseMethodHook(hook: Hook<*>) {
    hooks += hook
}

val PlayerResponseMethodHook = patch {
    val PARAMETER_VIDEO_ID = 0
    val PARAMETER_PROTO_BUFFER = 2
    val PARAMETER_PLAYLIST_ID = 3
    var parameterIsShortAndOpeningOrPlaying = -1
    ::playerParameterBuilderFingerprint.dexMethod.apply {
        parameterIsShortAndOpeningOrPlaying =
            paramTypeNames.zip(paramTypeNames.indices)
                .indexOfFirst { (type, i) -> i >= 10 && type == "boolean" }
    }.hookMethod {
        before { param ->
            val videoId = param.args[PARAMETER_VIDEO_ID] as String
            var protobuf = param.args[PARAMETER_PROTO_BUFFER] as String
            var playlistId = param.args[PARAMETER_PLAYLIST_ID] as String?
            val isShortAndOpeningOrPlaying =
                param.args[parameterIsShortAndOpeningOrPlaying] as Boolean

            // Reverse the order in order to preserve insertion order of the hooks.
            val beforeVideoIdHooks =
                hooks.filterIsInstance<Hook.ProtoBufferParameterBeforeVideoId>().asReversed()
            val playlistIdHooks = hooks.filterIsInstance<Hook.PlaylistId>().asReversed()
            val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
            val afterVideoIdHooks = hooks.filterIsInstance<Hook.ProtoBufferParameter>().asReversed()

            beforeVideoIdHooks.forEach {
                protobuf = it(protobuf, videoId, isShortAndOpeningOrPlaying)
            }
            playlistIdHooks.forEach {
                it(playlistId, isShortAndOpeningOrPlaying)
            }
            videoIdHooks.forEach {
                it(videoId, isShortAndOpeningOrPlaying)
            }
            afterVideoIdHooks.forEach {
                protobuf = it(protobuf, videoId, isShortAndOpeningOrPlaying)
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
