package com.sociallite.pro

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SocialWebView(
    platform: SocialPlatform,
    targetUrl: String,
    allowedSingleUrl: String?,
    settings: FilterSettings,
    reloadTrigger: Int,
    onUrlChanged: (String) -> Unit,
    onDistractionBlocked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }

    val webView = remember(platform) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    LaunchedEffect(targetUrl, reloadTrigger) {
        if (targetUrl.isNotBlank() && webView.url != targetUrl) {
            webView.loadUrl(targetUrl)
        } else if (reloadTrigger > 0) {
            webView.reload()
        }
    }

    DisposableEffect(webView, settings, allowedSingleUrl) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val nextUrl = request?.url?.toString() ?: return false
                if (ContentFilterEngine.shouldBlockNavigation(nextUrl, platform, settings, allowedSingleUrl)) {
                    onDistractionBlocked("Blocked distraction feed: $nextUrl")
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isLoading = true
                if (url != null) {
                    onUrlChanged(url)
                    val isSingle = allowedSingleUrl != null &&
                        url.substringBefore("?").trimEnd('/')
                            .equals(allowedSingleUrl.substringBefore("?").trimEnd('/'), ignoreCase = true)
                    val script = ContentFilterEngine.buildInjectionScript(platform, settings, isSingle)
                    view?.evaluateJavascript(script, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
                if (url != null) {
                    val isSingle = allowedSingleUrl != null &&
                        url.substringBefore("?").trimEnd('/')
                            .equals(allowedSingleUrl.substringBefore("?").trimEnd('/'), ignoreCase = true)
                    val script = ContentFilterEngine.buildInjectionScript(platform, settings, isSingle)
                    view?.evaluateJavascript(script, null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress / 100f
                if (newProgress in 30..90) {
                    val current = view?.url ?: ""
                    val isSingle = allowedSingleUrl != null &&
                        current.substringBefore("?").trimEnd('/')
                            .equals(allowedSingleUrl.substringBefore("?").trimEnd('/'), ignoreCase = true)
                    val script = ContentFilterEngine.buildInjectionScript(platform, settings, isSingle)
                    view?.evaluateJavascript(script, null)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
            }

            override fun onHideCustomView() {
                customView = null
            }
        }

        onDispose { }
    }

    BackHandler(enabled = customView != null || webView.canGoBack()) {
        if (customView != null) {
            customView = null
        } else if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading && progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        customView?.let { fsView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            addView(
                                fsView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
