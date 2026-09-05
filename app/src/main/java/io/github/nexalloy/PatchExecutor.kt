@file:OptIn(DexKitExperimentalApi::class)

package io.github.nexalloy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface
import io.github.nexalloy.BuildConfig.CI_BUILD
import io.github.nexalloy.BuildConfig.DEBUG
import io.github.nexalloy.morphe.Fingerprint
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty0
import kotlin.system.measureTimeMillis

typealias FindFunc = DexKitBridge.() -> Any
typealias FindClassFunc = DexKitBridge.() -> ClassData
typealias FindMethodFunc = DexKitBridge.() -> MethodData
typealias FindMethodListFunc = DexKitBridge.() -> List<MethodData>
typealias FindFieldFunc = DexKitBridge.() -> FieldData

fun patch(
    name: String = "",
    description: String = "",
    use: Boolean = true,
    func: PatchExecutor.() -> Unit
) =
    Patch(name, description, use, func)

class Patch(
    val name: String,
    val description: String,
    val use: Boolean,
    val run: PatchExecutor.() -> Unit
)

abstract class IHook(val xposed: XposedInterface) : XposedInterface by xposed {
    abstract val classLoader: ClassLoader

    fun DexMethod.hookMethod(callback: XC_MethodHook) {
        toMember().hookMethod(callback)
    }

    fun DexMethod.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        toMember().hookMethod(block)
    }

    fun DexClass.toClass() = getInstance(classLoader)
    fun DexMethod.toMethod(): Method {
        var clz = classLoader.loadClass(className)
        do {
            return XposedHelpers.findMethodExactIfExists(clz, name, *paramTypeNames.toTypedArray())
                ?: continue
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $this not found")
    }

    fun DexMethod.toConstructor(): Constructor<*> {
        var clz = classLoader.loadClass(className)
        do {
            return XposedHelpers.findConstructorExactIfExists(clz, *paramTypeNames.toTypedArray())
                ?: continue
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $this not found")
    }

    fun DexMethod.toMember(): Member {
        return when {
            isMethod -> toMethod()
            isConstructor -> toConstructor()
            else -> throw NotImplementedError()
        }
    }

    fun DexField.toField() = getFieldInstance(classLoader)

    override fun getApiVersion(): Int = xposed.apiVersion
}

private const val CACHE_LIST_PREFIX = "list-v1:"

internal fun encodeCacheStringList(value: List<String>): String = buildString {
    append(CACHE_LIST_PREFIX)
    value.forEach { item ->
        append(item.length)
        append(':')
        append(item)
    }
}

internal fun decodeCacheStringList(value: String): List<String>? {
    if (!value.startsWith(CACHE_LIST_PREFIX)) return null

    val result = mutableListOf<String>()
    var index = CACHE_LIST_PREFIX.length
    while (index < value.length) {
        val separator = value.indexOf(':', index)
        if (separator < 0) return null
        val length = value.substring(index, separator).toIntOrNull() ?: return null
        if (length < 0) return null
        val itemStart = separator + 1
        if (length > value.length - itemStart) return null
        val itemEnd = itemStart + length
        result += value.substring(itemStart, itemEnd)
        index = itemEnd
    }
    return result
}

internal fun shouldUseDexKitCache(
    cachedId: String?,
    expectedId: String,
    debug: Boolean,
    ciBuild: Boolean,
): Boolean = cachedId == expectedId && (!debug || ciBuild)

class SharedPrefCache(app: Application) : DexKitCacheBridge.Cache {
    val pref = app.getSharedPreferences("xpmorphe", MODE_PRIVATE)!!
    private val map = ConcurrentHashMap<String, String>().apply {
        pref.all.forEach { (key, value) ->
            if (value is String) put(key, value)
        }
    }

    override fun clearAll() {
        map.clear()
    }

    override fun getString(key: String, default: String?): String? = map[key] ?: default

    override fun getAllKeys(): Collection<String> = map.keys.toList()

    override fun getStringList(
        key: String, default: List<String>?
    ): List<String>? = map[key]?.let(::decodeCacheStringList) ?: default

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun putStringList(key: String, value: List<String>) {
        map[key] = encodeCacheStringList(value)
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    fun saveCache() {
        val snapshot = map.toMap()
        val edit = pref.edit()
        edit.clear()
        snapshot.forEach { (k, v) ->
            edit.putString(k, v)
        }
        edit.commit()
    }
}

class DependedHookFailedException(
    subHookName: String, exception: Throwable
) : Exception("Depended hook $subHookName failed.", exception)

@SuppressLint("CommitPrefEdits")
class PatchExecutor(
    val appContext: Application,
    val lpparam: XposedModuleInterface.PackageReadyParam,
    xposed: XposedInterface
) : IHook(xposed) {
    override val classLoader = lpparam.classLoader

    /**
     * @see io.github.nexalloy.activity.AppPatchSettingsActivity.AppPatchSettingsFragment.onCreate
     * */
    private val patchPreferences = xposed.getRemotePreferences(lpparam.packageName)

    private lateinit var patches: Array<Patch>
    private val appliedPatches = mutableSetOf<Patch>()
    private val failedPatchErrors = mutableMapOf<Patch, Throwable>()
    private val failedPatches = mutableListOf<Patch>()

    // cache
    private val moduleRel = BuildConfig.COMMIT_HASH
    private var cache = SharedPrefCache(appContext)
    private var dexkit = run {
        System.loadLibrary("dexkit")
        DexKitCacheBridge.init(cache)
        DexKitCacheBridge.create("", lpparam.applicationInfo.sourceDir)
    }

    fun applyPatches(patches: Array<Patch>) {
        this.patches = patches
        val t = measureTimeMillis {
            loadCacheIfValid()
            try {
                executePatches()
                finalizePatching()
                logDebugInfo()
            } finally {
                dexkit.close()
            }
        }
        Logger.printDebug { "${lpparam.packageName} handleLoadPackage: ${t}ms" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadCacheIfValid() {
        // Cache is normally disabled for local debug builds so fingerprint changes can be
        // tested without stale data. CI debug artifacts are immutable per commit, so keeping
        // their cache makes repeated on-device cold starts representative of release builds.
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)

        val id = "${packageInfo.lastUpdateTime}-$moduleRel"
        val cachedId = cache.getString("id", null)
        val isCached = shouldUseDexKitCache(cachedId, id, DEBUG, CI_BUILD)

        Logger.printInfo { "cache ID : $id" }
        Logger.printInfo { "cached ID: ${cachedId ?: ""}" }
        Logger.printInfo { "Using cached keys: $isCached" }

        if (!isCached) {
            cache.clearAll()
            cache.putString("id", id)
            Utils.showToastLong("NexAlloy is initializing, please wait...")
        }
    }

    private fun executePatches() {
        patches.forEach { hook ->
            if (appliedPatches.contains(hook)) return@forEach
            if (failedPatchErrors.containsKey(hook)) {
                if (!failedPatches.contains(hook)) failedPatches.add(hook)
                return@forEach
            }
            /**
             * @see io.github.nexalloy.activity.AppPatchSettingsActivity.AppPatchSettingsFragment.onCreate
             * */
            val isEnabled = patchPreferences?.getBoolean(hook.name, hook.use) ?: hook.use
            if (!isEnabled) return@forEach // Pref Key
            runCatching { hook.run(this) }.onFailure { err ->
                failedPatchErrors[hook] = err
                XposedBridge.log(err)
                if (!failedPatches.contains(hook)) failedPatches.add(hook)
            }.onSuccess {
                appliedPatches.add(hook)
            }
        }
    }

    private fun finalizePatching() {
        cache.saveCache()
        val success = failedPatches.isEmpty()
        if (!success) {
            XposedBridge.log("${lpparam.applicationInfo.packageName} version: ${getAppVersion()}")
            Utils.showToastLong("Error while apply following patches:\n${failedPatches.joinToString { it.name }}")
        }
    }

    private fun logDebugInfo() {
        val success = failedPatches.isEmpty()
        if (DEBUG) {
            XposedBridge.log("${lpparam.applicationInfo.packageName} version: ${getAppVersion()}")
            if (success) {
                Utils.showToastLong("apply patches success")
            }
        }
    }

    private fun getAppVersion(): String {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") packageInfo.versionCode
        }
        return "$versionName ($versionCode)"
    }

    fun dependsOn(vararg patches: Patch) {
        patches.forEach { hook ->
            if (appliedPatches.contains(hook)) return@forEach
            failedPatchErrors[hook]?.let { err ->
                throw DependedHookFailedException(hook.name, err)
            }
            runCatching { hook.run(this) }.onFailure { err ->
                failedPatchErrors[hook] = err
                throw DependedHookFailedException(hook.name, err)
            }.onSuccess {
                appliedPatches.add(hook)
            }
        }
    }

    val KProperty0<FindMethodFunc>.dexMethod
        get() = getDexMethod(this.name, this.get())

    val KProperty0<FindMethodFunc>.method
        get() = dexMethod.toMethod()

    val KProperty0<FindMethodFunc>.constructor
        get() = dexMethod.toConstructor()

    val KProperty0<FindMethodFunc>.member
        get() = dexMethod.toMember()

    val KProperty0<FindMethodFunc>.memberOrNull
        get() = runCatching { this.member }.getOrNull()

    fun KProperty0<FindMethodFunc>.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        dexMethod.hookMethod(block)
    }

    fun KProperty0<FindMethodFunc>.hookMethod(callback: XC_MethodHook) {
        dexMethod.hookMethod(callback)
    }

    val KProperty0<FindMethodListFunc>.dexMethodList
        get() = getDexMethods(this.name, this.get())

    val KProperty0<FindFieldFunc>.dexField
        get() = getDexField(this.name, this.get())

    val KProperty0<FindFieldFunc>.field
        get() = dexField.toField()

    val KProperty0<FindFieldFunc>.declaredClass
        get() = classLoader.loadClass(dexField.declaredClassName)

    val KProperty0<FindFieldFunc>.type
        get() = classLoader.loadClass(dexField.className)

    val KProperty0<FindClassFunc>.dexClass
        get() = getDexClass(this.name, this.get())

    val KProperty0<FindClassFunc>.clazz
        get() = dexClass.toClass()

    // Fingerprint object extensions

    private val Fingerprint.cacheKey
        get() = this::class.simpleName ?: error("Anonymous Fingerprint has no cache key")

    fun Fingerprint.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        getDexMethod(cacheKey) { this@hookMethod.run() }.hookMethod(block)
    }

    fun Fingerprint.hookMethod(callback: XC_MethodHook) {
        getDexMethod(cacheKey) { this@hookMethod.run() }.hookMethod(callback)
    }

    val Fingerprint.dexMethod get() = getDexMethod(cacheKey) { this@dexMethod.run() }

    val Fingerprint.member get() = dexMethod.toMember()

    val Fingerprint.memberOrNull get() = runCatching { this.member }.getOrNull()

    val Fingerprint.method get() = dexMethod.toMethod()

    val Fingerprint.declaredClass get() = classLoader.loadClass(dexMethod.declaredClassName)

    val Fingerprint.constructor get() = dexMethod.toConstructor()

    private inline fun <reified T : Any> wrapFind(
        key: String,
        crossinline funcFunc: DexKitBridge.() -> T,
        crossinline serializer: (T) -> String
    ): DexKitBridge.() -> T? {
        return {
            try {
                funcFunc().also { Logger.printInfo { "$key Matches: ${serializer(it)}" } }
            } catch (e: Exception) {
                Logger.printInfo({ "Fingerprint $key Not Found" }, e)
                null
            }
        }
    }

    private inline fun <reified T : Any> wrapFindList(
        key: String,
        crossinline funcFunc: DexKitBridge.() -> List<T>,
        crossinline serializer: (T) -> String
    ): DexKitBridge.() -> List<T> {
        return {
            try {
                funcFunc().also {
                    Logger.printInfo { "$key Matches: ${it.joinToString { serializer(it) }}" }
                }
            } catch (e: Exception) {
                Logger.printInfo({ "Fingerprint $key Not Found" }, e)
                emptyList()
            }
        }
    }

    private inline fun getDexClass(
        key: String, crossinline findFunc: DexKitBridge.() -> ClassData
    ): DexClass = dexkit.getClassDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor })!!

    private inline fun getDexMethod(
        key: String, crossinline findFunc: DexKitBridge.() -> MethodData
    ): DexMethod = dexkit.getMethodDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor })!!

    private inline fun getDexField(
        key: String, crossinline findFunc: DexKitBridge.() -> FieldData
    ): DexField = dexkit.getFieldDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor })!!

    private inline fun getDexMethods(
        key: String, crossinline findFunc: DexKitBridge.() -> List<MethodData>
    ): List<DexMethod> = dexkit.getMethodsDirectOrEmpty(
        key, wrapFindList(key, findFunc) { it.descriptor })
}

val ExtensionResourceHook = patch {
    appContext.addModuleAssets()
    ResourceUtils.fallbackPackageName = BuildConfig.APPLICATION_ID

    appContext.callMethod(
        "registerActivityLifecycleCallbacks", object : Application.ActivityLifecycleCallbacks {
            var handleWebView: Boolean = false

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                Logger.printDebug { "onActivityCreated $activity" }
                if (!handleWebView) {
                    WebView(activity).destroy()
                    appContext.addModuleAssets()
                    handleWebView = true
                }

                activity.addModuleAssets()
            }

            override fun onActivityDestroyed(activity: Activity) {
                Logger.printDebug { "onActivityDestroyed $activity" }
            }

            override fun onActivityPaused(activity: Activity) {
                Logger.printDebug { "onActivityPaused $activity" }
            }

            override fun onActivityResumed(activity: Activity) {
                Logger.printDebug { "onActivityResumed $activity" }
                activity.addModuleAssets()
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
                Logger.printDebug { "onActivitySaveInstanceState $activity" }
            }

            override fun onActivityStarted(activity: Activity) {
                Logger.printDebug { "onActivityStarted $activity" }
            }

            override fun onActivityStopped(activity: Activity) {
                Logger.printDebug { "onActivityStopped $activity" }
            }
        })
}
