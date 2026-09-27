package io.github.nexalloy.revanced.duolingo.ads

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableAds = patch(
    name = "Disable ads",
    description = "Forces Duolingo's internal disable_ads debug preference on at runtime.",
) {
    val preferencesClass = Class.forName("android.app.SharedPreferencesImpl")
    val getBoolean = preferencesClass.getDeclaredMethod(
        "getBoolean",
        String::class.java,
        Boolean::class.javaPrimitiveType!!,
    )

    getBoolean.hookMethod {
        before { param ->
            if (param.args.firstOrNull() == "disable_ads") {
                param.result = true
            }
        }
    }
}
