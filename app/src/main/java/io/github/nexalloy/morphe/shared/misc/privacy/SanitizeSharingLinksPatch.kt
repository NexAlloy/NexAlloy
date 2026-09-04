package io.github.nexalloy.morphe.shared.misc.privacy

import android.content.Intent
import app.morphe.extension.shared.patches.SanitizeSharingLinksPatch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

fun PatchExecutor.SanitizeSharingLinks(
    preferenceScreen: BasePreferenceScreen.Screen,
    replaceMusicLinksWithYouTube: Boolean = false,
    replaceLinksWithShortener: Boolean = false
) {
    val sanitizePreference = SwitchPreference("morphe_sanitize_sharing_links", summary = true)

    preferenceScreen.addPreferences(
        if (replaceMusicLinksWithYouTube || replaceLinksWithShortener) {
            val preferences = mutableSetOf<BasePreference>(sanitizePreference)
            if (replaceMusicLinksWithYouTube) {
                preferences += SwitchPreference("morphe_replace_music_with_youtube", summary = true)
            }
            if (replaceLinksWithShortener) {
                preferences += SwitchPreference("morphe_replace_links_with_shortener", summary = true)
            }
            noTitleUnsortedPreferenceCategory(preferences)
        } else {
            sanitizePreference
        }
    )

    val clipDataNewPlainText = DexMethod(
        "Landroid/content/ClipData;->newPlainText(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Landroid/content/ClipData;"
    ).toMethod()
    YouTubeCopyTextFingerprint.hookMethod(
        scopedHook(clipDataNewPlainText) {
            before { param ->
                val url = param.args[1] as? String ?: return@before
                if (url.startsWith("https://")) {
                    param.args[1] = SanitizeSharingLinksPatch.sanitize(url)
                }
            }
        }
    )

    val intentPutExtra = DexMethod(
        "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;"
    ).toMethod()
    fun hookIntent(fingerprint: io.github.nexalloy.morphe.Fingerprint) {
        fingerprint.hookMethod(
            scopedHook(intentPutExtra) {
                before { param ->
                    if (param.args[0] != Intent.EXTRA_TEXT) return@before
                    val url = param.args[1] as? String ?: return@before
                    if (url.startsWith("https://")) {
                        param.args[1] = SanitizeSharingLinksPatch.sanitize(url)
                    }
                }
            }
        )
    }

    hookIntent(YouTubeShareSheetFingerprint)
    hookIntent(YouTubeSystemShareSheetFingerprint)
}
