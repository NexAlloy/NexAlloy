package io.github.nexalloy.revanced.tiktok.interaction.speed

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import java.lang.reflect.Modifier

private val speedControlParentFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    parameters("L")
    strings("playback_speed")
}

val playbackSpeedEnabledFingerprint = findMethodDirect {
    val parent = speedControlParentFingerprint.invoke(this)

    parent.invokes
        .filter { method ->
            method.returnTypeName == "boolean" &&
                method.paramTypeNames.isEmpty() &&
                !Modifier.isStatic(method.modifiers)
        }
        .single()
}
