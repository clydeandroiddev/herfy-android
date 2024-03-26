package com.herfy.splash;

import android.content.Intent;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.FirebaseApp;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.models.objects.Slide;

import org.json.JSONObject;

import java.util.ArrayList;

public interface SplashContract {

    interface View extends BaseView<Presenter> {

        void checkIfRooted();

        void recreate();

        void animateLogo(boolean zoomIn, Intent intent);

        void proceed(boolean language, boolean onboarding, boolean forceLocation);

        void showForceUpdate(String message, String url);

        void getSoloDriver();

        void initializeSoloApi(String url);

        void showError(String error);

        void cacheSliders(boolean mid, ArrayList<Slide> slides);

        String getDefaultTranslations();

        void preloadImage(String imageUrl, RequestOptions requestOptions);

        void saveToCache(String key, String value);

    }

    interface Presenter extends BasePresenter {

        void setRecreate(boolean recreate);

        void setAreas();

        void checkTranslations();

        void getTranslations(String url, ApiRequestListener listener);

        void saveTranslations(String translations);

        void getRemoteConfig();

        void setSoloDriver(FirebaseApp soloDriver);

        void sendLanguage();

        void getApplication();

        void getIntegrations();

        void getSliders();

        void getSlider(boolean mid);

        Slide getSlide(int position);

        void getMenuCdn();

        void parseMenu(JSONObject data);

        void proceed();

        void removeMukafaaRedemption();

    }
}
