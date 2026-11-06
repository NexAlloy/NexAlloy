package io.github.nexalloy.morphe.youtube.layout.startpage

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.shared.misc.debugging.BootstrapStoreReadFingerprint
import io.github.nexalloy.morphe.string

internal object IntentActionFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Intent;"),
    filters = listOf(
        string("has_handled_intent")
    )
)

@StrictQuery
internal object BrowseIdFingerprint : Fingerprint(
    returnType = "L",

    //parameters() // 20.30 and earlier is no parameters = listOf(.  20.31+ parameter is L.),
    filters = listOf(
        string("FEwhat_to_watch"),
        methodCall(
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterWithin(3)
        ),
        literal(512),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/lang/String;")
    )
)

val browserIdProtoBuilder = findMethodDirect {
    BrowseIdFingerprint.instructionMatches[1].instruction.methodRef!!
}
internal object GuideRequestFingerprint : Fingerprint(
    custom = {
        addAnyOf { addUsingNumber(1074004527L) }
        addAnyOf { addUsingNumber(268698159L) }
    },
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        anyInstruction(
            literal(1074004527L),
            literal(268698159L)
        ),
        methodCall(returnType = "I", parameters = listOf("I"), location = MatchAfterWithin(3)),
        anyInstruction(
            literal(1073880817L),
            literal(268574449L)
        )
    )
)

val guideMilestoneMethod = findMethodDirect {
    Fingerprint(
        definingClass = BootstrapStoreReadFingerprint().declaredClass!!.descriptor,
        name = GuideRequestFingerprint.instructionMatches[1].instruction.methodRef!!.name,
        returnType = "I",
        parameters = listOf("I")
    )()
}
