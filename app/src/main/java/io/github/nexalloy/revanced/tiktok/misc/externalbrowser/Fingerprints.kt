package io.github.nexalloy.revanced.tiktok.misc.externalbrowser

import io.github.nexalloy.morphe.fingerprint

val sparkThirdRouterOpenFingerprint = fingerprint {
    returns("V")
    parameters(
        "Landroid/content/Context;",
        "Lcom/bytedance/hybrid/spark/third/router/SparkThirdContext;",
    )
    strings(
        "ContainerId",
        "Context_startActivity_1",
    )
}

val storyLinkSheetFingerprint = fingerprint {
    returns("V")
    parameters("Lcom/ss/android/ugc/aweme/sticker/data/InteractStickerStruct;")
    strings("external_website_security_pop_up_window_show")
}
