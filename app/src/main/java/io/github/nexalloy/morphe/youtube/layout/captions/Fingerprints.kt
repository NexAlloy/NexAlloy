package io.github.nexalloy.morphe.youtube.layout.captions

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

private object SubtitleManagerFingerprintClassFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "Landroid/view/accessibility/CaptioningManager;"),
    filters = listOf(
        fieldAccess("Ljava/util/concurrent/TimeUnit;->SECONDS:Ljava/util/concurrent/TimeUnit;"),
        methodCall("Landroid/view/accessibility/CaptioningManager;->isEnabled()Z"),
    ),
)

internal object SubtitleManagerFingerprint : Fingerprint(
    classFingerprint = SubtitleManagerFingerprintClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        string(""),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = emptyList(),
            returnType = "Z",
        ),
        opcode(
            Opcode.IF_EQZ,
            location = InstructionLocation.MatchAfterWithin(3),
        ),
    ),
)

val subtitleManagerBooleanMethod = findMethodDirect {
    SubtitleManagerFingerprint.instructionMatches[1].instruction.methodRef!!
}

val noVolumeCaptionsFeatureFlagMethods = findMethodListDirect {
    findMethod {
        matcher {
            literal { 45692436L }
        }
    }
}

val noVolumeCaptionsFeatureFlagGetters = findMethodListDirect {
    noVolumeCaptionsFeatureFlagMethods()
        .flatMap { it.invokes }
        .filter { method ->
            method.returnTypeName == "boolean" &&
                method.paramTypeNames.any { it == "long" || it == "int" }
        }
        .distinctBy { it.descriptor }
}

internal object StartVideoInformerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID,
    ),
    strings = listOf("pc"),
)
