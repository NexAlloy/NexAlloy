package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import android.widget.TextView
import app.morphe.extension.youtube.patches.NavigationBarPatch
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.navigation.hookNavigationButtonCreated
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_46_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val NavigationBar = patch(
    name = "Navigation bar",
    description = "Adds options to hide and change the bottom navigation bar (such as the Shorts button)" +
            " and the upper navigation toolbar.",
) {
    dependsOn(NavigationBarHook, VersionCheck)

    val navPreferences = mutableSetOf(
        SwitchPreference("morphe_hide_home_button"),
        SwitchPreference("morphe_hide_shorts_button"),
        SwitchPreference("morphe_hide_create_button"),
        SwitchPreference("morphe_hide_subscriptions_button"),
        SwitchPreference("morphe_hide_notifications_button"),
//        SwitchPreference("morphe_show_search_button"),         // TODO PivotBarRenderer proto
//        ListPreference("morphe_show_search_button_index"),     // TODO PivotBarRenderer proto
//        SwitchPreference("morphe_show_settings_button"),       // TODO PivotBarRenderer proto
//        ListPreference("morphe_show_settings_button_index"),   // TODO PivotBarRenderer proto
//        SwitchPreference("morphe_show_settings_button_type", summary = true),  // TODO PivotBarRenderer proto
        SwitchPreference("morphe_swap_create_with_notifications_button", summary = true),
        SwitchPreference("morphe_hide_navigation_button_labels"),
//        SwitchPreference("morphe_narrow_navigation_buttons", summary = true),  // TODO PivotBarChanged/PivotBarStyle METHOD_MID
//        SwitchPreference("morphe_hide_navigation_bar"),        // TODO addBottomBarContainerHook
    )

    navPreferences += SwitchPreference("morphe_disable_translucent_navigation_bar_light", summary = true)
    navPreferences += SwitchPreference("morphe_disable_translucent_navigation_bar_dark", summary = true)

    PreferenceScreen.GENERAL.addPreferences(
        SwitchPreference("morphe_disable_translucent_status_bar", summary = true)
    )

    navPreferences += SwitchPreference("morphe_navigation_bar_animations", summary = true)

    if (is_20_31_or_greater) {
        navPreferences += SwitchPreference("morphe_disable_auto_hide_navigation_bar", summary = true)
    }

    PreferenceScreen.GENERAL.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_navigation_buttons_screen",
            sorting = Sorting.UNSORTED,
            preferences = navPreferences
        )
    )

    // Swap create with notifications button.
    // TODO Morphe uses addOSNameHook(Endpoint.GUIDE, ...) which depends on clientContextHookPatch.
    // Alternative: scopedHook on AutoMotiveFeatureMethod.
    ::addCreateButtonViewFingerprint.hookMethod(scopedHook(::AutoMotiveFeatureMethod.member) {
        before { param ->
            param.result =
                NavigationBarPatch.swapCreateWithNotificationButton("") == "Android Automotive"
        }
    })

    // Hide navigation button labels.
    // Morphe: METHOD_MID in CreatePivotBarFingerprint (instruction index manipulation).
    // Alternative: scopedHook on TextView.setText.
    CreatePivotBarFingerprint.hookMethod(scopedHook(DexMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V").toMethod()) {
        before { param ->
            NavigationBarPatch.hideNavigationButtonLabels(param.thisObject as TextView)
        }
    })

    // Hook navigation button created, in order to hide them.
    hookNavigationButtonCreated.add { button, view ->
        NavigationBarPatch.navigationTabCreated(button, view)
    }

    // TODO Hide navigation bar — addBottomBarContainerHook

    // Force on/off translucent effect on status bar and navigation buttons.
    ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
        after {
            val flagId = it.args[1] as Long
            when (flagId) {
                // Translucent status bar.
                45400535L -> it.result =
                    NavigationBarPatch.useTranslucentNavigationStatusBar(it.result as Boolean)
                // Translucent navigation buttons (YouTube nav + system buttons).
                45630927L, 45632194L -> it.result =
                    NavigationBarPatch.useTranslucentNavigationButtons(it.result as Boolean)
            }
        }
    }

    ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
        after {
            // Animated navigation tabs.
            if (it.args[1] == 45680008L) {
                it.result =
                    NavigationBarPatch.useAnimatedNavigationButtons(it.result as Boolean)
            }
        }
    }

    if (is_20_46_or_greater) {
        // Feature interferes with translucent status bar and must be forced off.
        ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
            after {
                if (it.args[1] == 45736608L) {
                    it.result =
                        NavigationBarPatch.allowCollapsingToolbarLayout(it.result as Boolean)
                }
            }
        }
    }

    // TODO Narrow navigation buttons — PivotBarChangedFingerprint/PivotBarStyleFingerprint METHOD_MID

    //
    // Navigation search and settings button
    //

    // TODO ActionBarSearchResults searchQueryViewLoaded — METHOD_MID
    // TODO PivotBarRenderer search/settings button injection — METHOD_MID (proto manipulation)
    // TODO PivotBarRendererList getPivotBarRendererList — METHOD_MID

    //
    // Toolbar
    //

//    val toolbarPreferences = mutableSetOf(
//        SwitchPreference("morphe_hide_toolbar_cast_button")
//        SwitchPreference("morphe_hide_toolbar_create_button"),        // TODO hookToolBar
//        SwitchPreference("morphe_hide_toolbar_microphone_button"),    // TODO hookToolBar
//        SwitchPreference("morphe_hide_toolbar_notification_button"),  // TODO hookToolBar
//        SwitchPreference("morphe_hide_toolbar_search_button"),        // TODO hookToolBar
//        SwitchPreference("morphe_show_toolbar_settings_button"),      // TODO SettingIntentFingerprint
//        ListPreference("morphe_show_toolbar_settings_button_index"),  // TODO SettingIntentFingerprint
//        SwitchPreference("morphe_show_toolbar_settings_button_type", summary = true)  // TODO SettingIntentFingerprint
//    )
//    if (!is_20_31_or_greater) {
//        toolbarPreferences += SwitchPreference("morphe_wide_searchbar")  // TODO SetWordmarkHeader
//    }
//
//    PreferenceScreen.GENERAL.addPreferences(
//        PreferenceScreenPreference(
//            key = "morphe_toolbar_screen",
//            sorting = Sorting.UNSORTED,
//            preferences = toolbarPreferences
//        )
//    )

    // TODO hookToolBar — depends on toolBarHookPatch
    // TODO OldSearchButtonVisibilityFingerprint — METHOD_MID
    // TODO SearchButtonsVisibilityFingerprint — METHOD_MID
    // TODO SearchResultButtonVisibilityFingerprint — METHOD_MID
    // TODO SettingIntentFingerprint — interface injection (向类添加接口)
    // TODO TopBarRendererPrimaryFilter/SecondaryFilter — METHOD_MID (proto manipulation)
    // TODO Wide searchbar (<20.31) — SetWordmarkHeader/WideSearchbarLayout METHOD_MID
}
