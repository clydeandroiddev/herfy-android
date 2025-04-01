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
-keep class com.herfy.model.** { *; }
-keep class com.herfy.enum.** { *; }
-keep class com.herfy.util.hmac.** { *; }

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

# KochavaCore: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.core**

# KochavaCore: Keep annotations used for json serialization.
-keepattributes Signature
-keepattributes *Annotation*

# KochavaCore: BuildConfig
-keep class com.kochava.core.BuildConfig { *; }

# KochavaCore: Google Instant Apps Collection.
-keep class com.google.android.gms.common.wrappers.InstantApps {
    boolean isInstantApp(android.content.Context);
}

# KochavaTracker: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.tracker**

# KochavaTracker: BuildConfig
-keep class com.kochava.tracker.BuildConfig { *; }

# KochavaTracker: Internal SDK
-keep class com.kochava.tracker.modules.internal.Module {
    void setController(***);
    *** getController();
}
-keep class com.kochava.tracker.Tracker {
    static com.kochava.tracker.TrackerApi getInstance();
}

# KochavaTracker: Google ADID Collection.
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}

# KochavaTracker: Google Install Referrer Collection.
-keep class com.android.installreferrer.api.InstallReferrerClient { *; }
-keep class com.android.installreferrer.api.InstallReferrerClient$InstallReferrerResponse { *; }
-keep class com.android.installreferrer.api.InstallReferrerStateListener { *; }
-keep class com.android.installreferrer.api.ReferrerDetails { *; }

# KochavaTracker: Google App Set ID Collection
-keep class com.google.android.gms.appset.AppSet {
    com.google.android.gms.appset.AppSetIdClient getClient(android.content.Context);
}
-keep class com.google.android.gms.appset.AppSetIdClient {
    com.google.android.gms.tasks.Task getAppSetIdInfo();
}
-keep class com.google.android.gms.appset.AppSetIdInfo {
    java.lang.String getId();
    int getScope();
}
-keep class com.google.android.gms.tasks.Tasks {
    *** await(com.google.android.gms.tasks.Task, long, java.util.concurrent.TimeUnit);
}

# KochavaTracker: Huawei OAID Collection.
-keep class com.huawei.hms.ads.identifier.AdvertisingIdClient {
    com.huawei.hms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.huawei.hms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}

# KochavaTracker: Huawei Install Referrer Collection.
-keep class com.huawei.hms.ads.installreferrer.api.InstallReferrerClient { *; }
-keep class com.huawei.hms.ads.installreferrer.api.InstallReferrerClient$InstallReferrerResponse { *; }
-keep class com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener { *; }
-keep class com.huawei.hms.ads.installreferrer.api.ReferrerDetails { *; }

# KochavaTracker: Samsung Install Referrer Collection.
-keep class com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient { *; }
-keep class com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient$InstallReferrerResponse { *; }
-keep class com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerStateListener { *; }
-keep class com.samsung.android.sdk.sinstallreferrer.api.ReferrerDetails { *; }

# KochavaTracker: Notifications Enabled Collection.
-keep class androidx.core.app.NotificationManagerCompat {
    static androidx.core.app.NotificationManagerCompat from(android.content.Context);
    boolean areNotificationsEnabled();
}

# KochavaTracker: Samsung Cloud Game Dev
-keepclassmembers, includecode class
com.samsung.android.game.cloudgame.dev.sdk.* { *;}

# KochavaTrackerDatapointNetwork: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.tracker.datapointnetwork**

# KochavaTrackerDatapointNetwork: BuildConfig
-keep class com.kochava.tracker.datapointnetwork.BuildConfig { *; }

# KochavaTrackerDatapointNetwork: Internal SDK
-keep class com.kochava.tracker.datapointnetwork.internal.DataPointCollectionNetwork {
    public <methods>;
}

# KochavaTrackerEngagement: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.tracker.engagement**

# KochavaTrackerEngagement: BuildConfig
-keep class com.kochava.tracker.engagement.BuildConfig { *; }

# KochavaTrackerEngagement: Internal SDK
-keep class com.kochava.tracker.engagement.Engagement {
    static com.kochava.tracker.engagement.EngagementApi getInstance();
}

# KochavaTrackerEvents: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.tracker.events**

# KochavaTrackerEvents: BuildConfig
-keep class com.kochava.tracker.events.BuildConfig { *; }

# KochavaTrackerEvents: Internal SDK
-keep class com.kochava.tracker.events.Events {
    static com.kochava.tracker.events.EventsApi getInstance();
}

# KochavaTrackerLegacyReferrer: Ignore SDK warnings for missing dependencies.
-dontwarn com.kochava.tracker.legacyreferrer**

# KochavaTrackerLegacyReferrer: BuildConfig
-keep class com.kochava.tracker.legacyreferrer.BuildConfig { *; }

# KochavaTrackerLegacyReferrer: Referrer Receiver
-keep class com.kochava.tracker.legacyreferrer.LegacyReferrerReceiver {
    void onReceive(android.content.Context, android.content.Intent);
}

# Play Core Proguard Rules: Play In-app Review
-keep class com.google.android.play.core.review.PlayCoreDialogWrapperActivity { *; }
-keep class com.google.android.play.core.common.ReviewManagerFactory { *; }

-keep class com.google.android.play.core.review.ReviewManager {
  public com.google.android.gms.tasks.Task requestReviewFlow();
  public com.google.android.gms.tasks.Task launchReviewFlow(android.app.Activity, com.google.android.play.core.review.ReviewInfo);
}

-keepnames class com.google.android.play.core.review.ReviewInfo

-keep class com.google.android.play.core.review.ReviewManagerFactory {
  <init>();

  public static com.google.android.play.core.review.ReviewManager create(android.content.Context);
}

-keep class com.google.android.play.core.review.testing.FakeReviewManager {
  public <init>(android.content.Context);
  public com.google.android.gms.tasks.Task requestReviewFlow();
  public com.google.android.gms.tasks.Task launchReviewFlow(android.app.Activity, com.google.android.play.core.review.ReviewInfo);
}

-keep class com.google.android.play.core.review.model.ReviewErrorCode {
    public static int NO_ERROR;
    public static int PLAY_STORE_NOT_FOUND;
    public static int INVALID_REQUEST;
    public static int INTERNAL_ERROR;
}

-keep class com.google.android.play.core.review.ReviewException {
    public int getErrorCode();
}

-keep class com.google.android.play.core.** { *; }

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *;}
