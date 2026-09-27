package io.github.nexalloy.morphe.reddit.misc.links

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private fun Uri.unwrapRedditRedirect(): Uri {
    val target = runCatching { getQueryParameter("url") }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return this

    val parsed = runCatching { Uri.parse(target) }.getOrNull() ?: return this
    return if (parsed.scheme.equals("http", true) || parsed.scheme.equals("https", true)) {
        parsed
    } else {
        this
    }
}

val OpenLinksDirectly = patch(
    name = "Open Reddit links directly",
    description = "Skips Reddit redirect wrapper URLs and opens the original external URL.",
) {
    ::screenNavigatorFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val index = param.args.indexOfFirst { it is Uri }
            if (index < 0) return@before

            val uri = param.args[index] as Uri
            param.args[index] = uri.unwrapRedditRedirect()
        }
    }
}

val OpenLinksExternally = patch(
    name = "Open Reddit links externally",
    description = "Opens Reddit external links in the default browser instead of the in-app browser.",
    use = false,
) {
    ::screenNavigatorFingerprint.memberOrNull?.hookMethod {
        before { param ->
            val activity = param.args.firstOrNull { it is Activity } as? Activity ?: return@before
            val uri = param.args.firstOrNull { it is Uri } as? Uri ?: return@before

            val externalUri = uri.unwrapRedditRedirect()
            val intent = Intent(Intent.ACTION_VIEW, externalUri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            if (runCatching { activity.startActivity(intent) }.isSuccess) {
                param.result = null
            }
        }
    }
}
