package io.github.nexalloy.revanced.telegram.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findFieldRecursive
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.hookTelegramRequests
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField

@Volatile
private var lastRequestClass = "none"

private fun copy(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show()
}

val ProfileDebugTools = patch(
    name = "Profile ID and runtime debug tools",
    description = "Adds a profile toolbar menu for copying the internal dialog ID and Telegram runtime information including package version, account, DC, proxy, and last outgoing TL request.",
    use = false,
) {
    classLoader.hookTelegramRequests { request, _ ->
        lastRequestClass = request.javaClass.name.substringAfterLast('.')
    }

    val profile =
        classLoader.findTelegramClassOrNull("org.telegram.ui.ProfileActivity2")
            ?: return@patch
    val menuClass =
        classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenu")
            ?: return@patch
    val menuItemClass =
        classLoader.findTelegramClassOrNull("org.telegram.ui.ActionBar.ActionBarMenuItem")
            ?: return@patch

    val itemId = 0x4E580801

    profile.declaredMethods
        .filter { it.name == "createView" && it.parameterTypes.size == 1 }
        .forEach { method ->
            method.hookMethod {
                after { param ->
                    val profileObject = param.thisObject
                    val root = param.result as? View ?: return@after
                    val context = root.context
                    val dialogId = runCatching {
                        (profileObject.javaClass.getMethod("getDialogId").invoke(profileObject) as Number).toLong()
                    }.getOrNull() ?: return@after

                    val actionBar = profileObject.findFieldRecursive("actionBar")
                        ?.get(profileObject) ?: return@after
                    val menu = actionBar.javaClass.getMethod("createMenu").invoke(actionBar)

                    if (menu is ViewGroup) {
                        for (i in 0 until menu.childCount) {
                            if (menu.getChildAt(i).tag == itemId) return@after
                        }
                    }

                    val icon = runCatching {
                        classLoader.loadClass("org.telegram.messenger.R\$drawable")
                            .getField("msg_copy")
                            .getInt(null)
                    }.getOrDefault(0)

                    val item = menuClass
                        .getMethod("addItem", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        .invoke(menu, itemId, icon)

                    val copyId = menuItemClass.getMethod(
                        "addSubItem",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        CharSequence::class.java,
                    ).invoke(item, 0x4E580811, 0, "Copy dialog ID") as View

                    copyId.setOnClickListener {
                        copy(context, "Dialog ID", dialogId.toString())
                    }

                    val runtime = menuItemClass.getMethod(
                        "addSubItem",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        CharSequence::class.java,
                    ).invoke(item, 0x4E580812, 0, "Copy runtime info") as View

                    runtime.setOnClickListener {
                        val account = runCatching {
                            (profileObject.javaClass.getMethod("getCurrentAccount").invoke(profileObject) as Number).toInt()
                        }.getOrDefault(0)

                        val dc = runCatching {
                            val manager = classLoader.loadClass("org.telegram.tgnet.ConnectionsManager")
                            val instance = manager
                                .getMethod("getInstance", Int::class.javaPrimitiveType)
                                .invoke(null, account)
                            (manager.getMethod("getCurrentDatacenterId").invoke(instance) as Number).toInt()
                        }.getOrDefault(-1)

                        val packageInfo = runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }.getOrNull()

                        val proxy = runCatching {
                            val sharedConfig = classLoader.loadClass("org.telegram.messenger.SharedConfig")
                            sharedConfig.getDeclaredField("currentProxy").apply {
                                isAccessible = true
                            }.get(null)
                        }.getOrNull()
                        val proxyAddress = proxy?.readTelegramField("address") as? String

                        val info = buildString {
                            append("package="); appendLine(context.packageName)
                            append("version="); appendLine(packageInfo?.versionName ?: "unknown")
                            append("account="); appendLine(account)
                            append("dialog="); appendLine(dialogId)
                            append("dc="); appendLine(dc)
                            append("proxy="); appendLine(proxyAddress ?: "none")
                            append("lastRequest="); append(lastRequestClass)
                        }
                        copy(context, "Telegram runtime info", info)
                    }
                }
            }
        }
}
