package io.github.nexalloy.revanced.strava.subscription

import io.github.nexalloy.patch

val UnlockSubscription = patch(
    name = "Unlock subscription features",
    description = "Unlocks \"Routes\", \"Matched Runs\" and \"Segment Efforts\".",
) {
    ::getSubscribedFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }
}