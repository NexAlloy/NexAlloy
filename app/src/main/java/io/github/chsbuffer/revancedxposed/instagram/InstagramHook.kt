package io.github.chsbuffer.revancedxposed.instagram

import io.github.chsbuffer.revancedxposed.instgram.ads.HideAds
import io.github.chsbuffer.revancedxposed.instagram.network.BlockNetwork
import io.github.chsbuffer.revancedxposed.instagram.tracking.SanitizeTrackingLinks

val MetaPatches = arrayOf(HideAds, SanitizeTrackingLinks, BlockNetwork)
