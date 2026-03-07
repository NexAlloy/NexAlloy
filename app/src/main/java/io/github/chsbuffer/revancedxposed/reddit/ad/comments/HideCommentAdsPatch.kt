package io.github.chsbuffer.revancedxposed.reddit.ad.comments

import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.revancedxposed.patch

val HideCommentAds = patch(
    description = "Removes ads in the comments.",
) {
    ::hideCommentAdsFingerprint.hookMethod {
        before {
            Logger.printDebug { "Hide Comment Ad - invokeSuspend skipped" }
            it.result = kotlin.Unit
        }
    }

    // Hook tất cả method trong class chứa "blank_ad_container"
    val rendererClass = ::hideCommentAdsRendererFingerprint.member.declaringClass
    rendererClass.declaredMethods.forEach { method ->
        XposedBridge.hookMethod(method, object : de.robv.android.xposed.XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Logger.printDebug { "Hide Comment Ad - renderer blocked: ${method.name}" }
                param.result = null
            }
        })
    }
}
