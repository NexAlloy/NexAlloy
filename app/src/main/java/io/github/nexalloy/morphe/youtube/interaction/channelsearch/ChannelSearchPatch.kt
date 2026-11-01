package io.github.nexalloy.morphe.youtube.interaction.channelsearch

import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.youtube.patches.ChannelSearchPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.navigation.addBrowseIdHook
import io.github.nexalloy.morphe.youtube.misc.navigation.addSearchResultsHook
import io.github.nexalloy.morphe.youtube.misc.navigation.browseIdHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.misc.toolbar.hookToolBar
import io.github.nexalloy.morphe.youtube.misc.toolbar.toolBarHookPatch
import io.github.nexalloy.patch

val channelSearchPatch = patch(
    name = "Channel search",
    description = "Adds an option to search inside the channel that is currently open " +
            "instead of searching all of YouTube.",
) {
    dependsOn(toolBarHookPatch, browseIdHookPatch)

    PreferenceScreen.GENERAL.addPreferences(
        SwitchPreference("morphe_channel_search", summary = true)
    )

    hookToolBar(ChannelSearchPatch::setSearchButtonView)

    // A channel page browses by its channel id, which is what the search is scoped to.
    addBrowseIdHook(ChannelSearchPatch::setBrowseId)
    addSearchResultsHook(ChannelSearchPatch::clearBrowseId)

    // The search box otherwise still reads as a search of all of YouTube.
    // Only the default hint is replaced, not the hint of Shorts or playlist search.
    SearchBoxHintFingerprint.hookMethod {
        val search_hint = ResourceUtils.getString("search_hint")
        after {
            if (it.result == search_hint) {
                it.result = ChannelSearchPatch.getSearchHint(it.result as String?)
            }
        }
    }

    ::SearchSubmitFingerprint.hookMethod {
        before {

            if (ChannelSearchPatch.searchInChannel(it.args[0] as? String))
                it.result = null
        }
    }
}
