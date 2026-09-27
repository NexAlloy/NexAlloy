package io.github.nexalloy.revanced.truecaller.ui

import io.github.nexalloy.morphe.fingerprint

val assistantFeatureFingerprint = fingerprint {
    returns("Z")
    parameters()
    strings("featureCallAssistant")
}

val familyProtectFeatureFingerprint = fingerprint {
    returns("Z")
    parameters()
    strings("featureFamilyProtect")
}

val scamFeedEnabledFingerprint = fingerprint {
    returns("Z")
    parameters()
    methodMatcher { name = "a" }
    classMatcher { descriptor = "Lhc2/c;" }
}

val premiumBlockComposeFingerprint = fingerprint {
    returns("V")
    parameters(
        "Lcom/truecaller/usershome/presentaion/ui/components/featureditemsection/baz;",
        "Lkotlin/jvm/functions/Function0;",
        "Landroidx/compose/ui/b;",
        "Ll2/k;",
        "I",
    )
    strings("premium_block")
}
