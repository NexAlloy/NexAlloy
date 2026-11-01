package io.github.nexalloy.morphe.shared.misc.litho.relayout

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.methodCall

internal const val COMPONENT_TEXT_CONTENT = "Lcom/facebook/litho/TextContent;"
internal const val COMPONENT_HOST_CLASS = "Lcom/facebook/litho/ComponentHost;"
internal const val COMPONENT_TREE_CLASS = "Lcom/facebook/litho/ComponentTree;"

/**
 * Measures the Litho view. The force layout flag is read and cleared,
 * and passed to the component tree that calculates the layout again if set.
 */
@StrictQuery
internal object LithoViewOnMeasureFingerprint : Fingerprint(
    name = "onMeasure",
    returnType = "V",
    parameters = listOf("I", "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "this", type = "Z"),
        fieldAccess(
            opcode = Opcode.IPUT_BOOLEAN,
            definingClass = "this",
            type = "Z",
            location = MatchAfterImmediately()
        ),
        methodCall(
            definingClass = COMPONENT_TREE_CLASS,
            returnType = "V",
            parameters = listOf("I", "I", "[I", "Z"),
            location = MatchAfterWithin(20)
        )
    ),
    custom = { declaredClass { superClass { descriptor = COMPONENT_HOST_CLASS } } }
)

/**
 * Unmounts all mounted content of the Litho view. The content is mounted again by the next layout.
 */
@StrictQuery
internal object LithoViewUnmountAllItemsFingerprint : Fingerprint(
    classFingerprint = LithoViewOnMeasureFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = List(3) {
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, smali = "Landroid/graphics/Rect;->setEmpty()V")
    }
)

/**
 * Mounts a Litho text. The text layout and the text are set on the text drawable,
 * which is a drawable that implements TextContent.
 */
@StrictQuery
internal object LithoTextMountFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;", "L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/text/Layout;"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Ljava/lang/CharSequence;",
            location = MatchAfterWithin(3)
        )
    )
)

val lithoForceLayoutField = findFieldDirect {
    val matches = LithoViewOnMeasureFingerprint.instructionMatches
    val read = matches[0].instruction.fieldRef!!
    check(read == matches[1].instruction.fieldRef)
    read
}

val lithoMountedTextField = findFieldDirect {
    LithoTextMountFingerprint.instructionMatches.last().instruction.fieldRef!!
}
