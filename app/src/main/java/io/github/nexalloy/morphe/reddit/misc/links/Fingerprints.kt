package io.github.nexalloy.morphe.reddit.misc.links

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.result.MethodData

private val customReportsFingerprint = fingerprint {
    returns("V")
    strings("https://www.crisistextline.org/")
}

private fun MethodData.isScreenNavigatorCandidate(): Boolean {
    val params = paramTypeNames
    return returnTypeName == "void" &&
        "android.app.Activity" in params &&
        "android.net.Uri" in params
}

val screenNavigatorFingerprint = findMethodDirect {
    val seed = customReportsFingerprint.invoke(this)

    val direct = seed.invokes
        .filter { it.isScreenNavigatorCandidate() }

    if (direct.size == 1) {
        return@findMethodDirect direct.single()
    }

    val nested = seed.invokes
        .asSequence()
        .filter { it.returnTypeName == "void" }
        .flatMap { it.invokes.asSequence() }
        .filter { it.isScreenNavigatorCandidate() }
        .distinctBy { it.descriptor }
        .toList()

    (direct + nested)
        .distinctBy { it.descriptor }
        .minByOrNull { it.paramCount }
        ?: error("Reddit screen navigator method not found")
}
