package io.github.nexalloy.revanced.busuu

import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val BusuuPatches = arrayOf(
    AllowScreenCapture,
    DisableCommonAnalytics,
    BlockCommonDisplayAds,
)
