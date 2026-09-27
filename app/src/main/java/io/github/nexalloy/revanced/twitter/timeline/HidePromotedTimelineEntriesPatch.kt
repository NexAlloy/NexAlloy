package io.github.nexalloy.revanced.twitter.timeline

import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch

private fun shouldHideEntry(entryId: String): Boolean {
    val normalized = entryId.lowercase()
    return normalized.contains("promoted") ||
        normalized.contains("rtb")
}

val HidePromotedTimelineEntries = patch(
    name = "Hide promoted timeline entries",
    description = "Removes promoted and RTB timeline entries from X/Twitter at runtime.",
) {
    ::timelineEntryParseFingerprint.hookMethod {
        after { param ->
            val entry = param.result ?: return@after
            val entryId = entry.getObjectFieldOrNull("a") as? String ?: return@after
            if (shouldHideEntry(entryId)) {
                param.result = null
            }
        }
    }

    ::timelineModuleItemParseFingerprint.hookMethod {
        after { param ->
            val entry = param.result ?: return@after
            val entryId = entry.getObjectFieldOrNull("a") as? String ?: return@after
            if (shouldHideEntry(entryId)) {
                param.result = null
            }
        }
    }
}
