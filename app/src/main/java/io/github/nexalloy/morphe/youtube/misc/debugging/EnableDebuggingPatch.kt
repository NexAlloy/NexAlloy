package io.github.nexalloy.morphe.youtube.misc.debugging

import io.github.nexalloy.patch
import io.github.nexalloy.morphe.shared.misc.debugging.EnableDebugging
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen

val EnableDebugging = patch(
    name = "Enable debugging",
    description = "Adds options for debugging and exporting ReVanced logs to the clipboard.",
) {
    EnableDebugging(
        preferenceScreen = PreferenceScreen.MISC,
        additionalDebugPreferences = listOf(SwitchPreference("morphe_debug_protobuffer", summary = true))
    )
}
