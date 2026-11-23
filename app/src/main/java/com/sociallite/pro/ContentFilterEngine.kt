package com.sociallite.pro

object ContentFilterEngine {

    fun shouldBlockNavigation(
        url: String,
        platform: SocialPlatform,
        settings: FilterSettings,
        allowedSingleUrl: String?
    ): Boolean {
        val lower = url.lowercase()

        if (settings.allowSingleSharedLinks && allowedSingleUrl != null) {
            val cleanAllowed = allowedSingleUrl.substringBefore("?").trimEnd('/')
            val cleanCurrent = lower.substringBefore("?").trimEnd('/')
            if (cleanAllowed.isNotEmpty() && cleanCurrent.equals(cleanAllowed, ignoreCase = true)) {
                return false
            }
        }

        return when (platform) {
            SocialPlatform.INSTAGRAM -> {
                if (settings.blockReelsFeed) {
                    val isReelsTab = lower.endsWith("/reels") || lower.endsWith("/reels/") ||
                        lower.contains("instagram.com/reels/")
                    val isDirectReel = lower.contains("/reel/")
                    if (isReelsTab) return true
                    if (isDirectReel && !settings.allowSingleSharedLinks) return true
                }
                if (settings.blockExploreAndSuggested && lower.contains("instagram.com/explore")) {
                    return true
                }
                false
            }

            SocialPlatform.YOUTUBE -> {
                if (settings.blockShortsFeed) {
                    val isShortsRoot = lower.endsWith("/shorts") || lower.endsWith("/shorts/")
                    val isSpecificShort = lower.contains("/shorts/") && !isShortsRoot
                    if (isShortsRoot) return true
                    if (isSpecificShort && !settings.allowSingleSharedLinks) return true
                }
                false
            }

            SocialPlatform.X_TWITTER -> {
                if (settings.blockExploreAndSuggested && lower.contains("/explore")) {
                    return true
                }
                false
            }

            SocialPlatform.FACEBOOK -> {
                if (settings.blockReelsFeed && (lower.contains("/reel") || lower.contains("/watch"))) {
                    return true
                }
                false
            }
        }
    }

    fun buildInjectionScript(
        platform: SocialPlatform,
        settings: FilterSettings,
        isSingleSharedView: Boolean
    ): String {
        val cssRules = StringBuilder()

        when (platform) {
            SocialPlatform.INSTAGRAM -> {
                if (settings.blockReelsFeed) {
                    cssRules.append(
                        """
                        a[href*="/reels"], a[href="/reels/"], svg[aria-label="Reels"] {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
                if (settings.blockExploreAndSuggested) {
                    cssRules.append(
                        """
                        a[href*="/explore"], svg[aria-label="Explore"] {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
                if (settings.blockStories) {
                    cssRules.append(
                        """
                        div[role="menu"] ul, section main > div > div:first-child ul {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
            }

            SocialPlatform.YOUTUBE -> {
                if (settings.blockShortsFeed) {
                    cssRules.append(
                        """
                        ytm-reel-shelf-renderer,
                        ytd-reel-shelf-renderer,
                        ytm-pivot-bar-item-renderer:has(a[href*="/shorts"]),
                        a[href^="/shorts"] {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
                if (settings.blockExploreAndSuggested) {
                    cssRules.append(
                        """
                        ytm-single-column-watch-next-results-renderer ytm-item-section-renderer {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
            }

            SocialPlatform.X_TWITTER -> {
                if (settings.blockExploreAndSuggested) {
                    cssRules.append(
                        """
                        a[href="/explore"], div[aria-label="Timeline: Trending now"] {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
            }

            SocialPlatform.FACEBOOK -> {
                if (settings.blockReelsFeed) {
                    cssRules.append(
                        """
                        a[href*="/reel"], a[href*="/watch"], div[aria-label*="Reels"] {
                            display: none !important;
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        val lockSwipeJs = if (isSingleSharedView) {
            """
            document.documentElement.style.overflow = 'hidden';
            document.body.style.overflow = 'hidden';
            """.trimIndent()
        } else {
            ""
        }

        val escapedCss = cssRules.toString().replace("\n", " ").replace("'", "\\'")

        return """
            (function() {
                try {
                    var styleId = 'sociallite-pro-filter-style';
                    var styleEl = document.getElementById(styleId);
                    if (!styleEl) {
                        styleEl = document.createElement('style');
                        styleEl.id = styleId;
                        document.head.appendChild(styleEl);
                    }
                    styleEl.textContent = '$escapedCss';
                    $lockSwipeJs

                    function cleanFeed() {
                        if (${settings.blockExploreAndSuggested}) {
                            document.querySelectorAll('article').forEach(function(art) {
                                var txt = art.innerText || '';
                                if (txt.includes('Suggested for you') || txt.includes('مقترحة لك')) {
                                    art.style.display = 'none';
                                }
                            });
                        }
                        if (${settings.blockShortsFeed}) {
                            document.querySelectorAll('ytm-reel-shelf-renderer, ytm-shorts-lockup-view-model').forEach(function(el) {
                                el.style.display = 'none';
                            });
                        }
                    }

                    cleanFeed();
                    if (!window.__socialLiteObserver) {
                        window.__socialLiteObserver = new MutationObserver(function() {
                            cleanFeed();
                        });
                        window.__socialLiteObserver.observe(document.body, { childList: true, subtree: true });
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }
}
