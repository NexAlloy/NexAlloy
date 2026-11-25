package io.github.nexalloy.morphe.shared.misc.downloads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodFromToString
import io.github.nexalloy.morphe.shared.FormatStreamModelToStringFingerprint

internal object FormatStreamModelConstructorFingerprint : Fingerprint(
    classFingerprint = FormatStreamModelToStringFingerprint,
    name = "<init>",
    parameters = listOf("L", "Ljava/lang/String;", "Z", "L"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "Ljava/lang/String;"))
)

internal val formatStreamVideoIdField = findFieldDirect {
    FormatStreamModelConstructorFingerprint.instructionMatches.first().instruction.fieldRef!!
}
internal val formatStreamItagMethod = findMethodDirect { FormatStreamModelToStringFingerprint().findMethodFromToString("itag") }
internal val formatStreamWidthMethod = findMethodDirect { FormatStreamModelToStringFingerprint().findMethodFromToString("width") }
internal val formatStreamHeightMethod = findMethodDirect { FormatStreamModelToStringFingerprint().findMethodFromToString("height") }
internal val formatStreamMimeTypeMethod = findMethodDirect { FormatStreamModelToStringFingerprint().findMethodFromToString("mimeType") }
internal val formatStreamXtagsMethod = findMethodDirect { FormatStreamModelToStringFingerprint().findMethodFromToString("xtags=") }
