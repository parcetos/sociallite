# Keep WebView JavaScript interfaces and Kotlin metadata
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
