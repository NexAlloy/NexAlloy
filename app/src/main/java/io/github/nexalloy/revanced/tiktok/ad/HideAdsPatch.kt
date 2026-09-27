package io.github.nexalloy.revanced.tiktok.ad

import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

private fun clearPreloadAds(value: Any?) {
    value ?: return
    runCatching { value.setObjectField("preloadAds", null) }
}

val HideAds = patch(
    name = "Hide ads",
    description = "Clears TikTok's preloaded ad entries from feed item lists.",
) {
    ::feedItemListCloneFingerprint.hookMethod {
        after { param -> clearPreloadAds(param.result) }
    }
    ::convertHelpFeedItemListFingerprint.hookMethod {
        after { param -> clearPreloadAds(param.result) }
    }
}
