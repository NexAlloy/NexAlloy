package io.github.nexalloy.revanced.truecaller

import io.github.nexalloy.revanced.shared.privacy.BlockCommonDisplayAds
import io.github.nexalloy.revanced.truecaller.ads.HideAds
import io.github.nexalloy.revanced.truecaller.analytics.DisableAnalytics
import io.github.nexalloy.revanced.truecaller.analytics.DisableThirdPartySdks
import io.github.nexalloy.revanced.truecaller.misc.DisableAppStartTelemetry
import io.github.nexalloy.revanced.truecaller.misc.DisableUpdateCheck
import io.github.nexalloy.revanced.truecaller.premium.HideUpgradePrompts
import io.github.nexalloy.revanced.truecaller.premium.RemovePremiumUi
import io.github.nexalloy.revanced.truecaller.ui.HideAssistantTab
import io.github.nexalloy.revanced.truecaller.ui.HideFamilyProtectionButton
import io.github.nexalloy.revanced.truecaller.ui.HidePremiumSettingsBlock
import io.github.nexalloy.revanced.truecaller.ui.HideScamsTab
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val TruecallerPatches = arrayOf(
    AllowScreenCapture,
    HideAds,
    DisableAnalytics,
    DisableThirdPartySdks,
    DisableAppStartTelemetry,
    HideUpgradePrompts,
    RemovePremiumUi,
    HidePremiumSettingsBlock,
    HideAssistantTab,
    HideFamilyProtectionButton,
    HideScamsTab,
    DisableUpdateCheck,
    BlockCommonDisplayAds,
)
