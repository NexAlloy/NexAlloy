/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3582
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.layout.playlistautoplay

import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.hookMethod

/**
 * Hooks the method that starts navigation to another video.
 * The extension method receives the navigation intent enum and returns true to cancel the navigation.
 */
internal fun PatchExecutor.navigationIntentHook(
    callback: (Enum<*>) -> Boolean,
    includePlayerButtons: Boolean = false
) {
    val enumField = ::navigationIntentField.field
    val methods = ::navigationIntentMethods.dexMethodList +
            if (includePlayerButtons) listOf(NavigationPlayerButtonsFingerprint.dexMethod) else emptyList()
    methods.forEach { method ->
        method.hookMethod {
            before {
                if (callback(enumField.get(it.args[0]) as Enum<*>)) {
                    it.result = null
                }
            }
        }
    }
}
