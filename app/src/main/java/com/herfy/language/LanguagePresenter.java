package com.herfy.language;

import com.herfy.util.AppLogger;
import com.herfy.util.DeviceUtil;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;

import java.util.Locale;
import java.util.UUID;

public class LanguagePresenter implements LanguageContract.Presenter {

    private final LanguageContract.View languageView;

    public LanguagePresenter(LanguageContract.View languageView) {
        this.languageView = languageView;
        this.languageView.setPresenter(this);
    }

    @Override
    public void start() {
        languageView.setupViews();
        languageView.setupFonts();
        languageView.setupTranslations();
        getLanguage();
    }

    @Override
    public void getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (language.equalsIgnoreCase("ar") || language.equalsIgnoreCase(LocaleUtil.AR_SA)) {
            languageView.selectArabic();
        } else {
            languageView.selectEnglish();
        }
    }

    @Override
    public void changeLanguage() {
        CacheUtil.getInstance().setLanguage(true);
        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {
            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    DeviceUtil.save(obj.toString());
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                }
            });

        } else if (!AuthUtil.getInstance().getFirebase().isEmpty()) {
            String uniqueID = AuthUtil.getInstance().getUniqueId();

            if (uniqueID.isEmpty()) {
                uniqueID = UUID.randomUUID().toString();
                AuthUtil.getInstance().setUniqueId(uniqueID);
            }

            String customerId = null;
            String customerMobileNumber = null;
            if (AuthUtil.getInstance().isSignedIn()) {
                customerId = AuthUtil.getInstance().getCustomer().getId();
                String key = CacheUtil.getInstance().getConceptKey();
                if (key.equalsIgnoreCase("CQcFAwgLDA0")) {
                    customerMobileNumber = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
                } else {
                    customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
                }
            }

            UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customerMobileNumber, customerId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object object) {
                    DeviceUtil.save(object.toString());
                    if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

                        UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                            @Override
                            public void onSuccess(Object obj) {
                                DeviceUtil.save(obj.toString());
                            }

                            @Override
                            public void onError(Object error) {
                                AppLogger.printRetrofitError(error);
                            }
                        });

                    }
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                }
            });

        }

    }
}
