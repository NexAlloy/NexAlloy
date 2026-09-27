package io.github.nexalloy.revanced.telegram

import io.github.nexalloy.revanced.telegram.ads.RemoveSponsoredAds
import io.github.nexalloy.revanced.telegram.forward.ForwardOptions
import io.github.nexalloy.revanced.telegram.notifications.DisableNotificationMarkRead
import io.github.nexalloy.revanced.telegram.notifications.KeepUnreadAfterQuickReply
import io.github.nexalloy.revanced.telegram.privacy.GhostExceptions
import io.github.nexalloy.revanced.telegram.privacy.HideAllChatActivity
import io.github.nexalloy.revanced.telegram.privacy.HideChoosingContactStatus
import io.github.nexalloy.revanced.telegram.privacy.HideChoosingLocationStatus
import io.github.nexalloy.revanced.telegram.privacy.HideChoosingStickerStatus
import io.github.nexalloy.revanced.telegram.privacy.HideContentReadReceipts
import io.github.nexalloy.revanced.telegram.privacy.HideEmojiAcknowledgementStatus
import io.github.nexalloy.revanced.telegram.privacy.HideEmojiInteractions
import io.github.nexalloy.revanced.telegram.privacy.HideFileUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideGroupCallSpeakingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideOnlineStatus
import io.github.nexalloy.revanced.telegram.privacy.HidePhotoUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HidePlayingGameStatus
import io.github.nexalloy.revanced.telegram.privacy.HideReadReceipts
import io.github.nexalloy.revanced.telegram.privacy.HideScreenshotNotifications
import io.github.nexalloy.revanced.telegram.privacy.HideStoryViews
import io.github.nexalloy.revanced.telegram.privacy.HideTypingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVideoRecordingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVideoUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVoiceRecordingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVoiceUploadStatus

val TelegramPatches = arrayOf(
    RemoveSponsoredAds,
    ForwardOptions,
    DisableNotificationMarkRead,
    KeepUnreadAfterQuickReply,
    GhostExceptions,
    HideOnlineStatus,
    HideReadReceipts,
    HideContentReadReceipts,
    HideTypingStatus,
    HideVoiceRecordingStatus,
    HideVideoRecordingStatus,
    HideVoiceUploadStatus,
    HideVideoUploadStatus,
    HidePhotoUploadStatus,
    HideFileUploadStatus,
    HideChoosingLocationStatus,
    HideChoosingContactStatus,
    HideChoosingStickerStatus,
    HidePlayingGameStatus,
    HideEmojiInteractions,
    HideEmojiAcknowledgementStatus,
    HideGroupCallSpeakingStatus,
    HideStoryViews,
    HideScreenshotNotifications,
    HideAllChatActivity,
)
