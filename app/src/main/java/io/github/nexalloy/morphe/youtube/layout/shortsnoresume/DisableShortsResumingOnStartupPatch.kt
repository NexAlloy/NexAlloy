package io.github.nexalloy.morphe.youtube.layout.shortsnoresume

import app.morphe.extension.youtube.patches.DisableShortsResumingOnStartupPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_03_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val DisableShortsResumingOnStartup = patch(
    name = "Disable Shorts resuming on startup",
    description = "Adds an option to disable Shorts from resuming on app startup when Shorts were last being watched.",
) {
    PreferenceScreen.SHORTS.addPreferences(
        SwitchPreference("morphe_disable_shorts_resuming_on_startup"),
    )

    if (is_21_03_or_greater) {
        UserWasInShortsEvaluateFingerprint.hookMethod(
            scopedHook(::userWasInShortsEvaluateConstructor.member) {
                before { constructorParam ->
                    if (innerDepth != 0) return@before
                    constructorParam.args[1] =
                        DisableShortsResumingOnStartupPatch.disableShortsResumingOnStartup(
                            constructorParam.args[1] as Boolean
                        )
                }
            }
        )
    } else {
        UserWasInShortsListenerFingerprint.hookMethod(
            scopedHook(::userWasInShortsBooleanValueMethod.member) {
                after {
                    it.result =
                        DisableShortsResumingOnStartupPatch.disableShortsResumingOnStartup(it.result as Boolean)
                }
            }
        )
    }

    insertLiteralOverride(
        45358360L,
        DisableShortsResumingOnStartupPatch::disableShortsResumingOnStartup
    )
}
