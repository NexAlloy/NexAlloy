package io.github.nexalloy.revanced.twitch.chat.autoclaim

import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch

val AutoClaimChannelPoints = patch(
    name = "Auto-claim channel points",
    description = "Automatically presses Twitch's available Channel Points claim button.",
) {
    ::communityPointsButtonViewDelegateFingerprint.hookMethod {
        after { param ->
            val delegate = param.thisObject ?: return@after
            val buttonLayout = delegate.getObjectFieldOrNull("buttonLayout") ?: return@after
            buttonLayout.callMethodOrNull("callOnClick")
        }
    }
}
