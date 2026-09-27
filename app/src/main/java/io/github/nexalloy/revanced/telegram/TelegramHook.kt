package io.github.nexalloy.revanced.telegram

import io.github.nexalloy.revanced.telegram.ads.RemoveSponsoredAds
import io.github.nexalloy.revanced.telegram.behavior.DisableMediaAutoplay
import io.github.nexalloy.revanced.telegram.calls.ConfirmOutgoingCalls
import io.github.nexalloy.revanced.telegram.download.DownloadBoostMaximum
import io.github.nexalloy.revanced.telegram.download.DownloadBoostMedium
import io.github.nexalloy.revanced.telegram.forward.ForwardOptions
import io.github.nexalloy.revanced.telegram.history.KeepDeletedMessages
import io.github.nexalloy.revanced.telegram.history.KeepOriginalEditedMessages
import io.github.nexalloy.revanced.telegram.links.CleanSharedLinks
import io.github.nexalloy.revanced.telegram.links.OpenExternalLinksInSystemBrowser
import io.github.nexalloy.revanced.telegram.notifications.DisableNotificationMarkAsRead
import io.github.nexalloy.revanced.telegram.notifications.ForceHideNotificationPreviews
import io.github.nexalloy.revanced.telegram.notifications.QuickReplyWithoutReadReceipt
import io.github.nexalloy.revanced.telegram.privacy.HideAllChatActivity
import io.github.nexalloy.revanced.telegram.privacy.HideContactSelectionStatus
import io.github.nexalloy.revanced.telegram.privacy.HideContentReadReceipts
import io.github.nexalloy.revanced.telegram.privacy.HideEmojiAcknowledgement
import io.github.nexalloy.revanced.telegram.privacy.HideEmojiInteractions
import io.github.nexalloy.revanced.telegram.privacy.HideFileUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideGameStatus
import io.github.nexalloy.revanced.telegram.privacy.HideGroupCallSpeakingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideLocationSelectionStatus
import io.github.nexalloy.revanced.telegram.privacy.HideOnlineStatus
import io.github.nexalloy.revanced.telegram.privacy.HidePhotoUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideReadReceipts
import io.github.nexalloy.revanced.telegram.privacy.HideRecordingAndUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideRoundVideoStatus
import io.github.nexalloy.revanced.telegram.privacy.HideScreenshotNotifications
import io.github.nexalloy.revanced.telegram.privacy.HideStickerSelectionStatus
import io.github.nexalloy.revanced.telegram.privacy.HideStoryReadReceipts
import io.github.nexalloy.revanced.telegram.privacy.HideStoryViewIncrements
import io.github.nexalloy.revanced.telegram.privacy.HideStoryViews
import io.github.nexalloy.revanced.telegram.privacy.HideTypingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVideoRecordingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVideoUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVoiceRecordingStatus
import io.github.nexalloy.revanced.telegram.privacy.HideVoiceUploadStatus
import io.github.nexalloy.revanced.telegram.privacy.PerChatStealthAndExceptions
import io.github.nexalloy.revanced.telegram.tools.ProfileDebugTools

val TelegramPatches = arrayOf(
    RemoveSponsoredAds,
    ForwardOptions,
    PerChatStealthAndExceptions,

    HideOnlineStatus,
    HideReadReceipts,
    HideContentReadReceipts,
    HideTypingStatus,
    HideVoiceRecordingStatus,
    HideVoiceUploadStatus,
    HideVideoRecordingStatus,
    HideVideoUploadStatus,
    HidePhotoUploadStatus,
    HideFileUploadStatus,
    HideRoundVideoStatus,
    HideLocationSelectionStatus,
    HideContactSelectionStatus,
    HideStickerSelectionStatus,
    HideGameStatus,
    HideRecordingAndUploadStatus,
    HideEmojiInteractions,
    HideEmojiAcknowledgement,
    HideGroupCallSpeakingStatus,
    HideStoryReadReceipts,
    HideStoryViewIncrements,
    HideStoryViews,
    HideScreenshotNotifications,
    HideAllChatActivity,

    KeepDeletedMessages,
    KeepOriginalEditedMessages,

    DownloadBoostMedium,
    DownloadBoostMaximum,
    DisableMediaAutoplay,

    CleanSharedLinks,
    OpenExternalLinksInSystemBrowser,

    DisableNotificationMarkAsRead,
    QuickReplyWithoutReadReceipt,
    ForceHideNotificationPreviews,

    ConfirmOutgoingCalls,
    ProfileDebugTools,
)
