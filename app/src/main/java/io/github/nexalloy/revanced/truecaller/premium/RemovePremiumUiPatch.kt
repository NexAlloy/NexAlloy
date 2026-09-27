package io.github.nexalloy.revanced.truecaller.premium

import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val RemovePremiumUi = patch(
    name = "Remove Truecaller Premium UI",
    description = "Hides the Premium navigation entry and profile upgrade row without granting subscription features.",
    use = false,
) {
    ::premiumNavigationEnabledFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))

    ::premiumNavDrawerOnAttachFingerprint.memberOrNull?.hookMethod {
        after { param ->
            (param.thisObject as? View)?.visibility = View.GONE
        }
    }
}
