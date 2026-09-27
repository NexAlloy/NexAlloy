package io.github.nexalloy.revanced.tiktok.interaction.seekbar

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val awemeGetVideoControlFingerprint = fingerprint {
    methodMatcher { name = "getVideoControl" }
    classMatcher { className(".Aweme", StringMatchType.EndsWith) }
}
