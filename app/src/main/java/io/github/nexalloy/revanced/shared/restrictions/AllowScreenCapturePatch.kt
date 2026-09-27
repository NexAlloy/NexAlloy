package io.github.nexalloy.revanced.shared.restrictions

import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private const val SECURE_FLAG = WindowManager.LayoutParams.FLAG_SECURE

val AllowScreenCapture = patch(
    name = "Allow screenshots and screen recording",
    description = "Removes Android FLAG_SECURE restrictions inside this app so screenshots and screen recording can work.",
    use = false,
) {
    Window::class.java.declaredMethods
        .filter {
            it.name == "addFlags" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val flags = param.args.firstOrNull() as? Int ?: return@before
                    param.args[0] = flags and SECURE_FLAG.inv()
                }
            }
        }

    Window::class.java.declaredMethods
        .filter {
            it.name == "setFlags" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                it.parameterTypes[1] == Int::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val flags = param.args.getOrNull(0) as? Int ?: return@before
                    param.args[0] = flags and SECURE_FLAG.inv()
                }
            }
        }

    Window::class.java.declaredMethods
        .filter {
            it.name == "setAttributes" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == WindowManager.LayoutParams::class.java
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    val attributes =
                        param.args.firstOrNull() as? WindowManager.LayoutParams ?: return@before
                    attributes.flags = attributes.flags and SECURE_FLAG.inv()
                }
            }
        }

    SurfaceView::class.java.declaredMethods
        .filter {
            it.name == "setSecure" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType
        }
        .forEach { method ->
            method.hookMethod {
                before { param ->
                    if (param.args.isNotEmpty()) param.args[0] = false
                }
            }
        }
}
