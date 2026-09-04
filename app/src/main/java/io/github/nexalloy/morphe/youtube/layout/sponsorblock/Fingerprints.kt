package io.github.nexalloy.morphe.youtube.layout.sponsorblock

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.youtube.shared.seekbarFingerprint

val total_time get() = resourceMappings["string", "total_time"]

internal object AppendTimeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;"
    ),
    filters = listOf(
        resourceLiteral(
            type = ResourceType.STRING,
            name = "total_time"
        ),
        methodCall(
            smali = "Landroid/content/res/Resources;->getString(I[Ljava/lang/Object;)Ljava/lang/String;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
    )
)

val rectangleFieldInvalidatorFingerprint = findMethodDirect {
    val clazz = seekbarFingerprint().declaredClass!!
    clazz.findMethod {
        matcher {
            returnType = "void"
            paramTypes()
            addInvoke { name = "invalidate" }
        }
    }.single()
}

val SponsorBarRect = findFieldDirect {
    val method = rectangleFieldInvalidatorFingerprint()
    val invalidateIndex = method.instructions.first {
        it.methodRef?.name == "invalidate"
    }.index

    method.instructions.asReversed().firstNotNullOfOrNull { instruction ->
        if (instruction.index >= invalidateIndex) {
            null
        } else {
            instruction.fieldRef?.takeIf { it.typeName == "android.graphics.Rect" }
        }
    } ?: error("Could not resolve SponsorBlock seekbar rectangle field")
}

val seekbarOnDrawFingerprint = findMethodDirect {
    seekbarFingerprint().declaredClass!!.findMethod {
        matcher {
            name = "onDraw"
            addInvoke {
                descriptor = "Ljava/lang/Math;->round(F)I"
            }
        }
    }.single()
}

val inset_overlay_view_layout get() = resourceMappings["id", "inset_overlay_view_layout"]

val controlsOverlayFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addUsingNumber(inset_overlay_view_layout)
            paramCount = 0
            returnType = "void"
        }
    }.single()
}

internal object AdProgressTextViewVisibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/libraries/youtube/ads/player/ui/AdProgressTextView;",
            name = "setVisibility",
            parameters = listOf("I"),
            returnType = "V"
        )
    )
)
