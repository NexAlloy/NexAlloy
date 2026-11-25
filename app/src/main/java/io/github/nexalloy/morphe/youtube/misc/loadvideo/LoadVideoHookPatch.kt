/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.misc.loadvideo

import android.content.Intent
import android.os.Parcelable
import app.morphe.extension.youtube.patches.LoadVideoPatch
import app.morphe.extension.youtube.patches.LoadVideoPatch.InlinePlaybackInterface
import app.morphe.extension.youtube.patches.LoadVideoPatch.MainActivityInterface
import app.morphe.extension.youtube.patches.LoadVideoPatch.PlayerInterface
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

internal val loadVideoHookPatch = patch(
    description = "Hooks the player so the extension can dismiss it and open videos."
) {
    val dismissPlayer = ::dismissPlayerMethod.method
    val openNewVideoParcelable = OpenNewVideoIntentParcelableFingerprint.method

    // Add interface and helper methods to allow extension code to call obfuscated methods.
    // Add methods to access obfuscated player methods.
    ::playerControllerConstructor.hookMethod {
        after {
            LoadVideoPatch.initialize(it.thisObject.createProxy { reference ->
                object : PlayerInterface {
                    override fun patch_dismissPlayer() {
                        reference.get()?.let { player -> dismissPlayer(player) }
                    }

                    override fun patch_getIntentParcelable(intent: Intent): Parcelable? =
                        openNewVideoParcelable(null, intent) as Parcelable?
                }
            })
        }
    }

    // Once the player is dismissed, a video in the feed below it can start playing
    // and take the place of the video opened next, so the extension blocks it.
    val blockInlinePlayback = BlockInlinePlaybackFingerprint.method
    val unblockInlinePlayback = UnblockInlinePlaybackFingerprint.method
    ::inlinePlaybackConstructor.hookMethod {
        after {
            LoadVideoPatch.initializeInlinePlayback(it.thisObject.createProxy { reference ->
                object : InlinePlaybackInterface {
                    override fun patch_blockInlinePlayback(reason: Int) {
                        reference.get()?.let { controller -> blockInlinePlayback(controller, reason) }
                    }

                    override fun patch_unblockInlinePlayback(reason: Int) {
                        reference.get()?.let { controller -> unblockInlinePlayback(controller, reason) }
                    }
                }
            })
        }
    }

    // Activity.onNewIntent() is protected, so the extension calls it through a public method.
    val onNewIntent = DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onNewIntent(Landroid/content/Intent;)V")
        .toMethod()
    LoadVideoPatch.setMainActivityAdapter { activity ->
        MainActivityInterface { intent -> onNewIntent(activity, intent) }
    }

    BackButtonFinishActivityOnNewVideoIntentFingerprint.hookMethod {
        before { it.result = null }
    }
}
