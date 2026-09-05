package io.github.nexalloy.morphe.youtube.layout.shortsnoresume

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

@RequireAppVersion("21.03.00")
internal object UserWasInShortsEvaluateAnchorFingerprint: Fingerprint(
    returnType = "Z",
    filters = listOf(
        literal(1073815471),
        literal(1073815469)
    )
)

/**
 * 21.03+
 */
@RequireAppVersion("21.03.00")
internal object UserWasInShortsEvaluateFingerprint : Fingerprint(
    classFingerprint = UserWasInShortsEvaluateAnchorFingerprint,
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "Z", "Z", "L", "Z")
        ),
//        methodCall( // 21.30+
//            opcode = Opcode.INVOKE_DIRECT_RANGE,
//            name = "<init>",
//            parameters = listOf("L", "L", "L", "L", "L", "L",  "Ljava/lang/String;"),
//            location = InstructionLocation.MatchAfterWithin(50)
//        )
    )
)

/**
 * 20.02+
 */
@RequireAppVersion("20.02.00", "21.03.00")
internal object UserWasInShortsListenerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("Ljava/lang/Boolean;"),
        methodCall(smali = "Ljava/lang/Boolean;->booleanValue()Z", location = InstructionLocation.MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        string("ShortsStartup SetUserWasInShortsListener", StringComparisonType.CONTAINS, InstructionLocation.MatchAfterWithin(30))
    )
)
