package io.github.nexalloy.revanced.strava

import io.github.nexalloy.revanced.strava.subscription.UnlockSubscription
import io.github.nexalloy.revanced.strava.upselling.DisableSubscriptionSuggestions

val StravaPatches = arrayOf(
    UnlockSubscription,
    DisableSubscriptionSuggestions
)