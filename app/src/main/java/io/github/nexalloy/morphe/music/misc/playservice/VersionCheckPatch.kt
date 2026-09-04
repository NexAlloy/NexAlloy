package io.github.nexalloy.morphe.music.misc.playservice

import io.github.nexalloy.morphe.isVersionAtLeast
import io.github.nexalloy.patch
import kotlin.properties.Delegates


// Use notNull delegate so an exception is thrown if these fields are accessed before they are set.

var is_9_19_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_20_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_24_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_26_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_28_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_30_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_32_or_greater: Boolean by Delegates.notNull()
    private set
var is_9_33_or_greater: Boolean by Delegates.notNull()
    private set

val versionCheckPatch = patch {
    val versionName = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0).versionName!!

    fun isEqualsOrGreaterThan(version: String): Boolean {
        return isVersionAtLeast(versionName, version)
    }

    is_9_19_or_greater = isEqualsOrGreaterThan("9.19.00")
    is_9_20_or_greater = isEqualsOrGreaterThan("9.20.00")
    is_9_24_or_greater = isEqualsOrGreaterThan("9.24.00")
    is_9_26_or_greater = isEqualsOrGreaterThan("9.26.00")
    is_9_28_or_greater = isEqualsOrGreaterThan("9.28.00")
    is_9_30_or_greater = isEqualsOrGreaterThan("9.30.00")
    is_9_32_or_greater = isEqualsOrGreaterThan("9.32.00")
    is_9_33_or_greater = isEqualsOrGreaterThan("9.33.00")
}
