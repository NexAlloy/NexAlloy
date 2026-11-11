package io.github.nexalloy.morphe.shared.misc.litho.filter

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldFromToString
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContextToStringFingerprint
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.DexKitBridge

internal object AccessibilityIdFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "Ljava/lang/String;"
        ),
        string("primary_image", location = MatchAfterWithin(5)),
    )
)

@get:AllowAbstract
val AccessibilityIdMethod = findMethodDirect {
    AccessibilityIdFingerprint.instructionMatches.first().instruction.methodRef!!
}

@get:AllowAbstract
val accessibilityTextMethod = findMethodDirect {
    Fingerprint(
        returnType = "V",
        filters = listOf(
            methodCall(
                opcode = Opcode.INVOKE_INTERFACE,
                parameters = listOf(),
                returnType = "Ljava/lang/String;"
            ),
            methodCall(
                reference = AccessibilityIdMethod(),
                location = MatchAfterWithin(5)
            )
        ),
        custom = {
        }
    ).instructionMatches.first().instruction.methodRef!!
}

val buttonViewModelReceiver = findMethodDirect {
    ComponentCreateFingerprint.instructionMatches[2].instruction.methodRef!!
}

object ComponentCreateFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterWithin(5)
        ),
        opcode(Opcode.INVOKE_STATIC, MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT),
        string("Element missing correct type extension"),
        string("Element missing type")
    )
)

internal object ProtobufBufferEncodeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[B",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
//            definingClass = "this",
            type = "Lcom/google/android/libraries/elements/adl/UpbMessage;"
        ),
        methodCall(
            definingClass = "Lcom/google/android/libraries/elements/adl/UpbMessage;",
            name = "jniEncode"
        )
    )
)

private object EmptyComponentParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.CONSTRUCTOR),
    parameters = listOf(),
    filters = listOf(
        string("EmptyComponent")
    )
)

internal object LithoThreadExecutorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("I", "I", "I"),
    filters = listOf(
        literal(1L) // 1L = default thread timeout.
    ),
    custom = {
        declaredClass {
            superClass {
                descriptor = "Ljava/util/concurrent/ThreadPoolExecutor;"
            }
        }
    }
)

val emptyComponentClass = findClassDirect {
    EmptyComponentParentFingerprint().declaredClass!!
}

/**
 * Creates the decorator of the command event data of an item of a list, such as the feed.
 * The decorator holds the data, and the item is the first parameter of the data.
 */
@TargetApp("youtube")
internal object CommandEventDataDecoratorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("Ljava/lang/Object;", "L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/Object;", "L", "L", "L", "Ljava/util/List;")
        ),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf("Ljava/util/Map;", "L"),
            location = MatchAfterWithin(2)
        )
    )
)

/**
 * Constructor of the observable list that holds the items of the lists, such as the feed.
 */
@TargetApp("youtube")
internal object ObservableListConstructorFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Ljava/util/List;", "L"),
    custom = { declaredClass { superClass { descriptor = "Ljava/util/AbstractList;" } } }
)

/**
 * Removes an item from a section of a list, such as the feed or the related videos, and lays the
 * section out again without it. The grids of the tablet layout that group the items of a section
 * into rows, such as the related videos, are laid out again too.
 */
@StrictQuery
@TargetApp("youtube")
internal object ListSectionRemoveItemFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        // The header of the section, which hides the whole section instead.
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        opcode(Opcode.IF_NE, location = MatchAfterWithin(2)),
        // The items of the section.
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/util/List;", location = MatchAfterWithin(4)),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "remove",
            parameters = listOf("Ljava/lang/Object;"),
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately())
    )
)

/**
 * Releases a section of a list, such as when the list is created again.
 * The section stops listening to the events, such as the removal of its items.
 */
@TargetApp("youtube")
internal object ListSectionReleaseFingerprint : Fingerprint(
    classFingerprint = ListSectionRemoveItemFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "V",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Updates a section of a list after its items are removed. A grid lays out its rows again,
 * otherwise the items are removed one by one.
 */
@StrictQuery
@TargetApp("youtube")
internal object ListSectionUpdateRowsFingerprint : Fingerprint(
    classFingerprint = ListSectionRemoveItemFingerprint,
    returnType = "V",
    filters = listOf(
        // If the section is a grid.
        methodCall(opcode = Opcode.INVOKE_DIRECT, definingClass = "this", returnType = "Z", parameters = listOf()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        // Lays out the rows again.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "this",
            returnType = "V",
            parameters = listOf(),
            location = MatchAfterImmediately()
        ),
        // Updates the rest of the section, such as the show more button. It is right after
        // laying out the rows on some versions, and after removing the items on others.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "this",
            returnType = "V",
            parameters = listOf()
        )
    )
)

@get:TargetApp("youtube")
internal val commandEventDataDecoratorsMethod = findMethodDirect {
    val field = ConversionContextToStringFingerprint().findFieldFromToString(", globalCommandEventDataDecoratorsInternal=")
    Fingerprint(
        definingClass = field.declaredClass!!.descriptor,
        returnType = field.typeSign,
        parameters = emptyList(),
        filters = listOf(fieldAccess(opcode = Opcode.IGET_OBJECT, reference = field))
    ).match().method
}

@get:TargetApp("youtube")
internal val listItemDataField = findFieldDirect {
    CommandEventDataDecoratorFingerprint.instructionMatches.first().instruction.methodRef!!.declaredClass!!.fields
        .single { it.typeSign == "Ljava/lang/Object;" }
}

@get:TargetApp("youtube")
internal val listItemDecoratorDataField = findFieldDirect {
    val dataType = listItemDataField().declaredClass!!
    CommandEventDataDecoratorFingerprint.instructionMatches.last().instruction.methodRef!!.declaredClass!!.fields
        .single { it.type == dataType }
}

@get:TargetApp("youtube")
internal val listSectionItemsField = findFieldDirect {
    ListSectionRemoveItemFingerprint.instructionMatches[2].instruction.fieldRef!!
}

@get:TargetApp("youtube")
internal val listSectionIsGridMethod = findMethodDirect {
    ListSectionUpdateRowsFingerprint.instructionMatches[0].instruction.methodRef!!
}

@get:TargetApp("youtube")
internal val listSectionLayOutRowsMethod = findMethodDirect {
    ListSectionUpdateRowsFingerprint.instructionMatches[3].instruction.methodRef!!
}

@get:TargetApp("youtube")
internal val listSectionUpdateMethod = findMethodDirect {
    ListSectionUpdateRowsFingerprint.instructionMatches[4].instruction.methodRef!!
}

// region Sticky headers

/**
 * Updates the sticky headers of a list, such as the related video chips of the watch page.
 */
@TargetApp("youtube")
internal object StickyHeadersFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    custom = {
        addAnyOf {
            addEqString("A LinearLayoutManager or FlowLayoutManager is required for sticky headers to work")
        }
        addAnyOf {
            addEqString("A LinearLayoutManager is required for sticky headers to work")
        }
    }
)

/**
 * Restores the alpha and the visibility of the list item hidden while its sticky copy is shown.
 */
@TargetApp("youtube")
// NexAlloy: Older versions need instruction ordering to distinguish restoring from hiding.
@StrictQuery
internal object StickyHeaderRestoreItemFingerprint : Fingerprint(
    classFingerprint = StickyHeadersFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "Landroid/view/View;"),
        // Alpha of the item before it was hidden.
        fieldAccess(opcode = Opcode.IGET, definingClass = "this", type = "F", location = MatchAfterWithin(3)),
        methodCall(definingClass = "Landroid/view/View;", name = "setAlpha", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility")
    )
)

/**
 * Creates the sticky copy of the header, at the position saved when the copy was posted.
 */
internal fun stickyHeaderCreateFingerprint(stickyHeadersType: String) = Fingerprint(
    name = "run",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/view/ViewGroup;", name = "removeView"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = stickyHeadersType,
            type = "Landroid/view/ViewGroup;",
            location = MatchAfterWithin(4)
        ),
        // View type of the position.
        methodCall(returnType = "I", parameters = listOf("I"), location = MatchAfterImmediately()),
        // Creates the view holder.
        methodCall(parameters = listOf("Landroid/view/ViewGroup;", "I"), location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = stickyHeadersType,
            type = "Landroid/view/View;",
            location = MatchAfterWithin(3)
        ),
        // Binds the position to the view holder.
        methodCall(returnType = "V", parameters = listOf("L", "I"), location = MatchAfterImmediately()),
        // Id of the current header.
        methodCall(returnType = "J", parameters = listOf(), location = MatchAfterImmediately())
    )
)

/**
 * Id of the current sticky header, read from its entry in the list of headers.
 */
internal fun stickyHeaderCurrentIdFingerprint(headerListType: String, methodName: String) = Fingerprint(
    definingClass = headerListType,
    name = methodName,
    returnType = "J",
    parameters = listOf(),
    filters = listOf(
        // Index of the current header.
        fieldAccess(opcode = Opcode.IGET, definingClass = "this", type = "I"),
        // Entries of the headers.
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "Ljava/util/List;"),
        methodCall(definingClass = "Ljava/util/List;", name = "get", location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterWithin(2)),
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "J", location = MatchAfterImmediately())
    )
)

/**
 * Sorts the entries of the sticky headers by their position.
 */
internal fun stickyHeaderEntryCompareFingerprint(entryType: String) = Fingerprint(
    definingClass = entryType,
    name = "compareTo",
    returnType = "I",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, definingClass = "this", type = "I"),
        fieldAccess(opcode = Opcode.IGET, definingClass = "this", type = "I", location = MatchAfterImmediately())
    )
)

// endregion

@get:TargetApp("youtube")
val stickyHeaderCreateMethod = findMethodDirect {
    stickyHeaderCreateFingerprint(StickyHeadersFingerprint().declaredClass!!.descriptor).match().method
}

context(_: DexKitBridge)
private fun stickyCreateMatch() =
    stickyHeaderCreateFingerprint(StickyHeadersFingerprint().declaredClass!!.descriptor).match()

context(_: DexKitBridge)
private fun stickyCurrentIdMatch() = stickyCreateMatch().instructionMatches.last().instruction.methodRef!!.let {
    stickyHeaderCurrentIdFingerprint(it.declaredClass!!.descriptor, it.name).match()
}

@get:TargetApp("youtube")
val stickyHeaderAlphaField = findFieldDirect {
    StickyHeaderRestoreItemFingerprint.instructionMatches[1].instruction.fieldRef!!
}

@get:TargetApp("youtube")
val stickyHeaderListField = findFieldDirect {
    val type = stickyCreateMatch().instructionMatches.last().instruction.methodRef!!.declaredClass!!.descriptor
    StickyHeadersFingerprint().declaredClass!!.fields.single { it.typeSign == type }
}

@get:TargetApp("youtube")
val stickyHeaderIndexField = findFieldDirect { stickyCurrentIdMatch().instructionMatches[0].instruction.fieldRef!! }

@get:TargetApp("youtube")
val stickyHeaderEntriesField = findFieldDirect { stickyCurrentIdMatch().instructionMatches[1].instruction.fieldRef!! }

@get:TargetApp("youtube")
val stickyHeaderPositionField = findFieldDirect {
    val entryType = stickyCurrentIdMatch().instructionMatches[3].instruction.classRef!!.descriptor
    stickyHeaderEntryCompareFingerprint(entryType).match().instructionMatches.first().instruction.fieldRef!!
}

@get:TargetApp("youtube")
val stickyHeaderSavedPositionField = findFieldDirect {
    val match = stickyCreateMatch()
    match.method.instructions.take(match.instructionMatches[2].index).last {
        it.opcode == Opcode.IGET.ordinal && it.fieldRef!!.typeSign == "I" &&
            it.fieldRef!!.declaredClass!!.descriptor == match.method.declaredClass!!.descriptor
    }.fieldRef!!
}

@get:TargetApp("youtube")
val stickyHeaderOwnerField = findFieldDirect {
    val method = stickyHeaderCreateMethod()
    val ownerType = StickyHeadersFingerprint().declaredClass!!.descriptor
    method.declaredClass!!.fields.singleOrNull { it.typeSign == ownerType } ?: run {
        // R8 merged lambdas store the captured owner as Object before casting it.
        val instructions = method.instructions
        val cast = instructions.indexOfFirst {
            it.opcode == Opcode.CHECK_CAST.ordinal && it.classRef!!.descriptor == ownerType
        }
        instructions[cast - 1].fieldRef!!
    }
}
