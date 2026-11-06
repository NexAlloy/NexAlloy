/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4278
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.misc.fix.guidecache

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode

@RequireAppVersion(minVersion = "21.28.000", maxVersion = "21.31.000")
internal object GuideResponseAccountFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
    parameters = listOf("L"),
    filters = listOf(
        literal(1073830184L),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("I"),
            returnType = "Z",
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)

@get:RequireAppVersion(minVersion = "21.28.000", maxVersion = "21.31.000")
internal val guideAccountCheckMethods = io.github.nexalloy.morphe.findMethodListDirect {
    val called = GuideResponseAccountFingerprint.instructionMatches[1].instruction.methodRef!!
    val owner = called.declaredClass!!.descriptor
    fun implementsOwner(type: org.luckypray.dexkit.result.ClassData): Boolean =
        type.descriptor == owner || type.interfaces.any(::implementsOwner) ||
            type.superClass?.let(::implementsOwner) == true
    findMethod {
        matcher {
            name = called.name
            paramTypes("int")
            returnType = "boolean"
            not { accessFlags(AccessFlags.ABSTRACT.modifier) }
        }
    }.filter { implementsOwner(it.declaredClass!!) }.also { check(it.isNotEmpty()) }
}
