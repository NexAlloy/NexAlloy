package io.github.nexalloy.morphe.youtube.misc.backgroundplayback

import app.morphe.extension.youtube.patches.BackgroundPlaybackPatch
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import io.github.nexalloy.atLast
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.shared.misc.debugging.experimentalIndexedBooleanFeatureFlagFingerprint
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playercontrols.disableNewPlayerControlsFeatureFlag
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_29_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_49_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_15_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_21_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_23_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_36_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_42_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val BackgroundPlayback = patch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
) {

    dependsOn(VersionCheck)

    PreferenceScreen.SHORTS.addPreferences(
        SwitchPreference("morphe_shorts_disable_background_playback")
    )

    PreferenceScreen.MISC.addPreferences(
        SwitchPreference("morphe_remove_background_playback_restrictions"),
        ListPreference("morphe_auto_pause_on_screen_lock")
    )

    onCreateHook(BackgroundPlaybackPatch::initialize)

    PreferenceScreen.SHORTS.addPreferences(
        SwitchPreference("morphe_shorts_disable_background_playback"),
    )

    BackgroundPlaybackManagerFingerprint.hookMethod {
        after {
            it.result = BackgroundPlaybackPatch.isBackgroundPlaybackAllowed(it.result as Boolean)
        }
    }

    ::backgroundPlaybackManagerShortsFingerprint.dexMethodList.forEach {
        it.hookMethod {
            after {
                it.result =
                    BackgroundPlaybackPatch.isBackgroundShortsPlaybackAllowed(it.result as Boolean)
            }
        }
    }

    // Enable background playback option in YouTube settings
    val backgroundPlaybackAllowedMatch = if (is_21_23_or_greater)
        ::backgroundPlaybackSettingsSubFingerprint
    else
        ::backgroundPlaybackSettingsLegacySubFingerprint
    backgroundPlaybackAllowedMatch.hookMethod(returnConstant(true))

    // Prevents playback from resuming if it was interrupted from the notification
    // and the app was subsequently brought to the foreground.
    if (is_21_15_or_greater) {
        val accessors = mutableListOf(::experimentalBooleanFeatureFlagFingerprint.method)
        if (is_21_42_or_greater) accessors += ::experimentalIndexedBooleanFeatureFlagFingerprint.method
        ::playingWhenBackgroundedMethods.dexMethodList.forEach { method ->
            accessors.forEach { accessor ->
                method.hookMethod(scopedHook(accessor) {
                    after {
                        if (it.args.atLast(2) == 45770945L) {
                            it.result = BackgroundPlaybackPatch.isAutomaticForegroundPlaybackAllowed(it.result as Boolean)
                        }
                    }
                })
            }
        }
    }

    // Prevents playback from pausing when the overlay video settings is invoked.
    if (is_20_49_or_greater) {
        insertLiteralOverride(45741823L, BackgroundPlaybackPatch::isAutomaticPlaybackPauseInFlyout)
    }

    // Force allowing background play for Shorts.
    insertLiteralOverride(45415425, true)

    // Force allowing background play for videos labeled for kids.
    KidsBackgroundPlaybackPolicyControllerFingerprint.hookMethod(returnConstant(Unit))

    // Fix PiP buttons not working after locking/unlocking device screen.
    if (!is_21_21_or_greater) {
        insertLiteralOverride(45638483L)
    }

    if (is_20_29_or_greater && !is_21_36_or_greater) {
        // Client flag that interferes with background playback of some video types.
        // Exact purpose is not clear and it's used in ~ 100 locations.
        // Flag cannot be forced off with 21.36+ or the player seekbar is missing.
        //
        // Edit: This override may not be needed and only 45752335L override might be needed.
        insertLiteralOverride(45698813L)
    }

    // If NewPlayerTypeEnumFeatureFlagFingerprint is overridden then must also
    // force off new player control flags otherwise player has no buttons visible.
    disableNewPlayerControlsFeatureFlag()
}
