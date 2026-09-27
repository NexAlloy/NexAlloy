package io.github.nexalloy.revanced.twitch.ad.display

import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.result.ClassData
import java.lang.reflect.Modifier

val displayAdResponseParserFingerprint = fingerprint {
    parameters(
        "Lretrofit2/adapter/rxjava2/Result;",
        "Z",
    )
    strings(
        "failed to parse display ad response: ",
        "could not parse content type: ",
    )
    methodMatcher { name = "a" }
}

private fun ClassData.isSubtypeOf(typeName: String): Boolean {
    var current: ClassData? = this
    while (current != null) {
        if (current.name == typeName) return true
        current = current.superClass
    }
    return false
}

val noAdSingletonFingerprint = findFieldDirect {
    val parser = displayAdResponseParserFingerprint.invoke(this)
    val returnTypeName = parser.returnTypeName

    parser.usingFields
        .asSequence()
        .filter { Modifier.isStatic(it.modifiers) }
        .filter { it.className == it.typeName }
        .filter { runCatching { it.type.isSubtypeOf(returnTypeName) }.getOrDefault(false) }
        .distinctBy { it.descriptor }
        .toList()
        .single()
}
