package io.github.nexalloy.revanced.messenger.ads

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.fingerprint

val loadInboxAdsFingerprint = fingerprint {
    returns("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    strings("ads_load_begin", "inbox_ads_fetch_start")
    classMatcher {
        descriptor =
            "Lcom/facebook/messaging/business/inboxads/plugins/inboxads/itemsupplier/InboxAdsItemSupplierImplementation;"
    }
}
