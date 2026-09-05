package io.github.nexalloy.morphe.youtube.misc.playertype

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.opcodes
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.FieldUsingType

val playerTypeFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    methodMatcher {
        addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
    }
    classMatcher {
        className(".YouTubePlayerOverlaysLayout", StringMatchType.EndsWith)
    }
}

val reelWatchPlayerId get() = resourceMappings["id", "reel_watch_player"]
val reelWatchPagerFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Landroid/view/View;")
    literal { reelWatchPlayerId }
}

val ReelPlayerViewField = findFieldDirect {
    reelWatchPagerFingerprint().declaredClass!!.fields.single { it.typeName.endsWith("ReelPlayerView") }
}

val ControlsState = findClassDirect {
    findClass {
        matcher {
            usingStrings("controls can be in the buffering state only if in PLAYING or PAUSED video state")
        }
    }.single()
}

val videoStateFingerprint = findMethodDirect {
    // TODO this is terrible
    val controlsStateClass = ControlsState(this).descriptor
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("V")
            parameters(controlsStateClass)
            opcodes(
                Opcode.CONST_4,
                Opcode.IF_EQZ,
                Opcode.IF_EQZ,
                Opcode.IGET_OBJECT, // obfuscated parameter field name
            )
        }
    }.first()
}

val videoStateParameterField = findFieldDirect {
    videoStateFingerprint().let { method ->
        method.usingFields.distinct().single { field ->
            // obfuscated parameter field name
            field.usingType == FieldUsingType.Read && field.field.declaredClass == method.paramTypes[0]
        }.field
    }
}