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

internal fun matchesPlayerParameterBuilderSignature(parameters: List<String>): Boolean {
    if (parameters.size !in 15..16) return false

    fun objectType(index: Int) =
        parameters[index].startsWith('L') && parameters[index].endsWith(';')

    if (parameters[0] != "Ljava/lang/String;" ||
        parameters[1] != "[B" ||
        parameters[2] != "Ljava/lang/String;" ||
        parameters[3] != "Ljava/lang/String;" ||
        parameters[4] != "I" ||
        parameters[5] != "Z" ||
        parameters[6] != "I" ||
        !objectType(7) ||
        parameters[8] != "Ljava/util/Set;" ||
        parameters[9] != "Ljava/lang/String;" ||
        parameters[10] != "Ljava/lang/String;" ||
        !objectType(11) ||
        parameters[12] != "Z" ||
        parameters[13] != "Z" ||
        parameters[14] != "Z"
    ) {
        return false
    }

    return parameters.size == 15 || parameters[15] == "Lj$/time/Duration;"
}

val playerParameterBuilderFingerprint = findMethodDirect {
    playerParameterBuilderClass().findMethod {
        matcher {
            paramCount(min = 15, max = 16)
        }
    }.single { method ->
        matchesPlayerParameterBuilderSignature(method.paramTypes.map { it.descriptor })
    }
        // Unit Test
        .also {
            if (BuildConfig.DEBUG) {
                val old = runCatching { oldPlayerParameterBuilderFingerprint() }.getOrNull()
                if (old != null && it != old) throw Exception("Old: $old\nNew: $it")
            }
        }
}