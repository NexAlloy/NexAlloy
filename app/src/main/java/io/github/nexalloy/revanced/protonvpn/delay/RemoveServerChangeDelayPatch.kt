package io.github.nexalloy.revanced.protonvpn.delay

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val RemoveServerChangeDelay = patch(
    name = "Remove server-change delay",
    description = "Removes the client-side waiting time between ProtonVPN server changes. It does not unlock paid servers or plans.",
) {
    ::longDelayFingerprint.hookMethod(XC_MethodReplacement.returnConstant(0))
    ::shortDelayFingerprint.hookMethod(XC_MethodReplacement.returnConstant(0))
}
