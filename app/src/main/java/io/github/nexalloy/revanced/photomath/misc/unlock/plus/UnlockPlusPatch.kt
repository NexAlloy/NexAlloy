package io.github.nexalloy.revanced.photomath.misc.unlock.plus

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.photomath.misc.unlock.bookpoint.EnableBookpoint

val UnlockPlus = patch(
    name = "Unlock plus",
) {
    dependsOn(EnableBookpoint)
    ::isPlusUnlockedFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}