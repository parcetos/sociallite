package com.sociallite.pro

import android.net.Uri

sealed interface UrlDecision {
    data object Allow : UrlDecision
    data class Redirect(val url: String) : UrlDecision
    data object Block : UrlDecision
}

object ContentFilterEngine {

    const val DESKTOP_COMPAT_MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36"

    fun normalizePath(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            val raw = (uri.path ?: "/").trimEnd('/').ifEmpty { "/" }
            // Normalize /reels/<id> and /reel/<id> so Instagram internal redirects match
            if (raw.startsWith("/reels/")) {
                "/reel/" + raw.removePrefix("/reels/")
            } else {
                raw
            }
        }.getOrDefault("/")
    }

    fun isShortFormVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull().orEmpty()
        return lower.contains("/reels") ||
            lower.contains("/reel/") ||
            lower.contains("/shorts") ||
            host == "fb.watch" ||
            (lower.contains("/watch") && host.contains("facebook.com"))
    }

    fun evaluateUrl(
        url: String,
        platform: SocialPlatform,
        config: PlatformFilterConfig,
        allowedSingleSession: SharedLinkSession?
    ): UrlDecision {
        val lower = url.lowercase()
        val path = normalizePath(url)

        if (allowedSingleSession != null && allowedSingleSession.platform == platform) {
            if (path == allowedSingleSession.normalizedPath) {
                return UrlDecision.Allow
            }
        }

        val fallbackUrl = if (config.dmOrSubscriptionsOnly) platform.dmUrl else platform.homeUrl

        if (config.blockShortVideos) {
            when (platform) {
                SocialPlatform.INSTAGRAM -> {
                    if (lower.contains("/reels") || lower.contains("/reel/")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                SocialPlatform.YOUTUBE -> {
                    if (lower.contains("/shorts")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                SocialPlatform.FACEBOOK -> {
                    if (lower.contains("/reel") || lower.contains("/watch") || lower.contains("fb.watch")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                else -> Unit
            }
        }

        if (config.blockExploreAndDiscovery) {
            when (platform) {
                SocialPlatform.INSTAGRAM -> {
                    if (lower.contains("/explore")) return UrlDecision.Redirect(fallbackUrl)
                }
                SocialPlatform.YOUTUBE -> {
                    if (lower.contains("/feed/explore") || lower.contains("/feed/trending")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                SocialPlatform.X_TWITTER -> {
                    if (lower.contains("/explore") || lower.contains("/i/trends")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                SocialPlatform.REDDIT -> {
                    if (lower.contains("/r/popular") || lower.contains("/r/all")) {
                        return UrlDecision.Redirect(fallbackUrl)
                    }
                }
                else -> Unit
            }
        }

        if (config.dmOrSubscriptionsOnly) {
            when (platform) {
                SocialPlatform.INSTAGRAM -> {
                    if (path == "/" || path == "") {
                        return UrlDecision.Redirect(platform.dmUrl)
                    }
                }
                SocialPlatform.YOUTUBE -> {
                    if (path == "/" || path == "") {
                        return UrlDecision.Redirect(platform.dmUrl)
                    }
                }
                SocialPlatform.X_TWITTER -> {
                    if (path == "/" || path == "/home") {
                        return UrlDecision.Redirect(platform.dmUrl)
                    }
                }
                else -> Unit
            }
        }

        return UrlDecision.Allow
    }

    fun buildInjectionScript(
        platform: SocialPlatform,
        config: PlatformFilterConfig,
        isSingleVideoLocked: Boolean
    ): String {
        val cssRules = buildString {
            append("""
                [data-testid="app-banner"],
                div[role="dialog"] a[href*="intent://"],
                .open-in-app-banner {
                    display: none !important;
                }
            """.trimIndent())

            if (config.blockShortVideos && !isSingleVideoLocked) {
                when (platform) {
                    SocialPlatform.INSTAGRAM -> append("""
                        a[href*="/reels"],
                        a[href*="/reel/"],
                        svg[aria-label="Reels"],
                        div[role="tablist"] a[href*="reels"] {
                            display: none !important;
                        }
                    """.trimIndent())
                    SocialPlatform.YOUTUBE -> append("""
                        ytm-pivot-bar-item-renderer:has(.pivot-shorts),
                        ytm-reel-shelf-renderer,
                        ytm-rich-section-renderer:has(a[href*="/shorts"]),
                        a[href*="/shorts"] {
                            display: none !important;
                        }
                    """.trimIndent())
                    SocialPlatform.FACEBOOK -> append("""
                        a[href*="/reel"],
                        a[href*="/watch"],
                        div[data-pagelet*="Reels"] {
                            display: none !important;
                        }
                    """.trimIndent())
                    else -> Unit
                }
            }

            if (config.blockExploreAndDiscovery) {
                when (platform) {
                    SocialPlatform.INSTAGRAM -> append("""
                        a[href="/explore/"],
                        a[href*="/explore"],
                        svg[aria-label="Search & Explore"] {
                            display: none !important;
                        }
                    """.trimIndent())
                    SocialPlatform.X_TWITTER -> append("""
                        a[href="/explore"],
                        div[aria-label="Timeline: Trending now"] {
                            display: none !important;
                        }
                    """.trimIndent())
                    else -> Unit
                }
            }

            if (isSingleVideoLocked) {
                append("""
                    html, body {
                        overflow: hidden !important;
                        overscroll-behavior: none !important;
                        touch-action: none !important;
                    }
                    #sociallite-lock-badge {
                        position: fixed;
                        top: 12px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: rgba(15, 23, 42, 0.88);
                        color: #38BDF8;
                        border: 1px solid rgba(56, 189, 248, 0.45);
                        padding: 6px 14px;
                        border-radius: 999px;
                        font-family: system-ui, -apple-system, sans-serif;
                        font-size: 12px;
                        font-weight: 600;
                        z-index: 2147483647;
                        pointer-events: none;
                        backdrop-filter: blur(8px);
                    }
                """.trimIndent())
            }
        }

        val encodedCss = cssRules
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        return """
            (function() {
                const STYLE_ID = 'sociallite-pro-style';
                let styleEl = document.getElementById(STYLE_ID);
                if (!styleEl) {
                    styleEl = document.createElement('style');
                    styleEl.id = STYLE_ID;
                    (document.head || document.documentElement).appendChild(styleEl);
                }
                styleEl.textContent = `$encodedCss`;

                const PLATFORM_ID = "${platform.id}";
                const BLOCK_SHORTS = ${config.blockShortVideos};
                const BLOCK_EXPLORE = ${config.blockExploreAndDiscovery};
                const BLOCK_ADS = ${config.blockAdsAndSuggestions};
                const DM_ONLY = ${config.dmOrSubscriptionsOnly};
                const SINGLE_LOCK = $isSingleVideoLocked;
                const FALLBACK_URL = "${if (config.dmOrSubscriptionsOnly) platform.dmUrl else platform.homeUrl}";

                function enforceSpaRouteGuard() {
                    if (SINGLE_LOCK) return;
                    const href = window.location.href.toLowerCase();
                    const path = window.location.pathname;

                    if (BLOCK_SHORTS) {
                        if (
                            href.includes('/reels') ||
                            href.includes('/reel/') ||
                            href.includes('/shorts') ||
                            (PLATFORM_ID === 'facebook' && href.includes('/watch'))
                        ) {
                            window.location.replace(FALLBACK_URL);
                            return;
                        }
                    }
                    if (BLOCK_EXPLORE && (href.includes('/explore') || href.includes('/r/popular') || href.includes('/r/all'))) {
                        window.location.replace(FALLBACK_URL);
                        return;
                    }
                    if (DM_ONLY && (path === '/' || path === '/home')) {
                        window.location.replace(FALLBACK_URL);
                    }
                }

                if (!window.__socialliteHistoryHooked) {
                    window.__socialliteHistoryHooked = true;
                    const origPush = history.pushState;
                    const origReplace = history.replaceState;
                    history.pushState = function() {
                        const res = origPush.apply(this, arguments);
                        enforceSpaRouteGuard();
                        return res;
                    };
                    history.replaceState = function() {
                        const res = origReplace.apply(this, arguments);
                        enforceSpaRouteGuard();
                        return res;
                    };
                    window.addEventListener('popstate', enforceSpaRouteGuard);
                }

                function scrubDomElements() {
                    if (BLOCK_SHORTS && !SINGLE_LOCK) {
                        document.querySelectorAll('article, ytm-rich-item-renderer, ytm-video-with-context-renderer').forEach(node => {
                            if (node.querySelector('a[href*="/reel/"], a[href*="/reels"], a[href*="/shorts"]')) {
                                node.style.display = 'none';
                            }
                        });
                    }

                    if (BLOCK_ADS) {
                        document.querySelectorAll('article, ytm-promoted-sparkles-web-renderer, shreddit-ad-post').forEach(node => {
                            const text = node.innerText || '';
                            if (
                                text.includes('Sponsored') ||
                                text.includes('Suggested for you') ||
                                text.includes('مُموَّل') ||
                                text.includes('اقتراحات لك')
                            ) {
                                node.style.display = 'none';
                            }
                        });
                    }
                }

                if (SINGLE_LOCK && !window.__socialliteScrollLocked) {
                    window.__socialliteScrollLocked = true;
                    if (!document.getElementById('sociallite-lock-badge')) {
                        const badge = document.createElement('div');
                        badge.id = 'sociallite-lock-badge';
                        badge.textContent = 'Single-Link Safe Mode • Scroll Locked';
                        document.documentElement.appendChild(badge);
                    }

                    window.__socialliteBlockScrollFn = function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                    };
                    window.addEventListener('wheel', window.__socialliteBlockScrollFn, { passive: false, capture: true });
                    window.addEventListener('touchmove', window.__socialliteBlockScrollFn, { passive: false, capture: true });
                } else if (!SINGLE_LOCK && window.__socialliteScrollLocked) {
                    window.__socialliteScrollLocked = false;
                    const badge = document.getElementById('sociallite-lock-badge');
                    if (badge) badge.remove();
                    if (window.__socialliteBlockScrollFn) {
                        window.removeEventListener('wheel', window.__socialliteBlockScrollFn, { capture: true });
                        window.removeEventListener('touchmove', window.__socialliteBlockScrollFn, { capture: true });
                    }
                }

                enforceSpaRouteGuard();
                scrubDomElements();

                if (!window.__socialliteObserver) {
                    window.__socialliteObserver = new MutationObserver(() => {
                        scrubDomElements();
                    });
                    window.__socialliteObserver.observe(document.documentElement, {
                        childList: true,
                        subtree: true
                    });
                }
            })();
        """.trimIndent()
    }
}
