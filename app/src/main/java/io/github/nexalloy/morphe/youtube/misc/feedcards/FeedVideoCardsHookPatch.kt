package io.github.nexalloy.morphe.youtube.misc.feedcards

import app.morphe.extension.youtube.patches.utils.FeedVideoCardsPatch
import com.facebook.litho.ComponentHost
import io.github.nexalloy.morphe.youtube.layout.originaltitles.hookCompactVideoTitle
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookVideoId
import io.github.nexalloy.patch

/**
 * Finds the full width video cards of the feeds and the video of each card,
 * for the patches that draw on the thumbnails of feed videos.
 */
internal val feedVideoCardsHookPatch = patch(
    description = "Hooks the video cards of the feeds and finds their video ids.",
) {
    dependsOn(elementProtoParserHookPatch, PlayerTypeHook, VideoInformationPatch, VideoId)

    // The elements of the videos include the thumbnail url with the video id,
    // and the texts of the video, which are matched to the texts of the laid out cards.
    hookElement(FeedVideoCardsPatch::onElementLoaded)

    // The next video that plays in the feed redraws the cards.
    hookVideoId(FeedVideoCardsPatch::onVideoLoaded)

    // Feed videos are Litho components and have no views of their own,
    // so the root host of each video is found when it is laid out.
    FeedCardComponentHostLayoutFingerprint.hookMethod {
        after {
            if (!it.hasThrowable()) {
                FeedVideoCardsPatch.onComponentHostLayout(it.thisObject as ComponentHost)
            }
        }
    }
    // Compact videos are not drawn by Litho, such as the videos of the watch history.
    hookCompactVideoTitle(FeedVideoCardsPatch::onCompactVideoBound)
}
