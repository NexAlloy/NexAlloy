package io.github.nexalloy.morphe.youtube.misc.navigation

import io.github.nexalloy.patch

private val browseIdHooks = mutableListOf<(String?) -> Unit>()
private val searchResultsHooks = mutableListOf<() -> Unit>()

fun addBrowseIdHook(hook: (String?) -> Unit) { browseIdHooks += hook }
fun addSearchResultsHook(hook: () -> Unit) { searchResultsHooks += hook }

internal val browseIdHookPatch = patch {
    val getBrowseId = ::browserIdMethod.method
    ::BrowseFragmentTarget.hookMethod {
        after {
            val browseId = getBrowseId.invoke(null, it.args[0]) as String?
            browseIdHooks.forEach { hook -> hook(browseId) }
        }
    }
    SearchResultsFragmentOnCreateViewFingerprint.hookMethod {
        before { searchResultsHooks.forEach { hook -> hook() } }
    }
}
