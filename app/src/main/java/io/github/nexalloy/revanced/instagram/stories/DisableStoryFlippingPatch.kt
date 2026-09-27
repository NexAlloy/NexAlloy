package io.github.nexalloy.revanced.instagram.stories

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableStoryFlipping = patch(
    name = "Disable Instagram story flipping",
    description = "Stops the ReelViewer story transition method from automatically advancing to the next story.",
    use = false,
) {
    ::storyFlippingMethodFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
