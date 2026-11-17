package io.github.nexalloy.morphe.youtube.interaction.onthego

import app.morphe.extension.youtube.patches.onthego.OnTheGoPatch
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_05_or_greater
import io.github.nexalloy.patch

// State dependency used by swipe controls while the app's On-the-go panel covers the player.
internal val onTheGoStateHookPatch = patch {
    dependsOn(VersionCheck)
    if (is_21_05_or_greater) {
        val getter = ::onTheGoStateGetterMethod.method
        val setter = ::onTheGoStateSetterMethod.method
        ::onTheGoStateConstructor.hookMethod {
            after {
                val state = it.thisObject
                OnTheGoPatch.setOnTheGoState(object : OnTheGoPatch.OnTheGoState {
                    override fun patch_isOnTheGoActive() = getter.invoke(state) as Boolean
                    override fun patch_setOnTheGoActive(active: Boolean) { setter.invoke(state, active) }
                })
            }
        }
    }
}
