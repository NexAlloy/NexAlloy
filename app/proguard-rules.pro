# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-optimizations
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class io.github.nexalloy.MainHook { <init>(); }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepnames class * extends io.github.nexalloy.morphe.Fingerprint
-keepclassmembers class **.* {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.graphics.drawable.Drawable { public <init>(...); }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class app.morphe.extension.shared.settings.* {
    public <init>(...);
}