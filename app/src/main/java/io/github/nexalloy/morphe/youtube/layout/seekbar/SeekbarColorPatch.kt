/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.layout.seekbar

import app.morphe.extension.youtube.patches.theme.SeekbarColorPatch
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.layout.theme.lithoColorHookPatch
import io.github.nexalloy.morphe.shared.layout.theme.lithoColorOverrideHook
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_30_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val seekbarColorPatch = patch(
    description = "Hide the seekbar in video thumbnails",
) {
    dependsOn(VersionCheck, lithoColorHookPatch { is_21_30_or_greater })

    setExtensionIsPatchIncluded(SeekbarColorPatch::class.java)

    // Some videos have their own seekbar style, which replaces the handle with an image
    // and ignores the color of the player seekbar. The flag shows the style and draws its image.
    insertLiteralOverride(45681792L, false)

    lithoColorOverrideHook(SeekbarColorPatch::getLithoColor)

    // If hiding feed seekbar thumbnails, then turn off the gradient of the
    // watch history menu items. They use the same gradient as the player,
    // and there is no easy way to tell which of the two is being drawn.
    ::watchHistoryProgressDrawableCheckMethods.dexMethodList.forEach { method ->
        WatchHistoryMenuUseProgressDrawableFingerprint.hookMethod(scopedHook(method.toMethod()) {
            after { it.result = SeekbarColorPatch.showWatchHistoryProgressDrawable(it.result as Boolean) }
        })
    }

    LithoLinearGradientFingerprint.hookMethod {
        before {
            it.args[4] = SeekbarColorPatch.getLithoLinearGradient(it.args[4] as IntArray, it.args[5] as FloatArray)
        }
    }

    PlayerLinearGradientFingerprint.hookMethod(scopedHook(
        DexMethod("Landroid/graphics/LinearGradient;-><init>(FFFF[I[FLandroid/graphics/Shader\$TileMode;)V").toConstructor()
    ) {
        before {
            it.args[4] = SeekbarColorPatch.getPlayerLinearGradient(
                it.args[4] as IntArray, outerParam.args[0] as Int, outerParam.args[1] as Int
            )
        }
    })
}
