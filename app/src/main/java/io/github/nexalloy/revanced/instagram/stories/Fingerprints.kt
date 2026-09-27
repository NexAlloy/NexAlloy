package io.github.nexalloy.revanced.instagram.stories

import io.github.nexalloy.morphe.fingerprint

val storyFlippingMethodFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/Object;")
    strings("userSession")
    classMatcher {
        descriptor = "Linstagram/features/stories/fragment/ReelViewerFragment;"
    }
}
