package io.github.nexalloy.morphe

import io.github.nexalloy.morphe.Opcode.*
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData
import java.util.EnumSet

private val MethodData.definingClass
    get() = this.declaredClass?.descriptor

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
    var fieldUsageRegister = this.instructions[fieldUsageIndex].register(1)

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
            throw IllegalArgumentException("Unknown reference: $fieldSetReference")
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
