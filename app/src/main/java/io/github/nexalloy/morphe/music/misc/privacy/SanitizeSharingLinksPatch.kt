package io.github.nexalloy.morphe.music.misc.privacy

import io.github.nexalloy.patch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.privacy.SanitizeSharingLinks

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links."
) {
    // NexAlloy: the shared Intent.putExtra hook already sanitizes the system share-sheet link.
    SanitizeSharingLinks(
        preferenceScreen = PreferenceScreen.MISC,
        replaceMusicLinksWithYouTube = true
    )
}
