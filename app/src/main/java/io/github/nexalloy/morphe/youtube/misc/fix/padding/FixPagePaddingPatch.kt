package io.github.nexalloy.morphe.youtube.misc.fix.padding

import android.view.View
import app.morphe.extension.youtube.patches.FixPagePaddingPatch
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_07_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

internal val fixPagePaddingPatch = patch {
    dependsOn(VersionCheck)
    // A flag gives the pages the height of the navigation bar and the miniplayer in another way,
    // which leaves wrong padding in some pages, such as the incognito search. It is turned off.
    if (is_21_07_or_greater) {
        insertLiteralOverride(45724388L, false)
    }

    // The keyboard branch reads its insets before constructing the bottom-margin update.
    // Replace that margin where it is consumed, while leaving the other margins unchanged.
    PageBottomMarginFingerprint.hookMethod(scopedHook(::pageKeyboardInsetsMethod.method) {
        before { outerParam.setObjectExtra("navigationKeyboardMargin", true) }
    })
    PageBottomMarginFingerprint.hookMethod(scopedHook(::pageBottomMarginConstructor.constructor) {
        before {
            if (it.args[1] == 1 && outerParam.getObjectExtra("navigationKeyboardMargin") == true) {
                it.args[0] = FixPagePaddingPatch.getKeyboardMargin(it.args[0] as Int)
            }
        }
    })

    val setPadding = ::browseListSetPaddingMethod.method
    val listFingerprints = mutableListOf<io.github.nexalloy.morphe.Fingerprint>(BrowseListBottomPaddingFingerprint)
    if (is_20_26_or_greater) listFingerprints += FeedListPaddingFingerprint
    listFingerprints.forEach { fingerprint ->
        fingerprint.hookMethod(scopedHook(setPadding) {
            before {
                if (innerDepth == 0) {
                    it.args[3] = FixPagePaddingPatch.getAdjustedFeedListBottomPadding(it.thisObject as View, it.args[3] as Int)
                }
            }
        })
    }
}
