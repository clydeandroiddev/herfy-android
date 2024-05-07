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
-keep class com.herfy.menu.adapter.MenuCategoryAdapter.** { *; }
-keep class com.herfy.utils.event.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-keep class com.nguyenhoanglam.imagepicker.** { *; }

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# If using AsyncExecutord, keep required constructor of default event used.
# Adjust the class name if a custom failure event type is used.
-keepclassmembers class org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Accessed via reflection, avoid renaming or removal
-keep class org.greenrobot.eventbus.android.AndroidComponentsImpl
-keep class com.herfy.favorites.FavoriteOrderTypeUIModel { *; }
-keep class com.herfy.menu.adapters.model.ModifierItemClickParams { *; }
-keep class com.moyasar.android.sdk.** { *; }
-keep class android.databinding.** { *; }
