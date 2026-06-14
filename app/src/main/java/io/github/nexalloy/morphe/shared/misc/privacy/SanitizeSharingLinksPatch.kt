package io.github.nexalloy.morphe.shared.misc.privacy

import android.content.ClipData
import android.content.Intent
import app.morphe.extension.shared.patches.SanitizeSharingLinksPatch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory

fun PatchExecutor.SanitizeSharingLinks(
    preferenceScreen: BasePreferenceScreen.Screen,
    replaceMusicLinksWithYouTube: Boolean = false,
    replaceLinksWithShortener: Boolean = false
) {

    val sanitizePreference = SwitchPreference("morphe_sanitize_sharing_links", summary = true)

    preferenceScreen.addPreferences(
        if (replaceMusicLinksWithYouTube || replaceLinksWithShortener) {
            val preferences = mutableSetOf<BasePreference>(sanitizePreference)
            if (replaceMusicLinksWithYouTube) preferences += SwitchPreference("morphe_replace_music_with_youtube", summary = true)
            if (replaceLinksWithShortener) preferences += SwitchPreference("morphe_replace_links_with_shortener", summary = true)

            noTitleUnsortedPreferenceCategory(preferences)
        } else {
            sanitizePreference
        }
    )

    val sanitizeArg1 = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val url = param.args[1] as? String ?: return
            if (!url.startsWith("https://")) return
            param.args[1] = SanitizeSharingLinksPatch.sanitize(url)
        }
    }

    XposedHelpers.findAndHookMethod(
        ClipData::class.java.name,
        lpparam.classLoader,
        "newPlainText",
        CharSequence::class.java,
        CharSequence::class.java,
        sanitizeArg1
    )

    XposedHelpers.findAndHookMethod(
        Intent::class.java.name,
        lpparam.classLoader,
        "putExtra",
        String::class.java,
        String::class.java,
        sanitizeArg1
    )
}