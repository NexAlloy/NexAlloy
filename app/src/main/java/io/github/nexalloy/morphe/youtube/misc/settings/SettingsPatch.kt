package io.github.nexalloy.morphe.youtube.misc.settings

import android.app.Activity
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.settings.preference.ImportExportPreference
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import app.morphe.extension.youtube.settings.YouTubeActivityHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.R
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.initialization.initializationPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.settings.preferences
import io.github.nexalloy.morphe.youtube.layout.buttons.overlay.PlayerOverlayButtonsSettings
import io.github.nexalloy.patch

@Suppress("UNREACHABLE_CODE")
val SettingsHook = patch(
    name = "<SettingsHook>"
) {
    dependsOn(
        PlayerOverlayButtonsSettings,
        initializationPatch()
    )

    ::PreferenceFragmentCompat_addPreferencesFromResource.hookMethod {
        val settings_fragment = ResourceUtils.getXmlIdentifier("settings_fragment")
        val settings_fragment_cairo = ResourceUtils.getXmlIdentifier("settings_fragment_cairo")
        before { param ->
            val xml = when (param.args[0] as Int) {
                0 -> return@before
                settings_fragment -> R.xml.yt_morphe_settings
                settings_fragment_cairo -> R.xml.yt_morphe_settings_cairo
                else -> return@before
            }

            XposedBridge.invokeOriginalMethod(
                param.method, param.thisObject, arrayOf(xml)
            )
        }
    }

    val superOnCreate = ::licenseActivitySuperOnCreate.method
    superOnCreate.hookMethod { }

    ::licenseActivityOnCreateFingerprint.hookMethod(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as Activity
            YouTubeActivityHook.initialize(activity)
            activity.theme.applyStyle(R.style.ListDividerNull, true)
            XposedBridge.invokeOriginalMethod(superOnCreate, param.thisObject, param.args)
        }
    })

    // Remove other methods as they will break as the onCreate method is modified above.
    ::licenseActivityNOTonCreate.dexMethodList.forEach {
        if (it.returnTypeName == "void") it.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // Update shared dark mode status based on YT theme.
    // This is needed because YT allows forcing light/dark mode
    // which then differs from the system dark mode status.
    ::setThemeFingerprint.hookMethod {
        after { param ->
            YouTubeActivityHook.updateLightDarkModeStatus(param.result as Enum<*>)
        }
    }

    // Add an "About" preference to the top.
    preferences += NonInteractivePreference(
        key = "morphe_settings_screen_00_about",
        icon = "@drawable/morphe_settings_screen_00_about",
        iconBold = "@drawable/morphe_settings_screen_00_about_bold",
        layout = "@layout/preference_with_icon",
        summaryKey = null,
        tag = MorpheAboutPreference::class.java,
        selectable = true,
    )

//    PreferenceScreen.GENERAL.addPreferences(
//        SwitchPreference("morphe_restore_old_settings_menus")
//        SwitchPreference("morphe_settings_search_history"),
//        SwitchPreference("morphe_show_menu_icons")
//    )

    PreferenceScreen.MISC.addPreferences(
        TextPreference(
            key = null,
            titleKey = "morphe_pref_import_export_title",
            summaryKey = "morphe_pref_import_export_summary",
            inputType = InputType.TEXT_MULTI_LINE,
            tag = ImportExportPreference::class.java,
        ),
//        ListPreference(
//            key = "morphe_language",
//            tag = SortedListPreference::class.java
//        )
    )

    PreferenceScreen.close()
}

object PreferenceScreen : BasePreferenceScreen() {
    // Sort screens in the root menu by key, to not scatter related items apart
    // (sorting key is set in morphe_prefs.xml).
    // If no preferences are added to a screen, the screen will not be added to the settings.
    val ADS = Screen(
        key = "morphe_settings_screen_01_ads",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_01_ads",
        iconBold = "@drawable/morphe_settings_screen_01_ads_bold",
        layout = "@layout/preference_with_icon",
    )
    val ALTERNATIVE_THUMBNAILS = Screen(
        key = "morphe_settings_screen_02_alt_thumbnails",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_02_alt_thumbnails",
        iconBold = "@drawable/morphe_settings_screen_02_alt_thumbnails_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val FEED = Screen(
        key = "morphe_settings_screen_03_feed",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_03_feed",
        iconBold = "@drawable/morphe_settings_screen_03_feed_bold",
        layout = "@layout/preference_with_icon",
    )
    val GENERAL = Screen(
        key = "morphe_settings_screen_04_general",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_04_general",
        iconBold = "@drawable/morphe_settings_screen_04_general_bold",
        layout = "@layout/preference_with_icon",
    )
    val PLAYER = Screen(
        key = "morphe_settings_screen_05_player",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_05_player",
        iconBold = "@drawable/morphe_settings_screen_05_player_bold",
        layout = "@layout/preference_with_icon",
    )
    val SHORTS = Screen(
        key = "morphe_settings_screen_06_shorts",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_06_shorts",
        iconBold = "@drawable/morphe_settings_screen_06_shorts_bold",
        layout = "@layout/preference_with_icon",
    )
    val SEEKBAR = Screen(
        key = "morphe_settings_screen_07_seekbar",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_07_seekbar",
        iconBold = "@drawable/morphe_settings_screen_07_seekbar_bold",
        layout = "@layout/preference_with_icon",
    )
    val SWIPE_CONTROLS = Screen(
        key = "morphe_settings_screen_08_swipe_controls",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_08_swipe_controls",
        iconBold = "@drawable/morphe_settings_screen_08_swipe_controls_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val RETURN_YOUTUBE_DISLIKE = Screen(
        key = "morphe_settings_screen_09_return_youtube_dislike",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_09_return_youtube_dislike",
        iconBold = "@drawable/morphe_settings_screen_09_return_youtube_dislike_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val SPONSORBLOCK = Screen(
        key = "morphe_settings_screen_10_sponsorblock",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_10_sponsorblock",
        iconBold = "@drawable/morphe_settings_screen_10_sponsorblock_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val MISC = Screen(
        key = "morphe_settings_screen_11_misc",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_11_misc",
        iconBold = "@drawable/morphe_settings_screen_11_misc_bold",
        layout = "@layout/preference_with_icon",
    )
    val VIDEO = Screen(
        key = "morphe_settings_screen_12_video",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_12_video",
        iconBold = "@drawable/morphe_settings_screen_12_video_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.BY_KEY,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        preferences += screen
    }
}
