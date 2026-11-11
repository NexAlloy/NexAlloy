package io.github.nexalloy.morphe.shared.misc.debugging

import app.morphe.extension.shared.patches.EnableDebuggingPatch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.atLast
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch

fun enableDebuggingPatch(
    block: PatchExecutor.() -> Unit = {},
    hookIndexedBooleanFeatureFlag: PatchExecutor.() -> Boolean,
    hookStringFeatureFlag: PatchExecutor.() -> Boolean,
    hookLongFeatureFlag: PatchExecutor.() -> Boolean,
    hookDoubleFeatureFlag: PatchExecutor.() -> Boolean,
    hotFeatureFlags: PatchExecutor.() -> List<java.lang.reflect.Method>,
    preferenceScreen: BasePreferenceScreen.Screen,
    additionalDebugPreferences: List<BasePreference> = emptyList()
) = patch(
    name = "Enable debugging",
    description = "Adds options for debugging and exporting Morphe logs to the clipboard.",
) {
    block()

    val preferences = mutableSetOf<BasePreference>(
        ListPreference("morphe_debug_level"),
    )

    preferences.addAll(additionalDebugPreferences)

    preferences.addAll(
        listOf(
            SwitchPreference("morphe_debug_toast_on_error"),
            NonInteractivePreference(
                "morphe_debug_export_logs",
                tag = app.morphe.extension.shared.settings.preference.ExportLogToClipboardPreference::class.java,
                selectable = true
            ),
            NonInteractivePreference(
                "morphe_debug_feature_flags_manager",
                tag = app.morphe.extension.shared.settings.preference.FeatureFlagsManagerPreference::class.java,
                selectable = true
            )
        )
    )

    preferenceScreen.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_debug_screen",
            sorting = Sorting.UNSORTED,
            preferences = preferences,
        )
    )

    // Hook the methods that look up if a feature flag is active.
    ::experimentalBooleanFeatureFlagFingerprint.hookMethod {
        after {
            it.result = EnableDebuggingPatch.isBooleanFeatureFlagEnabled(
                it.result as Boolean,
                it.args.atLast(2) as Long
            )
        }
    }

    if (hookIndexedBooleanFeatureFlag()) {
        ::experimentalIndexedBooleanFeatureFlagFingerprint.hookMethod {
            after {
                it.result = EnableDebuggingPatch.isBooleanFeatureFlagEnabled(
                    it.result as Boolean,
                    it.args.atLast(2) as Long
                )
            }
        }
    }

    if (hookDoubleFeatureFlag()) {
        ::experimentalDoubleFeatureFlagFingerprint.hookMethod {
            after {
                it.result = EnableDebuggingPatch.isDoubleFeatureFlagEnabled(
                    it.result as Double,
                    it.args.atLast(2) as Long,
                    it.args.atLast(1) as Double
                )
            }
        }
    }

    if (hookLongFeatureFlag()) {
        ::experimentalLongFeatureFlagFingerprint.memberOrNull?.hookMethod {
            after {
                it.result = EnableDebuggingPatch.isLongFeatureFlagEnabled(
                    it.result as Long,
                    it.args.atLast(2) as Long,
                    it.args.atLast(1) as Long
                )
            }
        }
    }

    if (hookStringFeatureFlag()) {
        ::experimentalStringFeatureFlagFingerprint.memberOrNull?.hookMethod {
            after {
                it.result = EnableDebuggingPatch.isStringFeatureFlagEnabled(
                    it.result as String,
                    it.args.atLast(2) as Long,
                    it.args.atLast(1) as String
                )
            }
        }
    }

    hotFeatureFlags().forEach { method ->
        val flagIndex = method.parameterTypes.indexOf(Long::class.javaPrimitiveType)
        check(flagIndex >= 0)
        method.hookMethod {
            after {
                val flag = it.args[flagIndex] as Long
                it.result = when (val original = it.result) {
                    is Boolean -> EnableDebuggingPatch.isBooleanFeatureFlagEnabled(original, flag)
                    is Double -> EnableDebuggingPatch.isDoubleFeatureFlagEnabled(original, flag, 0.0)
                    is Long -> EnableDebuggingPatch.isLongFeatureFlagEnabled(original, flag, 0L)
                    is String -> EnableDebuggingPatch.isStringFeatureFlagEnabled(original, flag, "")
                    else -> original
                }
            }
        }
    }

    BootstrapStoreReadFingerprint.hookMethod {
        after {
            it.result = EnableDebuggingPatch.isBootstrapFeatureFlagEnabled(it.result as Long, it.args[0] as Int)
        }
    }

    // There exists other experimental accessor methods for byte[]
    // and wrappers for obfuscated classes, but currently none of those are hooked.
}
