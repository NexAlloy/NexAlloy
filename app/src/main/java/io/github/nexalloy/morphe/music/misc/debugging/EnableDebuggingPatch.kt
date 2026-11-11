package io.github.nexalloy.morphe.music.misc.debugging

import io.github.nexalloy.morphe.music.misc.playservice.is_9_37_or_greater
import io.github.nexalloy.morphe.music.misc.playservice.versionCheckPatch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.debugging.enableDebuggingPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference

val EnableDebugging = enableDebuggingPatch(
    block = { dependsOn(versionCheckPatch) },
    hookIndexedBooleanFeatureFlag = { false },
    // String feature flag does not appear to be present with YT Music.
    hookStringFeatureFlag = { false },
    hookLongFeatureFlag = { true },
    hookDoubleFeatureFlag = { true },
    hotFeatureFlags = {
        if (is_9_37_or_greater) listOf(::hotBooleanFeatureFlag.method, ::hotDoubleFeatureFlag.method, ::hotLongFeatureFlag.method)
        else emptyList()
    },
    preferenceScreen = PreferenceScreen.MISC,
    additionalDebugPreferences = listOf(
        ListPreference("morphe_debug_litho")
    )
)
