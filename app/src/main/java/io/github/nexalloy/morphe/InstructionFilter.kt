@file:OptIn(kotlin.experimental.ExperimentalTypeInference::class)

package io.github.nexalloy.morphe

import io.github.nexalloy.morphe.FieldAccessFilter.Companion.parseJvmFieldAccess
import io.github.nexalloy.morphe.MethodCallFilter.Companion.parseJvmMethodCall
import java.util.EnumSet
import java.util.WeakHashMap
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData

/**
 * Simple interface to control where an [InstructionFilter] can match,
 * relative to the index matched by the previous [InstructionFilter].
 *
 * Most locations match after the previous match, but some locations such as
 * [MatchBeforeAnywhere] and [MatchBeforeImmediately] match before the previous match. Each filter is
 * always located relative to the filter declared immediately before it, so the matching direction
 * can change between each filter.
 *
 * Because the matching direction can change, a filter can match the same instruction as an earlier
 * filter. For example, a [MatchAfterImmediately] filter declared after a [MatchBeforeImmediately]
 * filter matches the same instruction index as the filter declared before both of them.
 *
 * Each filter tries the instructions its location allows in the order the location searches.
 * If a filter cannot match, then the filter declared before it is retried with its next matching
 * instruction, and so on back to the first filter. The fingerprint does not match only if no
 * combination of matching instructions matches every filter.
 */
fun interface InstructionLocation {
    /**
     * Searching starts at [searchStartIndex] and stops at the first index that is not valid.
     *
     * @param previouslyMatchedIndex The previously matched index, or -1 if this is the first filter.
     * @param currentIndex The current method index that is about to be checked.
     */
    fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean

    /**
     * If searching is backwards towards the start of the method, instead of forward.
     * A filter that is retried continues searching in the same direction.
     */
    val searchesBackwards: Boolean
        get() = false

    /**
     * The first method index to check.
     *
     * @param previouslyMatchedIndex The previously matched index, or -1 if this is the first filter.
     * @param lastMethodIndex The index of the last instruction of the method.
     */
    fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
        return if (searchesBackwards) previouslyMatchedIndex - 1 else previouslyMatchedIndex + 1
    }

    /**
     * Matching can occur anywhere after the previous instruction filter match index.
     * Searching is forward from the previous match, so the closest matching instruction is matched.
     * If a later filter does not match, then the next closest matching instruction is tried.
     * Is the default behavior for all filters.
     *
     * [MatchAfterLast] instead tries the farthest instruction after the previous match first.
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
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex < 0) {
                "MatchFirst can only be used for the first instruction filter"
            }
            return currentIndex == 0
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
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchAfterImmediately cannot be used for the first instruction filter"
            }
            return currentIndex - 1 == previouslyMatchedIndex
        }
    }

    /**
     * Instruction index can occur within a range of the previous instruction filter match index.
     * Used to constrain instruction matching to a region after the previous instruction filter.
     * Searching is forward from the previous match, so the closest matching instruction is matched.
     * If a later filter does not match, then the next closest matching instruction within the range is tried.
     *
     * If used for the first filter, then the instruction must be within the first
     * [matchDistance] + 1 instructions of the method.
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

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            return currentIndex - previouslyMatchedIndex - 1 <= matchDistance
        }
    }

    /**
     * Matches the last instruction of the method that matches the filter, and is
     * after the previous instruction filter match. Searching is backwards from the end
     * of the method up to (and excluding) the previously matched index.
     *
     * If used for the first filter, then the last matching instruction of the entire method is matched.
     *
     * If a later filter does not match, then the next earlier matching instruction is tried.
     *
     * Unlike [MatchAfterAnywhere], which tries the closest instruction after the previous match first,
     * this tries the farthest first.
     */
    class MatchAfterLast : InstructionLocation {
        override val searchesBackwards: Boolean
            get() = true

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
            return lastMethodIndex
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            return currentIndex > previouslyMatchedIndex
        }
    }

    /**
     * Matching can occur anywhere before the previous instruction filter match index.
     * Searching is backwards from the previous match, so the closest matching instruction is matched.
     * If a later filter does not match, then the next closest matching instruction is tried.
     *
     * [MatchBeforeFirst] instead tries the farthest instruction before the previous match first.
     *
     * This cannot be used for the first filter and will throw an exception.
     */
    class MatchBeforeAnywhere : InstructionLocation {
        override val searchesBackwards: Boolean
            get() = true

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeAnywhere cannot be used for the first instruction filter"
            }
            return previouslyMatchedIndex - 1
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeAnywhere cannot be used for the first instruction filter"
            }
            return currentIndex < previouslyMatchedIndex
        }
    }

    /**
     * Matches the first instruction of the method that matches the filter, and is
     * before the previous instruction filter match. Searching is forward from the start
     * of the method up to (and excluding) the previously matched index.
     *
     * If a later filter does not match, then the next later matching instruction is tried.
     *
     * Unlike [MatchBeforeAnywhere], which tries the closest instruction before the previous match first,
     * this tries the farthest first.
     *
     * This cannot be used for the first filter and will throw an exception.
     */
    class MatchBeforeFirst : InstructionLocation {
        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeFirst cannot be used for the first instruction filter"
            }
            return 0
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeFirst cannot be used for the first instruction filter"
            }
            return currentIndex < previouslyMatchedIndex
        }
    }

    /**
     * Instruction index immediately before the previous filter.
     *
     * This cannot be used for the first filter and will throw an exception.
     */
    class MatchBeforeImmediately : InstructionLocation {
        override val searchesBackwards: Boolean
            get() = true

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeImmediately cannot be used for the first instruction filter"
            }
            return previouslyMatchedIndex - 1
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeImmediately cannot be used for the first instruction filter"
            }
            return currentIndex + 1 == previouslyMatchedIndex
        }
    }

    /**
     * Instruction index can occur within a range before the previous instruction filter match index.
     * Used to constrain instruction matching to a region before the previous instruction filter.
     * Searching is backwards from the previous match, so the closest matching instruction is matched.
     * If a later filter does not match, then the next closest matching instruction within the range is tried.
     *
     * This cannot be used for the first filter and will throw an exception.
     *
     * @param matchDistance The number of unmatched instructions that can exist between the
     *                      current instruction filter and the previously matched instruction filter.
     *                      A value of 0 means the current filter can only match immediately before
     *                      the previously matched instruction (making this functionally identical to
     *                      [MatchBeforeImmediately]). A value of 10 means between 0 and 10 unmatched
     *                      instructions can exist between the current instruction filter and the
     *                      previously matched instruction.
     */
    class MatchBeforeWithin(val matchDistance: Int) : InstructionLocation {
        init {
            require(matchDistance >= 0) {
                "matchDistance must be non-negative"
            }
        }

        override val searchesBackwards: Boolean
            get() = true

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) : Int {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeWithin cannot be used for the first instruction filter"
            }
            return previouslyMatchedIndex - 1
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchBeforeWithin cannot be used for the first instruction filter"
            }
            return currentIndex < previouslyMatchedIndex &&
                    previouslyMatchedIndex - currentIndex - 1 <= matchDistance
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

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) =
            previouslyMatchedIndex + minimumDistanceFromLastInstruction + 1

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
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

        override fun searchStartIndex(previouslyMatchedIndex: Int, lastMethodIndex: Int) =
            minMatcher.searchStartIndex(previouslyMatchedIndex, lastMethodIndex)

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int): Boolean {
            // For the first filter, previouslyMatchedIndex will be -1, and both delegates
            // will correctly enforce their own semantics starting from index 0.
            return minMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex) &&
                    maxMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex)
        }
    }
}


/** Values belong to an open bridge; a filter singleton can be reused for another APK. */
private class FilterValue<T>(private val supplier: DexKitBridge.() -> T) {
    private class Value<T>(val value: T)
    private val values = WeakHashMap<DexKitBridge, Value<T>>()

    context(bridge: DexKitBridge)
    fun get(): T = synchronized(values) {
        values.getOrPut(bridge) { Value(supplier(bridge)) }.value
    }
}

fun interface InstructionFilter {
    val location: InstructionLocation
        get() = InstructionLocation.MatchAfterAnywhere()

    context(bridge: DexKitBridge)
    fun matches(enclosingMethod: MethodData, instruction: InstructionData): Boolean

    context(bridge: DexKitBridge, matcher: MethodMatcher)
    fun addQuery() {
    }
}

class AnyInstruction internal constructor(
    internal val filters: List<InstructionFilter>,
    override val location: InstructionLocation
) : InstructionFilter {

    context(bridge: DexKitBridge)
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

    context(bridge: DexKitBridge)
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

    context(bridge: DexKitBridge)
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
    literalOrNull: DexKitBridge.() -> Long?,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val resolvedLiteral = FilterValue(literalOrNull)

    context(bridge: DexKitBridge)
    internal val literalValue: Long? get() = resolvedLiteral.get()

    val literal: DexKitBridge.() -> Long = { literalValue ?: error("Literal does not exist in this app") }

    context(bridge: DexKitBridge)
    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        if (instruction.literal == null) return false

        return literalValue != null && instruction.literal == literalValue
    }

    context(bridge: DexKitBridge, matcher: MethodMatcher)
    override fun addQuery() {
        literalValue?.let { matcher.addUsingNumber(it) }
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
    literal: DexKitBridge.() -> Long?,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter(literal, opcodes, location)


class MethodCallFilter private constructor(
    private val constantValues: Values?,
    private val dynamicValues: FilterValue<Values>?,
    opcodes: List<Opcode>?,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {
    internal constructor(
        definingClass: String? = null,
        name: String? = null,
        parameters: List<String>? = null,
        returnType: String? = null,
        opcodes: List<Opcode>? = null,
        location: InstructionLocation,
    ) : this(Values(definingClass, name, parameters, returnType), null, opcodes, location)

    internal constructor(
        evaluateValues: DexKitBridge.() -> Values,
        opcodes: List<Opcode>? = null,
        location: InstructionLocation,
    ) : this(null, FilterValue(evaluateValues), opcodes, location)

    internal class Values(
        val definingClass: String?,
        val name: String?,
        val parameters: List<String>?,
        val returnType: String?,
    ) {
        val definingClassComparison = StringComparisonType.typeDeclarationToComparison(definingClass)
        val returnTypeComparison = StringComparisonType.typeDeclarationToComparison(returnType)
        val parameterTypeComparison = StringComparisonType.typeDeclarationToComparison(parameters)
    }

    context(bridge: DexKitBridge)
    private val values: Values get() = constantValues ?: dynamicValues!!.get()

    context(bridge: DexKitBridge)
    val definingClass: String? get() = values.definingClass

    context(bridge: DexKitBridge)
    val name: String? get() = values.name

    context(bridge: DexKitBridge)
    val parameters: List<String>? get() = values.parameters

    context(bridge: DexKitBridge)
    val returnType: String? get() = values.returnType

    context(bridge: DexKitBridge)
    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val values = values
        val reference = instruction.methodRef ?: return false

        // Store local to avoid duplicate field access and Kotlin intrinsic null check calls.
        val nameLocal = values.name
        if (nameLocal != null && reference.name != nameLocal) {
            return false
        }

        val definingClassLocal = values.definingClass
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
            } else if (!values.definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val returnTypeLocal = values.returnType
        if (returnTypeLocal != null && !values.returnTypeComparison.compare(
                reference.returnType!!.descriptor,
                returnTypeLocal
            )
        ) {
            return false
        }

        val parametersLocal = values.parameters
        if (parametersLocal != null && !parametersMatch(
                reference.paramTypes.map { it.descriptor },
                parametersLocal,
                values.parameterTypeComparison
            )
        ) {
            return false
        }

        return true
    }

    context(bridge: DexKitBridge, matcher: MethodMatcher)
    override fun addQuery() {
        val values = values
        matcher.addInvoke {
            values.definingClass?.let(::getTypeNameCompat)?.let { declaredClass(it) }
            values.name?.let { name(it) }
            values.parameters?.let { parameters(it) }
            values.returnType?.let { returns(it) }
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

class FieldAccessFilter private constructor(
    private val constantValues: Values?,
    private val dynamicValues: FilterValue<Values>?,
    opcodes: List<Opcode>?,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {
    internal constructor(
        definingClass: String? = null,
        name: String? = null,
        type: String? = null,
        opcodes: List<Opcode>? = null,
        location: InstructionLocation,
    ) : this(Values(definingClass, name, type), null, opcodes, location)

    internal constructor(
        evaluateValues: DexKitBridge.() -> Values,
        opcodes: List<Opcode>? = null,
        location: InstructionLocation,
    ) : this(null, FilterValue(evaluateValues), opcodes, location)

    internal class Values(
        val definingClass: String?,
        val name: String?,
        val type: String?,
    ) {
        val definingClassComparison = StringComparisonType.typeDeclarationToComparison(definingClass)
        val typeComparison = StringComparisonType.typeDeclarationToComparison(type)
    }

    context(bridge: DexKitBridge)
    private val values: Values get() = constantValues ?: dynamicValues!!.get()

    context(bridge: DexKitBridge)
    val definingClass: String? get() = values.definingClass

    context(bridge: DexKitBridge)
    val name: String? get() = values.name

    context(bridge: DexKitBridge)
    val type: String? get() = values.type

    context(bridge: DexKitBridge)
    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val values = values
        val reference = instruction.fieldRef ?: return false

        val nameLocal = values.name
        if (nameLocal != null && reference.name != nameLocal) {
            return false
        }

        val definingClassLocal = values.definingClass
        if (definingClassLocal != null) {
            val referenceClass = reference.declaredClass.descriptor

            if (definingClassLocal == "this") {
                if (referenceClass != enclosingMethod.declaredClass!!.descriptor) {
                    return false
                }
            } else if (!values.definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val typeLocal = values.type
        if (typeLocal != null && !values.typeComparison.compare(reference.typeSign, typeLocal)) {
            return false
        }

        return true
    }

    context(bridge: DexKitBridge, matcher: MethodMatcher)
    override fun addQuery() {
        val values = values

        val declaredClassName = values.definingClass?.let(::getTypeNameCompat)
        val typeName = values.type?.let(::getTypeNameCompat)

        (declaredClassName ?: values.name ?: typeName)?.let { _ ->
            matcher.addUsingField {
                declaredClassName?.let { declaredClass(it) }
                values.name?.let { name(it) }
                typeName?.let { type(it) }
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
    val string: DexKitBridge.() -> String,
    val comparison: StringComparisonType,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO), location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val resolvedString = FilterValue(string)

    context(bridge: DexKitBridge)
    internal val stringValue: String get() = resolvedString.get()

    context(bridge: DexKitBridge)
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

    context(bridge: DexKitBridge, matcher: MethodMatcher)
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
    string: DexKitBridge.() -> String,
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
    string: DexKitBridge.() -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, comparison, location)


class NewInstanceFilter internal constructor(
    val type: DexKitBridge.() -> String,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.NEW_INSTANCE, Opcode.NEW_ARRAY), location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val resolvedType = FilterValue(type)

    context(bridge: DexKitBridge)
    private val typeValue: String get() = resolvedType.get()

    context(bridge: DexKitBridge)
    val comparison: StringComparisonType get() = StringComparisonType.typeDeclarationToComparison(typeValue)

    context(bridge: DexKitBridge)
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
@OverloadResolutionByLambdaReturnType
fun newInstance(
    type: DexKitBridge.() -> String,
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
@OverloadResolutionByLambdaReturnType
fun newInstance(
    type: DexKitBridge.() -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter(type, location)


class InstanceOfFilter internal constructor(
    val type: DexKitBridge.() -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.INSTANCE_OF, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val resolvedType = FilterValue(type)

    context(bridge: DexKitBridge)
    private val typeValue: String get() = resolvedType.get()

    context(bridge: DexKitBridge)
    val comparison: StringComparisonType get() = StringComparisonType.typeDeclarationToComparison(typeValue)

    context(bridge: DexKitBridge)
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
@OverloadResolutionByLambdaReturnType
fun instanceOf(
    type: DexKitBridge.() -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter(type, location)

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
) = InstanceOfFilter({ type }, location)

/**
 * Opcode type [Opcode.INSTANCE_OF] with a non obfuscated class type.
 *
 * @param type Class type semantics as described in [StringComparisonType].
 * @param location Where this filter is allowed to match. Default is anywhere after the previous instruction.
 */
@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
@OverloadResolutionByLambdaReturnType
fun instanceOf(
    type: DexKitBridge.() -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter(type, location)


class CheckCastFilter internal constructor(
    val type: DexKitBridge.() -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.CHECK_CAST, location) {

    /**
     * Store the lambda value instead of calling it more than once.
     */
    private val resolvedType = FilterValue(type)

    context(bridge: DexKitBridge)
    private val typeValue: String get() = resolvedType.get()

    context(bridge: DexKitBridge)
    val comparison: StringComparisonType get() = StringComparisonType.typeDeclarationToComparison(typeValue)

    context(bridge: DexKitBridge)
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
@OverloadResolutionByLambdaReturnType
fun checkCast(
    type: DexKitBridge.() -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)

@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun checkCast(
    type: String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type }, location)

@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun checkCast(
    type: DexKitBridge.() -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)

/** Resolves a class in the current bridge when matching, without capturing an APK at declaration time. */
@OverloadResolutionByLambdaReturnType
@JvmName("newInstanceOfClass")
fun newInstance(
    type: DexKitBridge.() -> ClassData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter({ type().descriptor }, location)

/** Resolves a class in the current bridge when matching, without capturing an APK at declaration time. */
@OverloadResolutionByLambdaReturnType
@JvmName("instanceOfOfClass")
fun instanceOf(
    type: DexKitBridge.() -> ClassData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter({ type().descriptor }, location)

/** Resolves a class in the current bridge when matching, without capturing an APK at declaration time. */
@OverloadResolutionByLambdaReturnType
@JvmName("checkCastOfClass")
fun checkCast(
    type: DexKitBridge.() -> ClassData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type().descriptor }, location)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    reference: DexKitBridge.() -> MethodData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    {
        val method = reference()
        MethodCallFilter.Values(
            method.declaredClass!!.descriptor,
            method.name,
            method.paramTypes.map { it.descriptor },
            method.returnType!!.descriptor,
        )
    },
    null,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass().toString(), name, parameters, returnType) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: DexKitBridge.() -> List<CharSequence>,
    returnType: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass, name, parameters().map { it.toString() }, returnType) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: DexKitBridge.() -> CharSequence,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass, name, parameters, returnType().toString()) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass().toString(), name, parameters, returnType) },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: DexKitBridge.() -> List<CharSequence>,
    returnType: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass, name, parameters().map { it.toString() }, returnType) },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: DexKitBridge.() -> CharSequence,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = MethodCallFilter(
    { MethodCallFilter.Values(definingClass, name, parameters, returnType().toString()) },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    reference: DexKitBridge.() -> FieldData,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    {
        val field = reference()
        FieldAccessFilter.Values(field.declaredClass.descriptor, field.name, field.typeSign)
    },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    type: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass().toString(), name, type) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: DexKitBridge.() -> CharSequence,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass, name, type().toString()) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    type: DexKitBridge.() -> CharSequence,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass().toString(), name, type().toString()) },
    opcodes,
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    reference: DexKitBridge.() -> FieldData,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    {
        val field = reference()
        FieldAccessFilter.Values(field.declaredClass.descriptor, field.name, field.typeSign)
    },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    type: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass().toString(), name, type) },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: DexKitBridge.() -> CharSequence,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass, name, type().toString()) },
    listOf(opcode),
    location,
)

/**
 * Resolves dynamic reference/type values once per bridge when querying or matching.
 * Type declarations and instruction locations follow the fixed-value overload.
 */
@OverloadResolutionByLambdaReturnType
fun fieldAccess(
    definingClass: DexKitBridge.() -> CharSequence,
    name: String? = null,
    type: DexKitBridge.() -> CharSequence,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = FieldAccessFilter(
    { FieldAccessFilter.Values(definingClass().toString(), name, type().toString()) },
    listOf(opcode),
    location,
)

/** Exact field reference with any field opcode. */
fun fieldAccess(
    reference: FieldData,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = FieldAccessFilter(reference.declaredClass.descriptor, reference.name, reference.typeSign, null, location)
