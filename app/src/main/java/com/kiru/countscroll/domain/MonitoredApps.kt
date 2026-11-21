package com.kiru.countscroll.domain

data class MonitoredApp(
    val packageName: String,
    val displayName: String,
    val shortCode: String
)

enum class AppFilter(val label: String) {
    ALL("Semua"),
    TIKTOK("TikTok"),
    INSTAGRAM("IG Reels"),
    YOUTUBE("YT Shorts")
}

object MonitoredApps {
    const val TIKTOK_GLOBAL = "com.zhiliaoapp.musically"
    const val TIKTOK_ASIA = "com.ss.android.ugc.trill"
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"

    val tiktokPackages = listOf(TIKTOK_GLOBAL, TIKTOK_ASIA)
    val instagramPackages = listOf(INSTAGRAM)
    val youtubePackages = listOf(YOUTUBE)

    val allMonitoredPackages: List<String> =
        tiktokPackages + instagramPackages + youtubePackages

    val supportedApps = listOf(
        MonitoredApp(TIKTOK_GLOBAL, "TikTok", "TT"),
        MonitoredApp(TIKTOK_ASIA, "TikTok", "TT"),
        MonitoredApp(INSTAGRAM, "Instagram Reels", "IG"),
        MonitoredApp(YOUTUBE, "YouTube Shorts", "YT")
    )

    fun isMonitored(packageName: String): Boolean {
        return supportedApps.any { it.packageName == packageName }
    }

    fun getDisplayName(packageName: String): String {
        return supportedApps.find { it.packageName == packageName }?.displayName ?: packageName
    }

    fun matchesFilter(packageName: String, filter: AppFilter): Boolean {
        return when (filter) {
            AppFilter.ALL -> isMonitored(packageName)
            AppFilter.TIKTOK -> packageName in tiktokPackages
            AppFilter.INSTAGRAM -> packageName in instagramPackages
            AppFilter.YOUTUBE -> packageName in youtubePackages
        }
    }
}
