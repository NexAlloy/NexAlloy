package io.github.nexalloy.revanced.truecaller.analytics

import io.github.nexalloy.morphe.fingerprint

val cleverTapPushEventFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/String;")
    methodMatcher { name = "push" }
    classMatcher { descriptor = "Lkr0/k;" }
}

val cleverTapPushEventWithPropsFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/String;", "Ljava/util/Map;")
    methodMatcher { name = "push" }
    classMatcher { descriptor = "Lkr0/k;" }
}
