package io.github.nexalloy.revanced.tiktok.interaction.downloads

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

private fun aclCommonShareFingerprint(methodName: String) = fingerprint {
    returns("I")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    methodMatcher { name = methodName }
    classMatcher { className(".ACLCommonShare", StringMatchType.EndsWith) }
}

val aclCommonShareCodeFingerprint = aclCommonShareFingerprint("getCode")
val aclCommonShareShowTypeFingerprint = aclCommonShareFingerprint("getShowType")
val aclCommonShareTranscodeFingerprint = aclCommonShareFingerprint("getTranscode")
