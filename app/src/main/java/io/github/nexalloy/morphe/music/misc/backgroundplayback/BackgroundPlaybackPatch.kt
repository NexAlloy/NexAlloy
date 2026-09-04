package io.github.nexalloy.morphe.music.misc.backgroundplayback

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.morphe.shared.misc.CheckRecycleBitmapMediaSession
import io.github.nexalloy.patch

val BackgroundPlayback = patch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
) {
    dependsOn(CheckRecycleBitmapMediaSession)

    BackgroundPlaybackDisableFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    KidsBackgroundPlaybackPolicyControllerFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}