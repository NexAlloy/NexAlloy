package io.github.nexalloy.morphe.youtube.shared

import io.github.nexalloy.callMethod
import io.github.nexalloy.morphe.*
import io.github.nexalloy.patch

internal object PushNotificationReceivedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L"),
    filters = listOf(
        string("Exception processing push notif renderer.")
    )
)
private val observers = mutableListOf<(ByteArray) -> Unit>()
private val filters = mutableListOf<(ByteArray) -> Boolean>()
fun observePushNotification(callback: (ByteArray) -> Unit) { observers += callback }
fun filterPushNotification(callback: (ByteArray) -> Boolean) { filters += callback }
val pushNotificationHookPatch = patch {
    PushNotificationReceivedFingerprint.hookMethod {
        before {
            val renderer = it.args[1] ?: return@before
            val bytes = renderer.callMethod("toByteArray") as ByteArray
            observers.forEach { callback -> callback(bytes) }
            if (filters.any { callback -> callback(bytes) }) it.result = null
        }
    }
}
