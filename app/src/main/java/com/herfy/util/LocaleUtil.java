package com.herfy.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;

import com.herfy.BuildConfig;
import com.skylinedynamics.solosdk.api.SoloAuth;

import java.util.Locale;

public class LocaleUtil {

    private static final String LANG_FILE = "LanguageStorage";
    private static final String LANG = "language";
    private static final String LANG_SETTING = "languageSetting";

    public static final String EN_US = "en-us";
    public static final String AR_SA = "ar-sa";

    public static final String FR = "fr";

    private static LocaleUtil instance;
    private SharedPreferences mPreferences;
    private Resources resources;
    private Resources resourcesApp;

    public static void init(Context context) {
        if (instance == null) {
            instance = new LocaleUtil(context);
        }
    }

    public static synchronized LocaleUtil getInstance() {
        return instance;
    }

    private LocaleUtil(Context context) {
        mPreferences = context.getSharedPreferences(LANG_FILE, Context.MODE_PRIVATE);
        resources = context.getResources();
        resourcesApp = context.getApplicationContext().getResources();
    }

    public void setLocale(Context context) {
        if (context != null) {
            SharedPreferences preferences = mPreferences;

            String language = mPreferences.getString(LANG_SETTING, Locale.getDefault().getLanguage());

            if(!language.equals("en") && !language.equals("ar") && !language.equals("fr")) {
                language = "en";
            }

            if(language.equals("en")){
                SoloAuth.getInstance().setLanguage(LocaleUtil.EN_US);
            }else if(language.equals("fr")){
                SoloAuth.getInstance().setLanguage(LocaleUtil.FR);
            }else {
                SoloAuth.getInstance().setLanguage(LocaleUtil.AR_SA);
            }



            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(LANG, language);
            editor.apply();

            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;

            if (resources != null && resourcesApp != null) {
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                resourcesApp.updateConfiguration(config, resources.getDisplayMetrics());

                Configuration configuration = resources.getConfiguration();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    configuration.setLocale(locale);
                    context.getApplicationContext().createConfigurationContext(configuration);
                    context.createConfigurationContext(configuration);
                }

                try {
                    boolean eng = "en".equals(language) || "fr".equals(language);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        ((Activity) context).getWindow().getDecorView()
                                .setLayoutDirection(eng ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);
                    }
                } catch (NullPointerException | ClassCastException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public void setLanguage(String language) {
        SharedPreferences preferences = mPreferences;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LANG_SETTING, language);
        editor.apply();
    }

    public String getLanguage() {
        SharedPreferences preferences = mPreferences;

        String language = preferences.getString(LANG, mPreferences.getString(LANG_SETTING, Locale.getDefault().getLanguage()));

        if(!language.equals("en") && !language.equals("ar") && !language.equals("fr")) {
            language = "en";
        }

        if(language.equalsIgnoreCase("en")) {
            return EN_US;
        }else if(language.equalsIgnoreCase("fr")){
            return FR;
        } else {
            return AR_SA;
        }
    }

    public boolean isEnglish() {
        SharedPreferences preferences = mPreferences;
        String language = preferences.getString(LANG, mPreferences.getString(LANG_SETTING, Locale.getDefault().getLanguage()));

        if(!language.equals("en") && !language.equals("ar") && !language.equals("fr")) {
            language = "en";
        }

        return language.equalsIgnoreCase("en");
    }

    public boolean isArabic() {
        SharedPreferences preferences = mPreferences;
        String language = preferences.getString(LANG, mPreferences.getString(LANG_SETTING, Locale.getDefault().getLanguage()));
        return language.equalsIgnoreCase("ar");
    }


    public boolean isFrench() {
        SharedPreferences preferences = mPreferences;
        String language = preferences.getString(LANG, mPreferences.getString(LANG_SETTING, Locale.getDefault().getLanguage()));
        return language.equalsIgnoreCase("fr");
    }

    public static Locale getLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Locale.getDefault(Locale.Category.DISPLAY);
        } else {
            return Locale.getDefault();
        }
    }

    public Locale createLocale(){
        if(isFrench()){
            return new Locale("fr", "fr");
        }else if(isArabic()){
            return new Locale("ar", "sa");
        }else {
            return new Locale("en", "us");
        }
    }

    public Locale createDefaultLocale(){
        if (isFrench()) {
            return new Locale("fr");
        }else if(isArabic()) {
            return new Locale("ar");
        }else {
            return new Locale("en");
        }
    }

    public String getLanguageSymbol(){
        if (isFrench()) {
            return FR;
        }else if(isArabic()) {
            return AR_SA;
        }else {
            return EN_US;
        }
    }

    public String getLanguageCode(){
        if (isFrench()) {
            return "fr";
        }else if(isArabic()) {
            return "ar";
        }else {
            return "en";
        }
    }


}
