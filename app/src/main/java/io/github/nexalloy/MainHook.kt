package io.github.nexalloy

import android.app.Application
import app.morphe.extension.shared.ResourceType
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.nexalloy.common.UpdateChecker
import io.github.nexalloy.morphe.ResourceFinder
import io.github.nexalloy.morphe.resourceMappings

class MainHook : XposedModule() {
    lateinit var param: PackageReadyParam
    lateinit var app: Application
    var targetPackageName: String? = null

    fun shouldHook(packageName: String): Boolean {
        if (!patchesByPackage.containsKey(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        modulePath = moduleApplicationInfo.sourceDir
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage) return
        if (!shouldHook(param.packageName)) return
        this.param = param

        inContext(param) { app ->
            this.app = app
            if (isReVancedPatched(param)) {
                Utils.showToastLong("NexAlloy module does not work with patched app")
                return@inContext
            }

            resourceMappings = object : ResourceFinder {
                override operator fun get(type: String, name: String): Int {
                    val id = ResourceUtils.getIdentifier(ResourceType.fromValue(type), name)
                    if (id == 0) throw Exception("Could not find resource type: $type name: $name")
                    return id
                }
            }

            val patches = patchesByPackage[param.packageName] ?: return@inContext
            PatchExecutor(app, param, this).applyPatches(patches)
        }
    }

    private fun isReVancedPatched(param: PackageReadyParam): Boolean {
        return runCatching {
            param.classLoader.loadClass("app.morphe.extension.shared.Utils")
        }.isSuccess || runCatching {
            param.classLoader.loadClass("app.morphe.extension.shared.utils.Utils")
        }.isSuccess || runCatching {
            param.classLoader.loadClass("app.revanced.integrations.shared.Utils")
        }.isSuccess || runCatching {
            param.classLoader.loadClass("app.revanced.integrations.shared.utils.Utils")
        }.isSuccess
    }
}

context(xposed: XposedInterface)
fun inContext(lpparam: PackageReadyParam, f: (Application) -> Unit) {
    val appClassName = lpparam.applicationInfo.className ?: Application::class.java.name
    val appClazz = XposedHelpers.findClass(appClassName, lpparam.classLoader)
    appClazz.getMethod("onCreate").hookMethod {
        before {
            val app = it.thisObject as Application
            Utils.setContext(app)
            f(app)
            if (modulePath.startsWith("/data/app/")) {
                val prefs = xposed.getRemotePreferences("prefs")
                if (!prefs.getBoolean("disable_auto_check_update", false)) {
                    UpdateChecker().hookNewActivity()
                }
            }
        }
    }
}
