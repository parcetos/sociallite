package com.sociallite.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProDashboardAndSettings(
    settings: FilterSettings,
    blockedToday: Int,
    minutesUsedToday: Int,
    onSettingsChanged: (FilterSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Focus Shield Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Distractions Blocked",
                value = blockedToday.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Active Minutes Today",
                value = "${minutesUsedToday}m",
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Feed & Short-Video Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SettingSwitchRow(
                    title = "Block Instagram & Facebook Reels",
                    subtitle = "Hides Reels tabs and prevents infinite scrolling",
                    checked = settings.blockReelsFeed,
                    onCheckedChange = { onSettingsChanged(settings.copy(blockReelsFeed = it)) }
                )

                HorizontalDivider()

                SettingSwitchRow(
                    title = "Block YouTube Shorts",
                    subtitle = "Removes Shorts shelves and Shorts navigation tab",
                    checked = settings.blockShortsFeed,
                    onCheckedChange = { onSettingsChanged(settings.copy(blockShortsFeed = it)) }
                )

                HorizontalDivider()

                SettingSwitchRow(
                    title = "Allow Single Shared Reels / Shorts",
                    subtitle = "Lets you watch a specific link sent in DMs without swiping",
                    checked = settings.allowSingleSharedLinks,
                    onCheckedChange = { onSettingsChanged(settings.copy(allowSingleSharedLinks = it)) }
                )

                HorizontalDivider()

                SettingSwitchRow(
                    title = "Block Explore & Suggested Posts",
                    subtitle = "Hides algorithmic discovery tabs and suggested cards",
                    checked = settings.blockExploreAndSuggested,
                    onCheckedChange = { onSettingsChanged(settings.copy(blockExploreAndSuggested = it)) }
                )

                HorizontalDivider()

                SettingSwitchRow(
                    title = "Hide Stories Bar",
                    subtitle = "Removes top stories row on Instagram",
                    checked = settings.blockStories,
                    onCheckedChange = { onSettingsChanged(settings.copy(blockStories = it)) }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingSwitchRow(
                    title = "Daily Time Limit",
                    subtitle = "Pause browsing after ${settings.dailyLimitMinutes} minutes/day",
                    checked = settings.enforceDailyLimit,
                    onCheckedChange = { onSettingsChanged(settings.copy(enforceDailyLimit = it)) }
                )

                Slider(
                    value = settings.dailyLimitMinutes.toFloat(),
                    onValueChange = {
                        onSettingsChanged(settings.copy(dailyLimitMinutes = it.toInt()))
                    },
                    valueRange = 10f..180f,
                    steps = 16
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
