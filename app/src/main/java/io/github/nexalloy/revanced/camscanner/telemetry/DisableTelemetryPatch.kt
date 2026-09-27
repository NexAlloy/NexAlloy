package io.github.nexalloy.revanced.camscanner.telemetry

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableTelemetry = patch(
    name = "Disable CamScanner telemetry",
    description = "Disables CamScanner's custom LogAgent telemetry pipeline.",
) {
    ::isSkipLoggingFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(true))

    ::logAgentRecordFingerprints.dexMethodList.forEach { method ->
        method.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
}
