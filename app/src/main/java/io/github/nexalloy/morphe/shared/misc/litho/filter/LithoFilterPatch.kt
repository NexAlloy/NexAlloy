package io.github.nexalloy.morphe.shared.misc.litho.filter

import app.morphe.extension.shared.patches.components.Filter
import app.morphe.extension.shared.patches.components.LithoFilterPatch
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.Patch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.createProxy
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.shared.misc.litho.context.commandEventDataDecorators
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

fun addLithoFilter(filter: Filter){
    LithoFilterPatch.addFilter(filter)
}

/**
 * Shared Litho component filter factory used by both YouTube and YT Music.
 *
 * The following patch inserts a hook into the method that parses the bytes into a ComponentContext.
 * This method contains a StringBuilder object that represents the pathBuilder of the component.
 * The pathBuilder is used to filter components by their path.
 *
 * Additionally, the method contains a reference to the component's identifier.
 * The identifier is used to filter components by their identifier.
 *
 * The protobuf buffer is passed along from a different injection point before the filtering occurs.
 * The buffer is a large byte array that represents the component tree.
 * This byte array is searched for strings that indicate the current component.
 *
 * All modifications done here must allow all the original code to still execute
 * even when filtering, otherwise memory leaks or poor app performance may occur.
 *
 * The following pseudocode shows how this patch works:
 *
 * class ComponentContextParser {
 *    public Component parseComponent() {
 *        ...
 *
 *        if (extensionClass.shouldFilter()) {  // Inserted by this patch.
 *            return emptyComponent;
 *        }
 *        return originalUnpatchedComponent; // Original code.
 *    }
 * }
 *
 * @param overrideUpbFeatureFlag Whether to override the A/B feature flag that enables Upb-native
 *                               protobuf parsing (only present on older YouTube; absent in Music).
 * @param removeFilteredListItems Remove fully filtered elements from their backing lists.
 * @param block Callback to add app-specific dependencies (sharedExtensionPatch, conversionContextPatch,
 *              versionCheckPatch, and any app-specific fixes).
 */
internal fun sharedLithoFilterPatch(
    overrideUpbFeatureFlag: () -> Boolean,
    removeFilteredListItems: Boolean,
    block: PatchExecutor.() -> Unit,
): Patch = patch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components."
) {
    block()

    // region Hook the method that parses bytes into a ComponentContext.

    // Return an EmptyComponent instead of the original component if the filterState method returns true.

    val buttonViewModelThreadLocal = ThreadLocal<Any?>()
    ComponentCreateFingerprint.hookMethod(scopedHook(::buttonViewModelReceiver.method) {
        before {
            buttonViewModelThreadLocal.set(it.args[0])
        }
    })

    ComponentCreateFingerprint.hookMethod {
        val emptyComponentClazz = ::emptyComponentClass.clazz
        val protoBufferEncodeMethod = ProtobufBufferEncodeFingerprint.method
        val protoBufferEncodeClass = ProtobufBufferEncodeFingerprint.declaredClass
        val accessibilityIdMethod = ::AccessibilityIdMethod.method
        val accessibilityTextMethod = ::accessibilityTextMethod.method
        after { param ->
            val conversion = param.args[1]
            val bufferParent = param.args[2]
            // Verify it's the expected subclass just in case.
            val buffer = if (protoBufferEncodeClass.isInstance(bufferParent)) {
                protoBufferEncodeMethod(bufferParent) as ByteArray?
            } else byteArrayOf()
            val buttonViewModel = buttonViewModelThreadLocal.get()
            buttonViewModelThreadLocal.remove()
            val accessibilityId = buttonViewModel?.let { accessibilityIdMethod(it) as String? }
            val accessibilityText = buttonViewModel?.let { accessibilityTextMethod(it) as String? }

            if (LithoFilterPatch.isFiltered(
                    ConversionContext(conversion),
                    buffer,
                    accessibilityId,
                    accessibilityText
                )
            ) {
                param.result = emptyComponentClazz.new()
            }
        }
    }

    //endregion

    // region Remove the items of the lists whose whole element is filtered.

    if (removeFilteredListItems) {
        val decorators = ::commandEventDataDecoratorsMethod.method
        val dataField = ::listItemDecoratorDataField.field
        val itemField = ::listItemDataField.field
        commandEventDataDecorators = { context ->
            (decorators.invoke(context) as? List<*>)?.map { decorator ->
                if (dataField.declaringClass.isInstance(decorator)) {
                    object : LithoFilterPatch.ListItemDecoratorInterface {
                        override fun patch_getListItem(): Any? =
                            dataField.get(decorator)?.let { itemField.get(it) }
                    }
                } else decorator
            }
        }

        // Keep the observable lists, to remove the items from them.
        ObservableListConstructorFingerprint.hookMethod {
            after { LithoFilterPatch.addObservableList(it.thisObject as List<*>) }
        }

        val itemsField = ::listSectionItemsField.field
        val removeItem = ListSectionRemoveItemFingerprint.method
        val isGrid = ::listSectionIsGridMethod.method
        val layOutRows = ::listSectionLayOutRowsMethod.method
        val updateSection = ::listSectionUpdateMethod.method
        val proxyKey = "lithoListSection"
        removeItem.declaringClass.declaredConstructors.forEach { constructor ->
            constructor.hookMethod {
                after {
                    if (XposedHelpers.getAdditionalInstanceField(it.thisObject, proxyKey) != null) return@after
                    val proxy = it.thisObject.createProxy { impl ->
                        LithoFilterPatch.ListSectionInterface { item ->
                            val section = impl.get() ?: return@ListSectionInterface false
                            val items = itemsField.get(section) as List<*>
                            val contains = items.contains(item)
                            if (contains) {
                                val wasGrid = isGrid.invoke(section) as Boolean
                                removeItem.invoke(section, item)
                                if (wasGrid && !(isGrid.invoke(section) as Boolean) &&
                                    (itemsField.get(section) as List<*>).isNotEmpty()) {
                                    layOutRows.invoke(section)
                                    updateSection.invoke(section)
                                }
                            }
                            contains
                        }
                    }
                    XposedHelpers.setAdditionalInstanceField(it.thisObject, proxyKey, proxy)
                    LithoFilterPatch.addListSection(proxy)
                }
            }
        }
        ListSectionReleaseFingerprint.hookMethod {
            before {
                val proxy = XposedHelpers.getAdditionalInstanceField(it.thisObject, proxyKey)
                    as? LithoFilterPatch.ListSectionInterface
                if (proxy != null) LithoFilterPatch.removeListSection(proxy)
            }
        }
    }

    // endregion

    if (removeFilteredListItems) {
        val alpha = ::stickyHeaderAlphaField.field
        StickyHeaderRestoreItemFingerprint.hookMethod {
            before { if (alpha.getFloat(it.thisObject) == 0f) alpha.setFloat(it.thisObject, 1f) }
        }

        val listField = ::stickyHeaderListField.field
        val indexField = ::stickyHeaderIndexField.field
        val entriesField = ::stickyHeaderEntriesField.field
        val positionField = ::stickyHeaderPositionField.field
        val savedPosition = ::stickyHeaderSavedPositionField.field
        val ownerField = ::stickyHeaderOwnerField.field
        ::stickyHeaderCreateMethod.hookMethod {
            before {
                val owner = ownerField.get(it.thisObject)
                // The same R8 runnable may also handle unrelated tasks.
                if (!listField.declaringClass.isInstance(owner)) return@before
                val list = listField.get(owner)
                val index = indexField.getInt(list)
                if (index < 0) return@before
                val entries = entriesField.get(list) as List<*>
                it.setObjectExtra("stickyHeaderSavedPosition", savedPosition.getInt(it.thisObject))
                // Update every use, including RecyclerView's lookup of the original item.
                savedPosition.setInt(it.thisObject, positionField.getInt(entries[index]))
            }
            after {
                (it.getObjectExtra("stickyHeaderSavedPosition") as? Int)?.let { original ->
                    savedPosition.setInt(it.thisObject, original)
                }
            }
        }
    }

    // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube.

    LithoThreadExecutorFingerprint.hookMethod {
        before {
            it.args[0] = LithoFilterPatch.getExecutorCorePoolSize(it.args[0] as Int)
            it.args[1] = LithoFilterPatch.getExecutorMaxThreads(it.args[1] as Int)
        }
    }

    // endregion

    // region A/B test of new Litho native code.

    // Turn off a feature flag that enables native code of protobuf parsing (Upb protobuf).
    // If this is enabled, then the litho protobuffer hook will always show an empty buffer
    // since it's no longer handled by the hooked Java code.
    if (overrideUpbFeatureFlag()) {
        insertLiteralOverride(45419603L)
    }

    // endregion
}
