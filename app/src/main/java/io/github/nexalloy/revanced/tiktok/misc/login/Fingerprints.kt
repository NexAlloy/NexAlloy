package io.github.nexalloy.revanced.tiktok.misc.login

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val enableForcedLoginFingerprint = fingerprint {
    methodMatcher { name = "enableForcedLogin" }
    classMatcher { className(".MandatoryLoginService", StringMatchType.EndsWith) }
}

val shouldShowForcedLoginFingerprint = fingerprint {
    methodMatcher { name = "shouldShowForcedLogin" }
    classMatcher { className(".MandatoryLoginService", StringMatchType.EndsWith) }
}

val googleAuthAvailableFingerprint = fingerprint {
    returns("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    classMatcher { descriptor = "Lcom/bytedance/lobby/google/GoogleAuth;" }
}

val googleOneTapAuthAvailableFingerprint = fingerprint {
    returns("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    classMatcher { descriptor = "Lcom/bytedance/lobby/google/GoogleOneTapAuth;" }
}
