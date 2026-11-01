package io.github.nexalloy.morphe.youtube.video.format

import app.morphe.extension.youtube.patches.VideoFormat.FormatInterface
import io.github.nexalloy.patch
import java.lang.reflect.Field

private lateinit var width: Field
private lateinit var height: Field
private lateinit var mimeType: Field
private lateinit var itag: Field

internal class FormatProxy(private val format: Any) : FormatInterface {
    override fun patch_getWidth() = width.getInt(format)
    override fun patch_getHeight() = height.getInt(format)
    override fun patch_getMimeType() = mimeType.get(format) as String?
    override fun patch_getItag() = itag.getInt(format)
}

private val adaptiveFormatHooks = mutableListOf<(String, List<FormatInterface>) -> Unit>()
internal fun hookAdaptiveFormat(hook: (String, List<FormatInterface>) -> Unit) { adaptiveFormatHooks += hook }

internal val videoFormatPatch = patch {
    width = ::formatWidthField.field
    height = ::formatHeightField.field
    mimeType = ::formatMimeTypeField.field
    itag = ::formatItagField.field
    val videoId = ::streamingVideoIdField.field
    val formats = ::adaptiveFormatsField.field
    VideoStreamingDataConstructorFingerprint.hookMethod {
        after {
            // These callbacks observe formats; construction does not change the source proto list.
            val id = videoId.get(it.thisObject) as String?
            val adaptive = formats.get(it.args[0]) as List<*>
            if (!id.isNullOrEmpty() && id != "zzzzzzzzzzz" && adaptive.isNotEmpty()) {
                val proxies = adaptive.map { format -> FormatProxy(format!!) }
                adaptiveFormatHooks.forEach { hook -> hook(id, proxies) }
            }
        }
    }
}
