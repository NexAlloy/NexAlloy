package io.github.nexalloy.revanced.duolingo

import io.github.nexalloy.revanced.duolingo.ads.DisableAds
import io.github.nexalloy.revanced.duolingo.icon.DisableDynamicIcon
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val DuolingoPatches = arrayOf(
    AllowScreenCapture,
    DisableAds,
    DisableDynamicIcon,
    DisableCommonAnalytics,
)
