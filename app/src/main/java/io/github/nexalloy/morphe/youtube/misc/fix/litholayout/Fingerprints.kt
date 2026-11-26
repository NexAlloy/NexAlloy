/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/5331
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.misc.fix.litholayout

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

internal const val STATE_UPDATE_LOOP_MESSAGE =
    "State update loop during layout detected. Most recent attribution: "

internal object ComponentTreeUpdateStateFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/litho/ComponentTree;",
    returnType = "V",
    parameters = listOf("Z", "Ljava/lang/String;", "Z"),
    filters = listOf(
        string(STATE_UPDATE_LOOP_MESSAGE),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = List(3) { "Ljava/lang/String;" },
            returnType = "Ljava/lang/String;",
            location = MatchAfterWithin(4),
        ),
    ),
)

internal val stateUpdateLoopMessageMethod = findMethodDirect {
    ComponentTreeUpdateStateFingerprint.instructionMatches.last().instruction.methodRef!!
}

internal object RecyclerBinderFingerprint : Fingerprint(
    strings = listOf("Too many retries -- RecyclerView is stuck in layout. Batch size: "),
)

@StrictQuery
internal object RecyclerBinderConstructorFingerprint : Fingerprint(
    classFingerprint = RecyclerBinderFingerprint,
    name = "<init>",
    filters = listOf(
        newInstance("Ljava/util/HashMap;"),
        methodCall(smali = "Ljava/util/HashMap;-><init>()V", location = MatchAfterWithin(5)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/util/Map;",
            location = MatchAfterWithin(5),
        ),
    ),
)

internal val recyclerItemHeightsField = findFieldDirect {
    RecyclerBinderConstructorFingerprint.instructionMatches.last().instruction.fieldRef!!
}

@StrictQuery
internal object ItemMeasureListenerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        fieldAccess(reference = recyclerItemHeightsField, opcode = Opcode.IGET_OBJECT),
        methodCall(
            smali = "Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z",
            location = MatchAfterWithin(5),
        ),
        opcode(Opcode.IF_EQZ, location = MatchAfterWithin(5)),
        methodCall(
            smali = "Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;",
            location = MatchAfterWithin(5),
        ),
        checkCast("Ljava/lang/Integer;", location = MatchAfterWithin(5)),
    ),
)
