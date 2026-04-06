package io.github.nexalloy.revanced.photomath.misc.unlock.bookpoint

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val EnableBookpoint = patch(
    description = "Enables textbook access",
) {
    ::isBookpointEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}