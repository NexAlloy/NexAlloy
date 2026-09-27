package io.github.nexalloy.revanced.tiktok.misc.share

import io.github.nexalloy.morphe.fingerprint

val shareUrlTrackerFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    strings(
        "utm_campaign",
        "share_link_id",
    )
}
