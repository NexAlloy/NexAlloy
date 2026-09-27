package io.github.nexalloy.revanced.tiktok.interaction.cleardisplay

import android.content.Context
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private const val PREFS = "nexalloy_tiktok"
private const val KEY_CLEAR_DISPLAY = "clear_display"
private const val EVENT_SWITCH_PAGE = 3
private const val EVENT_NOTIFY_EXIT = 9

private fun readBooleanField(target: Any, name: String): Boolean? =
    runCatching {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.getBoolean(target)
    }.getOrNull()

private fun readIntField(target: Any, name: String): Int? =
    runCatching {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.getInt(target)
    }.getOrNull()

val RememberClearDisplay = patch(
    name = "Remember TikTok clear display",
    description = "Remembers TikTok's clear-display state and reapplies it when the next video renders.",
    use = false,
) {
    val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val eventMethod = ::onClearDisplayEventFingerprint.memberOrNull as? java.lang.reflect.Method
    val renderMethod = ::onRenderFirstFrameBodyFingerprint.memberOrNull

    eventMethod?.hookMethod {
        before { param ->
            val event = param.args.firstOrNull() ?: return@before
            val eventType = readIntField(event, "LIZIZ") ?: return@before
            if (eventType == EVENT_SWITCH_PAGE || eventType == EVENT_NOTIFY_EXIT) return@before

            val enabled = readBooleanField(event, "LIZ") ?: return@before
            prefs.edit().putBoolean(KEY_CLEAR_DISPLAY, enabled).apply()
        }
    }

    if (eventMethod != null && renderMethod != null) {
        val eventClass = eventMethod.parameterTypes.firstOrNull()
        val constructor = eventClass?.declaredConstructors?.firstOrNull { ctor ->
            val types = ctor.parameterTypes
            types.size == 4 &&
                types[0] == Boolean::class.javaPrimitiveType &&
                types[1] == Int::class.javaPrimitiveType &&
                types[2] == String::class.java &&
                types[3] == String::class.java
        }?.apply { isAccessible = true }

        val postMethod = eventClass?.methods?.firstOrNull {
            it.name == "post" && it.parameterTypes.isEmpty()
        }

        if (constructor != null && postMethod != null) {
            renderMethod.hookMethod {
                before {
                    if (!prefs.getBoolean(KEY_CLEAR_DISPLAY, false)) return@before

                    runCatching {
                        val event = constructor.newInstance(true, 0, "", "long_press")
                        postMethod.invoke(event)
                    }
                }
            }
        }
    }
}
