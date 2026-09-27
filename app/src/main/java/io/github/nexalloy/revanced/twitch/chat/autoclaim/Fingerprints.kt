package io.github.nexalloy.revanced.twitch.chat.autoclaim

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val communityPointsButtonViewDelegateFingerprint = fingerprint {
    returns("V")
    methodMatcher { name = "showClaimAvailable" }
    classMatcher { className(".CommunityPointsButtonViewDelegate", StringMatchType.EndsWith) }
}
