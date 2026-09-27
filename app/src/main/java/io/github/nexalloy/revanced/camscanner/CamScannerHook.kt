package io.github.nexalloy.revanced.camscanner

import io.github.nexalloy.revanced.camscanner.telemetry.DisableTelemetry
import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.shared.privacy.DisableCommonAnalytics
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val CamScannerPatches = arrayOf(
    AllowScreenCapture,
    DisableTelemetry,
    DisableCommonAnalytics,
    BlockCommonDisplayAds,
)
