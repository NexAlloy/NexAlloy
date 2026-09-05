package io.github.nexalloy.morphe.youtube.layout.thumbnails

import app.morphe.extension.youtube.patches.AlternativeThumbnailsPatch.handleCronetFailure
import app.morphe.extension.youtube.patches.AlternativeThumbnailsPatch.handleCronetSuccess
import app.morphe.extension.youtube.patches.AlternativeThumbnailsPatch.overrideImageURL
import app.morphe.extension.youtube.settings.preference.AlternativeThumbnailsAboutDeArrowPreference
import io.github.nexalloy.patch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlErrorCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlSuccessCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.cronetImageUrlHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen

val AlternativeThumbnailsPatch = patch(
    name = "Alternative thumbnails",
    description = "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
) {
    dependsOn(
        cronetImageUrlHookPatch,
    )

    val entries = "morphe_alt_thumbnail_options_entries"
    val values = "morphe_alt_thumbnail_options_entry_values"
    PreferenceScreen.ALTERNATIVE_THUMBNAILS.addPreferences(
        ListPreference(
            key = "morphe_alt_thumbnail_home",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_subscription",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_library",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_player",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_search",
            entriesKey = entries,
            entryValuesKey = values
        ),
        NonInteractivePreference(
            "morphe_alt_thumbnail_dearrow_about",
            // Custom about preference with link to the DeArrow website.
            tag = AlternativeThumbnailsAboutDeArrowPreference::class.java,
            selectable = true,
        ),
        SwitchPreference("morphe_alt_thumbnail_dearrow_connection_toast", summary = true),
        TextPreference("morphe_alt_thumbnail_dearrow_api_url"),
        NonInteractivePreference("morphe_alt_thumbnail_stills_about"),
        SwitchPreference("morphe_alt_thumbnail_stills_fast", summary = true),
        ListPreference("morphe_alt_thumbnail_stills_time"),
    )

    addImageUrlHook(::overrideImageURL)
    addImageUrlSuccessCallbackHook(::handleCronetSuccess)
    addImageUrlErrorCallbackHook(::handleCronetFailure)
}
