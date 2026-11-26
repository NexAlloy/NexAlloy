package io.github.nexalloy.common

import android.app.Activity
import android.app.Dialog
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.util.Pair
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.ui.CustomDialog
import app.morphe.extension.shared.ui.Dim
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XposedHelpers
import fuel.Fuel
import fuel.get
import io.github.nexalloy.BuildConfig
import io.github.nexalloy.R
import io.github.nexalloy.activity.UpdateHandoffActivity
import io.github.nexalloy.hookMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.readString
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body_html") val releaseNoteHtml: String,
    @SerializedName("html_url") val releaseUrl: String,
    @SerializedName("assets") val assets: List<ReleaseAsset> = emptyList()
) {
    fun getApkAsset(betaUpdates: Boolean): ReleaseAsset? = assets.singleOrNull {
        if (betaUpdates) nightlyApkName.matches(it.name) else it.name == "NexAlloy-$tagName.apk"
    }

    fun getUpdateVersion(
        checkBetaUpdates: Boolean,
        currentVersionCode: Int,
        currentCommit: String
    ): String? {
        if (checkBetaUpdates) {
            val apk = checkNotNull(getApkAsset(true)) {
                "Expected exactly one NexAlloy APK asset in autobuild"
            }
            val commit = apk.name.removePrefix("NexAlloy-").removeSuffix(".apk")
            return "Nightly build ($commit)".takeIf { commit != currentCommit }
        }

        val version = VersionInfo.fromTagName(tagName)
        return version.versionName.takeIf { version.versionCode > currentVersionCode }
    }

    private companion object {
        val nightlyApkName = Regex("""NexAlloy-.+\.apk""")
    }
}

data class ReleaseAsset(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class VersionInfo(val versionCode: Int, val versionName: String) {
    companion object {
        fun fromTagName(tagName: String): VersionInfo {
            val versionCode: Int
            val versionName: String

            val split = tagName.split('-', limit = 2)
            if (split.count() == 2) {
                // VersionCode-VersionName
                versionCode = split[0].toIntOrNull() ?: 0
                versionName = split[1]
            } else {
                // X.Y.Z, Z is versionCode
                versionCode = tagName.split('.').last().toIntOrNull() ?: 0
                versionName = tagName
            }
            return VersionInfo(versionCode, versionName)
        }
    }
}

const val OWNER = "NexAlloy"
const val REPO = "NexAlloy"
const val currentVersionCode = BuildConfig.VERSION_CODE
const val CHECK_BETA_UPDATES = "check_beta_updates"
const val ACTION_CHECK_MODULE_UPDATE = "io.github.nexalloy.action.CHECK_MODULE_UPDATE"

class UpdateChecker(
    private val onUpdateAvailable: ((ReleaseInfo, String, Boolean, Boolean) -> Unit)? = null,
    private val checkBetaUpdates: () -> Boolean = { false }
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, err ->
            Logger.printException({ "coroutineContext error" }, err)
        }

    private var currentActivity = WeakReference<Activity>(null)
    private var checkJob: Job? = null

    var runOnce = false

    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun hookNewActivity() {

        XposedHelpers.findMethodExact(
            Instrumentation::class.java,
            "newActivity",
            ClassLoader::class.java,
            String::class.java,
            Intent::class.java
        ).hookMethod {
            after {
                if (!runOnce) {
                    runOnce = true
                    currentActivity = WeakReference(it.result as Activity)
                    autoCheckUpdate()
                }
            }
        }
    }

    fun autoCheckUpdate() {
        if (Random.nextInt(0, 10) != 0) return
        Logger.printInfo { "start auto check update." }
        runCatching { checkUpdate() }
    }

    fun checkUpdate(
        silent: Boolean = true,
        requestedBetaUpdates: Boolean? = null,
        startImmediately: Boolean = false
    ) {
        if (silent && checkJob?.isActive == true) return
        checkJob?.cancel()
        checkJob = launch {
            try {
                val betaUpdates = requestedBetaUpdates ?: checkBetaUpdates()
                val releasePath = if (betaUpdates) "tags/autobuild" else "latest"
                val response = Fuel.get(
                    "https://api.github.com/repos/$OWNER/$REPO/releases/$releasePath",
                    headers = mapOf("Accept" to "application/vnd.github.html+json")
                )
                if (response.statusCode != 200) {
                    error("Failed to fetch latest release: HTTP ${response.statusCode}")
                }

                val content = response.source.readString()
                Logger.printDebug { content }
                val release = Gson().fromJson(content, ReleaseInfo::class.java)
                val updateVersion = release.getUpdateVersion(
                    betaUpdates, currentVersionCode, BuildConfig.COMMIT_HASH
                )
                if (updateVersion != null) {
                    Logger.printInfo { "Found new version of NexAlloy $updateVersion" }
                    showUpdateDialog(release, updateVersion, betaUpdates, startImmediately)
                } else {
                    Logger.printInfo { "no update found for NexAlloy" }
                    if (!silent) Utils.showToastLong("NexAlloy is up to date.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.printException({ "checkUpdate error" }, e)
                if (!silent) Utils.showToastLong("Could not check for a NexAlloy update.")
            }
        }
    }

    @Deprecated("Test only.")
    fun showRelease(version: String) {
        launch {
            val response = Fuel.get(
                "https://api.github.com/repos/$OWNER/$REPO/releases/tags/$version",
                headers = mapOf("Accept" to "application/vnd.github.html+json")
            )
            if (response.statusCode != 200) {
                Logger.printException { "responseCode ${response.statusCode}" }
                return@launch
            }

            val content = response.source.readString()
            Logger.printDebug { content }

            val release = Gson().fromJson(content, ReleaseInfo::class.java)
            showUpdateDialog(
                release,
                VersionInfo.fromTagName(release.tagName).versionName,
                version == "autobuild"
            )
        }
    }

    private suspend fun showUpdateDialog(
        release: ReleaseInfo,
        versionName: String,
        betaUpdates: Boolean,
        startImmediately: Boolean = false
    ) {
        withContext(Dispatchers.Main) {
            val activity = currentActivity.get() ?: return@withContext
            if (activity.isFinishing || activity.isDestroyed) return@withContext
            try {
                if (onUpdateAvailable != null) {
                    onUpdateAvailable.invoke(release, versionName, betaUpdates, startImmediately)
                } else {
                    releaseDialog(activity, release, versionName, "Update now") {
                        openModule(activity, betaUpdates)
                    }.first.show()
                }
            } catch (e: Exception) {
                Logger.printException({ "showUpdateDialog error" }, e)
            }
        }
    }

    private fun openModule(activity: Activity, betaUpdates: Boolean) {
        try {
            val intent = Intent(ACTION_CHECK_MODULE_UPDATE).apply {
                component = ComponentName(BuildConfig.APPLICATION_ID, UpdateHandoffActivity::class.java.name)
                putExtra(CHECK_BETA_UPDATES, betaUpdates)
            }
            // The handoff uses the framework-reported calling package to authorize the update.
            @Suppress("DEPRECATION")
            activity.startActivityForResult(intent, 0x4B01)
        } catch (e: Exception) {
            Logger.printException({ "Could not open NexAlloy" }, e)
            Utils.showToastLong("Could not open NexAlloy.")
        }
    }
}

internal fun releaseDialog(
    activity: Activity,
    release: ReleaseInfo,
    versionName: String,
    primaryText: CharSequence? = null,
    onPrimaryClick: Runnable? = null
): Pair<Dialog, LinearLayout> =
    CustomDialog.create(
        activity,
        "Found new version of NexAlloy $versionName",
        Html.fromHtml(release.releaseNoteHtml, Html.FROM_HTML_MODE_LEGACY).trimEnd(),
        null,
        primaryText,
        onPrimaryClick,
        {},
        "GitHub",
        {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.releaseUrl)))
            } catch (e: Exception) {
                Logger.printException({ "Could not open the GitHub release page" }, e)
                Utils.showToastLong("Could not open the GitHub release page.")
            }
        },
        true
    ).withDialogInsets(activity)

internal fun updateProgressDialog(activity: Activity): Pair<Dialog, LinearLayout> =
    CustomDialog.create(
        activity, activity.getText(R.string.update_progress_title), null,
        null, null, null, {}, null, null, false
    ).withDialogInsets(activity)

private fun Pair<Dialog, LinearLayout>.withDialogInsets(activity: Activity): Pair<Dialog, LinearLayout> {
    val root = second
    val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    repeat(root.childCount) {
        val child = root.getChildAt(0)
        root.removeViewAt(0)
        content.addView(child)
    }
    // Some devices reset the dialog root's padding when the window is shown.
    root.setPadding(0, 0, 0, 0)
    root.addView(content, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
        setMargins(Dim.dp24, Dim.dp16, Dim.dp24, Dim.dp24)
    })
    return Pair(first, content)
}
