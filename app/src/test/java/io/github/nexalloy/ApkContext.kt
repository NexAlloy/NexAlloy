package io.github.nexalloy

import io.github.nexalloy.morphe.ResourceFinder
import io.github.nexalloy.morphe.resourceMappings
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.security.JadxSecurityFlag
import jadx.api.security.impl.JadxSecurity
import jadx.core.utils.android.AndroidManifestParser
import jadx.core.utils.android.AppAttribute
import org.luckypray.dexkit.DexKitBridge
import java.io.File
import java.util.EnumSet

class ApkContext(apkPath: String) : AutoCloseable {
    val dexkit: DexKitBridge
    val jadx: JadxDecompiler
    val appVersion: AppVersion

    init {
        dexkit = setupDexKit(apkPath)
        jadx = try {
            setupJadx(apkPath)
        } catch (e: Throwable) {
            e.suppressCleanup { dexkit.close() }
            throw e
        }
        appVersion = try {
            jadx.getAppVersion()
        } catch (e: Throwable) {
            e.suppressCleanup { dexkit.close() }
            e.suppressCleanup { jadx.close() }
            throw e
        }
    }

    companion object {
        val jadxResourceReader = ThreadLocal<JadxResourceReader>()

        init {
            resourceMappings = object : ResourceFinder {
                override operator fun get(type: String, name: String): Int =
                    jadxResourceReader.get()!![type, name]
            }
        }
    }

    fun setupCurrentThread() {
        jadxResourceReader.set(JadxResourceReader(jadx))
    }

    private fun setupDexKit(apkPath: String): DexKitBridge {
        try {
            System.loadLibrary("dexkit")
        } catch (_: UnsatisfiedLinkError) {
            System.loadLibrary("libdexkit")
        }
        return DexKitBridge.create(apkPath)
    }

    private fun setupJadx(apkPath: String): JadxDecompiler {
        val jadxArgs = JadxArgs().apply {
            setInputFile(File(apkPath))
            security = JadxSecurity(JadxSecurityFlag.none())
        }
        val jadx = JadxDecompiler(jadxArgs)
        try {
            jadx.load()
            return jadx
        } catch (e: Throwable) {
            e.suppressCleanup { jadx.close() }
            throw e
        }
    }

    private fun JadxDecompiler.getAppVersion(): AppVersion {
        val manifest = AndroidManifestParser(
            AndroidManifestParser.getAndroidManifest(resources),
            EnumSet.of(AppAttribute.VERSION_NAME),
            JadxSecurity(JadxSecurityFlag.none())
        )
        return AppVersion(manifest.parse().versionName)
    }

    override fun close() {
        jadxResourceReader.remove()
        var failure: Throwable? = null
        try {
            dexkit.close()
        } catch (e: Throwable) {
            failure = e
        }
        try {
            jadx.close()
        } catch (e: Throwable) {
            if (failure == null) {
                failure = e
            } else {
                failure.addSuppressed(e)
            }
        }
        failure?.let { throw it }
    }
}

private inline fun Throwable.suppressCleanup(block: () -> Unit) {
    try {
        block()
    } catch (cleanupError: Throwable) {
        addSuppressed(cleanupError)
    }
}
