package io.github.nexalloy.morphe.youtube.misc.debugging

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.shared.misc.debugging.hotFeatureFlag

@get:RequireAppVersion("21.35.000")
val hotBooleanFeatureFlag = hotFeatureFlag("Z")

@get:RequireAppVersion("21.35.000")
val hotDoubleFeatureFlag = hotFeatureFlag("D")

@get:RequireAppVersion("21.35.000")
val hotLongFeatureFlag = hotFeatureFlag("J")

@get:RequireAppVersion("21.35.000")
val hotStringFeatureFlag = hotFeatureFlag("Ljava/lang/String;")
