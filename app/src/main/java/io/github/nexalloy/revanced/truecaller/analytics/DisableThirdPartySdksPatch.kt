package io.github.nexalloy.revanced.truecaller.analytics

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun Class<*>.hookVoid(name: String, parameterCount: Int? = null) {
    declaredMethods
        .filter {
            it.name == name &&
                it.returnType == Void.TYPE &&
                (parameterCount == null || it.parameterTypes.size == parameterCount)
        }
        .forEach { it.hookMethod(XC_MethodReplacement.DO_NOTHING) }
}

val DisableThirdPartySdks = patch(
    name = "Disable Truecaller third-party SDK initialization",
    description = "Suppresses known third-party analytics/ad SDK initialization entry points bundled by Truecaller.",
    use = false,
) {
    classLoader.findClassOrNull("com.microsoft.clarity.Clarity")
        ?.declaredMethods
        ?.filter {
            it.name == "initialize" &&
                it.parameterTypes.size == 2 &&
                !it.returnType.isPrimitive
        }
        ?.forEach { it.hookMethod(XC_MethodReplacement.returnConstant(null)) }

    classLoader.findClassOrNull("com.appsflyer.internal.AFa1ySDK")
        ?.hookVoid("start", 1)

    classLoader.findClassOrNull("com.moloco.sdk.publisher.Moloco")
        ?.hookVoid("initialize", 2)

    classLoader.findClassOrNull("com.huawei.hms.aaid.InitProvider")
        ?.declaredMethods
        ?.filter {
            it.name == "onCreate" &&
                it.returnType == Boolean::class.javaPrimitiveType &&
                it.parameterTypes.isEmpty()
        }
        ?.forEach { it.hookMethod(XC_MethodReplacement.returnConstant(true)) }

    classLoader.findClassOrNull("com.freshchat.consumer.sdk.Freshchat")
        ?.declaredMethods
        ?.filter {
            it.name == "init" &&
                it.returnType == Boolean::class.javaPrimitiveType
        }
        ?.forEach { it.hookMethod(XC_MethodReplacement.returnConstant(false)) }

    classLoader.findClassOrNull("com.inmobi.sdk.InMobiSdk")
        ?.hookVoid("init", 4)

    classLoader.findClassOrNull("com.appnext.nexdk.AppnextSDK")
        ?.hookVoid("initialize", 1)

    classLoader.findClassOrNull("com.fyber.inneractive.sdk.external.InneractiveAdManager")
        ?.hookVoid("initialize", 3)

    classLoader.findClassOrNull("com.ironsource.mediationsdk.IronSource")
        ?.hookVoid("init", 4)

    classLoader.findClassOrNull("net.pubnative.lite.sdk.HyBid")
        ?.hookVoid("initialize", 3)

    classLoader.findClassOrNull("com.google.mlkit.common.internal.CommonComponentRegistrar")
        ?.declaredMethods
        ?.filter {
            it.name == "getComponents" &&
                java.util.List::class.java.isAssignableFrom(it.returnType) &&
                it.parameterTypes.isEmpty()
        }
        ?.forEach { it.hookMethod(XC_MethodReplacement.returnConstant(emptyList<Any>())) }
}
