package io.github.nexalloy.revanced.tiktok.interaction.looping

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val StopVideoLooping = patch(
    name = "Stop TikTok video looping",
    description = "Forces TikTok videos to stop at the end instead of automatically looping.",
    use = false,
) {
    ::videoEngineSetLoopingFingerprint.memberOrNull?.hookMethod {
        before { param ->
            if (param.args.isNotEmpty()) param.args[0] = false
        }
    }
}
