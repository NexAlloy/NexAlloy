package io.github.nexalloy.morphe.youtube.layout.captions

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.result.FieldData

private object SubtitleManagerFingerprintClassFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("L", "Landroid/view/accessibility/CaptioningManager;"),
    filters = listOf(
        fieldAccess("Ljava/util/concurrent/TimeUnit;->SECONDS:Ljava/util/concurrent/TimeUnit;"),
        methodCall("Landroid/view/accessibility/CaptioningManager;->isEnabled()Z"),
    ),
    custom = {
        accessFlags(AccessFlags.STATIC)
    }
)

internal object SubtitleManagerFingerprint : Fingerprint(
    classFingerprint = SubtitleManagerFingerprintClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchFirst()),
        string(""),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "Z"
        ),
        opcode(opcode = Opcode.IF_EQZ, location = MatchAfterWithin(3))
    )
)

val subtitleTrackIsDisableOptionMethod = findMethodDirect {
    SubtitleManagerFingerprint.instructionMatches[2].instruction.methodRef!!
}

internal object SubtitleManagerForcedTrackFingerprint : Fingerprint(
    classFingerprint = SubtitleManagerFingerprintClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, type = "Z"),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
        opcode(Opcode.IF_LTZ, location = MatchAfterImmediately())
    )
)
val captionSelectionReasonField = findFieldDirect {
    SubtitleManagerFingerprint.instructionMatches[0].instruction.fieldRef!!
}
val forcedCaptionTrackField = findFieldDirect {
    SubtitleManagerForcedTrackFingerprint.instructionMatches[0].instruction.fieldRef!!
}
val forcedCaptionOwnerFields = findFieldListDirect {
    val match = SubtitleManagerForcedTrackFingerprint.match()
    val forced = match.instructionMatches[0]
    var receiver = forced.instruction.registers[1]
    val thisRegister = SubtitleManagerFingerprint.instructionMatches[0].instruction.registers[1] - 1
    val fields = mutableListOf<FieldData>()
    var before = forced.index
    while (receiver != thisRegister) {
        val producer = match.method.instructions.take(before).last {
            it.opcode == Opcode.IGET_OBJECT.ordinal && it.registers[0] == receiver
        }
        fields += producer.fieldRef!!
        receiver = producer.registers[1]
        before = producer.index
    }
    fields.reversed()
}
