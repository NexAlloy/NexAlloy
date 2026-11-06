package io.github.nexalloy.morphe.youtube.interaction.swipecontrols

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.interaction.seekbar.SeekbarUpdatePointFingerprint
import io.github.nexalloy.morphe.youtube.interaction.seekbar.SlideSeekbarGetViewControllerFingerprint
import io.github.nexalloy.morphe.youtube.interaction.seekbar.SlideSeekbarHandlerConstructorFingerprint

@StrictQuery
internal object PlayerOverlayContainerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = $$"Landroid/view/ViewGroup$LayoutParams;",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        literal(-1),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this"
        ),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf("I", "I", "Z")
        )
    ),
    custom = {
        addUsingField { type = "boolean" }
    }
)

val PlayerOverlayNameField = findFieldDirect {
    PlayerOverlayContainerFingerprint().declaredClass!!.fields.first { it.typeName == "java.lang.String" }
}
internal object SlideSeekbarStartScrubFingerprint : Fingerprint(
    classFingerprint = SlideSeekbarHandlerConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("Landroid/graphics/Rect;"),
            returnType = "V"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "J",
            location = MatchAfterWithin(8)
        )
    )
)

internal object TimeBarStartScrubFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("maybeStartScrub")
    )
)

internal object TimeBarMoveScrubFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("maybeMoveScrub")
    )
)

internal object TimeBarCompleteScrubFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("maybeCompleteScrub")
    )
)

val scrubberPointField = findFieldDirect {
    SeekbarUpdatePointFingerprint.instructionMatches[0].instruction.fieldRef!!
}

val scrubberBoundsField = findFieldDirect {
    SeekbarUpdatePointFingerprint.instructionMatches[2].instruction.fieldRef!!
}

@get:AllowAbstract
val scrubberTimeBarMethods = findMethodListDirect {
    val calls = SlideSeekbarStartScrubFingerprint.instructionMatches.map { it.instruction.methodRef!! }
    val timeBar = SlideSeekbarGetViewControllerFingerprint.instructionMatches[3].instruction.fieldRef!!.type!!
    require(calls[2].declaredClass!!.descriptor == timeBar.descriptor)
    require(calls[1].descriptor == SeekbarUpdatePointFingerprint().descriptor)
    listOf(calls[0], calls[2]) + listOf(
        TimeBarStartScrubFingerprint, TimeBarMoveScrubFingerprint, TimeBarCompleteScrubFingerprint
    ).map { fingerprint ->
        Fingerprint(definingClass = timeBar.descriptor, name = fingerprint().name,
            returnType = "V", parameters = listOf("I"))()
    }
}
