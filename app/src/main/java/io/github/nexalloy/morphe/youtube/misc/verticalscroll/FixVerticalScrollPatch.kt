package io.github.nexalloy.morphe.youtube.misc.verticalscroll

import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_18_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_33_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val fixVerticalScrollPatch = patch(
    description = "Fixes issues with refreshing the feed when the first component is of type EmptyComponent."
) {
    dependsOn(VersionCheck)

    if (is_21_18_or_greater) {
        // Can cause issues with scrolling.
        insertLiteralOverride(45782902L)
    }

    if (is_21_33_or_greater) {
        ScrollToRelatedItemsOnVideoEndFingerprint.hookMethod {
            before { it.result = null }
        }
    }
    FullscreenMoreVideosButtonFingerprint.hookMethod(scopedHook(::fullscreenMoreVideosPanelStateMethod.method) {
        after { it.result = true }
    })

    CanScrollVerticallyFingerprint.hookMethod(scopedHook(::canScrollVerticallyMethod.method) {
        after { it.result = false }
    })
}
