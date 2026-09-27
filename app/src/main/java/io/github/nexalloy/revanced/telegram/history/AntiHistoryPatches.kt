package io.github.nexalloy.revanced.telegram.history

import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.telegram.runtime.findTelegramClassOrNull
import io.github.nexalloy.revanced.telegram.runtime.hookTelegramRequests
import io.github.nexalloy.revanced.telegram.runtime.messageDialogId
import io.github.nexalloy.revanced.telegram.runtime.readTelegramField
import io.github.nexalloy.revanced.telegram.runtime.requestDialogId
import io.github.nexalloy.revanced.telegram.runtime.writeTelegramField
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

private const val OWN_ACTION_TTL_MS = 120_000L

private val pendingOwnDeletes = ConcurrentHashMap<String, Long>()
private val pendingOwnEdits = ConcurrentHashMap<String, Long>()

private fun scopedKey(dialogId: Long, messageId: Int): String =
    "$dialogId:$messageId"

private fun cleanupPending(map: ConcurrentHashMap<String, Long>) {
    val cutoff = System.currentTimeMillis() - OWN_ACTION_TTL_MS
    for ((key, timestamp) in map) {
        if (timestamp < cutoff) {
            map.remove(key, timestamp)
        }
    }
}

private fun Any.messageIds(): List<Int> {
    val value = readTelegramField("id") ?: readTelegramField("messages")
    return when (value) {
        is Number -> listOf(value.toInt())
        is Iterable<*> -> value.mapNotNull { (it as? Number)?.toInt() }
        else -> emptyList()
    }
}

private fun Any.channelDialogId(): Long {
    val direct = (readTelegramField("channel_id") as? Number)?.toLong() ?: 0L
    if (direct != 0L) return -direct
    return requestDialogId(this) ?: 0L
}

private fun ClassLoader.trackOwnDeleteRequests() {
    hookTelegramRequests { request, _ ->
        val name = request.javaClass.name
        if (
            !name.endsWith("\$TL_messages_deleteMessages") &&
            !name.endsWith("\$TL_channels_deleteMessages")
        ) return@hookTelegramRequests

        cleanupPending(pendingOwnDeletes)
        val scope = if (name.endsWith("\$TL_channels_deleteMessages")) {
            request.channelDialogId()
        } else {
            0L
        }
        for (id in request.messageIds()) {
            pendingOwnDeletes[scopedKey(scope, id)] = System.currentTimeMillis()
        }
    }
}

private fun ClassLoader.trackOwnEditRequests() {
    hookTelegramRequests { request, _ ->
        if (!request.javaClass.name.endsWith("\$TL_messages_editMessage")) {
            return@hookTelegramRequests
        }

        val id = (request.readTelegramField("id") as? Number)?.toInt()
            ?: return@hookTelegramRequests
        val dialogId = requestDialogId(request) ?: 0L
        cleanupPending(pendingOwnEdits)
        pendingOwnEdits[scopedKey(dialogId, id)] = System.currentTimeMillis()
    }
}

private fun ClassLoader.hookUpdateList(
    transform: (MutableList<Any?>) -> Unit,
) {
    val controller =
        findTelegramClassOrNull("org.telegram.messenger.MessagesController") ?: return
    val updatesBase =
        findTelegramClassOrNull("org.telegram.tgnet.TLRPC\$Updates")

    controller.declaredMethods
        .filter {
            it.name == "processUpdates" &&
                it.parameterTypes.isNotEmpty() &&
                (updatesBase == null || updatesBase.isAssignableFrom(it.parameterTypes[0]))
        }
        .forEach { method ->
            method.isAccessible = true
            method.hookMethod {
                before { param ->
                    val envelope = param.args.firstOrNull() ?: return@before
                    @Suppress("UNCHECKED_CAST")
                    val updates = envelope.readTelegramField("updates") as? MutableList<Any?>
                        ?: return@before
                    transform(updates)
                }
            }
        }
}

val KeepDeletedMessages = patch(
    name = "Keep remotely deleted messages",
    description = "Keeps normal and channel messages in the local Telegram database when another participant deletes them. Deletions initiated from this device are still allowed.",
    use = false,
) {
    classLoader.trackOwnDeleteRequests()

    classLoader.hookUpdateList { updates ->
        cleanupPending(pendingOwnDeletes)

        val iterator = updates.listIterator()
        while (iterator.hasNext()) {
            val update = iterator.next() ?: continue
            val name = update.javaClass.name

            val isChannel = name.endsWith("\$TL_updateDeleteChannelMessages")
            val isNormal = name.endsWith("\$TL_updateDeleteMessages")
            if (!isChannel && !isNormal) continue

            val scope = if (isChannel) {
                val id = (update.readTelegramField("channel_id") as? Number)?.toLong() ?: 0L
                if (id != 0L) -id else 0L
            } else {
                0L
            }

            val ids = update.messageIds()
            if (ids.isEmpty()) continue

            val ownIds = ArrayList<Int>()
            for (id in ids) {
                if (pendingOwnDeletes.remove(scopedKey(scope, id)) != null) {
                    ownIds.add(id)
                }
            }

            when {
                ownIds.isEmpty() -> iterator.remove()
                ownIds.size != ids.size -> update.writeTelegramField("messages", ownIds)
            }
        }
    }
}

val KeepOriginalEditedMessages = patch(
    name = "Keep original text of remotely edited messages",
    description = "Ignores remote message-edit updates so the locally stored original text remains visible. Edits initiated from this device are still allowed.",
    use = false,
) {
    classLoader.trackOwnEditRequests()

    classLoader.hookUpdateList { updates ->
        cleanupPending(pendingOwnEdits)

        val iterator = updates.listIterator()
        while (iterator.hasNext()) {
            val update = iterator.next() ?: continue
            val name = update.javaClass.name
            if (
                !name.endsWith("\$TL_updateEditMessage") &&
                !name.endsWith("\$TL_updateEditChannelMessage")
            ) continue

            val message = update.readTelegramField("message") ?: continue
            val id = (message.readTelegramField("id") as? Number)?.toInt() ?: continue
            val dialogId = messageDialogId(message) ?: 0L

            if (pendingOwnEdits.remove(scopedKey(dialogId, id)) == null) {
                iterator.remove()
            }
        }
    }
}
