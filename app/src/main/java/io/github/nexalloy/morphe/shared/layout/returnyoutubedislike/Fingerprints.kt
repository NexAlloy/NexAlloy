package io.github.nexalloy.morphe.shared.layout.returnyoutubedislike

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.DexKitBridge

internal object EndpointServiceNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "L",
    filters = listOf(
        string("serviceName"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        )
    )
)

internal object DislikeFingerprint : Fingerprint(
    name = "<init>", // Public or protected constructor depending on the app version.
    returnType = "V",
    filters = listOf(
        string("like/dislike")
    )
)

context(dexkit: DexKitBridge)
private fun likeEndpointParserClass() = DislikeFingerprint().declaredClass!!.superClass!!

val endpointServiceNameField = findFieldDirect {
    EndpointServiceNameFingerprint.instructionMatches.last().instruction.fieldRef!!
}

val voteVideoIdField = findFieldDirect {
    Fingerprint(
        definingClass = likeEndpointParserClass().descriptor,
        accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
        parameters = listOf(),
        filters = listOf(
            // playlistId
            fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;"),
            methodCall(opcode = Opcode.INVOKE_VIRTUAL, smali = "Ljava/lang/String;->isEmpty()Z"),
            // videoId
            fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;")
        )
    ).instructionMatches.last().instruction.fieldRef!!
}

internal object LikeEndpointParserFingerprint : Fingerprint(
    definingClass = { likeEndpointParserClass().descriptor },
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, location = MatchFirst()),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this"),
        string("")
    )
)

val voteTargetField = findFieldDirect {
    LikeEndpointParserFingerprint.instructionMatches[1].instruction.fieldRef!!
}

val votePayloadTargetField = findFieldDirect {
    val match = LikeEndpointParserFingerprint.match()
    val assignment = match.instructionMatches[1]
    val register = assignment.instruction.register(0)
    match.method.instructions.take(assignment.index).last {
        it.opcode == Opcode.IGET_OBJECT.ordinal && it.registerOrNull(0) == register
    }.fieldRef!!.also { check(it.type == voteTargetField().type) }
}

val votePayloadPresenceField = findFieldDirect {
    val match = LikeEndpointParserFingerprint.match()
    match.method.instructions.take(match.instructionMatches[1].index).last {
        it.opcode == Opcode.IGET.ordinal && it.fieldRef?.declaredClass == votePayloadTargetField().declaredClass
    }.fieldRef!!
}

val votePayloadUnpackMethod = findMethodDirect {
    val match = LikeEndpointParserFingerprint.match()
    match.method.instructions.take(match.instructionMatches[1].index).last {
        it.methodRef?.returnType?.descriptor == "Ljava/lang/Object;"
    }.methodRef!!
}

internal object ComponentHostSetContentDescriptionFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/litho/ComponentHost;",
    name = "setContentDescription",
    returnType = "V",
    parameters = listOf("Ljava/lang/CharSequence;")
)

internal object YogaSetWidthFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/yoga/YogaNodeJNIBase;",
    returnType = "V",
    parameters = listOf("F"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_WIDE, definingClass = "this", type = "J"),
        methodCall(definingClass = "Lcom/facebook/yoga/YogaNative;", name = "jni_YGNodeStyleSetWidthJNI")
    )
)

val yogaNodePointerField = findFieldDirect {
    YogaSetWidthFingerprint.instructionMatches.first().instruction.fieldRef!!
}
