package io.github.nexalloy.morphe.music.misc.debugging

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.shared.misc.debugging.hotFeatureFlag

@get:RequireAppVersion("9.37.000")
val hotBooleanFeatureFlag = hotFeatureFlag("Z")

@get:RequireAppVersion("9.37.000")
val hotDoubleFeatureFlag = hotFeatureFlag("D")

@get:RequireAppVersion("9.37.000")
val hotLongFeatureFlag = hotFeatureFlag("J")
