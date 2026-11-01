package io.github.nexalloy.morphe.youtube.video.information

import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.patches.ExoPlayerInterface
import app.morphe.extension.youtube.patches.VideoInformation
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.bindProxy
import io.github.nexalloy.createProxy
import io.github.nexalloy.findFirstFieldByExactType
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.textComponentPatch
import io.github.nexalloy.morphe.shared.misc.videoinformation.playbackParametersSetterFingerprint
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_29_or_greater
import io.github.nexalloy.morphe.youtube.shared.InitializePlaybackSpeedValuesFingerprint
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterFingerprint
import io.github.nexalloy.morphe.youtube.video.playerresponse.Hook
import io.github.nexalloy.morphe.youtube.video.playerresponse.PlayerResponseMethodHook
import io.github.nexalloy.morphe.youtube.video.playerresponse.addPlayerResponseMethodHook
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponsePlaylistId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookVideoId
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.lang.ref.WeakReference
import java.lang.reflect.Method

private val onCreateHooks = mutableListOf<(VideoInformation.PlaybackController) -> Unit>()
private val onCreateHooksMDX = mutableListOf<(VideoInformation.PlaybackController) -> Unit>()
private val videoTimeHooks = mutableListOf<(Long) -> Unit>()
private val videoSpeedChangedHooks = mutableListOf<(Float) -> Unit>()
private val userSelectedPlaybackSpeedHooks = mutableListOf<(Float) -> Unit>()

/**
 * Hook the player controller.  Called when a video is opened or the current video is changed.
 *
 * Note: This hook is called very early and is called before the video id, video time, video length,
 * and many other data fields are set.
 *
 * @param hook Receives the newly created player controller.
 */
fun onCreateHook(hook: (controller: VideoInformation.PlaybackController) -> Unit) {
    onCreateHooks.add(hook)
}

/**
 * Hooks the MDX player director when casting to another device.
 */
internal fun onCreateHookMDX(hook: (controller: VideoInformation.PlaybackController) -> Unit) {
    onCreateHooksMDX.add(hook)
}

/**
 * Hooks the video time, usually once per second.
 */
fun videoTimeHook(hook: (videoTime: Long) -> Unit) {
    videoTimeHooks.add(hook)
}

/**
 * Hook when the video speed is changed for any reason _except when the user manually selects a new speed_.
 */
fun videoSpeedChangedHook(hook: (speed: Float) -> Unit) {
    videoSpeedChangedHooks.add(hook)
}

/**
 * Hook the video speed selected by the user.
 */
fun userSelectedPlaybackSpeedHook(hook: (speed: Float) -> Unit) {
    userSelectedPlaybackSpeedHooks.add(hook)
}

/**
 * Skips a player status change when the hook returns true.
 */
internal fun PatchExecutor.playerStatusOverrideHook(hook: (status: Enum<*>) -> Boolean) {
    ::playerStatusMethod.hookMethod {
        before {
            if (hook(it.args[0] as Enum<*>)) it.result = null
        }
    }
}

lateinit var setPlaybackSpeedMethod: Method

fun onUserSelectedPlaybackSpeed(speed: Float) {
    userSelectedPlaybackSpeedHooks.forEach { it(speed) }
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

private lateinit var qualityFormatIds: java.lang.reflect.Field

internal class VideoQualityProxy(val quality: Any) : VideoInformation.VideoQualityInterface {
    private val qualityNameField = quality.javaClass.findFirstFieldByExactType(String::class.java)
    private val resolutionField = quality.javaClass.findFirstFieldByExactType(Int::class.java)

    override fun patch_getQualityName(): String = qualityNameField.get(quality) as String
    override fun patch_getResolution(): Int = VideoInformation.fixVideoQualityResolution(
        patch_getQualityName(), resolutionField.getInt(quality)
    )
    override fun patch_getFormatIds(): Collection<*> =
        qualityFormatIds.get(quality) as Collection<*>
    override fun toString(): String = quality.toString()
    override fun equals(other: Any?): Boolean = other is VideoQualityProxy && quality == other.quality
    override fun hashCode(): Int = quality.hashCode()
}

val VideoInformationPatch = patch(
    description = "Hooks YouTube to get information about the current playing video.",
) {
    dependsOn(
        VideoId,
        PlayerResponseMethodHook,
        conversionContextPatch,
        textComponentPatch,
        VersionCheck,
    )

    qualityFormatIds = ::videoQualityFormatIdsField.field

    //region playerController
    PlayerInitFingerprint.apply {
        val seekSourceNone = ::seekSourceUnknown.field.get(null)!!
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
                onCreateHooks.forEach { it(playerController) }
            }
        }
    }

    //endregion

    //region mdxPlayerDirector
    MdxPlayerDirectorSetVideoStageFingerprint.apply {
        val seekSourceNone = ::mdxSeekSourceUnknown.field.get(null)!!
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
                onCreateHooksMDX.forEach { it(playerController) }
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
    hookVideoId(VideoInformation::setVideoId)
    hookPlayerResponsePlaylistId(VideoInformation::setPlayerResponsePlaylistId)
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
            videoSpeedChangedHooks.forEach { it(param.args[0] as Float) }
        }
    }

    setPlaybackSpeedMethod = ::setPlaybackSpeedMethodReference.method

    ::setPlaybackSpeedMethodReference.hookMethod {
        before { param ->
            // Hook when the video speed is changed for any reason _except when the user manually selects a new speed_.
            videoSpeedChangedHooks.forEach { it(param.args[0] as Float) }
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
    PlaybackSpeedMenuSpeedChangedFingerprint.hookMethod(scopedHook(::setPlaybackSpeedMethodReference.member) {
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
    // Detect video quality changes and override the current quality.
    VideoQualitySetterFingerprint.hookMethod {
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

    // region Set channel information.

    val channelId = ::playerResponseChannelIdMethod.method
    val channelName = ::playerResponseChannelNameMethod.method
    // The helper this resolves through only exists in later targets, and the title is
    // only used by the minimal miniplayer, which is only rebuilt for those.
    val videoTitle = if (is_21_29_or_greater) ::playerResponseVideoTitleMethod.method else null
    val isLive = ::playerResponseIsLiveMethod.method
    fun updateChannelInformation(playerResponse: Any) {
        VideoInformation.setChannelId(channelId.invoke(playerResponse) as String?)
        VideoInformation.setChannelName(channelName.invoke(playerResponse) as String?)
        if (videoTitle != null) VideoInformation.setVideoTitle(videoTitle.invoke(playerResponse) as String?)
        VideoInformation.setLiveStream(isLive.invoke(playerResponse) as Boolean)
    }
    ::channelInformationMethods.dexMethodList.forEach { method ->
        method.hookMethod {
            before { param ->
                updateChannelInformation(param.args[0])
            }
        }
    }

    // A preloaded playback, such as the next Short, becomes the current one without loadVideo().
    ::currentPlaybackMethod.hookMethod(scopedHook(::currentPlaybackApplyResponseMethod.method) {
        before { updateChannelInformation(it.args[0]) }
    })

    // endregion.

    addExoPlayerHooks()

    onCreateHook(VideoInformation::initialize)
    onCreateHookMDX(VideoInformation::initializeMDX)
    videoSpeedChangedHook(VideoInformation::videoSpeedChanged)
    userSelectedPlaybackSpeedHook(VideoInformation::userSelectedPlaybackSpeed)

    // TODO Addon
}

internal fun PatchExecutor.addExoPlayerHooks() {

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
            val speed = speedField.get(it.args[0]) as Float
            val pitch = pitchField.get(it.args[0]) as Float
            val newParam = playbackParametersClass.new(
                VideoInformation.overridePlaybackSpeed(speed),
                VideoInformation.overridePlaybackPitch(speed, pitch)
            )
            it.args[0] = newParam
        }
    }

    exoPlayerClass.constructors.single().hookMethod {
        before {
            VideoInformation.initializeExoPlayer(
                it.thisObject.createProxy { impl ->
                    ExoPlayerInterface { speed, pitch ->
                        setPlaybackParametersMethod(
                            impl.get(),
                            playbackParametersClass.new(speed, pitch)
                        )
                    }
                }
            )
        }
    }

}
