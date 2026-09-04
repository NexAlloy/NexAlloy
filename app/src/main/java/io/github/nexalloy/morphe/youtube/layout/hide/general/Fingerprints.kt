package io.github.nexalloy.morphe.youtube.layout.hide.general

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string

internal object HideSubscribedChannelsBarConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "parent_container"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            location = MatchAfterWithin(2)
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        newInstance(
            "Landroid/widget/LinearLayout\$LayoutParams;",
            location = MatchAfterWithin(5)
        )
    )
)

val subscribedChannelsFindViewByIdMethod = findMethodDirect {
    HideSubscribedChannelsBarConstructorFingerprint.instructionMatches[1].instruction.methodRef!!
}

internal object HideSubscribedChannelsBarLandscapeFingerprint : Fingerprint(
    classFingerprint = HideSubscribedChannelsBarConstructorFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.DIMEN, "parent_view_width_in_wide_mode"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "getDimensionPixelSize",
            returnType = "I"
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    )
)

val subscribedChannelsGetDimensionPixelSizeMethod = findMethodDirect {
    HideSubscribedChannelsBarLandscapeFingerprint.instructionMatches[1].instruction.methodRef!!
}

internal object LatestVideosContentPillFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Z"),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "content_pill"),
        methodCall(
            smali = "Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

internal object LatestVideosBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Z"),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "bar"),
        methodCall(
            smali = "Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

val latestVideosInflateMethod = findMethodDirect {
    val methods = listOf(
        LatestVideosContentPillFingerprint,
        LatestVideosBarFingerprint
    ).map { fingerprint ->
        fingerprint.instructionMatches[1].instruction.methodRef!!
    }.distinctBy { it.descriptor }
    methods.single()
}

internal object ParseElementFromBufferFingerprint : Fingerprint(
    parameters = listOf("L", "L", "[B", "L", "L"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        opcode(Opcode.INVOKE_INTERFACE, location = MatchAfterWithin(1)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        string("Failed to parse Element", StringComparisonType.STARTS_WITH),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L"),
            returnType = "L"
        ),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterWithin(4))
    ),
)

val parseElementEmptyReturnMethod = findMethodDirect {
    ParseElementFromBufferFingerprint.instructionMatches[4].instruction.methodRef!!
}

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
