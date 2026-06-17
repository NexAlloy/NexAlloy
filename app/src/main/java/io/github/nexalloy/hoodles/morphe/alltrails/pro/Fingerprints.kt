package io.github.nexalloy.hoodles.morphe.alltrails.pro

import io.github.nexalloy.morphe.Fingerprint

object IsProFingerprint : Fingerprint(
    name = "isPro",
    returnType = "Z",
)

object GetSubscriptionTierFingerprint : Fingerprint(
    name = "getSubscriptionTier",
    returnType = "Ljava/lang/String;",
)
