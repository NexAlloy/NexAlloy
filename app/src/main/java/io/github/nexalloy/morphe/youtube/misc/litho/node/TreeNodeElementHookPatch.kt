package io.github.nexalloy.morphe.youtube.misc.litho.node

import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.identifierFieldData
import io.github.nexalloy.patch

/**
 * Hooks the tree node result list to allow filtering lazily converted elements.
 *
 * Morphe injects a call before the return statement of the method (METHOD_MID).
 * Xposed: uses an after hook on the method — the return value IS the list,
 * and the ConversionContext is args[1].
 */

val treeNodeResultHooks = mutableListOf<(String, MutableList<Any>) -> Unit>()

/**
 * Register a handler to be called when a lazily converted element list is loaded.
 *
 * @param handler receives the identifier string and the mutable list of elements.
 *                The handler can modify the list in-place to filter elements.
 */
fun hookTreeNodeResult(handler: (String, MutableList<Any>) -> Unit) {
    treeNodeResultHooks.add(handler)
}

val TreeNodeElementHook = patch(
    description = "Hooks the tree node element lists to the extension."
) {
    dependsOn(LithoFilter)

    val identifierField = ::identifierFieldData.field

    TreeNodeResultListFingerprint.hookMethod {
        after {
            @Suppress("UNCHECKED_CAST")
            val list = it.result as? MutableList<Any> ?: return@after
            if (list.isEmpty()) return@after
            if (list[0].toString() != "LazilyConvertedElement") return@after

            // ConversionContext is p2 (args index 1).
            val conversionContext = it.args[1]
            val identifier = identifierField.get(conversionContext) as? String
            if (identifier.isNullOrEmpty()) return@after

            treeNodeResultHooks.forEach { hook -> hook(identifier, list) }
        }
    }
}
