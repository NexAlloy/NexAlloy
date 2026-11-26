package io.github.nexalloy.revanced.strava.upselling

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode

internal object GetModulesFingerprint : Fingerprint(
    definingClass = "/GenericLayoutEntry;",
    name = "getModules",
    filters = listOf(opcode(Opcode.IGET_OBJECT)),
)
