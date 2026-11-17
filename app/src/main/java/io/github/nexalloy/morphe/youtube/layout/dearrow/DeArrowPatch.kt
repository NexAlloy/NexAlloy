package io.github.nexalloy.morphe.youtube.layout.dearrow

import app.morphe.extension.youtube.patches.dearrow.DeArrowPatch
import app.morphe.extension.youtube.patches.dearrow.DeArrowVoteButton
import app.morphe.extension.youtube.settings.preference.DeArrowAboutPreference
import app.morphe.extension.youtube.settings.preference.DeArrowAccountPreferenceCategory
import app.morphe.extension.youtube.settings.preference.DeArrowGuidelinesPreference
import app.morphe.extension.youtube.settings.preference.DeArrowPrivateUserIdPreference
import app.morphe.extension.youtube.settings.preference.DeArrowTimeStepPreference
import app.morphe.extension.youtube.settings.preference.DeArrowVoteButtonSwitchPreference
import io.github.nexalloy.R
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.glide.GlideRequestBuilderLoadGenericFingerprint
import io.github.nexalloy.morphe.shared.misc.litho.relayout.lithoRelayoutPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.screenInfoPreferenceCategory
import io.github.nexalloy.morphe.youtube.layout.originaltitles.videoTitlesHookPatch
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlErrorCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlSuccessCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.cronetImageUrlHookPatch
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addTopControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeTopControl
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val deArrowPatch = patch(
    name = "DeArrow",
    description = "Adds options to replace video thumbnails and titles using the DeArrow API, " +
            "or replace video thumbnails with image captures from the video.",
) {
    dependsOn(
        NavigationBarHook,
        LegacyPlayerControls,
        VideoInformationPatch,
        cronetImageUrlHookPatch,
        // Thumbnails that fail to load are loaded again by mounting the Litho views again.
        lithoRelayoutPatch,
        // Titles are replaced by the same hooks that restore the original titles.
        videoTitlesHookPatch,
    )
    fun thumbnailPreference(key: String, titleKey: String) = ListPreference(
        key = key,
        titleKey = titleKey,
        entriesKey = "morphe_dearrow_thumbnail_options_entries",
        entryValuesKey = "morphe_dearrow_thumbnail_options_entry_values"
    )

    PreferenceScreen.DEARROW.addPreferences(
        PreferenceCategory(
            key = "morphe_dearrow_titles_category",
            titleKey = "morphe_dearrow_titles_title",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_dearrow_titles_home"),
                SwitchPreference("morphe_dearrow_titles_subscription"),
                SwitchPreference("morphe_dearrow_titles_library"),
                SwitchPreference("morphe_dearrow_titles_player"),
                SwitchPreference("morphe_dearrow_titles_search"),
                SwitchPreference("morphe_dearrow_titles_icon", summary = true),
                SwitchPreference("morphe_dearrow_vote_button", summary = true,
                    tag = DeArrowVoteButtonSwitchPreference::class.java),
                SwitchPreference("morphe_dearrow_casual_mode", summary = true),
                TextPreference("morphe_dearrow_casual_mode_min_votes", inputType = InputType.NUMBER)
            )
        ),
        PreferenceCategory(
            key = "morphe_dearrow_thumbnails_category",
            titleKey = "morphe_dearrow_thumbnails_title",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                thumbnailPreference("morphe_dearrow_thumbnail_home", "morphe_dearrow_titles_home_title"),
                thumbnailPreference("morphe_dearrow_thumbnail_subscription", "morphe_dearrow_titles_subscription_title"),
                thumbnailPreference("morphe_dearrow_thumbnail_library", "morphe_dearrow_titles_library_title"),
                thumbnailPreference("morphe_dearrow_thumbnail_player", "morphe_dearrow_titles_player_title"),
                thumbnailPreference("morphe_dearrow_thumbnail_search", "morphe_dearrow_titles_search_title"),
                NonInteractivePreference("morphe_dearrow_thumbnail_stills_about"),
                ListPreference("morphe_dearrow_thumbnail_stills_time")
            )
        ),
        PreferenceCategory(
            key = "morphe_dearrow_general_category",
            titleKey = "morphe_settings_screen_04_general_title",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                TextPreference("morphe_dearrow_api_url"),
                SwitchPreference("morphe_dearrow_connection_toast", summary = true)
            )
        ),
        PreferenceCategory(
            key = "morphe_dearrow_submitting_category",
            titleKey = "morphe_dearrow_submitting_title",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                NonInteractivePreference("morphe_dearrow_guidelines",
                    tag = DeArrowGuidelinesPreference::class.java,
                    selectable = true),
                TextPreference("morphe_dearrow_vote_time_step", inputType = InputType.NUMBER,
                    tag = DeArrowTimeStepPreference::class.java),
                TextPreference("morphe_sb_private_user_id_Do_Not_Share",
                    titleKey = "morphe_dearrow_private_user_id_title",
                    summaryKey = "morphe_dearrow_private_user_id_summary",
                    tag = DeArrowPrivateUserIdPreference::class.java)
            ),
            tag = DeArrowAccountPreferenceCategory::class.java
        ),
        screenInfoPreferenceCategory(
            key = "morphe_dearrow_about_category",
            preferences = setOf(
                NonInteractivePreference(
                    "morphe_dearrow_about",
                    titleKey = null,
                    // Custom about preference with link to the DeArrow website.
                    tag = DeArrowAboutPreference::class.java,
                    selectable = true
                )
            )
        )
    )
    // Other patches use DeArrow only if this patch is included.
    setExtensionIsPatchIncluded(DeArrowPatch::class.java)

    val voteId = R.id.morphe_dearrow_vote_button
    addTopControl(
        R.layout.morphe_dearrow_vote_button, voteId, voteId)
    initializeTopControl(
        ControlInitializer(
            voteId, DeArrowVoteButton::initializeLegacyButton))
    GlideRequestBuilderLoadGenericFingerprint.hookMethod {
        before { it.args[0] = DeArrowPatch.getImageModel(it.args[0]) }
    }
    addImageUrlHook(DeArrowPatch::overrideImageURL)
    addImageUrlSuccessCallbackHook(DeArrowPatch::handleCronetSuccess)
    addImageUrlErrorCallbackHook(DeArrowPatch::handleCronetFailure)
}
