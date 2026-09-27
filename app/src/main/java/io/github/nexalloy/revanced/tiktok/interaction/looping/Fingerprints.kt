package io.github.nexalloy.revanced.tiktok.interaction.looping

import io.github.nexalloy.morphe.fingerprint

val videoEngineSetLoopingFingerprint = fingerprint {
    returns("V")
    parameters("Z")
    methodMatcher { name = "setLooping" }
    classMatcher {
        descriptor = "Lcom/ss/ttvideoengine/TTVideoEngine;"
    }
}
