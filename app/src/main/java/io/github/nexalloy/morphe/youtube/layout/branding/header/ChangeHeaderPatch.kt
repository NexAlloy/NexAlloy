package io.github.nexalloy.morphe.youtube.layout.branding.header

import android.util.TypedValue
import android.view.ViewGroup
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.ChangeHeaderPatch
import app.morphe.extension.youtube.patches.ChangeHeaderPatch.HeaderLogo
import app.morphe.extension.youtube.settings.Settings
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val changeHeaderPatch = patch(
    name = "Change header",
    description = "Adds an option to change the header logo in the top left corner of the app."
) {
    PreferenceScreen.GENERAL.addPreferences(ListPreference("morphe_header_logo"))

    val regularHeader = ResourceUtils.getAttrIdentifier("ytWordmarkHeader")
    val premiumHeader = ResourceUtils.getAttrIdentifier("ytPremiumWordmarkHeader")
    // NexAlloy: some APK variants strip native drawable names. Resolve these logos from
    // the host theme for both the top bar and the upstream drawer updater.
    HeaderLogo::class.java.getDeclaredMethod("getDrawable").hookMethod {
        before {
            val attribute = when (it.thisObject) {
                HeaderLogo.REGULAR -> regularHeader
                HeaderLogo.PREMIUM -> premiumHeader
                else -> return@before
            }
            val context = Utils.getActivity() ?: Utils.getContext()
            val value = TypedValue()
            if (context.theme.resolveAttribute(attribute, value, true) && value.resourceId != 0) {
                it.result = context.getDrawable(value.resourceId)
            }
        }
    }

    // NexAlloy: replace the resolved drawable instead of adding attributes to the host themes.
    ::headerDrawableMethod.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            val attribute = it.args[1] as Int
            if (attribute != regularHeader && attribute != premiumHeader) return@after
            Settings.HEADER_LOGO.get().getDrawable()?.let { logo -> it.result = logo }
        }
    }

    val drawerContentView = ::drawerContentViewField.field
    DrawerHeaderRenderFingerprint.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            // NexAlloy: keep the account's original logo when Default is selected.
            if (Settings.HEADER_LOGO.get() != HeaderLogo.DEFAULT) {
                ChangeHeaderPatch.updateDrawerLogo(drawerContentView.get(it.thisObject) as ViewGroup)
            }
        }
    }
}
