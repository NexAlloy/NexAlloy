package io.github.nexalloy.morphe.youtube.misc.debugging

import app.morphe.extension.youtube.patches.SavePushNotificationsPatch
import app.morphe.extension.youtube.settings.preference.SavedPushNotificationsPreference
import io.github.nexalloy.morphe.shared.misc.debugging.enableDebuggingPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_40_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_41_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_35_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_42_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.observePushNotification
import io.github.nexalloy.morphe.youtube.shared.pushNotificationHookPatch

val EnableDebugging = enableDebuggingPatch(
    block = {
        dependsOn(VersionCheck, pushNotificationHookPatch)
        observePushNotification(
            SavePushNotificationsPatch::savePushNotificationBytes
        )
    },
    hookIndexedBooleanFeatureFlag = { is_21_42_or_greater },
    hookStringFeatureFlag = { true },
    // 20.40 has changes not worth supporting.
    hookLongFeatureFlag = { !is_20_40_or_greater || is_20_41_or_greater },
    hookDoubleFeatureFlag = { !is_20_40_or_greater || is_20_41_or_greater },
    hotFeatureFlags = {
        if (is_21_35_or_greater) listOf(::hotBooleanFeatureFlag.method, ::hotDoubleFeatureFlag.method, ::hotLongFeatureFlag.method, ::hotStringFeatureFlag.method)
        else emptyList()
    },
    preferenceScreen = PreferenceScreen.MISC,
    additionalDebugPreferences = listOf(
        ListPreference("morphe_debug_litho"),
        SwitchPreference("morphe_debug_save_push_notifications", summary = true),
        NonInteractivePreference(
            "morphe_debug_saved_push_notifications",
            tag = SavedPushNotificationsPreference::class.java,
            selectable = true
        )
    )
)
