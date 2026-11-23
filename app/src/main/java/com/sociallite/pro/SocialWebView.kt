package com.sociallite.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SocialWebView(
    platform: SocialPlatform,
    config: PlatformFilterConfig,
    sharedLinkSession: SharedLinkSession?,
    reloadTrigger: Int,
    homeTrigger: Int,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onClearSharedSession: () -> Unit,
    onExitPlatform: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentConfig by rememberUpdatedState(config)
    val currentSharedSession by rememberUpdatedState(sharedLinkSession)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val grayscalePaint = remember(config.grayscaleMode) {
        Paint().apply {
            if (config.grayscaleMode) {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
        }
    }

    val webView = remember(platform) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply, true)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                userAgentString = ContentFilterEngine.DESKTOP_COMPAT_MOBILE_UA
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback
                    val mime = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                    fileChooserLauncher.launch(mime)
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                private fun injectRules(view: WebView?, url: String?) {
                    val activeUrl = url ?: return
                    val session = currentSharedSession
                    val isLockedSingleVideo = session != null &&
                        session.lockVerticalScroll &&
                        session.platform == platform &&
                        ContentFilterEngine.normalizePath(activeUrl) == session.normalizedPath

                    val script = ContentFilterEngine.buildInjectionScript(
                        platform = platform,
                        config = currentConfig,
                        isSingleVideoLocked = isLockedSingleVideo
                    )
                    view?.evaluateJavascript(script, null)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    val scheme = request.url?.scheme?.lowercase().orEmpty()

                    if (scheme != "http" && scheme != "https") {
                        if (scheme in listOf("mailto", "tel")) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            }
                        }
                        return true
                    }

                    val decision = ContentFilterEngine.evaluateUrl(
                        url = url,
                        platform = platform,
                        config = currentConfig,
                        allowedSingleSession = currentSharedSession
                    )

                    return when (decision) {
                        UrlDecision.Allow -> {
                            val session = currentSharedSession
                            if (session != null &&
                                ContentFilterEngine.normalizePath(url) != session.normalizedPath
                            ) {
                                onClearSharedSession()
                            }
                            false
                        }
                        is UrlDecision.Redirect -> {
                            onClearSharedSession()
                            view?.loadUrl(decision.url)
                            true
                        }
                        UrlDecision.Block -> true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onLoadingChanged(true)
                    url?.let(onUrlChanged)
                    injectRules(view, url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onLoadingChanged(false)
                    url?.let(onUrlChanged)
                    injectRules(view, url)
                    CookieManager.getInstance().flush()
                }
            }

            val initialUrl = when {
                sharedLinkSession != null && sharedLinkSession.platform == platform ->
                    sharedLinkSession.targetUrl
                config.dmOrSubscriptionsOnly -> platform.dmUrl
                else -> platform.homeUrl
            }
            loadUrl(initialUrl)
        }.also { webViewRef = it }
    }

    // Navigate immediately if a new shared link arrives while this platform is already open
    LaunchedEffect(sharedLinkSession?.targetUrl) {
        val target = sharedLinkSession ?: return@LaunchedEffect
        if (target.platform == platform && webView.url != target.targetUrl) {
            webView.loadUrl(target.targetUrl)
        }
    }

    // Re-inject rules live when toggles change
    LaunchedEffect(config, sharedLinkSession) {
        val currentUrl = webView.url ?: return@LaunchedEffect
        val isLocked = sharedLinkSession != null &&
            sharedLinkSession.lockVerticalScroll &&
            sharedLinkSession.platform == platform &&
            ContentFilterEngine.normalizePath(currentUrl) == sharedLinkSession.normalizedPath
        webView.evaluateJavascript(
            ContentFilterEngine.buildInjectionScript(platform, config, isLocked),
            null
        )
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webView.reload()
        }
    }

    LaunchedEffect(homeTrigger) {
        if (homeTrigger > 0) {
            onClearSharedSession()
            val destination = if (currentConfig.dmOrSubscriptionsOnly) platform.dmUrl else platform.homeUrl
            webView.loadUrl(destination)
        }
    }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onExitPlatform()
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    webView.onPause()
                    CookieManager.getInstance().flush()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            if (config.grayscaleMode) {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, grayscalePaint)
            } else {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
