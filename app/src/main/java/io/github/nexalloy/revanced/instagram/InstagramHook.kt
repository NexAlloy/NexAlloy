package io.github.nexalloy.revanced.instagram

import io.github.nexalloy.revanced.instagram.download.EnableNativeDownloads
import io.github.nexalloy.revanced.instagram.links.SanitizeShareLinks
import io.github.nexalloy.revanced.instagram.privacy.DisableScreenshotDetection
import io.github.nexalloy.revanced.instagram.stories.DisableStoryFlipping
import io.github.nexalloy.revanced.meta.ads.HideAds
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val InstagramPatches = arrayOf(
    AllowScreenCapture,
    HideAds,
    EnableNativeDownloads,
    SanitizeShareLinks,
    DisableScreenshotDetection,
    DisableStoryFlipping,
)