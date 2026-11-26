package io.github.nexalloy.revanced.photomath.misc.unlock.plus

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.StringComparisonType.CONTAINS
import io.github.nexalloy.morphe.string

internal object IsPlusUnlockedFingerprint : Fingerprint(
    definingClass = "/User;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf(string("genius", CONTAINS)),
)
