package io.github.nexalloy

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.theme.ThemeUtils
import app.morphe.extension.shared.ui.CustomDialog
import de.robv.android.xposed.XposedBridge

/** Draws on the host's pending launch token while Application.onCreate blocks the main looper. */
internal class InitializationProgress private constructor(
    private val app: Application,
    private val total: Int
) : AutoCloseable {
    private data class State(val completed: Int = 0, val patchName: String? = null)

    @Volatile private var state = State()
    @Volatile private var handler: Handler? = null
    @Volatile private var closed = false
    private var started = false
    private var activePatchName: String? = null

    // Views and rendered state belong to the UI thread.
    private lateinit var context: Context
    private var dialog: Dialog? = null
    private var content: View? = null
    private var renderedState: State? = null

    private val thread = object : HandlerThread(
        "NexAlloy initialization UI", Process.THREAD_PRIORITY_BACKGROUND
    ) {
        override fun onLooperPrepared() {
            // Do not call getLooper() from the scanning thread: it waits for this setup.
            handler = Handler(Looper.myLooper()!!).also { it.post(refreshProgress) }
        }
    }

    private val refreshProgress = object : Runnable {
        override fun run() {
            if (closed) {
                stopUi()
                return
            }
            runCatching {
                if (dialog == null) {
                    pendingLaunchToken()?.let { show(it) }
                } else {
                    render()
                }
            }.onFailure {
                XposedBridge.log(it)
                closed = true
            }
            if (closed) stopUi() else handler?.postDelayed(this, 100)
        }
    }

    fun update(completed: Int) {
        state = state.copy(completed = completed)
        if (!started && completed > 0 && !closed) {
            // Let the first patch initialize shared extension resources before UI work starts.
            started = true
            thread.start()
        }
    }

    fun setPatchName(name: String?): String? {
        val previous = activePatchName
        activePatchName = name?.takeIf { it.isNotBlank() }
        activePatchName?.let { state = state.copy(patchName = it) }
        return previous
    }

    private fun render() {
        val current = state
        if (current == renderedState) return
        val view = content ?: return
        if (current.completed != renderedState?.completed) {
            view.findViewById<ProgressBar>(R.id.initialization_progress).progress = current.completed
            view.findViewById<TextView>(R.id.initialization_count).text =
                context.getString(R.string.initialization_count, current.completed, total)
        }
        // Keep the last label between named patches and while saving the cache.
        current.patchName?.takeIf { it != renderedState?.patchName }?.let { name ->
            view.findViewById<TextView>(R.id.initialization_status).apply {
                text = context.getString(R.string.initialization_applying_patch, name)
                visibility = View.VISIBLE
            }
        }
        renderedState = current
    }

    private fun show(token: IBinder) {
        val display = app.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
        val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            app.createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION, null)
        } else {
            app.createDisplayContext(display)
        }
        windowContext.addModuleAssets()
        // Unlike an Activity, this window context needs a dialog theme. MainTheme's
        // fitsSystemWindows would replace CustomDialog's content padding with insets.
        val theme = if (Utils.isDarkModeEnabled()) {
            android.R.style.Theme_Material_Dialog
        } else {
            android.R.style.Theme_Material_Light_Dialog
        }
        context = ContextThemeWrapper(windowContext, theme)
        if (closed) return
        val created = CustomDialog.create(
            context, context.getString(R.string.initialization_title), null, null,
            null, null, null, null, null, false
        )
        val container = created.second
        val view = View.inflate(context, R.layout.initialization_progress, null)
        container.addView(view, container.childCount - 1)
        view.findViewById<TextView>(R.id.initialization_status)
            .setTextColor(ThemeUtils.getDialogTextColor())
        view.findViewById<TextView>(R.id.initialization_count)
            .setTextColor(ThemeUtils.getDialogTextColor())
        view.findViewById<ProgressBar>(R.id.initialization_progress).apply {
            max = total.coerceAtLeast(1)
            val foreground = ThemeUtils.getAppForegroundColor()
            progressTintList = ColorStateList.valueOf(foreground)
            progressBackgroundTintList = ColorStateList.valueOf((foreground and 0x00FFFFFF) or 0x33000000)
        }
        content = view
        dialog = created.first
        created.first.apply {
            setCanceledOnTouchOutside(false)
            window!!.attributes = window!!.attributes.apply {
                this.token = token
                type = WindowManager.LayoutParams.TYPE_APPLICATION
                title = "NexAlloy initialization"
            }
            render()
            if (!closed) show()
        }
    }

    /** Read pending launch messages only; never dispatch host lifecycle work before hooks are ready. */
    private fun pendingLaunchToken(): IBinder? {
        val queue = Looper.getMainLooper().queue
        synchronized(queue) {
            // Android 17's DeliQueue stores the message chain in MessageStack instead.
            var message = (queue.getObjectFieldOrNull("mMessages")
                ?: queue.getObjectFieldOrNull("mStack")?.getObjectFieldOrNull("mTopValue")) as? Message
            while (message != null) {
                val transaction = message.obj
                when (transaction?.javaClass?.name) {
                    "android.app.servertransaction.ClientTransaction" -> {
                        val items = (transaction.callMethodOrNull("getTransactionItems")
                            ?: transaction.callMethodOrNull("getCallbacks")) as? List<*>
                        val launch = items?.firstOrNull {
                            it?.javaClass?.name == "android.app.servertransaction.LaunchActivityItem"
                        }
                        if (launch != null) {
                            return (launch.callMethodOrNull("getActivityToken")
                                ?: transaction.callMethodOrNull("getActivityToken")) as? IBinder
                        }
                    }
                    "android.app.ActivityThread\$ActivityClientRecord" ->
                        return transaction.getObjectField("token") as? IBinder
                }
                message = message.getObjectField("next") as? Message
            }
        }
        return null
    }

    override fun close() {
        closed = true
        handler?.post(refreshProgress)
    }

    private fun stopUi() {
        try {
            dialog?.dismiss()
            if (dialog != null) {
                // Dispose this looper's vsync receiver before shutting down its queue.
                Choreographer::class.java.callStaticMethod("releaseInstance")
            }
        } catch (error: Exception) {
            XposedBridge.log(error)
        } finally {
            dialog = null
            content = null
            handler = null
            thread.quitSafely()
        }
    }

    companion object {
        fun start(app: Application, total: Int) = InitializationProgress(app, total)
    }
}
