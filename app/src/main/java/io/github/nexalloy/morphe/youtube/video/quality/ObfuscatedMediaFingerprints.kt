package io.github.nexalloy.morphe.youtube.video.quality

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.*
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import org.luckypray.dexkit.DexKitBridge

@RequireAppVersion(minVersion = "21.46.000")
internal object ObfuscatedMediaDiscardFingerprint : Fingerprint(
    filters = listOf(
        string("discardBufferAfterCurrentSegment with disposed BufferManager"),
        // Returns the buffered media of a track.
        methodCall(
            returnType = "L",
            parameters = listOf("L"),
            location = MatchAfterWithin(10)
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            returnType = "V",
            parameters = listOf("J"),
            location = MatchAfterImmediately()
        )
    )
)
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedMediaGetMethod = findMethodDirect {
    ObfuscatedMediaDiscardFingerprint.instructionMatches[1].instruction.methodRef!!
}
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedMediaDiscardAfterMethod = findMethodDirect {
    ObfuscatedMediaDiscardFingerprint.instructionMatches.last().instruction.methodRef!!
}
context(bridge: DexKitBridge)
private fun clearBufferFingerprint() = Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "this", returnType = "V", parameters = listOf()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = obfuscatedMediaGetMethod(bridge).declaredClass!!.descriptor),
        string("setStartTimeUs with disposed BufferManager")
    )
)
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedClearBufferMethod = findMethodDirect { clearBufferFingerprint()() }
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedStopRequestsMethod = findMethodDirect { clearBufferFingerprint().instructionMatches[0].instruction.methodRef!! }
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedBufferManagerField = findFieldDirect { clearBufferFingerprint().instructionMatches[1].instruction.fieldRef!! }
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedFirstSegmentEndTimeField = findFieldDirect {
    Fingerprint(
        definingClass = obfuscatedMediaDiscardAfterMethod().declaredClass!!.descriptor,
        returnType = "V", parameters = listOf(),
        filters = listOf(
            methodCall(smali = "Ljava/util/List;->isEmpty()Z"),
            methodCall(returnType = "J", parameters = listOf()),
            fieldAccess(opcode = Opcode.IPUT_WIDE, definingClass = "this", type = "J")
        )
    ).instructionMatches.last().instruction.fieldRef!!
}
context(bridge: DexKitBridge)
private fun sourceDiscardFingerprint() = Fingerprint(
    returnType = "V", parameters = listOf("J"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_WIDE, type = "J"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = obfuscatedMediaGetMethod(bridge).declaredClass!!.descriptor),
        methodCall(smali = "Ljava/lang/Math;->max(JJ)J")
    )
)
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedSourceDiscardMethod = findMethodDirect { sourceDiscardFingerprint()() }
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedSourceManagerField = findFieldDirect { sourceDiscardFingerprint().instructionMatches[1].instruction.fieldRef!! }
@get:RequireAppVersion(minVersion = "21.46.000")
val obfuscatedSourcePrepareDiscardMethod = findMethodDirect {
    val match = sourceDiscardFingerprint().match()
    match.method.instructions.take(match.instructionMatches[1].index).last {
        it.methodRef?.declaredClass == match.method.declaredClass &&
            it.methodRef?.paramTypes?.map { p -> p.descriptor } == listOf("J") &&
            it.methodRef?.returnType?.descriptor == "V"
    }.methodRef!!
}
