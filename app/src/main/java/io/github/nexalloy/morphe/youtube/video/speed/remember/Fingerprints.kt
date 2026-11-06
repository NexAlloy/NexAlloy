package io.github.nexalloy.morphe.youtube.video.speed.remember

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall

@RequireAppVersion("21.11.000")
internal object SpeedmasterStartFingerprint : Fingerprint(
    filters = listOf(
        literal(307700L),
        literal(2.0f),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("F"),
            returnType = "V"
        )
    )
)

@get:RequireAppVersion("21.11.000")
val speedmasterSetSpeedMethod = findMethodDirect {
    SpeedmasterStartFingerprint.instructionMatches.last().instruction.methodRef!!
}
