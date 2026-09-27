package io.github.nexalloy.morphe.reddit

import io.github.nexalloy.morphe.reddit.ad.HideAds
import io.github.nexalloy.morphe.reddit.misc.links.OpenLinksDirectly
import io.github.nexalloy.morphe.reddit.misc.links.OpenLinksExternally
import io.github.nexalloy.morphe.reddit.misc.privacy.RemoveNsfwWarning
import io.github.nexalloy.morphe.reddit.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val RedditPatches = arrayOf(
    AllowScreenCapture,
    HideAds,
    SanitizeSharingLinks,
    OpenLinksDirectly,
    OpenLinksExternally,
    RemoveNsfwWarning,
)