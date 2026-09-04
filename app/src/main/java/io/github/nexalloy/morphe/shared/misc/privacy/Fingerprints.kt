package io.github.nexalloy.morphe.shared.misc.privacy

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

internal object YouTubeCopyTextFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        string("text/plain", location = MatchAfterWithin(2)),
        methodCall(
            smali = "Landroid/content/ClipData;->newPlainText(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Landroid/content/ClipData;",
            location = MatchAfterWithin(2)
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterWithin(2)),
        methodCall(
            smali = "Landroid/content/ClipboardManager;->setPrimaryClip(Landroid/content/ClipData;)V",
            location = MatchAfterWithin(2)
        )
    )
)

internal object YouTubeSystemShareSheetFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(
            smali = "Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;"
        ),
        methodCall(
            smali = "Ljava/util/List;->iterator()Ljava/util/Iterator;",
            location = MatchAfterWithin(4)
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterWithin(15)
        ),
        methodCall(
            smali = "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
            location = MatchAfterWithin(15)
        )
    )
)

internal object YouTubeShareSheetFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        checkCast("Ljava/lang/String;", location = MatchAfterImmediately()),
        opcode(Opcode.GOTO, location = MatchAfterImmediately()),
        methodCall(
            smali = "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;"
        ),
        string("YTShare_Logging_Share_Intent_Endpoint_Byte_Array")
    ),
    custom = {
        paramCount(1, 2)
    }
)
