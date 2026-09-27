package io.github.nexalloy.revanced.telegram.calls

import android.app.Activity
import android.app.AlertDialog
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField
import java.lang.reflect.Modifier

private val confirmedCall = ThreadLocal.withInitial { false }

private fun callTargetName(target: Any): String {
    val title = target.readTelegramField("title") as? String
    if (!title.isNullOrBlank()) return title

    val first = target.readTelegramField("first_name") as? String
    val last = target.readTelegramField("last_name") as? String
    return listOfNotNull(first, last)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "this contact" }
}

val ConfirmOutgoingCalls = patch(
    name = "Confirm outgoing calls",
    description = "Shows a confirmation dialog before starting Telegram user or group calls to reduce accidental calls.",
    use = false,
) {
    val helper =
        classLoader.findTelegramClassOrNull("org.telegram.ui.Components.voip.VoIPHelper")
            ?: return@patch

    helper.declaredMethods
        .filter {
            it.name == "startCall" &&
                Modifier.isStatic(it.modifiers) &&
                it.returnType == Void.TYPE &&
                it.parameterTypes.isNotEmpty() &&
                (
                    it.parameterTypes[0].name.endsWith("TLRPC\$User") ||
                    it.parameterTypes[0].name.endsWith("TLRPC\$Chat")
                ) &&
                it.parameterTypes.any { type -> Activity::class.java.isAssignableFrom(type) }
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    if (confirmedCall.get() == true) return@before

                    val activity = param.args.firstOrNull { it is Activity } as? Activity
                        ?: return@before
                    if (activity.isFinishing) return@before

                    val target = param.args.firstOrNull() ?: return@before
                    val copiedArgs = param.args.copyOf()
                    val name = callTargetName(target)

                    AlertDialog.Builder(activity)
                        .setTitle("Confirm call")
                        .setMessage("Start a call with $name?")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            confirmedCall.set(true)
                            try {
                                method.invoke(null, *copiedArgs)
                            } finally {
                                confirmedCall.set(false)
                            }
                        }
                        .show()

                    param.result = null
                }
            }
        }
}
