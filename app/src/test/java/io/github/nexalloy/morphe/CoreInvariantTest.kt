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

class CoreInvariantTest {
    private class MissingClassLoader : ClassLoader() {
        @Suppress("UNUSED_PARAMETER")
        fun lookup(name: String): Class<*> = throw ClassNotFoundException(name)
    }

    private class BrokenClassLoader : ClassLoader() {
        @Suppress("UNUSED_PARAMETER")
        fun lookup(name: String): Class<*> = throw IllegalStateException("broken loader")
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
