package io.github.nexalloy.revanced.telegram.ads

import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private fun ClassLoader.findClassOrNull(name: String): Class<*>? =
    runCatching { loadClass(name) }.getOrNull()

private fun Class<*>.replaceVoidMethods(vararg names: String) {
    val methodNames = names.toSet()
    declaredMethods
        .filter { it.name in methodNames && it.returnType == Void.TYPE }
        .forEach { it.hookMethod(XC_MethodReplacement.DO_NOTHING) }
}

private fun Class<*>.replaceMethodsWithNull(vararg names: String) {
    val methodNames = names.toSet()
    declaredMethods
        .filter { it.name in methodNames && it.returnType != Void.TYPE }
        .forEach { it.hookMethod(XC_MethodReplacement.returnConstant(null)) }
}

private fun ClassLoader.blockSponsoredTlResponse(className: String) {
    findClassOrNull(className)
        ?.declaredMethods
        ?.filter {
            it.name == "TLdeserialize" &&
                Modifier.isStatic(it.modifiers) &&
                it.returnType != Void.TYPE
        }
        ?.forEach { it.hookMethod(XC_MethodReplacement.returnConstant(null)) }
}

val RemoveSponsoredAds = patch(
    name = "Remove sponsored ads",
    description = "Removes Telegram sponsored messages, video ads, sponsored search results, and proxy sponsor dialogs without changing Premium state.",
) {
    // Current Telegram video/channel ad path.
    classLoader.findClassOrNull("org.telegram.messenger.video.VideoAds")
        ?.replaceVoidMethods("load", "show", "schedule")

    // Current sponsored-message view used in bot/channel chat surfaces.
    classLoader.findClassOrNull("org.telegram.ui.bots.BotAdView")?.let { botAdView ->
        botAdView.declaredConstructors.forEach { constructor ->
            constructor.hookMethod {
                after { param ->
                    (param.thisObject as? View)?.visibility = View.GONE
                }
            }
        }

        botAdView.declaredMethods
            .filter { it.name == "set" && it.returnType == Void.TYPE }
            .forEach { method ->
                method.hookMethod {
                    before { param ->
                        (param.thisObject as? View)?.visibility = View.GONE
                        param.result = null
                    }
                }
            }
    }

    // Block sponsored-message and sponsored-search TL responses before UI code can consume them.
    classLoader.blockSponsoredTlResponse("org.telegram.tgnet.TLRPC\$messages_SponsoredMessages")
    classLoader.blockSponsoredTlResponse("org.telegram.tgnet.TLRPC\$contacts_SponsoredPeers")

    // Compatibility with Telegram versions that still expose these controller/UI helpers.
    classLoader.findClassOrNull("org.telegram.messenger.MessagesController")?.let { controller ->
        controller.replaceMethodsWithNull("getSponsoredMessages")

        // Hide MTProxy sponsor dialogs only. Telegram PSA dialogs are intentionally preserved.
        val proxyPromoType = runCatching {
            controller.getDeclaredField("PROMO_TYPE_PROXY").apply { isAccessible = true }.getInt(null)
        }.getOrNull()
        val promoDialogType = runCatching {
            controller.getDeclaredField("promoDialogType").apply { isAccessible = true }
        }.getOrNull()

        if (proxyPromoType != null && promoDialogType != null) {
            controller.declaredMethods
                .filter {
                    it.name == "isPromoDialog" &&
                        it.returnType == Boolean::class.javaPrimitiveType
                }
                .forEach { method ->
                    method.hookMethod {
                        after { param ->
                            if (param.result == true) {
                                val type = runCatching {
                                    promoDialogType.getInt(param.thisObject)
                                }.getOrNull()
                                if (type == proxyPromoType) {
                                    param.result = false
                                }
                            }
                        }
                    }
                }
        }
    }

    classLoader.findClassOrNull("org.telegram.ui.ChatActivity")
        ?.replaceVoidMethods("addSponsoredMessages")
}
