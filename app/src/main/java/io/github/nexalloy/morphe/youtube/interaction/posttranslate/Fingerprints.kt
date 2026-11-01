/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4163
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.posttranslate

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.shared.misc.litho.relayout.COMPONENT_HOST_CLASS
import io.github.nexalloy.morphe.shared.misc.litho.relayout.COMPONENT_TEXT_CONTENT

/**
 * Collects the texts of the contents mounted in the host. The contents are taken from the
 * mount items of the host, with a shortcut for a single mount item that is matched here.
 */
internal object ComponentHostGetTextContentFingerprint : Fingerprint(
    definingClass = COMPONENT_HOST_CLASS,
    name = "getTextContent",
    returnType = COMPONENT_TEXT_CONTENT,
    parameters = listOf(),
    filters = listOf(
        // Creates the mount items if they do not exist yet.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "this",
            returnType = "V",
            parameters = listOf()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchAfterImmediately()
        ),
        // Size of the mount items.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "I",
            parameters = listOf(),
            location = MatchAfterImmediately()
        ),
        // Mount item at an index.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "Ljava/lang/Object;",
            parameters = listOf("I"),
            location = MatchAfterWithin(6)
        ),
        // Content of the mount item.
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/Object;",
            location = MatchAfterWithin(4)
        ),
        methodCall(
            smali = "Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;",
            location = MatchAfterWithin(3)
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = COMPONENT_TEXT_CONTENT,
            parameters = listOf("Ljava/util/List;")
        )
    )
)

/**
 * The text of the Litho text drawable.
 */
internal object LithoTextDrawableGetTextItemsFingerprint : Fingerprint(
    superclass = "Landroid/graphics/drawable/Drawable;",
    interfaces = listOf(COMPONENT_TEXT_CONTENT),
    name = "getTextItems",
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/CharSequence;"
        ),
        methodCall(
            smali = "Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;",
            location = MatchAfterWithin(3)
        )
    )
)

/**
 * Draws the text layout of the Litho text drawable.
 */
internal object LithoTextDrawableDrawFingerprint : Fingerprint(
    classFingerprint = LithoTextDrawableGetTextItemsFingerprint,
    name = "draw",
    returnType = "V",
    parameters = listOf("Landroid/graphics/Canvas;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/text/Layout;"
        )
    )
)

internal val postEnsureMountItems = io.github.nexalloy.morphe.findMethodDirect {
    ComponentHostGetTextContentFingerprint.instructionMatches[0].instruction.methodRef!!
}
internal val postMountItemsField = io.github.nexalloy.morphe.findFieldDirect {
    ComponentHostGetTextContentFingerprint.instructionMatches[1].instruction.fieldRef!!
}
internal val postMountItemsSize = io.github.nexalloy.morphe.findMethodDirect {
    ComponentHostGetTextContentFingerprint.instructionMatches[2].instruction.methodRef!!
}
internal val postMountItemAt = io.github.nexalloy.morphe.findMethodDirect {
    ComponentHostGetTextContentFingerprint.instructionMatches[3].instruction.methodRef!!
}
internal val postMountedContentField = io.github.nexalloy.morphe.findFieldDirect {
    ComponentHostGetTextContentFingerprint.instructionMatches[4].instruction.fieldRef!!
}
internal val postDrawableTextField = io.github.nexalloy.morphe.findFieldDirect {
    LithoTextDrawableGetTextItemsFingerprint.instructionMatches.first().instruction.fieldRef!!
}
internal val postDrawableLayoutField = io.github.nexalloy.morphe.findFieldDirect {
    LithoTextDrawableDrawFingerprint.instructionMatches.first().instruction.fieldRef!!
}
