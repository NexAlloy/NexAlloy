package io.github.nexalloy.morphe.youtube.video.videoid

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import java.lang.reflect.Modifier
import org.luckypray.dexkit.DexKitBridge

val videoIdFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addEqString("Null initialPlayabilityStatus")
        }
    }.single()
}

@get:AllowAbstract
val PlayerResponseModel_getVideoId = findMethodDirect {
    videoIdFingerprint().let { method ->
        method.invokes.distinct().single {
            it.returnTypeName == "java.lang.String" && it.declaredClass == method.paramTypes[0] // PlayerResponseModel, interface
        }
    }
}

@SkipTest
internal object VideoIdBackgroundPlayFingerprint : Fingerprint(
    custom = { declaredClass { methodCount(16..17) } },
    accessFlags = listOf(AccessFlags.DECLARED_SYNCHRONIZED, AccessFlags.FINAL, AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MONITOR_EXIT),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately())
    ),
)
context(_: DexKitBridge)
private fun backgroundVideoIdMatch() = VideoIdBackgroundPlayFingerprint.matchAll().single {
    it.method.declaredClass!!.methods.size in 16..17
}

val backgroundVideoIdMethod = findMethodDirect { backgroundVideoIdMatch().method }
val backgroundVideoIdGetterMethods = findMethodListDirect {
    val getter = backgroundVideoIdMatch().instructionMatches.first().instruction.methodRef!!
    Fingerprint(
        interfaces = listOf(getter.declaredClass!!.descriptor),
        name = getter.name,
        returnType = "Ljava/lang/String;",
        parameters = emptyList()
    ).matchAll().map { it.method }.filterNot { Modifier.isAbstract(it.modifiers) }
}

private object VideoIdParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[L",
    parameters = listOf("L"),
    filters = listOf(
        literal(524288L)
    )
)

internal object ForegroundVideoIdFingerprint : Fingerprint(
    classFingerprint = VideoIdParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()), // videoId
        anyInstruction(
            methodCall( // 21.35 and older.
                smali = "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            methodCall( // 21.36+
                opcode = Opcode.INVOKE_VIRTUAL,
                returnType = "V",
                parameters = listOf("Ljava/lang/String;", "L")
            ),
            location = MatchAfterWithin(6)
        ),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately())
    )
)
