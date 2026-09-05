package io.github.nexalloy.morphe.youtube.video.videoid

import io.github.nexalloy.morphe.findMethodDirect

val videoIdFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addEqString("Null initialPlayabilityStatus")
        }
    }.single()
}

val PlayerResponseModel_getVideoId = findMethodDirect {
    videoIdFingerprint().let { method ->
        method.invokes.distinct().single {
            it.returnTypeName == "java.lang.String" && it.declaredClass == method.paramTypes[0] // PlayerResponseModel, interface
        }
    }
}

//val videoIdBackgroundPlayFingerprint = fingerprint {
//    accessFlags(AccessFlags.DECLARED_SYNCHRONIZED, AccessFlags.FINAL, AccessFlags.PUBLIC)
//    returns("V")
//    parameters("L")
//    opcodes(
//        Opcode.IF_EQZ,
//        Opcode.INVOKE_INTERFACE,
//        Opcode.MOVE_RESULT_OBJECT,
//        Opcode.IPUT_OBJECT,
//        Opcode.MONITOR_EXIT,
//        Opcode.RETURN_VOID,
//        Opcode.MONITOR_EXIT,
//        Opcode.RETURN_VOID
//    )
//    // The target snippet of code is buried in a huge switch block and the target method
//    // has been changed many times by YT which makes identifying it more difficult than usual.
//    custom { method, classDef ->
//        // Access flags changed in 19.36
//        AccessFlags.FINAL.isSet(method.accessFlags) &&
//                AccessFlags.DECLARED_SYNCHRONIZED.isSet(method.accessFlags) &&
//                classDef.methods.count() == 17 &&
//                method.implementation != null &&
//                method.indexOfPlayerResponseModelString() >= 0
//    }
//
//}
//
//val videoIdParentFingerprint = fingerprint {
//    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
//    returns("[L")
//    parameters("L")
//    literal { 524288L }
//}
