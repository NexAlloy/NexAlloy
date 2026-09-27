package io.github.nexalloy.revanced.hexeditor.ad

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val DisableAds = patch(
    name = "Disable ads",
    description = "Disables Hex Editor ads by forcing its ad-disable preference check on.",
) {
    ::adsDisabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}
