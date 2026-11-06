package io.github.nexalloy.morphe.youtube.video.information

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.shared.misc.videoinformation.seekSourceUnknownField
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.PlaybackSpeedOnItemClickParentFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoQualityChangedFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoQualityClass
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.result.FieldUsingType

@SkipTest
internal object ChannelInformationFingerprint : Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    filters = listOf(string("loadVideo() called on LocalDirector in wrong state"))
)

internal val channelInformationMethods = findMethodListDirect {
    ChannelInformationFingerprint.matchAll(2..3).map { it.method }.also { methods ->
        check(methods.map { it.declaredClass!!.descriptor }.distinct().size == 1)
    }
}

private fun getChannelIdFingerprint(playerResponseType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        // Is live stream.
        methodCall(definingClass = playerResponseType, returnType = "Z"),
        // Channel id. 21.17 places the live stream check far before it.
        methodCall(
            definingClass = playerResponseType,
            returnType = "Ljava/lang/String;"
        ),
        string(
            "com.google.android.apps.youtube.mdx.watch.LAST_MEALBAR_PROMOTED_LIVE_FEED_CHANNELS",
            location = MatchAfterWithin(20)
        )
    )
)

@get:AllowAbstract
internal val playerResponseIsLiveMethod = findMethodDirect {
    val playerResponseType = channelInformationMethods().first().paramTypes.first().descriptor
    getChannelIdFingerprint(playerResponseType).instructionMatches[0].instruction.methodRef!!
}

@get:AllowAbstract
internal val playerResponseChannelIdMethod = findMethodDirect {
    val playerResponseType = channelInformationMethods().first().paramTypes.first().descriptor
    getChannelIdFingerprint(playerResponseType).instructionMatches[1].instruction.methodRef!!
}

@get:AllowAbstract
internal val playerResponseChannelNameMethod = findMethodDirect {
    val playerResponseType = channelInformationMethods().first().paramTypes.first().descriptor
    Fingerprint(
        filters = listOf(
            string("Person"),
            methodCall(
                definingClass = playerResponseType,
                returnType = "Ljava/lang/String;",
                location = MatchAfterWithin(5)
            )
        )
    ).instructionMatches.last().instruction.methodRef!!
}

@get:RequireAppVersion("21.29.000")
@get:AllowAbstract
internal val playerResponseVideoTitleMethod = findMethodDirect {
    val playerResponseType = channelInformationMethods().first().paramTypes.first().descriptor
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
        returnType = "Ljava/lang/String;",
        parameters = listOf(playerResponseType),
        filters = listOf(
            methodCall(
                definingClass = playerResponseType,
                returnType = "Ljava/lang/String;"
            ),
            opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
            opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
            string("", location = MatchAfterImmediately())
        )
    ).instructionMatches.first().instruction.methodRef!!
}

internal object PlaybackSpeedOnItemClickFingerprint : Fingerprint(
    classFingerprint = PlaybackSpeedOnItemClickParentFingerprint,
    name = "onItemClick",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L", "I", "J"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET,
            type = "F"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("F"),
            returnType = "V"
        )
    )
)

val setPlaybackSpeedMethodReference = findMethodDirect {
    PlaybackSpeedOnItemClickFingerprint().invokes.findMethod { matcher { paramTypes("float") } }
        .single()
}

val PlayerControllerClass = findClassDirect { setPlaybackSpeedMethodReference().declaredClass!! }

internal object PlayerControllerSetTimeReferenceFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("J", "J", "J", "J", "J", "J", "J", "Z", "Ljava/lang/String;")
        ),
        string("Media progress reported outside media playback: ", comparison = StringComparisonType.CONTAINS)
    )
)

val timeMethod = findMethodDirect {
    PlayerControllerSetTimeReferenceFingerprint.instructionMatches.first().instruction.methodRef!!
}

internal object PlayerStatusEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "NEW", "PLAYBACK_PENDING", "PLAYBACK_LOADED", "PLAYBACK_INTERRUPTED",
        "INTERSTITIAL_REQUESTED", "INTERSTITIAL_PLAYING", "VIDEO_PLAYING", "ENDED"
    )
)

val playerStatusMethod = findMethodDirect {
    Fingerprint(
        classFingerprint = PlayerInitFingerprint,
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(PlayerStatusEnumFingerprint().declaredClass!!.descriptor),
        filters = listOf(
            opcode(Opcode.SGET_OBJECT),
            methodCall(definingClass = "Lj$/time/Instant;", name = "plus")
        )
    )()
}

internal object PlayerInitFingerprint : Fingerprint(
    name = "<init>",
    custom = {
        declaredClass {
            addEqString("playVideo called on player response with no videoStreamingData.")
        }
    }
)

internal object SeekFingerprint : Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    filters = listOf(
        string("currentPositionMs.")
    )
)

val seekSourceType = findClassDirect {
    SeekFingerprint().paramTypes[1]
}

val seekSourceUnknown = findFieldDirect {
    seekSourceUnknownField(seekSourceType().descriptor)
}

private object CreateVideoPlayerSeekbarFingerprint : Fingerprint(
    name = "onDraw",
    returnType = "V",
    filters = listOf(
        string("timed_markers_width")
    )
)

internal object VideoLengthFingerprint : Fingerprint(
    classFingerprint = CreateVideoPlayerSeekbarFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall("Landroid/graphics/Rect;->set(Landroid/graphics/Rect;)V"),

        methodCall(returnType = "J"),
        methodCall(returnType = "J", location = MatchAfterWithin(5)),
        methodCall(returnType = "J", location = MatchAfterWithin(10)),
        methodCall(returnType = "J", location = MatchAfterWithin(10)),

        methodCall(returnType = "Z", parameters = listOf()),
        opcode(Opcode.CMP_LONG, location = MatchAfterWithin(8))
    )
)

val videoLengthField = findFieldDirect {
    VideoLengthFingerprint().usingFields.single { it.usingType == FieldUsingType.Write && it.field.typeName == "long" }.field
}

val videoLengthHolderField = findFieldDirect {
    val videoLengthField = videoLengthField()
    VideoLengthFingerprint().usingFields.single { it.usingType == FieldUsingType.Read && it.field.typeName == videoLengthField.declaredClassName }.field
}

object MdxSeekFingerprint : Fingerprint(
    classFingerprint = MdxPlayerDirectorSetVideoStageFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = {
        opCodesMatcher!!.matchType = OpCodeMatchType.Equals
    }
)

val mdxSeekSourceType = findClassDirect {
    MdxSeekFingerprint().paramTypes[1]
}

val mdxSeekSourceUnknown = findFieldDirect {
    seekSourceUnknownField(mdxSeekSourceType().descriptor)
}

internal object MdxPlayerDirectorSetVideoStageFingerprint : Fingerprint(
    filters = listOf(
        string("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state "),
    )
)

internal object MdxSeekRelativeFingerprint : Fingerprint(
    classFingerprint = MdxPlayerDirectorSetVideoStageFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    // Return type is boolean up to 19.39, and void with 19.39+.
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
    )
)

internal object SeekRelativeFingerprint : Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    // Return type is boolean up to 19.39, and void with 19.39+.
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.ADD_LONG_2ADDR,
        Opcode.INVOKE_VIRTUAL,
    )
)

internal object GetVideoTimeFingerprint : Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        methodCall(
            // getVideoTime()
            definingClass = "this",
            returnType = "J",
            parameters = listOf(),
        ),
        literal(69, location = MatchAfterWithin(5))
    )
)

val getVideoTime = findMethodDirect {
    GetVideoTimeFingerprint.instructionMatches.first().instruction.methodRef!!
}

val mdxGetVideoTime = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(MdxPlayerDirectorSetVideoStageFingerprint().declaredClassName)
            name(getVideoTime().name)
            returnType("long")
            paramTypes()
        }
    }.single()
}

/**
 * Resolves with the class found in [VideoQualityChangedFingerprint].
 */
@StrictQuery
internal object PlaybackSpeedMenuSpeedChangedFingerprint : Fingerprint(
    classFingerprint = VideoQualityChangedFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        fieldAccess(opcode = Opcode.IGET, type = "F")
    )
)

internal object VideoQualitySetterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("[L", "I", "Z"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_BOOLEAN,
    ),
    strings = listOf("menu_item_video_quality")
)

/**
 * Matches with the class found in [VideoQualitySetterFingerprint].
 */
@StrictQuery
internal object SetVideoQualityFingerprint : Fingerprint(
    classFingerprint = VideoQualitySetterFingerprint,
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchFirst()
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchAfterImmediately()
        ),
    )
)

val onItemClickListenerClassReference = findFieldDirect {
    SetVideoQualityFingerprint.instructionMatches[0].instruction.fieldRef!!
}
val setQualityFieldReference = findFieldDirect {
    SetVideoQualityFingerprint.instructionMatches[1].instruction.fieldRef!!
}
val setQualityMenuIndexMethod = findMethodDirect {
    setQualityFieldReference().type.findMethod {
        matcher { addParamType { descriptor = VideoQualityClass().descriptor } }
    }.single()
}

/**
 * Makes a playback the current one, such as a preloaded Short when it's scrolled to,
 * which is not loaded again with loadVideo().
 */
internal fun currentPlaybackFingerprint(playerResponseType: String) = Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this"
        ),
        // The player response of the playback.
        methodCall(
            returnType = playerResponseType,
            parameters = listOf()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(playerResponseType, "L"),
            location = MatchAfterImmediately()
        )
    )
)

internal val currentPlaybackApplyResponseMethod = findMethodDirect {
    val type = channelInformationMethods().first().paramTypes.first().descriptor
    currentPlaybackFingerprint(type).instructionMatches.last().instruction.methodRef!!
}

internal val currentPlaybackMethod = findMethodDirect {
    val type = channelInformationMethods().first().paramTypes.first().descriptor
    currentPlaybackFingerprint(type).match().method
}

internal val videoQualityFormatIdsField = findFieldDirect {
    val constructor = io.github.nexalloy.morphe.youtube.shared.VideoQualityChangedFingerprint
        .instructionMatches.last().instruction.methodRef!!
    constructor.declaredClass!!.fields.single { it.type == constructor.paramTypes.last() }
}
