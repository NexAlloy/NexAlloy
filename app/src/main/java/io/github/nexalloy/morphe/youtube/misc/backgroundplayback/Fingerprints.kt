package io.github.nexalloy.morphe.youtube.misc.backgroundplayback

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns

val prefBackgroundAndOfflineCategoryId get() = resourceMappings["string", "pref_background_and_offline_category"]

object BackgroundPlaybackManagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
        Opcode.CONST,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    )
)

/**
 * 21.23+
 */
@RequireAppVersion("21.23.000")
internal object BackgroundPlaybackSettingsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        // Background playback allowed.
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, returnType = "Z", parameters = listOf()),
        methodCall(
            smali = "Ljava/lang/Boolean;->booleanValue()Z",
            location = MatchAfterWithin(2)
        ),
        resourceLiteral(
            ResourceType.STRING,
            "pref_background_and_offline_category",
            location = MatchAfterWithin(10)
        ),
        resourceLiteral(ResourceType.STRING, "pref_background_category")
    )
)

/**
 * 21.22 and lower.
 */
@RequireAppVersion(maxVersion = "21.23.000")
internal object BackgroundPlaybackSettingsLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        methodCall(returnType = "Z"),
        // Background playback allowed.
        methodCall(returnType = "Z"),
        resourceLiteral(ResourceType.STRING, "pref_background_and_offline_category"),
        resourceLiteral(ResourceType.STRING, "pref_background_category")
    )
)

@get:RequireAppVersion("21.23.000")
val backgroundPlaybackSettingsSubFingerprint = findMethodDirect {
    BackgroundPlaybackSettingsFingerprint.instructionMatches.first().instruction.methodRef!!
}

@get:RequireAppVersion(maxVersion = "21.23.000")
val backgroundPlaybackSettingsLegacySubFingerprint = findMethodDirect {
    BackgroundPlaybackSettingsLegacyFingerprint.instructionMatches[1].instruction.methodRef!!
}

object KidsBackgroundPlaybackPolicyControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "L", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
    )
)

val backgroundPlaybackManagerShortsFingerprint = findMethodListDirect {
    /*
    * two matches in versions 21.02.32
    * It doesn't seem to be an A/B test;
    * it seems to be a different method that checks an additional property to determine the result.
    * */
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
            returns("Z")
            parameters("L")
            literal { 151635310 }
        }
    }
}

@get:RequireAppVersion("21.15.000")
val playingWhenBackgroundedMethods = findMethodListDirect {
    findMethod { matcher { addEqString("was_playing_when_backgrounded") } }
        .map { it.declaredClass!! }.distinctBy { it.descriptor }
        .flatMap { it.methods }
}
