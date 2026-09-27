package io.github.nexalloy.revanced.hexeditor.ad

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val adsDisabledFingerprint = fingerprint {
    returns("Z")
    methodMatcher { name = "isAdsDisabled" }
    classMatcher {
        className(".PreferencesHelper", StringMatchType.EndsWith)
    }
}
