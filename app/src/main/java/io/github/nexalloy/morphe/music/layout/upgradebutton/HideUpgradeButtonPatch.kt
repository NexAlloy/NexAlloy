package io.github.nexalloy.morphe.music.layout.upgradebutton

import io.github.nexalloy.patch
import java.util.ArrayList

val HideUpgradeButton = patch(
    name = "Hide upgrade button",
    description = "Hides the upgrade tab from the pivot bar.",
) {
    val pivotBarClass = classLoader.loadClass(::pivotBarConstructorFingerprint.dexMethod.declaredClassName)
    val pivotBarElementField = pivotBarClass.declaredFields
        .filter { ArrayList::class.java.isAssignableFrom(it.type) }
        .single()
        .apply { isAccessible = true }

    ::pivotBarConstructorFingerprint.hookMethod {
        after { param ->
            val list = pivotBarElementField.get(param.thisObject) as ArrayList<*>
            if (list.size > 4) list.removeAt(4)
        }
    }
}
