package io.github.nexalloy.revanced.telegram.notifications

import android.content.Context
import android.content.Intent
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.telegramString

private val quickReplyScope = ThreadLocal.withInitial { false }

val DisableNotificationMarkAsRead = patch(
    name = "Disable notification Mark as read",
    description = "Prevents Telegram's notification heard/mark-read receiver from marking conversations as read.",
    use = false,
) {
    classLoader.findTelegramClassOrNull("org.telegram.messenger.AutoMessageHeardReceiver")
        ?.declaredMethods
        ?.filter {
            it.name == "onReceive" &&
                it.parameterTypes.contentEquals(arrayOf(Context::class.java, Intent::class.java))
        }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
}

val QuickReplyWithoutReadReceipt = patch(
    name = "Quick reply without read receipt",
    description = "Allows notification quick replies without Telegram marking the whole dialog or included voice messages as read.",
    use = false,
) {
    val receiver =
        classLoader.findTelegramClassOrNull("org.telegram.messenger.WearReplyReceiver")
            ?: return@patch

    receiver.declaredMethods
        .filter { it.name == "sendMessage" && it.returnType == Void.TYPE }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before {
                    quickReplyScope.set(true)
                }
                after {
                    quickReplyScope.set(false)
                }
            }
        }

    classLoader.findTelegramClassOrNull("org.telegram.messenger.MessagesController")
        ?.declaredMethods
        ?.filter { it.name == "markDialogAsRead" }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (quickReplyScope.get() == true) {
                        param.result = null
                    }
                }
            }
        }

    classLoader.findTelegramClassOrNull("org.telegram.messenger.MessagesStorage")
        ?.declaredMethods
        ?.filter { it.name == "markVoiceMessageContentAsRead" }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (quickReplyScope.get() == true) {
                        param.result = null
                    }
                }
            }
        }
}

val ForceHideNotificationPreviews = patch(
    name = "Force-hide notification message previews",
    description = "Replaces notification message text with Telegram's generic new-message label. Telegram's native per-chat preview settings remain available when this patch is disabled.",
    use = false,
) {
    val controller =
        classLoader.findTelegramClassOrNull("org.telegram.messenger.NotificationsController")
            ?: return@patch
    val generic = classLoader.telegramString("YouHaveNewMessage", "New message").toString()

    controller.declaredMethods
        .filter {
            it.name == "getStringForMessage" &&
                it.returnType == String::class.java &&
                it.parameterTypes.isNotEmpty()
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    param.result = generic
                }
            }
        }
}
