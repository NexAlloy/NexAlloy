package io.github.nexalloy.morphe.youtube.misc.litho.filter

import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.litho.filter.sharedLithoFilterPatch
import io.github.nexalloy.morphe.youtube.misc.litho.rendernext.disableRenderNextPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_06_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_15_or_greater
import io.github.nexalloy.morphe.youtube.misc.verticalscroll.fixVerticalScrollPatch

val LithoFilter = sharedLithoFilterPatch(
    // Flag was removed in 21.15+.
    overrideUpbFeatureFlag = { !is_21_15_or_greater },
    removeFilteredListItems = true,
    hasReorderedListSections = { is_21_06_or_greater },
) {
    dependsOn(
        fixVerticalScrollPatch,
        VersionCheck,
        conversionContextPatch,
        disableRenderNextPatch,
    )
}
