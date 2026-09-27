package io.github.nexalloy.revanced.truecaller.misc

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableAppStartTelemetry = patch(
    name = "Disable Truecaller app-start telemetry",
    description = "Prevents AppStartTracker from enabling technical lifecycle telemetry.",
) {
    ::appStartTrackerFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)
}

val DisableUpdateCheck = patch(
    name = "Disable Truecaller update nag",
    description = "Disables the in-app update trigger. This does not block Play Store updates.",
    use = false,
) {
    ::shouldTriggerUpdateFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}
