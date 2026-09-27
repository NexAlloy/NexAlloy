package io.github.nexalloy.revanced.protonvpn

import io.github.nexalloy.revanced.protonvpn.delay.RemoveServerChangeDelay
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val ProtonVpnPatches = arrayOf(
    AllowScreenCapture,
    RemoveServerChangeDelay,
    DisableCommonAnalytics,
)
