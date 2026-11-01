package io.github.nexalloy.morphe.youtube.interaction.channelsearch

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral

/**
 * Picks the hint of the search box. The default hint is loaded last, after the hints of
 * Shorts search and playlist search.
 */
internal object SearchBoxHintFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "shorts_search_hint"),
        resourceLiteral(ResourceType.STRING, "playlists_search_hint"),
        resourceLiteral(ResourceType.STRING, "search_hint"),
        opcode(Opcode.MOVE_RESULT_OBJECT)
    )
)

val SearchSubmitFingerprint = findMethodDirect {
    try {
        SearchSubmitFingerprint2143()
    } catch (_: Exception) {
        try {
            SearchSubmitFingerprint1()
        } catch (_: Exception) {
            SearchSubmitFingerprint2()
        }
    }
}

/**
 * Every search submit path funnels through this method, including suggestions and filter chips.
 */
@SkipTest
internal object SearchSubmitFingerprint2143 : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        methodCall(name = "toByteArray", returnType = "[B"),
        methodCall( // 21.43+
            parameters = listOf(
                "Ljava/lang/String;",
                "[B",
                "Ljava/lang/String;",
                "I",
                "L",
                "L",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Z",
                "Ljava/lang/String;"
            ),
            returnType = "V"
        )
    )
)

/**
 * Every search submit path funnels through this method, including suggestions and filter chips.
 */
@SkipTest
internal object SearchSubmitFingerprint1 : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z"
    ),
    filters = listOf(
        methodCall(name = "toByteArray", returnType = "[B"),
        methodCall( // 21.31 - 21.42
            parameters = listOf(
                "Ljava/lang/String;",
                "[B",
                "Ljava/lang/String;",
                "I",
                "L",
                "L",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Z"
            ),
            returnType = "V"
        )
    ),
)

/**
 * Every search submit path funnels through this method, including suggestions and filter chips.
 */
@SkipTest
internal object SearchSubmitFingerprint2 : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),

    parameters = listOf(
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        methodCall(name = "toByteArray", returnType = "[B"),
        methodCall( // 21.30 and older.
            parameters = listOf(
                "Ljava/lang/String;",
                "[B",
                "Ljava/lang/String;",
                "I",
                "L",
                "L",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;"
            ),
            returnType = "V"
        )
    )
)
