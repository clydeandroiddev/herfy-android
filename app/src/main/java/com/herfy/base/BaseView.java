package com.herfy.base;

import java.util.HashMap;

import javax.annotation.Nullable;

public interface BaseView<T> {

    void setPresenter(T presenter);

    void logEvent(String eventName, HashMap<String, String> eventAttributes, @Nullable String eventMetricName, double eventMetricValue);

    void setCartExpiry();

    void setupViews();

    void setupFonts();

    void setupTranslations();

}
