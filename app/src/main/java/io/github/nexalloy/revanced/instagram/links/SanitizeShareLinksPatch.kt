package io.github.nexalloy.revanced.instagram.links

import android.net.Uri
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private val trackingParameters = setOf(
    "igsh",
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_content",
    "fbclid",
    "si",
)

private fun sanitizeInstagramUrl(value: String): String {
    if (!value.startsWith("http://") && !value.startsWith("https://")) return value

    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return value
    val names = runCatching { uri.queryParameterNames }.getOrNull() ?: return value
    if (names.none { it in trackingParameters }) return value

    val builder = uri.buildUpon().clearQuery()
    names.forEach { name ->
        if (name in trackingParameters) return@forEach
        uri.getQueryParameters(name).forEach { parameterValue ->
            builder.appendQueryParameter(name, parameterValue)
        }
    }
    return builder.build().toString()
}

private fun sanitizeUrlFields(target: Any) {
    var clazz: Class<*>? = target.javaClass

    while (clazz != null && clazz != Any::class.java) {
        clazz.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .forEach { field ->
                runCatching {
                    field.isAccessible = true
                    val original = field.get(target) as? String ?: return@runCatching
                    val sanitized = sanitizeInstagramUrl(original)
                    if (sanitized != original) field.set(target, sanitized)
                }
            }
        clazz = clazz.superclass
    }
}

val SanitizeShareLinks = patch(
    name = "Sanitize Instagram share links",
    description = "Removes igsh, UTM, fbclid and si tracking parameters from Instagram share URLs.",
) {
    listOf(
        ::storyUrlResponseImplFingerprint,
        ::liveUrlResponseImplFingerprint,
    ).forEach { fingerprint ->
        fingerprint.memberOrNull?.hookMethod {
            after { param ->
                val original = param.result as? String ?: return@after
                param.result = sanitizeInstagramUrl(original)
            }
        }
    }

    listOf(
        ::permalinkResponseJsonParserFingerprint,
        ::profileUrlResponseJsonParserFingerprint,
    ).forEach { fingerprint ->
        fingerprint.memberOrNull?.hookMethod {
            after { param ->
                val result = param.result ?: return@after
                sanitizeUrlFields(result)
            }
        }
    }
}
