package io.github.nexalloy.revanced.tiktok.misc.externalbrowser

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method

private fun readField(target: Any?, name: String): Any? {
    if (target == null) return null
    var type: Class<*>? = target.javaClass
    while (type != null) {
        try {
            val field: Field = type.getDeclaredField(name)
            field.isAccessible = true
            return field.get(target)
        } catch (_: Throwable) {
            type = type.superclass
        }
    }
    return null
}

private fun callNoArg(target: Any?, name: String): Any? {
    if (target == null) return null
    return runCatching {
        val method: Method = target.javaClass.getMethod(name)
        method.invoke(target)
    }.getOrNull()
}

private fun contextField(target: Any?): Context? {
    if (target == null) return null
    var type: Class<*>? = target.javaClass
    while (type != null) {
        type.declaredFields.forEach { field ->
            if (!Context::class.java.isAssignableFrom(field.type)) return@forEach
            val value = runCatching {
                field.isAccessible = true
                field.get(target)
            }.getOrNull()
            if (value is Context) return value
        }
        type = type.superclass
    }
    return null
}

private fun resolveTarget(source: String?): Uri? {
    var value = source?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    repeat(4) {
        var uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        if (uri.scheme == null) {
            value = "https://$value"
            uri = Uri.parse(value)
        }
        if (uri.scheme.equals("aweme", true)) {
            value = uri.getQueryParameter("url") ?: return null
            return@repeat
        }
        uri.getQueryParameter("target")?.let {
            value = it
            return@repeat
        }
        return if (
            (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
        ) uri else null
    }

    return null
}

private fun isAllowedScreen(target: Any?): Boolean {
    if (target == null) return false
    if (callNoArg(readField(target, "seclinkConfig"), "getScene") == "bio_url") return true

    val params = readField(target, "defaultParams") as? Map<*, *> ?: return false
    return params["sec_link_scene"] == "bio_url"
}

private fun openExternal(context: Context?, source: String?): Boolean {
    val target = resolveTarget(source) ?: return false
    val safeContext = context ?: return false
    val intent = Intent(Intent.ACTION_VIEW, target).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        if (safeContext !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching {
        safeContext.startActivity(intent)
        true
    }.getOrDefault(false)
}

val OpenExternalLinks = patch(
    name = "Open TikTok external links directly",
    description = "Opens supported profile and story website links in the system browser instead of TikTok's in-app browser.",
    use = false,
) {
    ::sparkThirdRouterOpenFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val context = param.args.getOrNull(0) as? Context ?: return@before
            val sparkContext = param.args.getOrNull(1) ?: return@before
            if (!isAllowedScreen(sparkContext)) return@before

            val source = readField(sparkContext, "url") as? String
            if (openExternal(context, source)) param.result = null
        }
    }

    ::storyLinkSheetFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val owner = param.thisObject ?: return@before
            val sticker = param.args.firstOrNull() ?: return@before
            if (callNoArg(sticker, "getType") != 106) return@before

            val link = callNoArg(sticker, "getUrlLinkSticker")
            val source = callNoArg(link, "getFullURL") as? String
            if (openExternal(contextField(owner), source)) param.result = null
        }
    }
}
