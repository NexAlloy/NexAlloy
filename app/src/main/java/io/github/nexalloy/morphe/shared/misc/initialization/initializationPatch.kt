package io.github.nexalloy.morphe.shared.misc.initialization

import app.morphe.extension.shared.patches.InitializationPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook


internal fun initializationPatch() = patch (
    description = "Prompts to restart the app on first load of a clean install",
) {
    GlobalConfigGroupFingerprint.hookMethod(scopedHook(::handleColdFingerprint.member) {
        InitializationPatch.onGlobalConfigUpdated()
    })
}
