package io.github.nexalloy.morphe.youtube.layout.hide.general

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.string

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

internal object ShowWatermarkFingerprint : Fingerprint(
    classFingerprint = PlayerOverlayFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L"),
)

internal val showWatermarkSubFingerprint = findMethodDirect {
    ShowWatermarkFingerprint.run().invokes.findMethod {
        matcher {
            returnType = "void"
            paramTypes("android.view.View", "boolean")
        }
    }.single()
}

/*
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

val bottomSheetMenuItemTextFingerprint = findMethodDirect {
    BottomSheetMenuItemBuilderFingerprint.instructionMatches[0].instruction.methodRef!!
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
        resourceLiteral(ResourceType.DIMEN, "poster_art_width_default"),
    )
)

val contextualMenuItemTextViewField = findFieldDirect {
    ContextualMenuItemBuilderFingerprint.instructionMatches[2].instruction.fieldRef!!
}

val contextualMenuItemTextFingerprint = findMethodDirect {
    ContextualMenuItemBuilderFingerprint.instructionMatches[0].instruction.methodRef!!
}
*/
