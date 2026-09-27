package io.github.nexalloy.revanced.tiktok.interaction.antirecording

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

private fun antiRecordingFingerprint(marker: String) = fingerprint {
    returns("V")
    parameters("I")
    strings(marker)
    classMatcher {
        className(".ClearModePanelComponent", StringMatchType.EndsWith)
    }
}

val antiRecordingAddedFingerprint = antiRecordingFingerprint("[onDisplayAdded]")
val antiRecordingRemovedFingerprint = antiRecordingFingerprint("[onDisplayRemoved]")
