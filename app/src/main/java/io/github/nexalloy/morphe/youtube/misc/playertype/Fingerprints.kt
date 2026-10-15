package io.github.nexalloy.morphe.youtube.misc.playertype

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.FieldUsingType

val playerTypeFingerprint = findMethodDirect {
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("V")
            paramCount = 1
            addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
            declaredClass { className(".YouTubePlayerOverlaysLayout", StringMatchType.EndsWith) }
        }
    }.firstOrNull() ?: findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("V")
            paramCount = 1
            addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
            declaredClass { className(".YouTubePlayerViewNotForReflection", StringMatchType.EndsWith) }
        }
    }.first()
}

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
    strings = listOf(
        "NEW",
        "PLAYING",
        "PAUSED",
        "RECOVERABLE_ERROR",
        "UNRECOVERABLE_ERROR",
        "ENDED"
    )
)

val videoStateFingerprint = findMethodDirect {
    val controlStateType = ControlsStateToStringFingerprint().declaredClass!!.descriptor
    val videoStateType = VideoStateEnumFingerprint().declaredClass!!.descriptor

    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(element = controlStateType),
        filters = listOf(
            // Obfuscated parameter field name.
            fieldAccess(
                definingClass = controlStateType,
                type = videoStateType
            ),
            resourceLiteral(ResourceType.STRING, "accessibility_play"),
            resourceLiteral(ResourceType.STRING, "accessibility_pause")
        )
    )()
}

val videoStateParameterField = findFieldDirect {
    videoStateFingerprint().let { method ->
        method.usingFields.distinct().single { field ->
            // obfuscated parameter field name
            field.usingType == FieldUsingType.Read && field.field.declaredClass == method.paramTypes[0]
        }.field
    }
}