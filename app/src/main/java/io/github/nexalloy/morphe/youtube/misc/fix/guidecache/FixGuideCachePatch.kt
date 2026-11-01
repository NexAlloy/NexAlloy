package io.github.nexalloy.morphe.youtube.misc.fix.guidecache

import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_28_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_31_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

internal val fixGuideCachePatch = patch(
    description = "Fixes startup guide responses not being cached."
) {
    dependsOn(VersionCheck)
    if (!is_21_28_or_greater || is_21_31_or_greater) return@patch

    // Backport the unconditional account copy at this check only.
    ::guideAccountCheckMethods.dexMethodList.forEach { accountCheck ->
        GuideResponseAccountFingerprint.hookMethod(scopedHook(accountCheck.toMethod()) {
            after {
                if (it.args[0] == 1073830184) it.result = true
            }
        })
    }
}
