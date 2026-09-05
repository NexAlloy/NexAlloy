package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.strings

internal const val ANDROID_AUTOMOTIVE_STRING = "Android Automotive"

val addCreateButtonViewFingerprint = fingerprint {
    strings("Android Wear", ANDROID_AUTOMOTIVE_STRING)
}

// rvxp
val AutoMotiveFeatureMethod = findMethodDirect {
    addCreateButtonViewFingerprint().invokes.findMethod {
        matcher { strings("android.hardware.type.automotive") }
    }.single()
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
