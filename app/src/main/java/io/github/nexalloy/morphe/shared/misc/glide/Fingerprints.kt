/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4477
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.shared.misc.glide

import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.string

/**
 * Glide RequestBuilder.into(Target, RequestListener, BaseRequestOptions, Executor).
 */
@TargetApp("youtube")
private object GlideRequestBuilderIntoFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("You must call #load() before calling #into()")
    )
)

/**
 * Glide RequestBuilder.loadGeneric(Object), which all load(...) methods use to set the image model.
 */
@TargetApp("youtube")
internal object GlideRequestBuilderLoadGenericFingerprint : Fingerprint(
    classFingerprint = GlideRequestBuilderIntoFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/Object;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_BOOLEAN,
            definingClass = "this",
            location = MatchAfterWithin(3)
        )
    )
)
