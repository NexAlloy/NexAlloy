package io.github.nexalloy.morphe.youtube.video.format

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodFromToString
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.shared.FormatStreamModelToStringFingerprint

internal object VideoStreamingDataToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(string("VideoStreamingData(itags="))
)

internal object VideoStreamingDataConstructorFingerprint : Fingerprint(
    classFingerprint = VideoStreamingDataToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass\$StreamingData;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "Ljava/lang/String;"),
        newInstance("Ljava/util/ArrayList;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass\$StreamingData;")
    )
)

context(dexkit: org.luckypray.dexkit.DexKitBridge)
private fun formatField(name: String) = FormatStreamModelToStringFingerprint().let { model ->
    val method = model.findMethodFromToString(name)
    Fingerprint(
        classFingerprint = FormatStreamModelToStringFingerprint,
        name = method.name,
        returnType = method.returnType!!.descriptor,
        filters = listOf(
            fieldAccess(opcode = Opcode.IGET_OBJECT, reference = FormatStreamModelToStringFingerprint.instructionMatches[5].instruction.fieldRef!!),
            fieldAccess(type = method.returnType!!.descriptor, location = MatchAfterImmediately())
        )
    ).instructionMatches[1].instruction.fieldRef!!
}

internal val formatWidthField = findFieldDirect { formatField("width") }
internal val formatHeightField = findFieldDirect { formatField("height") }
internal val formatMimeTypeField = findFieldDirect { formatField("mimeType") }
internal val formatItagField = findFieldDirect { formatField("itag") }
internal val streamingVideoIdField = findFieldDirect {
    VideoStreamingDataConstructorFingerprint.instructionMatches[1].instruction.fieldRef!!
}
internal val adaptiveFormatsField = findFieldDirect {
    VideoStreamingDataConstructorFingerprint.instructionMatches.last().instruction.fieldRef!!
}
internal val currentFormatItagMethod = findMethodDirect {
    val model = FormatStreamModelToStringFingerprint()
    model.findMethodFromToString("itag")
}
