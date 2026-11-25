package io.github.nexalloy.morphe.shared.misc.proto

import app.morphe.extension.shared.patches.ElementProtoParserPatch
import io.github.nexalloy.Patch
import io.github.nexalloy.patch


/**
 * Shared factory for the element proto parser hook used by both YouTube and YT Music.
 *
 * Hooks the generic proto parser method so that patched extensions can inspect (and modify)
 * the raw byte buffer of every parsed proto element.
 *
 * @param readsResourcesWithParser Whether inline resources can be read with this parser (YouTube 21.40+).
 */
internal fun createElementProtoParserHookPatch(
    readsResourcesWithParser: () -> Boolean = { false },
    vararg dependencies: Patch,
    reusesHookedElements: Boolean = false,
) = patch(
    description = "Hook to modify the proto message class, which can only be accessed through reflection.",
) {
    dependsOn(*dependencies)
    val resourceDepth = ThreadLocal.withInitial { 0 }
    if (readsResourcesWithParser()) {
        // Inline resources are not elements, so their parser calls have no element hooks.
        ResourceBytesFingerprint.hookMethod {
            before { resourceDepth.set(resourceDepth.get()!! + 1) }
            after { resourceDepth.set(resourceDepth.get()!! - 1) }
        }
    }

    if (reusesHookedElements) {
        ElementConverterFingerprint.hookMethod {
            before { ElementProtoParserPatch.onRenderNextCheck() }
        }
    }
    NewElementProtoParserFingerprint.hookMethod {
        after {
            if (it.hasThrowable() || resourceDepth.get()!! > 0) return@after
            val original = it.result as ByteArray?
            if (reusesHookedElements && original != null) {
                ElementProtoParserPatch.getHookedElement(original)?.let { cached ->
                    it.result = cached
                    return@after
                }
            }
            var result = original
            hooks.forEach {
                result = it(result)
            }
            if (reusesHookedElements && original != null) {
                ElementProtoParserPatch.setHookedElement(original, result)
            }
            it.result = result
        }
    }
}

private val hooks = mutableListOf<(ByteArray?) -> ByteArray?>()
fun hookElement(hook: (ByteArray?) -> ByteArray?) {
    hooks.add(hook)
}
