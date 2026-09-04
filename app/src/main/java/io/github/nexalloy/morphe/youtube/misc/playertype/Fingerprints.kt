package io.github.nexalloy.morphe.youtube.misc.playertype

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.FieldUsingType

val playerTypeFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    methodMatcher {
        addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
    }
    classMatcher {
        className(".YouTubePlayerOverlaysLayout", StringMatchType.EndsWith)
    }
}

val reelWatchPlayerId get() = resourceMappings["id", "reel_watch_player"]
val reelWatchPagerFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Landroid/view/View;")
    literal { reelWatchPlayerId }
}

val ReelPlayerViewField = findFieldDirect {
    reelWatchPagerFingerprint().declaredClass!!.fields.single { it.typeName.endsWith("ReelPlayerView") }
}

private val videoStateEnumFingerprint = fingerprint {
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    parameters()
    strings(
        "NEW",
        "PLAYING",
        "PAUSED",
        "RECOVERABLE_ERROR",
        "UNRECOVERABLE_ERROR",
        "ENDED"
    )
}

private val controlsStateToStringFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/String;")
    parameters()
    strings("videoState", "isBuffering")
}

val ControlsState = findClassDirect {
    controlsStateToStringFingerprint().declaredClass!!
}

private val VideoStateEnum = findClassDirect {
    videoStateEnumFingerprint().declaredClass!!
}

val videoStateFingerprint = findMethodDirect {
    val controlsStateClass = ControlsState(this).descriptor
    val videoStateClass = VideoStateEnum(this).descriptor

    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(controlsStateClass),
        filters = listOf(
            fieldAccess(
                definingClass = controlsStateClass,
                type = videoStateClass
            ),
            resourceLiteral(ResourceType.STRING, "accessibility_play"),
            resourceLiteral(ResourceType.STRING, "accessibility_pause")
        )
    )()
}

val videoStateParameterField = findFieldDirect {
    videoStateFingerprint().let { method ->
        method.usingFields.distinct().single { field ->
            field.usingType == FieldUsingType.Read &&
                field.field.declaredClass == method.paramTypes[0] &&
                field.field.type == VideoStateEnum(this)
        }.field
    }
}
