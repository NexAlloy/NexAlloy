package io.github.nexalloy.revanced.pixel

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint

val hasSystemFeatureCheckerFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            paramTypes = listOf("java.lang.String")
        }
    }.filter { method ->
        method.invokes.any {
            it.className == "android.content.pm.PackageManager" &&
                it.methodName == "hasSystemFeature"
        }
    }.single()
}

val pixelModelCheckerFingerprint = fingerprint {
    returns("Z")
    parameters()
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    strings("Pixel")
}

val rootDetectionCheckerFingerprint = fingerprint {
    returns("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    strings("Magisk")
}
