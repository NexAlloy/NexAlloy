package io.github.nexalloy.revanced.truecaller.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide Truecaller ads",
    description = "Blocks after-call and Neo caller-ID ad update entry points when present.",
) {
    ::afterCallMaybeUpdateAdFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)

    ::neoAfterCallMaybeUpdateAdFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
