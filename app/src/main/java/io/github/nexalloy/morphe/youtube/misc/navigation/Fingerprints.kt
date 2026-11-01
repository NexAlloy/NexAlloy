package io.github.nexalloy.morphe.youtube.misc.navigation

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchBeforeAnywhere
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.indexOfFirstInstructionOrThrow
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string

val toolbarContainerId get() = resourceMappings["id", "toolbar_container"]

object ToolbarLayoutFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "toolbar_container")
    )
)

/**
 * Matches to https://android.googlesource.com/platform/frameworks/support/+/9eee6ba/v7/appcompat/src/android/support/v7/widget/Toolbar.java#963
 */
object AppCompatToolbarBackButtonFingerprint : Fingerprint(
    definingClass = "Landroid/support/v7/widget/Toolbar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/drawable/Drawable;",
    parameters = listOf()
)

/**
 * Matches to the class found in [PivotBarConstructorFingerprint].
 */
internal object InitializeButtonsFingerprint : Fingerprint(
    classFingerprint = PivotBarConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        string("FEvideo_picker")
    )
)

val getNavigationEnumMethod = findMethodDirect {
    InitializeButtonsFingerprint().invokes.findMethod {
        matcher {
            declaredClass(navigationEnumClass(this@findMethodDirect).name)
            accessFlags(AccessFlags.STATIC)
        }
    }.single()
}

/**
 * Matches to the Enum class that looks up ordinal -> instance.
 */
internal object NavigationEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "PIVOT_HOME",
        "TAB_SHORTS",
        "CREATION_TAB_LARGE",
        "PIVOT_SUBSCRIPTIONS",
        "TAB_ACTIVITY",
        "VIDEO_LIBRARY_WHITE",
        "INCOGNITO_CIRCLE",
    )
)

val navigationEnumClass = findClassDirect { NavigationEnumFingerprint().declaredClass!! }

/**
 * 21.42 and earlier.
 */
@get:RequireAppVersion(maxVersion = "21.43.000")
val pivotBarButtonsCreateDrawableViewFingerprint = findMethodDirect {
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("Landroid/view/View;")
            declaredClass {
                descriptor =
                    "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"
            }
        }
    }.single {
        it.paramTypes.firstOrNull()?.descriptor == "Landroid/graphics/drawable/Drawable;"
    }
}

object PivotBarButtonsCreateResourceStyledViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf("L", "Z", "I", "L")
)

internal object PivotBarButtonsViewSetSelectedFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "Z"),
    filters = listOf(
        methodCall(name = "setSelected")
    )
)

val pivotBarButtonsViewSetSelectedSubFingerprint = findMethodDirect {
    PivotBarButtonsViewSetSelectedFingerprint.instructionMatches.single().instruction.methodRef!!
}

internal object PivotBarDispatchTouchEventFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    name = "dispatchTouchEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;")
)

internal object PivotBarConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        string("com.google.android.apps.youtube.app.endpoint.flags"),
    )
)

internal object InitializeBottomBarContainerFingerprint : Fingerprint(
    name = "run",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "bottom_bar_container"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = $$"Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View$OnLayoutChangeListener;)V"
        )
    )
)

internal val bottomBarContainerListenerMethod = findMethodDirect {
    InitializeBottomBarContainerFingerprint.instructionMatches.last().instruction.methodRef!!
}

val getNavIconResIdFingerprint = findMethodListDirect {
    // two matches in versions 20.24.xx-20.26.xx,
    // one match in versions <=v20.20.xx and >=v20.28.xx
    val navigationEnumClass = navigationEnumClass()
    findMethod {
        matcher {
            paramTypes(navigationEnumClass.name, "boolean")
            returnType = "int"
            // NexAlloy: newer versions also declare this signature on an abstract provider.
            not { accessFlags(AccessFlags.ABSTRACT.modifier) }
        }
    }
}

val endpointField = findFieldDirect {
    BrowseFragmentOnCreateViewFingerprint.instructionMatches[1].instruction.fieldRef!!
}

val browserIdMethod = findMethodDirect {
    BrowseFragmentOnCreateViewFingerprint().let {
        val endpointIndex = BrowseFragmentOnCreateViewFingerprint.instructionMatches[1].index

        // Browse data is an extension of the endpoint, and reading it starts with that field.
        val browseDataExtensionIndex = it.indexOfFirstInstructionOrThrow(
            endpointIndex, Opcode.SGET_OBJECT
        )
        val browseDataExtensionField = it.instructions[browseDataExtensionIndex].fieldRef!!

        Fingerprint(
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
            returnType = "Ljava/lang/String;",
            parameters = listOf(endpointField().type.descriptor),
            filters = listOf(
                fieldAccess(opcode = Opcode.SGET_OBJECT, reference = browseDataExtensionField)
            )
        ).matchAll(1..2).first().method // Both matches are identical methods in the same class.
    }
}

val BrowseFragmentTarget = findMethodDirect {
    val definingClass = BrowseFragmentOnCreateViewFingerprint().declaredClass!!.descriptor

    Fingerprint(
        definingClass = definingClass,
        returnType = "V",
        parameters = listOf(endpointField().type.descriptor),
        filters = listOf(
            fieldAccess(opcode = Opcode.IPUT_OBJECT, reference = endpointField())
        )
    )()
}

/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2964
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

/**
 * Every browse page is shown by this one fragment, and the endpoint it is handed names the page.
 * The browse request cannot be used instead, because a page served from cache makes no request.
 */
internal object BrowseFragmentOnCreateViewFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;"
    ),
    filters = listOf(
        string("Browse Fragment was given a navigation endpoint without browse data."),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchBeforeAnywhere()
        )
    )
)

/**
 * The search feed is its own fragment, so it does not go through the browse fragment when
 * the user returns to it from a channel.
 */
internal object SearchResultsFragmentOnCreateViewFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;"
    ),
    filters = listOf(
        string("search_cache_key")
    )
)
