package com.sociallite.pro

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class SocialPlatform(
    val id: String,
    val displayName: String,
    val homeUrl: String,
    val dmUrl: String,
    val accentColor: Color,
    val hostDomains: List<String>
) {
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram",
        homeUrl = "https://www.instagram.com/",
        dmUrl = "https://www.instagram.com/direct/inbox/",
        accentColor = Color(0xFFE1306C),
        hostDomains = listOf("instagram.com")
    ),
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube",
        homeUrl = "https://m.youtube.com/",
        dmUrl = "https://m.youtube.com/feed/subscriptions",
        accentColor = Color(0xFFFF0033),
        hostDomains = listOf("youtube.com", "youtu.be")
    ),
    X_TWITTER(
        id = "x_twitter",
        displayName = "X / Twitter",
        homeUrl = "https://x.com/home",
        dmUrl = "https://x.com/messages",
        accentColor = Color(0xFF1D9BF0),
        hostDomains = listOf("x.com", "twitter.com")
    ),
    FACEBOOK(
        id = "facebook",
        displayName = "Facebook",
        homeUrl = "https://m.facebook.com/",
        dmUrl = "https://m.facebook.com/messages/",
        accentColor = Color(0xFF1877F2),
        hostDomains = listOf("facebook.com", "fb.com", "fb.watch")
    ),
    REDDIT(
        id = "reddit",
        displayName = "Reddit",
        homeUrl = "https://www.reddit.com/",
        dmUrl = "https://www.reddit.com/message/inbox/",
        accentColor = Color(0xFFFF4500),
        hostDomains = listOf("reddit.com")
    );

    companion object {
        fun fromUrl(url: String): SocialPlatform? {
            val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
            return entries.firstOrNull { platform ->
                platform.hostDomains.any { domain ->
                    host == domain || host.endsWith(".$domain")
                }
            }
        }
    }
}

data class PlatformFilterConfig(
    val blockShortVideos: Boolean = true,
    val blockExploreAndDiscovery: Boolean = true,
    val dmOrSubscriptionsOnly: Boolean = false,
    val blockAdsAndSuggestions: Boolean = true,
    val grayscaleMode: Boolean = false,
    val dailyLimitMinutes: Int = 30
)

data class SharedLinkSession(
    val platform: SocialPlatform,
    val targetUrl: String,
    val normalizedPath: String,
    val lockVerticalScroll: Boolean
)
