@file:Suppress("DEPRECATION", "DiscouragedApi")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.content.Context
import android.preference.Preference
import android.preference.PreferenceManager
import android.preference.SwitchPreference

@Suppress("MemberVisibilityCanBePrivate")
class SwitchPreference(
    key: String? = null,
    titleKey: String = "${key}_title",
    summary: Boolean = false,
    tag: Class<out Preference> = SwitchPreference::class.java,
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
) : BasePreference(key, titleKey, if (summary) "${key}_summary" else null, icon, iconBold, layout, tag) {
    override fun build(ctx: Context, prefMgr: PreferenceManager): Preference {
        return SwitchPreference(ctx).apply {
            applyBaseAttrs(this)
        }
    }
}