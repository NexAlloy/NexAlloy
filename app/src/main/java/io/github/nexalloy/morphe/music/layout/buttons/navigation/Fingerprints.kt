package io.github.nexalloy.morphe.music.layout.buttons.navigation

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string

internal object TabLayoutTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        anyInstruction(
            string("FEmusic_search"), // 8.49 and lower.
            string("FEsearch"), // 8.50+
            newInstance("Ljava/util/ArrayList;") // 9.21+
        ),
        resourceLiteral(ResourceType.ID, "text1"),
        methodCall(
            smali = "Landroid/view/View;->findViewById(I)Landroid/view/View;",
            location = MatchAfterWithin(5)
        ),
        checkCast(
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(5)
        ),
        anyInstruction(
            opcode(Opcode.SGET_OBJECT),
            opcode(Opcode.IGET_OBJECT)
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            type = "I",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "L",
            parameters = listOf("I"),
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        // NexAlloy: obtain the attached tab view through the addTab argument.
        methodCall(
            definingClass = "Lcom/google/android/material/tabs/TabLayout;",
            returnType = "V",
            parameters = listOf("L", "I", "Z")
        ),
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        methodCall(name = "getVisibility", location = MatchAfterWithin(2))
    )
)

internal val navigationEnumMethod = findMethodDirect {
    TabLayoutTextFingerprint.instructionMatches[6].instruction.methodRef!!
}

internal val navigationAddTabMethod = findMethodDirect {
    TabLayoutTextFingerprint.instructionMatches[8].instruction.methodRef!!
}

internal val navigationTabViewField = findFieldDirect {
    TabLayoutTextFingerprint.instructionMatches[9].instruction.fieldRef!!
}
