package io.github.nexalloy.morphe.youtube.misc.commandrouter

import android.app.Activity
import app.morphe.extension.youtube.patches.CommandRouterPatch
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.youtube.interaction.downloads.GeneratedRegistryFingerprint
import io.github.nexalloy.morphe.youtube.interaction.downloads.ProtobufClassParseByteArrayWithRegistryFingerprint
import io.github.nexalloy.patch

internal val commandRouterHookPatch = patch(
    description = "Hooks the command router of each activity so the extension can run commands."
) {
    val executeCommand = CommandRouterExecuteCommandFingerprint.method
    val defaultCommand = ::commandDefaultInstance.field.get(null)
    val parseCommand = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    CommandRouterConstructorFingerprint.hookMethod {
        after {
            val adapter = it.thisObject.createProxy { reference ->
                CommandRouterPatch.CommandRouter { command, context ->
                    reference.get()?.let { router ->
                        val proto = parseCommand.invoke(null, defaultCommand, command, registry)
                        executeCommand.invoke(router, proto, context)
                    }
                }
            }
            CommandRouterPatch.setCommandRouter(adapter, it.args[0] as Activity)
        }
    }
}
