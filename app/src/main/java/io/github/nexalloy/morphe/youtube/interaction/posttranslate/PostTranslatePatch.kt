/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4163
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.posttranslate

import app.morphe.extension.youtube.patches.PostTranslatePatch
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.misc.feedcards.feedVideoCardsHookPatch
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val postTranslatePatch = patch(
    name = "Translate posts",
    description = "Adds a menu item to translate the text of posts.",
) {
    dependsOn(
        elementProtoParserHookPatch,
        lithoSpannableStringPatch,
        flyoutPatch,
        feedVideoCardsHookPatch,
    )

    PreferenceScreen.FEED.addPreferences(
        SwitchPreference("morphe_post_translate", summary = true),
        ListPreference("morphe_post_translate_language"),
    )

    setExtensionIsPatchIncluded(PostTranslatePatch::class.java)

    // The element of a post includes its whole text, while a long text is shown cut off.
    hookElement(PostTranslatePatch::onElementLoaded)

    // Texts that Litho creates again are replaced with their translation.
    hookLithoSpannableString(PostTranslatePatch::onLithoTextLoaded)
}
