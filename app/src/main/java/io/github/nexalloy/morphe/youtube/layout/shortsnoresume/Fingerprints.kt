package io.github.nexalloy.morphe.youtube.layout.shortsnoresume

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/**
 * 21.03+
 */
@RequireAppVersion("21.03.00")
internal object UserWasInShortsEvaluateFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "Z", "Z", "L", "Z")
        ),
        anyInstruction(
            methodCall(
                opcode = Opcode.INVOKE_DIRECT_RANGE,
                name = "<init>",
                parameters = listOf("L", "L", "L", "L", "L", "I"),
                location = InstructionLocation.MatchAfterWithin(50)
            ),
            methodCall(
                opcode = Opcode.INVOKE_DIRECT_RANGE,
                name = "<init>",
                parameters = listOf("L", "L", "L", "L", "L", "L", "Ljava/lang/String;"),
                location = InstructionLocation.MatchAfterWithin(50)
            )
        )
    )
)

val userWasInShortsEvaluateConstructor = findMethodDirect {
    UserWasInShortsEvaluateFingerprint.instructionMatches.first().instruction.methodRef!!
}

/**
 * 20.02+
 */
@RequireAppVersion("20.02.00", "21.03.00")
internal object UserWasInShortsListenerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("Ljava/lang/Boolean;"),
        methodCall(
            smali = "Ljava/lang/Boolean;->booleanValue()Z",
            location = InstructionLocation.MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        string(
            "ShortsStartup SetUserWasInShortsListener",
            StringComparisonType.CONTAINS,
            InstructionLocation.MatchAfterWithin(30)
        )
    )
)

val userWasInShortsBooleanValueMethod = findMethodDirect {
    UserWasInShortsListenerFingerprint.instructionMatches[1].instruction.methodRef!!
}
