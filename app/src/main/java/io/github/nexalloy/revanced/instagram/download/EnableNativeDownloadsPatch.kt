package io.github.nexalloy.revanced.instagram.download

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val EnableNativeDownloads = patch(
    name = "Enable native downloads",
    description = "Enables Instagram's own media download flow and restores the Download entry in reduced reel menus when available.",
) {
    ::nativeDownloadEligibleFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    ::nativeDownloadRestrictedFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))

    val optionClass = runCatching {
        classLoader.loadClass(
            "com.instagram.feed.media.mediaoption.MediaOption\$Option"
        )
    }.getOrNull()

    val downloadOption = optionClass
        ?.enumConstants
        ?.firstOrNull { (it as? Enum<*>)?.name == "DOWNLOAD" }

    if (downloadOption != null) {
        ::reducedOptionsListFingerprint.memberOrNull?.hookMethod {
            after { param ->
                @Suppress("UNCHECKED_CAST")
                val options = param.result as? MutableList<Any?> ?: return@after
                if (!options.contains(downloadOption)) {
                    options.add(downloadOption)
                }
            }
        }
    }
}
