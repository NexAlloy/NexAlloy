package io.github.nexalloy.morphe.youtube.misc.recyclerviewtree

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import org.luckypray.dexkit.result.ClassData

val recyclerViewTreeObserverFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
    )
    strings("LithoRVSLCBinder")
}

private val ClassData.isObject
    get() = this.descriptor.startsWith("L")

val RecyclerView_addOnScrollListener = findMethodDirect {
    recyclerViewTreeObserverFingerprint().let { method ->
        val recyclerView = method.paramTypes[1]
        method.invokes.single {
            it.declaredClass == recyclerView && it.paramTypes.singleOrNull { clz -> clz.isObject } != null
        }
    }
}