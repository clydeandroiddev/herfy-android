package com.herfy.country;

import androidx.annotation.Nullable;

import com.bumptech.glide.request.RequestOptions;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.model.SupportedCountry;

public interface CountriesContract {

    interface View extends BaseView<Presenter> {
        void proceedRestart();
        void preloadImage(String imageUrl, RequestOptions requestOptions);
    }

    interface Presenter extends BasePresenter {

        boolean isCacheExpired(@Nullable SupportedCountry country);
        void getApplication();
        void setOldCountry(SupportedCountry country);
    }

}

