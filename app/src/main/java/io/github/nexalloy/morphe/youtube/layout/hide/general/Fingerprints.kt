package io.github.nexalloy.morphe.youtube.layout.hide.general

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.video.speed.custom.TapAndHoldSpeedFingerprint

internal object ParseElementFromBufferFingerprint : Fingerprint(
    parameters = listOf("L", "L", "[B", "L", "L"),
    filters = listOf(
        string("Failed to parse Element", StringComparisonType.STARTS_WITH),
    ),
)

private object PlayerOverlayFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    strings = listOf("player_overlay_in_video_programming"),
)

/** Sets the visibility of the channel watermark from a boolean field of the overlay. */
internal object ShowWatermarkFingerprint : Fingerprint(
    classFingerprint = PlayerOverlayFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/View;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "this"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/view/View;", "Z"),
            returnType = "V",
            location = MatchAfterImmediately()
        )
    )
)

internal val showWatermarkSubFingerprint = findMethodDirect {
    ShowWatermarkFingerprint.instructionMatches[1].instruction.methodRef!!
}

@SkipTest
internal object MoreCommentsIconFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "more_comments_icon"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, name = "findViewById"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

val moreCommentsIconMethods = findMethodListDirect {
    MoreCommentsIconFingerprint.matchAll().map { it.method }
}

@SkipTest
internal object BottomSheetMenuItemBuilderFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Ljava/lang/CharSequence;",
            parameters = listOf("L")
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        string("Text missing for BottomSheetMenuItem.")
    )
)

val bottomSheetMenuItemBuilders = findMethodListDirect {
    BottomSheetMenuItemBuilderFingerprint.matchAll().map { it.method }
}

val bottomSheetMenuItemTextFingerprint = findMethodDirect {
    BottomSheetMenuItemBuilderFingerprint.matchAll().map {
        it.instructionMatches[0].instruction.methodRef!!
    }.distinct().single()
}

internal object ContextualMenuItemBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.SYNTHETIC),
    returnType = "V",
    parameters = listOf("L", "L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Ljava/lang/CharSequence;",
            parameters = listOf("L")
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/view/View;",
            location = MatchAfterImmediately()
        ),
        checkCast("Landroid/widget/TextView;", location = MatchAfterImmediately()),
        methodCall(
            smali = "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "I",
            location = MatchAfterWithin(7),
        ),
        resourceLiteral(ResourceType.DIMEN, "poster_art_width_default"),
    )
)

val contextualMenuItemTextViewField = findFieldDirect {
    ContextualMenuItemBuilderFingerprint.instructionMatches[2].instruction.fieldRef!!
}

val contextualMenuItemTextFingerprint = findMethodDirect {
    ContextualMenuItemBuilderFingerprint.instructionMatches[0].instruction.methodRef!!
}

internal object ContextualMenuItemBuilderOnClickFingerprint : Fingerprint(
    classFingerprint = ContextualMenuItemBuilderFingerprint,
    name = "onClick",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/Object;"
        ),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

val contextualMenuItemMessageField = findFieldDirect {
    ContextualMenuItemBuilderOnClickFingerprint.instructionMatches.first().instruction.fieldRef!!
}

/**
 * A row of an app bar menu.
 */
internal object ListMenuItemViewOnMeasureFingerprint : Fingerprint(
    definingClass = "Landroid/support/v7/view/menu/ListMenuItemView;",
    name = "onMeasure",
    returnType = "V",
    parameters = listOf("I", "I")
)

val tapAndHoldCanStartMethod = findMethodDirect {
    TapAndHoldSpeedFingerprint.instructionMatches[2].instruction.methodRef!!
}

val tapAndHoldRunnableOwnerField = findFieldDirect {
    val match = TapAndHoldSpeedFingerprint.match()
    val runnableType = match.method.declaredClass!!.descriptor
    match.method.instructions
        .take(match.instructionMatches[0].index)
        .last {
            it.opcode == Opcode.IGET_OBJECT.ordinal &&
                    it.fieldRef!!.declaredClass!!.descriptor == runnableType
        }.fieldRef!!
}

val tapAndHoldAlreadySpeedingUpField = findFieldDirect {
    TapAndHoldSpeedFingerprint.instructionMatches[4].instruction.fieldRef!!
}

val tapAndHoldEndMethod = findMethodDirect {
    val speedControllerType = TapAndHoldSpeedFingerprint.instructionMatches[6]
        .instruction.fieldRef!!.type!!.descriptor
    Fingerprint(
        definingClass = speedControllerType,
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf()
    )()
}

internal object ShowControlsOnRelatedPanelDragFingerprint : Fingerprint(
    classFingerprint = object : Fingerprint(
        returnType = "V",
        filters = listOf(
            resourceLiteral(ResourceType.ID, "scrim_overlay"),
            checkCast("Landroid/widget/ImageView;", location = MatchAfterWithin(10))
        )
    ) {},
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("F"),
    filters = listOf(
        opcode(Opcode.IF_GEZ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "this",
            parameters = listOf(),
            returnType = "V"
        ),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately())
    )
)

internal object EntityMutationPayloadFingerprint : Fingerprint(
    returnType = "[B",
    filters = listOf(string("There can be only one field in EntityMutationPayload"))
)

val entityMutationBytesMethod = findMethodDirect {
    val method = EntityMutationPayloadFingerprint()
    method.instructions.mapNotNull { it.methodRef }.distinct().single {
        it.declaredClass!!.descriptor == method.paramTypes.single().descriptor &&
            it.returnType!!.descriptor == "[B" && it.paramTypes.isEmpty()
    }
}
