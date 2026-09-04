package io.github.nexalloy.morphe.youtube.misc.imageurlhook

import io.github.nexalloy.patch
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.impl.CronetUrlRequest
import java.io.IOException
import java.lang.reflect.Field

val cronetImageUrlHookPatch = patch(
    description = "Hooks Cronet image urls.",
) {
    MessageDigestImageUrlFingerprint.hookMethod {
        before { param ->
            var url = param.args[0] as String? ?: return@before
            val hooks = imageUrlHooks
            for (hook in hooks) {
                url = hook(url)
            }
            param.args[0] = url
        }
    }

    OnSucceededFingerprint.hookMethod {
        before { param ->
            val hooks = imageUrlSuccessCallbackHooks
            val request = param.args[0] as UrlRequest
            val responseInfo = param.args[1] as UrlResponseInfo
            for (hook in hooks) {
                hook(request, responseInfo)
            }
        }
    }

    OnFailureFingerprint.hookMethod {
        before { param ->
            val hooks = imageUrlErrorCallbackHooks
            val request = param.args[0] as UrlRequest
            val responseInfo = param.args[1] as? UrlResponseInfo
            val exception = param.args[2] as IOException
            for (hook in hooks) {
                hook(request, responseInfo, exception)
            }
        }
    }

    urlJField = ::urlField.field
}

private lateinit var urlJField: Field

fun getHookedUrl(o: CronetUrlRequest) = urlJField.get(o) as String

@Volatile
private var imageUrlHooks = emptyArray<(String) -> String>()

@Volatile
private var imageUrlSuccessCallbackHooks = emptyArray<(UrlRequest, UrlResponseInfo) -> Unit>()

@Volatile
private var imageUrlErrorCallbackHooks = emptyArray<(UrlRequest, UrlResponseInfo?, IOException) -> Unit>()

private val hookRegistrationLock = Any()

fun addImageUrlHook(f: (String) -> String, highPriority: Boolean = false) {
    synchronized(hookRegistrationLock) {
        imageUrlHooks = if (highPriority) arrayOf(f) + imageUrlHooks else imageUrlHooks + f
    }
}

fun addImageUrlSuccessCallbackHook(f: (UrlRequest, UrlResponseInfo) -> Unit) {
    synchronized(hookRegistrationLock) {
        imageUrlSuccessCallbackHooks = imageUrlSuccessCallbackHooks + f
    }
}

fun addImageUrlErrorCallbackHook(f: (UrlRequest, UrlResponseInfo?, IOException) -> Unit) {
    synchronized(hookRegistrationLock) {
        imageUrlErrorCallbackHooks = imageUrlErrorCallbackHooks + f
    }
}
