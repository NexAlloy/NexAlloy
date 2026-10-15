package io.github.nexalloy.morphe.youtube.misc.contexthook

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionFilter
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
import io.github.nexalloy.morphe.youtube.shared.BuildClientContextBodyConstructorFingerprint
import io.github.nexalloy.morphe.youtube.shared.CLIENT_INFO_CLASS

// 21.33+
@SkipTest
internal object AuthenticationChangeListenerFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        returnType = "Ljava/util/List;",
        parameters = listOf(
            "Ljava/util/concurrent/Executor;",
            "Lcom/google/protobuf/MessageLite;",
            "L"
        ),
        filters = listOf(
            string("processFutAsync")
        )
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "L")
    )
)

object BuildClientContextBodyFingerprint : Fingerprint(
    classFingerprint = BuildClientContextBodyConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, name = "SDK_INT"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS,
            type = "Ljava/lang/String;"
        ),
        opcode(Opcode.OR_INT_LIT16),
    )
)

internal object BuildDummyClientContextBodyFingerprint : Fingerprint(
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
            type = CLIENT_INFO_CLASS,
        ),
    )
)

val clientInfoField = findFieldDirect {
    BuildDummyClientContextBodyFingerprint.instructionMatches.last().instruction.fieldRef!!
}

@get:SkipTest /* unused */
val clientVersionField = findFieldDirect {
    BuildDummyClientContextBodyFingerprint.instructionMatches[2].instruction.fieldRef!!
}

val messageLiteBuilderField = findFieldDirect {
    BuildDummyClientContextBodyFingerprint.instructionMatches.first().instruction.fieldRef!!
}

@get:RequireAppVersion(maxVersion = "21.33.000")
val messageLiteBuilderMethodLegacy = findMethodDirect {
    val messageLiteBuilderClassName = messageLiteBuilderField().declaredClassName
    Fingerprint(
        accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
        returnType = "V",
        strings = listOf("Authentication changed while request was being made"),
        filters = listOf(
            methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "L")
        )
    ).buildMethodMatcher().let {
        findMethod { matcher(it) }[0]
    }.invokes.single { it.returnTypeName == messageLiteBuilderClassName }
}

@get:RequireAppVersion("21.33.000")
val messageLiteBuilderMethod = findMethodDirect {
    val messageLiteBuilderClassName = messageLiteBuilderField().declaredClassName
    AuthenticationChangeListenerFingerprint()
        .invokes.first { it.returnTypeName == messageLiteBuilderClassName }
}

val osNameField = findFieldDirect {
    BuildClientContextBodyFingerprint.instructionMatches[1].instruction.fieldRef!!
}

@get:SkipTest /* unused */
val clientFormFactorField = findFieldDirect {
    val setClientFormFactorFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf("L"),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IGET,
                definingClass = CLIENT_INFO_CLASS,
                type = "I"
            ),
            methodCall(
                reference = ClientFormFactorEnumOrdinalFingerprint()
            )
        )
    )

    setClientFormFactorFingerprint.instructionMatches.first().instruction.fieldRef!!
}

private object ClientFormFactorEnumConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "UNKNOWN_FORM_FACTOR",
        "SMALL_FORM_FACTOR",
        "LARGE_FORM_FACTOR",
        "AUTOMOTIVE_FORM_FACTOR",
        "WEARABLE_FORM_FACTOR",
    )
)

internal object ClientFormFactorEnumOrdinalFingerprint : Fingerprint(
    classFingerprint = ClientFormFactorEnumConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("I")
)


/* Make following fingerprints directly match EndpointRequestBody */
open class EndpointRequestBodyFingerprint(
    classFingerprint: Fingerprint? = null,
    definingClass: String? = null,
    name: String? = null,
    accessFlags: List<AccessFlags>? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    filters: List<InstructionFilter>? = null,
    strings: List<String>? = null
) : Fingerprint(
    classFingerprint = Fingerprint(
        classFingerprint,
        definingClass,
        name,
        accessFlags,
        returnType,
        parameters,
        filters,
        strings
    ),
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
)

internal object BrowseEndpointParentFingerprint : EndpointRequestBodyFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("browseId"),
)

val GetWatchEndpointConstructor = findMethodListDirect {
    findClass {
        matcher {
            addMethod {
                accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
                returns("V")
                addEqString("get_watch")
            }
        }
    }.findMethod {
        matcher {
            accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
            returns("V")
            parameters()
        }
    }
}

internal object GuideEndpointConstructorFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("guide"),
)

internal object NextEndpointParentFingerprint : EndpointRequestBodyFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("watchNextType"),
)

internal object PlayerEndpointParentFingerprint : EndpointRequestBodyFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("dataExpiredForSeconds"),
)

@RequireAppVersion(maxVersion = "21.21.00")
internal object ReelCreateItemsEndpointConstructorFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/create_reel_items"),
)

internal object ReelItemWatchEndpointConstructorFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_item_watch"),
)

internal object ReelWatchSequenceEndpointConstructorFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("reel/reel_watch_sequence"),
)

internal object SearchRequestBuildParametersFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("searchFormData"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "toByteArray",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    )
)

internal object TranscriptEndpointConstructorFingerprint : EndpointRequestBodyFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("get_transcript"),
)
