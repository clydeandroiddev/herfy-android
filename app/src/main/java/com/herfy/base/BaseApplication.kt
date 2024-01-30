package com.herfy.base

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.herfy.BuildConfig
import com.herfy.analytics.MixpanelUtil
import com.herfy.data.AppDatabase
import com.herfy.font.FontHandler
import com.herfy.util.AuthUtil
import com.herfy.util.BuildUtil
import com.herfy.util.CacheUtil
import com.herfy.util.EnvUtil
import com.herfy.util.glide.UnsafeOkHttpClient
import com.herfy.util.obfuscator.Keychain
import com.herfy.util.obfuscator.reveal
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.SoloApi
import com.skylinedynamics.solosdk.api.SoloAuth
import dagger.hilt.android.HiltAndroidApp
import io.shipbook.shipbooksdk.ShipBook

@HiltAndroidApp
class BaseApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

      /*  startKoin {
            androidContext(this@BaseApplication)
            modules(listOf(appModule, viewModelModule))
        }*/



        FirebaseApp.initializeApp(this)
        sdkInitialize(this)
        AppEventsLogger.activateApp(this)
        AppDatabase.init(this)
        AuthUtil.init(this)
        CacheUtil.init(this)
        LocaleUtil.init(this)
        FontHandler.init(this)

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
            .redactHeaders("Authorization")
            .alwaysReadResponseBody(true) // Controls Android shortcut creation. Available in SNAPSHOTS versions only at the moment
            .build()
        SoloApi.getInstance()
            .addInterceptor(chuckerInterceptor)

        ShipBook.start(this, reveal(Keychain.SHIP_BOOK_APP_ID), reveal(Keychain.SHIP_BOOK_APP_KEY))
        ShipBook.enableInnerLog(BuildConfig.DEBUG)

        MixpanelUtil(applicationContext)
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