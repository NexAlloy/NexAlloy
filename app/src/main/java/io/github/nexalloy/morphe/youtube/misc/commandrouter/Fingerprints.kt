/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3905
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.misc.commandrouter

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.methodCall

/**
 * Looks up the account scoped command router, which only the command router of an activity does.
 */
internal object CommandRouterFragmentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    strings = listOf("com.google.android.apps.youtube.app.endpoint.routers.AccountScopeCommandRouterFragment")
)

/**
 * The command router of an activity, created with it.
 */
internal object CommandRouterConstructorFingerprint : Fingerprint(
    classFingerprint = CommandRouterFragmentFingerprint,
    name = "<init>",
    parameters = listOf("Landroid/app/Activity;", "L", "Ljava/util/concurrent/Executor;"),
    filters = listOf(
        methodCall(smali = "Ljava/lang/Object;-><init>()V")
    )
)

/**
 * Runs one command with its context, by the command router itself if no other router handles the command.
 */
@io.github.nexalloy.morphe.StrictQuery
internal object CommandRouterExecuteCommandFingerprint : Fingerprint(
    classFingerprint = CommandRouterFragmentFingerprint,
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "this",
            parameters = listOf("L", "Ljava/util/Map;"),
            returnType = "V"
        )
    )
)

internal val commandDefaultInstance = io.github.nexalloy.morphe.findFieldDirect {
    val command = CommandRouterExecuteCommandFingerprint().paramTypes.first()
    command.fields.single { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == command }
}
