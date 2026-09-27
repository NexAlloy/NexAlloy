package io.github.nexalloy.revanced.tiktok.interaction.downloads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val EnableDownloads = patch(
    name = "Enable downloads",
    description = "Removes TikTok's client-side sharing/download restrictions and requests non-watermarked transcode output.",
) {
    ::aclCommonShareCodeFingerprint.hookMethod(XC_MethodReplacement.returnConstant(0))
    ::aclCommonShareShowTypeFingerprint.hookMethod(XC_MethodReplacement.returnConstant(2))
    ::aclCommonShareTranscodeFingerprint.hookMethod(XC_MethodReplacement.returnConstant(1))
}
