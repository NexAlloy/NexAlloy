package io.github.nexalloy.revanced.truecaller.analytics

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableAnalytics = patch(
    name = "Disable Truecaller analytics",
    description = "Suppresses Truecaller's CleverTap behavioural event dispatch.",
) {
    ::cleverTapPushEventFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)

    ::cleverTapPushEventWithPropsFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
