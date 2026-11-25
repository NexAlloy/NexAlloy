package io.github.nexalloy.morphe.youtube.video.quality

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldFromToString
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.opcodes
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.shared.FormatStreamModelToStringFingerprint
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.PlaybackStartDescriptorToStringFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoQualityClass
import io.github.nexalloy.morphe.youtube.video.information.VideoQualitySetterFingerprint
import io.github.nexalloy.morphe.youtube.video.information.setQualityFieldReference
import io.github.nexalloy.morphe.youtube.video.videoid.ForegroundVideoIdFingerprint
import org.luckypray.dexkit.DexKitBridge

internal const val CLIENT_ABR_STATE_CLASS =
    "Lcom/google/android/apps/youtube/proto/ClientAbrStateOuterClass\$ClientAbrState;"

internal const val FIXED_RESOLUTION_STRING = ", initialPlaybackVideoQualityFixedResolution="

/**
 * 'Video quality preferences' setting of YouTube settings
 * (Auto, Higher picture quality, Data saver or Advanced).
 */
internal object VideoQualitySettingEnumFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("VIDEO_QUALITY_SETTING_ADVANCED_MENU"),
        fieldAccess( // VIDEO_QUALITY_SETTING_ADVANCED_MENU enum value.
            opcode = Opcode.SPUT_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(5)
        )
    )
)

val advancedVideoQualitySettingField = findFieldDirect {
    VideoQualitySettingEnumFingerprint.instructionMatches.last().instruction.fieldRef!!
}

/**
 * Video quality setting used for a video ID, sent with the SABR requests.
 */
internal object GetVideoQualitySettingFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = { VideoQualitySettingEnumFingerprint().declaredClass!!.descriptor },
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/Map;", name = "get"),
        checkCast(type = { VideoQualitySettingEnumFingerprint().declaredClass!!.descriptor })
    )
)

/**
 * Chooses the qualities allowed by the player, when the video starts and again while it plays:
 * the quality last selected in the quality menu, or an automatic quality.
 */
internal object GetVideoQualityConstraintFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "L", "Ljava/lang/String;", "L"),
    filters = listOf(
        methodCall(returnType = { LastSelectedVideoQualityConstructorFingerprint().declaredClass!!.descriptor }),
        methodCall(reference = { GetVideoQualitySettingFingerprint() })
    )
)

val lastSelectedVideoQualityGetter = findMethodDirect {
    GetVideoQualityConstraintFingerprint.instructionMatches.first().instruction.methodRef!!
}

val lastSelectedVideoQualityFields = findFieldListDirect {
    val matches = LastSelectedVideoQualityConstructorFingerprint.instructionMatches
    listOf(0, 1, 3, 4, 5).map { matches[it].instruction.fieldRef!! }
}

internal object PlaybackStartParametersToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string(FIXED_RESOLUTION_STRING)
    )
)

val InitialResolutionField = findFieldDirect {
    PlaybackStartParametersToStringFingerprint().findFieldFromToString(FIXED_RESOLUTION_STRING)
}

internal object PlaybackStartParametersInitFingerprint : Fingerprint(
    classFingerprint = PlaybackStartParametersToStringFingerprint,
    name = "<init>",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            reference = { InitialResolutionField() }
        )
    )
)

private object VideoQualityItemOnClickParentFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
    )
)

/**
 * Resolves to class found in [VideoQualityItemOnClickFingerprint].
 */
internal object VideoQualityItemOnClickFingerprint : Fingerprint(
    classFingerprint = VideoQualityItemOnClickParentFingerprint,
    name = "onItemClick",
    returnType = "V",
    parameters = listOf(
        "Landroid/widget/AdapterView;",
        "Landroid/view/View;",
        "I",
        "J"
    )
)

val videoQualityBottomSheetListFragmentTitle
    get() = resourceMappings[
        "layout",
        "video_quality_bottom_sheet_list_fragment_title",
    ]

val videoQualityMenuViewInflateFingerprint = findMethodListDirect {
    // two matches in versions 20.43.32
    // one match in versions <=v20.42.xx
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("L")
            parameters("L", "L", "L")
            opcodes(
                Opcode.INVOKE_SUPER,
                Opcode.CONST,
                Opcode.CONST_4,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CONST,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CONST_16,
                Opcode.INVOKE_VIRTUAL,
                Opcode.CONST,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CHECK_CAST,
            )
            literal { videoQualityBottomSheetListFragmentTitle }
        }
    }
}

val ShowVideoQualityQuickMenuFingerprint = findMethodListDirect {
    val matcher = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        strings = listOf("VIDEO_QUALITIES_QUICK_MENU_BOTTOM_SHEET_FRAGMENT"),
        filters = listOf(
            opcode(Opcode.MOVE_RESULT),
            opcode(
                opcode = Opcode.IF_NEZ,
                location = MatchAfterWithin(3)
            ),
            methodCall(
                opcode = Opcode.INVOKE_VIRTUAL,
                name = "getSupportFragmentManager",
                location = MatchAfterWithin(3)
            ),
            methodCall(
                opcode = Opcode.INVOKE_VIRTUAL,
                parameters = listOf("L", "Ljava/lang/String;"),
                returnType = "V",
                location = MatchAfterWithin(5)
            )
        )
    ).buildMethodMatcher()
    findMethod { matcher(matcher) }
}

internal object ShortsQualityMenuFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    returnType = "V",
    filters = listOf(
        resourceLiteral(
            type = ResourceType.STRING,
            name = "video_quality_unavailable_announcement"
        )
    )
)

internal object ShortsQualityConstructorFingerprint : Fingerprint(
    classFingerprint = ShortsQualityMenuFingerprint,
    name = "<init>",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this"
        )
    )
)

internal object ClientAbrStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = CLIENT_ABR_STATE_CLASS,
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_ABR_STATE_CLASS,
            type = "Lcom/google/android/apps/youtube/proto/streaming/MediaCapabilitiesOuterClass\$MediaCapabilities;"
        )
    )
)

internal object LastSelectedVideoQualityConstructorFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "I", "L"),
    filters = listOf(
        fieldAccess( // Time the quality was selected.
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J"
        ),
        fieldAccess( // Video ID the quality was selected for.
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        string("last_playback_start_timestamp"),
        fieldAccess( // Time the last playback started.
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J"
        ),
        fieldAccess( // Resolution of the quality.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I"
        ),
        fieldAccess( // Direction of the quality change.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I"
        )
    )
)

/**
 * If this flag is off, the qualities of the quality menu have no formats, and the player ignores
 * the formats of the quality last selected and finds the quality only by its resolution.
 */
internal object QualityFormatsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45400072)
    )
)

/**
 * The method of the player that sets the video formats to play, and clears the buffered video
 * if the parameter is true.
 */
@RequireAppVersion("21.17.000")
@StrictQuery
internal object PlaybackControllerSetVideoFormatsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackController;"
        ),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        // The parameter, if the buffered video is cleared.
        opcode(Opcode.IF_EQZ, location = MatchAfterWithin(3)),
        opcode(Opcode.SGET_OBJECT, location = MatchAfterImmediately()) // Video track type.
    )
)

/**
 * The method of the async media fetch controller that sets the video formats to play.
 * If the flag is on, the method sets the formats itself instead of calling
 * [PlaybackControllerSetVideoFormatsFingerprint], and clears the buffered video
 * if the boolean parameter is true.
 */
@RequireAppVersion("21.17.000")
internal object AsyncMediaFetchControllerSetVideoFormatsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Z"),
    filters = listOf(
        literal(45779716)
    )
)

internal object SetVideoQualityResolutionFingerprint : Fingerprint(
    definingClass = { setQualityFieldReference().typeSign },
    returnType = "V",
    parameters = listOf("I")
)

internal val videoQualityItemSetResolutionMethods = findMethodListDirect {
    Fingerprint(
        name = "onItemClick",
        returnType = "V",
        parameters = listOf("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J"),
        filters = listOf(methodCall(reference = SetVideoQualityResolutionFingerprint()))
    ).matchAll().map { it.method }
}

internal const val BUFFER_MANAGER_CLASS =
    "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/mediapush/BufferManagerImpl;"

/**
 * The method of the player that clears the buffered media of a track, and buffers it again
 * from the time the video started or was last seeked to.
 */
@RequireAppVersion("21.17.000", "21.46.000")
internal object PlaybackControllerClearBufferFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/PlaybackControllerWrapper;",
    returnType = "V",
    filters = listOf(
        // Stops the media requests.
        methodCall(definingClass = "this", returnType = "V", parameters = listOf()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = BUFFER_MANAGER_CLASS
        ),
        string("setStartTimeUs with disposed BufferManager")
    )
)

internal const val MEDIA_SEGMENT_HOLDER_CLASS =
    "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/mediapush/MediaSegmentHolder;"

/**
 * The call to the method of the buffered media of a track that clears it after the segment
 * that contains a time. Before 21.44 it is called from a method of the buffer manager,
 * and 21.44+ inlines that method into the player.
 */
@RequireAppVersion("21.17.000", "21.46.000")
internal object MediaSegmentHolderDiscardBufferAfterSegmentFingerprint : Fingerprint(
    filters = listOf(
        string("discardBufferAfterCurrentSegment with disposed BufferManager"),
        methodCall(
            definingClass = MEDIA_SEGMENT_HOLDER_CLASS,
            returnType = "V",
            parameters = listOf("J")
        )
    )
)

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val mediaSegmentHolderDiscardBufferAfterSegmentMethod = findMethodDirect {
    MediaSegmentHolderDiscardBufferAfterSegmentFingerprint.instructionMatches.last().instruction.methodRef!!
}

internal const val SAMPLE_HOLDER_CLASS =
    "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/mediapush/SampleHolder;"

/**
 * The method of the buffer manager that returns the buffered media of a track.
 */
@RequireAppVersion("21.17.000", "21.46.000")
internal object BufferManagerGetMediaSegmentHolderFingerprint : Fingerprint(
    definingClass = BUFFER_MANAGER_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = MEDIA_SEGMENT_HOLDER_CLASS,
    parameters = listOf("L")
)

/**
 * 21.17 - 21.42
 *
 * The method of the buffered media of an audio or video track that clears
 * the media before the segment that contains a time.
 */
@RequireAppVersion("21.17.000", "21.43.000")
internal object SampleHolderDiscardBufferBeforeSegmentLegacyFingerprint : Fingerprint(
    definingClass = SAMPLE_HOLDER_CLASS,
    returnType = "V",
    parameters = listOf("J"),
    filters = listOf(
        methodCall(smali = "Ljava/lang/Math;->min(JJ)J"),
        methodCall(smali = "Ljava/util/List;->subList(II)Ljava/util/List;"),
        methodCall(smali = "Ljava/util/List;->clear()V")
    )
)

/**
 * 21.43+
 *
 * Same as [SampleHolderDiscardBufferBeforeSegmentLegacyFingerprint],
 * with the parameters: time, playback time and size of the media kept before the playback time.
 */
@RequireAppVersion("21.43.000")
@StrictQuery
internal object SampleHolderDiscardBufferBeforeSegmentFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("J", "J", "J"),
    filters = listOf(
        methodCall(definingClass = "this", returnType = "Z", parameters = listOf("J", "J", "J")),
        methodCall(definingClass = "this", returnType = "V", parameters = listOf("J", "J", "J"))
    )
)

/**
 * The method of the buffered media of a track that saves the end time of its first segment.
 * The media before a time is cleared only if the time is after it.
 */
@RequireAppVersion("21.17.000", "21.46.000")
internal object MediaSegmentHolderSetFirstSegmentEndTimeFingerprint : Fingerprint(
    definingClass = MEDIA_SEGMENT_HOLDER_CLASS,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(smali = "Ljava/util/List;->isEmpty()Z"),
        methodCall(
            definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/mediapush/BufferedMediaSegment;",
            returnType = "J",
            parameters = listOf()
        ),
        fieldAccess(opcode = Opcode.IPUT_WIDE, definingClass = "this", type = "J")
    )
)

/**
 * The method of the media source that clears the media buffered before the playback time,
 * called by the player while the video is loaded.
 */
@RequireAppVersion("21.17.000", "21.46.000")
internal object MediaPushMediaSourceDiscardBufferFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/mediapush/MediaPushMediaSource;",
    returnType = "V",
    parameters = listOf("J"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET_WIDE,
            definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/live/LiveTimeline;"
        ),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = BUFFER_MANAGER_CLASS),
        methodCall(smali = "Ljava/lang/Math;->max(JJ)J")
    )
)


internal object PlaybackStartInitialResolutionFingerprint : Fingerprint(
    returnType = "Ljava/lang/Integer;",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, reference = { InitialResolutionField() }),
        methodCall(smali = "Lj$/util/Optional;->orElse(Ljava/lang/Object;)Ljava/lang/Object;"),
        checkCast("Ljava/lang/Integer;")
    )
)

context(dexkit: DexKitBridge)
private fun playerPlaybackStartDescriptorFingerprint() = Fingerprint(
    definingClass = PlaybackStartInitialResolutionFingerprint().declaredClass!!.descriptor,
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        methodCall(
            returnType = PlaybackStartDescriptorToStringFingerprint().declaredClass!!.descriptor,
            parameters = listOf(),
            location = MatchAfterImmediately()
        )
    )
)

internal val playbackStartPlayerField = findFieldDirect {
    playerPlaybackStartDescriptorFingerprint().instructionMatches[0].instruction.fieldRef!!
}

@get:AllowAbstract
internal val playerPlaybackStartDescriptorMethod = findMethodDirect {
    playerPlaybackStartDescriptorFingerprint().instructionMatches[1].instruction.methodRef!!
}

internal val videoQualityMenuQualitiesField = findFieldDirect {
    Fingerprint(
        classFingerprint = VideoQualitySetterFingerprint,
        returnType = "V",
        parameters = listOf("[L", "I", "Z"),
        strings = listOf("menu_item_video_quality"),
        filters = listOf(fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = VideoQualityItemOnClickFingerprint().declaredClass!!.descriptor,
            type = "[" + VideoQualityClass().descriptor
        ))
    ).instructionMatches.first().instruction.fieldRef!!
}

internal object CurrentAudioVideoFormatToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("currentVideoFormat=")
)

internal object CurrentVideoFormatConstructorFingerprint : Fingerprint(
    classFingerprint = CurrentAudioVideoFormatToStringFingerprint,
    name = "<init>",
    returnType = "V",
    filters = listOf(fieldAccess(
        opcode = Opcode.IPUT_OBJECT,
        definingClass = "this",
        type = { "[" + VideoQualityClass().descriptor }
    ))
)

internal val currentVideoFormatField = findFieldDirect {
    val type = FormatStreamModelToStringFingerprint().declaredClass!!.descriptor
    Fingerprint(
        definingClass = CurrentVideoFormatConstructorFingerprint().declaredClass!!.descriptor,
        name = "<init>",
        filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, type = type))
    ).instructionMatches.first().instruction.fieldRef!!
}

@get:RequireAppVersion("21.17.000")
internal val videoTrackTypeField = findFieldDirect {
    PlaybackControllerSetVideoFormatsFingerprint.instructionMatches.last().instruction.fieldRef!!
}

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val stopMediaRequestsMethod = findMethodDirect {
    PlaybackControllerClearBufferFingerprint.instructionMatches[0].instruction.methodRef!!
}

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val playbackBufferManagerField = findFieldDirect {
    PlaybackControllerClearBufferFingerprint.instructionMatches[1].instruction.fieldRef!!
}

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val firstSegmentEndTimeField = findFieldDirect {
    MediaSegmentHolderSetFirstSegmentEndTimeFingerprint.instructionMatches.last().instruction.fieldRef!!
}

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val mediaSourceBufferManagerField = findFieldDirect {
    MediaPushMediaSourceDiscardBufferFingerprint.instructionMatches[1].instruction.fieldRef!!
}

@get:RequireAppVersion("21.17.000", "21.46.000")
internal val mediaSourcePrepareDiscardMethod = findMethodDirect {
    val match = MediaPushMediaSourceDiscardBufferFingerprint.match()
    match.method.instructions.take(match.instructionMatches[1].index).last {
        it.methodRef?.declaredClass == match.method.declaredClass &&
            it.methodRef?.paramTypes?.map { p -> p.descriptor } == listOf("J") &&
            it.methodRef?.returnType?.descriptor == "V"
    }.methodRef!!
}

internal fun getVideoQualitySettingChangedEventFingerprint(
    videoQualitySettingType: String
) = Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(videoQualitySettingType, "Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = videoQualitySettingType
        ),
        literal(-2)
    )
)

internal fun getQualityMenuSelectedQualityFingerprint(
    videoQualityEventType: String,
    videoQualitySettingType: String
) = Fingerprint(
    filters = listOf(
        methodCall(smali = "Lj$/util/Optional;->orElse(Ljava/lang/Object;)Ljava/lang/Object;"),
        fieldAccess( // Resolution of the quality event.
            opcode = Opcode.IGET,
            definingClass = videoQualityEventType,
            type = "I",
            location = MatchAfterWithin(8)
        ),
        methodCall(
            definingClass = "Lj$/util/stream/Stream;",
            name = "filter",
            location = MatchAfterWithin(8)
        ),
        methodCall(
            smali = "Lj$/util/Optional;->orElse(Ljava/lang/Object;)Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        fieldAccess( // Video quality setting of the quality event.
            opcode = Opcode.IGET_OBJECT,
            definingClass = videoQualityEventType,
            type = videoQualitySettingType
        )
    )
)

internal fun getVideoQualityConstraintEqualsFingerprint(constraintType: String) = object : Fingerprint(
    definingClass = constraintType,
    name = "equals",
    filters = listOf(
        fieldAccess( // Quality range.
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this"
        )
    )
) {}

internal object VideoQualityRangeConstructorFingerprint : Fingerprint(
    definingClass = { qualityConstraintRangeField().type.descriptor },
    name = "<init>",
    parameters = listOf("I", "I", "I", "L"),
    filters = listOf(
        fieldAccess( // Highest quality.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I"
        ),
        fieldAccess( // Lowest quality.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I",
            location = MatchAfterImmediately()
        ),
        fieldAccess( // Mode.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I",
            location = MatchAfterImmediately()
        ),
        fieldAccess( // Formats.
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            location = MatchAfterImmediately()
        )
    )
)

internal fun getVideoQualityConstraintConstructorFingerprint(
    constraintType: String,
    rangeType: String
) = object : Fingerprint(
    definingClass = constraintType,
    name = "<init>",
    parameters = listOf(rangeType, rangeType, "Z", "Ljava/lang/String;", "I", "I", "J", "I", "I", "L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J"
        ),
        fieldAccess( // Last field.
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this"
        )
    )
) {}

internal object FixedVideoQualityConstraintFingerprint : Fingerprint(
    definingClass = { GetVideoQualityConstraintFingerprint().returnType!!.descriptor },
    returnType = { GetVideoQualityConstraintFingerprint().returnType!!.descriptor },
    parameters = { listOf("I") }
)

internal val qualityConstraintRangeField = findFieldDirect {
    getVideoQualityConstraintEqualsFingerprint(GetVideoQualityConstraintFingerprint().returnType!!.descriptor)
        .instructionMatches.first().instruction.fieldRef!!
}
internal val qualityRangeFields = findFieldListDirect {
    VideoQualityRangeConstructorFingerprint.instructionMatches.map { it.instruction.fieldRef!! }
}
internal val qualityConstraintLastField = findFieldDirect {
    getVideoQualityConstraintConstructorFingerprint(
        GetVideoQualityConstraintFingerprint().returnType!!.descriptor, qualityConstraintRangeField().type.descriptor
    ).instructionMatches.last().instruction.fieldRef!!
}
internal val sabrFormatSelectionMethod = findMethodDirect {
    val constraint = GetVideoQualityConstraintFingerprint().returnType!!.descriptor
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
        filters = listOf(methodCall(definingClass = constraint, returnType = "Z", parameters = listOf("I")))
    ).matchAll().map { it.method }.filter { method ->
        val types = method.paramTypes.map { it.descriptor }
        val index = types.indexOf("Lcom/google/android/apps/youtube/proto/streaming/SelectableFormatsOuterClass\$SelectableFormats;")
        types.firstOrNull() == constraint && index >= 2 &&
                types.getOrNull(index - 2) == "Ljava/util/List;" &&
                types.getOrNull(index - 1) == "Ljava/util/List;" &&
                types.getOrNull(index + 1) == "Lcom/google/android/apps/youtube/proto/streaming/MediaCapabilitiesOuterClass\$MediaCapabilities;"
    }.single()
}
internal val playingVideoCpnField = findFieldDirect {
    val method = ForegroundVideoIdFingerprint()
    Fingerprint(
        definingClass = method.declaredClass!!.descriptor,
        name = method.name,
        parameters = method.paramTypes.map { it.descriptor },
        filters = listOf(
            fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = method.paramTypes.first().descriptor, type = "Ljava/lang/String;"),
            fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "Ljava/lang/String;", location = MatchAfterImmediately())
        )
    ).instructionMatches.last().instruction.fieldRef!!
}
