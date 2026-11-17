package io.github.nexalloy.morphe.youtube.misc.verticalscroll

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode

internal object CanScrollVerticallyFingerprint : Fingerprint(
    definingClass = "Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(smali = "Landroid/view/View;->canScrollVertically(I)Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)

val canScrollVerticallyMethod = findMethodDirect {
    CanScrollVerticallyFingerprint.instructionMatches.first().instruction.methodRef!!
}

@RequireAppVersion(minVersion = "21.33.000")
internal object ScrollToRelatedItemsOnVideoEndFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        literal(45825801L),
        literal(45825802L),
        string("related-items")
    )
)

@StrictQuery
internal object FullscreenMoreVideosButtonFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        // The state of the panel.
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "Z",
            parameters = listOf(),
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        // Opens the panel.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "V",
            parameters = listOf("I", "I"),
            location = MatchAfterWithin(4)
        ),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately())
    )
)

val fullscreenMoreVideosPanelStateMethod = findMethodDirect {
    FullscreenMoreVideosButtonFingerprint.instructionMatches[1].instruction.methodRef!!
}
