package io.github.nexalloy.revanced.truecaller.ui

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val HideAssistantTab = patch(
    name = "Hide Truecaller Assistant tab",
    description = "Disables the Assistant feature gate so the Assistant tab is not added to bottom navigation.",
    use = false,
) {
    ::assistantFeatureFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}

val HideFamilyProtectionButton = patch(
    name = "Hide Truecaller Family Protection button",
    description = "Disables the Family Protection feature gate so the navigation button is not added.",
    use = false,
) {
    ::familyProtectFeatureFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}

val HideScamsTab = patch(
    name = "Hide Truecaller Scams tab",
    description = "Disables the scam-feed navigation gate when the matching Truecaller build exposes it.",
    use = false,
) {
    ::scamFeedEnabledFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}

val HidePremiumSettingsBlock = patch(
    name = "Hide Truecaller Premium settings block",
    description = "Suppresses the Premium member Compose block in settings without changing subscription state.",
    use = false,
) {
    ::premiumBlockComposeFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
