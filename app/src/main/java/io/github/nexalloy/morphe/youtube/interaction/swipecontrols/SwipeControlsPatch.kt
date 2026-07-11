package io.github.nexalloy.morphe.youtube.interaction.swipecontrols

import app.morphe.extension.shared.settings.preference.ColorPickerWithOpacitySliderPreference
import app.morphe.extension.shared.settings.preference.SeekBarPreference
import app.morphe.extension.youtube.settings.preference.SwipeZonePreference
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_34_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.mainActivityClass
import io.github.nexalloy.patch

val SwipeControls = patch(
    name = "Swipe controls",
    description = "Adds options to enable and configure volume and brightness swipe controls.",
) {
    dependsOn(
        PlayerTypeHook,
    )

//    if (!is_20_34_or_greater) {
//        PreferenceScreen.SWIPE_CONTROLS.addPreferences(
//            SwitchPreference("morphe_swipe_change_video", summary = true)
//        )
//    }

    PreferenceScreen.SWIPE_CONTROLS.addPreferences(
        SwitchPreference("morphe_swipe_brightness", summary = true),
        SwitchPreference("morphe_swipe_volume", summary = true),
        SwitchPreference("morphe_swipe_speed", summary = true),
        NonInteractivePreference(
            key = "morphe_swipe_zone_width",
            tag = SeekBarPreference::class.java,
            // Upstream inflates this from XML where AttributeSet handles 'selectable'.
            // NexAlloy instantiates via reflection, applying 'selectable' AFTER the constructor.
            // Aligning with SeekBar's init() default to avoid patching extension code.
            selectable = true,
        ),
        NonInteractivePreference(
            key = "morphe_swipe_speed_zone_height",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        NonInteractivePreference(
            key = "morphe_swipe_zone_preview",
            summaryKey = null,
            tag = SwipeZonePreference::class.java,
        ),
        NonInteractivePreference(
            key = "morphe_swipe_brightness_sensitivity",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        NonInteractivePreference(
            key = "morphe_swipe_volume_sensitivity",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        NonInteractivePreference(
            key = "morphe_swipe_speed_sensitivity",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        SwitchPreference("morphe_swipe_press_to_engage", summary = true),
        SwitchPreference("morphe_swipe_haptic_feedback"),
        SwitchPreference("morphe_swipe_save_and_restore_brightness", summary = true),
        SwitchPreference("morphe_swipe_lowest_value_enable_auto_brightness", summary = true),
        ListPreference("morphe_swipe_overlay_style"),
        NonInteractivePreference(
            key = "morphe_swipe_overlay_background_opacity",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        TextPreference("morphe_swipe_overlay_progress_brightness_color",
            tag = ColorPickerWithOpacitySliderPreference::class.java,
            inputType = InputType.TEXT_CAP_CHARACTERS),
        TextPreference("morphe_swipe_overlay_progress_volume_color",
            tag = ColorPickerWithOpacitySliderPreference::class.java,
            inputType = InputType.TEXT_CAP_CHARACTERS),
        TextPreference("morphe_swipe_overlay_progress_speed_color",
            tag = ColorPickerWithOpacitySliderPreference::class.java,
            inputType = InputType.TEXT_CAP_CHARACTERS),
        NonInteractivePreference(
            key = "morphe_swipe_text_overlay_size",
            tag = SeekBarPreference::class.java,
            selectable = true,
        ),
        TextPreference("morphe_swipe_overlay_timeout", inputType = InputType.NUMBER),
        TextPreference("morphe_swipe_threshold", inputType = InputType.NUMBER),
    )

    SwipeControlsHostActivity.hookActivity(::mainActivityClass.clazz)

    if (!is_20_34_or_greater) {
        ::featureFlagCheck.hookMethod {
            after {
                if (it.args[0] == 45631116L)
                    it.result = SwipeControlsHostActivity.allowSwipeChangeVideo(it.result as Boolean)
            }
        }
    }
}