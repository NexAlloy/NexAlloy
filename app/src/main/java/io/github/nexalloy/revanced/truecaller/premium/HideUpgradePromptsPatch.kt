package io.github.nexalloy.revanced.truecaller.premium

import android.app.Activity
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val HideUpgradePrompts = patch(
    name = "Hide Truecaller upgrade prompts",
    description = "Immediately closes Truecaller's full-screen upgrade/paywall activity without changing subscription entitlements.",
    use = false,
) {
    ::fullScreenPaywallOnCreateFingerprint.memberOrNull?.hookMethod {
        before { param ->
            (param.thisObject as? Activity)?.finish()
            param.result = null
        }
    }
}
