package io.github.nexalloy.morphe.music.layout.buttons.navigation

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.morphe.extension.music.patches.NavigationBarPatch
import app.morphe.extension.shared.ResourceUtils
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val navigationBarPatch = patch(
    name = "Navigation bar",
    description = "Adds options to hide navigation bar, labels and buttons.",
) {
    PreferenceScreen.GENERAL.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_navigation_bar_screen",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_music_hide_navigation_bar_home_button"),
                SwitchPreference("morphe_music_hide_navigation_bar_samples_button"),
                SwitchPreference("morphe_music_hide_navigation_bar_explore_button"),
                SwitchPreference("morphe_music_hide_navigation_bar_library_button"),
                SwitchPreference("morphe_music_hide_navigation_bar_upgrade_button"),
                // TODO: Port the navigation button proto factory for the Settings button.
                // SwitchPreference("morphe_music_show_navigation_bar_settings_button"),
                // SwitchPreference("morphe_music_show_navigation_bar_settings_button_type", summary = true),
                SwitchPreference("morphe_music_hide_navigation_bar"),
                SwitchPreference("morphe_music_hide_navigation_bar_labels"),
                // SwitchPreference("morphe_music_navigation_bar_glass", summary = true),
                // // Height, width, button spacing, opacity and rim brightness, with a preview.
                // NonInteractivePreference(
                // key = "morphe_music_navigation_bar_glass_height",
                // titleKey = "morphe_music_navigation_bar_glass_size_title",
                // summaryKey = "morphe_music_navigation_bar_glass_size_summary",
                // tag = app.morphe.extension.music.settings.preference.MusicGlassNavigationBarPreference::class.java,
                // selectable = true
                // ),
                // NonInteractivePreference(
                // key = "morphe_music_navigation_bar_glass_blur",
                // tag = app.morphe.extension.shared.settings.preference.SeekBarPreference::class.java
                // ),
                // SwitchPreference("morphe_music_navigation_bar_glass_light", summary = true),
                // SwitchPreference("morphe_music_navigation_bar_glass_album_tint", summary = true),
                // SwitchPreference("morphe_music_navigation_bar_glass_hide_indicator", summary = true),
            )
        )
    )

    // PreferenceScreen.addMiniplayerPreferences(
    // SwitchPreference("morphe_music_miniplayer_glass", summary = true),
    // // Corner radius, artwork padding, rim brightness and opacity, with a preview.
    // NonInteractivePreference(
    // key = "morphe_music_miniplayer_glass_corner_radius",
    // titleKey = "morphe_music_miniplayer_glass_style_title",
    // summaryKey = "morphe_music_miniplayer_glass_style_summary",
    // tag = app.morphe.extension.music.settings.preference.MusicGlassMiniplayerPreference::class.java,
    // selectable = true
    // ),
    // NonInteractivePreference(
    // key = "morphe_music_miniplayer_glass_blur",
    // tag = app.morphe.extension.shared.settings.preference.SeekBarPreference::class.java
    // ),
    // SwitchPreference("morphe_music_miniplayer_glass_light", summary = true),
    // SwitchPreference("morphe_music_miniplayer_glass_album_tint", summary = true),
    // SwitchPreference("morphe_music_miniplayer_glass_spinning_artwork", summary = true),
    // SwitchPreference("morphe_music_miniplayer_glass_hide_progress_bar", summary = true),
    // )

    TabLayoutTextFingerprint.hookMethod(scopedHook(::navigationEnumMethod.method) {
        after { NavigationBarPatch.setLastAppNavigationEnum(it.result as Enum<*>?) }
    })

    val tabView = ::navigationTabViewField.field
    val labelId = ResourceUtils.getIdIdentifier("text1")
    val iconId = ResourceUtils.getIdIdentifier("icon")
    TabLayoutTextFingerprint.hookMethod(scopedHook(::navigationAddTabMethod.method) {
        after {
            val view = tabView.get(it.args[0]) as? View ?: return@after
            view.findViewById<ImageView>(iconId)?.let { icon ->
                val params = icon.layoutParams as LinearLayout.LayoutParams
                if (params.weight == 0f) {
                    params.weight = 0.5f
                    icon.layoutParams = params
                }
            }
            view.findViewById<TextView>(labelId)?.let(NavigationBarPatch::hideNavigationLabel)
            NavigationBarPatch.hideNavigationButton(view)
        }
    })
}
