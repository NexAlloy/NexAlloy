package io.github.nexalloy.morphe.youtube.misc.debugging

import io.github.nexalloy.morphe.shared.misc.debugging.enableDebuggingPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_40_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_41_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_35_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_42_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen

val EnableDebugging = enableDebuggingPatch(
    block = {
        dependsOn(VersionCheck)
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
        SwitchPreference(
            "morphe_debug_protobuffer",
            summary = true
        )
    )
)
