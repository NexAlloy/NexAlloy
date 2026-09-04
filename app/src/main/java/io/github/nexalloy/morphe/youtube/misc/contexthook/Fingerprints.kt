package io.github.nexalloy.morphe.youtube.misc.contexthook

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_21_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_33_or_greater

private const val CLIENT_INFO_CLASS =
    "Lcom/google/protos/youtube/api/innertube/InnertubeContext\$ClientInfo;"

private object BuildClientContextBodyConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        string("Android Wear"),
        opcode(Opcode.IF_EQZ),
        string("Android Automotive", location = MatchAfterImmediately()),
        string("Android"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately())
    )
)

private object AuthenticationChangeListenerFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        returnType = "Ljava/util/List;",
        parameters = listOf(
            "Ljava/util/concurrent/Executor;",
            "Lcom/google/protobuf/MessageLite;",
            "L"
        ),
        filters = listOf(string("processFutAsync"))
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "L")
    )
)

private object AuthenticationChangeListenerLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("Authentication changed while request was being made"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "L")
    )
)

private object BuildClientContextBodyFingerprint : Fingerprint(
    classFingerprint = BuildClientContextBodyConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, name = "SDK_INT"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS,
            type = "Ljava/lang/String;"
        ),
        opcode(Opcode.OR_INT_LIT16)
    )
)

private object BuildDummyClientContextBodyFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, name = "instance"),
        string("10.29", location = MatchAfterWithin(10)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = CLIENT_INFO_CLASS
        )
    )
)

internal val messageLiteBuilderField = findFieldDirect {
    BuildDummyClientContextBodyFingerprint.instructionMatches.first().instruction.fieldRef!!
}

internal val clientInfoField = findFieldDirect {
    BuildDummyClientContextBodyFingerprint.instructionMatches.last().instruction.fieldRef!!
}

internal val osNameField = findFieldDirect {
    BuildClientContextBodyFingerprint.instructionMatches[1].instruction.fieldRef!!
}

internal val messageLiteBuilderMethod = findMethodDirect {
    val holderClass = messageLiteBuilderField().declaredClassName
    val listener = if (is_21_33_or_greater) {
        AuthenticationChangeListenerFingerprint()
    } else {
        AuthenticationChangeListenerLegacyFingerprint()
    }
    listener.invokes.first {
        it.paramCount == 0 && it.returnTypeName == holderClass
    }
}

private object BrowseEndpointParentFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("browseId")
)

private object GuideEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("guide")
)

private object NextEndpointParentFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("watchNextType")
)

private object PlayerEndpointParentFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("dataExpiredForSeconds")
)

private object ReelCreateItemsEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/create_reel_items")
)

private object ReelItemWatchEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_item_watch")
)

private object ReelWatchSequenceEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_watch_sequence")
)

private object SearchRequestBuildParametersFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("searchFormData"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "toByteArray",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

private fun requestBodyFingerprint(parent: Fingerprint) = Fingerprint(
    classFingerprint = parent,
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList()
)

internal val browseRequestBodyMethod = findMethodDirect {
    requestBodyFingerprint(BrowseEndpointParentFingerprint)()
}

internal val guideRequestBodyMethod = findMethodDirect {
    requestBodyFingerprint(GuideEndpointConstructorFingerprint)()
}

internal val nextRequestBodyMethod = findMethodDirect {
    requestBodyFingerprint(NextEndpointParentFingerprint)()
}

internal val playerRequestBodyMethod = findMethodDirect {
    requestBodyFingerprint(PlayerEndpointParentFingerprint)()
}

internal val searchRequestBodyMethod = findMethodDirect {
    requestBodyFingerprint(SearchRequestBuildParametersFingerprint)()
}

internal val getWatchRequestBodyMethods = findMethodListDirect {
    val constructors = findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
            strings("get_watch")
        }
    }.distinctBy { it.declaredClass!!.descriptor }

    val primary = constructors.filter {
        it.declaredClass!!.fields.any { field ->
            field.typeName == "java.util.function.Consumer"
        }
    }.single()
    val secondary = constructors.filter {
        it.declaredClass!!.fields.none { field ->
            field.typeName == "java.util.function.Consumer"
        }
    }.single()

    listOf(primary, secondary).map { constructor ->
        constructor.declaredClass!!.findMethod {
            matcher {
                accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
                returns("V")
                parameters()
            }
        }.single()
    }
}

internal val reelRequestBodyMethods = findMethodListDirect {
    buildList {
        add(requestBodyFingerprint(ReelItemWatchEndpointConstructorFingerprint)())
        add(requestBodyFingerprint(ReelWatchSequenceEndpointConstructorFingerprint)())
        if (!is_21_21_or_greater) {
            add(requestBodyFingerprint(ReelCreateItemsEndpointConstructorFingerprint)())
        }
    }.distinctBy { it.descriptor }
}
