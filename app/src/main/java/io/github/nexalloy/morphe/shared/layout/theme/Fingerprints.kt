/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.shared.layout.theme

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.methodCall

@TargetApp("youtube")
@RequireAppVersion(minVersion = "21.30.000")
@StrictQuery
internal object LithoOnBoundsChangeFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(),
        filters = listOf(
            methodCall(smali = $$"Landroid/graphics/Path;->addRoundRect(Landroid/graphics/RectF;[FLandroid/graphics/Path$Direction;)V"),
            fieldAccess(
                opcode = Opcode.IPUT_OBJECT,
                definingClass = "this",
                type = "Landroid/graphics/Path;"
            )
        )
    ),
    name = "onBoundsChange",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Rect;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/graphics/Paint",
            location = MatchAfterWithin(10)
        ),
        methodCall(
            smali = "Landroid/graphics/Paint;->setColor(I)V",
            location = MatchAfterImmediately()
        )
    )
)

// YT 21.29, Music 9.29 and older.
@TargetApp("youtube")
@RequireAppVersion(maxVersion = "21.29.999")
@StrictQuery
internal object LithoOnBoundsChangeLegacyFingerprint : Fingerprint(
    name = "onBoundsChange",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Rect;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Landroid/graphics/Path;"
        ),

        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/graphics/Paint",
            location = MatchAfterWithin(10)
        ),
        methodCall(
            smali = "Landroid/graphics/Paint;->setColor(I)V",
            location = MatchAfterImmediately()
        )
    )
)

@get:TargetApp("youtube")
@get:RequireAppVersion(minVersion = "21.30.000")
val lithoPaintField = findFieldDirect {
    LithoOnBoundsChangeFingerprint.instructionMatches[0].instruction.fieldRef!!
}

@get:TargetApp("youtube")
@get:RequireAppVersion(maxVersion = "21.29.999")
val lithoPaintLegacyField = findFieldDirect {
    LithoOnBoundsChangeLegacyFingerprint.instructionMatches[1].instruction.fieldRef!!
}
