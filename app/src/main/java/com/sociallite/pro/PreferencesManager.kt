package com.sociallite.pro

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sociallite_pro_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): FilterSettings {
        return FilterSettings(
            blockReelsFeed = prefs.getBoolean("block_reels_feed", true),
            blockShortsFeed = prefs.getBoolean("block_shorts_feed", true),
            allowSingleSharedLinks = prefs.getBoolean("allow_single_shared", true),
            blockExploreAndSuggested = prefs.getBoolean("block_explore", true),
            blockStories = prefs.getBoolean("block_stories", false),
            dailyLimitMinutes = prefs.getInt("daily_limit_minutes", 45),
            enforceDailyLimit = prefs.getBoolean("enforce_daily_limit", false)
        )
    }

    fun saveSettings(settings: FilterSettings) {
        prefs.edit()
            .putBoolean("block_reels_feed", settings.blockReelsFeed)
            .putBoolean("block_shorts_feed", settings.blockShortsFeed)
            .putBoolean("allow_single_shared", settings.allowSingleSharedLinks)
            .putBoolean("block_explore", settings.blockExploreAndSuggested)
            .putBoolean("block_stories", settings.blockStories)
            .putInt("daily_limit_minutes", settings.dailyLimitMinutes)
            .putBoolean("enforce_daily_limit", settings.enforceDailyLimit)
            .apply()
    }

    fun getBlockedCount(): Int {
        resetIfNewDay()
        return prefs.getInt("blocked_count", 0)
    }

    fun incrementBlockedCount(delta: Int = 1) {
        resetIfNewDay()
        val current = prefs.getInt("blocked_count", 0)
        prefs.edit().putInt("blocked_count", current + delta).apply()
    }

    fun getMinutesUsedToday(): Int {
        resetIfNewDay()
        return prefs.getInt("minutes_used_today", 0)
    }

    fun addMinuteUsed() {
        resetIfNewDay()
        val current = prefs.getInt("minutes_used_today", 0)
        prefs.edit().putInt("minutes_used_today", current + 1).apply()
    }

    private fun resetIfNewDay() {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("last_date", "")
        if (savedDate != today) {
            prefs.edit()
                .putString("last_date", today)
                .putInt("blocked_count", 0)
                .putInt("minutes_used_today", 0)
                .apply()
        }
    }
}
