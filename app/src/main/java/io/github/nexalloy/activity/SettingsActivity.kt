@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.BuildConfig
import io.github.nexalloy.R
import io.github.nexalloy.appPatchConfigurations
import io.github.nexalloy.common.UpdateChecker
import kotlin.system.exitProcess

class SettingsActivity : Activity(), SettingApplication.ServiceStateListener {

    private var mService: XposedService? = null
    private lateinit var aboutPreference: MorpheAboutPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBackPressed()
            }
        }
        setContentView(R.layout.activity_settings)
        actionBar?.setDisplayShowHomeEnabled(true)

        Utils.setContext(applicationContext)
        Utils.setActivity(this)
        aboutPreference = MorpheAboutPreference(this).apply {
            setTitle(R.string.about_title)
        }

        if (savedInstanceState != null) return

        fragmentManager.beginTransaction().replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onStart() {
        super.onStart()
        SettingApplication.addServiceStateListener(this, true)
    }

    override fun onStop() {
        SettingApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        mService = service
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.xp_settings_menu, menu)
        menu.findItem(R.id.menu_disable_auto_check).isVisible = false
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val aliasName = ComponentName(this, SettingsActivity::class.java.name + "Alias")
        menu.findItem(R.id.menu_hide_icon).isChecked =
            packageManager.getComponentEnabledSetting(aliasName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        val menuDisableAutoCheck = menu.findItem(R.id.menu_disable_auto_check)
        val autoCheckDisabled = try {
            mService?.getRemotePreferences("prefs")
                ?.getBoolean("disable_auto_check_update", false)
        } catch (_: RuntimeException) {
            null
        }
        menuDisableAutoCheck.isVisible = autoCheckDisabled != null
        if (autoCheckDisabled != null) {
            menuDisableAutoCheck.isChecked = autoCheckDisabled
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                aboutPreference.onPreferenceClickListener?.onPreferenceClick(aboutPreference)
                true
            }

            R.id.menu_hide_icon -> {
                val newChecked = !item.isChecked
                item.isChecked = newChecked
                val aliasName = ComponentName(this, SettingsActivity::class.java.name + "Alias")
                val status = if (newChecked) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                packageManager.setComponentEnabledSetting(
                    aliasName,
                    status,
                    PackageManager.DONT_KILL_APP
                )
                true
            }

            R.id.menu_disable_auto_check -> {
                val newChecked = !item.isChecked
                val saved = try {
                    val service = mService ?: throw IllegalStateException("Xposed service unavailable")
                    service.getRemotePreferences("prefs")
                        .edit().putBoolean("disable_auto_check_update", newChecked).apply()
                    true
                } catch (_: RuntimeException) {
                    false
                }
                if (saved) {
                    item.isChecked = newChecked
                } else {
                    item.isVisible = false
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // API 33+ back gestures are routed here explicitly through OnBackInvokedDispatcher in onCreate.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAndRemoveTask()
        exitProcess(0)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    class SettingsFragment : PreferenceFragment(), SettingApplication.ServiceStateListener {
        private var mService: XposedService? = null

        private var offPreference: Preference? = null
        private var onCategory: PreferenceCategory? = null

        fun AppPatchInfo.getPreference(): Preference {
            return Preference(context).apply {
                title = appName
                key = appName
                intent = Intent(context, AppPatchSettingsActivity::class.java).apply {
                    putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, appName)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val rootScreen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = rootScreen

            Preference(context).apply {
                setSummary(R.string.slogan_summary)
                isEnabled = false
                rootScreen.addPreference(this)
            }

            Utils.setContext(context.applicationContext)

            Preference(context).apply {
                summary =
                    "This app uses code from Morphe. To learn more, visit https://morphe.software"
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://morphe.software"))
                rootScreen.addPreference(this)
            }

            Preference(context).apply {
                setTitle(R.string.faq_title)
                intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/NexAlloy/NexAlloy/wiki/Frequently-Asked-Questions")
                )
                rootScreen.addPreference(this)
            }

            addPreferencesFromResource(R.xml.license_prefs)

            Preference(context).apply {
                setTitle(R.string.check_for_update_title)
                summary =
                    """Current version: ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH}) ${BuildConfig.BUILD_TYPE}
                       |Build Date: ${DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)}""".trimMargin()
                setOnPreferenceClickListener {
                    UpdateChecker().apply {
                        setActivity(activity)
                        checkUpdate(silent = false)
                    }
                    true
                }
                rootScreen.addPreference(this)
            }
            UpdateChecker().apply {
                setActivity(activity)
                autoCheckUpdate()
            }

            updateDynamicUI(false)
        }

        fun updateDynamicUI(on: Boolean) {
            val rootScreen = preferenceScreen ?: return
            if (onCategory != null) rootScreen.removePreference(onCategory)
            if (offPreference != null) rootScreen.removePreference(offPreference)

            if (!on) {
                offPreference = Preference(context).apply {
                    setSummary(R.string.module_not_activated_summary)
                    isEnabled = false
                    rootScreen.addPreference(this)
                }
            } else {
                onCategory = PreferenceCategory(context).apply {
                    setTitle(R.string.patch_selection)

                    rootScreen.addPreference(this)

                    this.addPreference(Preference(context).apply {
                        setSummary(R.string.force_stop_to_apply_summary)
                        isEnabled = false
                    })

                    for (appPatchInfo in appPatchConfigurations) {
                        this.addPreference(appPatchInfo.getPreference())
                    }
                }
            }
        }

        override fun onStart() {
            super.onStart()
            SettingApplication.addServiceStateListener(this, true)
        }

        override fun onStop() {
            SettingApplication.removeServiceStateListener(this)
            super.onStop()
        }

        override fun onServiceStateChanged(service: XposedService?) {
            mService = service
            if (service == null) {
                updateDynamicUI(false)
                return
            }

            val isModuleActivated = try {
                service.getRemotePreferences("prefs")
                service.apiVersion
                true
            } catch (_: RuntimeException) {
                false
            }

            updateDynamicUI(isModuleActivated)
        }
    }
}
