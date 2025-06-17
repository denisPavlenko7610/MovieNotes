# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.firebase.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity

# GSON (если используется)
-keep class com.google.gson.** { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Корректная работа Firebase с ProGuard
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses