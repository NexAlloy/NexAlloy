package io.github.nexalloy.revanced.telegram.links

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.hookTelegramRequests
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField
import io.github.nexalloy.revanced.telegram.runtime.writeTelegramField
import java.lang.reflect.Modifier
import java.util.Locale

private val trackingKeys = setOf(
    "fbclid",
    "gclid",
    "dclid",
    "igshid",
    "mc_cid",
    "mc_eid",
    "ref_src",
    "ref_url",
    "spm",
)

private fun isTrackingKey(key: String, host: String?): Boolean {
    val normalized = key.lowercase(Locale.US)
    if (normalized.startsWith("utm_") || normalized in trackingKeys) return true

    val normalizedHost = host?.lowercase(Locale.US).orEmpty()
    val youtube = normalizedHost == "youtu.be" ||
        normalizedHost == "youtube.com" ||
        normalizedHost.endsWith(".youtube.com")
    return youtube && (normalized == "si" || normalized == "feature")
}

private fun sanitizeUrl(value: String): String {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return value
    val scheme = uri.scheme?.lowercase(Locale.US) ?: return value
    if (scheme != "http" && scheme != "https") return value
    if (uri.query.isNullOrEmpty()) return value

    val names = runCatching { uri.queryParameterNames }.getOrNull() ?: return value
    if (names.none { isTrackingKey(it, uri.host) }) return value

    val builder = uri.buildUpon().clearQuery()
    for (name in names) {
        if (!isTrackingKey(name, uri.host)) {
            for (parameter in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, parameter)
            }
        }
    }
    return builder.build().toString()
}

private data class Replacement(
    val start: Int,
    val end: Int,
    val value: String,
)

private val urlPattern = Regex("""https?://[^\s<>()\[\]{}]+""", RegexOption.IGNORE_CASE)

private fun sanitizeMessage(text: String): Pair<String, List<Replacement>> {
    val replacements = urlPattern.findAll(text)
        .mapNotNull { match ->
            val clean = sanitizeUrl(match.value)
            if (clean == match.value) null
            else Replacement(match.range.first, match.range.last + 1, clean)
        }
        .toList()

    if (replacements.isEmpty()) return text to emptyList()

    val builder = StringBuilder(text)
    for (replacement in replacements.asReversed()) {
        builder.replace(replacement.start, replacement.end, replacement.value)
    }
    return builder.toString() to replacements
}

private fun adjustEntities(request: Any, replacements: List<Replacement>) {
    @Suppress("UNCHECKED_CAST")
    val entities = request.readTelegramField("entities") as? MutableList<Any?> ?: return

    for (replacement in replacements.asReversed()) {
        val delta = replacement.value.length - (replacement.end - replacement.start)
        if (delta == 0) continue

        for (entity in entities) {
            entity ?: continue
            val offset = (entity.readTelegramField("offset") as? Number)?.toInt() ?: continue
            val length = (entity.readTelegramField("length") as? Number)?.toInt() ?: continue
            val entityEnd = offset + length

            when {
                offset >= replacement.end ->
                    entity.writeTelegramField("offset", offset + delta)

                offset <= replacement.start && entityEnd >= replacement.end ->
                    entity.writeTelegramField("length", (length + delta).coerceAtLeast(0))
            }
        }
    }
}

val CleanSharedLinks = patch(
    name = "Clean outgoing tracking links",
    description = "Removes common tracking query parameters from HTTP/HTTPS links before Telegram sends text or captions while preserving functional Telegram link parameters.",
    use = false,
) {
    classLoader.hookTelegramRequests { request, _ ->
        val className = request.javaClass.name
        if (!className.contains("TL_messages_")) return@hookTelegramRequests
        if (
            !className.contains("sendMessage") &&
            !className.contains("sendMedia") &&
            !className.contains("editMessage")
        ) return@hookTelegramRequests

        val original = request.readTelegramField("message") as? String ?: return@hookTelegramRequests
        val (cleaned, replacements) = sanitizeMessage(original)
        if (cleaned == original) return@hookTelegramRequests

        adjustEntities(request, replacements)
        request.writeTelegramField("message", cleaned)
    }
}

private fun isTelegramInternalUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme == "tg" || scheme == "ton") return true

    val host = uri.host?.lowercase(Locale.US) ?: return false
    return host == "t.me" ||
        host.endsWith(".t.me") ||
        host == "telegram.me" ||
        host.endsWith(".telegram.me") ||
        host == "telegram.dog" ||
        host.endsWith(".telegram.dog")
}

val OpenExternalLinksInSystemBrowser = patch(
    name = "Open external links in system browser",
    description = "Opens ordinary HTTP/HTTPS links in the default browser while keeping Telegram deep links inside Telegram.",
    use = false,
) {
    val browser =
        classLoader.findTelegramClassOrNull("org.telegram.messenger.browser.Browser")
            ?: return@patch

    browser.declaredMethods
        .filter {
            it.name == "openUrl" &&
                Modifier.isStatic(it.modifiers) &&
                it.returnType == Void.TYPE &&
                it.parameterTypes.size >= 10 &&
                Context::class.java.isAssignableFrom(it.parameterTypes[0]) &&
                it.parameterTypes[1] == Uri::class.java
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val context = param.args.getOrNull(0) as? Context ?: return@before
                    val uri = param.args.getOrNull(1) as? Uri ?: return@before
                    val scheme = uri.scheme?.lowercase(Locale.US)
                    if (scheme != "http" && scheme != "https") return@before
                    if (isTelegramInternalUri(uri)) return@before

                    val opened = runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        if (context !is Activity) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.isSuccess

                    if (opened) {
                        param.result = null
                    }
                }
            }
        }
}
