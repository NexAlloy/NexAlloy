package io.github.nexalloy.morphe.youtube.layout.branding.header

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceLiteral

internal object HeaderLogoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/widget/ImageView;"),
    filters = listOf(
        resourceLiteral(ResourceType.ATTR, "ytWordmarkHeader"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Landroid/graphics/drawable/Drawable;",
            parameters = listOf("Landroid/content/Context;", "I"),
            location = MatchAfterWithin(2)
        )
    )
)

internal val headerDrawableMethod = findMethodDirect {
    HeaderLogoFingerprint.instructionMatches[1].instruction.methodRef!!
}

// NexAlloy: identify the actual drawer controller instead of all remove/add renderers.
internal object DrawerConstructorFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "drawer_content_view"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            returnType = "Landroid/view/View;",
            parameters = listOf("I"),
            location = MatchAfterWithin(2)
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(3)
        )
    )
)

internal val drawerContentViewField = findFieldDirect {
    DrawerConstructorFingerprint.instructionMatches[2].instruction.fieldRef!!
}

internal object DrawerHeaderRenderFingerprint : Fingerprint(
    classFingerprint = DrawerConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/view/ViewGroup;->removeAllViews()V"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/view/ViewGroup;->addView(Landroid/view/View;)V"
        )
    )
)
