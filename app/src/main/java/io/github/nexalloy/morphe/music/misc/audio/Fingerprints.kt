package io.github.nexalloy.morphe.music.misc.audio

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode


@RequireAppVersion(minVersion = "9.32.00")
internal object AllowExclusiveAudioPlaybackFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    )
)

@RequireAppVersion(maxVersion = "9.31.99")
internal object AllowExclusiveAudioPlaybackLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    )
)

@StrictQuery
internal object ExternalAudioRestrictionFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(
            smali = "Lj$/util/concurrent/ConcurrentHashMap;->keySet()Ljava/util/Set;",
            location = MatchAfterWithin(7)
        ),
        methodCall(
            smali = $$"Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;",
            location = MatchAfterWithin(2)
        ),
        methodCall(
            smali = "Lj$/util/stream/Stream;->anyMatch(Ljava/util/function/Predicate;)Z",
            location = MatchAfterWithin(4)
        ),
        // Compares the skip result with "action unavailable" (11) before stopping playback.
        methodCall(
            smali = "Lj$/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z",
            location = MatchAfterWithin(6)
        )
    )
)

internal val externalAudioRestrictionMethod = findMethodDirect {
    ExternalAudioRestrictionFingerprint.instructionMatches.first().instruction.methodRef!!
}
