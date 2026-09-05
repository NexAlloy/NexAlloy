package io.github.nexalloy.morphe.youtube.video.information

import app.morphe.extension.shared.Logger
import app.morphe.extension.youtube.patches.VideoInformation
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.bindProxy
import io.github.nexalloy.createProxy
import io.github.nexalloy.findFirstFieldByExactType
import io.github.nexalloy.getStaticObjectField
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.textComponentPatch
import io.github.nexalloy.morphe.youtube.shared.InitializePlaybackSpeedValuesFingerprint
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoQualityClass
import io.github.nexalloy.morphe.youtube.video.playerresponse.Hook
import io.github.nexalloy.morphe.youtube.video.playerresponse.PlayerResponseMethodHook
import io.github.nexalloy.morphe.youtube.video.playerresponse.addPlayerResponseMethodHook
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.morphe.youtube.video.videoid.videoIdHooks
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 * Hook the player controller.  Called when a video is opened or the current video is changed.
 *
 * Note: This hook is called very early and is called before the video id, video time, video length,
 * and many other data fields are set.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
val onCreateHook = mutableListOf<(VideoInformation.PlaybackController) -> Unit>()
val videoTimeHooks = mutableListOf<(Long) -> Unit>()

/*
 * Hook when the video speed is changed for any reason _except when the user manually selects a new speed_.
 * */
val videoSpeedChangedHook = mutableListOf<(Float) -> Unit>()

/**
 * Hook the video speed selected by the user.
 */
val userSelectedPlaybackSpeedHook = mutableListOf<(Float) -> Unit>()

lateinit var setPlaybackSpeedMethod: Method

fun onUserSelectedPlaybackSpeed(speed: Float) {
    userSelectedPlaybackSpeedHook.forEach { it(speed) }
}

class PlaybackController(
    obj: Any,
    private val seekTo: Method,
    private val seekToRelative: Method,
    private val getVideoTime: Method,
    val seekSourceNone: Any
) : VideoInformation.PlaybackController {
    init {
        obj.bindProxy(this)
    }

    val obj = WeakReference(obj)

    override fun patch_seekTo(videoTime: Long): Boolean {
        return seekTo(obj.get(), videoTime, seekSourceNone) as Boolean
    }

    override fun patch_seekToRelative(videoTimeOffset: Long) {
        seekToRelative(obj.get(), videoTimeOffset, seekSourceNone)
    }

    override fun patch_getVideoTime(): Long {
        return getVideoTime(obj.get()) as Long
    }
}

val playerControllerFieldName = "playerController"

class PlaybackSpeedMenu(
    menu: Any
) : VideoInformation.PlaybackSpeedMenuInterface {
    val controller = WeakReference(XposedHelpers.getAdditionalInstanceField(menu, playerControllerFieldName))

    init {
        menu.bindProxy(this)
    }

    override fun patch_setSpeed(speed: Float) {
        setPlaybackSpeedMethod(controller.get(), speed)
    }
}

val VideoInformationPatch = patch(
    description = "Hooks YouTube to get information about the current playing video.",
) {
    dependsOn(
        VideoId,
        PlayerResponseMethodHook,
        conversionContextPatch,
        textComponentPatch,
    )

    //region playerController
    PlayerInitFingerprint.apply {
        val seekSourceType = ::seekSourceType.clazz
        val seekSourceNone = seekSourceType.getStaticObjectField("a")!!
        hookMethod {
            val seekFingerprint = SeekFingerprint.method
            val seekRelativeFingerprint = SeekRelativeFingerprint.method
            val getVideoTime = ::getVideoTime.method

            after { param ->
                val playerController = PlaybackController(
                    param.thisObject,
                    seekFingerprint,
                    seekRelativeFingerprint,
                    getVideoTime,
                    seekSourceNone,
                )
                onCreateHook.forEach { it(playerController) }
            }
        }
    }

    //endregion

    //region mdxPlayerDirector
    MdxPlayerDirectorSetVideoStageFingerprint.apply {
        val seekSourceType = ::mdxSeekSourceType.clazz
        val seekSourceNone = seekSourceType.getStaticObjectField("a")!!
        hookMethod {
            val mdxSeekFingerprint = MdxSeekFingerprint.method
            val mdxSeekRelativeFingerprint = MdxSeekRelativeFingerprint.method
            val getVideoTime = ::mdxGetVideoTime.method

            after { param ->
                val playerController = PlaybackController(
                    param.thisObject,
                    mdxSeekFingerprint,
                    mdxSeekRelativeFingerprint,
                    getVideoTime,
                    seekSourceNone
                )
                VideoInformation.initializeMDX(playerController)
            }
        }
    }
    //endregion

    VideoLengthFingerprint.hookMethod {
        val videoLengthField = ::videoLengthField.field
        val videoLengthHolderField = ::videoLengthHolderField.field

        after { param ->
            val videoLength = param.thisObject
                .let { videoLengthHolderField.get(it) }
                .let { videoLengthField.getLong(it) }
            VideoInformation.setVideoLength(videoLength)
        }
    }

    /*
     * Inject call for video ids
     */
    videoIdHooks.add { VideoInformation.setVideoId(it) }
    // rvxp: currently this is only used for ReloadVideoButtonPatch
//    hookPlayerResponsePlaylistId(VideoInformation::setPlayerResponsePlaylistId)
    hookPlayerResponseVideoId(VideoInformation::setPlayerResponseVideoId)

    // Call before any other video id hooks,
    // so they can use VideoInformation and check if the video id is for a Short.
    addPlayerResponseMethodHook(
        Hook.ProtoBufferParameterBeforeVideoId(
            VideoInformation::newPlayerResponseSignature
        )
    )

    /*
     * Set the video time method
     */
    ::timeMethod.hookMethod {
        before { param ->
            val videoTime = param.args[0] as Long
            videoTimeHooks.forEach { it(videoTime) }
        }
    }

    // region Hook the user playback speed selection.

    // SetPlaybackSpeedFormattedStringFingerprint
    // formattedSpeedStringInsertMethodRef
    // extension custom change

    SpeedLimiterFingerprint.hookMethod {
        before { param ->
            videoSpeedChangedHook.forEach { it(param.args[0] as Float) }
        }
    }

    setPlaybackSpeedMethod = ::setPlaybackSpeedMethodReference.method

    ::setPlaybackSpeedMethodReference.hookMethod {
        before { param ->
            // Hook when the video speed is changed for any reason _except when the user manually selects a new speed_.
            videoSpeedChangedHook.forEach { it(param.args[0] as Float) }
        }
    }

    // legacySpeedSelection
    PlaybackSpeedOnItemClickFingerprint.hookMethod(scopedHook(::setPlaybackSpeedMethodReference.member) {
        before { param ->
            // Hook the video speed selected by the user.
            val speed = param.args[0] as Float
            Logger.printDebug { "onPlaybackSpeedItemClickFingerprint: ${speed}" }
            onUserSelectedPlaybackSpeed(speed)
        }
    })

    InitializePlaybackSpeedValuesFingerprint.declaredClass.constructors[0].hookMethod {
        val playerControllerClass = ::PlayerControllerClass.clazz
        after {
            val c = it.args.first { it.javaClass == playerControllerClass }
            XposedHelpers.setAdditionalInstanceField(
                it.thisObject,
                playerControllerFieldName,
                c
            )
            VideoInformation.setPlaybackSpeedMenu(PlaybackSpeedMenu(it.thisObject))
        }
    }

    // endregion.

    // region Handle new playback speed menu.
    ::playbackSpeedMenuSpeedChangedFingerprint.hookMethod(scopedHook(::setPlaybackSpeedMethodReference.member) {
        before { param ->
            val speed = param.args[0] as Float
            Logger.printDebug { "Playback speed menu speed changed: ${speed}" }
            onUserSelectedPlaybackSpeed(speed)
        }
    })

    hookSpannableString(
        VideoInformation::onNativePlaybackSpeedPanelLoaded
    )

    // endregion.

    // videoQuality
    val videoQualityClass = ::VideoQualityClass.clazz
    val qualityNameField = videoQualityClass.findFirstFieldByExactType(String::class.java)
    val resolutionField = videoQualityClass.findFirstFieldByExactType(Int::class.java)

    val getQualityName = { quality: Any -> qualityNameField.get(quality) as String }
    val getResolution = { quality: Any -> resolutionField.get(quality) as Int }

    // Fix bad data used by YouTube.
    XposedBridge.hookAllConstructors(
        videoQualityClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val quality = param.thisObject
                val newResolution = VideoInformation.fixVideoQualityResolution(
                    getQualityName(quality), getResolution(quality)
                )
                resolutionField.set(quality, newResolution)
            }
        })

    // Detect video quality changes and override the current quality.
    class VideoQualityProxy(val quality: Any) : VideoInformation.VideoQualityInterface {
        override fun patch_getQualityName(): String = getQualityName(quality)
        override fun patch_getResolution(): Int = getResolution(quality)
        override fun toString(): String = quality.toString()
        override fun equals(other: Any?): Boolean =
            other is VideoQualityProxy && quality == other.quality

        override fun hashCode(): Int = quality.hashCode()
    }

    // Detect video quality changes and override the current quality.
    ::videoQualitySetterFingerprint.hookMethod {
        val onItemClickListenerClass = ::onItemClickListenerClassReference.field
        val setQualityField = ::setQualityFieldReference.field
        val setQualityMenuIndexMethod = ::setQualityMenuIndexMethod.method

        @Suppress("UNCHECKED_CAST") before { param ->
            val qualities =
                (param.args[0] as Array<out Any>).map { VideoQualityProxy(it) }.toTypedArray()

            val originalQualityIndex = param.args[1] as Int
            val menu = param.thisObject.let { onItemClickListenerClass.get(it) }
                .let { setQualityField.get(it) }

            param.args[1] = VideoInformation.setVideoQuality(
                qualities,
                { proxy -> setQualityMenuIndexMethod(menu, (proxy as VideoQualityProxy).quality) },
                originalQualityIndex
            )
        }
    }

    // TODO ChannelInformationFingerprint


    // region ExoPlayerImpl.

    val exoPlayerClass =
        classLoader.loadClass(::playbackParametersSetterFingerprint.dexMethod.className)

    val setPlaybackParametersMethod = ::playbackParametersSetterFingerprint.method

    val playbackParametersClass =
        classLoader.loadClass(::playbackParametersSetterFingerprint.dexMethod.paramTypeNames[0])

    val floatFields = playbackParametersClass.declaredFields.filter { it.type == Float::class.java }
    val speedField = floatFields[0]
    val pitchField = floatFields[1]
    ::playbackParametersSetterFingerprint.hookMethod {
        before {
            val newParam = playbackParametersClass.new(
                speedField.get(it.args[0]),
                VideoInformation.getPlaybackAudioPitch()
            )
            it.args[0] = newParam
        }
    }

    exoPlayerClass.constructors.single().hookMethod {
        before {
            VideoInformation.initializeExoPlayerImpl(
                it.thisObject.createProxy { impl ->
                    VideoInformation.ExoPlayerImpl { speed, pitch ->
                        setPlaybackParametersMethod(
                            impl.get(),
                            playbackParametersClass.new(speed, pitch)
                        )
                    }
                }
            )
        }
    }

    // endregion

    onCreateHook.add { VideoInformation.initialize(it) }
    videoSpeedChangedHook.add { VideoInformation.videoSpeedChanged(it) }
    userSelectedPlaybackSpeedHook.add { VideoInformation.userSelectedPlaybackSpeed(it) }

    // TODO Addon
}
