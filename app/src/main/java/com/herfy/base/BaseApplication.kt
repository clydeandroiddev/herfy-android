package com.herfy.base

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.capillary.hydra.android.sdk.FcmConfig
import com.capillary.hydra.android.sdk.HydraAPI
import com.capillary.hydra.android.sdk.HydraConfig
import com.capillary.hydra.android.sdk.NotificationConfig
import com.capillary.hydra.android.sdk.utils.LogType
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.database.AppDatabase
import com.herfy.font.FontHandler
import com.herfy.util.AppLogger
import com.herfy.util.glide.UnsafeOkHttpClient
import com.herfy.util.hmac.HmacHeaderInterceptor
import com.herfy.util.obfuscator.Keychain
import com.herfy.util.obfuscator.reveal
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.ApiHeaders
import com.skylinedynamics.solosdk.api.SoloApi
import com.skylinedynamics.solosdk.api.SoloAuth
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.shipbook.shipbooksdk.ShipBook

@HiltAndroidApp
class BaseApplication : Application(), ImageLoaderFactory {



    override fun onCreate() {
        super.onCreate()

        val notificationConfig = NotificationConfig(
            R.drawable.notification,
            R.color.colorPrimary,
            true
        )

        //Hydra Config
        val hydraConfig = HydraConfig.Builder(
            this,
            accountId = "rnu-282033"
        )
            .notificationConfig(notificationConfig)
            .debugLevel(LogType.VERBOSE)
            .captureViewPortDetails(true)
            .fcmConfig(FcmConfig(true))
            .build()

        HydraAPI.instanceWithConfig(this, hydraConfig)


        AppDatabase.init(this)
        FirebaseApp.initializeApp(this)
        sdkInitialize(this)
        AppEventsLogger.activateApp(this)
        if(BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
        AppDatabase.init(this)
        AuthUtil.init(this)
        CacheUtil.init(this)
        LocaleUtil.init(this)
        FontHandler.init(this)
        KochavaTrackerUtil.init(applicationContext, reveal(Keychain.KOCHAVA_GUID))

        //FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
        SoloApi.init(this, EnvUtil.getEnvironmentUrl(), BuildUtil.soloAppKey())
        SoloAuth.getInstance()
            .setLanguage(LocaleUtil.getInstance().languageSymbol)
        SoloApi.getInstance().isDebug = BuildConfig.BUILD_TYPE == "debug"

        // Create the Collector
        val chuckerCollector = ChuckerCollector(
            this,  // Toggles visibility of the push notification
            true, RetentionManager.Period.ONE_HOUR
        )

        // Create the Interceptor
        val chuckerInterceptor: ChuckerInterceptor = ChuckerInterceptor.Builder(this)
            .collector(chuckerCollector)
            .redactHeaders(
                ApiHeaders.AUTHORIZATION_HEADER,
                ApiHeaders.SOLO_APP_HEADER,
                HmacHeaderInterceptor.HEADER_SIGNATURE
            )
            .alwaysReadResponseBody(true) // Controls Android shortcut creation. Available in SNAPSHOTS versions only at the moment
            .build()
        SoloApi.getInstance()
            .addInterceptor(chuckerInterceptor)

        SoloApi.getInstance().addInterceptor(HmacHeaderInterceptor())

        ShipBook.start(this, reveal(Keychain.SHIP_BOOK_APP_ID), reveal(Keychain.SHIP_BOOK_APP_KEY))
        ShipBook.enableInnerLog(BuildConfig.DEBUG)

        RxJavaPlugins.setErrorHandler { e ->
            if (e is UndeliverableException) {
                // Merely log undeliverable exceptions
                AppLogger.error(e.message)
            } else {
                // Forward all others to current thread's uncaught exception handler
                Thread.currentThread().also { thread ->
                    thread.uncaughtExceptionHandler?.uncaughtException(thread, e)
                }
            }
        }




    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .build()
    }
}