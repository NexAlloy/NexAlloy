package io.github.nexalloy.revanced.tiktok.interaction.cleardisplay

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val onClearDisplayEventFingerprint = fingerprint {
    methodMatcher { name = "onClearModeEvent" }
    classMatcher {
        className(".ClearModePanelComponent", StringMatchType.EndsWith)
    }
}

val onRenderFirstFrameBodyFingerprint = fingerprint {
    returns("V")
    parameters(
        "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
        "LX/0pb0;",
    )
    methodMatcher { name = "LLILZIL" }
    classMatcher {
        descriptor = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;"
    }
}
