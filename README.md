# SocialLite Pro (Android)

Distraction-free social media wrapper built with **Kotlin** and **Jetpack Compose**.

## Features Included
- **Multi-Platform Guardrails:** Instagram, YouTube, X/Twitter, Facebook, and Reddit.
- **Reels / Shorts / Watch Blocker:** Dual-layer URL interception + live DOM/CSS/JS scrubbing + SPA `history.pushState` route guard.
- **Explore & Trending Blocker:** Prevents algorithmic discovery rabbit holes.
- **DMs / Subscriptions Focus Mode:** Redirects home feeds directly to your inbox or subscriptions.
- **Single-Link Safe Mode:** Share a Reel/Short link to SocialLite Pro to watch that single video with vertical swipe scrolling locked.
- **Dopamine-Detox Grayscale:** Hardware-accelerated monochrome rendering per platform.
- **Foreground-Only Daily Time Budgets:** Per-platform daily usage tracker (pauses automatically when app is backgrounded or screen is off) with automatic lock screen.

---

## How to Build the APK

### Option 1: GitHub Actions (Cloud APK Build — No Android Studio Needed)
1. Extract this ZIP archive.
2. Create a new repository on GitHub and upload all files from inside the `SocialLitePro` folder.
3. Open the **Actions** tab in your GitHub repository.
4. The **Build Debug APK** workflow runs automatically and attaches `SocialLite-Pro-debug.apk` under **Artifacts**.

### Option 2: Android Studio (Local)
1. Extract this ZIP archive.
2. Open **Android Studio** -> **Open** -> select the `SocialLitePro` folder.
3. Wait for Gradle Sync to complete.
4. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)** (or click **Run** on your phone).
