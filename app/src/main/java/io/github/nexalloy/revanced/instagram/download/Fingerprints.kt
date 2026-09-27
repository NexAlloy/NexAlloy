package io.github.nexalloy.revanced.instagram.download

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint

private const val MEDIA_OPTION_DESCRIPTOR =
    "Lcom/instagram/feed/media/mediaoption/MediaOption\$Option;"

val nativeDownloadEligibleFingerprint = fingerprint {
    returns("Z")
    parameters(
        "Lcom/instagram/common/session/UserSession;",
        "Lcom/instagram/feed/media/Media;",
    )
    literal { 36313978552585585L }
}

val nativeDownloadRestrictedFingerprint = fingerprint {
    returns("Z")
    parameters(
        "Lcom/instagram/common/session/UserSession;",
        "Z",
    )
    literal { 36313978552847731L }
}

val reducedOptionsListFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "java.util.ArrayList"
            addUsingField(
                "$MEDIA_OPTION_DESCRIPTOR->PLAYBACK_CONTROLS:$MEDIA_OPTION_DESCRIPTOR"
            )
            addUsingField(
                "$MEDIA_OPTION_DESCRIPTOR->UNSAVE:$MEDIA_OPTION_DESCRIPTOR"
            )
        }
    }.first()
}
