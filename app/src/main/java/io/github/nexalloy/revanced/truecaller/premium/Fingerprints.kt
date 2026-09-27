package io.github.nexalloy.revanced.truecaller.premium

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val fullScreenPaywallOnCreateFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/os/Bundle;")
    methodMatcher { name = "onCreate" }
    classMatcher {
        className(".FullScreenPaywallActivity", StringMatchType.EndsWith)
    }
}
