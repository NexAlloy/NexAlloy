@file:Suppress("DEPRECATION")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.content.Context
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceGroup
import android.preference.PreferenceManager
import app.morphe.extension.shared.settings.preference.NoTitlePreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting

open class PreferenceCategory(
    key: String? = null,
    titleKey: String? = if (key == null) null else "${key}_title",
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
    sorting: Sorting = Sorting.BY_TITLE,
    tag: Class<out PreferenceGroup> = PreferenceCategory::class.java,
    val preferences: Set<BasePreference>
) : BasePreference(sorting.appendSortType(key), titleKey, null, icon, iconBold, layout, tag) {
    lateinit var children: List<Preference>
    lateinit var preferenceGroup: PreferenceGroup
    override fun build(ctx: Context, prefMgr: PreferenceManager): Preference {
        return (super.build(ctx, prefMgr) as PreferenceGroup).apply {
            preferenceGroup = this
            children = preferences.map { it.build(ctx, prefMgr) }
        }
    }

    override fun onAttachedToHierarchy() {
        preferenceGroup.apply {
            children.forEach { addPreference(it) }
            preferences.forEach { it.onAttachedToHierarchy() }
        }
    }
}

internal fun noTitleUnsortedPreferenceCategory(
    vararg preferences: BasePreference
) = noTitleUnsortedPreferenceCategory(preferences.toSet())

internal fun noTitleUnsortedPreferenceCategory(
    preferences: Set<BasePreference>
) = PreferenceCategory(
    key = null,
    titleKey = null,
    sorting = Sorting.UNSORTED,
    tag = NoTitlePreferenceCategory::class.java,
    preferences = preferences
)

