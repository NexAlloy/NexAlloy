package io.github.nexalloy.activity

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

class SettingApplication : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        @Volatile
        var mService: XposedService? = null
            private set

        private val mainHandler = Handler(Looper.getMainLooper())
        private val services = LinkedHashSet<XposedService>()
        private val serviceStateListeners = CopyOnWriteArraySet<ServiceStateListener>()

        private fun runOnMain(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                block()
            } else {
                mainHandler.post(block)
            }
        }

        private fun dispatchServiceState(
            listener: ServiceStateListener,
            service: XposedService?
        ) {
            runOnMain {
                if (serviceStateListeners.contains(listener)) {
                    listener.onServiceStateChanged(service)
                }
            }
        }

        fun addServiceStateListener(
            listener: ServiceStateListener,
            notifyImmediately: Boolean
        ) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                dispatchServiceState(listener, mService)
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }

        private fun notifyServiceStateChanged(service: XposedService?) {
            for (listener in serviceStateListeners) {
                dispatchServiceState(listener, service)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    override fun onServiceBind(service: XposedService) {
        runOnMain {
            services.remove(service)
            services.add(service)
            if (mService !== service) {
                mService = service
                notifyServiceStateChanged(service)
            }
        }
    }

    override fun onServiceDied(service: XposedService) {
        runOnMain {
            if (!services.remove(service) || mService !== service) return@runOnMain
            mService = services.lastOrNull()
            notifyServiceStateChanged(mService)
        }
    }
}
