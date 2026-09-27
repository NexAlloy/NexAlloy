package io.github.nexalloy.revanced.shared.privacy

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun ClassLoader.noopVoidMethods(className: String, vararg methodNames: String) {
    val clazz = findClassOrNull(className) ?: return
    val names = methodNames.toSet()

    clazz.declaredMethods
        .filter { it.name in names && it.returnType == Void.TYPE }
        .forEach { it.hookMethod(XC_MethodReplacement.DO_NOTHING) }
}

val DisableCommonAnalytics = patch(
    name = "Disable common analytics",
    description = "Suppresses common Firebase, CleverTap, AppsFlyer, Mixpanel and Facebook analytics calls when those SDKs are present.",
    use = false,
) {
    val firebase = classLoader.findClassOrNull("com.google.firebase.analytics.FirebaseAnalytics")
    firebase?.declaredMethods
        ?.filter { it.name == "logEvent" && it.returnType == Void.TYPE }
        ?.forEach { it.hookMethod(XC_MethodReplacement.DO_NOTHING) }

    firebase?.declaredMethods
        ?.filter {
            it.name == "setAnalyticsCollectionEnabled" &&
                it.returnType == Void.TYPE &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType
        }
        ?.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (param.args.isNotEmpty()) param.args[0] = false
                }
            }
        }

    classLoader.noopVoidMethods(
        "com.clevertap.android.sdk.CleverTapAPI",
        "pushEvent",
        "pushProfile",
        "recordChargedEvent",
    )
    classLoader.noopVoidMethods(
        "com.appsflyer.AppsFlyerLib",
        "logEvent",
        "start",
    )
    classLoader.noopVoidMethods(
        "com.mixpanel.android.mpmetrics.MixpanelAPI",
        "track",
        "identify",
    )
    classLoader.noopVoidMethods(
        "com.facebook.appevents.AppEventsLogger",
        "logEvent",
        "logPurchase",
    )
}

val BlockCommonDisplayAds = patch(
    name = "Block common display ads",
    description = "Blocks common banner/interstitial ad load calls when supported SDKs are present. Rewarded ads are intentionally left untouched.",
    use = false,
) {
    classLoader.noopVoidMethods(
        "com.google.android.gms.ads.BaseAdView",
        "loadAd",
    )
    classLoader.noopVoidMethods(
        "com.google.android.gms.ads.interstitial.InterstitialAd",
        "load",
    )
    classLoader.noopVoidMethods(
        "com.applovin.adview.AppLovinAdView",
        "loadNextAd",
    )
    classLoader.noopVoidMethods(
        "com.facebook.ads.AdView",
        "loadAd",
    )
    classLoader.noopVoidMethods(
        "com.inmobi.ads.InMobiBanner",
        "load",
    )
}
