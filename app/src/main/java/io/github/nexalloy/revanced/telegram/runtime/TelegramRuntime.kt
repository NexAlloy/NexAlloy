package io.github.nexalloy.revanced.telegram.runtime

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.hookMethod
import java.lang.reflect.Field

internal const val TELEGRAM_PREFS = "nexalloy_telegram"

internal fun ClassLoader.findTelegramClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

internal fun Any.findFieldRecursive(name: String): Field? {
    var type: Class<*>? = javaClass
    while (type != null) {
        val current = type
        try {
            return current.getDeclaredField(name).apply { isAccessible = true }
        } catch (_: Throwable) {
            type = current.superclass
        }
    }
    return null
}

internal fun Any.readTelegramField(name: String): Any? =
    runCatching { javaClass.getField(name).get(this) }
        .recoverCatching { findFieldRecursive(name)?.get(this) }
        .getOrNull()

internal fun Any.writeTelegramField(name: String, value: Any?): Boolean {
    val field = runCatching { javaClass.getField(name) }
        .getOrElse { findFieldRecursive(name) ?: return false }
    return runCatching {
        field.isAccessible = true
        field.set(this, value)
        true
    }.getOrDefault(false)
}

internal fun ClassLoader.telegramContextOrNull(): Context? =
    runCatching {
        val app = loadClass("org.telegram.messenger.ApplicationLoader")
        app.getField("applicationContext").get(null) as Context
    }.getOrNull()

internal fun ClassLoader.telegramPrefs(): SharedPreferences? =
    telegramContextOrNull()?.getSharedPreferences(TELEGRAM_PREFS, Context.MODE_PRIVATE)

internal fun ClassLoader.telegramString(name: String, fallback: String): CharSequence =
    runCatching {
        val id = loadClass("org.telegram.messenger.R\$string").getField(name).getInt(null)
        loadClass("org.telegram.messenger.LocaleController")
            .getMethod("getString", Int::class.javaPrimitiveType)
            .invoke(null, id) as CharSequence
    }.getOrDefault(fallback)

internal fun peerDialogId(peer: Any?): Long? {
    peer ?: return null

    fun number(name: String): Long =
        (peer.readTelegramField(name) as? Number)?.toLong() ?: 0L

    val userId = number("user_id")
    if (userId != 0L) return userId

    val chatId = number("chat_id")
    if (chatId != 0L) return -chatId

    val channelId = number("channel_id")
    if (channelId != 0L) return -channelId

    return null
}

internal fun requestDialogId(request: Any): Long? {
    peerDialogId(request.readTelegramField("peer"))?.let { return it }
    peerDialogId(request.readTelegramField("channel"))?.let { return it }
    peerDialogId(request.readTelegramField("peer_id"))?.let { return it }
    return null
}

internal fun messageDialogId(message: Any?): Long? {
    message ?: return null

    val methodValue = runCatching {
        val method = message.javaClass.methods.firstOrNull {
            it.name == "getDialogId" && it.parameterTypes.isEmpty()
        }
        (method?.invoke(message) as? Number)?.toLong()
    }.getOrNull()
    if (methodValue != null && methodValue != 0L) return methodValue

    val owner = message.readTelegramField("messageOwner") ?: message
    val explicit = (owner.readTelegramField("dialog_id") as? Number)?.toLong() ?: 0L
    if (explicit != 0L) return explicit
    return peerDialogId(owner.readTelegramField("peer_id"))
}

internal fun ClassLoader.hookTelegramRequests(
    handler: (request: Any, param: XC_MethodHook.MethodHookParam) -> Unit,
) {
    val manager = findTelegramClassOrNull("org.telegram.tgnet.ConnectionsManager") ?: return

    manager.declaredMethods
        .filter {
            (it.name == "sendRequest" || it.name == "sendRequestTyped") &&
                it.parameterTypes.isNotEmpty() &&
                it.returnType == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    val request = param.args.firstOrNull() ?: return@before
                    handler(request, param)
                }
            }
        }
}
