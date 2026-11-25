/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2628
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.layout.playlistautoplay

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.string
import java.lang.reflect.Modifier

internal object NavigationIntentEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        string("NEXT"),
        string("PREVIOUS"),
        string("AUTOPLAY"),
        string("AUTONAV"),
        string("JUMP"),
        opcode(Opcode.RETURN_VOID),
    ),
    custom = {
        declaredClass { addMethod { name = "<init>"; paramCount(min = 3) } }
    }
)

internal object NavigationIntentWrapperConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = { listOf(NavigationIntentEnumFingerprint().declaredClass!!.descriptor, "L", "L") }
)

val navigationIntentField = findFieldDirect {
    val enumType = NavigationIntentEnumFingerprint().declaredClass!!
    NavigationIntentWrapperConstructorFingerprint().declaredClass!!.fields.first { it.type == enumType }
}

val navigationIntentMethods = findMethodListDirect {
    val wrapperType = NavigationIntentWrapperConstructorFingerprint().declaredClass!!.descriptor
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(wrapperType)
    ).matchAll().map { it.method }
        .filterNot { Modifier.isAbstract(it.modifiers) }
        .also { require(it.isNotEmpty()) }
}

internal object NavigationPlayerButtonsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = { listOf(NavigationIntentWrapperConstructorFingerprint().declaredClass!!.descriptor, "Ljava/lang/Runnable;") },
    filters = listOf(
        methodCall(parameters = { listOf(NavigationIntentWrapperConstructorFingerprint().declaredClass!!.descriptor) }, returnType = "Z"),
        methodCall(smali = "Ljava/lang/Runnable;->run()V", location = MatchAfterWithin(3)),
        methodCall(parameters = { listOf(NavigationIntentWrapperConstructorFingerprint().declaredClass!!.descriptor) }, returnType = "V", location = MatchAfterWithin(3))
    )
)
