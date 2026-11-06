 /*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3592
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.copytext

import android.view.MotionEvent
import app.morphe.extension.youtube.patches.CopyTextPatch
import com.facebook.litho.ComponentHost
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val copyTextPatch = patch(
    name = "Copy text",
    description = "Adds options to copy the video title and comments by tapping and holding them."
) {
    dependsOn(lithoSpannableStringPatch, VideoInformationPatch)

    PreferenceScreen.GENERAL.addPreferences(
        SwitchPreference("morphe_copy_video_title", summary = true),
        SwitchPreference("morphe_copy_comments", summary = true),
        SwitchPreference("morphe_copy_select_text", summary = true)
    )

    // Verify stubbed classes are not obfuscated.
    classLoader.loadClass("com.facebook.litho.TextContent")

    // The shown texts are recognized by the path of their component,
    // and the title can be auto-translated, so it is taken from the text that is drawn.
    hookLithoSpannableString(CopyTextPatch::onLithoTextLoaded)

    // The texts are drawn by Litho and have no view of their own,
    // so the host that draws them is found by its text when touched.
    ComponentHostDispatchTouchEventFingerprint.hookMethod {
        before {
            CopyTextPatch.onComponentHostTouch(it.thisObject as ComponentHost, it.args[0] as MotionEvent)
        }
    }
}
