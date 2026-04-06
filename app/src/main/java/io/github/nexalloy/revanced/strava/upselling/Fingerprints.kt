package io.github.nexalloy.revanced.strava.upselling

import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getModulesFingerprint = fingerprint {
    opcodes(Opcode.IGET_OBJECT)
    methodMatcher { name = "getModules" }
    classMatcher { className(".GenericLayoutEntry", StringMatchType.EndsWith) }
}
