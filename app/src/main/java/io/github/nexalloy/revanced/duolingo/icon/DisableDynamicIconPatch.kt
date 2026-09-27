package io.github.nexalloy.revanced.duolingo.icon

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val DisableDynamicIcon = patch(
    name = "Disable dynamic app icon",
    description = "Prevents Duolingo from switching launcher activity-alias icons at runtime.",
    use = false,
) {
    val launcherIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(appContext.packageName)

    val launcherComponents = appContext.packageManager
        .queryIntentActivities(launcherIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
        .map {
            ComponentName(
                it.activityInfo.packageName,
                it.activityInfo.name,
            )
        }
        .toSet()

    val packageManagerImpl = runCatching {
        Class.forName("android.app.ApplicationPackageManager")
    }.getOrNull()

    if (packageManagerImpl != null) {
        packageManagerImpl.declaredMethods
            .filter {
                it.name == "setComponentEnabledSetting" &&
                    it.returnType == Void.TYPE &&
                    it.parameterTypes.firstOrNull() == ComponentName::class.java
            }
            .forEach { method ->
                method.hookMethod {
                    before { param ->
                        val component = param.args.firstOrNull() as? ComponentName ?: return@before
                        if (component in launcherComponents) {
                            param.result = null
                        }
                    }
                }
            }
    }
}
