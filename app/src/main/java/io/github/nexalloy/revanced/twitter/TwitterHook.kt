package io.github.nexalloy.revanced.twitter

import io.github.nexalloy.revanced.twitter.links.BlockRedirectToXLite
import io.github.nexalloy.revanced.twitter.links.ClearTrackingParams
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture
import io.github.nexalloy.revanced.twitter.timeline.HidePromotedTimelineEntries

val TwitterPatches = arrayOf(
    AllowScreenCapture,
    HidePromotedTimelineEntries,
    ClearTrackingParams,
    BlockRedirectToXLite,
)
