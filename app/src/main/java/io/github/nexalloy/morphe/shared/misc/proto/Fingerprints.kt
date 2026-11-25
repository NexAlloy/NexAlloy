package io.github.nexalloy.morphe.shared.misc.proto

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.StringComparisonType
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
@RequireAppVersion(minVersion = "21.40.000")
internal object ResourceBytesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "[B",
    parameters = { listOf(NewElementProtoParserFingerprint().paramTypes.first().descriptor, "Z") },
    filters = listOf(
        methodCall(reference = { NewElementProtoParserFingerprint() }),
        opcode(Opcode.ARRAY_LENGTH)
    )
)

@TargetApp("youtube")
internal object RenderNextEnablementCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    filters = listOf(
        string("RenderNextEnablementCheck", StringComparisonType.CONTAINS)
    )
)

@TargetApp("youtube")
internal object ElementConverterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(methodCall(returnType = "[B"), methodCall(reference = { RenderNextEnablementCheckFingerprint() }))
)
