@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.R as AndroidR
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import io.github.libxposed.service.XposedService
import io.github.nexalloy.R
import io.github.nexalloy.appPatchConfigurations

class AppPatchSettingsActivity : Activity() {

    companion object {
        const val ARGUMENT_APP_NAME = "app_name_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_patch_settings)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        val appName = intent.getStringExtra(ARGUMENT_APP_NAME)
        actionBar?.title = appName

        if (savedInstanceState != null) return
        val fragment = AppPatchSettingsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_APP_NAME, appName)
            }
        }
        fragmentManager.beginTransaction()
            .replace(R.id.app_patch_settings_container, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    class AppPatchSettingsFragment : PreferenceFragment(), SettingApplication.ServiceStateListener {

        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }

        private var mService: XposedService? = null

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
                activity.actionBar?.title = "Binder is null"
                return
            }

            activity.runOnUiThread {
                // Retrieve appName from the Activity's Intent extras
                val appName = arguments?.getString(ARGUMENT_APP_NAME)
                val appPatchInfo = appPatchConfigurations.find { it.appName == appName }
                if (appPatchInfo == null) throw Exception("AppPatchInfo not found, app_name: $appName")
                val defaultPatchStates = appPatchInfo.patches.associate { it.name to it.use }

                val screen = preferenceManager.createPreferenceScreen(context)

                val remotePrefs = service.getRemotePreferences(appPatchInfo.packageName)

                object : Preference(context) {
                    @Deprecated("Deprecated in Java")
                    override fun onBindView(view: View) {
                        super.onBindView(view)
                        view.findViewById<Button>(R.id.button_default).setOnClickListener {
                            restoreDefaultPreferences(remotePrefs, defaultPatchStates)
                        }
                        view.findViewById<Button>(R.id.button_none).setOnClickListener {
                            setAllPreferences(remotePrefs, false)
                        }
                        val isInstalled = runCatching {
                            context.packageManager.getPackageInfo(appPatchInfo.packageName, 0)
                        }.isSuccess

                        view.findViewById<Button>(R.id.button_app_info).apply {
                            if (!isInstalled) visibility = View.GONE
                            setOnClickListener {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:${appPatchInfo.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                    }
                }.apply {
                    layoutResource = R.layout.preference_header_buttons
                    screen.addPreference(this)
                }

                for (patchInfo in appPatchInfo.patches.sortedBy { it.name }) {
                    if (patchInfo.name == "") continue
                    if (patchInfo.name.startsWith("<")) continue
                    CheckBoxPreference(context).apply {
                        key = patchInfo.name // Pref Key
                        title = patchInfo.name
                        summary = patchInfo.description
                        isChecked = remotePrefs.getBoolean(patchInfo.name, patchInfo.use)

                        setOnPreferenceChangeListener { _, newValue ->
                            val enabled = newValue as Boolean
                            remotePrefs.edit().putBoolean(key, enabled).apply()

                            val vibrator =
                                context.getSystemService(VIBRATOR_SERVICE) as Vibrator?
                            if (vibrator?.hasVibrator() ?: false) {
                                vibrator.vibrate(50)
                            }
                            true
                        }
                        screen.addPreference(this)
                    }
                }

                preferenceScreen = screen
            }
        }

        fun setAllPreferences(prefs: SharedPreferences, enable: Boolean) {
            if (!isAdded) return
            val editor = prefs.edit()
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked = enable
                    editor.putBoolean(preference.key, enable)
                }
            }
            editor.apply()
        }

        fun restoreDefaultPreferences(prefs: SharedPreferences,defaultPatchStates: Map<String, Boolean>) {
            if (!isAdded) return
            val editor = prefs.edit()
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked =
                        defaultPatchStates[preference.key] ?: preference.isChecked
                    editor.putBoolean(preference.key, preference.isChecked)
                }
            }
            editor.apply()
        }
    }
}
