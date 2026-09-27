package io.github.nexalloy.revanced.facebook.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

private val storyAdRunnableNames = setOf(
    "AdBucketDataSourceUtil\$attemptAdsInsertion\$1",
    "AdBucketDataSourceUtil\$attemptFetchMoreAds\$1",
)

val HideStoryAds = patch(
    name = "Hide story ads",
    description = "Stops Facebook's story ad insertion and story ad prefetch runnables.",
) {
    ::storyAdRunCandidates.dexMethodList.forEach { dexMethod ->
        val marker = runCatching {
            val clazz = classLoader.loadClass(dexMethod.className)
            val field = clazz.getDeclaredField("__redex_internal_original_name")
            field.isAccessible = true
            field.get(null) as? String
        }.getOrNull()

        if (marker in storyAdRunnableNames) {
            dexMethod.hookMethod(XC_MethodReplacement.DO_NOTHING)
        }
    }
}
