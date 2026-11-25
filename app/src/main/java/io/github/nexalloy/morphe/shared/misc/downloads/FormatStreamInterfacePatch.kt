package io.github.nexalloy.morphe.shared.misc.downloads

import app.morphe.extension.shared.patches.BaseInAppDownloadsPatch.FormatStreamInterface
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method

private lateinit var videoId: Field
private lateinit var itag: Method
private lateinit var width: Method
private lateinit var height: Method
private lateinit var mimeType: Method
private lateinit var xtags: Method

internal class FormatStreamProxy(private val stream: Any) : FormatStreamInterface {
    override fun patch_getVideoId() = videoId.get(stream) as String?
    override fun patch_getItag() = itag.invoke(stream) as Int
    override fun patch_getWidth() = width.invoke(stream) as Int
    override fun patch_getHeight() = height.invoke(stream) as Int
    override fun patch_getMimeType() = mimeType.invoke(stream) as String?
    override fun patch_getXtags() = xtags.invoke(stream) as String?
}

internal val formatStreamInterfacePatch = patch {
    videoId = ::formatStreamVideoIdField.field
    itag = ::formatStreamItagMethod.method
    width = ::formatStreamWidthMethod.method
    height = ::formatStreamHeightMethod.method
    mimeType = ::formatStreamMimeTypeMethod.method
    xtags = ::formatStreamXtagsMethod.method
}
