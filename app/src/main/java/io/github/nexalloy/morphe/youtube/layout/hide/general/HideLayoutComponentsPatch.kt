@file:Suppress("LocalVariableName")

package io.github.nexalloy.morphe.youtube.layout.hide.general

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.morphe.extension.shared.ResourceUtils.getDimenIdentifier
import app.morphe.extension.shared.ResourceUtils.getIdIdentifier
import app.morphe.extension.shared.ResourceUtils.getLayoutIdentifier
import app.morphe.extension.shared.settings.preference.BulletPointPreference
import app.morphe.extension.youtube.patches.components.CommentsFilter
import app.morphe.extension.youtube.patches.components.CustomFilter
import app.morphe.extension.youtube.patches.components.DescriptionComponentsFilter
import app.morphe.extension.youtube.patches.components.ExploreMenuFilter
import app.morphe.extension.youtube.patches.components.KeywordContentFilter
import app.morphe.extension.youtube.patches.components.LayoutComponentsFilter
import app.morphe.extension.youtube.patches.spans.SanitizeVideoSubtitleFilter
import app.morphe.extension.youtube.patches.spans.SearchLinksFilter
import app.morphe.extension.youtube.settings.preference.HTMLPreference
import app.morphe.extension.youtube.settings.preference.KeywordContentStatsPreferenceCategory
import com.google.protobuf.CodedInputStream
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.emptyComponentClass
import io.github.nexalloy.morphe.shared.misc.litho.node.hookTreeNodeResult
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.screenInfoPreferenceCategory
import io.github.nexalloy.morphe.shared.misc.spans.addSpanFilter
import io.github.nexalloy.morphe.shared.misc.spans.inclusiveSpanPatch
import io.github.nexalloy.morphe.shared.misc.textcomponent.textComponentPatch
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.layout.hide.shelves.HideHorizontalShelves
import io.github.nexalloy.morphe.youtube.misc.aislist.aiSListPatch
import io.github.nexalloy.morphe.youtube.misc.engagement.EngagementPanelHook
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.litho.node.TreeNodeElementHook
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.navigation.addBrowseIdHook
import io.github.nexalloy.morphe.youtube.misc.navigation.addSearchResultsHook
import io.github.nexalloy.morphe.youtube.misc.navigation.browseIdHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_11_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_25_or_greater
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.speed.custom.TapAndHoldSpeedFingerprint
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val HideLayoutComponents = patch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
) {
    dependsOn(
        browseIdHookPatch,
        aiSListPatch,
        LithoFilter,
        EngagementPanelHook,
        NavigationBarHook,
        VersionCheck,
        HideHorizontalShelves,
        elementProtoParserHookPatch,
        TreeNodeElementHook,
        inclusiveSpanPatch,
        textComponentPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_hide_description_components_screen",
            preferences = setOf(
                SwitchPreference("morphe_hide_ai_generated_video_summary_section"),
                SwitchPreference("morphe_hide_ask_section"),
                SwitchPreference("morphe_hide_attributes_section", summary = true),
                SwitchPreference("morphe_hide_channel_links_section"),
                SwitchPreference("morphe_hide_chapters_section"),
                SwitchPreference("morphe_hide_corrections_section"),
                SwitchPreference("morphe_hide_course_progress_section"),
                SwitchPreference("morphe_hide_explore_section", summary = true),
                SwitchPreference("morphe_hide_explore_course_section"),
                SwitchPreference("morphe_hide_explore_podcast_section"),
                SwitchPreference("morphe_hide_featured_channels_section"),
                SwitchPreference("morphe_hide_featured_links_section"),
                SwitchPreference("morphe_hide_featured_playlists_section"),
                SwitchPreference("morphe_hide_featured_videos_section"),
                SwitchPreference("morphe_hide_hashtag_section"),
                SwitchPreference("morphe_hide_how_this_was_made_section"),
                SwitchPreference("morphe_hide_hype_points"),
                SwitchPreference("morphe_hide_info_cards_section"),
                SwitchPreference("morphe_hide_key_concepts_section"),
                SwitchPreference("morphe_hide_quizzes_section"),
                SwitchPreference("morphe_hide_search_inside_this_video_section"),
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
//                        tag = BulletPointSwitchPreference::class.java
//                    ),
//                    TextPreference(
//                        "morphe_hide_comments_carousel_filter_strings",
//                        inputType = InputType.TEXT_MULTI_LINE
//                    )
//                ),
                SwitchPreference("morphe_hide_comments_ai_chat_summary"),
                SwitchPreference("morphe_hide_comments_channel_guidelines"),
                SwitchPreference("morphe_hide_comments_by_members_header"),
                SwitchPreference("morphe_hide_comments_section"),
                SwitchPreference("morphe_hide_comments_section_in_home_feed"),
                SwitchPreference("morphe_hide_comments_community_guidelines"),
                SwitchPreference("morphe_hide_comments_contexts"),
                SwitchPreference("morphe_hide_comments_create_a_short_button"),
                SwitchPreference("morphe_hide_comments_dislike_button"),
                SwitchPreference("morphe_hide_comments_emoji_button"),
                SwitchPreference("morphe_hide_comments_filter_bar_options", summary = true),
                SwitchPreference("morphe_hide_comments_gif_button"),
                SwitchPreference("morphe_hide_comments_gifs_and_images", summary = true),
                SwitchPreference("morphe_hide_comments_gift_animation_and_cards"),
                SwitchPreference("morphe_hide_comments_gift_button"),
                SwitchPreference("morphe_hide_comments_info_button"),
                SwitchPreference("morphe_hide_comments_live_chat_donators_bar"),
                SwitchPreference("morphe_hide_comments_live_chat_scroll_to_bottom_button"),
                SwitchPreference("morphe_hide_comments_live_chat_tooltips", summary = true),
                SwitchPreference("morphe_hide_comments_menu_button", summary = true),
//                SwitchPreference("morphe_hide_comments_preview_comment", summary = true),
                SwitchPreference("morphe_hide_comments_zero_state"),
                SwitchPreference("morphe_minimal_comments_button", summary = true),
                SwitchPreference("morphe_hide_comments_thanks_button"),
                SwitchPreference("morphe_hide_comments_timestamp_button"),
                SwitchPreference("morphe_hide_comments_top_fans_button"),
                SwitchPreference("morphe_hide_comments_translate_button"),
                SwitchPreference("morphe_sanitize_comments_highlighted_search_links", summary = true)
            ),
            sorting = Sorting.UNSORTED
        ),
        SwitchPreference("morphe_hide_ask_gemini_button", summary = true),
        SwitchPreference("morphe_hide_channel_bar"),
        SwitchPreference("morphe_hide_channel_watermark"),
        SwitchPreference("morphe_hide_chapters_timeline_button"),
        SwitchPreference("morphe_hide_crowdfunding_box"),
        SwitchPreference("morphe_hide_emergency_box"),
        SwitchPreference("morphe_hide_info_panels", summary = true),
        SwitchPreference("morphe_hide_join_membership_button"),
        SwitchPreference("morphe_hide_live_chat_replay_button", summary = true),
        SwitchPreference("morphe_hide_medical_panels"),
        SwitchPreference("morphe_hide_player_gesture_hints", summary = true),
        SwitchPreference("morphe_hide_subscribers_community_guidelines"),
        SwitchPreference("morphe_hide_sync_button"),
        SwitchPreference("morphe_hide_tap_and_hold_gradient", summary = true),
        SwitchPreference("morphe_hide_timed_reactions", summary = true),
        SwitchPreference("morphe_hide_video_title", summary = true),
        SwitchPreference("morphe_sanitize_video_subtitle", summary = true)
    )

    if (is_20_31_or_greater) {
        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_explore_menu_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_hide_explore_button"),
                    SwitchPreference("morphe_hide_shopping_menu"),
                    SwitchPreference("morphe_hide_music_menu"),
                    SwitchPreference("morphe_hide_movies_menu"),
                    SwitchPreference("morphe_hide_hype_menu"),
                    SwitchPreference("morphe_hide_live_menu"),
                    SwitchPreference("morphe_hide_gaming_menu"),
                    SwitchPreference("morphe_hide_news_menu"),
                    SwitchPreference("morphe_hide_sports_menu"),
                    SwitchPreference("morphe_hide_courses_menu"),
                    SwitchPreference("morphe_hide_learning_menu"),
                    SwitchPreference("morphe_hide_fashion_menu"),
                    SwitchPreference("morphe_hide_podcasts_menu"),
                    SwitchPreference("morphe_hide_playables_menu"),
                    SwitchPreference("morphe_hide_memberships_menu"),
                    SwitchPreference("morphe_hide_youtube_premium_menu"),
                    SwitchPreference("morphe_hide_youtube_studio_menu"),
                    SwitchPreference("morphe_hide_youtube_music_menu"),
                    SwitchPreference("morphe_hide_youtube_kids_menu"),
                    SwitchPreference("morphe_hide_youtube_create_menu"),
                    SwitchPreference("morphe_hide_youtube_works_menu"),
                    SwitchPreference("morphe_hide_privacy_tos_footer")
                )
            )
        )
    }

    PreferenceScreen.FEED.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_hide_keyword_content_screen",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_hide_keyword_content_comments"),
                SwitchPreference("morphe_hide_keyword_content_home"),
                SwitchPreference("morphe_hide_keyword_content_search"),
                SwitchPreference("morphe_hide_keyword_content_subscriptions"),
                TextPreference(
                    "morphe_hide_keyword_content_phrases",
                    inputType = InputType.TEXT_MULTI_LINE
                ),
                PreferenceCategory(
                    key = "morphe_hide_keyword_content_stats_category",
                    titleKey = "morphe_hide_stats_category_title",
                    sorting = Sorting.UNSORTED,
                    preferences = emptySet(),
                    tag = KeywordContentStatsPreferenceCategory::class.java
                ),
                screenInfoPreferenceCategory(
                    key = "morphe_hide_keyword_content_about_category",
                    preferences = setOf(
                        NonInteractivePreference(
                            key = "morphe_hide_keyword_content_about",
                            titleKey = null,
                            tag = BulletPointPreference::class.java
                        ),
                        NonInteractivePreference(
                            key = "morphe_hide_keyword_content_about_whole_words",
                            tag = HTMLPreference::class.java
                        )
                    )
                )
            )
        ),
        PreferenceScreenPreference(
            key = "morphe_hide_filter_bar_screen",
            preferences = setOf(
                SwitchPreference("morphe_hide_filter_bar_in_channel_page"),
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
                SwitchPreference("morphe_hide_members_only_chip", summary = true),
                SwitchPreference("morphe_hide_members_shelf", summary = true),
                SwitchPreference("morphe_hide_posts_shelf"),
                SwitchPreference("morphe_hide_store_button"),
//                SwitchPreference("morphe_hide_subscribe_button_in_channel_page")
            ),
        ),
        SwitchPreference("morphe_hide_album_cards", summary = true),
        SwitchPreference("morphe_hide_artist_cards", summary = true),
        SwitchPreference("morphe_hide_channel_buttons", summary = true),
        SwitchPreference("morphe_hide_community_posts"),
            PreferenceScreenPreference(
                key = "morphe_post_buttons_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    ListPreference("morphe_post_buttons_layout"),
                    SwitchPreference("morphe_hide_post_like_dislike_buttons"),
                    SwitchPreference("morphe_hide_post_dislike_button", summary = true),
                    SwitchPreference("morphe_hide_post_comment_button"),
                    SwitchPreference("morphe_hide_post_comments_preview", summary = true),
                    SwitchPreference("morphe_hide_post_share_button", summary = true)
                )
            ),
        SwitchPreference("morphe_hide_compact_banner", summary = true),
        ListPreference("morphe_hide_expandable_card"),
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_hide_feed_flyout_menu"),
            TextPreference(
                "morphe_hide_feed_flyout_menu_filter_strings",
                inputType = InputType.TEXT_MULTI_LINE
            )
        ),
//        noTitleUnsortedPreferenceCategory(
//            SwitchPreference("morphe_hide_account_menu"),
//            TextPreference(
//                "morphe_hide_account_menu_filter_strings",
//                inputType = InputType.TEXT_MULTI_LINE
//            ),
//            SwitchPreference("morphe_hide_account_menu_downloads") // TODO: Account menu filtering.
//        ),
//        SwitchPreference("morphe_hide_floating_microphone_button", summary = true),
        SwitchPreference("morphe_hide_get_premium_button"),
        SwitchPreference("morphe_hide_history_shelf"),
        SwitchPreference("morphe_hide_horizontal_shelves", summary = true),
        SwitchPreference("morphe_hide_image_shelf", summary = true),
        SwitchPreference("morphe_hide_handle", summary = true),
        SwitchPreference("morphe_hide_help_feedback_menu", summary = true),
        SwitchPreference("morphe_hide_invite_to_message_card", summary = true),
        SwitchPreference("morphe_hide_latest_videos_button", summary = true),
        SwitchPreference("morphe_hide_live_streams", summary = true),
        SwitchPreference("morphe_hide_mix_playlists"),
        SwitchPreference("morphe_hide_movies_section"),
        SwitchPreference("morphe_hide_notifications_menu_header", summary = true),
        SwitchPreference("morphe_hide_notify_me_button", summary = true),
        SwitchPreference("morphe_hide_playables", summary = true),
//        SwitchPreference("morphe_hide_search_term_thumbnails", summary = true),
//        SwitchPreference("morphe_hide_show_more_button", summary = true),
        SwitchPreference("morphe_hide_subscribed_channels_bar"),
        SwitchPreference("morphe_hide_subscribed_channels_bar_names", summary = true),
        SwitchPreference("morphe_hide_surveys", summary = true),
        SwitchPreference("morphe_hide_ticket_shelf"),
        SwitchPreference("morphe_hide_unavailable_videos_hidden_banner", summary = true),
//        SwitchPreference(
//            "morphe_hide_upload_time",
//            summary = true,
//            tag = app.morphe.extension.shared.settings.preference.BulletPointSwitchPreference::class.java,
//        ),
        SwitchPreference("morphe_hide_video_thumbnail"),
        SwitchPreference("morphe_hide_video_labels", summary = true),
//        SwitchPreference(
//            "morphe_hide_view_count",
//            summary = true,
//            tag = app.morphe.extension.shared.settings.preference.BulletPointSwitchPreference::class.java,
//        ),
        SwitchPreference("morphe_hide_web_search_results", summary = true),
//        SwitchPreference("morphe_hide_youtube_doodles", summary = true),
    )

//    PreferenceScreen.FEED.addPreferences(
//        SwitchPreference("morphe_hide_you_may_like_section")
//    )

    PreferenceScreen.GENERAL.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_custom_filter_screen",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_custom_filter"),
                TextPreference(
                    "morphe_custom_filter_strings",
                    titleKey = "morphe_custom_filter_screen_title",
                    inputType = InputType.TEXT_MULTI_LINE
                ),
            ),
        ),
    )

    addBrowseIdHook(LayoutComponentsFilter::setBrowseId)
    addSearchResultsHook(LayoutComponentsFilter::clearBrowseId)
    // region hide guidelines sheet shown before commenting

    val mutationBytes = ::entityMutationBytesMethod.method
    EntityMutationPayloadFingerprint.hookMethod {
        after {
            val payload = it.result as? ByteArray ?: return@after
            val bytes = mutationBytes.invoke(it.args[0]) as ByteArray
            val entityType = CodedInputStream.newInstance(bytes).readTag() ushr 3
            it.result = CommentsFilter.hideGuidelinesSheet(entityType, payload)
        }
    }

    // endregion

    hookElement(LayoutComponentsFilter::setPostButtons)
    addLithoFilter(LayoutComponentsFilter())
    addLithoFilter(DescriptionComponentsFilter())
    if (is_20_31_or_greater) {
        addLithoFilter(ExploreMenuFilter())
    }
    addLithoFilter(CommentsFilter())
    addLithoFilter(KeywordContentFilter())
    addLithoFilter(CustomFilter())
    addSpanFilter(SanitizeVideoSubtitleFilter())
    addSpanFilter(SearchLinksFilter())
    hookTreeNodeResult(CommentsFilter::hideCommentsFilterBarOptions)

    // region hide mix playlists

    ParseElementFromBufferFingerprint.hookMethod({
        val emptyComponent = ::emptyComponentClass.clazz.new()
        after {
            val bytes = it.args[2] as ByteArray
            if (LayoutComponentsFilter.filterMixPlaylists(bytes)) {
                app.morphe.extension.shared.patches.components.LithoFilterPatch.removeFilteredElementListItem(
                    io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext(it.args[1])
                )
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

    // TODO hide comment preview

    // hide page header buttons
    hookElement(LayoutComponentsFilter::hidePageHeaderButtons)

    // hide comments carousel
    hookElement(CommentsFilter::onCommentsLoaded)

    // hide comments info button
    // id.information_button

    // hide crowdfunding box
    // layout.donation_companion

    // hide live chat donators bar
    // layout.live_chat_ticker_item

    // region hide tap and hold gradient

    val tapAndHoldOwnerField = ::tapAndHoldRunnableOwnerField.field
    val alreadySpeedingUpField = ::tapAndHoldAlreadySpeedingUpField.field
    TapAndHoldSpeedFingerprint.hookMethod(scopedHook(::tapAndHoldCanStartMethod.method) {
        after {
            // R8 merges several Runnable branches into this method. Check the
            // captured owner and both guards before marking a hold as active.
            val owner = tapAndHoldOwnerField.get(outerParam.thisObject)
            if (it.result == true && alreadySpeedingUpField.declaringClass.isInstance(owner) &&
                !alreadySpeedingUpField.getBoolean(owner)
            ) {
                LayoutComponentsFilter.onTapAndHoldStart()
            }
        }
    })

    ::tapAndHoldEndMethod.hookMethod {
        before { LayoutComponentsFilter.onTapAndHoldEnd() }
    }

    ShowControlsOnRelatedPanelDragFingerprint.hookMethod {
        before {
            if (LayoutComponentsFilter.hideTapAndHoldGradient()) {
                it.result = null
            }
        }
    }

    // endregion

    // region fix live chat text field behind the navigation bar

    // https://github.com/MorpheApp/morphe-patches/issues/3453
    // The flag is read by different layouts, and each must use the same value,
    // otherwise other panels such as the comments show the bottom padding twice.
    // Flag was removed in 21.26+.
    if (is_21_11_or_greater && !is_21_25_or_greater) {
        insertLiteralOverride(45744676, false)
    }

    // endregion

    // region collapse filtered boxes of the fullscreen engagement panel

    // The box keeps its rounded border and padding when the element in it is filtered.
    val measure = MetadataHighlightsColumnMeasureFingerprint.method
    val superMeasure = measure.declaringClass.superclass.getDeclaredMethod(
        "onMeasure", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
    )
    val setMeasuredDimension = View::class.java.getDeclaredMethod(
        "setMeasuredDimension", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
    ).apply { isAccessible = true }
    measure.hookMethod(scopedHook(superMeasure) {
        after {
            if (it.thisObject === outerParam.thisObject && !it.hasThrowable() &&
                LayoutComponentsFilter.isEngagementPanelBoxEmpty(it.thisObject as ViewGroup)) {
                setMeasuredDimension.invoke(it.thisObject, 0, 0)
            }
        }
    })

    // endregion

    // TODO hide floating microphone

    // hide latest videos button
    // layout.content_pill
    // layout.bar

    // TODO hide YouTube Doodles

    // TODO hide view count

    // region hide filter bar
    // dimen.filter_bar_height
    // dimen.bar_container_height

    // id.related_chip_cloud

    // fix: related video overlay is broken due to patch.
    insertLiteralOverride(45614162L, LayoutComponentsFilter::hideInRelatedVideos)
    insertLiteralOverride(45661108L, LayoutComponentsFilter::hideInRelatedVideos)

    // TODO PanelSubheaderFingerprint

    // endregion

    // TODO hide you may like section

    // region hide flyout menu items

    val bottomSheetMenuItemText = ::bottomSheetMenuItemTextFingerprint.method
    ::bottomSheetMenuItemBuilders.dexMethodList.forEach { builder ->
        builder.hookMethod(scopedHook(bottomSheetMenuItemText) {
            after {
                if (innerDepth == 0) {
                    it.result = LayoutComponentsFilter.hideFlyoutMenu(it.result as CharSequence?)
                }
            }
        })
    }

    ContextualMenuItemBuilderFingerprint.hookMethod(scopedHook(
        DexMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V").toMethod()
    ) {
        val textViewField = ::contextualMenuItemTextViewField.field
        after {
            val textView = it.thisObject as TextView
            if (textView === textViewField.get(outerParam.thisObject)) {
                LayoutComponentsFilter.hideFlyoutMenu(textView, it.args[0] as CharSequence?)
            }
        }
    })

    // The list popup menu ignores the zero height of a hidden item and only keeps its text hidden,
    // so the filtered items are left out of the items of the popup menus instead.
    val menuTitle = ::contextualMenuItemTextFingerprint.method
    LayoutComponentsFilter.flyoutMenuItemAdapter = java.util.function.Function { item ->
        if (menuTitle.parameterTypes.single().isInstance(item)) {
            LayoutComponentsFilter.FlyoutMenuItem { menuTitle.invoke(null, item) as? CharSequence }
        } else null
    }
    MenuItemListBuilderFingerprint.hookMethod(scopedHook(::menuItemsCollectorMethod.method) {
        before {
            it.args[0] = LayoutComponentsFilter.hideFlyoutMenuItems(it.args[0] as Iterable<*>)
        }
    })

    ::actionBarMenuAddMethods.dexMethodList.forEach { addMethod ->
        ActionBarMenuBuilderFingerprint.hookMethod(scopedHook(addMethod.toMethod()) {
            after {
                if (innerDepth == 0 && !it.hasThrowable()) {
                    LayoutComponentsFilter.hideFlyoutMenu(it.thisObject as Menu, it.args[1] as Int, it.args[3] as? CharSequence)
                }
            }
        })
    }

    // endregion

    // TODO hide channel tab

    // TODO hide search term thumbnails

    // region hide live chat tooltips

    // layout.tooltip_content_view

    // endregion

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

    // region hide live chat gift button

    // id.jewels_button_container

    // endregion

    // region hide player chapters & timeline button

    // id.time_bar_chapter_title
    // id.time_bar_entry_point_tap_container

    // endregion

    // region hide help & feedback menu

    ListMenuItemViewOnMeasureFingerprint.hookMethod {
        before {
            it.args[1] = LayoutComponentsFilter.hideHelpFeedbackMenuRow(
                it.thisObject as View, it.args[1] as Int
            )
        }
    }

    // endregion

    // id hook
    DexMethod("Landroid/view/ViewGroup;->findViewById(I)Landroid/view/View;").hookMethod {
        val parent_container = getIdIdentifier("parent_container")
        val information_button = getIdIdentifier("information_button")
        val related_chip_cloud = getIdIdentifier("related_chip_cloud")
        val thumbnail_and_emoji_picker_container =
            getIdIdentifier("thumbnail_and_emoji_picker_container")
        val inline_extra_buttons_container = getIdIdentifier("inline_extra_buttons_container")
        val jewels_button_container = getIdIdentifier("jewels_button_container")
        val time_bar_chapter_title = getIdIdentifier("time_bar_chapter_title")
        val time_bar_entry_point_tap_container =
            getIdIdentifier("time_bar_entry_point_tap_container")
        after {
            val id = it.args[0] as Int
            val view = it.result as? View ?: return@after
            when (id) {
                parent_container -> LayoutComponentsFilter.hideSubscribedChannelsBar(view)
                information_button -> CommentsFilter.hideCommentsInfoButton(view)
                related_chip_cloud -> LayoutComponentsFilter.hideInRelatedVideos(view)
                thumbnail_and_emoji_picker_container -> CommentsFilter.hideLiveChatEmojiButton(view)
                inline_extra_buttons_container -> CommentsFilter.hideLiveChatThanksButton(view)
                jewels_button_container -> CommentsFilter.hideLiveChatGiftButton(view)
                time_bar_chapter_title -> LayoutComponentsFilter.hideChapterTitle(view)
                time_bar_entry_point_tap_container -> LayoutComponentsFilter.hideChaptersTimelineButton(
                    view
                )
            }
        }
    }

    // region hide live chat scroll to bottom button

    val moreCommentsIcon = getIdIdentifier("more_comments_icon")
    val findViewById = View::class.java.getDeclaredMethod("findViewById", Int::class.javaPrimitiveType)
    ::moreCommentsIconMethods.dexMethodList.forEach { method ->
        method.hookMethod(scopedHook(findViewById) {
            after {
                if (it.args[0] == moreCommentsIcon) {
                    CommentsFilter.hideLiveChatScrollToBottomButton(it.result as? View)
                }
            }
        })
    }

    // endregion

    // layout hook
    DexMethod("Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;").hookMethod {
        val live_chat_ticker_item = getLayoutIdentifier("live_chat_ticker_item")
        val donation_companion = getLayoutIdentifier("donation_companion")
        val album_card = getLayoutIdentifier("album_card")
        val content_pill = getLayoutIdentifier("content_pill")
        val bar = getLayoutIdentifier("bar")
        val sync_button = getLayoutIdentifier("sync_button")
        val tooltip_content_view = getLayoutIdentifier("tooltip_content_view")
        after {
            val view = it.result as View
            when (it.args[0] as Int) {
                live_chat_ticker_item -> CommentsFilter.hideLiveChatDonatorsBar(view)
                donation_companion -> LayoutComponentsFilter.hideCrowdfundingBox(view)
                album_card -> LayoutComponentsFilter.hideAlbumCard(view)
                content_pill, bar -> LayoutComponentsFilter.hideLatestVideosButton(view)
                sync_button -> LayoutComponentsFilter.hideSyncButton(view)
                tooltip_content_view -> CommentsFilter.hideLiveChatTooltip(view)
            }
        }
    }

    // getDimensionPixelSize hook
    DexMethod("Landroid/content/res/Resources;->getDimensionPixelSize(I)I").hookMethod {
        val filter_bar_height = getDimenIdentifier("filter_bar_height")
        val bar_container_height = getDimenIdentifier("bar_container_height")
        val parent_view_width_in_wide_mode = getDimenIdentifier("parent_view_width_in_wide_mode")
        after {
            val id = it.result as Int
            if (id == 0) return@after
            it.result = when (it.args[0]) {
                filter_bar_height -> LayoutComponentsFilter.hideInFeed(id)
                bar_container_height -> LayoutComponentsFilter.hideInSearch(id)
                parent_view_width_in_wide_mode -> LayoutComponentsFilter.hideSubscribedChannelsBar(
                    id
                )

                else -> return@after
            }
        }
    }
}
