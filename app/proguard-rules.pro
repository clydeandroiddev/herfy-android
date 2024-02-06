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
-ignorewarnings
-dontwarn okio.**

-keep class com.skylinedynamics.solosdk.api.models.** { *; }
-keep class com.herfy.country.** { *; }
-keep class com.herfy.database.converters.** { *; }
-keep class com.herfy.database.entities.** { *; }
-keep class com.herfy.database.responses.** { *; }
-keep class com.herfy.util.checkout.** { *; }
-keep class com.herfy.util.obfuscator.** { *; }
-keep class com.herfy.promotions.data.** { *; }
-keep class com.herfy.popup.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-keep class com.nguyenhoanglam.imagepicker.** { *; }

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
