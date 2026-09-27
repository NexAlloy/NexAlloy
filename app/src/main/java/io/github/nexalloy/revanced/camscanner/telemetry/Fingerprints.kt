package io.github.nexalloy.revanced.camscanner.telemetry

import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.fingerprint

val isSkipLoggingFingerprint = fingerprint {
    returns("Z")
    parameters()
    classMatcher { descriptor = "Lcom/intsig/log/LogAgentHelper;" }
}

val logAgentRecordFingerprints = findMethodListDirect {
    findClass {
        matcher { descriptor = "Lcom/intsig/logagent/LogAgent;" }
    }.single().findMethod {
        matcher { name = "record" }
    }.toList()
}
