package com.sociallite.pro

enum class SocialPlatform(
    val title: String,
    val homeUrl: String,
    val dmUrl: String,
    val allowedHosts: List<String>
) {
    INSTAGRAM(
        title = "Instagram",
        homeUrl = "https://www.instagram.com/",
        dmUrl = "https://www.instagram.com/direct/inbox/",
        allowedHosts = listOf("instagram.com", "cdninstagram.com", "fbcdn.net")
    ),
    YOUTUBE(
        title = "YouTube",
        homeUrl = "https://m.youtube.com/",
        dmUrl = "https://m.youtube.com/feed/subscriptions",
        allowedHosts = listOf("youtube.com", "youtu.be", "googlevideo.com", "ytimg.com", "google.com")
    ),
    X_TWITTER(
        title = "X",
        homeUrl = "https://x.com/home",
        dmUrl = "https://x.com/messages",
        allowedHosts = listOf("x.com", "twitter.com", "twimg.com")
    ),
    FACEBOOK(
        title = "Facebook",
        homeUrl = "https://m.facebook.com/",
        dmUrl = "https://m.facebook.com/messages/",
        allowedHosts = listOf("facebook.com", "fbcdn.net", "messenger.com")
    );

    companion object {
        fun fromUrl(url: String): SocialPlatform? {
            val lower = url.lowercase()
            return when {
                lower.contains("instagram.com") -> INSTAGRAM
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("x.com") || lower.contains("twitter.com") -> X_TWITTER
                lower.contains("facebook.com") || lower.contains("fb.watch") -> FACEBOOK
                else -> null
            }
        }
    }
}

data class FilterSettings(
    val blockReelsFeed: Boolean = true,
    val blockShortsFeed: Boolean = true,
    val allowSingleSharedLinks: Boolean = true,
    val blockExploreAndSuggested: Boolean = true,
    val blockStories: Boolean = false,
    val dailyLimitMinutes: Int = 45,
    val enforceDailyLimit: Boolean = false
)
