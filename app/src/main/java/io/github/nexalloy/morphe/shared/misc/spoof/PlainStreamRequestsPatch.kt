package io.github.nexalloy.morphe.shared.misc.spoof

import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.nio.ByteBuffer

// The Cronet hooks are shared with stream spoofing upstream; downloads use them independently.
val plainStreamRequestsPatch = patch {
    // Streams of other clients are plain urls, which the server rejects or sends slowly if they are
    // requested like the streams of the app, with a POST body. Send them as plain GET requests instead.
    val urlField = ::cronetUrlField.field
    val httpMethodField = ::cronetHttpMethodField.field
    val uploadField = ::cronetUploadField.field
    CronetUrlRequestBuildFingerprint.hookMethod {
        before {
            if (SpoofVideoStreamsPatch.isPlainStreamRequest(urlField.get(it.thisObject) as String?)) {
                httpMethodField.set(it.thisObject, "GET")
                uploadField.set(it.thisObject, null)
            }
        }
    }
    // Plain streams are sent slowly if a large range is requested. Request a smaller range instead.
    val limitRange: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            if (innerDepth == 0) {
                it.args[0] = SpoofVideoStreamsPatch.limitPlainStreamRange(it.args[0] as String?)
            }
        }
    }
    val createRequestHooks = ::cronetCreateRequestMethods.dexMethodList.map { method ->
        method.toMethod() to limitRange
    }.toTypedArray()
    CronetUrlRequestBuildFingerprint.hookMethod(scopedHook(*createRequestHooks))

    // A shortened response must pause the transfer so saving continues from the last byte read.
    // The server can also answer a range with only a SABR error and no media. Instead of requesting
    // that range over and over, fail like a server error so the app tries a fallback host.
    val remainingBytes = ::cronetRemainingBytesField.field
    val pauseTransfer = PriorityTooLowExceptionFingerprint.constructor
    val serverError = InvalidResponseCodeExceptionFingerprint.constructor
    val dataSpec = ::cronetDataSpecField.field
    listOf(CronetDataSourceReadFingerprint, CronetDataSourceReadBufferFingerprint).forEach { fingerprint ->
        fingerprint.hookMethod(scopedHook(::cronetReadResponseMethod.method) {
            before { SpoofVideoStreamsPatch.onStreamResponseReadStart(it.args[0] as ByteBuffer) }
            after {
                if (!it.hasThrowable()) SpoofVideoStreamsPatch.onStreamResponseRead(it.args[0] as ByteBuffer)
            }
        })
        fingerprint.hookMethod {
            before {
                if (SpoofVideoStreamsPatch.isStreamSabrErrorResponse()) {
                    it.throwable = serverError.newInstance(503, null, emptyMap<Any, Any>(), dataSpec.get(it.thisObject)) as Throwable
                } else if (SpoofVideoStreamsPatch.isLimitedStreamRangeFinished(remainingBytes.getLong(it.thisObject))) {
                    it.throwable = pauseTransfer.newInstance(0, 0) as Throwable
                }
            }
            after {
                // A successful read still delivers its bytes; the next read pauses at entry.
                if (!it.hasThrowable() && SpoofVideoStreamsPatch.isStreamSabrErrorResponse()) {
                    it.throwable = serverError.newInstance(503, null, emptyMap<Any, Any>(), dataSpec.get(it.thisObject)) as Throwable
                } else if (!it.hasThrowable() && it.result == -1 &&
                    SpoofVideoStreamsPatch.isLimitedStreamRangeFinished(remainingBytes.getLong(it.thisObject))
                ) {
                    it.throwable = pauseTransfer.newInstance(0, 0) as Throwable
                }
            }
        }
    }
}
