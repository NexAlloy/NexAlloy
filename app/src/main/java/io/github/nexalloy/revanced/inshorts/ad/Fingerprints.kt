package io.github.nexalloy.revanced.inshorts.ad

import io.github.nexalloy.morphe.fingerprint

val inshortsAdsFingerprint = fingerprint {
    returns("V")
    strings("GoogleAdLoader", "exception in requestAd")
}
