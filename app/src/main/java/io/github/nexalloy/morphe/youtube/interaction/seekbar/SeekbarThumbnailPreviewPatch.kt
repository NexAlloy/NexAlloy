/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2182
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.seekbar

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.LruCache
import android.widget.ImageView
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import app.morphe.extension.youtube.patches.SeekbarThumbnailPreviewPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.layout.sponsorblock.seekbarOnDrawFingerprint
import io.github.nexalloy.morphe.youtube.misc.chapters.chaptersHookPatch
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_12_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_21_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val seekbarThumbnailPreviewPatch = patch(
    description = "Adds an option to restore the seekbar thumbnail preview."
) {
    dependsOn(VersionCheck, chaptersHookPatch, PlayerTypeHook, VideoInformationPatch)

    PreferenceScreen.SEEKBAR.addPreferences(
        SwitchPreference("morphe_seekbar_thumbnail_preview")
    )

    val updatePoint = ::updateSeekbarPointMethod.method

    // To show the thumbnail during the seeking straight on seekbar.
    ::seekbarHandlerOnTouchMethod.hookMethod {
        before {
            val point = Point()
            updatePoint(it.thisObject, point)
            SeekbarThumbnailPreviewPatch.updateHandlerThumbnailPreview(
                it.thisObject as View, it.args[0] as MotionEvent, point
            )
        }
    }

    // To show the thumbnail during the use of slide to seek feature.
    val controllerField = ::slideSeekbarControllerField.field
    val modelField = ::slideSeekbarModelField.field
    val viewField = ::slideSeekbarViewField.field
    SlideSeekbarHandlerOnTouchFingerprint.hookMethod {
        before {
            val seekbar = viewField.get(modelField.get(controllerField.get(it.thisObject)))
            val point = Point()
            updatePoint(seekbar, point)
            SeekbarThumbnailPreviewPatch.updateSlideThumbnailPreview(
                it.args[0] as View, it.args[1] as MotionEvent, point
            )
        }
    }

    SeekbarFineScrubbingBitmapFingerprint.hookMethod {
        before { SeekbarThumbnailPreviewPatch.setFineScrubbingPreviewBitmap(it.args[0] as? Bitmap) }
    }

    // The app cuts storyboard frames with the frame size of the storyboard spec,
    // which can be smaller than the frames of the storyboard image.
    StoryboardFrameRegionFingerprint.hookMethod(scopedHook(
        LruCache::class.java.getDeclaredMethod("get", Any::class.java)
    ) {
        after {
            if (it.result is BitmapRegionDecoder) {
                outerParam.setObjectExtra("storyboardDecoder", it.result)
            }
        }
    })
    StoryboardFrameRegionFingerprint.hookMethod(scopedHook(::storyboardFrameRectMethod.method) {
        after {
            val decoder = outerParam.getObjectExtra("storyboardDecoder") as? BitmapRegionDecoder ?: return@after
            if (!it.hasThrowable()) {
                SeekbarThumbnailPreviewPatch.fixStoryboardFrameRect(it.result as Rect, decoder)
            }
        }
    })

    // The full screen seek preview fits in the player, also when the video is zoomed to fill the screen.
    // Scale it to the video when it is shown.
    val bigBoardImage = ::bigBoardImageField.field
    SeekbarBigBoardsScrubFingerprint.hookMethod(scopedHook(::bigBoardVisibilityMethod.method) {
        after {
            if (!it.hasThrowable() && outerParam.args[0] == 1 && it.args[1] == true) {
                SeekbarThumbnailPreviewPatch.setBigBoardScale(bigBoardImage.get(outerParam.thisObject) as? ImageView)
            }
        }
    })

    ::seekbarOnDrawFingerprint.hookMethod {
        before { SeekbarThumbnailPreviewPatch.setSeekbarRectangle(it.thisObject as View) }
    }

    if (is_21_12_or_greater) {
        SeekbarBigBoardsUpdateFingerprint
    } else {
        SeekbarBigBoardsUpdateLegacyFingerprint
    }.hookMethod {
        before {
            if (SeekbarThumbnailPreviewPatch.disableBigBoardUpdate()) it.result = false
        }
    }

    PreciseSeekingRecyclerViewFingerprint.hookMethod {
        before { SeekbarThumbnailPreviewPatch.setPreciseSeekingVisible(it.args[0] as? RecyclerView) }
    }

    if (is_21_21_or_greater) {
        insertLiteralOverride(45787901, SeekbarThumbnailPreviewPatch::disableShortsSeekbarThumbnails)
    }
    // When a Shorts player is closed, this flag detaches the seekbar from the views of the Shorts player
    // that was opened next, since both use the same id. The seekbar is then gone until the app restarts.
    insertLiteralOverride(45421233L)

}
