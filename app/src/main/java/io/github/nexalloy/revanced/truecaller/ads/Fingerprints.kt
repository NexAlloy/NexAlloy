package io.github.nexalloy.revanced.truecaller.ads

import io.github.nexalloy.morphe.fingerprint

val afterCallMaybeUpdateAdFingerprint = fingerprint {
    returns("V")
    parameters("Z")
    methodMatcher { name = "Rh" }
    classMatcher { descriptor = "Lcom/truecaller/acs/ui/baz;" }
}

val neoAfterCallMaybeUpdateAdFingerprint = fingerprint {
    returns("V")
    parameters("Z")
    methodMatcher { name = "Th" }
    classMatcher { descriptor = "Ltw1/f;" }
}
