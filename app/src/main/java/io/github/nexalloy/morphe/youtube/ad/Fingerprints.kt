package io.github.nexalloy.morphe.youtube.ad

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string

private val ADD_METHOD_CALL = methodCall(
    opcode = Opcode.INVOKE_VIRTUAL,
    name = "add",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Z",
)

internal object FullScreenEngagementAdContainerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "fullscreen_engagement_ad_container"),
        opcode(Opcode.IGET_BOOLEAN),
        ADD_METHOD_CALL,
        ADD_METHOD_CALL,
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "size",
            parameters = listOf(),
            returnType = "I"
        )
    )
)

internal object GetPremiumViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/red/presenter/CompactYpcOfferModuleView;",
    name = "onMeasure",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "I"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    )
)

internal object PlayerOverlayTimelyShelfFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;", location = MatchAfterImmediately()),
        string("player_overlay_timely_shelf", location = MatchAfterImmediately()),
        methodCall(smali = "Ljava/lang/String;->equals(Ljava/lang/Object;)Z", location = MatchAfterWithin(5)),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)

val PlayerOverlayEventType = findClassDirect {
    PlayerOverlayTimelyShelfFingerprint.instructionMatches[0].instruction.classRef!!
}

val PlayerOverlayIdField = findFieldDirect {
    PlayerOverlayTimelyShelfFingerprint.instructionMatches[1].instruction.fieldRef!!
}

internal object LoadVideoAdsFingerprint : Fingerprint(
    strings = listOf(
        "TriggerBundle doesn't have the required metadata specified by the trigger ",
        "Ping migration no associated ping bindings for activated trigger: ",
    )
)

internal object PlayerBytesAdLayoutFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf(
        "Bootstrapped layout construction resulted in non PlayerBytesLayout. PlayerAds count: ",
    )
)
