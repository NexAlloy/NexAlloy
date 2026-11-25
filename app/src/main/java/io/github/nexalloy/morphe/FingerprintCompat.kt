package io.github.nexalloy.morphe

import io.github.nexalloy.FindClassFunc
import io.github.nexalloy.FindFieldFunc
import io.github.nexalloy.FindFieldListFunc
import io.github.nexalloy.FindMethodFunc
import io.github.nexalloy.FindMethodListFunc
import java.util.BitSet
import org.luckypray.dexkit.DexAccessFlags
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.result.InstructionData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.util.DexSignUtil.getTypeName

fun getTypeNameCompat(it: String): String? {
    val component = it.trimStart('[')
    val isPrimitive = component.length == 1 && component[0] in "BCDFIJSVZ"
    val isClass = component.startsWith('L') && component.endsWith(';')
    // DexKit's query accepts complete types. Partial descriptors are checked after querying.
    return if (isPrimitive || isClass) getTypeName(it) else null
}

enum class AccessFlags(val modifier: Int) {
    PUBLIC(DexAccessFlags.PUBLIC),
    PRIVATE(DexAccessFlags.PRIVATE),
    PROTECTED(DexAccessFlags.PROTECTED),
    STATIC(DexAccessFlags.STATIC),
    FINAL(DexAccessFlags.FINAL),
    SYNCHRONIZED(DexAccessFlags.SYNCHRONIZED),
    SUPER(DexAccessFlags.SUPER),
    VOLATILE(DexAccessFlags.VOLATILE),
    BRIDGE(DexAccessFlags.BRIDGE),
    TRANSIENT(DexAccessFlags.TRANSIENT),
    VARARGS(DexAccessFlags.VARARGS),
    NATIVE(DexAccessFlags.NATIVE),
    INTERFACE(DexAccessFlags.INTERFACE),
    ABSTRACT(DexAccessFlags.ABSTRACT),
    STRICTFP(DexAccessFlags.STRICT),
    SYNTHETIC(DexAccessFlags.SYNTHETIC),
    ANNOTATION(DexAccessFlags.ANNOTATION),
    ENUM(DexAccessFlags.ENUM),
    CONSTRUCTOR(DexAccessFlags.CONSTRUCTOR),
    DECLARED_SYNCHRONIZED(DexAccessFlags.DECLARED_SYNCHRONIZED),
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
    val modifiers = accessFlags.map { it.modifier }.reduce { acc, next -> acc or next }
    if (modifiers != 0) this.accessFlags(modifiers)
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
        methodMatcherBlocks += {
            strings?.let { strings(*it) }
            // Keep the legacy DSL's contains-flags query semantics.
            this@FingerprintDsl.accessFlags?.let { accessFlags(*it.toTypedArray()) }
        }

        val fp = Fingerprint(
            definingClass = definingClass,
            name = name,
            returnType = returnType,
            parameters = parameters,
        )

        // Apply classFinder or classMatcher
        if (classFinder != null) {
            fp.legacyClassFinder = classFinder
        }
        if (classMatcherBlock != null) {
            fp.classMatcherBlock = classMatcherBlock
        }

        // Apply extra methodMatcher blocks
        if (methodMatcherBlocks.isNotEmpty()) {
            fp.extraMethodMatcherBlocks = methodMatcherBlocks.toList()
        }

        return fp
    }
}

/**
 * DexKit Query is not equivalent to instruction matching. <br>
 * This annotation forces the query results to be filtered again using instruction matching.
 * @see Fingerprint.run
 * */
annotation class StrictQuery

open class Fingerprint private constructor(
    val classFingerprint: Fingerprint? = null,
    private val declaredDefiningClass: (DexKitBridge.() -> CharSequence)? = null,
    private val declaredSuperclass: (DexKitBridge.() -> CharSequence)? = null,
    private val declaredInterfaces: (DexKitBridge.() -> List<CharSequence>)? = null,
    private val name: String? = null,
    private val accessFlags: List<AccessFlags>? = null,
    private val isStatic: Boolean? = null,
    private val declaredReturnType: (DexKitBridge.() -> CharSequence)? = null,
    private val declaredParameters: (DexKitBridge.() -> List<CharSequence>)? = null,
    val filters: List<InstructionFilter>? = null,
    val stringFilters: List<StringFilter>? = null,
    val custom: (MethodMatcher.() -> Unit)? = null,
) {
    internal var legacyClassFinder: FindClassFunc? = null
    internal var classMatcherBlock: (ClassMatcher.() -> Unit)? = null
    internal var extraMethodMatcherBlocks: List<MethodMatcher.() -> Unit>? = null

    context(bridge: DexKitBridge)
    val strings: List<String>?
        get() = stringFilters?.map { it.stringValue }

    init {
        require(accessFlags == null || isStatic == null) {
            "Cannot specify both accessFlags and isStatic"
        }
        require(declaredDefiningClass == null || classFingerprint == null) {
            "Cannot specify both definingClass and classFingerprint"
        }
        require(filters == null || filters.isNotEmpty()) { "Filters cannot be empty" }
        stringFilters?.forEach {
            require(it.location is InstructionLocation.MatchAfterAnywhere) {
                "Strings match in any order and cannot declare an instruction location"
            }
        }
        if (custom != null)
            extraMethodMatcherBlocks = listOf(custom)
    }

    /**
     * Set class matcher for chain matching (find class first, then method within).
     * Can be called from object init blocks.
     */
    protected fun classMatcher(block: ClassMatcher.() -> Unit) {
        classMatcherBlock = block
    }

    private class Values(
        val definingClass: String?,
        val superclass: String?,
        val interfaces: List<String>?,
        val returnType: String?,
        val parameters: List<String>?,
    ) {
        init {
            require(interfaces == null || interfaces.isNotEmpty()) { "Interfaces cannot be empty" }
        }
    }

    private fun evaluateValues(bridge: DexKitBridge) = Values(
        declaredDefiningClass?.invoke(bridge)?.toString(),
        declaredSuperclass?.invoke(bridge)?.toString(),
        declaredInterfaces?.invoke(bridge)?.map { it.toString() },
        declaredReturnType?.invoke(bridge)?.toString(),
        declaredParameters?.invoke(bridge)?.map { it.toString() },
    )

    context(bridge: DexKitBridge)
    private fun buildMethodMatcher(values: Values): MethodMatcher = MethodMatcher().apply {
        if (values.definingClass != null || values.superclass != null || values.interfaces != null) {
            declaredClass {
                values.definingClass?.let(::getTypeNameCompat)?.let { className(it) }
                values.superclass?.let(::getTypeNameCompat)?.let { superClass(it) }
                values.interfaces?.mapNotNull(::getTypeNameCompat)?.forEach { addInterface(it) }
            }
        }
        if (isStatic == true) accessFlags(DexAccessFlags.STATIC)
        this@Fingerprint.name?.let { name(it) }
        this@Fingerprint.accessFlags?.let { flags ->
            accessFlags(flags.fold(0) { mask, flag -> mask or flag.modifier }, MatchType.Equals)
        }
        values.returnType?.let { returns(it) }
        values.parameters?.let { parameters(*it.toTypedArray()) }
        stringFilters?.forEach { it.addQuery() }
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

    context(dexkit: DexKitBridge)
    fun buildMethodMatcher(): MethodMatcher =
        buildMethodMatcher(evaluateValues(dexkit))

    /**
     * A fingerprint for a method. A fingerprint is a partial description of a method,
     * used to uniquely match a method by its characteristics.
     *
     * See the patcher documentation for more detailed explanations and example fingerprinting.
     *
     * @param classFingerprint Fingerprint that finds the class this fingerprint resolves against.
     *                         Cannot be declared with [definingClass].
     * @param definingClass Defining class. Type declaration follow the semantics described in [StringComparisonType].
     *                      Cannot be declared with [classFingerprint].
     * @param superclass The direct superclass of the defining class.
     *                   Type declaration follow the semantics described in [StringComparisonType].
     * @param interfaces Interfaces the defining class directly implements, declared in any order.
     *                   The class can implement other interfaces not declared here.
     *                   Type declarations follow the semantics described in [StringComparisonType].
     * @param name Exact method name.
     * @param accessFlags The exact access flags using values of [AccessFlags].
     *                    Access flags can change between app versions. If only static or
     *                    non-static matters, instead declare [isStatic].
     * @param isStatic If the method is static. All other access flags are ignored.
     *                 Cannot be declared with [accessFlags]. To match a constructor declare
     *                 `name = "<init>"`, or for a static initializer declare `name = "<clinit>"`.
     * @param returnType The return type. Type declaration follow the semantics described in [StringComparisonType].
     * @param parameters The parameters. Type declaration follow the semantics described in [StringComparisonType].
     * @param filters A list of filters to match, declared in the order they are matched.
     *                Each filter matches relative to the previous filter match, using the
     *                [InstructionFilter.location] of the filter. Some locations, such as
     *                [InstructionLocation.MatchBeforeImmediately], match an instruction that appears
     *                before the previous filter match. If a filter cannot match, then the filters
     *                declared before it are retried with their next matching instruction.
     * @param strings A list of strings that appear anywhere in the method in any order. Compared using [String.equals].
     * @param custom A custom condition for this fingerprint.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint,
        constantOrNull(definingClass),
        constantOrNull(superclass),
        constantInterfacesOrNull(interfaces),
        name,
        accessFlags,
        isStatic,
        constantOrNull(returnType),
        constantOrNull(parameters),
        filters,
        rawStringFilters(strings),
        custom,
    )

    /**
     * A fingerprint for a method. A fingerprint is a partial description of a method,
     * used to uniquely match a method by its characteristics.
     *
     * See the patcher documentation for more detailed explanations and example fingerprinting.
     *
     * The strings parameter is a Collection and not a List because List<StringFilter>
     * has the same JVM signature as List<String>. The parameter is required and not nullable,
     * so calls that do not declare string filters always resolve to the List<String> constructor.
     *
     * @param classFingerprint Fingerprint that finds the class this fingerprint resolves against.
     *                         Cannot be declared with [definingClass].
     * @param definingClass Defining class. Type declaration follow the semantics described in [StringComparisonType].
     *                      Cannot be declared with [classFingerprint].
     * @param superclass The direct superclass of the defining class.
     *                   Type declaration follow the semantics described in [StringComparisonType].
     * @param interfaces Interfaces the defining class directly implements, declared in any order.
     *                   The class can implement other interfaces not declared here.
     *                   Type declarations follow the semantics described in [StringComparisonType].
     * @param name Exact method name.
     * @param accessFlags The exact access flags using values of [AccessFlags].
     *                    Access flags can change between app versions. If only static or
     *                    non-static matters, instead declare [isStatic].
     * @param isStatic If the method is static. All other access flags are ignored.
     *                 Cannot be declared with [accessFlags]. To match a constructor declare
     *                 `name = "<init>"`, or for a static initializer declare `name = "<clinit>"`.
     * @param returnType The return type. Type declaration follow the semantics described in [StringComparisonType].
     * @param parameters The parameters. Type declaration follow the semantics described in [StringComparisonType].
     * @param filters A list of filters to match, declared in the order they are matched.
     *                Each filter matches relative to the previous filter match, using the
     *                [InstructionFilter.location] of the filter. Some locations, such as
     *                [InstructionLocation.MatchBeforeImmediately], match an instruction that appears
     *                before the previous filter match. If a filter cannot match, then the filters
     *                declared before it are retried with their next matching instruction.
     * @param strings A list of [string] filters that match anywhere in the method in any order.
     *                Filters cannot declare an [InstructionLocation].
     * @param custom A custom condition for this fingerprint.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint,
        constantOrNull(definingClass),
        constantOrNull(superclass),
        constantInterfacesOrNull(interfaces),
        name,
        accessFlags,
        isStatic,
        constantOrNull(returnType),
        constantOrNull(parameters),
        filters,
        strings.toList(),
        custom,
    )

    /**
     * Evaluates [definingClass] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * A dynamic [definingClass] cannot be combined with [classFingerprint].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: DexKitBridge.() -> CharSequence,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = definingClass,
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [definingClass] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * A dynamic [definingClass] cannot be combined with [classFingerprint].
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: DexKitBridge.() -> CharSequence,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = definingClass,
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [superclass] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: DexKitBridge.() -> CharSequence,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = superclass,
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [superclass] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: DexKitBridge.() -> CharSequence,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = superclass,
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [interfaces] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: DexKitBridge.() -> List<CharSequence>,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [interfaces] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: DexKitBridge.() -> List<CharSequence>,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [returnType] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: DexKitBridge.() -> CharSequence,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [returnType] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: DexKitBridge.() -> CharSequence,
        parameters: List<String>? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = constantOrNull(parameters),
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: DexKitBridge.() -> List<CharSequence>,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = parameters,
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        definingClass: String? = null,
        superclass: String? = null,
        interfaces: List<String>? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: String? = null,
        parameters: DexKitBridge.() -> List<CharSequence>,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = constantOrNull(definingClass),
        declaredSuperclass = constantOrNull(superclass),
        declaredInterfaces = constantInterfacesOrNull(interfaces),
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = constantOrNull(returnType),
        declaredParameters = parameters,
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [definingClass], [superclass], [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     * A dynamic [definingClass] replaces [classFingerprint].
     */
    constructor(
        definingClass: DexKitBridge.() -> CharSequence,
        superclass: (DexKitBridge.() -> CharSequence)? = null,
        interfaces: (DexKitBridge.() -> List<CharSequence>)? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = null,
        declaredDefiningClass = definingClass,
        declaredSuperclass = superclass,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [definingClass], [superclass], [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     * A dynamic [definingClass] replaces [classFingerprint].
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        definingClass: DexKitBridge.() -> CharSequence,
        superclass: (DexKitBridge.() -> CharSequence)? = null,
        interfaces: (DexKitBridge.() -> List<CharSequence>)? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = null,
        declaredDefiningClass = definingClass,
        declaredSuperclass = superclass,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [superclass], [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        superclass: DexKitBridge.() -> CharSequence,
        interfaces: (DexKitBridge.() -> List<CharSequence>)? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = superclass,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [superclass], [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        superclass: DexKitBridge.() -> CharSequence,
        interfaces: (DexKitBridge.() -> List<CharSequence>)? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = superclass,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        interfaces: DexKitBridge.() -> List<CharSequence>,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = null,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [interfaces], [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        interfaces: DexKitBridge.() -> List<CharSequence>,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: (DexKitBridge.() -> CharSequence)? = null,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = null,
        declaredInterfaces = interfaces,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    /**
     * Evaluates [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: DexKitBridge.() -> CharSequence,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: List<String>? = null,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = null,
        declaredInterfaces = null,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = rawStringFilters(strings),
        custom = custom,
    )

    /**
     * Evaluates [returnType], [parameters] using the current [DexKitBridge] when queried.
     * Suppliers run once per uncached query, so values are never shared across bridges.
     * Type descriptors follow [StringComparisonType] semantics. Other arguments follow
     * the constructor with fixed types.
     * All supplied types from the first dynamic type onward must use lambdas.
     * [strings] contains unordered filters without an [InstructionLocation].
     */
    constructor(
        classFingerprint: Fingerprint? = null,
        name: String? = null,
        accessFlags: List<AccessFlags>? = null,
        isStatic: Boolean? = null,
        returnType: DexKitBridge.() -> CharSequence,
        parameters: (DexKitBridge.() -> List<CharSequence>)? = null,
        filters: List<InstructionFilter>? = null,
        strings: Collection<StringFilter>,
        custom: (MethodMatcher.() -> Unit)? = null,
    ) : this(
        classFingerprint = classFingerprint,
        declaredDefiningClass = null,
        declaredSuperclass = null,
        declaredInterfaces = null,
        name = name,
        accessFlags = accessFlags,
        isStatic = isStatic,
        declaredReturnType = returnType,
        declaredParameters = parameters,
        filters = filters,
        stringFilters = strings.toList(),
        custom = custom,
    )

    companion object {
        private fun <T> constantOrNull(value: T?): (DexKitBridge.() -> T)? =
            value?.let { { it } }

        private fun constantInterfacesOrNull(value: List<String>?): (DexKitBridge.() -> List<String>)? {
            require(value == null || value.isNotEmpty()) { "Interfaces cannot be empty" }
            return constantOrNull(value)
        }

        private fun rawStringFilters(strings: List<String>?) = strings?.map { string(it) }
    }

    context(dexkit: DexKitBridge)
    operator fun invoke(): MethodData {
        return run()
    }

    /**
     * Finds a unique method result using DexKit query.
     *
     * Strict queries apply instruction matching before resolving chained fingerprints
     * and checking the candidate count.
     */
    context(dexkit: DexKitBridge)
    fun run(): MethodData {
        val results = queryAll()

        if (results.size != 1) {
            val name = this::class.simpleName ?: "Anonymous Fingerprint"
            val list = results.joinToString("\n  ") { it.descriptor }
            System.err.println("$name matched ${results.size} methods:\n  $list")
        }
        return results.single()
    }

    private fun signatureMatches(method: MethodData, values: Values): Boolean {
        fun matches(type: String?, actual: String?) = type == null ||
                (actual != null && StringComparisonType.typeDeclarationToComparison(type).compare(actual, type))
        val clazz = method.declaredClass!!
        return (isStatic == null || (method.accessFlags and DexAccessFlags.STATIC != 0) == isStatic) &&
                matches(values.definingClass, clazz.descriptor) &&
                matches(values.returnType, method.returnType?.descriptor) &&
                (values.parameters == null || parametersMatch(method.paramTypes.map { it.descriptor }, values.parameters)) &&
                matches(values.superclass, clazz.superClass?.descriptor) &&
                (values.interfaces == null || values.interfaces.all { type -> clazz.interfaces.any { matches(type, it.descriptor) } })
    }

    private val isStrict = this::class.java.annotations.filterIsInstance<StrictQuery>().isNotEmpty()

    context(dexkit: DexKitBridge)
    private fun queryAll(): List<MethodData> =
        FingerprintQueryCache.shared.getOrQuery(dexkit, this) { queryAllUncached() }

    context(dexkit: DexKitBridge)
    private fun queryAllUncached(): List<MethodData> {
        val values = evaluateValues(dexkit)
        val methodMatcher = buildMethodMatcher(values)
        val results: List<MethodData> = if (classMatcherBlock != null) {
            dexkit.findClass {
                matcher(ClassMatcher().apply(classMatcherBlock!!))
            }.findMethod {
                matcher(methodMatcher)
            }
        } else if (classFingerprint != null) {
            val classes = classFingerprint.queryAll().map { it.declaredClass!! }

            dexkit.findMethod {
                searchInClass(classes)
                matcher(methodMatcher)
            }
        } else if (legacyClassFinder != null) {
            legacyClassFinder!!.invoke(dexkit).findMethod {
                matcher(methodMatcher)
            }
        } else {
            dexkit.findMethod {
                matcher(methodMatcher)
            }
        }

        val candidates = results.filter { signatureMatches(it, values) }
        val filteredResults = if (candidates.size > 1 && isStrict) {
            candidates.filter { matchOrNull(it) != null }
        } else {
            candidates
        }

        if (filteredResults.size > 10) {
            throw IllegalStateException("Bad query, too much results")
        }

        return filteredResults
    }

    context(dexkit: DexKitBridge)
    fun match() = matchOrNull(run()) ?: throw Exception("getInstructionMatches failed")

    context(dexkit: DexKitBridge)
    fun matchOrNull() = matchOrNull(run())

    context(bridge: DexKitBridge)
    fun matchOrNull(
        method: MethodData
    ): Match? {
        val instructions by lazy { method.instructions.toList() }
        val stringMatches = stringFilters?.map { filter ->
            val index = instructions.indexOfFirst { filter.matches(method, it) }
            if (index < 0) return null
            Match.StringMatch(filter.stringValue, index, instructions[index])
        }
        val instructionMatches = filters?.let { matchFilters(method, instructions, it) ?: return null }
        return Match(method, instructionMatches, stringMatches)
    }

    context(bridge: DexKitBridge)
    private fun matchFilters(
        method: MethodData,
        instructions: List<InstructionData>,
        filters: List<InstructionFilter>,
    ): List<Match.InstructionMatch>? {
        val lastMethodIndex = instructions.lastIndex
        val lastFilterIndex = filters.lastIndex
        // The instruction index each filter matched.
        val matchIndexes = IntArray(filters.size)
        // The instruction index each filter continues searching from.
        val searchIndexes = IntArray(filters.size)
        // Created once a filter after the first filter does not match.
        var failures: FilterFailures? = null

        var filterIndex = 0
        searchIndexes[0] = filters.first().location.searchStartIndex(-1, lastMethodIndex)

        while (true) {
            val filter = filters[filterIndex]
            val location = filter.location
            val previousIndex = if (filterIndex == 0) -1 else matchIndexes[filterIndex - 1]
            val searchStep = if (location.searchesBackwards) -1 else 1
            var subIndex = searchIndexes[filterIndex]
            var matchedIndex = -1

            while (subIndex in 0..lastMethodIndex &&
                location.indexIsValidForMatching(previousIndex, subIndex)
            ) {
                if (filter.matches(method, instructions[subIndex])) {
                    matchedIndex = subIndex
                    break
                }
                subIndex += searchStep
            }

            if (matchedIndex < 0) {
                if (filterIndex == 0) {
                    return null // First filter has no more matches to start from.
                }

                if (failures == null) failures = FilterFailures(filters.size, instructions.size)
                failures.add(filterIndex, location, previousIndex)

                // Retry the previous filter.
                filterIndex--
                continue
            }

            matchIndexes[filterIndex] = matchedIndex
            // If this filter is retried, then searching continues after this match.
            searchIndexes[filterIndex] = matchedIndex + searchStep

            if (filterIndex == lastFilterIndex) {
                return List(filters.size) { index ->
                    val instructionIndex = matchIndexes[index]
                    Match.InstructionMatch(filters[index], instructionIndex, instructions[instructionIndex])
                }
            }

            val nextLocation = filters[filterIndex + 1].location
            if (failures != null && failures.contains(filterIndex + 1, nextLocation, matchedIndex)) {
                continue // The next filter already failed after this index, so retry this filter.
            }

            filterIndex++
            searchIndexes[filterIndex] = nextLocation.searchStartIndex(matchedIndex, lastMethodIndex)
        }
    }

    context(_: DexKitBridge)
    val stringMatches
        get() = match().stringMatches

    context(_: DexKitBridge)
    val instructionMatches
        get() = match().instructionMatches

    /**
     * Match all methods in the target app.
     *
     * @return All methods that match.
     * @throws Exception If the fingerprint failed to match any methods.
     */
    context(patchContext: DexKitBridge)
    fun matchAll() = matchAllOrNull() ?: throw Exception()

    context(_: DexKitBridge)
    fun matchAll(range: IntRange): List<Match> {
        val matches = matchAllOrNull().orEmpty()
        check(matches.size in range) {
            "Expected a number of matches in $range but instead found ${matches.size}: $this"
        }
        return matches
    }

    context(_: DexKitBridge)
    fun matchAllOrNull(): List<Match>? {
        return queryAll().mapNotNull { matchOrNull(it) }
            .distinctBy(Match::method)
            .ifEmpty { null }
    }
}

class Match constructor(
    val method: MethodData,
    private val _instructionMatches: List<InstructionMatch>?,
    private val _stringMatches: List<StringMatch>? = null,
) {

    /**
     * Matches corresponding to the [InstructionFilter] declared in the [Fingerprint].
     */
    val instructionMatches
        get() = _instructionMatches
            ?: throw Exception("Fingerprint declared no instruction filters")
    val instructionMatchesOrNull = _instructionMatches

    val stringMatches
        get() = _stringMatches ?: error("Fingerprint declared no string filters")
    val stringMatchesOrNull = _stringMatches

    class StringMatch internal constructor(
        val string: String,
        val index: Int,
        val instruction: InstructionData
    )

    /**
     * A match for an [InstructionFilter].
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
fun findFieldListDirect(block: FindFieldListFunc) = block

private class FilterFailures(filterCount: Int, private val instructionCount: Int) {
    /**
     * For a filter that searches every instruction after or before the previous match, the failed
     * previous match index that searched the most instructions, or -1 if none failed. Searching from
     * an index farther in the search direction tries only instructions that were already tried.
     */
    private val unboundedFailures = IntArray(filterCount) { -1 }

    /**
     * Failures of filters that search a limited number of instructions, by filter and previous match index.
     */
    private var boundedFailures: BitSet? = null

    fun add(filterIndex: Int, location: InstructionLocation, previousIndex: Int) {
        val failedIndex = unboundedFailures[filterIndex]
        when {
            location.searchesAllAfter() -> if (failedIndex < 0 || previousIndex < failedIndex) {
                unboundedFailures[filterIndex] = previousIndex
            }
            location.searchesAllBefore() -> if (previousIndex > failedIndex) {
                unboundedFailures[filterIndex] = previousIndex
            }
            else -> (boundedFailures ?: BitSet().also { boundedFailures = it })
                .set(filterIndex * instructionCount + previousIndex)
        }
    }

    fun contains(filterIndex: Int, location: InstructionLocation, previousIndex: Int): Boolean {
        val failedIndex = unboundedFailures[filterIndex]
        return when {
            location.searchesAllAfter() -> failedIndex >= 0 && previousIndex >= failedIndex
            location.searchesAllBefore() -> failedIndex >= 0 && previousIndex <= failedIndex
            else -> boundedFailures?.get(filterIndex * instructionCount + previousIndex) == true
        }
    }

    private fun InstructionLocation.searchesAllAfter() =
        this is InstructionLocation.MatchAfterAnywhere || this is InstructionLocation.MatchAfterLast

    private fun InstructionLocation.searchesAllBefore() =
        this is InstructionLocation.MatchBeforeAnywhere || this is InstructionLocation.MatchBeforeFirst
}
