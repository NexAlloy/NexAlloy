package io.github.nexalloy.morphe

import io.github.nexalloy.morphe.FieldAccessFilter.Companion.parseJvmFieldAccess
import io.github.nexalloy.morphe.MethodCallFilter.Companion.parseJvmMethodCall
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData
import java.util.EnumSet

/**
 * Simple interface to control how much space is allowed between a previous
 * [InstructionFilter] match and the current [InstructionFilter].
 */
fun interface InstructionLocation {
    /**
     * @param previouslyMatchedIndex The previously matched index, or -1 if this is the first filter.
     * @param currentIndex The current method index that is about to be checked.
     */
    fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int): Boolean

    /**
     * Matching can occur anywhere after the previous instruction filter match index.
     * Is the default behavior for all filters.
     */
    class MatchAfterAnywhere : InstructionLocation {
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) = true
    }

    /**
     * Matches the first instruction of a method.
     *
     * This can only be used for the first filter, and using with any other filter will throw an exception.
     */
    class MatchFirst() : InstructionLocation {
        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            require(previouslyMatchedIndex < 0) {
                "MatchFirst can only be used for the first instruction filter"
            }
            return true
        }
    }

    /**
     * Instruction index immediately after the previous filter.
     *
     * Useful for opcodes that must always appear immediately after the last filter such as:
     * - [Opcode.MOVE_RESULT]
     * - [Opcode.MOVE_RESULT_WIDE]
     * - [Opcode.MOVE_RESULT_OBJECT]
     *
     * This cannot be used for the first filter and will throw an exception.
     */
    class MatchAfterImmediately() : InstructionLocation {
        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchAfterImmediately cannot be used for the first instruction filter"
            }
            return currentIndex - 1 == previouslyMatchedIndex
        }
    }

    /**
     * Instruction index can occur within a range of the previous instruction filter match index.
     * used to constrain instruction matching to a region after the previous instruction filter.
     *
     * This cannot be used for the first filter and will throw an exception.
     *
     * @param matchDistance The number of unmatched instructions that can exist between the
     *                      current instruction filter and the previously matched instruction filter.
     *                      A value of 0 means the current filter can only match immediately after
     *                      the previously matched instruction (making this functionally identical to
     *                      [MatchAfterImmediately]). A value of 10 means between 0 and 10 unmatched
     *                      instructions can exist between the previously matched instruction and
     *                      the current instruction filter.
     */
    class MatchAfterWithin(val matchDistance: Int) : InstructionLocation {
        init {
            require(matchDistance >= 0) {
                "matchDistance must be non-negative"
            }
        }

        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            return currentIndex - previouslyMatchedIndex - 1 <= matchDistance
        }
    }

    /**
     * Instruction index can occur only after a minimum number of unmatched instructions from the
     * previous instruction match. Or if this is used with the first filter of a fingerprint then
     * this can only match starting from a given instruction index.
     *
     * @param minimumDistanceFromLastInstruction The minimum number of unmatched instructions that
     * must exist between this instruction and the last matched instruction. A value of 0 is
     * functionally identical to [MatchAfterImmediately].
     */
    class MatchAfterAtLeast(var minimumDistanceFromLastInstruction: Int) : InstructionLocation {
        init {
            require(minimumDistanceFromLastInstruction >= 0) {
                "minimumDistanceFromLastInstruction must >= 0"
            }
        }

        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            return currentIndex - previouslyMatchedIndex - 1 >= minimumDistanceFromLastInstruction
        }
    }

    /**
     * Functionally combines both [MatchAfterAtLeast] and [MatchAfterWithin] to give a bounded range
     * where the next instruction must match relative to the previous matched instruction.
     *
     * Unlike [MatchAfterImmediately] or [MatchAfterWithin], this can be used for the first filter
     * to constrain matching to a specific range starting from index 0.
     *
     * @param minimumDistanceFromLastInstruction The minimum number of unmatched instructions that
     *                                           must exist between this instruction and the last
     *                                           matched instruction.
     * @param maximumDistanceFromLastInstruction The maximum number of unmatched instructions
     *                                           that can exist between this instruction and the
     *                                           last matched instruction.
     */
    class MatchAfterRange(
        val minimumDistanceFromLastInstruction: Int,
        val maximumDistanceFromLastInstruction: Int
    ) : InstructionLocation {

        private val minMatcher = MatchAfterAtLeast(minimumDistanceFromLastInstruction)
        private val maxMatcher = MatchAfterWithin(maximumDistanceFromLastInstruction)

        init {
            require(minimumDistanceFromLastInstruction <= maximumDistanceFromLastInstruction) {
                "minimumDistanceFromLastInstruction must be <= maximumDistanceFromLastInstruction"
            }
        }

        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            // For the first filter, previouslyMatchedIndex will be -1, and both delegates
            // will correctly enforce their own semantics starting from index 0.
            return minMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex) &&
                    maxMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex)
        }
    }
}

interface InstructionFilter {
    val location: InstructionLocation
        get() = InstructionLocation.MatchAfterAnywhere()

    fun matches(enclosingMethod: MethodData, instruction: InstructionData): Boolean = true

    context(matcher: MethodMatcher)
    fun addQuery() {
    }
}

class AnyInstruction internal constructor(
    internal val filters: List<InstructionFilter>,
    override val location: InstructionLocation
) : InstructionFilter {

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        return filters.any { filter ->
            filter.matches(enclosingMethod, instruction)
        }
    }
}

/**
 * Logical OR operator where the first filter that matches satisfies this filter.
 *
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun anyInstruction(
    vararg filters: InstructionFilter,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = AnyInstruction(filters.asList(), location)

//open class OpcodesFilter {
//    companion object {
//        fun opcodesToFilters(vararg opcodes: Opcode) = listOf(InstructionFilter {
//            opcodes(*opcodes)
//        })
//    }
//}

/**
 * Single opcode match.
 *
 * Patches can extend this as desired to do unusual or app specific instruction filtering.
 * Or Alternatively can implement [InstructionFilter] directly.
 *
 * @param opcode Opcode to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
open class OpcodeFilter(
    val opcode: Opcode,
    override val location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : InstructionFilter {

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        return Opcode.fromInt(instruction.opcode) == opcode
    }
}

/**
 * Single opcode match.
 *
 * @param opcode Opcode to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun opcode(
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = OpcodeFilter(opcode, location)

/**
 * Matches a single instruction from many kinds of opcodes.
 *
 * Patches can extend this as desired to do unusual or app specific instruction filtering.
 * Or Alternatively can implement [InstructionFilter] directly.
 *
 * @param opcodes Set of opcodes to match to. Value of `null` will match any opcode.
 *                If matching only a single opcode then instead use [OpcodeFilter].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
open class OpcodesFilter protected constructor(
    val opcodes: EnumSet<Opcode>?,
    override val location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : InstructionFilter {

    protected constructor(
        opcodes: List<Opcode>?,
        location: InstructionLocation
    ) : this(if (opcodes == null) null else EnumSet.copyOf(opcodes), location)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        val opcodesLocal = opcodes ?: return true // Match anything.
        return opcodesLocal.contains(Opcode.fromInt(instruction.opcode))
    }

    companion object {
        /**
         * First opcode can match anywhere in a method, but all
         * subsequent opcodes must match after the previous opcode.
         *
         * A value of `null` indicates to match any opcode.
         */
        fun opcodesToFilters(vararg opcodes: Opcode?): List<InstructionFilter> {
            val list = ArrayList<InstructionFilter>(opcodes.size)
            var location: InstructionLocation? = null

            opcodes.forEach { opcode ->
                // First opcode can match anywhere.
                val opcodeLocation = location ?: InstructionLocation.MatchAfterAnywhere()

                list += if (opcode == null) {
                    // Null opcode matches anything.
                    OpcodesFilter(
                        null as List<Opcode>?,
                        opcodeLocation
                    )
                } else {
                    OpcodeFilter(opcode, opcodeLocation)
                }

                if (location == null) {
                    location = InstructionLocation.MatchAfterImmediately()
                }
            }

            return list
        }
    }
}

class LiteralFilter internal constructor(
    val literal: () -> Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val literalValue: Long by lazy(literal)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        if (instruction.literal == null) return false

        return instruction.literal == literalValue
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        matcher.addUsingNumber(literalValue)
    }
}

/**
 * Long literal. Automatically converts literal to opcode hex.
 *
 * @param literal Literal number.
 * @param opcodes Opcodes to match. By default this matches any literal number opcode such as:
 *                [Opcode.CONST_4], [Opcode.CONST_16], [Opcode.CONST], [Opcode.CONST_WIDE].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun literal(
    literal: Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal }, opcodes, location)

/**
 * Integer literal. Automatically converts literal to opcode hex.
 *
 * @param literal Literal number.
 * @param opcodes Opcodes to match. By default this matches any literal number opcode such as:
 *                [Opcode.CONST_4], [Opcode.CONST_16], [Opcode.CONST], [Opcode.CONST_WIDE].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun literal(
    literal: Int,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toLong() }, opcodes, location)

/**
 * Double point literal. Automatically converts literal to opcode hex.
 *
 * @param literal Literal number.
 * @param opcodes Opcodes to match. By default this matches any literal number opcode such as:
 *                [Opcode.CONST_4], [Opcode.CONST_16], [Opcode.CONST], [Opcode.CONST_WIDE].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun literal(
    literal: Double,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits() }, opcodes, location)

/**
 * Floating point literal. Automatically converts literal to opcode hex.
 *
 * @param literal Floating point literal.
 * @param opcodes Opcodes to match. By default this matches any literal number opcode such as:
 *                [Opcode.CONST_4], [Opcode.CONST_16], [Opcode.CONST], [Opcode.CONST_WIDE].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun literal(
    literal: Float,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits().toLong() }, opcodes, location)

/**
 * Literal number value. Automatically converts the provided number to opcode hex.
 *
 * @param literal Literal number.
 * @param opcodes Opcodes to match. By default this matches any literal number opcode such as:
 *                [Opcode.CONST_4], [Opcode.CONST_16], [Opcode.CONST], [Opcode.CONST_WIDE].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun literal(
    literal: () -> Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter(literal, opcodes, location)


class MethodCallFilter internal constructor(
    val definingClass: String? = null,
    val name: String? = null,
    val parameters: List<String>? = null,
    val returnType: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    private val definingClassComparison =
        StringComparisonType.typeDeclarationToComparison(definingClass)

    private val returnTypeComparison = StringComparisonType.typeDeclarationToComparison(returnType)

    private val parameterTypeComparison =
        StringComparisonType.typeDeclarationToComparison(parameters)


    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = instruction.methodRef ?: return false

        // Store local to avoid duplicate field access and Kotlin intrinsic null check calls.
        val nameLocal = name
        if (nameLocal != null && reference.name != nameLocal) {
            return false
        }

        val definingClassLocal = definingClass
        if (definingClassLocal != null) {
            val referenceClass = reference.declaredClass!!.descriptor

            // Check if 'this' defining class is used.
            // Would be nice if this also checked all super classes,
            // but doing so requires iteratively checking all superclasses
            // up to the root class since class defs are mere Strings.
            if (definingClassLocal == "this") {
                if (referenceClass != enclosingMethod.declaredClass!!.descriptor) {
                    return false
                }
            } else if (!definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val returnTypeLocal = returnType
        if (returnTypeLocal != null && !returnTypeComparison.compare(
                reference.returnType!!.descriptor,
                returnTypeLocal
            )
        ) {
            return false
        }

        val parametersLocal = parameters
        if (parametersLocal != null && !parametersMatch(
                reference.paramTypes.map { it.descriptor },
                parametersLocal,
                parameterTypeComparison
            )
        ) {
            return false
        }

        return true
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        matcher.addInvoke {
            this@MethodCallFilter.definingClass?.let(::getTypeNameCompat)?.let { declaredClass(it) }
            this@MethodCallFilter.name?.let { name(it) }
            this@MethodCallFilter.parameters?.let { parameters(it) }
            this@MethodCallFilter.returnType?.let { returns(it) }
        }
    }


    internal companion object {
        private val regex =
            Regex("""^(L[^;]+;)->([^(\s]+)\(([^)]*)\)(\[?L[^;]+;|\[?[BCSIJFDZV])${'$'}""")

        internal fun parseJvmMethodCall(
            methodSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): MethodCallFilter {
            val matchResult = regex.matchEntire(methodSignature)
                ?: throw IllegalArgumentException("Invalid method signature: $methodSignature")

            val classDescriptor = matchResult.groupValues[1]
            val methodName = matchResult.groupValues[2]
            val paramDescriptorString = matchResult.groupValues[3]
            val returnDescriptor = matchResult.groupValues[4]

            val paramDescriptors = parseParameterDescriptors(paramDescriptorString)

            return MethodCallFilter(
                classDescriptor,
                methodName,
                paramDescriptors,
                returnDescriptor,
                opcodes,
                location
            )
        }

        /**
         * Parses a single JVM type descriptor or an array descriptor at the current position.
         * For example: Lcom/example/SomeClass; or I or [I or [Lcom/example/SomeClass;
         */
        private fun parseSingleType(params: String, startIndex: Int): Pair<String, Int> {
            var i = startIndex

            // Skip past array declaration, including multi-dimensional arrays.
            val paramsLength = params.length
            while (i < paramsLength && params[i] == '[') {
                i++
            }

            return if (i < paramsLength && params[i] == 'L') {
                // It's an object type starting with 'L', read until ';'
                val semicolonPos = params.indexOf(';', i)
                if (semicolonPos < 0) {
                    throw IllegalArgumentException("Malformed object descriptor (missing semicolon): $params")
                }
                // Substring from startIndex up to and including the semicolon.
                val typeDescriptor = params.substring(startIndex, semicolonPos + 1)
                typeDescriptor to (semicolonPos + 1)
            } else {
                // It's either a primitive or we've already consumed the array part
                // So just take one character (e.g. 'I', 'Z', 'B', etc.)
                val typeDescriptor = params.substring(startIndex, i + 1)
                typeDescriptor to (i + 1)
            }
        }

        /**
         * Parses the parameters into a list of JVM type descriptors.
         */
        private fun parseParameterDescriptors(paramString: String): List<String> {
            val result = mutableListOf<String>()
            var currentIndex = 0
            val stringLength = paramString.length

            while (currentIndex < stringLength) {
                val (type, nextIndex) = parseSingleType(paramString, currentIndex)
                result.add(type)
                currentIndex = nextIndex
            }

            return result
        }
    }
}

/**
 * Matches a method call, such as:
 * `invoke-virtual {v3, v4}, La;->b(I)V`
 *
 * @param definingClass Defining class of the field call.
 *   For calls to a method in the same class, use 'this' as the defining class.
 *   Note: 'this' does not work for methods found in superclasses.
 *   Otherwise the type declaration follow the semantics described in [StringComparisonType].
 * @param name Full name of the method. Compares using [StringComparisonType.EQUALS].
 * @param parameters Parameters of the method call. Parameter type semantics follows the syntax
 *   described in [StringComparisonType].
 * @param returnType Return type. Type declaration follow the semantics described in [StringComparisonType].
 * @param opcodes Opcode types to match. By default this matches any method call opcode: `Opcode.INVOKE_*`.
 *   If this filter must match specific types of method call, then specify the desired opcodes
 *   such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to match only static calls.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    definingClass,
    name,
    parameters,
    returnType,
    opcodes,
    location
)

/**
 * Matches a method call, such as:
 * `invoke-virtual {v3, v4}, La;->b(I)V`
 *
 * @param definingClass Defining class of the field call.
 *   For calls to a method in the same class, use 'this' as the defining class.
 *   Note: 'this' does not work for methods found in superclasses.
 *   Otherwise the type declaration follow the semantics described in [StringComparisonType].
 * @param name Full name of the method. Compares using [StringComparisonType.EQUALS].
 * @param parameters Parameters of the method call. Parameter type semantics follows the syntax
 *   described in [StringComparisonType].
 * @param returnType Return type. Type declaration follow the semantics described in [StringComparisonType].
 * @param opcode Single opcode to match. By default this matches any method call opcode: `Opcode.INVOKE_*`.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    definingClass,
    name,
    parameters,
    returnType,
    listOf(opcode),
    location
)

/**
 * Matches a method call, such as:
 * `invoke-virtual {v3, v4}, La;->b(I)V`
 *
 * @param reference Exact method reference to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun methodCall(
    reference: MethodData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    definingClass = reference.declaredClass!!.descriptor,
    name = reference.name,
    parameters = reference.paramTypes.map { it.descriptor },
    returnType = reference.returnType!!.descriptor,
    opcodes = null,
    location = location
)

/**
 * Method call for a copy pasted SMALI style method signature. e.g.:
 * `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`
 *
 * Should never be used with obfuscated method names or parameter/return types.
 *
 * @param smali Smali method call reference, such as
 *              `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`.
 * @param opcodes List of all possible opcodes to match. Defaults to matching all method calls types: `Opcode.INVOKE_*`.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun methodCall(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmMethodCall(smali, opcodes, location)

/**
 * Method call for a copy pasted SMALI style method signature. e.g.:
 * `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`
 *
 * Should never be used with obfuscated method names or parameter/return types.
 *
 * @param smali Smali method call reference, such as
 *              `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`.
 * @param opcode Single opcode type to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun methodCall(
    smali: String,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmMethodCall(smali, listOf(opcode), location)

class FieldAccessFilter internal constructor(
    val definingClass: String? = null,
    val name: String? = null,
    val type: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    private val definingClassComparison =
        StringComparisonType.typeDeclarationToComparison(definingClass)

    private val typeComparison = StringComparisonType.typeDeclarationToComparison(type)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = instruction.fieldRef ?: return false

        val nameLocal = name
        if (nameLocal != null && reference.name != nameLocal) {
            return false
        }

        val definingClassLocal = definingClass
        if (definingClassLocal != null) {
            val referenceClass = reference.declaredClass.descriptor

            if (definingClassLocal == "this") {
                if (referenceClass != enclosingMethod.declaredClass!!.descriptor) {
                    return false
                }
            } else if (!definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val typeLocal = type
        if (typeLocal != null && !typeComparison.compare(reference.typeSign, typeLocal)) {
            return false
        }

        return true
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {

        val declaredClassName = this@FieldAccessFilter.definingClass?.let(::getTypeNameCompat)

        (declaredClassName ?: name ?: type)?.let { _ ->
            matcher.addUsingField {
                declaredClassName?.let { declaredClass(it) }
                this@FieldAccessFilter.name?.let { name(it) }
                this@FieldAccessFilter.type?.let(::getTypeNameCompat)?.let { type(it) }
            }
        }
    }

    internal companion object {
        private val regex = Regex("""^(L[^;]+;)->([^:]+):(\[?L[^;]+;|\[?[BCSIJFDZV])${'$'}""")

        internal fun parseJvmFieldAccess(
            fieldSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): FieldAccessFilter {
            val matchResult = regex.matchEntire(fieldSignature)
                ?: throw IllegalArgumentException("Invalid field access smali: $fieldSignature")

            return fieldAccess(
                definingClass = matchResult.groupValues[1],
                name = matchResult.groupValues[2],
                type = matchResult.groupValues[3],
                opcodes = opcodes,
                location = location
            )
        }
    }
}


/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 *
 * @param definingClass Defining class of the field call.
 *   For calls to a method in the same class, use 'this' as the defining class.
 *   Note: 'this' does not work for methods found in superclasses.
 *   Otherwise the type declaration follow the semantics described in [StringComparisonType].
 * @param name Full name of the field. Compares using [StringComparisonType.EQUALS].
 * @param type Class type of field. Type declaration follow the semantics described in [StringComparisonType].
 * @param opcode Single opcode type to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = fieldAccess(
    definingClass,
    name,
    type,
    listOf(opcode),
    location
)

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 *
 * @param definingClass Defining class of the field call.
 *   For calls to a method in the same class, use 'this' as the defining class.
 *   Note: 'this' does not work for methods found in superclasses.
 *   Otherwise the type declaration follow the semantics described in [StringComparisonType].
 * @param name Full name of the field. Compares using [StringComparisonType.EQUALS].
 * @param type Class type of field. Type declaration follow the semantics described in [StringComparisonType].
 * @param opcodes List of all possible opcodes to match. Defaults to matching all get/put opcodes.
 *                (`Opcode.IGET`, `Opcode.SGET`, `Opcode.IPUT`, `Opcode.SPUT`, etc).
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = FieldAccessFilter(
    definingClass,
    name,
    type,
    opcodes,
    location
)

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 *
 * @param reference Exact reference to match.
 * @param opcode List of all possible opcodes to match. Defaults to matching all get/put opcodes.
 *               (`Opcode.IGET`, `Opcode.SGET`, `Opcode.IPUT`, `Opcode.SPUT`, etc).
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun fieldAccess(
    reference: FieldData,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = FieldAccessFilter(
    definingClass = reference.declaredClass.descriptor,
    name = reference.name,
    type = reference.type.descriptor,
    opcodes = listOf(opcode),
    location = location
)

///**
// * Matches a field call, such as:
// * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
// *
// * @param reference Exact reference to match.
// * @param opcode Single opcode to match.
// * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
// */
//fun fieldAccess(
//    reference: FieldReference,
//    opcodes: List<Opcode>? = null,
//    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
//) = FieldAccessFilter(
//    definingClass = reference.definingClass,
//    name = reference.name,
//    type = reference.type,
//    opcodes = opcodes,
//    location = location
//)

/**
 * Field access for a copy pasted SMALI style field access call. e.g.:
 * `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`
 *
 * Should never be used with obfuscated field names or obfuscated field types.
 * @param smali Smali field access statement, such as `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`.
 * @param opcodes List of all possible opcodes to match. Defaults to matching all get/put opcodes.
 *                (`Opcode.IGET`, `Opcode.SGET`, `Opcode.IPUT`, `Opcode.SPUT`, etc).
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun fieldAccess(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmFieldAccess(smali, opcodes, location)

/**
 * Field access for a copy pasted SMALI style field access call. e.g.:
 * `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`
 *
 * Should never be used with obfuscated field names or obfuscated field types.
 *
 * @param smali Smali field access statement, such as `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`.
 * @param opcode Single opcode type to match.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun fieldAccess(
    smali: String,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmFieldAccess(smali, listOf(opcode), location)


class StringFilter internal constructor(
    val string: () -> String,
    val comparison: StringComparisonType,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO), location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    internal val stringValue: String by lazy(string)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val stringReference = instruction.string!!
        return comparison.compare(stringReference, stringValue)
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        matcher.addUsingString(stringValue, comparison.value)
    }
}

/**
 * Literal String instruction.
 *
 * @param string string literal, using exact matching of [StringComparisonType.EQUALS].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun string(
    string: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter({ string }, StringComparisonType.EQUALS, location)

/**
 * Literal String instruction.
 *
 * @param string string literal, using exact matching of [StringComparisonType.EQUALS].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun string(
    string: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, StringComparisonType.EQUALS, location)

/**
 * Literal String instruction.
 *
 * @param string string literal.
 * @param comparison How to compare the string literal. For more precise matching of strings,
 *                   consider using [anyInstruction] with multiple exact string declarations.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun string(
    string: String,
    /**
     * How to match a given string opcode literal. Default is exact string equality. For more
     * precise matching of multiple strings, consider using [anyInstruction] with multiple
     * exact string declarations.
     */
    comparison: StringComparisonType = StringComparisonType.EQUALS,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter({ string }, comparison, location)

/**
 * Literal String instruction.
 *
 * @param string string literal.
 * @param comparison How to compare the string literal. For more precise matching of strings,
 *                   consider using [anyInstruction] with multiple exact string declarations.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun string(
    string: () -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, comparison, location)


class NewInstanceFilter internal constructor(
    val type: () -> String,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.NEW_INSTANCE, Opcode.NEW_ARRAY), location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val typeValue: String by lazy {
        val typeValue = type()
        typeValue
    }

    val comparison by lazy {
        StringComparisonType.typeDeclarationToComparison(typeValue)
    }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = instruction.classRef!!
        return comparison.compare(reference.descriptor, typeValue)
    }
}

/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun newInstance(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter({ type }, location)

/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun newInstance(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = NewInstanceFilter(type, location)

/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param comparison How to compare the opcode class type. For more precise matching of types,
 *                   consider using [anyInstruction] with multiple exact type declarations.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun newInstance(
    type: String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter({ type }, location)

/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param comparison How to compare the opcode class type. For more precise matching of types,
 *                   consider using [anyInstruction] with multiple exact type declarations.
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun newInstance(
    type: () -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter(type, location)


class InstanceOfFilter internal constructor(
    val type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.INSTANCE_OF, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val typeValue: String by lazy {
        val typeValue = type()
        typeValue
    }

    val comparison by lazy {
        StringComparisonType.typeDeclarationToComparison(typeValue)
    }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = instruction.classRef!!
        return comparison.compare(reference.descriptor, typeValue)
    }
}

/**
 * Opcode type [Opcode.INSTANCE_OF] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun instanceOf(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter({ type }, location)

/**
 * Opcode type [Opcode.INSTANCE_OF] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun instanceOf(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)

/**
 * Opcode type [Opcode.INSTANCE_OF] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun instanceOf(
    type: String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type }, location)

/**
 * Opcode type [Opcode.INSTANCE_OF] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun instanceOf(
    type: () -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)


class CheckCastFilter internal constructor(
    val type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.CHECK_CAST, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val typeValue: String by lazy {
        val typeValue = type()
        typeValue
    }

    val comparison by lazy {
        StringComparisonType.typeDeclarationToComparison(typeValue)
    }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = instruction.classRef!!
        return comparison.compare(reference.descriptor, typeValue)
    }
}

/**
 * Opcode type [Opcode.CHECK_CAST] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun checkCast(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type }, location)

/**
 * Opcode type [Opcode.CHECK_CAST] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
fun checkCast(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)
