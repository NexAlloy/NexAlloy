package io.github.nexalloy.morphe

import io.github.nexalloy.AppVersion
import io.github.nexalloy.ScopedHookStateStack
import io.github.nexalloy.decodeCacheStringList
import io.github.nexalloy.encodeCacheStringList
import io.github.nexalloy.invokeFindClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.MethodMatcher

class CoreInvariantTest {
    private class MissingClassLoader : ClassLoader() {
        @Suppress("UNUSED_PARAMETER")
        fun lookup(name: String): Class<*> = throw ClassNotFoundException(name)
    }

    private class BrokenClassLoader : ClassLoader() {
        @Suppress("UNUSED_PARAMETER")
        fun lookup(name: String): Class<*> = throw IllegalStateException("broken loader")
    }

    @Suppress("DEPRECATION")
    @Test
    fun instanceOfUsesInstanceOfOpcode() {
        val filters = listOf(
            instanceOf("Ljava/lang/String;"),
            instanceOf({ "Ljava/lang/String;" }),
            instanceOf("Ljava/lang/String;", StringComparisonType.EQUALS),
            instanceOf({ "Ljava/lang/String;" }, StringComparisonType.EQUALS)
        )

        filters.forEach { filter ->
            assertEquals(Opcode.INSTANCE_OF, (filter as OpcodeFilter).opcode)
        }
    }

    @Test
    fun versionComparisonIsNumeric() {
        assertTrue(isVersionAtLeast("10.0.0", "9.33.00"))
        assertFalse(isVersionAtLeast("9.9.0", "9.19.0"))
        assertTrue(isVersionAtLeast("21.02.0", "21.02.000"))
        assertTrue(isVersionAtLeast("21.32", "21.32.0"))
        assertFalse(isVersionAtLeast("2026.3.9", "2026.04.0"))
    }

    @Test
    fun appVersionNormalizesMissingParts() {
        assertEquals(0, AppVersion("21.35").compareTo(AppVersion("21.35.0")))
        assertTrue(AppVersion("10.0") > AppVersion("9.999.999"))
        assertTrue(AppVersion("9999999999.0") > AppVersion("2147483647.999"))
    }

    @Test
    fun partialTypesSkipOnlyTheDexKitPrefilter() {
        assertNull(getTypeNameCompat("["))
        assertNull(getTypeNameCompat("[["))
        assertNull(getTypeNameCompat("[L"))
        assertNull(getTypeNameCompat("String;"))
        assertNull(getTypeNameCompat("java/lang"))
        assertEquals("int[]", getTypeNameCompat("[I"))
        assertEquals("java.lang.String", getTypeNameCompat("Ljava/lang/String;"))
        assertEquals("java.lang.String[][]", getTypeNameCompat("[[Ljava/lang/String;"))
    }

    @Test
    fun partialTypesAreEnforcedAfterDexKitPrefilter() {
        assertTrue(
            methodTypeDeclarationsMatch(
                targetDefiningClass = "Lcom/google/YouTubePlayerOverlaysLayout;",
                targetReturnType = "Ljava/lang/String;",
                targetParameters = listOf("Ljava/lang/Object;", "J"),
                fingerprintDefiningClass = "/YouTubePlayerOverlaysLayout;",
                fingerprintReturnType = "L",
                fingerprintParameters = listOf("L", "J"),
            )
        )
        assertTrue(
            methodTypeDeclarationsMatch(
                targetDefiningClass = "Lx;",
                targetReturnType = "[Ljava/lang/String;",
                targetParameters = emptyList(),
                fingerprintDefiningClass = null,
                fingerprintReturnType = "[L",
                fingerprintParameters = emptyList(),
            )
        )
        assertFalse(
            methodTypeDeclarationsMatch(
                targetDefiningClass = "Lx;",
                targetReturnType = "I",
                targetParameters = listOf("Ljava/lang/Object;"),
                fingerprintDefiningClass = null,
                fingerprintReturnType = "L",
                fingerprintParameters = listOf("L"),
            )
        )
        assertFalse(
            methodTypeDeclarationsMatch(
                targetDefiningClass = "Lx;",
                targetReturnType = "V",
                targetParameters = listOf("I"),
                fingerprintDefiningClass = null,
                fingerprintReturnType = "V",
                fingerprintParameters = listOf("L"),
            )
        )
    }

    @Test
    fun fingerprintAccessFlagsAreExactAndLegacyStringsContain() {
        val accessMatcher = MethodMatcher().apply {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        }
        assertEquals(MatchType.Equals, accessMatcher.modifiersMatcher!!.matchType)
        assertEquals(
            AccessFlags.PUBLIC.modifier or AccessFlags.FINAL.modifier,
            accessMatcher.modifiersMatcher!!.modifiers
        )

        val noFlagsMatcher = MethodMatcher().apply { accessFlags() }
        assertNull(noFlagsMatcher.modifiersMatcher)

        val stringMatcher = Fingerprint(
            classFingerprint = null,
            strings = listOf("partial text")
        ).buildMethodMatcher().usingStringsMatcher!!.single()
        assertEquals(StringMatchType.Contains, stringMatcher.matchType)
    }

    @Test
    fun matchFirstOnlyAcceptsMethodStart() {
        val location = InstructionLocation.MatchFirst()
        assertTrue(location.indexIsValidForMatching(-1, 0))
        assertFalse(location.indexIsValidForMatching(-1, 1))
        assertThrows(IllegalArgumentException::class.java) {
            location.indexIsValidForMatching(0, 1)
        }
    }

    @Test
    fun matchAfterWithinRequiresPreviousMatch() {
        val location = InstructionLocation.MatchAfterWithin(2)
        assertTrue(location.indexIsValidForMatching(3, 4))
        assertTrue(location.indexIsValidForMatching(3, 6))
        assertFalse(location.indexIsValidForMatching(3, 7))
        assertFalse(location.indexIsValidForMatching(3, 3))
        assertFalse(location.indexIsValidForMatching(3, 2))
        assertThrows(IllegalArgumentException::class.java) {
            location.indexIsValidForMatching(-1, 0)
        }
    }

    @Test
    fun matchAfterRangeSupportsFirstFilter() {
        val location = InstructionLocation.MatchAfterRange(2, 4)
        assertFalse(location.indexIsValidForMatching(-1, 1))
        assertTrue(location.indexIsValidForMatching(-1, 2))
        assertTrue(location.indexIsValidForMatching(-1, 4))
        assertFalse(location.indexIsValidForMatching(-1, 5))
        assertTrue(location.indexIsValidForMatching(10, 13))
    }

    @Test
    fun smaliParsersSupportMultidimensionalArrays() {
        val method = methodCall("Lx;->m([[I[[Ljava/lang/String;)[[[D")
        assertEquals(listOf("[[I", "[[Ljava/lang/String;"), method.parameters)
        assertEquals("[[[D", method.returnType)

        val field = fieldAccess("Lx;->f:[[Ljava/lang/String;")
        assertEquals("[[Ljava/lang/String;", field.type)
    }

    @Test
    fun smaliParsersRejectVoidOutsideReturnType() {
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m(V)V")
        }
        assertThrows(IllegalArgumentException::class.java) {
            fieldAccess("Lx;->f:V")
        }
    }

    @Test
    fun smaliParsersRejectEmptyObjectDescriptors() {
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m(L;)V")
        }
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m([L;)V")
        }
    }

    @Test
    fun smaliParsersValidateJvmNames() {
        listOf(
            "L/foo;->m()V",
            "Lfoo/;->m()V",
            "Lfoo//bar;->m()V",
            "Lfoo.bar;->m()V",
            "Lfoo;->bad/name()V",
            "Lfoo;->bad.name()V",
            "Lfoo;->bad[method()V",
            "Lfoo;-><bad>()V",
            "Lfoo;->m(Ljava//lang/String;)V"
        ).forEach { signature ->
            assertThrows(IllegalArgumentException::class.java) {
                methodCall(signature)
            }
        }

        methodCall("Lfoo;-><init>()V")
        methodCall("Lfoo;-><clinit>()V")

        listOf(
            "L/foo;->f:I",
            "Lfoo//bar;->f:I",
            "Lfoo;->bad/name:I",
            "Lfoo;->bad.name:I",
            "Lfoo;->bad[field:I",
            "Lfoo;->f:Ljava//lang/String;"
        ).forEach { signature ->
            assertThrows(IllegalArgumentException::class.java) {
                fieldAccess(signature)
            }
        }
    }

    @Test
    fun smaliParsersValidateJvmSpecialMethodDescriptors() {
        methodCall("Lfoo;-><init>(I)V")
        methodCall("Lfoo;-><clinit>()V")

        listOf(
            "Lfoo;-><init>()I",
            "Lfoo;-><clinit>(I)V",
            "Lfoo;-><clinit>()I"
        ).forEach { signature ->
            assertThrows(IllegalArgumentException::class.java) {
                methodCall(signature)
            }
        }
    }

    @Test
    fun smaliParsersEnforceJvmDescriptorLimits() {
        val tooDeepArray = "[".repeat(256) + "I"
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m($tooDeepArray)V")
        }
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m()$tooDeepArray")
        }
        assertThrows(IllegalArgumentException::class.java) {
            fieldAccess("Lx;->f:$tooDeepArray")
        }

        val validSlots = "J".repeat(127) + "I"
        methodCall("Lx;->m($validSlots)V")

        val tooManySlots = "J".repeat(128)
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m($tooManySlots)V")
        }

        val validConstructorSlots = "J".repeat(127)
        methodCall("Lx;-><init>($validConstructorSlots)V")

        val tooManyConstructorSlots = validConstructorSlots + "I"
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;-><init>($tooManyConstructorSlots)V")
        }
    }

    @Test
    fun cacheStringListsRoundTripLosslessly() {
        val cases = listOf(
            emptyList(),
            listOf(""),
            listOf("a|b", "c:d", "", "\u2603"),
            listOf("list-v1:5:hello")
        )

        cases.forEach { original ->
            assertEquals(original, decodeCacheStringList(encodeCacheStringList(original)))
        }
    }

    @Test
    fun cacheStringListDecoderRejectsMalformedValues() {
        assertNull(decodeCacheStringList("legacy|value"))
        assertNull(decodeCacheStringList("list-v1:nope:value"))
        assertNull(decodeCacheStringList("list-v1:10:short"))
        assertNull(decodeCacheStringList("list-v1:-1:"))
    }

    @Test
    fun scopedHookStatePreservesParentAcrossReentrancy() {
        val state = ScopedHookStateStack<Any>()
        val parent = Any()
        val child = Any()

        state.push(parent)
        val parentFrame = state.current()!!
        assertSame(parent, parentFrame.outerParam)
        parentFrame.innerDepth = 2

        state.push(child)
        val childFrame = state.current()!!
        assertSame(child, childFrame.outerParam)
        assertEquals(0, childFrame.innerDepth)

        assertTrue(state.pop(child))
        val restoredParent = state.current()!!
        assertSame(parent, restoredParent.outerParam)
        assertEquals(2, restoredParent.innerDepth)

        assertTrue(state.pop(parent))
        assertNull(state.current())
    }

    @Test
    fun reflectiveClassLookupUnwrapsExpectedMisses() {
        val loader = MissingClassLoader()
        val method = MissingClassLoader::class.java
            .getDeclaredMethod("lookup", String::class.java)
            .apply { isAccessible = true }
        val error = assertThrows(ClassNotFoundException::class.java) {
            invokeFindClass(method, loader, "missing.Type")
        }
        assertEquals("missing.Type", error.message)
    }

    @Test
    fun reflectiveClassLookupPreservesUnexpectedFailures() {
        val loader = BrokenClassLoader()
        val method = BrokenClassLoader::class.java
            .getDeclaredMethod("lookup", String::class.java)
            .apply { isAccessible = true }
        val error = assertThrows(IllegalStateException::class.java) {
            invokeFindClass(method, loader, "broken.Type")
        }
        assertEquals("broken loader", error.message)
    }
}
