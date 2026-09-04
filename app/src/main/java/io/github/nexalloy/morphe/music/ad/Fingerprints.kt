package io.github.nexalloy.morphe.music.ad

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import org.luckypray.dexkit.query.enums.StringMatchType

internal object ShowVideoAdsFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
    ),
    strings = listOf("maybeRegenerateCpnAndStatsClient called unexpectedly, but no error.")
)

val showVideoAds = findMethodDirect {
    ShowVideoAdsFingerprint.instructionMatches[1].instruction.methodRef!!
}

internal object HideGetPremiumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("FEmusic_history", "FEmusic_offline")
)

val hideGetPremiumSetVisibility = findMethodDirect {
    HideGetPremiumFingerprint().invokes.single {
        it.className == "android.view.View" &&
            it.name == "setVisibility" &&
            it.paramTypeNames == listOf("int") &&
            it.returnTypeName == "void"
    }
}

internal object MembershipSettingsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/CharSequence;",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    )
)

internal object FloatingLayoutFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "floating_layout"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            parameters = listOf("I"),
            returnType = "Landroid/view/View;"
        ),
        opcode(
            Opcode.MOVE_RESULT_OBJECT,
            InstructionLocation.MatchAfterImmediately()
        )
    )
) {
    init {
        classMatcher {
            className(".MusicActivity", StringMatchType.EndsWith)
        }
    }
}

val floatingLayoutFindViewById = findMethodDirect {
    FloatingLayoutFingerprint.instructionMatches[1].instruction.methodRef!!
}
