package io.github.nexalloy.revanced.truecaller.premium

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint

private val premiumNavigationClassFingerprint = fingerprint {
    methodMatcher { name = "<init>" }
    strings("premiumPurchaseSupportedCheck", "premiumButton", "callsButton")
}

val premiumNavigationEnabledFingerprint = findMethodDirect {
    val clazz = premiumNavigationClassFingerprint.invoke(this).declaredClass
        ?: error("Truecaller premium navigation class not found")

    clazz.findMethod {
        matcher {
            name = "b"
            returnType = "boolean"
            paramCount = 0
        }
    }.single()
}

val premiumNavDrawerOnAttachFingerprint = fingerprint {
    returns("V")
    parameters()
    methodMatcher { name = "onAttachedToWindow" }
    classMatcher {
        descriptor = "Lcom/truecaller/premium/ui/PremiumNavDrawerItemView;"
    }
}
