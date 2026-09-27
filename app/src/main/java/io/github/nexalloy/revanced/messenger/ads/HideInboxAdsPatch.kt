package io.github.nexalloy.revanced.messenger.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideInboxAds = patch(
    name = "Hide inbox ads",
    description = "Prevents Messenger from loading inbox advertising items.",
) {
    ::loadInboxAdsFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
