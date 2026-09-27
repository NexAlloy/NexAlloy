package io.github.nexalloy.revanced.twitch.ad.display

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val HideDisplayAds = patch(
    name = "Hide Twitch display ads",
    description = "Returns Twitch's own no-ad result from the display-ad parser to hide banners, overlays and in-feed display ads.",
) {
    val field = runCatching { ::noAdSingletonFingerprint.field }.getOrNull()
    val singleton = runCatching {
        field?.isAccessible = true
        field?.get(null)
    }.getOrNull()

    if (singleton != null) {
        ::displayAdResponseParserFingerprint.memberOrNull?.hookMethod {
            before { param ->
                param.result = singleton
            }
        }
    }
}
