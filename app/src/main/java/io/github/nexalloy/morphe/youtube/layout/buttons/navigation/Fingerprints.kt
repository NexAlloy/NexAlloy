package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.layout.hide.shorts.ShortsBottomBarContainerFingerprint
import java.lang.reflect.Modifier

internal const val PIVOT_BAR_CLASS = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"

/**
 * Sets the bottom padding of the navigation bar to the bottom window inset, the room for
 * the system navigation buttons when the bar is drawn behind them. The bar height also grows
 * by the same value, which is first stored in a field of the bar and then added to its height.
 */
@SkipTest
internal object NavigationBarBottomPaddingFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT,
            definingClass = PIVOT_BAR_CLASS,
            type = "I"
        ),
        // Sets the bar height to its content height and the room in the field.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = PIVOT_BAR_CLASS,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            definingClass = PIVOT_BAR_CLASS,
            name = "setPadding",
            location = MatchAfterWithin(10)
        )
    )
)

internal val navigationBarBottomPaddingMethods = findMethodListDirect {
    NavigationBarBottomPaddingFingerprint.matchAll().map { it.method }
}

internal val navigationBarBottomPaddingField = findFieldDirect {
    NavigationBarBottomPaddingFingerprint.matchAll().map {
        it.instructionMatches.first().instruction.fieldRef!!
    }.distinctBy { it.descriptor }.single()
}

internal val navigationBarRelayoutMethods = findMethodListDirect {
    val paddingField = navigationBarBottomPaddingField()
    NavigationBarBottomPaddingFingerprint.matchAll().map { match ->
        // The stored padding is consumed by the immediately following height update.
        val instruction = match.instructionMatches[1].instruction
        check(instruction.opcode == Opcode.INVOKE_VIRTUAL.opCode ||
                instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE.opCode)
        val reference = instruction.methodRef!!
        check(reference.declaredClass!!.descriptor == paddingField.declaredClass!!.descriptor &&
                reference.returnType!!.descriptor == "V" && reference.paramTypes.isEmpty())
        // A cross-dex method reference may have no body; resolve its definition before checking it.
        Fingerprint(
            definingClass = reference.declaredClass!!.descriptor,
            name = reference.name,
            returnType = "V",
            parameters = emptyList()
        ).match().method.also { method ->
            check(method.instructions.any {
                it.opcode == Opcode.IGET.opCode &&
                        it.fieldRef?.descriptor == paddingField.descriptor
            })
        }
    }.distinctBy { it.descriptor }
}

internal object CreatePivotBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;",
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    ),
)

internal object AutoHideNavigationBarOnFeedScrollingFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/support/v7/widget/RecyclerView;", "I", "I"),
    filters = listOf(
        methodCall("Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;"),
        methodCall(
            smali = "Landroid/view/ViewConfiguration;->getScaledTouchSlop()I",
            location = MatchAfterWithin(5)
        )
    )
)

internal object AutoHideNavigationBarOnDismissMiniplayerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("F"),
    filters = listOf(
        literal(2),
        methodCall(
            smali = "Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Set;->iterator()Ljava/util/Iterator;",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Iterator;->hasNext()Z",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Iterator;->next()Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        )
    )
)

internal object NavigationBarBlurDrawFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("frostedGlassRenderNode")
        )
    ),
    name = "draw",
    returnType = "V",
    parameters = listOf("Landroid/graphics/Canvas;"),
    filters = listOf(
        methodCall(smali = "Landroid/graphics/Canvas;->isHardwareAccelerated()Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(smali = "Landroid/view/View;->draw(Landroid/graphics/Canvas;)V")
    )
)

internal val shortsBottomRoomConstructor = findMethodDirect {
    val match = ShortsBottomBarContainerFingerprint.match()
    match.method.instructions.drop(match.instructionMatches[2].index + 1).first {
        it.opcode in listOf(Opcode.INVOKE_DIRECT.opCode, Opcode.INVOKE_DIRECT_RANGE.opCode) &&
                it.methodRef?.name == "<init>"
    }.methodRef!!.also { constructor ->
        check(constructor.paramTypes.zipWithNext().any { (view, height) ->
            view.descriptor == "Landroid/view/View;" && height.descriptor == "I"
        })
    }
}

internal object PivotBarRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    returnType = "Lj$/util/Optional;",
    filters = listOf(
        literal(117501096L),
        opcode(Opcode.IF_NE),
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            definingClass = "this",
            name = "<init>",
            returnType = "V"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

@get:RequireAppVersion(minVersion = "21.35.000")
internal val pivotBarItemRendererClass = findClassDirect {
    PivotBarRendererFingerprint.instructionMatches[2].instruction.classRef!!
}

internal val pivotBarItemDefaultInstance = findFieldDirect {
    val type = PivotBarRendererFingerprint.match().method.paramTypes.single()
    type.fields.single {
        Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) && it.type.descriptor == type.descriptor
    }
}

@get:RequireAppVersion(minVersion = "21.35.000")
internal val pivotBarItemMessageField = findFieldDirect {
    PivotBarRendererFingerprint.match().method.declaredClass!!.fields.single {
        it.type!!.descriptor == "Lcom/google/protobuf/MessageLite;"
    }
}

@RequireAppVersion(minVersion = "21.35.000")
@StrictQuery
internal object PivotBarRendererListFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "L"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "L"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "L"),
        // The new list is compared to the previous list.
        methodCall(smali = "Ljava/lang/Class;->isInstance(Ljava/lang/Object;)Z")
    )
)

@get:RequireAppVersion(minVersion = "21.35.000")
internal val pivotBarRendererListMethod = findMethodDirect {
    val matches = PivotBarRendererListFingerprint.instructionMatches
    matches[1].instruction.methodRef!!.also {
        check(it.returnType!!.descriptor == matches[2].instruction.fieldRef!!.type!!.descriptor)
    }
}

@get:RequireAppVersion(minVersion = "21.35.000")
internal val pivotBarRendererListBuilderMethod = findMethodDirect {
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
        returnType = PivotBarRendererListFingerprint.instructionMatches[2].instruction.fieldRef!!.type!!.descriptor,
        parameters = listOf("Ljava/util/Collection;")
    ).match().method
}

/**
 * Tabs of the browse response (singleColumnBrowseResultsRenderer).
 * The tab protos are converted to tab items, that are cached in a field of the class.
 */
@RequireAppVersion(minVersion = "21.35.000")
internal object BrowseResponseTabsFingerprint : Fingerprint(
    interfaces = listOf("Landroid/os/Parcelable;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        literal(58173949L),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            name = "stream",
            parameters = listOf("Ljava/util/Collection;")
        ),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this")
    )
)

@get:RequireAppVersion(minVersion = "21.35.000")
internal val browseResponseTabsStreamMethod = findMethodDirect {
    BrowseResponseTabsFingerprint.instructionMatches[1].instruction.methodRef!!
}

/**
 * Creates a navigation bar item proto with a browse endpoint.
 * Parameters: browse id, label, icon type, icon only.
 */
@RequireAppVersion(minVersion = "21.35.000")
internal object PivotBarItemProtoFactoryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "L", "Z"),
    filters = listOf(literal(117501096L))
)

@get:RequireAppVersion(minVersion = "21.35.000")
internal val pivotBarIconTypeLookupMethod = findMethodDirect {
    val iconType = PivotBarItemProtoFactoryFingerprint.match().method.paramTypes[2].descriptor
    Fingerprint(
        definingClass = iconType,
        isStatic = true,
        returnType = iconType,
        parameters = listOf("I")
    ).match().method
}
