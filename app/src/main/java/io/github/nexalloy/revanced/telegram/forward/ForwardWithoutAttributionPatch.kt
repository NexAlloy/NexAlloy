package io.github.nexalloy.revanced.telegram.forward

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.util.Collections
import java.util.WeakHashMap

private val hideCaptionByShareAlert =
    Collections.synchronizedMap(WeakHashMap<Any, Boolean>())
private val combinedPresetByShareAlert =
    Collections.synchronizedMap(WeakHashMap<Any, Boolean>())

private val buildingShareMenu = ThreadLocal<Any?>()
private val sendingShareAlert = ThreadLocal<Any?>()
private val shareMenuItemCount = ThreadLocal.withInitial { 0 }
private val injectingMenuItem = ThreadLocal.withInitial { false }

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun ClassLoader.telegramString(name: String, fallback: String): CharSequence =
    runCatching {
        val stringClass = loadClass("org.telegram.messenger.R\$string")
        val id = stringClass.getField(name).getInt(null)
        val localeController = loadClass("org.telegram.messenger.LocaleController")
        localeController
            .getMethod("getString", Int::class.javaPrimitiveType)
            .invoke(null, id) as CharSequence
    }.getOrDefault(fallback)

val ForwardOptions = patch(
    name = "Enhanced forward options",
    description = "Keeps Telegram's native sender-attribution option and adds a quick forward-without-caption option to the share menu.",
) {
    val shareAlert =
        classLoader.findClassOrNull("org.telegram.ui.Components.ShareAlert")
            ?: return@patch
    val menuItemClass =
        classLoader.findClassOrNull("org.telegram.ui.ActionBar.ActionBarMenuSubItem")
            ?: return@patch
    val popupLayoutClass =
        classLoader.findClassOrNull(
            "org.telegram.ui.ActionBar.ActionBarPopupWindow\$ActionBarPopupWindowLayout"
        ) ?: return@patch
    val sendMessagesHelper =
        classLoader.findClassOrNull("org.telegram.messenger.SendMessagesHelper")
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

                    val context = runCatching {
                        val field = alert.javaClass.getDeclaredField("parentActivity").apply { isAccessible = true }
                        field.get(alert) as? Context
                    }.getOrNull()

                    if (context != null) {
                        val prefs = context.getSharedPreferences(
                            "nexalloy.telegram.forward",
                            Context.MODE_PRIVATE,
                        )
                        if (prefs.getBoolean("remember", false)) {
                            val hideSender = prefs.getBoolean("hide_sender", false)
                            val hideCaption = prefs.getBoolean("hide_caption", false)
                            runCatching {
                                alert.javaClass.getDeclaredField("showSendersName").apply {
                                    isAccessible = true
                                    setBoolean(alert, !hideSender)
                                }
                            }
                            hideCaptionByShareAlert[alert] = hideCaption
                            combinedPresetByShareAlert[alert] = hideSender && hideCaption
                        }
                    }
                }
                after {
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
        .forEach { method ->
            method.hookMethod {
                after { param ->
                    if (injectingMenuItem.get() == true) return@after

                    val alert = buildingShareMenu.get() ?: return@after
                    val child = param.args.getOrNull(0) as? View ?: return@after
                    if (child.javaClass.name != menuItemClass.name) return@after

                    val count = shareMenuItemCount.get() + 1
                    shareMenuItemCount.set(count)
                    if (count != 2) return@after

                    runCatching {
                        val resourcesProvider = menuItemClass
                            .getDeclaredField("resourcesProvider")
                            .apply { isAccessible = true }
                            .get(child)

                        val constructor = menuItemClass.declaredConstructors.first {
                            val types = it.parameterTypes
                            types.size == 5 &&
                                types[0] == Context::class.java &&
                                types[1] == Boolean::class.javaPrimitiveType &&
                                types[2] == Boolean::class.javaPrimitiveType &&
                                types[3] == Boolean::class.javaPrimitiveType
                        }.apply { isAccessible = true }

                        val option = constructor.newInstance(
                            child.context,
                            true,
                            false,
                            true,
                            resourcesProvider,
                        ) as View

                        val label = classLoader.telegramString(
                            "HideCaption",
                            "Forward without caption",
                        )

                        menuItemClass.getMethod(
                            "setTextAndIcon",
                            CharSequence::class.java,
                            Int::class.javaPrimitiveType,
                        ).invoke(option, label, 0)

                        fun updateChecked() {
                            menuItemClass.getMethod(
                                "setChecked",
                                Boolean::class.javaPrimitiveType,
                            ).invoke(option, hideCaptionByShareAlert[alert] == true)
                        }

                        updateChecked()
                        option.setOnClickListener {
                            hideCaptionByShareAlert[alert] =
                                hideCaptionByShareAlert[alert] != true
                            updateChecked()
                        }

                        // The original "hide sender" row used to be the bottom item.
                        runCatching {
                            menuItemClass.getDeclaredField("bottom").apply {
                                isAccessible = true
                                setBoolean(child, false)
                            }
                            menuItemClass.getMethod("updateBackground").invoke(child)
                        }

                        val density = child.resources.displayMetrics.density
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (48f * density).toInt(),
                        )

                        fun addExtraOption(label: CharSequence, checked: () -> Boolean, click: () -> Unit) {
                            val extra = constructor.newInstance(
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
                            ).invoke(extra, label, 0)

                            fun sync() {
                                menuItemClass.getMethod(
                                    "setChecked",
                                    Boolean::class.javaPrimitiveType,
                                ).invoke(extra, checked())
                            }
                            sync()
                            extra.setOnClickListener {
                                click()
                                sync()
                            }
                            method.invoke(param.thisObject, extra, params)
                        }

                        injectingMenuItem.set(true)
                        try {
                            method.invoke(param.thisObject, option, params)

                            addExtraOption(
                                "Without sender + caption",
                                { combinedPresetByShareAlert[alert] == true },
                            ) {
                                runCatching {
                                    alert.javaClass.getDeclaredField("showSendersName").apply {
                                        isAccessible = true
                                        setBoolean(alert, false)
                                    }
                                }
                                hideCaptionByShareAlert[alert] = true
                                combinedPresetByShareAlert[alert] = true
                            }

                            val prefs = child.context.getSharedPreferences(
                                "nexalloy.telegram.forward",
                                Context.MODE_PRIVATE,
                            )
                            addExtraOption(
                                "Remember forward choice",
                                { prefs.getBoolean("remember", false) },
                            ) {
                                val newValue = !prefs.getBoolean("remember", false)
                                val hideSender = runCatching {
                                    !alert.javaClass.getDeclaredField("showSendersName").apply {
                                        isAccessible = true
                                    }.getBoolean(alert)
                                }.getOrDefault(false)
                                prefs.edit()
                                    .putBoolean("remember", newValue)
                                    .putBoolean("hide_sender", hideSender)
                                    .putBoolean(
                                        "hide_caption",
                                        hideCaptionByShareAlert[alert] == true,
                                    )
                                    .apply()
                            }
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
                    if (hideCaptionByShareAlert[alert] == true) {
                        param.args[3] = true
                    }
                }
            }
        }
}
