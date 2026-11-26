package io.github.nexalloy.morphe.shared.misc.litho.relayout

import android.graphics.drawable.Drawable
import android.view.View
import app.morphe.extension.shared.patches.LithoRelayoutPatch
import app.morphe.extension.shared.patches.LithoRelayoutPatch.ComponentTreeInterface
import app.morphe.extension.shared.patches.LithoRelayoutPatch.LithoViewInterface
import io.github.nexalloy.createProxy
import io.github.nexalloy.patch
import java.util.WeakHashMap

/**
 * Adds support to force Litho views to calculate their layout again,
 * such as to show texts that changed after the views were laid out.
 */
val lithoRelayoutPatch = patch(
    description = "Adds support to force Litho views to calculate their layout again."
) {
    val lithoViewClass = LithoViewOnMeasureFingerprint.declaredClass
    val forceLayoutField = ::lithoForceLayoutField.field
    val unmountAllItems = LithoViewUnmountAllItemsFingerprint.method
    val componentTree = ::lithoComponentTreeField.field
    val layoutAsync = ComponentTreeSetRootAndSizeSpecFingerprint.method
    val adapters = WeakHashMap<View, LithoViewInterface>()
    val componentTreeAdapters = WeakHashMap<Any, ComponentTreeInterface>()
    LithoRelayoutPatch.setLithoViewAdapter { view ->
        if (!lithoViewClass.isInstance(view)) null
        else adapters.getOrPut(view) {
            view.createProxy { reference ->
                object : LithoViewInterface {
                    override fun patch_getComponentTree(): ComponentTreeInterface? {
                        val tree = reference.get()?.let(componentTree::get) ?: return null
                        // The extension counts relayouts by adapter identity.
                        return componentTreeAdapters.getOrPut(tree) {
                            tree.createProxy { treeReference ->
                                ComponentTreeInterface {
                                    treeReference.get()?.let {
                                        layoutAsync.invoke(it, null, -1, -1, true, null, 3, null, null, false, true)
                                    }
                                }
                            }
                        }
                    }

                    override fun patch_forceRelayout() {
                        val host = reference.get() ?: return
                        forceLayoutField.setBoolean(host, true)
                        host.requestLayout()
                    }

                    override fun patch_forceRemount() {
                        val host = reference.get() ?: return
                        unmountAllItems(host)
                        host.requestLayout()
                    }
                }
            }
        }
    }

    // Remember the Litho views, so all shown views can be mounted again.
    LithoViewOnMeasureFingerprint.hookMethod {
        before { LithoRelayoutPatch.onLithoViewMeasured(it.thisObject as View) }
    }

    // Remember the mounted texts that are laid out again when outdated.
    val textField = ::lithoMountedTextField.field
    LithoTextMountFingerprint.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            val drawable = it.args[1] as Drawable
            LithoRelayoutPatch.onLithoTextMounted(drawable, textField.get(drawable) as? CharSequence)
        }
    }
}
