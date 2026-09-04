package io.github.nexalloy.morphe

internal fun isVersionAtLeast(current: String, required: String): Boolean {
    val currentParts = current.versionParts()
    val requiredParts = required.versionParts()
    val count = maxOf(currentParts.size, requiredParts.size)

    for (index in 0 until count) {
        val currentPart = currentParts.getOrElse(index) { 0L }
        val requiredPart = requiredParts.getOrElse(index) { 0L }
        if (currentPart != requiredPart) return currentPart > requiredPart
    }

    return true
}

private fun String.versionParts(): List<Long> = split('.').map { part ->
    val digits = part.takeWhile { it.isDigit() }
    require(digits.isNotEmpty()) { "Invalid version: $this" }
    digits.toLong()
}
