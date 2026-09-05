package io.github.nexalloy.morphe

import io.github.nexalloy.FindClassFunc
import io.github.nexalloy.FindFieldFunc
import io.github.nexalloy.FindMethodFunc
import io.github.nexalloy.FindMethodListFunc
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.util.DexSignUtil.getTypeName

fun getTypeNameCompat(it: String): String? {
    return when {
        it == "this" -> null
        it.isNotEmpty() && it.all { char -> char == '[' } -> null
        it.trimStart('[').startsWith('L') && !it.endsWith(';') -> null
        else -> runCatching { getTypeName(it) }.getOrNull()
    }
}

internal fun methodTypeDeclarationsMatch(
    targetDefiningClass: CharSequence,
    targetReturnType: CharSequence,
    targetParameters: Iterable<CharSequence>,
    fingerprintDefiningClass: CharSequence?,
    fingerprintReturnType: CharSequence?,
    fingerprintParameters: Iterable<CharSequence>?,
): Boolean {
    if (fingerprintDefiningClass != null &&
        !StringComparisonType.typeDeclarationToComparison(fingerprintDefiningClass)
            .compare(targetDefiningClass, fingerprintDefiningClass)
    ) {
        return false
    }

    if (fingerprintReturnType != null &&
        !StringComparisonType.typeDeclarationToComparison(fingerprintReturnType)
            .compare(targetReturnType, fingerprintReturnType)
    ) {
        return false
    }

    if (fingerprintParameters != null &&
        !parametersMatch(targetParameters, fingerprintParameters)
    ) {
        return false
    }

    return true
}

enum class AccessFlags(val modifier: Int) {
    PUBLIC(0x1),
    PRIVATE(0x2),
    PROTECTED(0x4),
    STATIC(0x8),
    FINAL(0x10),
    SYNCHRONIZED(0x20),
    VOLATILE(0x40),
    BRIDGE(0x40),
    TRANSIENT(0x80),
    VARARGS(0x80),
    NATIVE(0x100),
    INTERFACE(0x200),
    ABSTRACT(0x400),
    STRICTFP(0x800),
    SYNTHETIC(0x1000),
    ANNOTATION(0x2000),
    ENUM(0x4000),
    CONSTRUCTOR(0x10000),
    DECLARED_SYNCHRONIZED(0x20000);
}

fun MethodMatcher.strings(vararg strings: String) = this.usingStrings(strings.toList())

fun MethodMatcher.opcodes(vararg opcodes: Opcode): OpCodesMatcher {
    return OpCodesMatcher(opcodes.map { it.opCode }).also {
        this.opCodes(it)
    }
}

fun MethodMatcher.opcodes(opcodes: Collection<Opcode>): OpCodesMatcher {
    return OpCodesMatcher(opcodes.map { it.opCode }).also {
        this.opCodes(it)
    }
}

fun MethodMatcher.accessFlags(vararg accessFlags: AccessFlags) {
    val modifiers = accessFlags.fold(0) { acc, next -> acc or next.modifier }
    if (modifiers != 0) this.modifiers(modifiers)
    if (accessFlags.contains(AccessFlags.CONSTRUCTOR)) {
        if (accessFlags.contains(AccessFlags.STATIC)) this.name = "<clinit>"
        else this.name = "<init>"
    }
}

fun MethodMatcher.parameters(vararg parameters: String) {
    this.paramTypes(parameters.map(::getTypeNameCompat))
}

fun MethodMatcher.parameters(parameters: Collection<String>) {
    this.paramTypes(parameters.map(::getTypeNameCompat))
}

fun MethodMatcher.returns(returnType: String) {
    getTypeNameCompat(returnType)?.let { this.returnType = it }
}

fun MethodMatcher.literal(literalSupplier: () -> Number) {
    this.usingNumbers(literalSupplier())
}

private fun findLongestOpcodeSequence(filters: List<InstructionFilter>): List<InstructionFilter> {
    if (filters.isEmpty()) return emptyList()

    val chunks = mutableListOf<MutableList<InstructionFilter>>()

    for (filter in filters) {
        if (filter.location is InstructionLocation.MatchAfterImmediately) {
            if (chunks.isNotEmpty()) {
                chunks.last().add(filter)
            } else {
                chunks.add(mutableListOf(filter))
            }
        } else {
            chunks.add(mutableListOf(filter))
        }
    }

    return chunks.filter { it.all { it is OpcodeFilter } }.maxByOrNull { it.size }
        ?: emptyList()
}

class FingerprintDsl(init: FingerprintDsl.() -> Unit) {
    private var name: String? = null
    private var definingClass: String? = null
    private var accessFlags: List<AccessFlags>? = null
    private var returnType: String? = null
    private var parameters: List<String>? = null
    private var strings: Array<out String>? = null
    private var classFinder: FindClassFunc? = null
    private var classMatcherBlock: (ClassMatcher.() -> Unit)? = null
    private val methodMatcherBlocks = mutableListOf<MethodMatcher.() -> Unit>()

    init {
        init(this)
    }

    fun name(name: String) {
        this.name = name
    }

    fun definingClass(descriptor: String) {
        this.definingClass = descriptor
    }

    fun strings(vararg strings: String) {
        this.strings = strings
    }

    fun opcodes(vararg opcodes: Opcode): OpCodesMatcher {
        val matcher = OpCodesMatcher(opcodes.map { it.opCode })
        methodMatcherBlocks += { opCodes(matcher) }
        return matcher
    }

    fun accessFlags(vararg accessFlags: AccessFlags) {
        this.accessFlags = accessFlags.toList()
    }

    fun parameters(vararg parameters: String) {
        this.parameters = parameters.toList()
    }

    fun returns(returnType: String) {
        this.returnType = returnType
    }

    fun literal(literalSupplier: () -> Number) {
        methodMatcherBlocks += { literal(literalSupplier) }
    }

    @JvmName("classFingerprint2")
    fun classFingerprint(findClassFunc: FindClassFunc) {
        classFinder = findClassFunc
    }

    fun classFingerprint(findMethodFunc: FindMethodFunc) {
        classFinder = { findMethodFunc().declaredClass!! }
    }

    /**
     * Direct access to DexKit MethodMatcher for custom queries.
     */
    fun methodMatcher(block: MethodMatcher.() -> Unit) {
        methodMatcherBlocks += block
    }

    /**
     * DexKit ClassMatcher for chain-matching (find class first, then find method within).
     */
    fun classMatcher(block: ClassMatcher.() -> Unit) {
        classMatcherBlock = block
    }

    fun build(): Fingerprint {
        methodMatcherBlocks += { strings?.let { strings(*it) } }

        val fp = Fingerprint(
            definingClass = definingClass,
            name = name,
            accessFlags = accessFlags,
            returnType = returnType,
            parameters = parameters,
        )

        if (classFinder != null) {
            fp.classFinder = classFinder
        }
        if (classMatcherBlock != null) {
            fp.classMatcherBlock = classMatcherBlock
        }
        if (methodMatcherBlocks.isNotEmpty()) {
            fp.extraMethodMatcherBlocks = methodMatcherBlocks.toList()
        }

        return fp
    }
}

open class Fingerprint internal constructor(
    classFingerprint: Fingerprint? = null,
    definingClass: String? = null,
    name: String? = null,
    accessFlags: List<AccessFlags>? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    val filters: List<InstructionFilter>? = null,
    strings: List<String>? = null,
    custom: (MethodMatcher.() -> Unit)? = null
) {
    internal var classFinder: FindClassFunc? = null
    internal var classMatcherBlock: (ClassMatcher.() -> Unit)? = null
    internal var extraMethodMatcherBlocks: List<MethodMatcher.() -> Unit>? = null
    private val definingClassPattern = definingClass
    private val returnTypePattern = returnType
    private val parametersPattern = parameters

    init {
        if (classFingerprint != null) {
            classFinder = { classFingerprint.run().declaredClass!! }
        }
        if (custom != null) {
            extraMethodMatcherBlocks = listOf(custom)
        }
    }

    /**
     * Set class matcher for chain matching (find class first, then method within).
     * Can be called from object init blocks.
     */
    protected fun classMatcher(block: ClassMatcher.() -> Unit) {
        classMatcherBlock = block
    }

    private val methodMatcherBuilder = fun MethodMatcher.(): Unit {
        definingClass?.let(::getTypeNameCompat)?.let { declaredClass(it) }
        if (name != null) name(name)
        if (accessFlags != null) accessFlags(*accessFlags.toTypedArray())
        if (returnType != null) returns(returnType)
        if (parameters != null) parameters(*parameters.toTypedArray())
        if (strings != null) usingStrings(strings)
        filters?.forEach { filter ->
            filter.addQuery()
        }

        filters?.also {
            val opcodes = findLongestOpcodeSequence(it).map { filter ->
                when (filter) {
                    is OpcodeFilter -> filter.opcode.ordinal
                    else -> throw IllegalStateException()
                }
            }
            if (opcodes.any())
                opCodes(opcodes)
        }

        extraMethodMatcherBlocks?.forEach { block -> block() }
    }

    fun buildMethodMatcher(): MethodMatcher =
        MethodMatcher().apply(methodMatcherBuilder)

    /**
     * A fingerprint for a method. A fingerprint is a partial description of a method,
     * used to uniquely match a method by its characteristics.
     *
     * See the patcher documentation for more detailed explanations and example fingerprinting.
     *
     * @param classFingerprint Fingerprint that finds the class this fingerprint resolves against.
     * @param name Exact method name.
     * @param accessFlags The exact access flags using values of [AccessFlags].
     * @param returnType The return type. Type declaration follow the semantics described in [StringComparisonType].
     * @param parameters The parameters. Type declaration follow the semantics described in [StringComparisonType].
     * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
     * @param strings A list of strings that appear anywhere in the method in any order. Compared using [String.contains].
     * @param custom A custom condition for this fingerprint.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint, null, name, accessFlags, returnType, parameters, filters, strings, custom
    )

    /**
     * A fingerprint for a method. A fingerprint is a partial description of a method,
     * used to uniquely match a method by its characteristics.
     *
     * See the patcher documentation for more detailed explanations and example fingerprinting.
     *
     * @param name Exact method name.
     * @param accessFlags The exact access flags using values of [AccessFlags].
     * @param returnType The return type. Type declaration follow the semantics described in [StringComparisonType].
     * @param parameters The parameters. Type declaration follow the semantics described in [StringComparisonType].
     * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
     * @param strings A list of strings that appear anywhere in the method in any order. Compared using [String.contains].
     * @param custom A custom condition for this fingerprint.
     */
    constructor(
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        null, null, name, accessFlags, returnType, parameters, filters, strings, custom
    )

    /**
     * A fingerprint for a method. A fingerprint is a partial description of a method,
     * used to uniquely match a method by its characteristics.
     *
     * See the patcher documentation for more detailed explanations and example fingerprinting.
     *
     * @param definingClass Defining class. Type declaration follow the semantics described in [StringComparisonType].
     * @param name Exact method name.
     * @param accessFlags The exact access flags using values of [AccessFlags].
     * @param returnType The return type. Type declaration follow the semantics described in [StringComparisonType].
     * @param parameters The parameters. Type declaration follow the semantics described in [StringComparisonType].
     * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
     * @param strings A list of strings that appear anywhere in the method in any order. Compared using [String.contains].
     * @param custom A custom condition for this fingerprint.
     */
    constructor(
        definingClass: String? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        null, definingClass, name, accessFlags, returnType, parameters, filters, strings, custom
    )

    context(dexkit: DexKitBridge)
    operator fun invoke(): MethodData {
        return run()
    }

    private fun matchesTypeDeclarations(method: MethodData): Boolean {
        val targetDefiningClass = method.declaredClass?.descriptor ?: return false
        val targetReturnType = method.returnType?.descriptor ?: return false
        val targetParameters = method.paramTypes.map { it.descriptor }

        return methodTypeDeclarationsMatch(
            targetDefiningClass = targetDefiningClass,
            targetReturnType = targetReturnType,
            targetParameters = targetParameters,
            fingerprintDefiningClass = definingClassPattern,
            fingerprintReturnType = returnTypePattern,
            fingerprintParameters = parametersPattern,
        )
    }

    context(dexkit: DexKitBridge)
    fun run(): MethodData {
        val methodMatcher = buildMethodMatcher()

        val candidates = if (classMatcherBlock != null) {
            dexkit.findClass {
                matcher(ClassMatcher().apply(classMatcherBlock!!))
            }.findMethod {
                matcher(methodMatcher)
            }
        } else if (classFinder != null) {
            classFinder!!.invoke(dexkit).findMethod {
                matcher(methodMatcher)
            }
        } else {
            dexkit.findMethod {
                matcher(methodMatcher)
            }
        }
        val results = candidates.filter {
            matchesTypeDeclarations(it) && matchOrNull(it) != null
        }
        if (results.size != 1) {
            val name = this::class.simpleName ?: "Anonymous Fingerprint"
            val list = results.joinToString("\n  ") { it.descriptor }
            System.err.println("$name matched ${results.size} methods:\n  $list")
        }
        return results.single()
    }

    context(dexkit: DexKitBridge)
    fun match() = matchOrNull(run()) ?: throw Exception("getInstructionMatches failed")

    context(dexkit: DexKitBridge)
    fun matchOrNull() = matchOrNull(run())

    fun matchOrNull(
        method: MethodData
    ): Match? {
        val filtersLocal = filters
        val instructionMatches = when {
            filtersLocal == null -> null
            filtersLocal.isEmpty() -> emptyList()
            else -> {
                val instructions = method.instructions.toList()

                fun matchFilters(): List<Match.InstructionMatch>? {
                    val lastMethodIndex = instructions.lastIndex
                    var instructionMatches: MutableList<Match.InstructionMatch>? = null

                    var firstInstructionIndex = 0
                    var lastMatchIndex = -1

                    firstFilterLoop@ while (true) {
                        var firstFilterIndex = -1
                        var subIndex = firstInstructionIndex

                        for (filterIndex in filtersLocal.indices) {
                            val filter = filtersLocal[filterIndex]
                            val location = filter.location
                            var instructionsMatched = false

                            while (subIndex <= lastMethodIndex &&
                                location.indexIsValidForMatching(
                                    lastMatchIndex, subIndex
                                )
                            ) {
                                val instruction = instructions[subIndex]
                                if (filter.matches(method, instruction)) {
                                    lastMatchIndex = subIndex

                                    if (filterIndex == 0) {
                                        firstFilterIndex = subIndex
                                    }
                                    if (instructionMatches == null) {
                                        instructionMatches = ArrayList(filtersLocal.size)
                                    }
                                    instructionMatches += Match.InstructionMatch(
                                        filter,
                                        subIndex,
                                        instruction
                                    )
                                    instructionsMatched = true
                                    subIndex++
                                    break
                                }
                                subIndex++
                            }

                            if (!instructionsMatched) {
                                if (filterIndex == 0) {
                                    return null
                                }

                                firstInstructionIndex = firstFilterIndex + 1
                                lastMatchIndex = -1
                                instructionMatches?.clear()
                                continue@firstFilterLoop
                            }
                        }

                        return instructionMatches
                    }
                }

                matchFilters() ?: return null
            }
        }

        return Match(
            method,
            instructionMatches
        )
    }

    context(_: DexKitBridge)
    val instructionMatches
        get() = match().instructionMatches
}

class Match constructor(
    val method: MethodData,
    private val _instructionMatches: List<InstructionMatch>?,
) {

    /**
     * Matches corresponding to the [InstructionFilter] declared in the [Fingerprint].
     */
    val instructionMatches
        get() = _instructionMatches
            ?: throw Exception("Fingerprint declared no instruction filters")
    val instructionMatchesOrNull = _instructionMatches

    /**
     * A match for the [InstructionFilter].
     * @param filter The filter that matched
     * @param index The instruction index it matched with.
     * @param instruction The instruction that matched.
     */
    class InstructionMatch internal constructor(
        val filter: InstructionFilter,
        val index: Int,
        val instruction: InstructionData
    ) {
        /**
         * Helper method to simplify casting the instruction to it's known and expected type.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> getInstruction(): T = instruction as T

        override fun toString(): String {
            return "InstructionMatch{filter='${filter.javaClass.simpleName}, opcode='${instruction.opcode}, 'index=$index}"
        }
    }

    override fun toString(): String {
        return "Match(originalMethod=$method, " +
                "instructionMatches=$_instructionMatches)"
    }
}

fun DexKitBridge.fingerprint(block: FingerprintDsl.() -> Unit): MethodData {
    return FingerprintDsl(block).build().run()
}

fun fingerprint(block: FingerprintDsl.() -> Unit): FindMethodFunc {
    return { FingerprintDsl(block).build().run() }
}

fun findMethodDirect(block: FindMethodFunc) = block
fun findMethodListDirect(block: FindMethodListFunc) = block
fun findClassDirect(block: FindClassFunc) = block
fun findFieldDirect(block: FindFieldFunc) = block
