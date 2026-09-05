package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import android.widget.TextView
import app.morphe.extension.youtube.patches.NavigationBarPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
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
//        SwitchPreference("morphe_hide_navigation_bar"),        // TODO addBottomBarContainerHook
//        SwitchPreference("morphe_narrow_navigation_buttons", summary = true),  // TODO PivotBarChanged/PivotBarStyle METHOD_MID
        SwitchPreference("morphe_hide_navigation_button_labels"),
        SwitchPreference("morphe_navigation_bar_animations", summary = true),
        SwitchPreference("morphe_disable_translucent_navigation", summary = true)
    )

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
    // setExtensionIsPatchIncluded(NavigationBarPatch::class.java)

    // Alternative: scopedHook on AutoMotiveFeatureMethod.
    ::addCreateButtonViewFingerprint.hookMethod(scopedHook(::AutoMotiveFeatureMethod.member) {
        before { param ->
            param.result =
                NavigationBarPatch.swapCreateWithNotificationButton("") == "Android Automotive"
        }
    })

    // Hide navigation button labels.
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
    // Translucent status bar.
    insertLiteralOverride(45400535L, NavigationBarPatch::useTranslucentNavigation)
    // Translucent system buttons feature flag.
    insertLiteralOverride(45632194L, NavigationBarPatch::useTranslucentNavigation)
    // Translucent navigation bar buttons feature flag.
    insertLiteralOverride(45630927L, NavigationBarPatch::useTranslucentNavigation)

    if (is_20_46_or_greater) {
        // Feature interferes with translucent status bar and must be forced off.
        insertLiteralOverride(45736608L, NavigationBarPatch::allowCollapsingToolbarLayout)
    }

    // Animated navigation tabs.
    insertLiteralOverride(45680008L, NavigationBarPatch::useAnimatedNavigationButtons)

    // TODO Narrow navigation buttons

    // disableAutoHidingNavigationBar

    if (is_20_31_or_greater) {
        listOf(
            AutoHideNavigationBarOnFeedScrollingFingerprint,
            AutoHideNavigationBarOnDismissMiniplayerFingerprint,
        ).forEach {
            it.hookMethod {
                before { param ->
                    if (NavigationBarPatch.disableAutoHidingNavigationBar()){
                        param.result = null
                    }
                }
            }
        }
    }

    // TODO upper navigation toolbar
}
