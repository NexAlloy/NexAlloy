package io.github.nexalloy.revanced.twitch.ad.audio

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val audioAdsPresenterPlayFingerprint = fingerprint {
    methodMatcher { name = "playAd" }
    classMatcher { className(".AudioAdsPlayerPresenter", StringMatchType.EndsWith) }
}
