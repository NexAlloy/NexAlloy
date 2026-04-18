package io.github.nexalloy.common

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fuel.Fuel
import fuel.get
import io.github.nexalloy.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readString
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body_html") val releaseNoteHtml: String,
    @SerializedName("html_url") val releaseUrl: String
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

class UpdateChecker() : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, err ->
            Logger.printException({ "coroutineContext error" }, err)
        }

    private var currentActivity = WeakReference<Activity>(null)
    private lateinit var latestVersionInfo: VersionInfo
    private lateinit var latestRelease: ReleaseInfo

    lateinit var unhook: XC_MethodHook.Unhook

    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun hookNewActivity() {
        unhook = XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "newActivity",
            ClassLoader::class.java,
            String::class.java,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentActivity = WeakReference(param.result as Activity)
                    autoCheckUpdate()
                    unhook.unhook()
                }
            })
    }

    fun autoCheckUpdate() {
        if (Random.nextInt(0, 10) != 0) return
        Logger.printInfo { "start auto check update." }
        runCatching { checkUpdate() }
    }

    fun checkUpdate(silent: Boolean = true) {
        launch {
            try {
                val response = Fuel.get(
                    "https://api.github.com/repos/$OWNER/$REPO/releases/latest",
                    headers = mapOf("Accept" to "application/vnd.github.html+json")
                )
                if (response.statusCode != 200) {
                    Logger.printException { "Failed to fetch latest release: HTTP ${response.statusCode}" }
                    return@launch
                }

                val content = response.source.readString()
                Logger.printDebug { content }
                latestRelease = Gson().fromJson(content, ReleaseInfo::class.java)
                latestVersionInfo = VersionInfo.fromTagName(latestRelease.tagName)
                Logger.printDebug { "$latestVersionInfo" }
                if (latestVersionInfo.versionCode > currentVersionCode) {
                    Logger.printInfo { "Found new version of NexAlloy ${latestRelease.tagName}" }
                    showUpdateDialog()
                } else {
                    Logger.printInfo { "no update found for NexAlloy" }
                    if (!silent) Utils.showToastLong("NexAlloy is up to date.")
                }
            } catch (e: Throwable) {
                Logger.printException({ "checkUpdate error" }, e)
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

            latestRelease = Gson().fromJson(content, ReleaseInfo::class.java)
            latestVersionInfo = VersionInfo.fromTagName(latestRelease.tagName)
            showUpdateDialog()
        }
    }

    fun requireActivity() = currentActivity.get()!!

    private fun showUpdateDialog() {
        launch(Dispatchers.Main) {
            try {
                val theme =
                    if (Utils.isDarkModeEnabled()) R.style.Theme_DeviceDefault_Dialog_Alert
                    else R.style.Theme_DeviceDefault_Light_Dialog_Alert
                val dialog = AlertDialog.Builder(requireActivity(), theme)
                    .setTitle("Found new version of NexAlloy ${latestVersionInfo.versionName}")
                    .setMessage(
                        Html.fromHtml(latestRelease.releaseNoteHtml, Html.FROM_HTML_MODE_LEGACY)
                    ).setPositiveButton(R.string.ok) { _, _ ->
                        openReleasePage()
                    }.setNegativeButton(requireActivity().getString(R.string.cancel), null)
                    .create()
                dialog.show()
                dialog.findViewById<TextView>(R.id.message).movementMethod =
                    LinkMovementMethod.getInstance()
            } catch (e: Throwable) {
                Logger.printException({ "showUpdateDialog error" }, e)
            }
        }
    }

    private fun openReleasePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestRelease.releaseUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireActivity().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.showToastLong(e.message.toString())
        }
    }
}
