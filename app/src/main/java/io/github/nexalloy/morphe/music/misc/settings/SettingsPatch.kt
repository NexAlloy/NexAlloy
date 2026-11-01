package io.github.nexalloy.morphe.music.misc.settings

import android.app.Activity
import android.os.Bundle
import app.morphe.extension.music.settings.MusicActivityHook
import app.morphe.extension.music.settings.preference.AppLanguagePreference
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.preference.ImportExportPreference
import app.morphe.extension.shared.settings.preference.SeekBarPreference
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.R
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.music.shared.MusicActivityOnCreateFingerprint
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.settings.preferences
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceFragmentCompat_addPreferencesFromResource
import io.github.nexalloy.patch

val SettingsHook = patch(
    description = "Adds settings for Morphe to YouTube Music.",
) {
    val addPreferencesFromResource = ::PreferenceFragmentCompat_addPreferencesFromResource.method

    ::PreferenceFragmentCompat_setPreferencesFromResource.hookMethod {
        before {
            val context = Utils.getContext()
            val preferencesName = context.resources.getResourceName(it.args[0] as Int)
            Logger.printDebug { "setPreferencesFromResource $preferencesName" }
            if (!preferencesName.endsWith("settings_headers")) return@before
            addPreferencesFromResource(it.thisObject, R.xml.yt_morphe_settings_music)
            addPreferencesFromResource(it.thisObject, it.args[0])
            it.result = Unit
        }
    }

    // Add an "About" preference to the top.
    preferences += NonInteractivePreference(
        key = "morphe_settings_music_screen_0_about",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_00_about",
        iconBold = "@drawable/morphe_settings_screen_00_about_bold",
        layout = "@layout/morphe_preference_with_icon",
        tag = MorpheAboutPreference::class.java,
        selectable = true
    )

    PreferenceScreen.GENERAL.addPreferences(
        SwitchPreference("morphe_settings_search_history"),
//        SwitchPreference("morphe_show_menu_icons")
    )

    PreferenceScreen.MISC.addPreferences(
        NonInteractivePreference(
            key = "morphe_settings_new_indicator_days",
            tag = SeekBarPreference::class.java,
            selectable = true
        ),
        NonInteractivePreference(
            key = "morphe_music_app_language",
            summaryKey = null,
            tag = AppLanguagePreference::class.java,
            selectable = true
        ),
        TextPreference(
            key = null,
            titleKey = "morphe_pref_import_export_title",
            summaryKey = "morphe_pref_import_export_summary",
            inputType = InputType.TEXT_MULTI_LINE,
            tag = ImportExportPreference::class.java,
        )
    )

    val superOnCreateMethod =
        Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)

    val superOnCreate = xposed.getInvoker(superOnCreateMethod)

    ::googleApiActivityFingerprint.hookMethod {
        before { param ->
            val activity = param.thisObject as Activity
            activity.setTheme(ResourceUtils.getStyleIdentifier("@style/Theme.YouTubeMusic"))
            MusicActivityHook.initialize(activity)
            activity.theme.applyStyle(R.style.ListDividerNull, true)
            superOnCreate.invokeSpecial(param.thisObject, *param.args)
            param.result = Unit
        }
    }

    // Remove other methods as they will break as the onCreate method is modified above.
    ::googleApiActivityNOTonCreate.dexMethodList.forEach {
        if (it.returnTypeName == "void" && it.name != "finish") {
            it.hookMethod(XC_MethodReplacement.DO_NOTHING)
        }
    }

    val settingsActivityClass = ::googleApiActivityClass.clazz
    settingsActivityClass.getMethod("finish").hookMethod {
        before {
            if (settingsActivityClass.isInstance(it.thisObject) &&
                MusicActivityHook.handleFinish(it.thisObject as Activity)
            ) it.result = null
        }
    }

    MusicActivityOnCreateFingerprint.hookMethod {
        before {
            MusicActivityHook.onMainActivityCreate()
        }
    }

    PreferenceScreen.close()
}

object PreferenceScreen : BasePreferenceScreen() {
    val ADS = Screen(
        key = "morphe_settings_music_screen_1_ads",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_01_ads",
        iconBold = "@drawable/morphe_settings_screen_01_ads_bold",
        layout = "@layout/morphe_preference_with_icon"
    )
    val GENERAL = Screen(
        key = "morphe_settings_music_screen_3_general",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_04_general",
        iconBold = "@drawable/morphe_settings_screen_04_general_bold",
        layout = "@layout/morphe_preference_with_icon"
    )
    val PLAYER = Screen(
        key = "morphe_settings_music_screen_4_player",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_05_player",
        iconBold = "@drawable/morphe_settings_screen_05_player_bold",
        layout = "@layout/morphe_preference_with_icon"
    )
    val MISC = Screen(
        key = "morphe_settings_music_screen_9_misc",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_11_misc",
        iconBold = "@drawable/morphe_settings_screen_11_misc_bold",
        layout = "@layout/morphe_preference_with_icon"
    )

    override fun commit(screen: PreferenceScreenPreference) {
        preferences += screen
    }
}
