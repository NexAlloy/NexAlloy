package io.github.nexalloy.revanced.telegram.notifications

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private val quickReplyThread = ThreadLocal.withInitial { false }

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

val DisableNotificationMarkRead = patch(
    name = "Disable notification mark-as-read action",
    description = "Prevents Telegram notification/Android Auto heard actions from marking a conversation as read.",
    use = false,
) {
    classLoader.findClassOrNull("org.telegram.messenger.AutoMessageHeardReceiver")
        ?.declaredMethods
        ?.filter { it.name == "onReceive" && it.returnType == Void.TYPE }
        ?.forEach {
            it.hookMethod(XC_MethodReplacement.DO_NOTHING)
        }
}

val KeepUnreadAfterQuickReply = patch(
    name = "Keep unread after notification reply",
    description = "Sends quick replies without locally marking the conversation as read.",
    use = false,
) {
    val replyReceiver =
        classLoader.findClassOrNull("org.telegram.messenger.WearReplyReceiver")
            ?: return@patch

    replyReceiver.declaredMethods
        .filter {
            it.name == "sendMessage" &&
                it.returnType == Void.TYPE
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { quickReplyThread.set(true) }
                after { quickReplyThread.remove() }
            }
        }

    classLoader.findClassOrNull("org.telegram.messenger.MessagesController")
        ?.declaredMethods
        ?.filter { it.name == "markDialogAsRead" && it.returnType == Void.TYPE }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (quickReplyThread.get() == true) {
                        param.result = null
                    }
                }
            }
        }
}
