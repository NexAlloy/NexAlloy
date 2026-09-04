package io.github.nexalloy

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SkipTest()

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class TargetApp(val app: String)

class AppVersion(val versionString: String) : Comparable<AppVersion> {
    init {
        require(versionString.matches(Regex("\\d+(\\.\\d+)*"))) {
            "Version string must consist of numbers separated by dots (e.g., 1.2 or 1.2.3.4)"
        }
    }

    private val parts: List<Long> by lazy { versionString.split('.').map { it.toLong() } }

    fun getPart(index: Int): Long = parts.elementAtOrElse(index) { 0L }

    override fun compareTo(other: AppVersion): Int {
        val len = maxOf(parts.size, other.parts.size)
        for (i in 0 until len) {
            val thisPart = getPart(i)
            val otherPart = other.getPart(i)
            if (thisPart != otherPart) {
                return thisPart.compareTo(otherPart)
            }
        }
        return 0
    }

    override fun toString(): String = versionString
}

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequireAppVersion(
    val minVersion: String = "",
    val maxVersion: String = ""
)

class VersionConstraintFailedException(message: String) : Exception(message)

fun match(appVersion: AppVersion, minVersionStr: String, maxVersionStr: String) {
    val minVersion = minVersionStr.takeIf { it.isNotEmpty() }?.let { AppVersion(it) }
    val maxVersion = maxVersionStr.takeIf { it.isNotEmpty() }?.let { AppVersion(it) }
    when {
        minVersion == null && maxVersion == null -> return
        minVersion != null && appVersion < minVersion ->
            throw VersionConstraintFailedException("Min version mismatch (current: $appVersion, required: $minVersion)")

        maxVersion != null && appVersion > maxVersion ->
            throw VersionConstraintFailedException("Max version mismatch (current: $appVersion, required: $maxVersion)")
    }
}
