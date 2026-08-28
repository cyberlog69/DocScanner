package com.example.docscanner.model

/**
 * Encapsulates release update information retrieved from the distribution repository.
 */
data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String = "",
    val downloadUrl: String? = null,
    val releasePageUrl: String = "",
    val publishedAt: String = "",
    val apkSizeBytes: Long = 0L
) {
    val formattedApkSize: String
        get() = if (apkSizeBytes > 0) {
            val mb = apkSizeBytes / (1024.0 * 1024.0)
            "${((mb * 10).toInt()) / 10.0} MB"
        } else ""
}

/**
 * Semantic version comparator supporting standard semver and pre-release tags (e.g. 1.6.0-beta).
 */
object VersionComparator {

    fun isNewerVersion(currentVersionStr: String, latestVersionStr: String): Boolean {
        val current = parseVersion(currentVersionStr)
        val latest = parseVersion(latestVersionStr)

        if (latest.major != current.major) return latest.major > current.major
        if (latest.minor != current.minor) return latest.minor > current.minor
        if (latest.patch != current.patch) return latest.patch > current.patch

        // If numeric versions are equal (e.g. 1.6.0 vs 1.6.0-beta):
        // non-prerelease (stable) is newer than prerelease (beta/rc/alpha)
        if (current.isPreRelease && !latest.isPreRelease) return true
        if (!current.isPreRelease && latest.isPreRelease) return false

        return false
    }

    private data class ParsedVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val isPreRelease: Boolean
    )

    private fun parseVersion(versionStr: String): ParsedVersion {
        val cleaned = versionStr.trim().removePrefix("v").removePrefix("V")
        val isPreRelease = cleaned.contains("-") || cleaned.contains("beta", ignoreCase = true) || cleaned.contains("rc", ignoreCase = true) || cleaned.contains("alpha", ignoreCase = true)

        val basePart = cleaned.split("-").firstOrNull() ?: cleaned
        val segments = basePart.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val major = segments.getOrNull(0) ?: 0
        val minor = segments.getOrNull(1) ?: 0
        val patch = segments.getOrNull(2) ?: 0

        return ParsedVersion(major, minor, patch, isPreRelease)
    }
}
