package io.github.nexalloy.morphe.youtube.misc.litho.rendernext

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Match
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.shared.misc.debugging.BootstrapStoreReadFingerprint
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.DexKitBridge

/**
 * Reads the enable_rendernext field of an Element proto, set by the server.
 * If true, the element is presented with an ElementsView instead of Litho.
 */
internal object RenderNextEnablementCheckFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        string("Failed to read Element proto passed in to RenderNextEnablementCheck: ")
    )
)

/**
 * Callers of the enablement check, that decide if an element is RenderNext.
 * The first boolean they read is the RenderNext field of the element config, set by the server.
 */
val RenderNextConfigField = findFieldDirect {
    val enablementCheck = RenderNextEnablementCheckFingerprint()
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IGET_BOOLEAN,
                type = "Z"
            ),
            methodCall(reference = enablementCheck),
            methodCall(name = "toBuilder")
        )
    ).matchAll().first()
        .instructionMatches.first().instruction.fieldRef!!
}

@SkipTest
fun DexKitBridge.RenderNextConfigFieldReadMatches(): List<Match> {
    val configField = RenderNextConfigField()

    return Fingerprint(
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IGET_OBJECT,
                type = configField.declaredClass.descriptor
            ),
            fieldAccess(
                opcode = Opcode.IGET_BOOLEAN,
                reference = configField
            )
        )
    ).matchAll().also {
        /*
        void M(A aVar) {
            B bVar = aVar.b;       // RenderNextConfigParentField
            boolean cVar = bVar.c; // RenderNextConfigField
            if (cVar)
            ...
        }
        */

        val all = it.all { match ->
            match.method.parameters[0] ==
                    match.instructionMatches[0].instruction.fieldRef!!.declaredClass.descriptor
        }
        if (!all) throw Exception()
    }
}

val RenderNextConfigParentField = findFieldDirect {
    RenderNextConfigFieldReadMatches().first().instructionMatches[0].instruction.fieldRef!!
}

val RenderNextConfigFieldReadFingerprint = findMethodListDirect {
    RenderNextConfigFieldReadMatches().map { it.method }
}

/**
 * Checks if a component identifier is in the comma separated list of the flag 45661551
 * ("template" or "parentTemplate:template"). If true, the Litho component is converted to RenderNext.
 */
@RequireAppVersion(minVersion = "20.29.00")
internal object RenderNextTemplateCheckFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Z",
    parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(
        literal(58),
        methodCall(smali = "Ljava/lang/String;->indexOf(I)I"),
        methodCall(smali = "Ljava/lang/String;->substring(II)Ljava/lang/String;"),
    )
)

@RequireAppVersion("21.35.000")
internal object PageFragmentRouteFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L", "Z", "Z", "Z"),
    filters = listOf(
        string("top_level_browse_pane"),
        literal(1073830434L),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("I"),
            returnType = "Z",
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)

@get:RequireAppVersion("21.35.000")
val pageFragmentRouteFlagMethod = io.github.nexalloy.morphe.findMethodDirect {
    val reference = PageFragmentRouteFingerprint.instructionMatches[2].instruction.methodRef!!
    // Hook the concrete bootstrap store, rather than its interface declaration.
    Fingerprint(
        definingClass = BootstrapStoreReadFingerprint().declaredClass!!.descriptor,
        name = reference.name,
        returnType = "Z",
        parameters = listOf("I")
    )()
}
