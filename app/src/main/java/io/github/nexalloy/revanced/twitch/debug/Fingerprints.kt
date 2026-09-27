package io.github.nexalloy.revanced.twitch.debug

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

private fun buildConfigFlagFingerprint(methodName: String) = fingerprint {
    returns("Z")
    methodMatcher { name = methodName }
    classMatcher { className(".BuildConfigUtil", StringMatchType.EndsWith) }
}

val isDebugConfigEnabledFingerprint = buildConfigFlagFingerprint("isDebugConfigEnabled")
val isOmVerificationEnabledFingerprint = buildConfigFlagFingerprint("isOmVerificationEnabled")
val shouldShowDebugOptionsFingerprint = buildConfigFlagFingerprint("shouldShowDebugOptions")
