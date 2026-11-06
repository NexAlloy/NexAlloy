package io.github.nexalloy.morphe.shared.misc.debugging

import android.annotation.SuppressLint
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.SkipTest
import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.parametersMatch
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.strings

internal val experimentFlagUtilFingerprint = findClassDirect {
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC)
            returns("L")
            strings("Unable to parse proto typed experiment flag: ")
            paramCount(min = 2)
        }
    }.filter {
        it.parameters.let { params ->
            parametersMatch(
                params, listOf("L", "J", "[B")
            ) || parametersMatch(
                params, listOf("L", "J")
            ) || parametersMatch( // 21.35+
                params, listOf("J", "[B")
            ) || parametersMatch( // 21.42+
                params, listOf("I", "J", "[B")
            )
        }
    }.map { it.declaredClass }.distinct().single()!!
}

@SuppressLint("NonUniqueDexKitData")
internal val experimentalBooleanFeatureFlagFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns("Z")
            paramCount(min = 2)
        }
    }.first {
        it.parameters.let { params ->
            parametersMatch(
                params, listOf("L", "J", "Z")
            ) || parametersMatch( // 21.35+
                params,
                listOf("J", "Z")
            )
        }
    }
}

/**
 * 21.42+ boolean accessor with an additional int parameter.
 * The [experimentalBooleanFeatureFlagFingerprint] accessor still exists and is also used.
 */
@get:TargetApp("youtube")
@get:RequireAppVersion("21.42.000")
internal val experimentalIndexedBooleanFeatureFlagFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns("Z")
            parameters("I", "J", "Z")
        }
    }.single()
}

@SuppressLint("NonUniqueDexKitData")
internal val experimentalDoubleFeatureFlagFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns("D")
            paramCount(min = 2)
        }
    }.first {
        it.parameters.let { params ->
            parametersMatch(
                params, listOf("L", "J", "D")
            ) || parametersMatch( // 21.35+
                params,
                listOf("J", "D")
            ) || parametersMatch( // 21.42+
                params, listOf("I", "J", "D")
            )
        }
    }
}

@SuppressLint("NonUniqueDexKitData")
internal val experimentalLongFeatureFlagFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns("J")
            paramCount(min = 2)
        }
    }.first {
        it.parameters.let { params ->
            parametersMatch(
                params, listOf("L", "J", "J")
            ) || parametersMatch( // 21.35+
                params,
                listOf("J", "J")
            ) || parametersMatch( // 21.42+
                params, listOf("I", "J", "J")
            )
        }
    }
}

@get:TargetApp("youtube")
@SuppressLint("NonUniqueDexKitData")
internal val experimentalStringFeatureFlagFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns("Ljava/lang/String;")
            paramCount(min = 2)
        }
    }.first {
        it.parameters.let { params ->
            parametersMatch(
                params, listOf("L", "J", "Ljava/lang/String;")
            ) || parametersMatch( // 21.35+
                params,
                listOf("J", "Ljava/lang/String;")
            ) || parametersMatch( // 21.42+
                params, listOf("I", "J", "Ljava/lang/String;")
            )
        }
    }
}

// Covered by the version-gated per-app finders.
@SkipTest
internal fun hotFeatureFlag(returnType: String) = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(experimentFlagUtilFingerprint().name)
            accessFlags(AccessFlags.PUBLIC)
            returns(returnType)
        }
    }.single {
        parametersMatch(it.parameters, listOf("J")) ||
            parametersMatch(it.parameters, listOf("I", "J"))
    }
}

internal object BootstrapStoreSerializeFingerprint : Fingerprint(
    filters = listOf(
        string("!serialize")
    )
)

internal object BootstrapStoreReadFingerprint : Fingerprint(
    classFingerprint = BootstrapStoreSerializeFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "J",
    parameters = listOf("I")
)
