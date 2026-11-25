package io.github.nexalloy.morphe.youtube.layout.returnyoutubedislike

import app.morphe.extension.shared.returnyoutubedislike.ui.ReturnYouTubeDislikeAboutPreference
import app.morphe.extension.shared.returnyoutubedislike.ui.ReturnYouTubeDislikeDebugStatsPreferenceCategory
import app.morphe.extension.youtube.patches.ReturnYouTubeDislikePatch
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.LikeEndpointParserFingerprint
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.endpointServiceNameField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.hookLikeDislikeButtons
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadPresenceField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadTargetField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadUnpackMethod
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.voteVideoIdField
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.screenInfoPreferenceCategory
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.shared.misc.textcomponent.textComponentPatch
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.feedcards.feedVideoCardsHookPatch
import io.github.nexalloy.morphe.youtube.misc.fix.videoactionbar.restoreOldVideoActionBarPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookVideoId
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val returnYouTubeDislikePatch = patch(
    name = "Return YouTube Dislike",
    description = "Adds an option to show the dislike count of videos with Return YouTube Dislike, " +
            "and the like count and like ratio on the thumbnails of feed videos."
) {
    dependsOn(
        textComponentPatch,
        lithoSpannableStringPatch,
        VideoId,
        VideoInformationPatch,
        PlayerTypeHook,
        feedVideoCardsHookPatch,
        restoreOldVideoActionBarPatch
    )

    PreferenceScreen.RETURN_YOUTUBE_DISLIKE.addPreferences(
        SwitchPreference("morphe_ryd_enabled"),
        SwitchPreference("morphe_ryd_dislike_percentage", summary = true),
        SwitchPreference("morphe_ryd_feed_like_count"),
        SwitchPreference("morphe_ryd_feed_like_ratio"),
            ListPreference("morphe_ryd_feed_counts_position"),
        SwitchPreference("morphe_ryd_estimated_like", summary = true),
        SwitchPreference("morphe_ryd_toast_on_connection_error", summary = true),
        screenInfoPreferenceCategory(
            key = "morphe_ryd_about",
            preferences = setOf(
                NonInteractivePreference(
                    key = "morphe_ryd_attribution",
                    titleKey = null,
                    tag = ReturnYouTubeDislikeAboutPreference::class.java,
                    selectable = true
                )
            )
        ),
        PreferenceCategory(
            key = "morphe_ryd_statistics_category",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = emptySet(), // Preferences are added by custom class at runtime.
            tag = ReturnYouTubeDislikeDebugStatsPreferenceCategory::class.java
        )
    )

    // region Inject newVideoLoaded event handler to update dislikes when a new video is loaded.

    hookVideoId(ReturnYouTubeDislikePatch::newVideoLoaded)

    // Hook the player response video ID, to start loading RYD sooner in the background.
    hookPlayerResponseVideoId(ReturnYouTubeDislikePatch::preloadVideoId)

    // endregion

    // region Hook like/dislike/remove like button clicks to send votes to the API.

    val endpointField = ::endpointServiceNameField.field
    val videoIdField = ::voteVideoIdField.field
    val targetField = ::votePayloadTargetField.field
    val presenceField = ::votePayloadPresenceField.field
    LikeEndpointParserFingerprint.hookMethod(scopedHook(::votePayloadUnpackMethod.method) {
        after {
            val payload = it.result ?: return@after
            if (targetField.declaringClass.isInstance(payload) && presenceField.getInt(payload) and 2 != 0) {
                outerParam.setObjectExtra("rydVoteTarget", targetField.get(payload))
            }
        }
    })
    LikeEndpointParserFingerprint.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            val target = it.getObjectExtra("rydVoteTarget") ?: return@after
            ReturnYouTubeDislikePatch.sendVote(
                endpointField.get(it.thisObject) as String,
                videoIdField.get(target) as String
            )
        }
    }

    // endregion

    hookLithoSpannableString(ReturnYouTubeDislikePatch::onLithoTextLoaded)

    hookLikeDislikeButtons(
        ReturnYouTubeDislikePatch::onComponentHostContentDescription,
        ReturnYouTubeDislikePatch::onYogaSetWidth
    )
}
