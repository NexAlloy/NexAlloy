package io.github.nexalloy.revanced.meta.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.strings

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returns("Z")
            strings("Is ad pod")
        }
    }.single()
}
