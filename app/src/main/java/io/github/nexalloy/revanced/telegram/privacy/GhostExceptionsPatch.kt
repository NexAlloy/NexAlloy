package io.github.nexalloy.revanced.telegram.privacy

import android.content.Context
import android.view.View
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private const val GHOST_MENU_ID = 0x4E5847

private fun ClassLoader.findClassOrNullGhost(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

val GhostExceptions = patch(
    name = "Ghost privacy exceptions",
    description = "Adds a per-user profile action that lets selected users keep seeing real typing/read activity while stealth privacy remains enabled for everyone else.",
    use = false,
) {
    val profile =
        classLoader.findClassOrNullGhost("org.telegram.ui.ProfileActivity")
            ?: return@patch

    profile.declaredMethods
        .filter {
            it.name == "createActionBarMenu" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                after { param ->
                    val activity = param.thisObject
                    val dialogId = runCatching {
                        profile.getMethod("getDialogId").invoke(activity) as Long
                    }.getOrNull() ?: return@after
                    if (dialogId <= 0L) return@after

                    val otherItem = runCatching {
                        profile.getDeclaredField("otherItem").apply { isAccessible = true }
                            .get(activity)
                    }.getOrNull() ?: return@after

                    val context = (otherItem as? View)?.context ?: return@after
                    val prefs = context.getSharedPreferences(
                        "nexalloy.telegram.ghost",
                        Context.MODE_PRIVATE,
                    )

                    val addSubItem = otherItem.javaClass.methods.firstOrNull {
                        it.name == "addSubItem" &&
                            it.parameterTypes.size >= 3 &&
                            it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                            it.parameterTypes[1] == Int::class.javaPrimitiveType &&
                            CharSequence::class.java.isAssignableFrom(it.parameterTypes[2])
                    } ?: return@after

                    val current = prefs.getStringSet("exceptions", emptySet())
                        ?.contains(dialogId.toString()) == true
                    val label = if (current) {
                        "Remove Ghost exception"
                    } else {
                        "Add Ghost exception"
                    }

                    val args = arrayOfNulls<Any>(addSubItem.parameterTypes.size)
                    args[0] = GHOST_MENU_ID
                    args[1] = 0
                    args[2] = label
                    for (i in 3 until args.size) {
                        args[i] = when (addSubItem.parameterTypes[i]) {
                            Boolean::class.javaPrimitiveType -> false
                            Int::class.javaPrimitiveType -> 0
                            else -> null
                        }
                    }

                    val item = runCatching {
                        addSubItem.invoke(otherItem, *args)
                    }.getOrNull() as? View ?: return@after

                    item.setOnClickListener {
                        val set = prefs.getStringSet("exceptions", emptySet())
                            ?.toMutableSet() ?: mutableSetOf()
                        val key = dialogId.toString()
                        if (!set.add(key)) {
                            set.remove(key)
                        }
                        prefs.edit().putStringSet("exceptions", set).apply()
                        method.invoke(activity, false)
                    }
                }
            }
        }
}
