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
import com.capillary.hydra.android.sdk.exceptions.HydraConfigurationException
import com.capillary.hydra.android.sdk.utils.LogType
import com.capillary.hydra.pushbase.HydraPushApi
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
import com.herfy.main.session.GlobalUnauthorizedInterceptor
import com.herfy.util.AppLogger
import com.herfy.util.AppPushMessageListener
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

    @Volatile
    private var hydraInitialized = false



    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        CacheUtil.init(this)
        AppDatabase.init(this)
        sdkInitialize(this)
        AppEventsLogger.activateApp(this)
        if(BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
        AuthUtil.init(this)
        LocaleUtil.init(this)
        FontHandler.init(this)
        KochavaTrackerUtil.init(applicationContext, reveal(Keychain.KOCHAVA_GUID))

        // Best-effort early Hydra init from cached remote-config values.
        // Splash initialization still runs later and keeps behavior unchanged.
        ensureHydraInitializedFromCache()

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

        if (GlobalUnauthorizedInterceptor.markRegisteredIfNeeded()) {
            SoloApi.getInstance().addInterceptor(GlobalUnauthorizedInterceptor)
        }

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

    fun initializeHydraSdk(context: Context) {
        if (hydraInitialized) return

        val notificationConfig = NotificationConfig(
            R.drawable.notification,
            R.color.colorPrimary,
            true
        )

        //Hydra Config
        val accountId: String = if (BuildConfig.BUILD_TYPE.equals("release", ignoreCase = true)) {
            CacheUtil.getInstance().capillaryAccountPlayStoreAppSigning
        }else{
            CacheUtil.getInstance().capillaryAccountId
        }

        val hydraConfig = HydraConfig.Builder(
            context,
            accountId = accountId
        )
            .notificationConfig(notificationConfig)
            .debugLevel(LogType.VERBOSE)
            .captureViewPortDetails(true)
            .sslPublicKey(CacheUtil.getInstance().capillarySslPublicKey)
            .baseURL(CacheUtil.getInstance().capillaryBaseUrl)
            .fcmConfig(FcmConfig(false))
            .build()

        try {
            HydraAPI.instanceWithConfig(context, hydraConfig)
            hydraInitialized = true
        } catch (e: HydraConfigurationException) {
            AppLogger.error("Hydra init failed $e")
            hydraInitialized = false
        }
    }

    fun ensureHydraInitializedFromCache(): Boolean {
        if (hydraInitialized) return true

        val baseUrl = CacheUtil.getInstance().capillaryBaseUrl
        val publicKey = CacheUtil.getInstance().capillarySslPublicKey
        val accountId = if (BuildConfig.BUILD_TYPE.equals("release", ignoreCase = true)) {
            CacheUtil.getInstance().capillaryAccountPlayStoreAppSigning
        } else {
            CacheUtil.getInstance().capillaryAccountId
        }

        if (baseUrl.isEmpty() || publicKey.isEmpty() || accountId.isEmpty()) {
            return false
        }

        initializeHydraSdk(this)
        return hydraInitialized
    }
}