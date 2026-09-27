package io.github.nexalloy.revanced.tiktok.interaction.seekbar

import io.github.nexalloy.patch
import io.github.nexalloy.setBooleanField

val ShowSeekbar = patch(
    name = "Show seekbar",
    description = "Shows the video progress bar for all videos when TikTok exposes VideoControl.",
) {
    ::awemeGetVideoControlFingerprint.hookMethod {
        after { param ->
            val videoControl = param.result ?: return@after
            runCatching { videoControl.setBooleanField("showProgressBar", true) }
            runCatching { videoControl.setBooleanField("draftProgressBar", true) }
        }
    }
}
