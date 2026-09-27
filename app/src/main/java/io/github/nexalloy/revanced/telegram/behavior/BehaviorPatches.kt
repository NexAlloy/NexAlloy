package io.github.nexalloy.revanced.telegram.behavior

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull

val DisableMediaAutoplay = patch(
    name = "Disable media autoplay",
    description = "Force-disables Telegram video and GIF autoplay while keeping manual playback available. Telegram's native autoplay setting remains the preferred control when this patch is disabled.",
    use = false,
) {
    classLoader.findTelegramClassOrNull("org.telegram.messenger.SharedConfig")
        ?.declaredMethods
        ?.filter {
            (it.name == "isAutoplayVideo" || it.name == "isAutoplayGifs") &&
                it.parameterTypes.isEmpty() &&
                it.returnType == Boolean::class.javaPrimitiveType
        }
        ?.forEach {
            it.hookMethod(XC_MethodReplacement.returnConstant(false))
        }
}
