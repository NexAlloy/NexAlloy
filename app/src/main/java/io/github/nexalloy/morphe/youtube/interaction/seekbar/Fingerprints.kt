/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.interaction.seekbar

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.shared.FormatStreamModelToStringFingerprint
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.SeekbarFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoStreamingDataToStringFingerprint
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher

private object SwipingUpGestureParentFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45379021) // Swipe up fullscreen feature flag
    )
)

internal object ShowSwipingUpGuideFingerprint : Fingerprint(
    classFingerprint = SwipingUpGestureParentFingerprint,
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1)
    ),
    custom = { accessFlags(AccessFlags.FINAL.modifier, MatchType.Equals) }
)

internal object AllowSwipingUpGestureFingerprint : Fingerprint(
    classFingerprint = SwipingUpGestureParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    custom = { protoShorty("VL") }
)

// NexAlloy: Use the upstream call target on 21.40+; older versions require the boxed value producer below.
@RequireAppVersion(minVersion = "21.40.000")
internal object DisableFastForwardGestureFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    ),
    custom = {
        opCodesMatcher!!.size(31..Int.MAX_VALUE)
    }
)

// NexAlloy: Hook the boxed value producer instead of Boolean.booleanValue(), which the hook itself calls.
@RequireAppVersion(maxVersion = "21.40.000")
internal object DisableFastForwardGestureFingerprintLegacy : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(definingClass = "this", opcode = Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            parameters = listOf(),
            returnType = "Ljava/lang/Object;",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        checkCast("Ljava/lang/Boolean;", location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall("Ljava/lang/Boolean;->booleanValue()Z", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
    custom = {
        opCodes(OpCodesMatcher().size(31..Int.MAX_VALUE))
    }
)

internal object OnTouchEventHandlerFingerprint : Fingerprint(
    name = "onTouchEvent",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL), // nMethodReference
        // Skip over a return (21.41 and lower) or goto (21.42+).
        opcode(Opcode.IGET_OBJECT, location = MatchAfterWithin(1)),
        opcode(Opcode.IGET_BOOLEAN, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.INT_TO_FLOAT, location = MatchAfterWithin(1)),
        opcode(Opcode.INT_TO_FLOAT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()), // oMethodReference
    )
)

@StrictQuery
internal object TapToSeekFingerprint : Fingerprint(
    name = "onTouchEvent",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
    filters = listOf(
        literal(Int.MAX_VALUE),

        newInstance("Landroid/graphics/Point;"),
        methodCall(
            smali = "Landroid/graphics/Point;-><init>(II)V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            smali = "Lj$/util/Optional;->of(Ljava/lang/Object;)Lj$/util/Optional;",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Lj$/util/Optional;",
            location = MatchAfterImmediately()
        ),

        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterWithin(10))
    )
)

@StrictQuery
internal object SlideToSeekFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "F"),
    filters = listOf(
        methodCall(
            returnType = "Z",
            parameters = listOf(),
            location = MatchAfterWithin(10) // Match close to start of method.
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        literal(67108864)
    )
)

@StrictQuery
internal object VideoStreamingDataAllowSeekingFingerprint : Fingerprint(
    classFingerprint = VideoStreamingDataToStringFingerprint,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(8),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        // Another method in the same class almost matches this fingerprint but uses literal(0) here.
        literal(1, location = MatchAfterImmediately()),
    )
)

// DVR window duration in seconds; 0 for non-DVR streams.
// Caller multiplies result by 1e6 with 4-hour fallback when <= 0, logs "windowMaxMediaTimeUs".
@StrictQuery
internal object FormatStreamModelMaxDVRDurationFingerprint : Fingerprint(
    classFingerprint = FormatStreamModelToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "D",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "D", location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_WIDE, location = MatchAfterImmediately()),
    )
)

val seekbarHandlerOnTouchMethod = findMethodDirect {
    SeekbarFingerprint().declaredClass!!.findMethod {
        matcher { name = "onTouchEvent" }
    }.single()
}

@StrictQuery
internal object SeekbarUpdatePointFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            definingClass = "this",
            type = "Landroid/graphics/Point;"
        ),
        methodCall( // Get seekbar point.
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("Landroid/graphics/Point;"),
            returnType = "V",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/graphics/Rect;",
            location = MatchAfterWithin(10)
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            smali = "Landroid/graphics/Rect;->left:I",
            location = MatchAfterWithin(5)
        )
    )
)

internal object SlideSeekbarHandlerOnTouchFingerprint : Fingerprint (
    classFingerprint = Fingerprint (
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
        filters = listOf(
            resourceLiteral(ResourceType.DIMEN, "seek_easy_horizontal_touch_offset_to_start_scrubbing")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;")
)

@StrictQuery
internal object SlideSeekbarGetViewControllerFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "F"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(10) // Match close to start of method.
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterWithin(10)
        ),
        literal(124587, location = MatchAfterWithin(20)),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterWithin(10)
        ),
        literal(67108864)
    )
)

internal object SeekbarFineScrubbingBitmapFingerprint : Fingerprint (
    classFingerprint = Fingerprint (
        returnType = "Landroid/graphics/Bitmap;",
        parameters = listOf("L", "I", "Landroid/graphics/Bitmap;"),
        filters = listOf(
            string("Storyboard regionDecoder.decodeRegion exception - ")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Bitmap;")
)

@RequireAppVersion(minVersion = "21.12.000")
@StrictQuery
internal object SeekbarBigBoardsUpdateFingerprint : Fingerprint (
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "Ljava/lang/String;",
        parameters = listOf(),
        filters = listOf(
            string("player_overlay_big_boards")
        )
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1),
        opcode(opcode = Opcode.SGET_OBJECT),
        opcode(opcode = Opcode.IF_EQ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.IF_EQ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately())
    )
)

@RequireAppVersion(maxVersion = "21.12.000")
@StrictQuery
internal object SeekbarBigBoardsUpdateLegacyFingerprint : Fingerprint (
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf("Z"),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                smali = $$"Landroid/widget/ImageView$ScaleType;->CENTER_CROP:Landroid/widget/ImageView$ScaleType;"
            ),
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                smali = $$"Landroid/widget/ImageView$ScaleType;->FIT_CENTER:Landroid/widget/ImageView$ScaleType;"
            )
        )
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1),
        opcode(opcode = Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.RETURN, location = MatchAfterImmediately())
    )
)

internal object PreciseSeekingRecyclerViewFingerprint : Fingerprint (
    classFingerprint = Fingerprint(
        accessFlags = listOf(
            AccessFlags.PUBLIC,
            AccessFlags.FINAL,
            AccessFlags.BRIDGE,
            AccessFlags.SYNTHETIC
        ),
        filters = listOf(
            resourceLiteral(ResourceType.LAYOUT, "film_strip_thumbnail_item")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/support/v7/widget/RecyclerView;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/support/v7/widget/RecyclerView;"),
        opcode(opcode = Opcode.RETURN_VOID, location = MatchAfterImmediately())
    )
)

val tapSeekStartMethod = findMethodDirect {
    OnTouchEventHandlerFingerprint.instructionMatches.first().instruction.methodRef!!
}

val tapSeekUpdateMethod = findMethodDirect {
    OnTouchEventHandlerFingerprint.instructionMatches.last().instruction.methodRef!!
}

val tapSeekTouchDownMethod = findMethodDirect {
    TapToSeekFingerprint.instructionMatches.last().instruction.methodRef!!
}

val tapSeekPointField = findFieldDirect {
    TapToSeekFingerprint.instructionMatches[5].instruction.fieldRef!!
}

val slideToSeekCheckMethod = findMethodDirect {
    SlideToSeekFingerprint.instructionMatches.first().instruction.methodRef!!
}

@get:RequireAppVersion(minVersion = "21.40.000")
val fastForwardGestureCheckMethod = findMethodDirect {
    DisableFastForwardGestureFingerprint.instructionMatches[1].instruction.methodRef!!
}

// NexAlloy: Separate targets for the legacy getter and its receiver fields.
@get:RequireAppVersion(maxVersion = "21.40.000")
val fastForwardGestureCheckMethodLegacy = findMethodDirect {
    DisableFastForwardGestureFingerprintLegacy.instructionMatches[2].instruction.methodRef!!
}

@get:RequireAppVersion(maxVersion = "21.40.000")
val fastForwardGestureControllerFieldLegacy = findFieldDirect {
    DisableFastForwardGestureFingerprintLegacy.instructionMatches[0].instruction.fieldRef!!
}

@get:RequireAppVersion(maxVersion = "21.40.000")
val fastForwardGestureValueFieldLegacy = findFieldDirect {
    DisableFastForwardGestureFingerprintLegacy.instructionMatches[1].instruction.fieldRef!!
}

val slideToSeekCheckCallers = findMethodListDirect {
    val checkMethod = slideToSeekCheckMethod()
    SlideToSeekFingerprint().declaredClass!!.methods.filter { method ->
        method.invokes.any { it.descriptor == checkMethod.descriptor }
    }.also { require(it.isNotEmpty()) }
}

@get:AllowAbstract
val updateSeekbarPointMethod = findMethodDirect {
    SeekbarUpdatePointFingerprint.instructionMatches[1].instruction.methodRef!!
}

val slideSeekbarControllerField = findFieldDirect {
    SlideSeekbarGetViewControllerFingerprint.instructionMatches[0].instruction.fieldRef!!
}

val slideSeekbarModelField = findFieldDirect {
    SlideSeekbarGetViewControllerFingerprint.instructionMatches[1].instruction.fieldRef!!
}

val slideSeekbarViewField = findFieldDirect {
    SlideSeekbarGetViewControllerFingerprint.instructionMatches[3].instruction.fieldRef!!
}

internal object SlideSeekbarHandlerConstructorFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.DIMEN, "seek_easy_horizontal_touch_offset_to_start_scrubbing")
    )
)

internal object StoryboardFrameRegionFingerprint : Fingerprint (
    returnType = "Landroid/graphics/Bitmap;",
    parameters = listOf("L", "I", "Landroid/graphics/Bitmap;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("I"),
            returnType = "Landroid/graphics/Rect;"
        ),
        opcode(opcode = Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            smali = $$"Landroid/graphics/BitmapRegionDecoder;->decodeRegion(Landroid/graphics/Rect;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;"
        ),
        string("Storyboard regionDecoder.decodeRegion exception - ")
    )
)

internal object SeekbarBigBoardsScaleTypeFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            smali = $$"Landroid/widget/ImageView$ScaleType;->CENTER_CROP:Landroid/widget/ImageView$ScaleType;"
        ),
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            smali = $$"Landroid/widget/ImageView$ScaleType;->FIT_CENTER:Landroid/widget/ImageView$ScaleType;"
        )
    )
)

internal object SeekbarBigBoardsScrubFingerprint : Fingerprint (
    classFingerprint = SeekbarBigBoardsScaleTypeFingerprint,
    returnType = "V",
    parameters = listOf("I", "J"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            definingClass = "this",
            parameters = listOf("J"),
            returnType = "V"
        )
    )
)

val storyboardFrameRectMethod = findMethodDirect {
    StoryboardFrameRegionFingerprint.instructionMatches.first().instruction.methodRef!!
}

val bigBoardImageField = findFieldDirect {
    SeekbarBigBoardsScrubFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == "Landroid/widget/ImageView;"
    }
}

val bigBoardVisibilityMethod = findMethodDirect {
    SeekbarBigBoardsScrubFingerprint().invokes.filter {
        it.paramTypes.map { type -> type.descriptor } == listOf("Landroid/view/View;", "Z") &&
                it.returnType!!.descriptor == "V"
    }.distinct().single()
}
