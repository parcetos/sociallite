package com.sociallite.pro

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sociallite_pro_prefs", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(loadAllConfigs())
    val configs: StateFlow<Map<SocialPlatform, PlatformFilterConfig>> = _configs.asStateFlow()

    private val _usageSeconds = MutableStateFlow(loadAllUsageSeconds())
    val usageSeconds: StateFlow<Map<SocialPlatform, Int>> = _usageSeconds.asStateFlow()

    private fun todayKey(): String = LocalDate.now().toString()

    private fun ensureDateReset() {
        val savedDate = prefs.getString("usage_date", null)
        val today = todayKey()
        if (savedDate != today) {
            val editor = prefs.edit().putString("usage_date", today)
            SocialPlatform.entries.forEach { platform ->
                editor.putInt("usage_${platform.id}", 0)
            }
            editor.apply()
            _usageSeconds.value = SocialPlatform.entries.associateWith { 0 }
        }
    }

    private fun loadAllConfigs(): Map<SocialPlatform, PlatformFilterConfig> {
        return SocialPlatform.entries.associateWith { platform ->
            val p = platform.id
            PlatformFilterConfig(
                blockShortVideos = prefs.getBoolean("${p}_block_shorts", true),
                blockExploreAndDiscovery = prefs.getBoolean("${p}_block_explore", true),
                dmOrSubscriptionsOnly = prefs.getBoolean("${p}_dm_only", false),
                blockAdsAndSuggestions = prefs.getBoolean("${p}_block_ads", true),
                grayscaleMode = prefs.getBoolean("${p}_grayscale", false),
                dailyLimitMinutes = prefs.getInt("${p}_limit_min", 30)
            )
        }
    }

    private fun loadAllUsageSeconds(): Map<SocialPlatform, Int> {
        ensureDateReset()
        return SocialPlatform.entries.associateWith { platform ->
            prefs.getInt("usage_${platform.id}", 0)
        }
    }

    fun updateConfig(platform: SocialPlatform, transform: (PlatformFilterConfig) -> PlatformFilterConfig) {
        val current = _configs.value[platform] ?: PlatformFilterConfig()
        val updated = transform(current)
        val p = platform.id

        prefs.edit()
            .putBoolean("${p}_block_shorts", updated.blockShortVideos)
            .putBoolean("${p}_block_explore", updated.blockExploreAndDiscovery)
            .putBoolean("${p}_dm_only", updated.dmOrSubscriptionsOnly)
            .putBoolean("${p}_block_ads", updated.blockAdsAndSuggestions)
            .putBoolean("${p}_grayscale", updated.grayscaleMode)
            .putInt("${p}_limit_min", updated.dailyLimitMinutes)
            .apply()

        _configs.update { it + (platform to updated) }
    }

    fun incrementUsageSecond(platform: SocialPlatform) {
        ensureDateReset()
        val current = _usageSeconds.value[platform] ?: 0
        val next = current + 1
        prefs.edit().putInt("usage_${platform.id}", next).apply()
        _usageSeconds.update { it + (platform to next) }
    }
}
