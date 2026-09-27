package io.github.nexalloy.revanced.tiktok.interaction.speed

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val EnablePlaybackSpeed = patch(
    name = "Enable playback speed",
    description = "Enables TikTok's playback-speed control for videos when the internal capability method is present.",
) {
    ::playbackSpeedEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}
