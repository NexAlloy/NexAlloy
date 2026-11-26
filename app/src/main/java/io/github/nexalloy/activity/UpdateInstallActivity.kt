@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import app.morphe.extension.shared.Logger
import io.github.nexalloy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateInstallActivity : Activity() {
    private val assetId by lazy {
        intent.getLongExtra(EXTRA_UPDATE_ASSET_ID, 0)
    }
    private var requestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestingPermission = savedInstanceState?.getBoolean("requestingPermission") ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("requestingPermission", requestingPermission)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return
        if (assetId <= 0 || !updateApk(this, assetId).isFile) {
            finish()
            return
        }
        UpdateInstaller.foregroundActivity = this
        try {
            continueInstall()
        } catch (e: Exception) {
            Logger.printException({ "Could not open update installer" }, e)
            showFailure()
        }
    }

    override fun onPause() {
        if (UpdateInstaller.foregroundActivity === this) UpdateInstaller.foregroundActivity = null
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_ACCESS) {
            requestingPermission = false
            if (!packageManager.canRequestPackageInstalls()) finish()
        }
    }

    private fun continueInstall() {
        if (intent.action == ACTION_CONFIRM_INSTALL) {
            val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, NO_SESSION)
            val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (sessionId == UpdateInstaller.sessionId(this, assetId) && confirmation != null) {
                if (!launchConfirmation(assetId, confirmation)) showFailure()
            } else {
                finish()
            }
            return
        }
        if (!packageManager.canRequestPackageInstalls()) {
            if (!requestingPermission) {
                requestingPermission = true
                startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.fromParts("package", packageName, null)), REQUEST_INSTALL_ACCESS)
            }
            return
        }

        val sessionId = UpdateInstaller.sessionId(this, assetId)
        val confirmation = confirmationIntent(this, assetId, sessionId, PendingIntent.FLAG_NO_CREATE)
        if (confirmation != null) {
            startIntentSenderForResult(confirmation.intentSender, -1, null, 0, 0, 0)
            finish()
            return
        }
        UpdateInstaller.start(applicationContext, assetId)
    }

    internal fun launchConfirmation(id: Long, confirmation: Intent): Boolean {
        if (isFinishing || id != assetId) return false
        return try {
            startActivity(confirmation)
            finish()
            true
        } catch (e: Exception) {
            Logger.printException({ "Could not launch installation confirmation" }, e)
            false
        }
    }

    internal fun onInstallFinished(id: Long, status: Int) {
        if (isFinishing || id != assetId) return
        if (status == PackageInstaller.STATUS_SUCCESS || status == PackageInstaller.STATUS_FAILURE_ABORTED) {
            finish()
        } else {
            showFailure()
        }
    }

    private fun showFailure() {
        Toast.makeText(this, R.string.update_install_failed, Toast.LENGTH_LONG).show()
        finish()
    }

    private companion object {
        const val REQUEST_INSTALL_ACCESS = 1
    }
}

private object UpdateInstaller {
    var foregroundActivity: UpdateInstallActivity? = null
    private var staging: Job? = null

    fun start(context: Context, assetId: Long) {
        if (staging?.isActive == true) return
        val installer = context.packageManager.packageInstaller
        staging = CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    stageUpdate(context, installer, assetId)
                }
            } catch (e: Exception) {
                Logger.printException({ "Could not stage update" }, e)
                foregroundActivity?.onInstallFinished(assetId, PackageInstaller.STATUS_FAILURE)
            }
        }
    }

    private fun stageUpdate(
        context: Context,
        installer: PackageInstaller,
        assetId: Long
    ) {
        val existing = installer.getSessionInfo(sessionId(context, assetId))
        if (existing?.isSealed == true) return
        installer.mySessions.filter { it.appPackageName == context.packageName }.forEach {
            installer.abandonSession(it.sessionId)
        }
        val apk = updateApk(context, assetId)
        check(apk.isFile) { "The downloaded APK is missing" }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                recordSession(context, assetId, sessionId)
                val callback = Intent(context, UpdateInstallStatusReceiver::class.java)
                    .putExtra(EXTRA_UPDATE_ASSET_ID, assetId)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val status = PendingIntent.getBroadcast(context, sessionId, callback, flags)
                session.commit(status.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            clearSession(context, assetId, sessionId)
            throw e
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("module_update_install", Context.MODE_PRIVATE)

    @Synchronized
    private fun recordSession(context: Context, assetId: Long, sessionId: Int) {
        check(preferences(context).edit().putLong(EXTRA_UPDATE_ASSET_ID, assetId)
            .putInt(PackageInstaller.EXTRA_SESSION_ID, sessionId).commit())
    }

    fun sessionId(context: Context, assetId: Long): Int = with(preferences(context)) {
        if (getLong(EXTRA_UPDATE_ASSET_ID, 0) == assetId) {
            getInt(PackageInstaller.EXTRA_SESSION_ID, NO_SESSION)
        } else {
            NO_SESSION
        }
    }

    @Synchronized
    fun clearSession(context: Context, assetId: Long, sessionId: Int) {
        if (sessionId(context, assetId) == sessionId) preferences(context).edit().clear().apply()
    }
}

class UpdateInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val assetId = intent.getLongExtra(EXTRA_UPDATE_ASSET_ID, 0)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, NO_SESSION)
        if (assetId <= 0 || sessionId == NO_SESSION ||
            sessionId != UpdateInstaller.sessionId(context, assetId)
        ) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (confirmation != null) {
                // Keep the system confirmation available when the app returns to the foreground.
                confirmationIntent(context, assetId, sessionId, PendingIntent.FLAG_UPDATE_CURRENT, confirmation)
                UpdateInstaller.foregroundActivity?.launchConfirmation(assetId, confirmation)
                return
            }
        }

        confirmationIntent(context, assetId, sessionId, PendingIntent.FLAG_NO_CREATE)?.cancel()
        UpdateInstaller.clearSession(context, assetId, sessionId)
        if (status == PackageInstaller.STATUS_SUCCESS) {
            updateApk(context, assetId).delete()
        } else {
            Logger.printInfo { "Update install status $status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}" }
        }
        UpdateInstaller.foregroundActivity?.onInstallFinished(assetId, status)
    }
}

private const val NO_SESSION = -1
private const val ACTION_CONFIRM_INSTALL = "io.github.nexalloy.action.CONFIRM_UPDATE_INSTALL"

private fun confirmationIntent(
    context: Context,
    assetId: Long,
    sessionId: Int,
    flags: Int,
    confirmation: Intent? = null
): PendingIntent? {
    if (sessionId == NO_SESSION) return null
    val intent = Intent(context, UpdateInstallActivity::class.java)
        .setAction(ACTION_CONFIRM_INSTALL)
        .putExtra(EXTRA_UPDATE_ASSET_ID, assetId)
        .putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
        .putExtra(Intent.EXTRA_INTENT, confirmation)
    return PendingIntent.getActivity(context, sessionId, intent, flags or PendingIntent.FLAG_IMMUTABLE)
}
