package io.github.nexalloy.revanced.twitch.chat.antidelete

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val deletedMessageClickableSpanCtorFingerprint = fingerprint {
    returns("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    classMatcher { className(".DeletedMessageClickableSpan", StringMatchType.EndsWith) }
}

val setHasModAccessFingerprint = fingerprint {
    returns("V")
    methodMatcher { name = "setHasModAccess" }
    classMatcher { className(".DeletedMessageClickableSpan", StringMatchType.EndsWith) }
}
