/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3966
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.postimages

import android.view.MotionEvent
import app.morphe.extension.youtube.patches.PostImageDownloadPatch
import com.facebook.litho.ComponentHost
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.interaction.copytext.ComponentHostDispatchTouchEventFingerprint
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.misc.feedcards.feedVideoCardsHookPatch
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val postImageDownloadPatch = patch(
    name = "Download post images",
    description = "Adds a button and a menu item to download the images and GIFs of posts.",
) {
    dependsOn(
        elementProtoParserHookPatch,
        flyoutPatch,
        feedVideoCardsHookPatch,
    )

    PreferenceScreen.FEED.addPreferences(
        SwitchPreference("morphe_post_image_download", summary = true)
    )

    setExtensionIsPatchIncluded(PostImageDownloadPatch::class.java)

    // The element of a post includes the urls of its images and the texts it shows.
    hookElement(PostImageDownloadPatch::onElementLoaded)

    // The download button is drawn on the overlay of the root host of the post,
    // so the touches of the root host decide if it was tapped.
    ComponentHostDispatchTouchEventFingerprint.hookMethod {
        before {
            if (PostImageDownloadPatch.onComponentHostTouch(
                    it.thisObject as ComponentHost, it.args[0] as MotionEvent
                )
            ) {
                it.result = true
            }
        }
    }
}
