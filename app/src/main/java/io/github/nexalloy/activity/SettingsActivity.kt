@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

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
import io.github.nexalloy.common.ACTION_CHECK_MODULE_UPDATE
import io.github.nexalloy.common.CHECK_BETA_UPDATES
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

        Utils.setContext(this)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStop() {
        SettingApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        mService = service
        runOnUiThread { invalidateOptionsMenu() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.xp_settings_menu, menu)
        menu.findItem(R.id.menu_disable_auto_check).isVisible = false
        menu.findItem(R.id.menu_check_beta_updates).isVisible = false
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val aliasName = ComponentName(this, SettingsActivity::class.java.name + "Alias")
        menu.findItem(R.id.menu_hide_icon).isChecked =
            packageManager.getComponentEnabledSetting(aliasName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        val menuDisableAutoCheck = menu.findItem(R.id.menu_disable_auto_check)
        val menuCheckBetaUpdates = menu.findItem(R.id.menu_check_beta_updates)
        try {
            val prefs = mService!!.getRemotePreferences("prefs")
            menuDisableAutoCheck.isChecked =
                prefs.getBoolean("disable_auto_check_update", false)
            menuDisableAutoCheck.isVisible = true
            menuCheckBetaUpdates.isChecked = prefs.getBoolean(CHECK_BETA_UPDATES, false)
            menuCheckBetaUpdates.isVisible = true
        } catch (_: Throwable) {
            menuDisableAutoCheck.isVisible = false
            menuCheckBetaUpdates.isVisible = false
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

            R.id.menu_disable_auto_check, R.id.menu_check_beta_updates -> {
                val newChecked = !item.isChecked
                item.isChecked = newChecked
                val key = if (item.itemId == R.id.menu_check_beta_updates) CHECK_BETA_UPDATES
                else "disable_auto_check_update"
                mService!!.getRemotePreferences("prefs")
                    .edit().putBoolean(key, newChecked).apply()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAndRemoveTask()
        exitProcess(0)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    class SettingsFragment : PreferenceFragment(), SettingApplication.ServiceStateListener {
        private var mService: XposedService? = null
        private var autoUpdateChecked = false
        private lateinit var updateDownload: UpdateDownload
        private val updateChecker = UpdateChecker(onUpdateAvailable = { release, versionName, betaUpdates, startImmediately ->
            updateDownload.show(release, versionName, betaUpdates, startImmediately)
        }) {
            mService?.getRemotePreferences("prefs")?.getBoolean(CHECK_BETA_UPDATES, false) ?: false
        }

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
            updateChecker.setActivity(activity)
            updateDownload = UpdateDownload(activity)

            val rootScreen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = rootScreen

            Preference(context).apply {
                setSummary(R.string.slogan_summary)
                isEnabled = false
                rootScreen.addPreference(this)
            }

            Utils.setContext(context)

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
                    updateChecker.checkUpdate(silent = false)
                    true
                }
                rootScreen.addPreference(this)
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

        override fun onResume() {
            super.onResume()
            updateDownload.resume()
            val intent = activity.intent
            if (intent.action == ACTION_CHECK_MODULE_UPDATE) {
                intent.action = null
                autoUpdateChecked = true
                updateChecker.checkUpdate(
                    silent = false,
                    requestedBetaUpdates = intent.getBooleanExtra(CHECK_BETA_UPDATES, false),
                    startImmediately = intent.component?.className == UPDATE_SETTINGS_ALIAS
                )
            }
        }

        override fun onPause() {
            updateDownload.pause()
            super.onPause()
        }

        override fun onDestroy() {
            updateDownload.close()
            super.onDestroy()
        }

        override fun onStop() {
            SettingApplication.removeServiceStateListener(this)
            super.onStop()
        }

        override fun onServiceStateChanged(service: XposedService?) {
            mService = service

            activity?.runOnUiThread {
                if (service == null) {
                    updateDynamicUI(false)
                    return@runOnUiThread
                }

                val isModuleActivated: Boolean = try {
                    service.getRemotePreferences("prefs")
                    service.apiVersion
                    true
                } catch (_: Throwable) {
                    false
                }

                updateDynamicUI(isModuleActivated)
                // Wait for remote preferences before choosing the update channel.
                if (isModuleActivated && !autoUpdateChecked &&
                    activity.intent.action != ACTION_CHECK_MODULE_UPDATE
                ) {
                    autoUpdateChecked = true
                    updateChecker.autoCheckUpdate()
                }
            }
        }
    }
}
