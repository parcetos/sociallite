package com.sociallite.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.ExploreOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ProHomeDashboard(
    configs: Map<SocialPlatform, PlatformFilterConfig>,
    usageSeconds: Map<SocialPlatform, Int>,
    onOpenPlatform: (SocialPlatform) -> Unit,
    onConfigurePlatform: (SocialPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUsedMinutes = usageSeconds.values.sum() / 60

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SocialLite",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Intentional browsing • Zero algorithmic traps",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = Color(0xFF111827),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${totalUsedMinutes}m today",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        items(SocialPlatform.entries) { platform ->
            val cfg = configs[platform] ?: PlatformFilterConfig()
            val usedSec = usageSeconds[platform] ?: 0
            val usedMin = usedSec / 60
            val limitMin = cfg.dailyLimitMinutes
            val progress = (usedSec.toFloat() / (limitMin * 60f)).coerceIn(0f, 1f)
            val isLocked = usedSec >= limitMin * 60

            Surface(
                color = Color(0xFF111827),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isLocked) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onOpenPlatform(platform) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(platform.accentColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = if (isLocked) Color(0xFFEF4444) else platform.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = platform.displayName,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = buildActiveFilterSummary(cfg),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = { onConfigurePlatform(platform) }) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Configure ${platform.displayName}",
                                tint = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLocked) "Daily Limit Reached" else "Daily Budget",
                            color = if (isLocked) Color(0xFFEF4444) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$usedMin / $limitMin min",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isLocked) Color(0xFFEF4444) else platform.accentColor,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

private fun buildActiveFilterSummary(cfg: PlatformFilterConfig): String {
    val active = mutableListOf<String>()
    if (cfg.blockShortVideos) active.add("No Reels/Shorts")
    if (cfg.blockExploreAndDiscovery) active.add("No Explore")
    if (cfg.dmOrSubscriptionsOnly) active.add("Focus Inbox")
    if (cfg.grayscaleMode) active.add("Grayscale")
    return if (active.isEmpty()) "Unfiltered mode" else active.joinToString(" • ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformFilterSheet(
    platform: SocialPlatform,
    config: PlatformFilterConfig,
    onUpdateConfig: ((PlatformFilterConfig) -> PlatformFilterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111827),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${platform.displayName} Pro Guardrails",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            FilterToggleRow(
                icon = Icons.Rounded.VisibilityOff,
                title = "Block Short-Form Videos",
                subtitle = "Removes Reels, Shorts & vertical video shelves",
                checked = config.blockShortVideos,
                onCheckedChange = { v -> onUpdateConfig { it.copy(blockShortVideos = v) } }
            )

            FilterToggleRow(
                icon = Icons.Rounded.ExploreOff,
                title = "Block Explore & Trending",
                subtitle = "Prevents algorithmic discovery rabbit holes",
                checked = config.blockExploreAndDiscovery,
                onCheckedChange = { v -> onUpdateConfig { it.copy(blockExploreAndDiscovery = v) } }
            )

            FilterToggleRow(
                icon = Icons.Rounded.ChatBubble,
                title = "DMs / Subscriptions Only",
                subtitle = "Redirects home feed straight to messages or subscriptions",
                checked = config.dmOrSubscriptionsOnly,
                onCheckedChange = { v -> onUpdateConfig { it.copy(dmOrSubscriptionsOnly = v) } }
            )

            FilterToggleRow(
                icon = Icons.Rounded.Block,
                title = "Hide Sponsored & Suggested Posts",
                subtitle = "Scrubs injected feed recommendations in real time",
                checked = config.blockAdsAndSuggestions,
                onCheckedChange = { v -> onUpdateConfig { it.copy(blockAdsAndSuggestions = v) } }
            )

            FilterToggleRow(
                icon = Icons.Rounded.Contrast,
                title = "Dopamine-Detox Grayscale",
                subtitle = "Renders WebView in hardware-accelerated monochrome",
                checked = config.grayscaleMode,
                onCheckedChange = { v -> onUpdateConfig { it.copy(grayscaleMode = v) } }
            )

            HorizontalDivider(color = Color(0xFF1E293B))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Time Limit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "${config.dailyLimitMinutes} min / day",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Slider(
                    value = config.dailyLimitMinutes.toFloat(),
                    onValueChange = { raw ->
                        val stepped = ((raw / 5f).roundToInt() * 5).coerceIn(5, 180)
                        onUpdateConfig { it.copy(dailyLimitMinutes = stepped) }
                    },
                    valueRange = 5f..180f
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF090D16))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Guardrails", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterToggleRow(
    icon: ImageVector,
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
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun TimeLimitLockOverlay(
    platform: SocialPlatform,
    limitMinutes: Int,
    onBackToDashboard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "${platform.displayName} Locked for Today",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You reached your $limitMinutes-minute daily budget. Take a break and reclaim your focus.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBackToDashboard,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("Return to Dashboard", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }
        }
    }
}
