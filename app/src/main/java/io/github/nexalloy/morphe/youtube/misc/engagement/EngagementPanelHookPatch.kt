package io.github.nexalloy.morphe.youtube.misc.engagement

import app.morphe.extension.youtube.shared.EngagementPanel
import io.github.nexalloy.morphe.youtube.shared.EngagementPanelControllerFingerprint
import io.github.nexalloy.patch

typealias EngagementPanelIdHook = (String?) -> Boolean

private val engagementPanelIdHooks = mutableListOf<EngagementPanelIdHook>()

val EngagementPanelHook = patch(
    description = "Hook to get the current engagement panel state.",
) {
    val panelIdFrames = ThreadLocal<MutableList<String?>>()

    EngagementPanelControllerFingerprint.hookMethod {
        before {
            val frames = panelIdFrames.get()
                ?: mutableListOf<String?>().also(panelIdFrames::set)
            frames.add(null)
        }
        after { param ->
            val frames = panelIdFrames.get()
            val id = if (!frames.isNullOrEmpty()) {
                frames.removeAt(frames.lastIndex)
            } else {
                null
            }
            if (frames.isNullOrEmpty()) {
                panelIdFrames.remove()
            }
            if (param.hasThrowable()) return@after

            engagementPanelIdHooks.forEach { hook ->
                if (hook(id)) {
                    param.result = null
                    return@after
                }
            }

            EngagementPanel.open(id)
        }
    }

    ::panelInitFingerprint.hookMethod {
        after { param ->
            if (param.hasThrowable()) return@after
            val frames = panelIdFrames.get() ?: return@after
            if (frames.isNotEmpty()) {
                frames[frames.lastIndex] = param.args[0] as String?
            }
        }
    }

    EngagementPanelUpdateFingerprint.hookMethod {
        val panelIdField = ::panelIdField.field
        before {
            val p1 = it.args[0]
            if (p1 != null) {
                EngagementPanel.close(panelIdField.get(p1) as String?)
            }
        }
    }
}

fun addEngagementPanelIdHook(hook: EngagementPanelIdHook) {
    engagementPanelIdHooks.add(hook)
}
