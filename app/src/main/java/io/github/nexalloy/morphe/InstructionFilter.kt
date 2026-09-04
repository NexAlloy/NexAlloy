package io.github.nexalloy.morphe

import io.github.nexalloy.morphe.FieldAccessFilter.Companion.parseJvmFieldAccess
import io.github.nexalloy.morphe.MethodCallFilter.Companion.parseJvmMethodCall
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData
import java.util.EnumSet

fun interface InstructionLocation {
    fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int): Boolean

    class MatchAfterAnywhere : InstructionLocation {
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) = true
    }

    class MatchFirst : InstructionLocation {
        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            require(previouslyMatchedIndex < 0) {
                "MatchFirst can only be used for the first instruction filter"
            }
            return currentIndex == 0
        }
    }

    class MatchAfterImmediately : InstructionLocation {
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
            require(previouslyMatchedIndex >= 0) {
                "MatchAfterWithin cannot be used for the first instruction filter"
            }
            val distance = currentIndex - previouslyMatchedIndex - 1
            return distance in 0..matchDistance
        }
    }

    class MatchAfterAtLeast(val minimumDistanceFromLastInstruction: Int) : InstructionLocation {
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

    class MatchAfterRange(
        val minimumDistanceFromLastInstruction: Int,
        val maximumDistanceFromLastInstruction: Int
    ) : InstructionLocation {
        init {
            require(minimumDistanceFromLastInstruction >= 0) {
                "minimumDistanceFromLastInstruction must be non-negative"
            }
            require(minimumDistanceFromLastInstruction <= maximumDistanceFromLastInstruction) {
                "minimumDistanceFromLastInstruction must be <= maximumDistanceFromLastInstruction"
            }
        }

        override fun indexIsValidForMatching(
            previouslyMatchedIndex: Int,
            currentIndex: Int
        ): Boolean {
            val distance = currentIndex - previouslyMatchedIndex - 1
            return distance in minimumDistanceFromLastInstruction..maximumDistanceFromLastInstruction
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
    ): Boolean = filters.any { it.matches(enclosingMethod, instruction) }
}

fun anyInstruction(
    vararg filters: InstructionFilter,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = AnyInstruction(filters.asList(), location)

open class OpcodeFilter(
    val opcode: Opcode,
    override val location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : InstructionFilter {
    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean = Opcode.fromInt(instruction.opcode) == opcode
}

fun opcode(
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = OpcodeFilter(opcode, location)

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
        val opcodesLocal = opcodes ?: return true
        return opcodesLocal.contains(Opcode.fromInt(instruction.opcode))
    }

    companion object {
        fun opcodesToFilters(vararg opcodes: Opcode?): List<InstructionFilter> {
            val list = ArrayList<InstructionFilter>(opcodes.size)
            var location: InstructionLocation? = null

            opcodes.forEach { opcode ->
                val opcodeLocation = location ?: InstructionLocation.MatchAfterAnywhere()
                list += if (opcode == null) {
                    OpcodesFilter(null as List<Opcode>?, opcodeLocation)
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
    private val literalValue: Long by lazy(literal)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        return instruction.literal == literalValue
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        matcher.addUsingNumber(literalValue)
    }
}

fun literal(
    literal: Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal }, opcodes, location)

fun literal(
    literal: Int,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toLong() }, opcodes, location)

fun literal(
    literal: Double,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits() }, opcodes, location)

fun literal(
    literal: Float,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits().toLong() }, opcodes, location)

fun literal(
    literal: () -> Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter(literal, opcodes, location)

private const val MAX_ARRAY_DIMENSIONS = 255
private val primitiveDescriptorTypes = "BCDFIJSZ"
private val invalidUnqualifiedNameChars = charArrayOf('.', ';', '[', '/')

private fun isValidUnqualifiedName(name: String): Boolean =
    name.isNotEmpty() && invalidUnqualifiedNameChars.none(name::contains)

private fun isValidMethodName(name: String): Boolean {
    if (name == "<init>" || name == "<clinit>") return true
    return isValidUnqualifiedName(name) && '<' !in name && '>' !in name
}

private fun isValidInternalClassName(name: String): Boolean =
    name.isNotEmpty() && name.split('/').all(::isValidUnqualifiedName)

private fun isValidObjectDescriptor(descriptor: String): Boolean =
    descriptor.length >= 3 && descriptor.first() == 'L' && descriptor.last() == ';' &&
        isValidInternalClassName(descriptor.substring(1, descriptor.lastIndex))

private fun isValidFieldDescriptor(descriptor: String): Boolean {
    if (descriptor.length == 1) return descriptor[0] in primitiveDescriptorTypes
    if (isValidObjectDescriptor(descriptor)) return true
    if (!descriptor.startsWith('[')) return false

    val dimensions = descriptor.indexOfFirst { it != '[' }.let { if (it < 0) descriptor.length else it }
    if (dimensions !in 1..MAX_ARRAY_DIMENSIONS || dimensions >= descriptor.length) return false
    val component = descriptor.substring(dimensions)
    return component.length == 1 && component[0] in primitiveDescriptorTypes ||
        isValidObjectDescriptor(component)
}

private fun parameterSlotCount(descriptor: String): Int {
    if (descriptor.startsWith('[') || descriptor.startsWith('L')) return 1
    return if (descriptor == "J" || descriptor == "D") 2 else 1
}

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
        if (!super.matches(enclosingMethod, instruction)) return false
        val reference = instruction.methodRef ?: return false

        val nameLocal = name
        if (nameLocal != null && reference.name != nameLocal) return false

        val definingClassLocal = definingClass
        if (definingClassLocal != null) {
            val referenceClass = reference.declaredClass!!.descriptor
            if (definingClassLocal == "this") {
                if (referenceClass != enclosingMethod.declaredClass!!.descriptor) return false
            } else if (!definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val returnTypeLocal = returnType
        if (returnTypeLocal != null && !returnTypeComparison.compare(
                reference.returnType!!.descriptor,
                returnTypeLocal
            )
        ) return false

        val parametersLocal = parameters
        if (parametersLocal != null && !parametersMatch(
                reference.paramTypes.map { it.descriptor },
                parametersLocal,
                parameterTypeComparison
            )
        ) return false

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
        private val regex = Regex(
            """^(L[^;]+;)->([^(\s]+)\(([^)]*)\)(V|[BCDFIJSZ]|L[^;]+;|\[+(?:[BCDFIJSZ]|L[^;]+;))${'$'}"""
        )

        internal fun parseJvmMethodCall(
            methodSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): MethodCallFilter {
            val matchResult = regex.matchEntire(methodSignature)
                ?: throw IllegalArgumentException("Invalid method signature: $methodSignature")

            val classDescriptor = matchResult.groupValues[1]
            val methodName = matchResult.groupValues[2]
            val returnDescriptor = matchResult.groupValues[4]
            require(isValidObjectDescriptor(classDescriptor)) {
                "Invalid defining class descriptor: $classDescriptor"
            }
            require(isValidMethodName(methodName)) {
                "Invalid method name: $methodName"
            }
            require(returnDescriptor == "V" || isValidFieldDescriptor(returnDescriptor)) {
                "Invalid return descriptor: $returnDescriptor"
            }

            val paramDescriptors = parseParameterDescriptors(matchResult.groupValues[3])
            when (methodName) {
                "<init>" -> require(returnDescriptor == "V") {
                    "Constructor must return V: $methodSignature"
                }
                "<clinit>" -> require(paramDescriptors.isEmpty() && returnDescriptor == "V") {
                    "Class initializer must use ()V: $methodSignature"
                }
            }

            val parameterSlots = paramDescriptors.sumOf(::parameterSlotCount)
            val maximumParameterSlots = if (methodName == "<init>") 254 else 255
            require(parameterSlots <= maximumParameterSlots) {
                "Method parameter descriptors exceed $maximumParameterSlots slots: $methodSignature"
            }

            return MethodCallFilter(
                classDescriptor,
                methodName,
                paramDescriptors,
                returnDescriptor,
                opcodes,
                location
            )
        }

        private fun parseSingleType(params: String, startIndex: Int): Pair<String, Int> {
            var i = startIndex
            while (i < params.length && params[i] == '[') i++
            require(i < params.length) { "Malformed type descriptor: $params" }

            val dimensions = i - startIndex
            require(dimensions <= MAX_ARRAY_DIMENSIONS) {
                "Array descriptor exceeds $MAX_ARRAY_DIMENSIONS dimensions: $params"
            }

            return if (params[i] == 'L') {
                val semicolonPos = params.indexOf(';', i)
                require(semicolonPos > i + 1) {
                    "Malformed object descriptor: $params"
                }
                val componentDescriptor = params.substring(i, semicolonPos + 1)
                require(isValidObjectDescriptor(componentDescriptor)) {
                    "Invalid object descriptor: $componentDescriptor"
                }
                params.substring(startIndex, semicolonPos + 1) to (semicolonPos + 1)
            } else {
                require(params[i] in primitiveDescriptorTypes) {
                    "Invalid parameter descriptor: $params"
                }
                params.substring(startIndex, i + 1) to (i + 1)
            }
        }

        private fun parseParameterDescriptors(paramString: String): List<String> {
            val result = mutableListOf<String>()
            var currentIndex = 0
            while (currentIndex < paramString.length) {
                val (type, nextIndex) = parseSingleType(paramString, currentIndex)
                result.add(type)
                currentIndex = nextIndex
            }
            return result
        }
    }
}

fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(definingClass, name, parameters, returnType, opcodes, location)

fun methodCall(
    definingClass: String? = null,
    name: String? = null,
    parameters: List<String>? = null,
    returnType: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(definingClass, name, parameters, returnType, listOf(opcode), location)

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

fun methodCall(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmMethodCall(smali, opcodes, location)

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
        if (!super.matches(enclosingMethod, instruction)) return false
        val reference = instruction.fieldRef ?: return false

        val nameLocal = name
        if (nameLocal != null && reference.name != nameLocal) return false

        val definingClassLocal = definingClass
        if (definingClassLocal != null) {
            val referenceClass = reference.declaredClass.descriptor
            if (definingClassLocal == "this") {
                if (referenceClass != enclosingMethod.declaredClass!!.descriptor) return false
            } else if (!definingClassComparison.compare(referenceClass, definingClassLocal)) {
                return false
            }
        }

        val typeLocal = type
        if (typeLocal != null && !typeComparison.compare(reference.typeSign, typeLocal)) return false
        return true
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        val declaredClassName = this@FieldAccessFilter.definingClass?.let(::getTypeNameCompat)
        (declaredClassName ?: name ?: type)?.let {
            matcher.addUsingField {
                declaredClassName?.let { declaredClass(it) }
                this@FieldAccessFilter.name?.let { name(it) }
                this@FieldAccessFilter.type?.let(::getTypeNameCompat)?.let { type(it) }
            }
        }
    }

    internal companion object {
        private val regex = Regex(
            """^(L[^;]+;)->([^:]+):([BCDFIJSZ]|L[^;]+;|\[+(?:[BCDFIJSZ]|L[^;]+;))${'$'}"""
        )

        internal fun parseJvmFieldAccess(
            fieldSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): FieldAccessFilter {
            val matchResult = regex.matchEntire(fieldSignature)
                ?: throw IllegalArgumentException("Invalid field access smali: $fieldSignature")

            val classDescriptor = matchResult.groupValues[1]
            val fieldName = matchResult.groupValues[2]
            val fieldType = matchResult.groupValues[3]
            require(isValidObjectDescriptor(classDescriptor)) {
                "Invalid defining class descriptor: $classDescriptor"
            }
            require(isValidUnqualifiedName(fieldName)) {
                "Invalid field name: $fieldName"
            }
            require(isValidFieldDescriptor(fieldType)) {
                "Invalid field descriptor: $fieldType"
            }

            return fieldAccess(
                definingClass = classDescriptor,
                name = fieldName,
                type = fieldType,
                opcodes = opcodes,
                location = location
            )
        }
    }
}

fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: String? = null,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = fieldAccess(definingClass, name, type, listOf(opcode), location)

fun fieldAccess(
    definingClass: String? = null,
    name: String? = null,
    type: String? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = FieldAccessFilter(definingClass, name, type, opcodes, location)

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

fun fieldAccess(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmFieldAccess(smali, opcodes, location)

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
    internal val stringValue: String by lazy(string)

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        return comparison.compare(instruction.string!!, stringValue)
    }

    context(matcher: MethodMatcher)
    override fun addQuery() {
        matcher.addUsingString(stringValue, comparison.value)
    }
}

fun string(
    string: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter({ string }, StringComparisonType.EQUALS, location)

fun string(
    string: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, StringComparisonType.EQUALS, location)

fun string(
    string: String,
    comparison: StringComparisonType = StringComparisonType.EQUALS,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter({ string }, comparison, location)

fun string(
    string: () -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, comparison, location)

class NewInstanceFilter internal constructor(
    val type: () -> String,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.NEW_INSTANCE, Opcode.NEW_ARRAY), location) {
    private val typeValue: String by lazy(type)
    val comparison by lazy { StringComparisonType.typeDeclarationToComparison(typeValue) }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        return comparison.compare(instruction.classRef!!.descriptor, typeValue)
    }
}

fun newInstance(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter({ type }, location)

fun newInstance(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = NewInstanceFilter(type, location)

@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun newInstance(
    type: String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = NewInstanceFilter({ type }, location)

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
    private val typeValue: String by lazy(type)
    val comparison by lazy { StringComparisonType.typeDeclarationToComparison(typeValue) }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        return comparison.compare(instruction.classRef!!.descriptor, typeValue)
    }
}

fun instanceOf(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter({ type }, location)

fun instanceOf(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter(type, location)

@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun instanceOf(
    type: String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter({ type }, location)

@Deprecated("Instead use non comparison constructor where comparison is based on the type declaration")
fun instanceOf(
    type: () -> String,
    comparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = InstanceOfFilter(type, location)

class CheckCastFilter internal constructor(
    val type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.CHECK_CAST, location) {
    private val typeValue: String by lazy(type)
    val comparison by lazy { StringComparisonType.typeDeclarationToComparison(typeValue) }

    override fun matches(
        enclosingMethod: MethodData,
        instruction: InstructionData
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        return comparison.compare(instruction.classRef!!.descriptor, typeValue)
    }
}

fun checkCast(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type }, location)

fun checkCast(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, location)
