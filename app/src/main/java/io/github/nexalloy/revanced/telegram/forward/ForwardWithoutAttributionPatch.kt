package io.github.nexalloy.revanced.telegram.forward

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findFieldRecursive
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField
import io.github.nexalloy.revanced.telegram.runtime.telegramPrefs
import io.github.nexalloy.revanced.telegram.runtime.writeTelegramField
import java.util.Collections
import java.util.WeakHashMap

private const val PREF_REMEMBER_FORWARD = "remember_forward_preset"
private const val PREF_FORWARD_MODE = "forward_preset"

private const val MODE_NORMAL = 0
private const val MODE_WITHOUT_SENDER = 1
private const val MODE_WITHOUT_CAPTION = 2
private const val MODE_WITHOUT_SENDER_AND_CAPTION = 3

private val hideCaptionByShareAlert =
    Collections.synchronizedMap(WeakHashMap<Any, Boolean>())

private val buildingShareMenu = ThreadLocal<Any?>()
private val sendingShareAlert = ThreadLocal<Any?>()
private val shareMenuItemCount = ThreadLocal.withInitial { 0 }
private val injectingMenuItem = ThreadLocal.withInitial { false }

private fun currentMode(alert: Any): Int {
    val showSender = (alert.readTelegramField("showSendersName") as? Boolean) ?: true
    val hideCaption = hideCaptionByShareAlert[alert] == true
    return (if (!showSender) MODE_WITHOUT_SENDER else 0) or
        (if (hideCaption) MODE_WITHOUT_CAPTION else 0)
}

private fun applyMode(alert: Any, mode: Int) {
    alert.writeTelegramField(
        "showSendersName",
        (mode and MODE_WITHOUT_SENDER) == 0,
    )
    hideCaptionByShareAlert[alert] =
        (mode and MODE_WITHOUT_CAPTION) != 0
}

val ForwardOptions = patch(
    name = "Enhanced forward presets",
    description = "Adds Normal, without sender, without caption, and without sender+caption forward presets with an optional remember-last-choice setting.",
) {
    val shareAlert =
        classLoader.findTelegramClassOrNull("org.telegram.ui.Components.ShareAlert")
            ?: return@patch
    val menuItemClass =
        classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenuSubItem")
            ?: return@patch
    val popupLayoutClass =
        classLoader.findTelegramClassOrNull(
            "org.telegram.ui.ActionBar.ActionBarPopupWindow\$ActionBarPopupWindowLayout"
        ) ?: return@patch
    val sendMessagesHelper =
        classLoader.findTelegramClassOrNull("org.telegram.messenger.SendMessagesHelper")
            ?: return@patch

    shareAlert.declaredMethods
        .filter { it.name == "onSendLongClick" }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    val alert = param.thisObject
                    buildingShareMenu.set(alert)
                    shareMenuItemCount.set(0)

                    val prefs = classLoader.telegramPrefs()
                    if (prefs?.getBoolean(PREF_REMEMBER_FORWARD, false) == true) {
                        applyMode(alert, prefs.getInt(PREF_FORWARD_MODE, MODE_NORMAL))
                    }
                }
                after { param ->
                    val alert = param.thisObject
                    val prefs = classLoader.telegramPrefs()
                    if (prefs?.getBoolean(PREF_REMEMBER_FORWARD, false) == true) {
                        applyMode(alert, prefs.getInt(PREF_FORWARD_MODE, MODE_NORMAL))
                    }
                    buildingShareMenu.remove()
                    shareMenuItemCount.remove()
                    injectingMenuItem.remove()
                }
            }
        }

    popupLayoutClass.declaredMethods
        .filter {
            it.name == "addView" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == View::class.java &&
                it.parameterTypes[1].name == "android.widget.LinearLayout\$LayoutParams"
        }
        .forEach { addViewMethod ->
            addViewMethod.hookMethod {
                after { param ->
                    if (injectingMenuItem.get() == true) return@after

                    val alert = buildingShareMenu.get() ?: return@after
                    val child = param.args.getOrNull(0) as? View ?: return@after
                    if (child.javaClass.name != menuItemClass.name) return@after

                    val count = shareMenuItemCount.get() + 1
                    shareMenuItemCount.set(count)
                    if (count != 2) return@after

                    runCatching {
                        val resourcesProvider =
                            child.findFieldRecursive("resourcesProvider")?.get(child)

                        val constructor = menuItemClass.declaredConstructors.first {
                            val types = it.parameterTypes
                            types.size == 5 &&
                                types[0] == Context::class.java &&
                                types[1] == Boolean::class.javaPrimitiveType &&
                                types[2] == Boolean::class.javaPrimitiveType &&
                                types[3] == Boolean::class.javaPrimitiveType
                        }.apply { isAccessible = true }

                        val density = child.resources.displayMetrics.density
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (48f * density).toInt(),
                        )

                        runCatching {
                            child.writeTelegramField("bottom", false)
                            menuItemClass.getMethod("updateBackground").invoke(child)
                        }

                        val presetRows = listOf(
                            MODE_NORMAL to "Normal forward",
                            MODE_WITHOUT_SENDER to "Forward without sender",
                            MODE_WITHOUT_CAPTION to "Forward without caption",
                            MODE_WITHOUT_SENDER_AND_CAPTION to "Forward without sender + caption",
                        )

                        val presetViews = mutableListOf<Pair<Int, View>>()

                        fun setChecked(view: View, checked: Boolean) {
                            menuItemClass.getMethod(
                                "setChecked",
                                Boolean::class.javaPrimitiveType,
                            ).invoke(view, checked)
                        }

                        fun effectiveMode(): Int {
                            val prefs = classLoader.telegramPrefs()
                            return if (prefs?.getBoolean(PREF_REMEMBER_FORWARD, false) == true) {
                                prefs.getInt(PREF_FORWARD_MODE, MODE_NORMAL)
                            } else {
                                currentMode(alert)
                            }
                        }

                        fun refreshPresets() {
                            val mode = effectiveMode()
                            presetViews.forEach { (rowMode, view) ->
                                setChecked(view, rowMode == mode)
                            }
                        }

                        injectingMenuItem.set(true)
                        try {
                            presetRows.forEach { (mode, label) ->
                                val option = constructor.newInstance(
                                    child.context,
                                    true,
                                    false,
                                    false,
                                    resourcesProvider,
                                ) as View

                                menuItemClass.getMethod(
                                    "setTextAndIcon",
                                    CharSequence::class.java,
                                    Int::class.javaPrimitiveType,
                                ).invoke(option, label, 0)

                                presetViews += mode to option
                                option.setOnClickListener {
                                    applyMode(alert, mode)
                                    val prefs = classLoader.telegramPrefs()
                                    if (prefs?.getBoolean(PREF_REMEMBER_FORWARD, false) == true) {
                                        prefs.edit().putInt(PREF_FORWARD_MODE, mode).apply()
                                    }
                                    refreshPresets()
                                }

                                addViewMethod.invoke(param.thisObject, option, layoutParams)
                            }

                            val remember = constructor.newInstance(
                                child.context,
                                true,
                                false,
                                true,
                                resourcesProvider,
                            ) as View

                            menuItemClass.getMethod(
                                "setTextAndIcon",
                                CharSequence::class.java,
                                Int::class.javaPrimitiveType,
                            ).invoke(remember, "Remember last forward preset", 0)

                            fun refreshRemember() {
                                val enabled = classLoader.telegramPrefs()
                                    ?.getBoolean(PREF_REMEMBER_FORWARD, false) == true
                                setChecked(remember, enabled)
                            }

                            refreshRemember()
                            remember.setOnClickListener {
                                val prefs = classLoader.telegramPrefs() ?: return@setOnClickListener
                                val enabled = !prefs.getBoolean(PREF_REMEMBER_FORWARD, false)
                                prefs.edit()
                                    .putBoolean(PREF_REMEMBER_FORWARD, enabled)
                                    .putInt(PREF_FORWARD_MODE, currentMode(alert))
                                    .apply()
                                refreshRemember()
                                refreshPresets()
                            }

                            addViewMethod.invoke(param.thisObject, remember, layoutParams)
                            refreshPresets()
                        } finally {
                            injectingMenuItem.set(false)
                        }
                    }
                }
            }
        }

    shareAlert.declaredMethods
        .filter {
            it.name == "sendInternal" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    sendingShareAlert.set(param.thisObject)
                }
                after {
                    sendingShareAlert.remove()
                }
            }
        }

    sendMessagesHelper.declaredMethods
        .filter {
            it.name == "sendMessage" &&
                it.parameterTypes.size >= 4 &&
                it.parameterTypes[0].name == "java.util.ArrayList" &&
                it.parameterTypes[1] == Long::class.javaPrimitiveType &&
                it.parameterTypes[2] == Boolean::class.javaPrimitiveType &&
                it.parameterTypes[3] == Boolean::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val alert = sendingShareAlert.get() ?: return@before
                    val showSender =
                        (alert.readTelegramField("showSendersName") as? Boolean) ?: true
                    param.args[2] = !showSender
                    if (hideCaptionByShareAlert[alert] == true) {
                        param.args[3] = true
                    }
                }
            }
        }
}
