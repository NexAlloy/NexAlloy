package io.github.nexalloy.morphe.youtube.layout.sponsorblock

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.view.ViewGroup
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.sponsorblock.objects.SegmentCategoryPreference
import app.morphe.extension.shared.sponsorblock.ui.SponsorBlockAboutPreference
import app.morphe.extension.youtube.sponsorblock.YouTubeSponsorBlockConfig
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockApiUrlPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockChannelWhitelistPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockCreateSegmentSwitchPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockGuidelinesPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockImportExportPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockPrivateUserIdPreference
import app.morphe.extension.youtube.sponsorblock.preferences.SponsorBlockSegmentStepPreference
import app.morphe.extension.youtube.sponsorblock.ui.CreateSegmentButton
import app.morphe.extension.youtube.sponsorblock.ui.SponsorBlockStatsPreferenceCategory
import app.morphe.extension.youtube.sponsorblock.ui.SponsorBlockViewController
import app.morphe.extension.youtube.sponsorblock.ui.VotingButton
import io.github.nexalloy.R
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addTopControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeTopControl
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.morphe.youtube.video.information.videoTimeHooks
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookBackgroundPlayVideoId
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

private const val SB_PREFERENCES_PACKAGE = "app.morphe.extension.youtube.sponsorblock.preferences"
public fun categoryPreference(settingKey: String): BasePreference =
    object : BasePreference(settingKey, null, null, null, null, null, SegmentCategoryPreference::class.java) {}

val SponsorBlock = patch(
    name = "SponsorBlock",
    description = "Adds options to enable and configure SponsorBlock, which can skip undesired video segments such as sponsored content.",
) {
    dependsOn(
        VideoInformationPatch,
        VideoId,
        PlayerTypeHook,
        LegacyPlayerControls,
    )

    PreferenceScreen.SPONSORBLOCK.addPreferences(
        SwitchPreference("morphe_sb_enabled", summary = true),
        PreferenceCategory(
            key = "morphe_sb_appearance_category",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_sb_voting_button", summary = true),
                SwitchPreference("morphe_sb_compact_skip_button", summary = true),
                SwitchPreference("morphe_sb_auto_hide_skip_button", summary = true),
                ListPreference(key = "morphe_sb_auto_hide_skip_button_duration"),
                SwitchPreference("morphe_sb_toast_on_skip", summary = true),
                ListPreference(key = "morphe_sb_toast_on_skip_duration"),
                SwitchPreference("morphe_sb_video_length_without_segments", summary = true),
                SwitchPreference("morphe_sb_square_layout", summary = true)
            )
        ),
        PreferenceCategory(
            key = "morphe_sb_diff_segments",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                categoryPreference("morphe_sb_sponsor_color"),
                categoryPreference("morphe_sb_selfpromo_color"),
                categoryPreference("morphe_sb_interaction_color"),
                categoryPreference("morphe_sb_highlight_color"),
                categoryPreference("morphe_sb_intro_color"),
                categoryPreference("morphe_sb_outro_color"),
                categoryPreference("morphe_sb_preview_color"),
                categoryPreference("morphe_sb_hook_color"),
                categoryPreference("morphe_sb_filler_color"),
                categoryPreference("morphe_sb_music_offtopic_color")
            )
        ),
        PreferenceCategory(
            key = "morphe_sb_create_segment_category",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference(
                    key = "morphe_sb_create_new_segment",
                    summary = true,
                    tag = SponsorBlockCreateSegmentSwitchPreference::class.java,
                ),
                TextPreference(
                    key = "morphe_sb_create_new_segment_step",
                    tag = SponsorBlockSegmentStepPreference::class.java,
                    inputType = InputType.NUMBER
                ),
                NonInteractivePreference(
                    key = "morphe_sb_guidelines",
                    tag = SponsorBlockGuidelinesPreference::class.java,
                    selectable = true
                )
            )
        ),
        PreferenceCategory(
            key = "morphe_sb_general",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_sb_toast_on_connection_error", summary = true),
                SwitchPreference("morphe_sb_track_skip_count", summary = true),
                TextPreference(
                    key = "morphe_sb_min_segment_duration",
                    inputType = InputType.NUMBER_DECIMAL
                ),
                TextPreference(
                    key = "morphe_sb_private_user_id_Do_Not_Share",
                    tag = SponsorBlockPrivateUserIdPreference::class.java,
                ),
                NonInteractivePreference(
                    key = "morphe_sb_api_url",
                    tag = SponsorBlockApiUrlPreference::class.java,
                    selectable = true
                ),
                NonInteractivePreference(
                    key = "morphe_sb_channel_whitelist",
                    tag = SponsorBlockChannelWhitelistPreference::class.java,
                    selectable = true
                ),
                SwitchPreference("morphe_sb_toast_on_whitelisted_channel", summary = true),
                TextPreference(
                    key = null,
                    titleKey = "morphe_sb_settings_ie_title",
                    summaryKey = "morphe_sb_settings_ie_summary",
                    tag = SponsorBlockImportExportPreference::class.java,
                )
            )
        ),
        PreferenceCategory(
            key = "morphe_sb_stats",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = emptySet(),
            tag = SponsorBlockStatsPreferenceCategory::class.java
        ),
        PreferenceCategory(
            key = "morphe_sb_about",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                NonInteractivePreference(
                    key = "morphe_sb_about_api",
                    tag = SponsorBlockAboutPreference::class.java,
                    selectable = true,
                )
            )
        )
    )

    addTopControl(
        R.layout.morphe_sb_button,
        R.id.morphe_sb_voting_button,
        R.id.morphe_sb_create_segment_button
    )

    videoTimeHooks.add { YouTubeSponsorBlockConfig.setVideoTime(it) }
    hookBackgroundPlayVideoId(YouTubeSponsorBlockConfig::setCurrentVideoId)

    ::seekbarOnDrawFingerprint.hookMethod {
        val sponsorBarRectField = ::SponsorBarRect.field
        before { param ->
            YouTubeSponsorBlockConfig.setSeekbarRectangle(sponsorBarRectField.get(param.thisObject) as Rect)
        }
    }
    val drawCircle =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            "Landroid/view/DisplayListCanvas;->drawCircle(FFFLandroid/graphics/Paint;)V"
        else
            "Landroid/graphics/RecordingCanvas;->drawCircle(FFFLandroid/graphics/Paint;)V"
    ::seekbarOnDrawFingerprint.hookMethod(
        scopedHook(
            DexMethod("Ljava/lang/Math;->round(F)I").toMethod() to {
                after { param ->
                    YouTubeSponsorBlockConfig.setSeekbarThickness(param.result as Int)
                }
            },
            DexMethod(drawCircle).toMethod() to {
                before { param ->
                    YouTubeSponsorBlockConfig.drawSegmentTimeBars(
                        param.thisObject as Canvas, param.args[1] as Float
                    )
                }
            },
        )
    )

    initializeTopControl(
        ControlInitializer(
            R.id.morphe_sb_create_segment_button,
            CreateSegmentButton::initializeLegacyButton
        )
    )
    initializeTopControl(
        ControlInitializer(
            R.id.morphe_sb_voting_button,
            VotingButton::initializeLegacyButton
        )
    )

    AppendTimeFingerprint.hookMethod(
        scopedHook(
            DexMethod("Landroid/content/res/Resources;->getString(I[Ljava/lang/Object;)Ljava/lang/String;").toMember()
        ) {
            after { param ->
                if (param.args[0] != total_time) return@after
                param.result = YouTubeSponsorBlockConfig.appendTimeWithoutSegments(param.result.toString())
            }
        }
    )

    onCreateHook.add { YouTubeSponsorBlockConfig.initialize(it) }

    val controls_overlay_layout =
        ResourceUtils.getLayoutIdentifier("size_adjustable_youtube_controls_overlay")
    ::controlsOverlayFingerprint.hookMethod(scopedHook(DexMethod("Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;").toMember()) {
        val insetOverlayViewLayout = inset_overlay_view_layout
        after { param ->
            if (param.args[0] != controls_overlay_layout) return@after
            val layout = param.result as ViewGroup
            val overlay_view = layout.findViewById<ViewGroup>(insetOverlayViewLayout)
            SponsorBlockViewController.initialize(overlay_view)
        }
    })

    AdProgressTextViewVisibilityFingerprint.hookMethod(
        scopedHook(
            DexMethod("Lcom/google/android/libraries/youtube/ads/player/ui/AdProgressTextView;->setVisibility(I)V").toMember()
        ) {
            before { param ->
                YouTubeSponsorBlockConfig.setAdProgressTextVisibility(param.args[0] as Int)
            }
        }
    )
}
