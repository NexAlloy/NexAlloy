package io.github.nexalloy.revanced.twitter.timeline

import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val timelineEntryParseFingerprint = fingerprint {
    returns("Ljava/lang/Object;")
    methodMatcher { name = "parse" }
    classMatcher {
        className(".JsonTimelineEntry\$\$JsonObjectMapper", StringMatchType.EndsWith)
    }
}

val timelineModuleItemParseFingerprint = fingerprint {
    returns("Ljava/lang/Object;")
    methodMatcher { name = "parse" }
    classMatcher {
        className(".JsonTimelineModuleItem\$\$JsonObjectMapper", StringMatchType.EndsWith)
    }
}
