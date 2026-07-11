@file:Suppress("LocalVariableName")

package io.github.nexalloy.morphe.youtube.layout.hide.general

import android.view.View
import app.morphe.extension.shared.ResourceUtils.getDimenIdentifier
import app.morphe.extension.shared.ResourceUtils.getIdIdentifier
import app.morphe.extension.shared.ResourceUtils.getLayoutIdentifier
import app.morphe.extension.shared.settings.preference.BulletPointPreference
import app.morphe.extension.youtube.patches.components.CommentsFilter
import app.morphe.extension.youtube.patches.components.CustomFilter
import app.morphe.extension.youtube.patches.components.DescriptionComponentsFilter
import app.morphe.extension.youtube.patches.components.HorizontalShelvesFilter
import app.morphe.extension.youtube.patches.components.KeywordContentFilter
import app.morphe.extension.youtube.patches.components.LayoutComponentsFilter
import app.morphe.extension.youtube.patches.spans.SanitizeVideoSubtitleFilter
import app.morphe.extension.youtube.patches.spans.SearchLinksFilter
import app.morphe.extension.youtube.settings.preference.HTMLPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.layout.buttons.navigation.NavigationBar
import io.github.nexalloy.morphe.youtube.misc.engagement.EngagementPanelHook
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.emptyComponentClass
import io.github.nexalloy.morphe.shared.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.youtube.misc.litho.node.TreeNodeElementHook
import io.github.nexalloy.morphe.youtube.misc.litho.node.hookTreeNodeResult
import io.github.nexalloy.morphe.youtube.misc.litho.observer.LayoutReloadObserver
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val HideHorizontalShelves = patch {
    dependsOn(
        LithoFilter,
        PlayerTypeHook,
        NavigationBar,
        EngagementPanelHook,
        LayoutReloadObserver
    )

    addLithoFilter(HorizontalShelvesFilter())
}

val HideLayoutComponents = patch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
) {
    dependsOn(
        LithoFilter,
        EngagementPanelHook,
        NavigationBarHook,
        VersionCheck,
        HideHorizontalShelves,
        TreeNodeElementHook,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_hide_description_components_screen",
            preferences = setOf(
                SwitchPreference("morphe_hide_ai_generated_video_summary_section"),
                SwitchPreference("morphe_hide_ask_section"),
                SwitchPreference("morphe_hide_attributes_section", summary = true),
                SwitchPreference("morphe_hide_chapters_section"),
                SwitchPreference("morphe_hide_corrections_section"),
                SwitchPreference("morphe_hide_course_progress_section"),
                SwitchPreference("morphe_hide_explore_section", summary = true),
                SwitchPreference("morphe_hide_explore_course_section"),
                SwitchPreference("morphe_hide_explore_podcast_section"),
                SwitchPreference("morphe_hide_featured_channels_section"),
                SwitchPreference("morphe_hide_featured_links_section"),
                SwitchPreference("morphe_hide_featured_places_section"),
                SwitchPreference("morphe_hide_featured_videos_section"),
                SwitchPreference("morphe_hide_gaming_section"),
                SwitchPreference("morphe_hide_how_this_was_made_section"),
                SwitchPreference("morphe_hide_hype_points"),
                SwitchPreference("morphe_hide_info_cards_section"),
                SwitchPreference("morphe_hide_key_concepts_section"),
                SwitchPreference("morphe_hide_music_section"),
                SwitchPreference("morphe_hide_quizzes_section"),
                SwitchPreference("morphe_hide_subscribe_button"),
                SwitchPreference("morphe_hide_transcript_section"),
                SwitchPreference("morphe_hide_video_details_section")
            ),
        ),
        PreferenceScreenPreference(
            "morphe_comments_screen",
            preferences = setOf(
//                noTitleUnsortedPreferenceCategory(
//                    SwitchPreference(
//                        "morphe_hide_comments_carousel",
//                        summary = true,
//                        tag = "app.morphe.extension.shared.settings.preference.BulletPointSwitchPreference"
//                    ),
//                    TextPreference(
//                        "morphe_hide_comments_carousel_filter_strings",
//                        inputType = InputType.TEXT_MULTI_LINE
//                    )
//                ),
                SwitchPreference("morphe_hide_comments_ai_chat_summary"),
                SwitchPreference("morphe_hide_comments_channel_guidelines"),
                SwitchPreference("morphe_hide_comments_prompts", summary = true),
                SwitchPreference("morphe_hide_comments_by_members_header"),
                SwitchPreference("morphe_hide_comments_section"),
                SwitchPreference("morphe_hide_comments_section_in_home_feed"),
                SwitchPreference("morphe_hide_comments_community_guidelines"),
                SwitchPreference("morphe_hide_comments_create_a_short_button"),
                SwitchPreference("morphe_hide_comments_emoji_and_timestamp_buttons"),
                SwitchPreference("morphe_hide_comments_filter_bar_options", summary = true),
                SwitchPreference("morphe_hide_comments_info_button"),
                SwitchPreference("morphe_hide_comments_live_chat_donators_bar"),
                SwitchPreference("morphe_hide_comments_preview_comment", summary = true),
                SwitchPreference("morphe_hide_comments_thanks_button"),
//                SwitchPreference("morphe_sanitize_comments_highlighted_search_links", summary = true)
            ),
            sorting = Sorting.UNSORTED
        ),
        SwitchPreference("morphe_hide_channel_bar"),
        SwitchPreference("morphe_hide_channel_watermark"),
        SwitchPreference("morphe_hide_crowdfunding_box"),
        SwitchPreference("morphe_hide_emergency_box"),
        SwitchPreference("morphe_hide_info_panels", summary = true),
        SwitchPreference("morphe_hide_join_membership_button"),
        SwitchPreference("morphe_hide_live_chat_replay_button", summary = true),
        SwitchPreference("morphe_hide_medical_panels"),
//        SwitchPreference("morphe_hide_snackbar"),
        SwitchPreference("morphe_hide_subscribers_community_guidelines"),
        SwitchPreference("morphe_hide_sync_button"),
        SwitchPreference("morphe_hide_timed_reactions", summary = true),
        SwitchPreference("morphe_hide_video_title", summary = true),
//        SwitchPreference("morphe_sanitize_video_subtitle", summary = true)
    )

    PreferenceScreen.FEED.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_hide_keyword_content_screen",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_hide_keyword_content_home"),
                SwitchPreference("morphe_hide_keyword_content_subscriptions"),
                SwitchPreference("morphe_hide_keyword_content_search"),
                SwitchPreference("morphe_hide_keyword_content_comments"),
                TextPreference("morphe_hide_keyword_content_phrases", inputType = InputType.TEXT_MULTI_LINE),
                NonInteractivePreference(
                    key = "morphe_hide_keyword_content_about",
                    tag = BulletPointPreference::class.java
                ),
                NonInteractivePreference(
                    key = "morphe_hide_keyword_content_about_whole_words",
                    tag = HTMLPreference::class.java,
                )
            )
        ),
        PreferenceScreenPreference(
            key = "morphe_hide_filter_bar_screen",
            preferences = setOf(
                SwitchPreference("morphe_hide_filter_bar_in_comments"),
                SwitchPreference("morphe_hide_filter_bar_in_feed"),
                SwitchPreference("morphe_hide_filter_bar_in_related_videos"),
                SwitchPreference("morphe_hide_filter_bar_in_search"),
                SwitchPreference("morphe_hide_filter_bar_in_history")
            ),
        ),
        PreferenceScreenPreference(
            key = "morphe_channel_screen",
            preferences = setOf(
//                noTitleUnsortedPreferenceCategory(
//                    SwitchPreference("morphe_hide_channel_tab"),
//                    TextPreference(
//                        "morphe_hide_channel_tab_filter_strings",
//                        inputType = InputType.TEXT_MULTI_LINE
//                    )
//                ),
                SwitchPreference("morphe_hide_community_button"),
                SwitchPreference("morphe_hide_join_button"),
                SwitchPreference("morphe_hide_links_preview", summary = true),
                SwitchPreference("morphe_hide_members_shelf", summary = true),
                SwitchPreference("morphe_hide_posts_shelf"),
                SwitchPreference("morphe_hide_store_button"),
//                SwitchPreference("morphe_hide_subscribe_button_in_channel_page")
            ),
        ),
        SwitchPreference("morphe_hide_album_cards", summary = true),
        SwitchPreference("morphe_hide_artist_cards", summary = true),
        SwitchPreference("morphe_hide_auto_dubbed_label"),
        SwitchPreference("morphe_hide_community_posts"),
        SwitchPreference("morphe_hide_compact_banner", summary = true),
        if (is_20_26_or_greater) {
            ListPreference("morphe_hide_expandable_card")
        } else {
            ListPreference(
                key = "morphe_hide_expandable_card",
                entriesKey = "morphe_hide_expandable_card_legacy_entries",
                entryValuesKey = "morphe_hide_expandable_card_legacy_entry_values"
            )
        },
//        noTitleUnsortedPreferenceCategory(
//            SwitchPreference("morphe_hide_feed_flyout_menu"),
//            TextPreference(
//                "morphe_hide_feed_flyout_menu_filter_strings",
//                inputType = InputType.TEXT_MULTI_LINE
//            )
//        ),
//        noTitleUnsortedPreferenceCategory(
//            SwitchPreference("morphe_hide_account_menu"),
//            TextPreference(
//                "morphe_hide_account_menu_filter_strings",
//                inputType = InputType.TEXT_MULTI_LINE
//            )
//        ),
//        SwitchPreference("morphe_hide_floating_microphone_button", summary = true),
        SwitchPreference("morphe_hide_horizontal_shelves", summary = true),
        SwitchPreference("morphe_hide_hyped_label"),
        SwitchPreference("morphe_hide_image_shelf", summary = true),
        SwitchPreference("morphe_hide_latest_videos_button", summary = true),
        SwitchPreference("morphe_hide_mix_playlists"),
        SwitchPreference("morphe_hide_movies_section"),
        SwitchPreference("morphe_hide_notify_me_button", summary = true),
        SwitchPreference("morphe_hide_playables", summary = true),
//        SwitchPreference("morphe_hide_search_term_thumbnails", summary = true),
//        SwitchPreference("morphe_hide_show_more_button", summary = true),
        SwitchPreference("morphe_hide_subscribed_channels_bar"),
        SwitchPreference("morphe_hide_surveys", summary = true),
        SwitchPreference("morphe_hide_ticket_shelf"),
//        SwitchPreference(
//            "morphe_hide_upload_time",
//            summary = true,
//            tag = app.morphe.extension.shared.settings.preference.BulletPointSwitchPreference::class.java,
//        ),
        SwitchPreference("morphe_hide_video_recommendation_labels", summary = true),
//        SwitchPreference(
//            "morphe_hide_view_count",
//            summary = true,
//            tag = app.morphe.extension.shared.settings.preference.BulletPointSwitchPreference::class.java,
//        ),
        SwitchPreference("morphe_hide_web_search_results", summary = true),
//        SwitchPreference("morphe_hide_youtube_doodles", summary = true),
    )

//    if (is_20_21_or_greater) {
//        PreferenceScreen.FEED.addPreferences(
//            SwitchPreference("morphe_hide_you_may_like_section")
//        )
//    }

    PreferenceScreen.GENERAL.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_custom_filter_screen",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_custom_filter"),
                TextPreference(
                    "morphe_custom_filter_strings",
                    inputType = InputType.TEXT_MULTI_LINE
                ),
            ),
        ),
    )

    addLithoFilter(LayoutComponentsFilter())
    addLithoFilter(DescriptionComponentsFilter())
    addLithoFilter(CommentsFilter())
    addLithoFilter(KeywordContentFilter())
    addLithoFilter(CustomFilter())
//    TODO InclusiveSpanPatch TextComponentPatch
//    addSpanFilter(SanitizeVideoSubtitleFilter())
//    addSpanFilter(SearchLinksFilter())
    hookTreeNodeResult { identifier, list ->
        CommentsFilter.hideCommentsFilterBarOptions(identifier, list)
    }

    // region hide mix playlists

    ParseElementFromBufferFingerprint.hookMethod({
        val emptyComponent = ::emptyComponentClass.clazz.new()
        after {
            val bytes = it.args[2] as ByteArray
            if (LayoutComponentsFilter.filterMixPlaylists(bytes)) {
                it.result = emptyComponent
            }
        }
    })

    // endregion

    // region hide watermark (legacy code for old versions of YouTube)

    ShowWatermarkFingerprint.hookMethod(scopedHook(::showWatermarkSubFingerprint.member) {
        before { it.args[1] = LayoutComponentsFilter.showWatermark() }
    })

    // endregion

    // TODO hide show more button

    // hide subscribed channels bar
    //  Tablet: id.parent_container
    //  Phone (landscape mode): dimen.parent_view_width_in_wide_mode

    // hide album cards
    // layout.album_card

    // hide comments carousel
    // TODO depends on elementProtoParserHookPatch
    // hookElement("$COMMENTS_FILTER_CLASS_NAME->onCommentsLoaded([B)[B")

    // hide comments info button
    // id.information_button

    // hide crowdfunding box
    // layout.donation_companion

    // hide live chat donators bar
    // layout.live_chat_ticker_item

    // TODO hide floating microphone — ShowFloatingMicrophoneButtonFingerprint METHOD_MID

    // hide latest videos button
    // layout.content_pill
    // layout.bar

    // TODO hide YouTube Doodles — YouTubeDoodlesImageViewFingerprint METHOD_MID (replace setImageDrawable)

    // TODO hide view count — HideViewCountFingerprint METHOD_MID (modifyFeedSubtitleSpan)

    // region hide filter bar
    // dimen.filter_bar_height
    // dimen.bar_container_height
    // dimen.watch_next_chip_bar_height

    ::featureFlagCheck.hookMethod {
        after {
            if (it.args[0] == 45682279L)
                it.result = LayoutComponentsFilter.hideInRelatedVideos(it.result as Boolean)
        }
    }
    // id.related_chip_cloud

    // endregion

    // TODO hide you may like section — SearchSuggestionEndpoint/SearchBoxTypingString METHOD_MID (complex helper)

    // TODO PanelSubheaderFingerprint

    // region TODO hide flyout menu
/*

    BottomSheetMenuItemBuilderFingerprint.hookMethod(scopedHook(::bottomSheetMenuItemTextFingerprint.member) {
        after {
            it.result = LayoutComponentsFilter.hideFlyoutMenu(it.result as CharSequence?)
        }
    })

    ContextualMenuItemBuilderFingerprint.hookMethod(scopedHook(::contextualMenuItemTextFingerprint.member) {
        val textViewField = ::contextualMenuItemTextViewField.field
        after {
            val textView = textViewField.get(outerParam.thisObject) as TextView?
            val text = it.result as CharSequence?
            it.result = LayoutComponentsFilter.hideFlyoutMenu(textView, text)
        }
    })
*/

    // endregion

    // TODO hide channel tab — ChannelTabBuilder/ChannelTabRenderer METHOD_MID (iterator manipulation)

    // TODO hide search term thumbnails


    // region hide live chat emoji button

    // id.thumbnail_and_emoji_picker_container

    // endregion

    // region hide live chat thanks button

    // id.inline_extra_buttons_container

    // endregion

    // TODO hide account menu

    // TODO hide snackbar

    // region hide sync button

    // layout.sync_button

    // endregion

    // id hook
    DexMethod("Landroid/view/ViewGroup;->findViewById(I)Landroid/view/View;").hookMethod {
        val parent_container = getIdIdentifier("parent_container")
        val information_button = getIdIdentifier("information_button")
        val related_chip_cloud = getIdIdentifier("related_chip_cloud")
        val thumbnail_and_emoji_picker_container = getIdIdentifier("thumbnail_and_emoji_picker_container")
        val inline_extra_buttons_container = getIdIdentifier("inline_extra_buttons_container")
        after {
            val id = it.args[0] as Int
            val view = it.result as? View ?: return@after
            when (id) {
                parent_container -> LayoutComponentsFilter.hideSubscribedChannelsBar(view)
                information_button -> CommentsFilter.hideCommentsInfoButton(view)
                related_chip_cloud -> LayoutComponentsFilter.hideInRelatedVideos(view)
                thumbnail_and_emoji_picker_container -> CommentsFilter.hideLiveChatEmojiButton(view)
                inline_extra_buttons_container -> CommentsFilter.hideLiveChatThanksButton(view)
            }
        }
    }

    // layout hook
    DexMethod("Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;").hookMethod {
        val live_chat_ticker_item = getLayoutIdentifier("live_chat_ticker_item")
        val donation_companion = getLayoutIdentifier("donation_companion")
        val album_card = getLayoutIdentifier("album_card")
        val content_pill = getLayoutIdentifier("content_pill")
        val bar = getLayoutIdentifier("bar")
        val related_chip_cloud_reduced_margins = getLayoutIdentifier("related_chip_cloud_reduced_margins")
        val sync_button = getLayoutIdentifier("sync_button")
        after {
            val view = it.result as View
            when (it.args[0] as Int) {
                live_chat_ticker_item -> CommentsFilter.hideLiveChatDonatorsBar(view)
                donation_companion -> LayoutComponentsFilter.hideCrowdfundingBox(view)
                album_card -> LayoutComponentsFilter.hideAlbumCard(view)
                content_pill, bar -> LayoutComponentsFilter.hideLatestVideosButton(view)
                related_chip_cloud_reduced_margins -> LayoutComponentsFilter.hideInRelatedVideos(view)
                sync_button -> LayoutComponentsFilter.hideSyncButton(view)
            }
        }
    }

    // getDimensionPixelSize hook
    DexMethod("Landroid/content/res/Resources;->getDimensionPixelSize(I)I").hookMethod {
        val filter_bar_height = getDimenIdentifier("filter_bar_height")
        val bar_container_height = getDimenIdentifier("bar_container_height")
        val parent_view_width_in_wide_mode = getDimenIdentifier("parent_view_width_in_wide_mode")
        val watch_next_chip_bar_height = getDimenIdentifier("watch_next_chip_bar_height")
        after {
            val id = it.result as Int
            if (id == 0)  return@after
            it.result = when (it.args[0]) {
                filter_bar_height -> LayoutComponentsFilter.hideInFeed(id)
                bar_container_height -> LayoutComponentsFilter.hideInSearch(id)
                parent_view_width_in_wide_mode -> LayoutComponentsFilter.hideSubscribedChannelsBar(id)
                watch_next_chip_bar_height -> LayoutComponentsFilter.hideInRelatedVideos(id)
                else -> return@after
            }
        }
    }
}