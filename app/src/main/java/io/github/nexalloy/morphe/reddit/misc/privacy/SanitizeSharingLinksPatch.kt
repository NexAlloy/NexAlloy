package io.github.nexalloy.morphe.reddit.misc.privacy

import app.morphe.extension.reddit.patches.SanitizeSharingLinksPatch
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Adds an option to sanitize sharing links by removing tracking query parameters."
) {
    SanitizeSharingLinksPatch::class.java.getDeclaredMethod("isPatchIncluded").hookMethod {
        before { it.result = true }
    }

    ShareLinkFormatterFingerprint.hookMethod {
        before {
            if (SanitizeSharingLinksPatch.stripQueryParameters()) {
                it.result = it.args[0]
            }
        }
    }
}
