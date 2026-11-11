package io.github.nexalloy.morphe.shared.misc.spoof

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import java.lang.reflect.Modifier

/**
 * Builds a Cronet url request, which every network request of the app is made with.
 */
internal object CronetUrlRequestBuildFingerprint : Fingerprint(
    name = "build",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lorg/chromium/net/ExperimentalUrlRequest;",
    parameters = listOf(),
    filters = listOf(
        // Creates the request with the url and the other values of the builder.
        methodCall(returnType = "Lorg/chromium/net/ExperimentalUrlRequest;")
    ),
    custom = {
        declaredClass { superClass { descriptor = "Lorg/chromium/net/ExperimentalUrlRequest\$Builder;" } }
    }
)

internal object CronetUrlRequestSetHttpMethodFingerprint : Fingerprint(
    classFingerprint = CronetUrlRequestBuildFingerprint,
    name = "setHttpMethod",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lorg/chromium/net/ExperimentalUrlRequest\$Builder;",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        )
    )
)

internal val cronetCreateRequestMethods = findMethodListDirect {
    val dispatch = CronetUrlRequestBuildFingerprint.instructionMatches.last().instruction.methodRef!!
    val engineType = dispatch.declaredClass!!.descriptor
    // The call can reference an abstract engine method. Xposed needs its concrete overrides.
    Fingerprint(
        name = dispatch.name,
        returnType = dispatch.returnType!!.descriptor,
        parameters = dispatch.paramTypes.map { it.descriptor },
    ).matchAll().map { it.method }.filter { method ->
        !Modifier.isAbstract(method.modifiers) && !Modifier.isStatic(method.modifiers) &&
                generateSequence(method.declaredClass) { it.superClass }.any { it.descriptor == engineType }
    }.also { check(it.isNotEmpty()) { "No concrete Cronet request creation methods found" } }
}

internal val cronetUrlField = findFieldDirect {
    CronetUrlRequestBuildFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == "Ljava/lang/String;" && Modifier.isFinal(it.modifiers)
    }
}

internal val cronetUploadField = findFieldDirect {
    CronetUrlRequestBuildFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == "Lorg/chromium/net/UploadDataProvider;"
    }
}

internal val cronetHttpMethodField = findFieldDirect {
    CronetUrlRequestSetHttpMethodFingerprint.instructionMatches.first().instruction.fieldRef!!
}

internal object CronetDataSourceReadFingerprint : Fingerprint(
    // NexAlloy: Match both final and non-final Cronet read implementations.
    returnType = "I",
    parameters = listOf("[B", "I", "I"),
    filters = listOf(
        methodCall(definingClass = "Ljava/nio/ByteBuffer;", name = "clear"),
        // Reads the next part of the response.
        methodCall(
            definingClass = "this",
            parameters = listOf("Ljava/nio/ByteBuffer;", "L"),
            returnType = "V"
        ),
        // If the response is finished.
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            type = "Z",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        // Remaining byte count of the request.
        fieldAccess(
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J",
            location = MatchAfterImmediately()
        )
    )
)

internal object CronetDataSourceReadBufferFingerprint : Fingerprint(
    classFingerprint = CronetDataSourceReadFingerprint,
    returnType = "I",
    parameters = listOf("Ljava/nio/ByteBuffer;"),
    filters = listOf(
        // Reads the next part of the response.
        methodCall(
            definingClass = "this",
            parameters = listOf("Ljava/nio/ByteBuffer;", "L"),
            returnType = "V"
        ),
        // If the response is finished.
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            type = "Z",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        // Remaining byte count of the request.
        fieldAccess(
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J",
            location = MatchAfterImmediately()
        )
    )
)

internal object PriorityTooLowExceptionFingerprint : Fingerprint(
    superclass = "Ljava/io/IOException;",
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("I", "I"),
    filters = listOf(
        string("Priority too low [priority=")
    )
)

val cronetRemainingBytesField = findFieldDirect {
    CronetDataSourceReadFingerprint.instructionMatches.last().instruction.fieldRef!!
}

internal object InvalidResponseCodeExceptionFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("I", "Ljava/io/IOException;", "Ljava/util/Map;", "L"),
    filters = listOf(
        string("Response code: ")
    )
)

val cronetReadResponseMethod = findMethodDirect {
    CronetDataSourceReadFingerprint.instructionMatches[1].instruction.methodRef!!
}

val cronetDataSpecField = findFieldDirect {
    val dataSpecType = cronetReadResponseMethod().paramTypes[1].descriptor
    CronetDataSourceReadFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == dataSpecType
    }
}
