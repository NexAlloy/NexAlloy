package io.github.nexalloy.revanced.strava.subscription

import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getSubscribedFingerprint = fingerprint {
    opcodes(Opcode.IGET_BOOLEAN)
    classMatcher { className(".SubscriptionDetailResponse", StringMatchType.EndsWith) }
    methodMatcher { name = "getSubscribed" }
}