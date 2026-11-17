package io.github.nexalloy.morphe.shared.misc.proto

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

@StrictQuery
internal object NewElementProtoParserFingerprint : Fingerprint(
    classFingerprint = ProtoStuffReflectionFingerprint,
    isStatic = true,
    parameters = listOf("L"),
    returnType = "[B",
    filters = listOf(
        checkCast("[B")
    )
)

private object ProtoStuffReflectionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Ljava/lang/reflect/Field;",
    filters = listOf(
        string("buf"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "getDeclaredField"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "setAccessible"
        )
    )
)

/** Bytes of an inline resource of a response, rather than a proto element. Added in YouTube 21.40. */
@get:RequireAppVersion(minVersion = "21.40.000")
val resourceBytesMethod = findMethodDirect {
    val parser = NewElementProtoParserFingerprint()
    Fingerprint(
        accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
        returnType = "[B",
        parameters = listOf(parser.paramTypes.first().descriptor, "Z"),
        filters = listOf(
            methodCall(reference = parser),
            opcode(Opcode.ARRAY_LENGTH)
        )
    )()
}
