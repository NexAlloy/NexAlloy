package io.github.nexalloy.revanced.tiktok.misc.share

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun stripQuery(url: String): String {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return url
    val query = url.indexOf('?')
    return if (query > 0) url.substring(0, query) else url
}

val SanitizeShareUrls = patch(
    name = "Sanitize TikTok sharing links",
    description = "Removes TikTok tracking query parameters before shared links leave the app.",
) {
    ::shareUrlTrackerFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val url = param.args
                .filterIsInstance<String>()
                .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                ?: return@before

            param.result = stripQuery(url)
        }
    }
}
