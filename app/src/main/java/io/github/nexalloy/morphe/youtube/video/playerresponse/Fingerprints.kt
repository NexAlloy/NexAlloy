package io.github.nexalloy.morphe.youtube.video.playerresponse

import io.github.nexalloy.BuildConfig
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect

// no longer works since 20.46.33
@get:SkipTest
val oldPlayerParameterBuilderFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("psns", "psnr", "psps", "pspe")
        }
    }.single {
        it.paramTypeNames.contains("java.lang.String")
    }
}

val playerParameterBuilderClass = findClassDirect {
    findMethod {
        matcher {
            usingEqStrings(
                "ps_s",
                "ps_r",
                "PLAYER_REQUEST_WAS_AUTOPLAY",
                "PLAYER_REQUEST_WAS_AUTONAV",
                "PLAYER_REQUEST_CLICK_TRACKING",
                "",
                "PLAYER_RESPONSE_SOURCE_KEY"
            )
        }
    }.single().declaredClass!! //
        .methods.first { it.isConstructor && it.paramCount >= 3 } //
        .paramTypes[2]
}

val playerParameterBuilderFingerprint = findMethodDirect {
    playerParameterBuilderClass().findMethod {
        matcher {
            // java.lang.String,
            // byte[],
            // java.lang.String,
            // java.lang.String,
            // int,
            // int / [boolean, int, XXX]
            // java.util.Set,
            // java.lang.String,
            // java.lang.String,
            // XXX
            // boolean, // (IsShortAndOpeningOrPlaying)
            // boolean,
            // boolean,
            // boolean / XXX.time.Duration
            paramCount(min = 11)
        }
    }.single()
        // Unit Test
        .also {
            if (BuildConfig.DEBUG) {
                val old = runCatching { oldPlayerParameterBuilderFingerprint() }.getOrNull()
                if (old != null && it != old) throw Exception("Old: $old\nNew: $it")
            }
        }
}