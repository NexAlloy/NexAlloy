/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/1837
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.layout.flyout

import app.morphe.extension.youtube.patches.AddToQueuePatch
import app.morphe.extension.youtube.patches.LocalQueuePatch
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.layout.hide.general.ContextualMenuItemBuilderOnClickFingerprint
import io.github.nexalloy.morphe.youtube.layout.hide.general.contextualMenuItemMessageField
import io.github.nexalloy.morphe.youtube.layout.playlistautoplay.navigationIntentHook
import io.github.nexalloy.morphe.youtube.misc.auth.authHookPatch
import io.github.nexalloy.morphe.youtube.misc.loadvideo.loadVideoHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_05_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.playerStatusOverrideHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val addToQueuePatch = patch(
    name = "Add to queue",
    description = "Overrides the feed flyout 'Play next in queue' with the Morphe video queue."
) {
    dependsOn(flyoutPatch, authHookPatch, loadVideoHookPatch, VideoInformationPatch)

    PreferenceScreen.FEED.addPreferences(
        noTitleUnsortedPreferenceCategory(
            // TODO: Port the native playlist queue before exposing its queue type.
            // ListPreference("morphe_queue_type"),
            ListPreference(
                key = "morphe_queue_type",
                entriesKey = "morphe_queue_type_legacy_entries",
                entryValuesKey = "morphe_queue_type_legacy_entry_values"
            ),
            SwitchPreference("morphe_queue_add_flyout_menu", summary = true),
            // TODO Native playlist queue paging requires its unported queue engine.
            // SwitchPreference("morphe_native_queue_fetch_on_scroll", summary = true)
        )
    )

    val runnableField = ::feedFlyoutButtonRunnableField.field
    FeedFlyoutButtonsInitializerFingerprint.hookMethod(scopedHook(::feedFlyoutMenuItemOptionalMethod.method) {
        before {
            val menuItem = it.args[0]
            if (runnableField.declaringClass.isInstance(menuItem)) {
                runnableField.set(menuItem, AddToQueuePatch.replaceButtonRunnable(runnableField.get(menuItem) as Runnable?))
            }
        }
    })

    val messageField = ::contextualMenuItemMessageField.field
    val getSubMessage = ::getSubMessageReference.method
    val enumIntField = ::enumIntField.field
    val enumMethodCall = ::enumMethodCall.method
    ContextualMenuItemBuilderOnClickFingerprint.hookMethod {
        before {
            val message = messageField.get(it.thisObject)
            if (!getSubMessage.parameterTypes.single().isInstance(message)) return@before
            val buttonInfo = getSubMessage(null, message)
            val buttonName = (enumMethodCall(null, enumIntField.get(buttonInfo)) as Enum<*>).name
            if (AddToQueuePatch.replaceOnItemClick(buttonName)) {
                it.result = null
            }
        }
    }

    if (!is_21_05_or_greater) {
        FeedFlyoutButtonsInitializerOnItemClickFingerprint.hookMethod {
            before {
                if (AddToQueuePatch.replaceOnItemClick(it.args[2] as Int)) {
                    it.result = null
                }
            }
        }
    }

    navigationIntentHook(LocalQueuePatch::shouldCancelNavigation, includePlayerButtons = true)

    playerStatusOverrideHook(LocalQueuePatch::shouldCancelEndOfVideo)

    LocalQueueOnUserLeaveHintFingerprint.hookMethod {
        before { LocalQueuePatch.onUserLeaveHint() }
    }

    setExtensionIsPatchIncluded(AddToQueuePatch::class.java)
}
