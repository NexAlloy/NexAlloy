package io.github.nexalloy.morphe.reddit.ad

import app.morphe.extension.reddit.patches.HideAdsPatch as ExtensionHideAdsPatch
import io.github.nexalloy.getObjectField
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.reddit.misc.version.VersionCheck
import io.github.nexalloy.morphe.reddit.misc.version.is_2026_04_0_or_greater
import io.github.nexalloy.morphe.reddit.misc.version.is_2026_16_0_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide ads."
) {
    dependsOn(VersionCheck)

    ExtensionHideAdsPatch::class.java.getDeclaredMethod("isPatchIncluded").hookMethod {
        before { it.result = true }
    }

    fun hideOldAds(fingerprint: Fingerprint) {
        fingerprint.hookMethod {
            after { param ->
                val children = param.thisObject.getObjectField("children") as List<*>
                val filtered = ExtensionHideAdsPatch.hideOldPostAds(children)
                param.thisObject.setObjectField("children", filtered)
            }
        }
    }

    hideOldAds(ListingFingerprint)

    if (!is_2026_16_0_or_greater) {
        hideOldAds(SubmittedListingFingerprint)
    }

    AdPostSectionConstructorFingerprint.hookMethod {
        val immutableListBuilder = ::ImmutableListBuilderReference.method
        before { param ->
            val section = param.args[4] as? List<*>
            val filtered = ExtensionHideAdsPatch.hideNewPostAds(section)
            param.args[4] = filtered ?: immutableListBuilder(null, arrayListOf<Any>())
        }
    }

    CommentsViewModelAdLoaderFingerprint.hookMethod {
        before {
            if (ExtensionHideAdsPatch.hideCommentAds()) it.result = null
        }
    }

    if (is_2026_04_0_or_greater) {
        CommentsAdStateConstructorFingerprint.hookMethod {
            val adsLoadCompletedField = ::adsLoadCompletedField.field
            after { param ->
                val original = adsLoadCompletedField.getBoolean(param.thisObject)
                adsLoadCompletedField.setBoolean(
                    param.thisObject,
                    ExtensionHideAdsPatch.hideCommentAds(original)
                )
            }
        }
    }
}
