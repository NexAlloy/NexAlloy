package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import android.widget.TextView
import app.morphe.extension.youtube.patches.NavigationBarPatch
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextEndpoint.GUIDE
import io.github.nexalloy.morphe.youtube.misc.contexthook.ClientContextHook
import io.github.nexalloy.morphe.youtube.misc.contexthook.addOSNameHook
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
    dependsOn(NavigationBarHook, VersionCheck, ClientContextHook)

    val navPreferences = mutableSetOf(
        SwitchPreference("morphe_hide_home_button"),
        SwitchPreference("morphe_hide_shorts_button"),
        SwitchPreference("morphe_hide_create_button"),
        SwitchPreference("morphe_hide_subscriptions_button"),
        SwitchPreference("morphe_hide_notifications_button"),
        SwitchPreference("morphe_swap_create_with_notifications_button", summary = true),
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

    NavigationBarPatch::class.java.getDeclaredMethod("isPatchIncluded").hookMethod {
        before { it.result = true }
    }
    addOSNameHook(GUIDE, NavigationBarPatch::swapCreateWithNotificationButton)

    CreatePivotBarFingerprint.hookMethod(
        scopedHook(
            DexMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V").toMethod()
        ) {
            before { param ->
                if (param.thisObject !== outerParam.args[1]) return@before
                NavigationBarPatch.hideNavigationButtonLabels(param.thisObject as TextView)
            }
        }
    )

    hookNavigationButtonCreated.add { button, view ->
        NavigationBarPatch.navigationTabCreated(button, view)
    }

    insertLiteralOverride(45400535L, NavigationBarPatch::useTranslucentNavigation)
    insertLiteralOverride(45632194L, NavigationBarPatch::useTranslucentNavigation)
    insertLiteralOverride(45630927L, NavigationBarPatch::useTranslucentNavigation)

    if (is_20_46_or_greater) {
        insertLiteralOverride(45736608L, NavigationBarPatch::allowCollapsingToolbarLayout)
    }

    insertLiteralOverride(45680008L, NavigationBarPatch::useAnimatedNavigationButtons)

    if (is_20_31_or_greater) {
        listOf(
            AutoHideNavigationBarOnFeedScrollingFingerprint,
            AutoHideNavigationBarOnDismissMiniplayerFingerprint,
        ).forEach {
            it.hookMethod {
                before { param ->
                    if (NavigationBarPatch.disableAutoHidingNavigationBar()) {
                        param.result = null
                    }
                }
            }
        }
    }
}
