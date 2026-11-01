package io.github.nexalloy.morphe.youtube.layout.buttons.action

import app.morphe.extension.youtube.patches.components.QuickActionButtonsFilter
import app.morphe.extension.youtube.patches.components.VideoActionButtonsFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.fix.videoactionbar.restoreOldVideoActionBarPatch
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val HideVideoActionButtons = patch(
    name = "Hide video action buttons",
    description = "Adds options to hide video action buttons in fullscreen and portrait modes.",
) {
    dependsOn(
        LithoFilter,
        elementProtoParserHookPatch,
        restoreOldVideoActionBarPatch
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_action_buttons_screen",
            preferences = setOf(
                PreferenceCategory(
                    titleKey = "morphe_portrait_buttons",
                    preferences = setOf(
                        SwitchPreference("morphe_disable_like_subscribe_glow", summary = true),
                        SwitchPreference("morphe_hide_action_bar"),
                        SwitchPreference("morphe_hide_ask_button"),
                        SwitchPreference("morphe_hide_channel_profile_button", summary = true),
                        SwitchPreference("morphe_hide_channel_profile_subscribe_button", summary = true),
                        SwitchPreference("morphe_hide_clip_button", summary = true),
                        SwitchPreference("morphe_hide_comments_button"),
                        SwitchPreference("morphe_hide_connect_button"),
                        SwitchPreference("morphe_hide_download_button"),
                        SwitchPreference("morphe_hide_hype_button"),
                        SwitchPreference("morphe_hide_like_dislike_button"),
                        SwitchPreference("morphe_hide_more_button"),
                        SwitchPreference("morphe_hide_promote_button"),
                        SwitchPreference("morphe_hide_remix_button"),
                        SwitchPreference("morphe_hide_report_button"),
                        SwitchPreference("morphe_hide_save_button"),
                        SwitchPreference("morphe_hide_share_button"),
                        SwitchPreference("morphe_hide_shop_button"),
                        SwitchPreference("morphe_hide_stop_ads_button"),
                        SwitchPreference("morphe_hide_thanks_button")
                    )
                ),
                PreferenceCategory(
                    titleKey = "morphe_fullscreen_buttons",
                    preferences = setOf(
//                        NonInteractivePreference(
//                            key = "morphe_quick_actions_top_margin",
//                            tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference"
//                        ),
                        SwitchPreference("morphe_hide_quick_actions", titleKey = "morphe_hide_action_bar_title"),
                        SwitchPreference("morphe_hide_quick_actions_ask_button", titleKey = "morphe_hide_ask_button_title"),
                        SwitchPreference("morphe_hide_quick_actions_comments_button", titleKey = "morphe_hide_comments_button_title"),
                        SwitchPreference("morphe_hide_quick_actions_dislike_button"),
                        SwitchPreference("morphe_hide_quick_actions_like_button"),
                        SwitchPreference("morphe_hide_quick_actions_live_chat_button"),
                        SwitchPreference("morphe_hide_quick_actions_mix_button"),
                        SwitchPreference("morphe_hide_quick_actions_more_button", titleKey = "morphe_hide_more_button_title"),
                        SwitchPreference("morphe_hide_quick_actions_more_videos_button"),
                        SwitchPreference("morphe_hide_quick_actions_playlist_button"),
                        SwitchPreference("morphe_hide_quick_actions_save_button", titleKey = "morphe_hide_save_button_title"),
                        SwitchPreference("morphe_hide_quick_actions_share_button", titleKey = "morphe_hide_share_button_title")
                    )
                )
            )
        )
    )

    addLithoFilter(VideoActionButtonsFilter())
    addLithoFilter(QuickActionButtonsFilter())

    // The hidden buttons are removed from the element of the action bar, for every layout of the action bar.
    hookElement(VideoActionButtonsFilter::hideActionButtons)
}
