package io.github.nexalloy.revanced.facebook

import io.github.nexalloy.revanced.facebook.ads.HideStoryAds
import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val FacebookPatches = arrayOf(
    AllowScreenCapture,
    HideStoryAds,
    DisableCommonAnalytics,
    BlockCommonDisplayAds,
)
