package io.github.nexalloy.revanced.spotify.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val RelaxAudioCapturePolicy = patch(
    name = "Allow audio capture",
    description = "Forces Spotify runtime audio-capture policy calls to allow capture. Manifest-level restrictions may still apply.",
    use = false,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val intType = Int::class.javaPrimitiveType!!

        runCatching {
            AudioAttributes.Builder::class.java
                .getDeclaredMethod("setAllowedCapturePolicy", intType)
                .hookMethod {
                    before { param ->
                        param.args[0] = AudioAttributes.ALLOW_CAPTURE_BY_ALL
                    }
                }
        }

        runCatching {
            AudioManager::class.java
                .getDeclaredMethod("setAllowedCapturePolicy", intType)
                .hookMethod {
                    before { param ->
                        param.args[0] = AudioAttributes.ALLOW_CAPTURE_BY_ALL
                    }
                }
        }
    }
}
