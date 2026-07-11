package io.github.nexalloy.morphe.shared.misc.litho.filter

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.conversionContextFingerprintToString
import org.luckypray.dexkit.result.FieldUsingType

//val componentContextParserFingerprint = fingerprint {
//    strings("Number of bits must be positive")
//}

internal object AccessibilityIdFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "Ljava/lang/String;"
        ),
        string("primary_image", location = MatchAfterWithin(5)),
    )
)

val AccessibilityIdMethod = findMethodDirect {
    AccessibilityIdFingerprint.instructionMatches.first().instruction.methodRef!!
}

val accessibilityTextMethod = findMethodDirect {
    Fingerprint(
        returnType = "V",
        filters = listOf(
            methodCall(
                opcode = Opcode.INVOKE_INTERFACE,
                parameters = listOf(),
                returnType = "Ljava/lang/String;"
            ),
            methodCall(
                reference = AccessibilityIdMethod(),
                location = MatchAfterWithin(5)
            )
        ),
        custom = {
        }
    ).instructionMatches.first().instruction.methodRef!!
}

val buttonViewModelReceiver = findMethodDirect {
    ComponentCreateFingerprint.instructionMatches[2].instruction.methodRef!!
}

object ComponentCreateFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.INVOKE_STATIC, MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT),
        string("Element missing correct type extension"),
        string("Element missing type")
    )
)

internal object ProtobufBufferEncodeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[B",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
//            definingClass = "this",
            type = "Lcom/google/android/libraries/elements/adl/UpbMessage;"
        ),
        methodCall(
            definingClass = "Lcom/google/android/libraries/elements/adl/UpbMessage;",
            name = "jniEncode"
        )
    )
)

object ProtobufBufferReferenceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "Ljava/nio/ByteBuffer;"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.SUB_INT_2ADDR,
    )
)

val emptyComponentFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.CONSTRUCTOR)
    parameters()
    strings("EmptyComponent")
}

val lithoThreadExecutorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("I", "I", "I")
    classMatcher {
        superClass {
            descriptor = "Ljava/util/concurrent/ThreadPoolExecutor;"
        }
    }
    literal { 1L }
}

//region rvxp
val conversionContextClass = findClassDirect {
    conversionContextFingerprintToString(this).declaredClass!!
}
val identifierFieldData = findFieldDirect {
    val stringFieldIndex =
        if (findMethod { matcher { usingStrings(", pathInternal=") } }.any()) 2 else 1
    conversionContextClass(this).methods.single {
        it.isConstructor && it.paramCount != 0
    }.usingFields.filter {
        it.usingType == FieldUsingType.Write && it.field.typeName == String::class.java.name
    }[stringFieldIndex].field
}

val pathBuilderFieldData = findFieldDirect {
    conversionContextClass(this).fields.single { it.typeSign == "Ljava/lang/StringBuilder;" }
}

val emptyComponentClass = findClassDirect {
    emptyComponentFingerprint().declaredClass!!
}

val featureFlagCheck = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            paramTypes("long", "boolean")
            addInvoke { paramTypes(null, "long", "boolean") }
        }
    }.single()
}
//endregion
