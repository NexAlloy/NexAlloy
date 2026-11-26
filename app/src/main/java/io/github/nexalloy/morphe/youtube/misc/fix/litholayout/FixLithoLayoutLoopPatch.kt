/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/5331
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.misc.fix.litholayout

import app.morphe.extension.youtube.patches.FixLithoLayoutLoopPatch
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_51_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_23_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_28_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.util.concurrent.ConcurrentHashMap

/**
 * A/B tests can move a layout between threads or measure list items again in a loop.
 * Keep those experiments off and keep the layout and item-release fixes on.
 */
internal val fixLithoLayoutLoopPatch = patch(
    description = "Fixes the high CPU usage of Litho layouts that are calculated again in a loop.",
) {
    dependsOn(VersionCheck)

    insertLiteralOverride(45532131L) // Layouts on the main thread.
    insertLiteralOverride(45650902L) // Interruptible layouts.
    if (is_21_28_or_greater) insertLiteralOverride(45807884L) // Measure after each layout.
    if (is_21_23_or_greater) insertLiteralOverride(45795775L, true) // Remove released item heights.
    if (is_20_51_or_greater) insertLiteralOverride(45708674L, true) // Detach only the view's own tree.
    // When available, the host flag enables both a concurrent map and a null-safe height lookup.
    insertLiteralOverride(45866719L, true)

    ComponentTreeUpdateStateFingerprint.hookMethod(scopedHook(::stateUpdateLoopMessageMethod.method) {
        before {
            if (it.args[1] == STATE_UPDATE_LOOP_MESSAGE) {
                FixLithoLayoutLoopPatch.onStateUpdateLoop(it.args[0] as String?)
            }
        }
    })

    val heightsField = ::recyclerItemHeightsField.field
    RecyclerBinderConstructorFingerprint.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            @Suppress("UNCHECKED_CAST")
            val heights = heightsField.get(it.thisObject) as? HashMap<Any, Int> ?: return@after
            heightsField.set(it.thisObject, RecyclerItemHeights(heights))
        }
    }

    // Keep the same lookup result if the main thread removes the height between containsKey and get.
    ItemMeasureListenerFingerprint.hookMethod {
        before {
            it.setObjectExtra("previousHeightLookup", heightLookup.get())
            heightLookup.set(HeightLookup())
        }
        after {
            val previous = it.getObjectExtra("previousHeightLookup") as? HeightLookup
            if (previous == null) heightLookup.remove() else heightLookup.set(previous)
        }
    }
}

private val heightLookup = ThreadLocal<HeightLookup>()

private class HeightLookup {
    var map: RecyclerItemHeights? = null
    var key: Any? = null
    var height: Int? = null
}

private class RecyclerItemHeights(heights: Map<Any, Int>) : ConcurrentHashMap<Any, Int>(heights) {
    override fun containsKey(key: Any): Boolean {
        val lookup = heightLookup.get() ?: return super.containsKey(key)
        val height = super.get(key)
        lookup.map = if (height == null) null else this
        lookup.key = key
        lookup.height = height
        return height != null
    }

    override fun get(key: Any): Int? {
        val lookup = heightLookup.get()
        if (lookup?.map === this && lookup.key === key) {
            lookup.map = null
            return lookup.height
        }
        return super.get(key)
    }
}
