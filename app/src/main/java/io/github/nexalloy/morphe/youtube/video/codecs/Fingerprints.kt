package io.github.nexalloy.morphe.youtube.video.codecs

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall

private val HDRCapabilityMethodFingerprint = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/Display\$HdrCapabilities;",
            name = "getSupportedHdrTypes",
        )
    ),
)

/**
 * Host methods that directly query HDR capabilities. Extension methods are excluded to match
 * upstream and to keep overrideSupportedHdrTypes() free to query the unmodified platform value.
 */
val HDRCapabilityFingerprint = findMethodListDirect {
    val matcher = HDRCapabilityMethodFingerprint.buildMethodMatcher()
    findMethod { matcher(matcher) }.filter { method ->
        method.declaredClass?.descriptor?.startsWith("Lapp/morphe/") != true &&
            HDRCapabilityMethodFingerprint.matchOrNull(method) != null
    }
}

internal object Vp9CapabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)
