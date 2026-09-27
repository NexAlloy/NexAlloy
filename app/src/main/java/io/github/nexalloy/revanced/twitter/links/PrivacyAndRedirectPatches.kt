package io.github.nexalloy.revanced.twitter.links

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val ClearTrackingParams = patch(
    name = "Clear X sharing tracking parameters",
    description = "Stops X from appending the session tracking token to shared links.",
) {
    ::addSessionTokenFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val original = param.args.firstOrNull() as? String ?: return@before
            param.result = original
        }
    }
}

val BlockRedirectToXLite = patch(
    name = "Block redirecting to X Lite",
    description = "Prevents the client-side flag path that redirects existing users to the X Lite interface.",
) {
    ::redirectToXLiteFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}
