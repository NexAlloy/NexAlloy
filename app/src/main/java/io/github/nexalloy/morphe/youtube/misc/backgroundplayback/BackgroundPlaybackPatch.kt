package io.github.nexalloy.morphe.youtube.misc.backgroundplayback

import app.morphe.extension.youtube.patches.BackgroundPlaybackPatch
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_29_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_04_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_21_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val BackgroundPlayback = patch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
) {

    dependsOn(VersionCheck)

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
    ::backgroundPlaybackSettingsSubFingerprint.hookMethod(returnConstant(true))

    // Force allowing background play for Shorts.
    ShortsBackgroundPlaybackFeatureFlagFingerprint.hookMethod(returnConstant(true))

    // Force allowing background play for videos labeled for kids.
    KidsBackgroundPlaybackPolicyControllerFingerprint.hookMethod(returnConstant(Unit))

    // Fix PiP buttons not working after locking/unlocking device screen.
    if (!is_21_21_or_greater) {
        ::featureFlagCheck.hookMethod {
            before { if (it.args[0] == PIP_INPUT_CONSUMER_FEATURE_FLAG) it.result = false }
        }
    }

    if (is_20_29_or_greater) {
        // Client flag that interferes with background playback of some video types.
        // Exact purpose is not clear and it's used in ~ 100 locations.
        ::featureFlagCheck.hookMethod {
            before {
                if (it.args[0] == 45698813L)
                    it.result = false
            }
        }
    }

    if (is_21_04_or_greater) {
        // If NewPlayerTypeEnumFeatureFlagFingerprint is present and forced off then this flag
        // must also be disabled, otherwise the player is a black screen with no buttons and no playback.
        ::featureFlagCheck.hookMethod {
            before {
                if (it.args[0] == 45752335L)
                    it.result = false
            }
        }
    }
}