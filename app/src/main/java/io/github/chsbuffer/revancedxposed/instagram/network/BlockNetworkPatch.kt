package io.github.chsbuffer.revancedxposed.instagram.network

import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.revancedxposed.hookMethod
import io.github.chsbuffer.revancedxposed.patch
import java.net.URI

val BlockNetwork = patch(
    name = "Block ads and analytics",
    description = "Blocks ads and analytics network requests.",
) {
    ::networkInterceptorFingerprint.member.hookMethod {
        before {
            val obj = it.args[0] ?: return@before
            val uriFieldName = obj.javaClass.declaredFields
                .firstOrNull { it.type == URI::class.java }?.name ?: return@before
            val uri = XposedHelpers.getObjectField(obj, uriFieldName) as? URI ?: return@before
            val path = uri.path ?: return@before
            val host = uri.host ?: ""

            val block = path.contains("profile_ads/get_profile_ads/") ||
                path.contains("/async_ads/") ||
                path.contains("/feed/injected_reels_media/") ||
                path == "/api/v1/ads/graphql/" ||
                host.contains("graph.instagram.com") ||
                host.contains("graph.facebook.com") ||
                path.contains("/logging_client_events")

            if (block) {
                Logger.printDebug { "Blocked: $host$path" }
                XposedHelpers.setObjectField(obj, uriFieldName,
                    URI("https", "0.0.0.0", "/0", null))
            }
        }
    }
}
