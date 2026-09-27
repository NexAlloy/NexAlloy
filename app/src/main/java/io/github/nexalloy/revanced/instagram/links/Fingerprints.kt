package io.github.nexalloy.revanced.instagram.links

import io.github.nexalloy.morphe.fingerprint

val permalinkResponseJsonParserFingerprint = fingerprint {
    strings("XDTPermalinkResponse")
}

val profileUrlResponseJsonParserFingerprint = fingerprint {
    strings("profile_to_share_url")
}

val storyUrlResponseImplFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    classMatcher {
        descriptor = "Lcom/instagram/request/StoryItemUrlResponseImpl;"
    }
}

val liveUrlResponseImplFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    classMatcher {
        descriptor = "Lcom/instagram/request/LiveItemLinkUrlResponseImpl;"
    }
}
