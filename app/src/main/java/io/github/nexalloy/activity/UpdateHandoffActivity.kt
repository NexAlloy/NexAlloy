package io.github.nexalloy.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import io.github.nexalloy.appPatchConfigurations
import io.github.nexalloy.common.ACTION_CHECK_MODULE_UPDATE
import io.github.nexalloy.common.CHECK_BETA_UPDATES

internal const val UPDATE_SETTINGS_ALIAS = "io.github.nexalloy.activity.UpdateSettingsAlias"

class UpdateHandoffActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = callingPackage
        if (intent.action == ACTION_CHECK_MODULE_UPDATE &&
            appPatchConfigurations.any { it.packageName == caller }
        ) {
            startActivity(Intent(ACTION_CHECK_MODULE_UPDATE).apply {
                component = ComponentName(packageName, UPDATE_SETTINGS_ALIAS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CHECK_BETA_UPDATES, intent.getBooleanExtra(CHECK_BETA_UPDATES, false))
            })
        }
        finish()
    }
}
