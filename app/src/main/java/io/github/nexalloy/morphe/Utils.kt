package io.github.nexalloy.morphe

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.Opcode.ADD_DOUBLE
import io.github.nexalloy.morphe.Opcode.ADD_DOUBLE_2ADDR
import io.github.nexalloy.morphe.Opcode.ADD_FLOAT
import io.github.nexalloy.morphe.Opcode.ADD_FLOAT_2ADDR
import io.github.nexalloy.morphe.Opcode.ADD_INT
import io.github.nexalloy.morphe.Opcode.ADD_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.ADD_INT_LIT16
import io.github.nexalloy.morphe.Opcode.ADD_INT_LIT8
import io.github.nexalloy.morphe.Opcode.ADD_LONG
import io.github.nexalloy.morphe.Opcode.ADD_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.AGET
import io.github.nexalloy.morphe.Opcode.AGET_BOOLEAN
import io.github.nexalloy.morphe.Opcode.AGET_BYTE
import io.github.nexalloy.morphe.Opcode.AGET_CHAR
import io.github.nexalloy.morphe.Opcode.AGET_OBJECT
import io.github.nexalloy.morphe.Opcode.AGET_SHORT
import io.github.nexalloy.morphe.Opcode.AGET_WIDE
import io.github.nexalloy.morphe.Opcode.AND_INT
import io.github.nexalloy.morphe.Opcode.AND_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.AND_INT_LIT16
import io.github.nexalloy.morphe.Opcode.AND_INT_LIT8
import io.github.nexalloy.morphe.Opcode.AND_LONG
import io.github.nexalloy.morphe.Opcode.AND_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.ARRAY_LENGTH
import io.github.nexalloy.morphe.Opcode.CONST
import io.github.nexalloy.morphe.Opcode.CONST_16
import io.github.nexalloy.morphe.Opcode.CONST_4
import io.github.nexalloy.morphe.Opcode.CONST_CLASS
import io.github.nexalloy.morphe.Opcode.CONST_HIGH16
import io.github.nexalloy.morphe.Opcode.CONST_STRING
import io.github.nexalloy.morphe.Opcode.CONST_STRING_JUMBO
import io.github.nexalloy.morphe.Opcode.CONST_WIDE
import io.github.nexalloy.morphe.Opcode.CONST_WIDE_16
import io.github.nexalloy.morphe.Opcode.CONST_WIDE_32
import io.github.nexalloy.morphe.Opcode.CONST_WIDE_HIGH16
import io.github.nexalloy.morphe.Opcode.DIV_DOUBLE
import io.github.nexalloy.morphe.Opcode.DIV_DOUBLE_2ADDR
import io.github.nexalloy.morphe.Opcode.DIV_FLOAT
import io.github.nexalloy.morphe.Opcode.DIV_FLOAT_2ADDR
import io.github.nexalloy.morphe.Opcode.DIV_INT
import io.github.nexalloy.morphe.Opcode.DIV_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.DIV_INT_LIT16
import io.github.nexalloy.morphe.Opcode.DIV_INT_LIT8
import io.github.nexalloy.morphe.Opcode.DIV_LONG
import io.github.nexalloy.morphe.Opcode.DIV_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.DOUBLE_TO_FLOAT
import io.github.nexalloy.morphe.Opcode.DOUBLE_TO_INT
import io.github.nexalloy.morphe.Opcode.DOUBLE_TO_LONG
import io.github.nexalloy.morphe.Opcode.FLOAT_TO_DOUBLE
import io.github.nexalloy.morphe.Opcode.FLOAT_TO_INT
import io.github.nexalloy.morphe.Opcode.FLOAT_TO_LONG
import io.github.nexalloy.morphe.Opcode.IGET
import io.github.nexalloy.morphe.Opcode.IGET_BOOLEAN
import io.github.nexalloy.morphe.Opcode.IGET_BYTE
import io.github.nexalloy.morphe.Opcode.IGET_CHAR
import io.github.nexalloy.morphe.Opcode.IGET_OBJECT
import io.github.nexalloy.morphe.Opcode.IGET_SHORT
import io.github.nexalloy.morphe.Opcode.IGET_WIDE
import io.github.nexalloy.morphe.Opcode.INSTANCE_OF
import io.github.nexalloy.morphe.Opcode.INT_TO_BYTE
import io.github.nexalloy.morphe.Opcode.INT_TO_CHAR
import io.github.nexalloy.morphe.Opcode.INT_TO_DOUBLE
import io.github.nexalloy.morphe.Opcode.INT_TO_FLOAT
import io.github.nexalloy.morphe.Opcode.INT_TO_LONG
import io.github.nexalloy.morphe.Opcode.INT_TO_SHORT
import io.github.nexalloy.morphe.Opcode.LONG_TO_DOUBLE
import io.github.nexalloy.morphe.Opcode.LONG_TO_FLOAT
import io.github.nexalloy.morphe.Opcode.LONG_TO_INT
import io.github.nexalloy.morphe.Opcode.MOVE
import io.github.nexalloy.morphe.Opcode.MOVE_16
import io.github.nexalloy.morphe.Opcode.MOVE_EXCEPTION
import io.github.nexalloy.morphe.Opcode.MOVE_FROM16
import io.github.nexalloy.morphe.Opcode.MOVE_OBJECT
import io.github.nexalloy.morphe.Opcode.MOVE_OBJECT_16
import io.github.nexalloy.morphe.Opcode.MOVE_OBJECT_FROM16
import io.github.nexalloy.morphe.Opcode.MOVE_RESULT
import io.github.nexalloy.morphe.Opcode.MOVE_RESULT_OBJECT
import io.github.nexalloy.morphe.Opcode.MOVE_RESULT_WIDE
import io.github.nexalloy.morphe.Opcode.MOVE_WIDE
import io.github.nexalloy.morphe.Opcode.MOVE_WIDE_16
import io.github.nexalloy.morphe.Opcode.MOVE_WIDE_FROM16
import io.github.nexalloy.morphe.Opcode.MUL_DOUBLE
import io.github.nexalloy.morphe.Opcode.MUL_DOUBLE_2ADDR
import io.github.nexalloy.morphe.Opcode.MUL_FLOAT
import io.github.nexalloy.morphe.Opcode.MUL_FLOAT_2ADDR
import io.github.nexalloy.morphe.Opcode.MUL_INT
import io.github.nexalloy.morphe.Opcode.MUL_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.MUL_INT_LIT16
import io.github.nexalloy.morphe.Opcode.MUL_INT_LIT8
import io.github.nexalloy.morphe.Opcode.MUL_LONG
import io.github.nexalloy.morphe.Opcode.MUL_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.NEG_DOUBLE
import io.github.nexalloy.morphe.Opcode.NEG_FLOAT
import io.github.nexalloy.morphe.Opcode.NEG_INT
import io.github.nexalloy.morphe.Opcode.NEG_LONG
import io.github.nexalloy.morphe.Opcode.NEW_ARRAY
import io.github.nexalloy.morphe.Opcode.NEW_INSTANCE
import io.github.nexalloy.morphe.Opcode.NOT_INT
import io.github.nexalloy.morphe.Opcode.NOT_LONG
import io.github.nexalloy.morphe.Opcode.OR_INT
import io.github.nexalloy.morphe.Opcode.OR_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.OR_INT_LIT16
import io.github.nexalloy.morphe.Opcode.OR_INT_LIT8
import io.github.nexalloy.morphe.Opcode.OR_LONG
import io.github.nexalloy.morphe.Opcode.OR_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.REM_DOUBLE
import io.github.nexalloy.morphe.Opcode.REM_DOUBLE_2ADDR
import io.github.nexalloy.morphe.Opcode.REM_FLOAT
import io.github.nexalloy.morphe.Opcode.REM_FLOAT_2ADDR
import io.github.nexalloy.morphe.Opcode.REM_INT
import io.github.nexalloy.morphe.Opcode.REM_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.REM_INT_LIT16
import io.github.nexalloy.morphe.Opcode.REM_INT_LIT8
import io.github.nexalloy.morphe.Opcode.REM_LONG
import io.github.nexalloy.morphe.Opcode.REM_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.RSUB_INT
import io.github.nexalloy.morphe.Opcode.RSUB_INT_LIT8
import io.github.nexalloy.morphe.Opcode.SGET
import io.github.nexalloy.morphe.Opcode.SGET_BOOLEAN
import io.github.nexalloy.morphe.Opcode.SGET_BYTE
import io.github.nexalloy.morphe.Opcode.SGET_CHAR
import io.github.nexalloy.morphe.Opcode.SGET_OBJECT
import io.github.nexalloy.morphe.Opcode.SGET_SHORT
import io.github.nexalloy.morphe.Opcode.SGET_WIDE
import io.github.nexalloy.morphe.Opcode.SHL_INT
import io.github.nexalloy.morphe.Opcode.SHL_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.SHL_INT_LIT8
import io.github.nexalloy.morphe.Opcode.SHL_LONG
import io.github.nexalloy.morphe.Opcode.SHL_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.SHR_INT
import io.github.nexalloy.morphe.Opcode.SHR_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.SHR_INT_LIT8
import io.github.nexalloy.morphe.Opcode.SHR_LONG
import io.github.nexalloy.morphe.Opcode.SHR_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.SUB_DOUBLE
import io.github.nexalloy.morphe.Opcode.SUB_DOUBLE_2ADDR
import io.github.nexalloy.morphe.Opcode.SUB_FLOAT
import io.github.nexalloy.morphe.Opcode.SUB_FLOAT_2ADDR
import io.github.nexalloy.morphe.Opcode.SUB_INT
import io.github.nexalloy.morphe.Opcode.SUB_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.SUB_LONG
import io.github.nexalloy.morphe.Opcode.SUB_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.USHR_INT
import io.github.nexalloy.morphe.Opcode.USHR_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.USHR_INT_LIT8
import io.github.nexalloy.morphe.Opcode.USHR_LONG
import io.github.nexalloy.morphe.Opcode.USHR_LONG_2ADDR
import io.github.nexalloy.morphe.Opcode.XOR_INT
import io.github.nexalloy.morphe.Opcode.XOR_INT_2ADDR
import io.github.nexalloy.morphe.Opcode.XOR_INT_LIT16
import io.github.nexalloy.morphe.Opcode.XOR_INT_LIT8
import io.github.nexalloy.morphe.Opcode.XOR_LONG
import io.github.nexalloy.morphe.Opcode.XOR_LONG_2ADDR
import java.util.EnumSet
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData

@Suppress("NOTHING_TO_INLINE")
inline fun setExtensionIsPatchIncluded(extension: Class<*>){
    extension.getDeclaredMethod("isPatchIncluded").hookMethod(XC_MethodReplacement.returnConstant(true))
}

private val MethodData.definingClass
    get() = this.declaredClass?.descriptor

private fun getParamTypeSigns(paramSigns: String): List<String> {
    val params = mutableListOf<String>()
    var left = 0
    var right = 0
    while (right < paramSigns.length) {
        val c = paramSigns[right]
        if (c == '[') {
            right++
            continue
        } else if (c == 'L') {
            val end = paramSigns.indexOf(';', right)
            right = end
        }
        val sign = paramSigns.substring(left, right + 1)
        params.add(sign)
        left = ++right
    }
    if (left != right) {
        throw IllegalStateException("Unknown signString: $paramSigns")
    }
    return params
}

val MethodData.parameters: List<String>
    get() {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf("(", idx1 + 1)
        val idx3 = descriptor.indexOf(")", idx2 + 1)
        return getParamTypeSigns(descriptor.substring(idx2 + 1, idx3))
    }

val writeOpcodes: EnumSet<Opcode> = EnumSet.of(
    ARRAY_LENGTH,
    INSTANCE_OF,
    NEW_INSTANCE, NEW_ARRAY,
    MOVE, MOVE_FROM16, MOVE_16, MOVE_WIDE, MOVE_WIDE_FROM16, MOVE_WIDE_16, MOVE_OBJECT,
    MOVE_OBJECT_FROM16, MOVE_OBJECT_16, MOVE_RESULT, MOVE_RESULT_WIDE, MOVE_RESULT_OBJECT, MOVE_EXCEPTION,
    CONST, CONST_4, CONST_16, CONST_HIGH16, CONST_WIDE_16, CONST_WIDE_32,
    CONST_WIDE, CONST_WIDE_HIGH16, CONST_STRING, CONST_STRING_JUMBO,
    CONST_CLASS,
    IGET, IGET_WIDE, IGET_OBJECT, IGET_BOOLEAN, IGET_BYTE, IGET_CHAR, IGET_SHORT,
//    IGET_VOLATILE, IGET_WIDE_VOLATILE, IGET_OBJECT_VOLATILE,
    SGET, SGET_WIDE, SGET_OBJECT, SGET_BOOLEAN, SGET_BYTE, SGET_CHAR, SGET_SHORT,
//    SGET_VOLATILE, SGET_WIDE_VOLATILE, SGET_OBJECT_VOLATILE,
    AGET, AGET_WIDE, AGET_OBJECT, AGET_BOOLEAN, AGET_BYTE, AGET_CHAR, AGET_SHORT,
    // Arithmetic and logical operations.
    ADD_DOUBLE_2ADDR, ADD_DOUBLE, ADD_FLOAT_2ADDR, ADD_FLOAT, ADD_INT_2ADDR,
    ADD_INT_LIT8, ADD_INT, ADD_LONG_2ADDR, ADD_LONG, ADD_INT_LIT16,
    AND_INT_2ADDR, AND_INT_LIT8, AND_INT_LIT16, AND_INT, AND_LONG_2ADDR, AND_LONG,
    DIV_DOUBLE_2ADDR, DIV_DOUBLE, DIV_FLOAT_2ADDR, DIV_FLOAT, DIV_INT_2ADDR,
    DIV_INT_LIT16, DIV_INT_LIT8, DIV_INT, DIV_LONG_2ADDR, DIV_LONG,
    DOUBLE_TO_FLOAT, DOUBLE_TO_INT, DOUBLE_TO_LONG,
    FLOAT_TO_DOUBLE, FLOAT_TO_INT, FLOAT_TO_LONG,
    INT_TO_BYTE, INT_TO_CHAR, INT_TO_DOUBLE, INT_TO_FLOAT, INT_TO_LONG, INT_TO_SHORT,
    LONG_TO_DOUBLE, LONG_TO_FLOAT, LONG_TO_INT,
    MUL_DOUBLE_2ADDR, MUL_DOUBLE, MUL_FLOAT_2ADDR, MUL_FLOAT, MUL_INT_2ADDR,
    MUL_INT_LIT16, MUL_INT_LIT8, MUL_INT, MUL_LONG_2ADDR, MUL_LONG,
    NEG_DOUBLE, NEG_FLOAT, NEG_INT, NEG_LONG,
    NOT_INT, NOT_LONG,
    OR_INT_2ADDR, OR_INT_LIT16, OR_INT_LIT8, OR_INT, OR_LONG_2ADDR, OR_LONG,
    REM_DOUBLE_2ADDR, REM_DOUBLE, REM_FLOAT_2ADDR, REM_FLOAT, REM_INT_2ADDR,
    REM_INT_LIT16, REM_INT_LIT8, REM_INT, REM_LONG_2ADDR, REM_LONG,
    RSUB_INT_LIT8, RSUB_INT,
    SHL_INT_2ADDR, SHL_INT_LIT8, SHL_INT, SHL_LONG_2ADDR, SHL_LONG,
    SHR_INT_2ADDR, SHR_INT_LIT8, SHR_INT, SHR_LONG_2ADDR, SHR_LONG,
    SUB_DOUBLE_2ADDR, SUB_DOUBLE, SUB_FLOAT_2ADDR, SUB_FLOAT, SUB_INT_2ADDR,
    SUB_INT, SUB_LONG_2ADDR, SUB_LONG,
    USHR_INT_2ADDR, USHR_INT_LIT8, USHR_INT, USHR_LONG_2ADDR, USHR_LONG,
    XOR_INT_2ADDR, XOR_INT_LIT16, XOR_INT_LIT8, XOR_INT, XOR_LONG_2ADDR, XOR_LONG,
)
val InstructionData.opcodeEnum: Opcode get() = Opcode.fromInt(opcode)
val InstructionData.writeRegister: Int?
    get() {
        if (opcodeEnum !in writeOpcodes) {
            return null
        }
        return register(0)
    }

private val moveOpcodes: EnumSet<Opcode> = EnumSet.of(
    Opcode.MOVE, Opcode.MOVE_FROM16, Opcode.MOVE_16,
    Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE_16,
    Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16
)

/**
 * Find the instruction index used for a toString() StringBuilder write of a given String name.
 *
 * @param fieldName The name of the field to find. Partial matches are allowed.
 */
private fun MethodData.findInstructionIndexFromToString(fieldName: String, isField: Boolean) : Int {
    val stringIndex = indexOfFirstInstruction { this.string?.contains(fieldName) == true }
    if (stringIndex < 0) {
        throw IllegalArgumentException("Could not find usage of string: '$fieldName'")
    }
    val stringRegister = this.instructions[stringIndex].register(0)

    // Find use of the string with a StringBuilder.
    val stringUsageIndex = indexOfFirstInstruction(stringIndex) {
        val reference = this.methodRef
        reference?.definingClass == "Ljava/lang/StringBuilder;" &&
                registerOrNull(1) == stringRegister
    }
    if (stringUsageIndex < 0) {
        throw IllegalArgumentException("Could not find StringBuilder usage in: $this")
    }

    // Find the next usage of StringBuilder, which should be the desired field.
    val fieldUsageIndex = indexOfFirstInstruction(stringUsageIndex + 1) {
        val reference = this.methodRef
        reference?.definingClass == "Ljava/lang/StringBuilder;" && reference.name == "append"
    }
    if (fieldUsageIndex < 0) {
        // Should never happen.
        throw IllegalArgumentException("Could not find StringBuilder append usage in: $this")
    }
    val fieldUsageInstruction = this.instructions[fieldUsageIndex]
    var fieldUsageRegister = fieldUsageInstruction.registerOrNull(1) ?: fieldUsageInstruction.register(0)

    // Look backwards up the method to find the instruction that sets the register.
    var fieldSetIndex = indexOfFirstInstructionReversedOrThrow(fieldUsageIndex - 1) {
        fieldUsageRegister == writeRegister
    }

    // Some 'toString()' methods, despite using a StringBuilder, Convert the value via
    // 'Object.toString()' or 'String.valueOf(object)' before appending it to the StringBuilder.
    // In this case, the correct index cannot be found.
    // Additional validation is done to find the index of the correct field or method.
    //
    // Check up to 3 method calls.
    var checksLeft = 3
    while (checksLeft > 0) {
        // If the field is a method call, then adjust from MOVE_RESULT to the method call.
        val fieldSetOpcode = instructions[fieldSetIndex].opcodeEnum
        if (fieldSetOpcode == MOVE_RESULT ||
            fieldSetOpcode == MOVE_RESULT_WIDE ||
            fieldSetOpcode == MOVE_RESULT_OBJECT
        ) {
            fieldSetIndex--
        }

        val fieldSetInstruction = instructions[fieldSetIndex]

        // If the instruction is a register MOVE (e.g. move-object/16, move/16),
        // trace backwards to find where the source register was set.
        if (fieldSetInstruction.registerCount == 2 &&
            Opcode.fromInt(fieldSetInstruction.opcode) in moveOpcodes
        ) {
            fieldUsageRegister = fieldSetInstruction.register(1)
            fieldSetIndex = indexOfFirstInstructionReversedOrThrow(fieldSetIndex - 1) {
                fieldUsageRegister == writeRegister
            }
            continue
        }

        val fieldSetReference = instructions[fieldSetIndex]

        if (isField && fieldSetReference.fieldRef != null ||
            !isField && fieldSetReference.methodRef != null
        ) {
            // Valid index.
            return fieldSetIndex
        } else if (fieldSetReference.methodRef?.returnTypeName == "java.lang.String"
            // Object.toString(), String.valueOf(object)
        ) {
            fieldUsageRegister = instructions[fieldSetIndex].register(0)

            // Look backwards up the method to find the instruction that sets the register.
            fieldSetIndex = indexOfFirstInstructionReversedOrThrow(fieldSetIndex - 1) {
                fieldUsageRegister == writeRegister
            }
            checksLeft--
        } else {
            throw IllegalArgumentException("Unknown reference or instruction: $fieldSetInstruction")
        }
    }

    return fieldSetIndex
}

/**
 * Find the field used for a toString() StringBuilder write of a given String name.
 *
 * @param fieldName The name of the field to find. Partial matches are allowed.
 */
fun MethodData.findFieldFromToString(fieldName: String) : FieldData {
    val methodUsageIndex = findInstructionIndexFromToString(fieldName, true)
    return instructions[methodUsageIndex].fieldRef!!
}

/** Resolves the getter used for a named toString() component. */
fun MethodData.findMethodFromToString(fieldName: String): MethodData =
    instructions[findInstructionIndexFromToString(fieldName, false)].methodRef!!

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun MethodData.indexOfFirstInstruction(startIndex: Int = 0, filter: InstructionData.() -> Boolean): Int {
    var instructions = this.instructions ?: return -1
    if (startIndex != 0) {
        instructions = instructions.drop(startIndex)
    }
    val index = instructions.indexOfFirst(filter)

    return if (index >= 0) {
        startIndex + index
    } else {
        -1
    }
}

/**
 * Get the index of matching instruction.
 *
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun MethodData.indexOfFirstInstruction(filter: InstructionFilter): Int = indexOfFirstInstruction(0, filter)

/**
 * Get the index of matching instruction, starting from [startIndex].
 *
 * @param startIndex Optional starting index to search from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun MethodData.indexOfFirstInstruction(startIndex: Int = 0, filter: InstructionFilter): Int {
    val method = this
    return indexOfFirstInstruction(startIndex) { filter.matches(method, this) }
}

/**
 * @return The index of the first opcode specified
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun MethodData.indexOfFirstInstructionOrThrow(targetOpcode: Opcode): Int = indexOfFirstInstructionOrThrow(0, targetOpcode)

/**
 * @return The index of the first opcode specified, starting from the index specified.
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun MethodData.indexOfFirstInstructionOrThrow(startIndex: Int = 0, targetOpcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(startIndex) {
        opcode == targetOpcode.opCode
    }

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return The index of the instruction.
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun MethodData.indexOfFirstInstructionOrThrow(startIndex: Int = 0, filter: InstructionData.() -> Boolean): Int {
    val index = indexOfFirstInstruction(startIndex, filter)
    if (index < 0) {
        throw Exception("Could not find instruction index")
    }

    return index
}

/**
 * Get the index of matching instruction.
 *
 * @return The index of the instruction.
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun MethodData.indexOfFirstInstructionOrThrow(filter: InstructionFilter): Int = indexOfFirstInstructionOrThrow(0, filter)

/**
 * Get the index of matching instruction, starting from [startIndex].
 *
 * @return The index of the instruction.
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun MethodData.indexOfFirstInstructionOrThrow(startIndex: Int = 0, filter: InstructionFilter): Int {
    val method = this
    return indexOfFirstInstructionOrThrow(startIndex) { filter.matches(method, this) }
}

/**
 * Get the index of matching instruction,
 * starting from the end of the method and searching down.
 *
 * @return -1 if the instruction is not found.
 */
fun MethodData.indexOfFirstInstructionReversedOrThrow(targetOpcode: Opcode): Int = indexOfFirstInstructionReversedOrThrow {
    opcode == targetOpcode.ordinal
}

/**
 * Get the index of matching instruction,
 * starting from [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return The index of the instruction.
 * @see indexOfFirstInstructionReversed
 */
fun MethodData.indexOfFirstInstructionReversedOrThrow(startIndex: Int? = null, filter: InstructionData.() -> Boolean): Int {
    val index = indexOfFirstInstructionReversed(startIndex, filter)

    if (index < 0) {
        throw Exception("Could not find instruction index")
    }

    return index
}

/**
 * Get the index of matching instruction,
 * starting from the end of the method and searching down.
 *
 * @return The index of the instruction.
 * @throws PatchException
 * @see indexOfFirstInstructionReversed
 */
fun MethodData.indexOfFirstInstructionReversedOrThrow(filter: InstructionFilter): Int = indexOfFirstInstructionReversedOrThrow(null, filter)

/**
 * Get the index of matching instruction,
 * starting from [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return The index of the instruction.
 * @see indexOfFirstInstructionReversed
 */
fun MethodData.indexOfFirstInstructionReversedOrThrow(startIndex: Int? = null, filter: InstructionFilter): Int {
    val method = this
    return indexOfFirstInstructionReversedOrThrow(startIndex) {
        filter.matches(method, this)
    }
}

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun MethodData.indexOfFirstInstructionReversed(startIndex: Int? = null, targetOpcode: Opcode): Int =
    indexOfFirstInstructionReversed(startIndex) {
        opcode == targetOpcode.ordinal
    }


/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun MethodData.indexOfFirstInstructionReversed(startIndex: Int? = null, filter: InstructionData.() -> Boolean): Int {
    var instructions = this.instructions
    if (startIndex != null) {
        instructions = instructions.take(startIndex + 1)
    }

    return instructions.indexOfLast(filter)
}
