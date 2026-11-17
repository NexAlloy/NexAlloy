package io.github.nexalloy.morphe.youtube.layout.originaltitles

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string
import java.lang.reflect.Modifier

/**
 * Binds a video of the playlist panel (the queue of a playlist or mix on the watch page).
 * The panel has multiple layouts, each with its own bind method.
 */
@SkipTest
internal object PlaylistPanelVideoBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    filters = listOf(
        // Title view.
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/widget/TextView;"),
        string("PLAYLIST_CURRENT_VIDEO_MONITOR"),
        // Video id.
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterWithin(10)
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Binds the video information shown on the watch page while the video is loading,
 * which shows the title of the element that opened the video.
 */
@StrictQuery
internal object LoadingVideoInformationBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "channel_navigation_container"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.ID, "title", location = MatchAfterWithin(8)),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        resourceLiteral(ResourceType.ID, "collapsed_subtitle", location = MatchAfterWithin(8))
    )
)

/**
 * Creates the fullscreen engagement overlay, shown by swiping up in fullscreen.
 */
@StrictQuery
internal object FullscreenEngagementOverlayFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "fullscreen_engagement_overlay"),
        resourceLiteral(ResourceType.ID, "engagement_title"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Landroid/widget/TextView;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Creates the title view of the minimal miniplayer.
 */
internal object MinimalMiniplayerTitleViewFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "floaty_title"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Landroid/widget/TextView;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Creates the title view of the next video of a playlist, shown when the playlist panel is collapsed.
 */
@SkipTest
internal object NextVideoTitleViewFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "next_video_title"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Landroid/widget/TextView;",
            location = MatchAfterImmediately()
        )
    )
)

val playlistPanelVideoBindMethods = findMethodListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.method }
}

val playlistPanelTitleFields = findFieldListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.instructionMatches.first().instruction.fieldRef!! }
        .distinctBy { it.descriptor }
}

val playlistPanelVideoIdFields = findFieldListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.instructionMatches.last().instruction.fieldRef!! }
        .distinctBy { it.descriptor }
}

val nextVideoTitleViewMethods = findMethodListDirect {
    NextVideoTitleViewFingerprint.matchAll().map { it.method }
}

internal object CompactVideoBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    filters = listOf(
        // Renderer of the video.
        opcode(Opcode.CHECK_CAST),
        resourceLiteral(ResourceType.INTEGER, "compact_renderer_title_max_lines"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "[B"),
        methodCall(returnType = "Landroid/text/Spanned;"),
        // Sets the title.
        methodCall(
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "V",
            location = MatchAfterWithin(2)
        )
    )
)

internal fun compactVideoTitleSetterFingerprint(definingClass: String, name: String) = Fingerprint(
    definingClass = definingClass,
    name = name,
    returnType = "V",
    parameters = listOf("Ljava/lang/CharSequence;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/widget/TextView;"),
        methodCall(name = "setText", location = MatchAfterWithin(2))
    )
)

@StrictQuery
internal object SuggestedVideoEndScreenBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/View;"),
    filters = listOf(
        // Renderer of the suggested video.
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(2)),
        // Sets the title.
        methodCall(
            definingClass = "Lcom/google/android/libraries/youtube/common/ui/YouTubeTextView;",
            name = "setText",
            location = MatchAfterWithin(10)
        )
    )
)

val compactVideoIdField = findFieldDirect {
    CompactVideoBindFingerprint.instructionMatches.first().instruction.classRef!!.fields.single {
        it.typeSign == "Ljava/lang/String;" && !Modifier.isStatic(it.accessFlags)
    }
}

val compactVideoTitleSetter = findMethodDirect {
    CompactVideoBindFingerprint.instructionMatches.last().instruction.methodRef!!
}

val compactVideoTitleViewField = findFieldDirect {
    val setter = compactVideoTitleSetter()
    compactVideoTitleSetterFingerprint(setter.declaredClass!!.descriptor, setter.name)
        .instructionMatches.first().instruction.fieldRef!!
}

val suggestedVideoRendererGetter = findMethodDirect {
    SuggestedVideoEndScreenBindFingerprint.instructionMatches.first().instruction.methodRef!!
}

val suggestedVideoIdField = findFieldDirect {
    suggestedVideoRendererGetter().returnType!!.fields.single {
        it.typeSign == "Ljava/lang/String;" && !Modifier.isStatic(it.accessFlags)
    }
}

val suggestedVideoTitleField = findFieldDirect {
    val match = SuggestedVideoEndScreenBindFingerprint.match()
    val setter = match.instructionMatches.last()
    val viewRegister = setter.instruction.register(0)
    match.method.instructions.take(setter.index).last {
        it.opcode == Opcode.IGET_OBJECT.opCode && it.registerOrNull(0) == viewRegister
    }.fieldRef!!
}
