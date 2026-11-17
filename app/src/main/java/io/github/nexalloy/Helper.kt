package io.github.nexalloy

import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedInterface.PRIORITY_DEFAULT
import java.io.File
import java.lang.Enum.valueOf as javaEnumValueOf
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.util.WeakHashMap

typealias IScopedHookCallback = ScopedHookParam.(MethodHookParam) -> Unit
typealias IHookCallback = (MethodHookParam) -> Unit

fun <T> Array<T>.atLast(index: Int) = this[this.size - index]

private val proxyRef = WeakHashMap<Any, Any>()

/*
 * Bind a proxy to the implement's lifecycle via WeakHashMap.
 *
 *
 * Do not hold a strong reference to `impl` inside the proxy!
 * It will create a strong-reference loop and cause memory leaks.
 *
 * See Implementation note https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/WeakHashMap.html
 * */
internal fun <TImpl> TImpl.bindProxy(proxy: Any) {
    synchronized(proxyRef) {
        proxyRef[this] = proxy
    }
}

/*
 * Creates a GC-safe proxy bound to the implement's lifecycle via WeakHashMap.
 *
 * WARN: Capturing the host instance directly in the closure will create a strong-reference loop and cause memory leaks.
 * */
internal inline fun <reified TProxy : Any, TImpl> TImpl.createProxy(crossinline createProxy: (impl: WeakReference<TImpl>) -> TProxy): TProxy {
    val hostRef = WeakReference(this)
    val proxy = createProxy(hostRef)
    this.bindProxy(proxy)
    return proxy
}

class HookDsl<TCallback>(emptyCallback: TCallback) {
    var priority: Int = PRIORITY_DEFAULT
    var before: TCallback = emptyCallback
    var after: TCallback = emptyCallback

    fun before(f: TCallback) {
        this.before = f
    }

    fun after(f: TCallback) {
        this.after = f
    }
}

fun Member.hookMethod(cb: XC_MethodHook) {
    XposedBridge.hookMethod(this, cb)
}

inline fun Member.hookMethod(
    crossinline block: HookDsl<IHookCallback>.() -> Unit
) {
    val builder = HookDsl<IHookCallback> {}.apply(block)
    hookMethodInternal(builder.before, builder.after, builder.priority)
}

inline fun Member.hookMethodInternal(
    crossinline before: IHookCallback, crossinline after: IHookCallback, priority: Int
) {
    XposedBridge.hookMethod(this, object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) {
            before(param)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            after(param)
        }
    })
}

data class ScopedHookParam(
    val outerParam: MethodHookParam,
    val innerDepth: Int
)

fun scopedHook(vararg pairs: Pair<Member, HookDsl<IScopedHookCallback>.() -> Unit>): XC_MethodHook {
    val hook = ScopedHook()
    pairs.forEach { (member, block) ->
        val builder = HookDsl<IScopedHookCallback> {}.apply(block)
        hook.hookInnerMethod(member, builder.before, builder.after)
    }
    return hook
}

inline fun scopedHook(
    hookMethod: Member, crossinline f: HookDsl<IScopedHookCallback>.() -> Unit
): XC_MethodHook {
    val hook = ScopedHook()
    val builder = HookDsl<IScopedHookCallback> {}.apply(f)
    hook.hookInnerMethod(hookMethod, builder.before, builder.after)
    return hook
}

class ScopedHook : XC_MethodHook() {
    inline fun hookInnerMethod(
        hookMethod: Member,
        crossinline before: IScopedHookCallback,
        crossinline after: IScopedHookCallback
    ) {
        hookMethod.hookMethod {
            before {
                val outerParam = outerParam.get() ?: return@before
                val depth = innerDepth.get() ?: 0
                innerDepth.set(depth + 1)
                before(ScopedHookParam(outerParam, depth), it)
            }

            after {
                val outerParam = outerParam.get() ?: return@after
                val depth = ((innerDepth.get() ?: 0) - 1).coerceAtLeast(0)
                innerDepth.set(depth)
                try {
                    after(ScopedHookParam(outerParam, depth), it)
                } finally {
                    if (depth == 0) innerDepth.remove()
                }
            }

        }
    }

    val outerParam: ThreadLocal<MethodHookParam> = ThreadLocal<MethodHookParam>()
    val innerDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

    override fun beforeHookedMethod(param: MethodHookParam) {
        outerParam.set(param)
        innerDepth.set(0)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        outerParam.remove()
        innerDepth.remove()
    }
}

lateinit var modulePath: String

private val resourceLoader by lazy @RequiresApi(Build.VERSION_CODES.R) {
    val fileDescriptor = ParcelFileDescriptor.open(
        File(modulePath), ParcelFileDescriptor.MODE_READ_ONLY
    )
    val provider = ResourcesProvider.loadFromApk(fileDescriptor)
    val loader = ResourcesLoader()
    loader.addProvider(provider)
    return@lazy loader
}

fun Context.addModuleAssets() {
    val moduleFile = File(modulePath)
    if (!moduleFile.exists()) {
        Utils.showToastLong("NexAlloy has been updated")
        Utils.restartApp(this)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        resources.addLoaders(resourceLoader)
        return
    }

    resources.assets.callMethod("addAssetPath", moduleFile.path)
}

fun injectHostClassLoaderToSelf(self: ClassLoader, host: ClassLoader) {
    val findClassMethod =
        XposedHelpers.findMethodExact(ClassLoader::class.java, "findClass", String::class.java)
    self.setObjectField("parent", object : ClassLoader(self.parent) {
        /**
         * In the context of Xposed modules, the class loading hierarchy can be complex.
         * The module's classes are loaded by its own ClassLoader (`self`).
         * The host application's classes are loaded by its ClassLoader (`host`).
         *
         * The goal here is to allow the module to access classes from the host application.
         * We achieve this by creating a new ClassLoader that becomes the parent of `self`.
         * This new parent ClassLoader will first attempt to load classes using `self.findClass()`.
         * If that fails, it will then try to load the class from the `host` ClassLoader.
         *
         * This explicit ordering is crucial for compatibility with various Xposed frameworks:
         * - **LSPosed:** LSPosed's `LspModuleClassLoader` already prioritizes its own `findClass`
         *   before delegating to `parent.loadClass`. So, this customization might seem redundant for LSPosed.
         *
         * - **Other Xposed Frameworks:** Other frameworks might use a standard `PathClassLoader`
         *   as the module's ClassLoader. It delegates to its parent before trying its own classes,
         *   so explicitly checking module classes here keeps the same module-first ordering.
         *
         * By inserting this intermediary ClassLoader and overriding `findClass` to prioritize
         * `self.findClass()`, we ensure that the module's classes are always checked first,
         * preventing potential infinite recursion and ensuring that stubbed classes are loaded
         * from the host only as a fallback.
         */
        override fun findClass(name: String): Class<*> {
            try {
                return findClassMethod(self, name) as Class<*>
            } catch (_: InvocationTargetException) {
            }

            try {
                return host.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }

            throw ClassNotFoundException(name)
        }
    })
}

@Suppress("UNCHECKED_CAST")
fun Class<*>.enumValueOf(name: String): Enum<*>? {
    return try {
        javaEnumValueOf(this as Class<out Enum<*>>, name)
    } catch (_: IllegalArgumentException) {
        null
    }
}
