package io.github.nexalloy.morphe.shared.misc.litho.filter

import app.morphe.extension.shared.patches.components.Filter
import app.morphe.extension.shared.patches.components.LithoFilterPatch
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.Patch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.nio.ByteBuffer

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
 * The identifier is used to filter components by its identifier.
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
 * class SomeOtherClass {
 *    // Called before ComponentContextParser.parseComponent() method.
 *    public void someOtherMethod(ByteBuffer byteBuffer) {
 *        ExtensionClass.setProtoBuffer(byteBuffer); // Inserted by this patch.
 *        ...
 *   }
 * }
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
 * @param hookNonNativeBuffer Whether to also insert the non-native ByteBuffer hook.
 *                            Older client versions push data through a non-native path; newer ones
 *                            (YouTube 20.22+, YT Music 9.x) always use the native Upb encode path.
 *                            Evaluated lazily inside execute so version flags are already set.
 * @param overrideUpbFeatureFlag Whether to override the A/B feature flag that enables Upb-native
 *                               protobuf parsing (only present on older YouTube; absent in Music).
 * @param block Callback to add app-specific dependencies (sharedExtensionPatch, conversionContextPatch,
 *              versionCheckPatch, and any app-specific fixes).
 */
internal fun sharedLithoFilterPatch(
    hookNonNativeBuffer: () -> Boolean,
    overrideUpbFeatureFlag: () -> Boolean,
    useLegacyLithoFiltering: () -> Boolean,
    block: PatchExecutor.() -> Unit,
): Patch = patch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components."
) {
    block()


    LithoFilterPatch::class.java.getDeclaredMethod("useLegacyLithoFiltering")
        .hookMethod(XC_MethodReplacement.returnConstant(useLegacyLithoFiltering()))

    if (hookNonNativeBuffer()) {
        ProtobufBufferReferenceFingerprint.hookMethod {
            before { param ->
                LithoFilterPatch.setProtoBuffer(param.args[1] as ByteBuffer)
            }
        }
    }

    val buttonViewModelFrames = ThreadLocal<MutableList<Any?>>()
    ComponentCreateFingerprint.hookMethod(scopedHook(::buttonViewModelReceiver.method) {
        before {
            val frames = buttonViewModelFrames.get() ?: return@before
            if (frames.isNotEmpty()) {
                frames[frames.lastIndex] = it.args[0]
            }
        }
    })

    ComponentCreateFingerprint.hookMethod {
        val emptyComponentBuilder = EmptyComponentBuilderFingerprint.method
        val emptyComponentField = emptyComponentBuilder.returnType.declaredFields
            .single()
            .apply { isAccessible = true }
        val protoBufferEncodeMethod = ProtobufBufferEncodeFingerprint.method
        val protoBufferEncodeClass = ProtobufBufferEncodeFingerprint.declaredClass
        val accessibilityIdMethod = ::AccessibilityIdMethod.method
        val accessibilityTextMethod = ::accessibilityTextMethod.method
        before {
            val frames = buttonViewModelFrames.get()
                ?: mutableListOf<Any?>().also(buttonViewModelFrames::set)
            frames.add(null)
        }
        after { param ->
            val frames = buttonViewModelFrames.get()
            val buttonViewModel = if (!frames.isNullOrEmpty()) {
                frames.removeAt(frames.lastIndex)
            } else {
                null
            }
            if (frames.isNullOrEmpty()) {
                buttonViewModelFrames.remove()
            }
            if (param.hasThrowable()) return@after

            val conversion = param.args[1]
            val bufferParent = param.args[2]
            val buffer = if (protoBufferEncodeClass.isInstance(bufferParent)) {
                protoBufferEncodeMethod.invoke(bufferParent) as ByteArray?
            } else byteArrayOf()
            val accessibilityId = buttonViewModel?.let { accessibilityIdMethod.invoke(it) as String? }
            val accessibilityText = buttonViewModel?.let { accessibilityTextMethod.invoke(it) as String? }

            if (LithoFilterPatch.isFiltered(
                    ConversionContext(conversion),
                    buffer,
                    accessibilityId,
                    accessibilityText
                )
            ) {
                val emptyComponentBuilderResult = emptyComponentBuilder.invoke(null, param.args[0])
                param.result = emptyComponentField.get(emptyComponentBuilderResult)
            }
        }
    }

    ::lithoThreadExecutorFingerprint.hookMethod {
        before {
            it.args[0] = LithoFilterPatch.getExecutorCorePoolSize(it.args[0] as Int)
            it.args[1] = LithoFilterPatch.getExecutorMaxThreads(it.args[1] as Int)
        }
    }

    if (overrideUpbFeatureFlag()) {
        insertLiteralOverride(45419603L)
    }
}