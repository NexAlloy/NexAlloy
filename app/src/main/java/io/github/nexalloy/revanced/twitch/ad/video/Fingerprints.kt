package io.github.nexalloy.revanced.twitch.ad.video

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val adsManagerPlayAdsFingerprint = fingerprint {
    methodMatcher { name = "playAds" }
    classMatcher { className(".AdsManagerImpl", StringMatchType.EndsWith) }
}

val contentConfigShowAdsFingerprint = fingerprint {
    methodMatcher { name = "getShowAds" }
    classMatcher { className(".ContentConfigData", StringMatchType.EndsWith) }
}
