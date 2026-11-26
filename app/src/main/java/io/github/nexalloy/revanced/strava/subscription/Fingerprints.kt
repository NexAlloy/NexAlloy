package io.github.nexalloy.revanced.strava.subscription

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode

internal object GetSubscribedFingerprint : Fingerprint(
    definingClass = "/SubscriptionDetailResponse;",
    name = "getSubscribed",
    filters = listOf(opcode(Opcode.IGET_BOOLEAN)),
)
