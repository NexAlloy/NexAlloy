package io.github.nexalloy.morphe.youtube.misc.playertype

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.result.FieldUsingType

object PlayerTypeFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/player/YouTubePlayerViewNotForReflection;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    custom = {
        addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
    }
)

internal object ReelWatchPagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "reel_watch_player"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterWithin(10))
    )
)

val ReelPlayerViewField = findFieldDirect {
    ReelWatchPagerFingerprint().declaredClass!!.fields.single { it.typeName.endsWith("ReelPlayerView") }
}

// 20.33 and lower class name ControlsState. 20.34+ class name is obfuscated.
internal object ControlsStateToStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("videoState"),
        string("isBuffering")
    )
)

internal object VideoStateEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(),
    filters = listOf(
        string("NEW"),
        string("PLAYING"),
        string("PAUSED"),
        string("RECOVERABLE_ERROR"),
        string("UNRECOVERABLE_ERROR"),
        string("ENDED"),
        opcode(Opcode.APUT_OBJECT)
    )
)

internal object VideoStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = { listOf(ControlsStateToStringFingerprint().declaredClass!!.descriptor) },
    filters = listOf(
        // Obfuscated parameter field name.
        fieldAccess(
            definingClass = { ControlsStateToStringFingerprint().declaredClass!!.descriptor },
            type = { VideoStateEnumFingerprint().declaredClass!!.descriptor }
        ),
        resourceLiteral(ResourceType.STRING, "accessibility_play"),
        resourceLiteral(ResourceType.STRING, "accessibility_pause")
    )
)

val videoStateParameterField = findFieldDirect {
    VideoStateFingerprint().let { method ->
        method.usingFields.distinct().single { field ->
            // obfuscated parameter field name
            field.usingType == FieldUsingType.Read && field.field.declaredClass == method.paramTypes[0]
        }.field
    }
}
