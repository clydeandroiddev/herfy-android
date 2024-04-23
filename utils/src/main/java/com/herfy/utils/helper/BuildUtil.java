package com.herfy.utils.helper;

import android.content.Context;

import com.herfy.model.SupportedCountry;
import com.herfy.utils.BuildConfig;
import com.herfy.utils.R;


public class BuildUtil {
    public static String soloAppKey() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = BuildConfig.SOLO_APP;
        if (country != null) {
            app = country.applicationKey;
        }
        return app;
    }

    public static String soloAppConcept() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = BuildConfig.SOLO_CONCEPT;
        if (country != null) {
            app = country.applicationConcept;
        }
        return app;
    }

    public static String splashColorApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = BuildConfig.SPLASH_COLOR;
        if (country != null) {
            app = country.splashColor;
        }
        return app;
    }

    public static boolean splashLogoApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.SPLASH_LOGO;
        if (country != null) {
            app = country.splashLogo;
        }
        return app;
    }

    public static boolean splashBackgroundApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.SPLASH_BACKGROUND;
        if (country != null) {
            app = country.splashBackground;
        }
        return app;
    }

    public static boolean splashVideoApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.SPLASH_VIDEO;
        if (country != null) {
            app = country.splashVideo;
        }
        return app;
    }

    public static boolean splashOnboardingApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.ONBOARDING_ENABLED;
        if (country != null) {
            app = country.onboardingEnabled;
        }
        return app;
    }

    public static boolean soloLogo() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.SOLO_LOGO;
        if (country != null) {
            app = country.soloLogo;
        }
        return app;
    }

    public static boolean foodicsLogo() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.FOODICS_LOGO;
        if (country != null) {
            app = country.foodicsLogo;
        }
        return app;
    }

    public static String tapTest() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = BuildConfig.TAP_TEST;
        if (country != null) {
            app = country.tapTest;
        }
        return app;
    }

    public static String googleInClient(Context context) {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = context.getString(R.string.google_sign_in_client_id);
        if (country != null) {
            app = country.googleSignInClient;
        }
        return app;
    }

    public static String tapLive() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        String app = BuildConfig.TAP_LIVE;
        if (country != null) {
            app = country.tapLive;
        }
        return app;
    }

    public static int splashImage() {
        return R.drawable.splash;
    }

    public static boolean isEkfc() {
        return BuildConfig.SOLO_APP.equalsIgnoreCase("czsWEKJ0MhMoLRNM")
                || BuildConfig.SOLO_APP.equalsIgnoreCase("ERZolT6uapgMH1cC");
    }

    public static boolean splashGIFApp() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        boolean app = BuildConfig.SPLASH_GIF;
        if (country != null) {
        }
        return app;
    }
}
