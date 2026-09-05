package io.github.nexalloy.morphe.youtube.misc.engagement


import app.morphe.extension.youtube.shared.EngagementPanel
import io.github.nexalloy.morphe.youtube.shared.EngagementPanelControllerFingerprint
import io.github.nexalloy.patch

typealias EngagementPanelIdHook = (String?) -> Boolean

private val engagementPanelIdHooks = mutableListOf<EngagementPanelIdHook>()

val EngagementPanelHook = patch(
    description = "Hook to get the current engagement panel state.",
) {
    val panelId = ThreadLocal<String?>()
    ::panelInitFingerprint.hookMethod {
        after {
            panelId.set(it.args[0] as String?)
        }
    }
    EngagementPanelControllerFingerprint.hookMethod {
        after { param ->
            val id = panelId.get()
            engagementPanelIdHooks.forEach { hook ->
                if (hook(id)) {
                    param.result = null
                    return@after
                }
            }

            EngagementPanel.open(id)
            panelId.remove()
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