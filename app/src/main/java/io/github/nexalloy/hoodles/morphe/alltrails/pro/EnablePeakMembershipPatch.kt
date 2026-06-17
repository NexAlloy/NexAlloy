package io.github.nexalloy.hoodles.morphe.alltrails.pro

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val EnablePeakMembership = patch(
    name = "Enable Peak membership",
    description = "Enables app features locked behind the subscription paywall.",
) {
    IsProFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    GetSubscriptionTierFingerprint.hookMethod(XC_MethodReplacement.returnConstant("peak"))
}
