package io.github.nexalloy.revanced.meta

import io.github.nexalloy.revanced.meta.ads.HideAds
import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val MetaPatches = arrayOf(
    AllowScreenCapture,
    HideAds,
    DisableCommonAnalytics,
    BlockCommonDisplayAds,
)
