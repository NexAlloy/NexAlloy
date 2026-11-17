package io.github.nexalloy.morphe.shared.misc.videoinformation

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.FieldData

/**
 * Matches method {androidx.media3.exoplayer.ExoPlayerImpl.setPlaybackParameters(PlaybackParameters p1)}
 */
val playbackParametersSetterFingerprint = findMethodDirect {
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(PlaybackParametersToStringFingerprint().declaredClass!!.descriptor),
        custom = {
            declaredClass {
                addInterface { descriptor = "Landroidx/media3/exoplayer/ExoPlayer;" }
            }
        }
    )()
}


/**
 * Matches method {androidx.media3.common.PlaybackParameters}.toString()
 */
internal object PlaybackParametersToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(definingClass = "this", opcode = Opcode.IGET, type = "F"),
        string("PlaybackParameters(speed=%.2f, pitch=%.2f)")
    )
)

/** The enum fields are obfuscated, but its initializer keeps the constant names as strings. */
context(bridge: DexKitBridge)
internal fun seekSourceUnknownField(seekSourceEnumType: String): FieldData = Fingerprint(
    definingClass = seekSourceEnumType,
    name = "<clinit>",
    filters = listOf(
        string("SEEK_SOURCE_UNKNOWN"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, definingClass = seekSourceEnumType, type = seekSourceEnumType)
    )
).instructionMatches[1].instruction.fieldRef!!
