package io.github.nexalloy.morphe.reddit.misc.version

import io.github.nexalloy.morphe.isVersionAtLeast
import io.github.nexalloy.patch
import kotlin.properties.Delegates

var is_2025_48_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_04_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_11_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_15_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_16_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_18_0_or_greater: Boolean by Delegates.notNull()
    private set

val VersionCheck = patch {
    val versionName = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0).versionName!!

    fun isEqualsOrGreaterThan(version: String): Boolean {
        return isVersionAtLeast(versionName, version)
    }

    is_2025_48_0_or_greater = isEqualsOrGreaterThan("2025.48.0")
    is_2026_04_0_or_greater = isEqualsOrGreaterThan("2026.04.0")
    is_2026_11_0_or_greater = isEqualsOrGreaterThan("2026.11.0")
    is_2026_15_0_or_greater = isEqualsOrGreaterThan("2026.15.0")
    is_2026_16_0_or_greater = isEqualsOrGreaterThan("2026.16.0")
    is_2026_18_0_or_greater = isEqualsOrGreaterThan("2026.18.0")

}
