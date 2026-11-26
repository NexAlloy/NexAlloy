package io.github.nexalloy.revanced.photomath.misc.unlock.bookpoint

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.StringComparisonType.CONTAINS
import io.github.nexalloy.morphe.string

internal object IsBookpointEnabledFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf(
        string("NoGeoData", CONTAINS),
        string("NoCountryInGeo", CONTAINS),
        string("RemoteConfig", CONTAINS),
        string("GeoRCMismatch", CONTAINS),
    ),
)
