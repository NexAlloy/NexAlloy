package io.github.nexalloy.revanced.photomath.misc.unlock.bookpoint

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.fingerprint

val isBookpointEnabledFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    strings(
        "NoGeoData",
        "NoCountryInGeo",
        "RemoteConfig",
        "GeoRCMismatch"
    )
}