package io.github.nexalloy.revanced.truecaller.misc

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint

val appStartTrackerFingerprint = fingerprint {
    returns("V")
    parameters()
    methodMatcher { name = "enableTracking" }
    classMatcher {
        descriptor = "Lcom/truecaller/analytics/technical/AppStartTracker;"
    }
}

private val inAppUpdateManagerClassFingerprint = fingerprint {
    strings("playAppUpdateManager", "configsInventory")
}

val shouldTriggerUpdateFingerprint = findMethodDirect {
    val clazz = inAppUpdateManagerClassFingerprint.invoke(this).declaredClass
        ?: error("Truecaller update manager class not found")

    clazz.findMethod {
        matcher {
            returnType = "boolean"
            paramTypes = listOf("com.truecaller.inappupdate.UpdateTrigger")
        }
    }.single()
}
