package io.github.nexalloy.revanced.messenger

import io.github.nexalloy.revanced.messenger.ads.HideInboxAds
import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val MessengerPatches = arrayOf(
    AllowScreenCapture,
    HideInboxAds,
    DisableCommonAnalytics,
    BlockCommonDisplayAds,
)