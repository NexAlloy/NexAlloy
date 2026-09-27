package io.github.nexalloy.revanced.tiktok.interaction.quickactions

import io.github.nexalloy.morphe.fingerprint

val quickCommentReactionGateFingerprint = fingerprint {
    returns("Z")
    parameters("I")
    methodMatcher { name = "LIZ" }
    classMatcher { descriptor = "LX/0BIZ;" }
}

val longPressQuickShareGateFingerprint = fingerprint {
    returns("I")
    parameters()
    methodMatcher { name = "LIZ" }
    classMatcher { descriptor = "LX/0BJV;" }
}

val longPressRepostGateFingerprint = fingerprint {
    returns("Z")
    parameters("Landroid/view/View;")
    strings(
        "Long press detected on digg button for aweme: ",
        "long_press_like_panel",
    )
    classMatcher {
        descriptor = "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;"
    }
}
