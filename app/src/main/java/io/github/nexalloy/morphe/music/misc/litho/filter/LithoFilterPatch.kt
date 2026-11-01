package io.github.nexalloy.morphe.music.misc.litho.filter

import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.litho.filter.sharedLithoFilterPatch

val lithoFilterPatch = sharedLithoFilterPatch(
    // YT Music does not ship the Upb feature flag.
    overrideUpbFeatureFlag = { false },
    removeFilteredListItems = false
) {
    dependsOn(conversionContextPatch)
}
