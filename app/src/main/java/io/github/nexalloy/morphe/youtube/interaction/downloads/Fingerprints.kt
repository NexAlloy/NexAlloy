package io.github.nexalloy.morphe.youtube.interaction.downloads

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.string
import java.lang.reflect.Modifier
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData

internal object OfflineVideoEndpointFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String", // Video ID
        "L",
    ),
    filters = listOf(
        anyInstruction(
            string("Unsupported Offline Video Action: "), // 21.14 and lower
            string("Unsupported Offline Video Action: %s") // 21.15+
        )
    ),
    custom = {
        addAnyOf { addEqString("Unsupported Offline Video Action: ") }
        addAnyOf { addEqString("Unsupported Offline Video Action: %s") }
    }
)

/**
 * Matches the offline video endpoint method, to find the call that saves a video with its offlineability renderer.
 * The quality picker and the Premium upsell of the video are both shown by the called method.
 */
@StrictQuery
internal object SaveOfflineVideoCallFingerprint : Fingerprint(
    classFingerprint = OfflineVideoEndpointFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String",
        "L",
    ),
    filters = listOf(
        methodCall(
            parameters = listOf(
                "Ljava/lang/String;", // Video ID
                "L", // Offlineability renderer
                "L",
                "L",
                "L",
                "I"
            ),
            returnType = "V"
        )
    )
)

/**
 * Resolves a command with the first command mapping that supports it.
 */
internal object ResolveCommandFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        anyInstruction(
            string("Unknown command not resolved"), // 20.x
            string("Unknown command not resolved %s") // 21.x
        )
    ),
    custom = {
        addAnyOf { addEqString("Unknown command not resolved") }
        addAnyOf { addEqString("Unknown command not resolved %s") }
    }
)

/**
 * Response callback of the 'ypc/get_offline_upsell' endpoint.
 */
internal object OfflineUpsellResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("L"), // Response.
        literal(131899791), // Upsell renderer of the response.
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"), // Endpoint command.
        string("YpcGetOfflineUpsellResponse_videoIdKey")
    )
)

/**
 * Fetches the player response a video is saved with, and puts it into the cache.
 */
internal object OfflinePlayerResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        string("Some error occurred when reading from the cache player response"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Lcom/google/common/util/concurrent/ListenableFuture;"
        )
    )
)

/**
 * Creates a player response model from the bytes of a player response.
 */
internal fun playerResponseModelFromBytesFingerprint(playerResponseModel: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = playerResponseModel,
    parameters = listOf("[B", "J"),
    filters = listOf(
        methodCall(
            parameters = listOf("[B", "Lcom/google/protobuf/MessageLite;"),
            returnType = "Lcom/google/protobuf/MessageLite;"
        ),
        opcode(Opcode.CHECK_CAST) // Player response.
    )
)

internal fun playerResponseGetterFingerprint(playerResponseModelClass: String, playerResponse: String) = Fingerprint(
    definingClass = playerResponseModelClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = playerResponse,
    parameters = listOf()
)

internal val saveOfflineVideoMethod = findMethodDirect {
    SaveOfflineVideoCallFingerprint.instructionMatches.first().instruction.methodRef!!
}

internal val offlineUpsellCommandField = findFieldDirect {
    OfflineUpsellResponseFingerprint.instructionMatches[2].instruction.fieldRef!!
}

internal val playerResponseModelFromBytesMethod = findMethodDirect {
    playerResponseModelFromBytesFingerprint(OfflinePlayerResponseFingerprint().returnType!!.descriptor)
        .match().method
}

internal val playerResponseGetterMethod = findMethodDirect {
    val factory = playerResponseModelFromBytesMethod()
    val responseType = playerResponseModelFromBytesFingerprint(factory.returnType!!.descriptor)
        .instructionMatches.last().instruction.classRef!!.descriptor
    playerResponseGetterFingerprint(factory.declaredClass!!.descriptor, responseType).match().method
}

internal val offlinePlayerResponseFactoryMethod = findMethodDirect {
    val outer = OfflinePlayerResponseFingerprint.match()
    val modelType = outer.method.returnType!!.descriptor
    // Transform the freshly created model before the cache put, so both cache and caller receive it.
    outer.method.instructions.take(outer.instructionMatches.last().index).filter {
        (it.opcode == Opcode.INVOKE_VIRTUAL.opCode || it.opcode == Opcode.INVOKE_VIRTUAL_RANGE.opCode)
                && it.methodRef?.returnType?.descriptor == modelType
    }.map { it.methodRef!! }.distinctBy { it.descriptor }.single()
}

@SkipTest
internal object ProtobufClassParseByteArrayFingerprint : Fingerprint(
    name = "parseFrom",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "[B"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            definingClass = "Lcom/google/protobuf/ExtensionRegistryLite;",
            type = "Lcom/google/protobuf/ExtensionRegistryLite;"
        )
    )
)

internal object ProtobufClassParseByteArrayWithRegistryFingerprint : Fingerprint(
    classFingerprint = ProtobufClassParseByteArrayFingerprint,
    name = "parseFrom",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "[B", "Lcom/google/protobuf/ExtensionRegistryLite;")
)

internal object GeneratedRegistryFingerprint : Fingerprint(
    definingClass = "Lcom/google/protobuf/ExtensionRegistryLite;",
    name = "getGeneratedRegistry",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lcom/google/protobuf/ExtensionRegistryLite;",
    parameters = emptyList()
)

context(bridge: DexKitBridge)
private fun defaultInstance(type: ClassData) = type.fields.single {
    Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) && it.type.descriptor == type.descriptor
}

internal val offlineCommandDefaultInstance = findFieldDirect {
    defaultInstance(ResolveCommandFingerprint().paramTypes.first())
}

internal val offlineabilityDefaultInstance = findFieldDirect {
    defaultInstance(saveOfflineVideoMethod().paramTypes[1])
}

internal val offlineUpsellDefaultInstance = findFieldDirect {
    defaultInstance(OfflineUpsellResponseFingerprint.instructionMatches.first().instruction.classRef!!)
}

/**
 * Saves or removes a playlist, such as from the Download button of a playlist page header.
 */
internal object OfflinePlaylistEndpointFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "Ljava/lang/String;", // Playlist ID
        "L", // Action
    ),
    filters = listOf(
        anyInstruction(
            string("Unsupported Offline Playlist Action: "), // 20.x
            string("Unsupported Offline Playlist Action: %d") // 21.x
        )
    ),
    custom = {
        addAnyOf { addEqString("Unsupported Offline Playlist Action: ") }
        addAnyOf { addEqString("Unsupported Offline Playlist Action: %d") }
    }
)

/**
 * Updates the offline state of a saved video with the state of the 'player/refresh' response.
 * Matches two methods, one for each of the two ways the app refreshes saved videos.
 */
@SkipTest
internal object UpdateOfflineVideoPolicyFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "L",
        "J", // Time of the refresh.
        "L", // Offline state.
        "L" // Video policy.
    ),
    filters = listOf(
        anyInstruction(
            string("[Offline] UpdateVideoPolicy failed for video "), // 20.x
            string("[Offline] UpdateVideoPolicy failed for video %s") // 21.x
        )
    ),
    custom = {
        addAnyOf { addEqString("[Offline] UpdateVideoPolicy failed for video ") }
        addAnyOf { addEqString("[Offline] UpdateVideoPolicy failed for video %s") }
    }
)

internal val updateOfflineVideoPolicyMethods = findMethodListDirect {
    UpdateOfflineVideoPolicyFingerprint.matchAll(2..2).map { it.method }
}

internal val offlineStateDefaultInstance = findFieldDirect {
    defaultInstance(updateOfflineVideoPolicyMethods().first().paramTypes[2])
}

/** Reads the interaction logger interface from the context of commands. */
internal val interactionLoggerClass = io.github.nexalloy.morphe.findClassDirect {
    findMethod {
        matcher { usingStrings("com.google.android.libraries.youtube.logging.interaction_logger") }
    }.flatMap { method ->
        method.instructions.zipWithNext().mapNotNull { (key, type) ->
            if (key.string == "com.google.android.libraries.youtube.logging.interaction_logger" &&
                type.opcodeEnum == Opcode.CONST_CLASS) type.classRef else null
        }
    }.distinctBy { it.descriptor }.single()
}

internal val noOpInteractionLogger = findFieldDirect {
    val type = interactionLoggerClass()
    type.fields.single { Modifier.isStatic(it.modifiers) && it.type == type }
}
