@file:Suppress("DEPRECATION", "DiscouragedApi")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.content.Context
import android.content.res.Resources
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.AttributeSet
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import io.github.nexalloy.BuildConfig
import io.github.nexalloy.R
import io.github.nexalloy.new

@Suppress("MemberVisibilityCanBePrivate")
abstract class BasePreference(
    val key: String? = null,
    val titleKey: String? = "${key}_title",
    val summaryKey: String? = "${key}_summary",
    val icon: String? = null,
    val iconBold: String? = null,
    val layout: String? = null,
    val tag: Class<out Preference>
) {
    fun trySetString(
        key: String?,
        resources: Resources = Utils.getContext().resources,
        pkg: String = BuildConfig.APPLICATION_ID,
        setString: (str: String) -> Unit
    ) = trySetRes(key, "string", resources, pkg) { _, id -> setString(ResourceUtils.getString(id)) }

    fun trySetRes(
        key: String?,
        type: String,
        resources: Resources = Utils.getContext().resources,
        pkg: String = BuildConfig.APPLICATION_ID,
        setRes: (res: Resources, id: Int) -> Unit,
    ) {
        if (key == null) return
        when (val id = resources.getIdentifier(key, type, pkg)) {
            0 -> Logger.printDebug { "$key not found." }
            else -> setRes(resources, id)
        }
    }

    fun applyBaseAttrs(preference: Preference) {
        preference.also { pref ->
            key?.let { pref.key = key }
            trySetString(titleKey) { pref.title = it }
            trySetString(summaryKey) { pref.summary = it }

            val iconUsed = if (Utils.appIsUsingBoldIcons()) iconBold else icon
            trySetRes(iconUsed, "drawable") { res, id ->
                pref.icon = res.getDrawable(id, res.newTheme().apply {
                    applyStyle(R.style.MainTheme, true)
                })
            }
            layout?.let {
                pref.layoutResource = ResourceUtils.getLayoutIdentifier(layout)
            }
        }
    }

    open fun build(ctx: Context, prefMgr: PreferenceManager): Preference {
        Logger.printDebug { "build $key" }
        return (tag.new(
            arrayOf(Context::class.java, AttributeSet::class.java),
            ctx, null
        ) as Preference).apply {
            applyBaseAttrs(this)
        }
    }

    open fun onAttachedToHierarchy() {

    }
}
