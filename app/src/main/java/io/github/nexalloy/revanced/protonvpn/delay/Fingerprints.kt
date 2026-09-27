package io.github.nexalloy.revanced.protonvpn.delay

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val longDelayFingerprint = fingerprint {
    returns("I")
    methodMatcher { name = "getChangeServerLongDelayInSeconds" }
    classMatcher { className(".AppConfigResponse", StringMatchType.EndsWith) }
}

val shortDelayFingerprint = fingerprint {
    returns("I")
    methodMatcher { name = "getChangeServerShortDelayInSeconds" }
    classMatcher { className(".AppConfigResponse", StringMatchType.EndsWith) }
}
