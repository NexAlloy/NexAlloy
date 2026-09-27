package io.github.nexalloy.revanced.tiktok.interaction.quickactions

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val HideQuickCommentReactions = patch(
    name = "Hide TikTok quick comment reactions",
    description = "Forces TikTok's quick emoji reaction gate to stay hidden.",
    use = false,
) {
    ::quickCommentReactionGateFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(true))
}

val DisableLongPressQuickShare = patch(
    name = "Disable TikTok long-press quick share",
    description = "Disables TikTok's long-press quick-share interaction.",
    use = false,
) {
    ::longPressQuickShareGateFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(0))
}

val DisableLongPressRepost = patch(
    name = "Disable TikTok long-press repost",
    description = "Prevents holding the Like button from opening TikTok's repost action.",
    use = false,
) {
    ::longPressRepostGateFingerprint.memberOrNull
        ?.hookMethod(XC_MethodReplacement.returnConstant(false))
}
