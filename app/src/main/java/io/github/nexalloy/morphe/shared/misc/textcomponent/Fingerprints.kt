package io.github.nexalloy.morphe.shared.misc.textcomponent

import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContextToStringFingerprint
import io.github.nexalloy.morphe.string

@TargetApp("youtube")
internal object TextComponentConstructorFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(string("TextComponent"))
)

@TargetApp("youtube")
internal object TextComponentLookupFingerprint : Fingerprint(
    classFingerprint = TextComponentConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(string("…"))
)

@TargetApp("youtube")
internal object TextComponentDataFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "L"),
    filters = listOf(string("text")),
    custom = { declaredClass { addField { type = "java.util.BitSet" } } }
)

@TargetApp("youtube")
internal object LithoSpannableStringCreationFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;", "L"),
    filters = listOf(
        newInstance(type = "Landroid/text/SpannableString;"),
        methodCall(
            smali = "Landroid/text/SpannableString;-><init>(Ljava/lang/CharSequence;)V",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Landroid/text/SpannableString;->getSpans(IILjava/lang/Class;)[Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        methodCall(name = "addOnLayoutChangeListener", parameters = listOf("Landroid/view/View\$OnLayoutChangeListener;"))
    )
)

@get:TargetApp("youtube")
val textComponentConversionContextField = findFieldDirect {
    val contextClass = ConversionContextToStringFingerprint().declaredClass!!
    TextComponentConstructorFingerprint().declaredClass!!.fields.single { it.type == contextClass }
}

@get:TargetApp("youtube")
val lithoTextConversionContextField = findFieldDirect {
    val contextClass = ConversionContextToStringFingerprint().declaredClass!!
    LithoSpannableStringCreationFingerprint().declaredClass!!.fields.single { it.type == contextClass }
}

@get:TargetApp("youtube")
val textComponentTextField = findFieldDirect {
    val match = TextComponentLookupFingerprint.match()
    val dataClass = TextComponentDataFingerprint().declaredClass!!
    val newData = match.method.instructions.indexOfFirst {
        it.opcode == Opcode.NEW_INSTANCE.ordinal && it.classRef == dataClass
    }
    check(newData >= 0)
    match.method.instructions.drop(newData).first {
        it.opcode == Opcode.IPUT_OBJECT.ordinal && it.fieldRef?.typeSign == "Ljava/lang/CharSequence;"
    }.fieldRef!!
}

@get:TargetApp("youtube")
val lithoTextHolderTextField = findFieldDirect {
    val match = LithoSpannableStringCreationFingerprint.match()
    val constructor = match.instructionMatches[1]
    var register = constructor.instruction.register(1)
    val source = match.method.instructions.take(constructor.index).asReversed().first {
        when {
            it.registerOrNull(0) != register -> false
            it.opcode in Opcode.MOVE_OBJECT.ordinal..Opcode.MOVE_OBJECT_16.ordinal -> {
                register = it.register(1)
                false
            }
            else -> it.opcode == Opcode.IGET_OBJECT.ordinal
        }
    }
    source.fieldRef!!.also { check(it.typeSign == "Ljava/lang/Object;") }
}

@get:TargetApp("youtube")
val lithoTextHolderConstructor = findMethodDirect {
    lithoTextHolderTextField().declaredClass!!.methods.single {
        it.name == "<init>" && it.paramCount == 0
    }
}
