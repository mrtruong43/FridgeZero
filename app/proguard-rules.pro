# Preserve WebView JavascriptInterface bridge methods under R8/ProGuard minification
-keepattributes JavascriptInterface, RuntimeVisibleAnnotations, *Annotation*

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.truong.fridgezero.ui.fridge3d.FridgeJsBridge { *; }
-keepclassmembers class com.truong.fridgezero.ui.fridge3d.FridgeJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
