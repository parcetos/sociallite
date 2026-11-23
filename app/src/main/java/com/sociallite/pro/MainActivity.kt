package com.sociallite.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var incomingUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractIncomingUrl(intent)?.let { incomingUrlState.value = it }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SocialLiteProApp(
                    prefsManager = remember { PreferencesManager(applicationContext) },
                    incomingUrl = incomingUrlState.value,
                    onConsumeIncomingUrl = { incomingUrlState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingUrl(intent)?.let { incomingUrlState.value = it }
    }

    private fun extractIncomingUrl(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_VIEW) {
            return intent.dataString
        }
        if (intent.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            val regex = Regex("https?://[^\\s]+")
            return regex.find(shared)?.value
        }
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialLiteProApp(
    prefsManager: PreferencesManager,
    incomingUrl: String?,
    onConsumeIncomingUrl: () -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(SocialPlatform.INSTAGRAM) }
    var showSettings by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(prefsManager.loadSettings()) }
    var blockedCount by remember { mutableIntStateOf(prefsManager.getBlockedCount()) }
    var minutesUsed by remember { mutableIntStateOf(prefsManager.getMinutesUsedToday()) }

    var activeUrl by remember { mutableStateOf(SocialPlatform.INSTAGRAM.homeUrl) }
    var allowedSingleUrl by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            val detected = SocialPlatform.fromUrl(incomingUrl)
            if (detected != null) {
                selectedPlatform = detected
                showSettings = false
                allowedSingleUrl = incomingUrl
                activeUrl = incomingUrl
            }
            onConsumeIncomingUrl()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            prefsManager.addMinuteUsed()
            minutesUsed = prefsManager.getMinutesUsedToday()
        }
    }

    val limitReached = settings.enforceDailyLimit && minutesUsed >= settings.dailyLimitMinutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showSettings) "SocialLite Pro" else selectedPlatform.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = {
                            allowedSingleUrl = null
                            activeUrl = selectedPlatform.homeUrl
                            reloadTrigger++
                        }) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                        IconButton(onClick = {
                            allowedSingleUrl = null
                            activeUrl = selectedPlatform.dmUrl
                            reloadTrigger++
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                        }
                        IconButton(onClick = { reloadTrigger++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Shield, contentDescription = "Focus Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                SocialPlatform.entries.forEach { platform ->
                    NavigationBarItem(
                        selected = !showSettings && selectedPlatform == platform,
                        onClick = {
                            showSettings = false
                            selectedPlatform = platform
                            allowedSingleUrl = null
                            activeUrl = platform.homeUrl
                        },
                        icon = {
                            Text(
                                text = platform.title.take(2).uppercase(),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = { Text(platform.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showSettings -> {
                    ProDashboardAndSettings(
                        settings = settings,
                        blockedToday = blockedCount,
                        minutesUsedToday = minutesUsed,
                        onSettingsChanged = { updated ->
                            settings = updated
                            prefsManager.saveSettings(updated)
                        }
                    )
                }

                limitReached -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Daily Limit Reached (${settings.dailyLimitMinutes}m)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Take a break or adjust your limit in Focus Settings.",
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Button(onClick = { showSettings = true }) {
                            Text("Open Focus Settings")
                        }
                    }
                }

                else -> {
                    SocialWebView(
                        platform = selectedPlatform,
                        targetUrl = activeUrl,
                        allowedSingleUrl = allowedSingleUrl,
                        settings = settings,
                        reloadTrigger = reloadTrigger,
                        onUrlChanged = { newUrl -> activeUrl = newUrl },
                        onDistractionBlocked = { _ ->
                            prefsManager.incrementBlockedCount()
                            blockedCount = prefsManager.getBlockedCount()
                            scope.launch {
                                snackbarHostState.showSnackbar("Blocked Reels/Shorts feed")
                            }
                        }
                    )
                }
            }
        }
    }
}
