package io.github.nexalloy.morphe.reddit.misc.privacy

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

val RemoveNsfwWarning = patch(
    name = "Remove Reddit NSFW warning",
    description = "Treats NSFW communities as already visited so Reddit does not show the repeat warning dialog.",
    use = false,
) {
    listOf(
        "com.reddit.domain.model.Subreddit",
        "com.reddit.domain.model.UserSubreddit",
    ).forEach { className ->
        val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: return@forEach
        clazz.declaredMethods
            .filter {
                it.name == "getHasBeenVisited" &&
                    it.returnType == Boolean::class.javaPrimitiveType &&
                    it.parameterTypes.isEmpty()
            }
            .forEach { method ->
                method.hookMethod(XC_MethodReplacement.returnConstant(true))
            }
    }
}
