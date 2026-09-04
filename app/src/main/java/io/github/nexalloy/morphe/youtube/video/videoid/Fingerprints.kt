package io.github.nexalloy.morphe.youtube.video.videoid

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode

val videoIdFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addEqString("Null initialPlayabilityStatus")
        }
    }.single()
}

val PlayerResponseModel_getVideoId = findMethodDirect {
    videoIdFingerprint().let { method ->
        method.invokes.distinct().single {
            it.returnTypeName == "java.lang.String" && it.declaredClass == method.paramTypes[0]
        }
    }
}

internal object VideoIdBackgroundPlayFingerprint : Fingerprint(
    accessFlags = listOf(
        AccessFlags.DECLARED_SYNCHRONIZED,
        AccessFlags.FINAL,
        AccessFlags.PUBLIC,
    ),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;",
        ),
        opcode(Opcode.MONITOR_EXIT),
        opcode(Opcode.RETURN_VOID),
        opcode(Opcode.MONITOR_EXIT),
        opcode(Opcode.RETURN_VOID),
    ),
    custom = {
        declaredClass {
            methodCount(16, 17)
        }
    },
)

val backgroundVideoIdField = findFieldDirect {
    VideoIdBackgroundPlayFingerprint.instructionMatches[2].instruction.fieldRef!!
}
