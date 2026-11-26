package io.github.nexalloy.activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ui.CustomDialog
import app.morphe.extension.shared.ui.Dim
import io.github.nexalloy.R
import io.github.nexalloy.common.ReleaseInfo
import io.github.nexalloy.common.releaseDialog
import io.github.nexalloy.common.updateProgressDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File

internal class UpdateDownload(private val activity: Activity) {
    private var dialog: Dialog? = null
    private var resumed = false
    private var pendingInstall: (() -> Unit)? = null

    fun show(release: ReleaseInfo, versionName: String, betaUpdates: Boolean, startImmediately: Boolean) {
        close()
        val asset = release.getApkAsset(betaUpdates)?.takeIf { it.id > 0 }
        val apk = asset?.let { updateApk(activity, it.id) }
        val dialogLayout = if (startImmediately) updateProgressDialog(activity)
        else releaseDialog(activity, release, versionName)
        val dialog = dialogLayout.first
        this.dialog = dialog

        var call: Call? = null
        var downloadJob: Job? = null
        // The built-in primary action dismisses the dialog. This button stays in place during the download.
        val button = CustomDialog.createButton(activity, null, activity.getText(R.string.update_now), null, true, false)
        dialogLayout.second.addView(button, LinearLayout.LayoutParams(MATCH_PARENT, Dim.dp36).apply {
            topMargin = Dim.dp8
        })

        fun showError(message: Int) {
            button.setText(R.string.update_retry_download)
            button.isEnabled = true
            button.alpha = 1f
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }

        fun startUpdate() {
            if (asset == null || apk == null) {
                showError(R.string.update_download_failed)
                return
            }

            button.isEnabled = false
            button.alpha = 0.6f
            downloadJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    if (!apk.isFile) {
                        button.setText(R.string.update_downloading)
                        val request = client.newCall(Request.Builder().url(asset.downloadUrl).build())
                        call = request
                        downloadApk(request, apk) { percent ->
                            button.text = activity.getString(R.string.update_download_progress, percent)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.printException({ "Update download failed" }, e)
                    showError(R.string.update_download_failed)
                    return@launch
                }
                if (!dialog.isShowing) return@launch
                button.setText(R.string.update_installing)
                val install = {
                    try {
                        activity.startActivity(Intent(activity, UpdateInstallActivity::class.java)
                            .putExtra(EXTRA_UPDATE_ASSET_ID, asset.id))
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Logger.printException({ "Could not open update installer" }, e)
                        showError(R.string.update_install_failed)
                    }
                }
                if (resumed) install() else pendingInstall = install
            }
        }

        button.setOnClickListener { startUpdate() }
        dialog.setOnDismissListener {
            downloadJob?.cancel()
            call?.cancel()
            if (this.dialog === dialog) {
                this.dialog = null
                pendingInstall = null
            }
        }
        dialog.show()
        if (startImmediately) startUpdate()
    }

    fun resume() {
        resumed = true
        val install = pendingInstall
        pendingInstall = null
        install?.invoke()
    }

    fun pause() {
        resumed = false
    }

    fun close() {
        pendingInstall = null
        dialog?.dismiss()
        dialog = null
    }

    private suspend fun downloadApk(call: Call, apk: File, onProgress: (Int) -> Unit) =
        withContext(Dispatchers.IO) {
            call.executeAsync().use { response ->
                check(response.isSuccessful) { "Update download HTTP ${response.code}" }
                val temp = File.createTempFile("nexalloy-update-", ".part", apk.parentFile)
                try {
                    val total = response.body.contentLength()
                    val input = response.body.byteStream()
                    var downloaded = 0L
                    var lastPercent = -1
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            if (total > 0) {
                                val percent = (100 * downloaded / total).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    withContext(Dispatchers.Main) { onProgress(percent) }
                                }
                            }
                        }
                    }
                    ensureActive()
                    check(downloaded > 0 && (total < 0 || downloaded == total)) { "Incomplete APK download" }
                    // Publish the cached APK only once the full response has been written.
                    check(temp.renameTo(apk)) { "Could not save the update APK" }
                } finally {
                    temp.delete()
                }
            }
        }

    private companion object {
        val client = OkHttpClient()
    }
}

internal const val EXTRA_UPDATE_ASSET_ID = "io.github.nexalloy.extra.UPDATE_ASSET_ID"

internal fun updateApk(context: Context, assetId: Long): File =
    File(context.cacheDir, "nexalloy-update-$assetId.apk")
