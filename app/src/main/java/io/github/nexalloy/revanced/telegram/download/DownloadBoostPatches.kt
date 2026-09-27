package io.github.nexalloy.revanced.telegram.download

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.writeTelegramField
import java.util.concurrent.atomic.AtomicBoolean

private val downloadHookInstalled = AtomicBoolean(false)

@Volatile
private var requestedBoostLevel = 0

private fun ClassLoader.installDownloadBoostHook() {
    if (!downloadHookInstalled.compareAndSet(false, true)) return

    val operation =
        findTelegramClassOrNull("org.telegram.messenger.FileLoadOperation") ?: return

    operation.declaredMethods
        .filter {
            it.name == "updateParams" &&
                it.parameterTypes.isEmpty() &&
                it.returnType == Void.TYPE
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                after { param ->
                    val target = param.thisObject
                    when (requestedBoostLevel) {
                        1 -> {
                            target.writeTelegramField("downloadChunkSize", 128 * 1024)
                            target.writeTelegramField("downloadChunkSizeBig", 512 * 1024)
                            target.writeTelegramField("downloadChunkSizeAnimation", 256 * 1024)
                            target.writeTelegramField("maxDownloadRequests", 8)
                            target.writeTelegramField("maxDownloadRequestsBig", 8)
                            target.writeTelegramField("maxDownloadRequestsAnimation", 8)
                        }
                        2 -> {
                            target.writeTelegramField("downloadChunkSize", 256 * 1024)
                            target.writeTelegramField("downloadChunkSizeBig", 1024 * 1024)
                            target.writeTelegramField("downloadChunkSizeAnimation", 512 * 1024)
                            target.writeTelegramField("maxDownloadRequests", 12)
                            target.writeTelegramField("maxDownloadRequestsBig", 12)
                            target.writeTelegramField("maxDownloadRequestsAnimation", 12)
                        }
                    }
                }
            }
        }
}

val DownloadBoostMedium = patch(
    name = "Download speed boost: Medium",
    description = "Uses larger Telegram file chunks and up to 8 parallel download requests. If Maximum is also enabled, Maximum takes precedence.",
    use = false,
) {
    requestedBoostLevel = maxOf(requestedBoostLevel, 1)
    classLoader.installDownloadBoostHook()
}

val DownloadBoostMaximum = patch(
    name = "Download speed boost: Maximum",
    description = "Uses larger Telegram file chunks and up to 12 parallel download requests. This can increase RAM, bandwidth, and server throttling pressure.",
    use = false,
) {
    requestedBoostLevel = maxOf(requestedBoostLevel, 2)
    classLoader.installDownloadBoostHook()
}
