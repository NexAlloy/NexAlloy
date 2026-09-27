package io.github.nexalloy.revanced.tiktok.ad

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val feedItemListCloneFingerprint = fingerprint {
    methodMatcher { name = "clone" }
    classMatcher { className(".FeedItemList", StringMatchType.EndsWith) }
}

val convertHelpFeedItemListFingerprint = fingerprint {
    methodMatcher {
        addUsingField { name = "preloadAds" }
    }
    classMatcher { className(".ConvertHelp", StringMatchType.EndsWith) }
}
