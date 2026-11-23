package com.sociallite.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val incomingSharedSession = mutableStateOf<SharedLinkSession?>(null)
    private val activePlatformState = mutableStateOf<SocialPlatform?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        val prefsManager = PreferencesManager(applicationContext)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val lifecycleOwner = LocalLifecycleOwner.current
                val configs by prefsManager.configs.collectAsState()
                val usageSeconds by prefsManager.usageSeconds.collectAsState()

                var activePlatform by activePlatformState
                var sharedSession by incomingSharedSession
                var editingPlatform by remember { mutableStateOf<SocialPlatform?>(null) }
                var isAppInForeground by remember { mutableStateOf(true) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                            Lifecycle.Event.ON_PAUSE -> isAppInForeground = false
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF090D16)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val currentPlatform = activePlatform
                        if (currentPlatform == null) {
                            ProHomeDashboard(
                                configs = configs,
                                usageSeconds = usageSeconds,
                                onOpenPlatform = { selected ->
                                    sharedSession = null
                                    activePlatform = selected
                                },
                                onConfigurePlatform = { target ->
                                    editingPlatform = target
                                }
                            )
                        } else {
                            val platformConfig = configs[currentPlatform] ?: PlatformFilterConfig()
                            val usedSec = usageSeconds[currentPlatform] ?: 0
                            val isTimeExpired = usedSec >= platformConfig.dailyLimitMinutes * 60

                            LaunchedEffect(currentPlatform, isTimeExpired, isAppInForeground) {
                                while (!isTimeExpired && isAppInForeground) {
                                    delay(1000L)
                                    prefsManager.incrementUsageSecond(currentPlatform)
                                }
                            }

                            if (isTimeExpired) {
                                TimeLimitLockOverlay(
                                    platform = currentPlatform,
                                    limitMinutes = platformConfig.dailyLimitMinutes,
                                    onBackToDashboard = {
                                        sharedSession = null
                                        activePlatform = null
                                    }
                                )
                            } else {
                                ActivePlatformBrowser(
                                    platform = currentPlatform,
                                    config = platformConfig,
                                    usedSeconds = usedSec,
                                    sharedLinkSession = sharedSession,
                                    onOpenSettings = { editingPlatform = currentPlatform },
                                    onClearSharedSession = { sharedSession = null },
                                    onExitPlatform = {
                                        sharedSession = null
                                        activePlatform = null
                                    }
                                )
                            }
                        }

                        editingPlatform?.let { target ->
                            PlatformFilterSheet(
                                platform = target,
                                config = configs[target] ?: PlatformFilterConfig(),
                                onUpdateConfig = { transform ->
                                    prefsManager.updateConfig(target, transform)
                                },
                                onDismiss = { editingPlatform = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val rawUrl = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                Regex("""https?://\S+""").find(sharedText)?.value
            }
            else -> null
        } ?: return

        val detectedPlatform = SocialPlatform.fromUrl(rawUrl) ?: return
        val session = SharedLinkSession(
            platform = detectedPlatform,
            targetUrl = rawUrl,
            normalizedPath = ContentFilterEngine.normalizePath(rawUrl),
            lockVerticalScroll = ContentFilterEngine.isShortFormVideoUrl(rawUrl)
        )

        incomingSharedSession.value = session
        activePlatformState.value = detectedPlatform
    }
}

@Composable
private fun ActivePlatformBrowser(
    platform: SocialPlatform,
    config: PlatformFilterConfig,
    usedSeconds: Int,
    sharedLinkSession: SharedLinkSession?,
    onOpenSettings: () -> Unit,
    onClearSharedSession: () -> Unit,
    onExitPlatform: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var homeTrigger by remember { mutableIntStateOf(0) }

    val remainingSeconds = (config.dailyLimitMinutes * 60 - usedSeconds).coerceAtLeast(0)
    val remainingMin = remainingSeconds / 60
    val remainingSec = remainingSeconds % 60

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExitPlatform) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = platform.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (sharedLinkSession?.lockVerticalScroll == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Single Link Safe Mode",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d left", remainingMin, remainingSec),
                        color = if (remainingSeconds < 120) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { homeTrigger++ }) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Platform Home",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = { reloadTrigger++ }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reload",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Guardrails",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = platform.accentColor,
                trackColor = Color.Transparent
            )
        }

        SocialWebView(
            platform = platform,
            config = config,
            sharedLinkSession = sharedLinkSession,
            reloadTrigger = reloadTrigger,
            homeTrigger = homeTrigger,
            onUrlChanged = {},
            onLoadingChanged = { isLoading = it },
            onClearSharedSession = onClearSharedSession,
            onExitPlatform = onExitPlatform,
            modifier = Modifier.weight(1f)
        )
    }
}
