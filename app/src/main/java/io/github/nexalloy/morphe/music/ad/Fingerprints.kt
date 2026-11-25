package io.github.nexalloy.morphe.music.ad

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.music.shared.YOUTUBE_MUSIC_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral

internal object ShowVideoAdsFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
    ),
    strings = listOf("maybeRegenerateCpnAndStatsClient called unexpectedly, but no error.")
)

/**
 * Lazily builds the in-player ad overlay (ad title, ad progress text, skip ad button).
 * Matches the findViewById call for skip_ad_button, whose receiver is the overlay root.
 *
 * Older app versions build the overlay in a separate method that returns the root view,
 * newer versions inline it into a method with no parameters. The method signature is not
 * declared so both match.
 */
internal object AdOverlayInflateFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "default_ad_overlay"),
        methodCall(name = "inflate"),
        resourceLiteral(ResourceType.ID, "skip_ad_button"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            returnType = "Landroid/view/View;"
        )
    )
)

internal val adOverlayViewLookupMethod = findMethodDirect {
    AdOverlayInflateFingerprint.instructionMatches.last().instruction.methodRef!!
}

val showVideoAds = findMethodDirect {
    ShowVideoAdsFingerprint.instructionMatches[1].instruction.methodRef!!
}

internal object HideGetPremiumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("FEmusic_history", "FEmusic_offline"),
    // NexAlloy: scope the visibility override to the upstream panel's receiver.
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/view/View;"
        ),
        methodCall(
            smali = "Landroid/view/View;->setVisibility(I)V",
            location = MatchAfterWithin(5)
        )
    )
)

internal val getPremiumViewField = findFieldDirect {
    HideGetPremiumFingerprint.instructionMatches[0].instruction.fieldRef!!
}

internal val getPremiumSetVisibilityMethod = findMethodDirect {
    HideGetPremiumFingerprint.instructionMatches[1].instruction.methodRef!!
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
    definingClass = YOUTUBE_MUSIC_MAIN_ACTIVITY_CLASS_TYPE,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "floating_layout"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            returnType = "Landroid/view/View;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
    )
)
