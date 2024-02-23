package com.herfy.curbside;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.base.BaseView;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CurbsideApiCall;
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CurbsidePresenter implements CurbsideContract.Presenter {

    private CurbsideContract.View view;
    private List<CarColor> colors;
    private List<CarMaker> carMakerList;

    public CurbsidePresenter(BaseView baseView) {
        view = (CurbsideContract.View) baseView;
        view.setPresenter(this);
    }

    @Override
    public void getColorsList(boolean showCarColor) {
        CurbsideApiCall.getInstance().getCarColors(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    String items = object.isNull("data") ? "" : object.optString("data");
                    colors = GsonHelper.gsonFinal().fromJson(items, new TypeToken<List<CarColor>>() {
                    }.getType());
                    if (showCarColor) {
                        view.showCarColors(colors);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    view.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void getMakersList(boolean showCarMaker) {
        CurbsideApiCall.getInstance().getCarMakers(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    String items = object.isNull("data") ? "" : object.optString("data");
                    carMakerList = GsonHelper.gsonFinal().fromJson(items, new TypeToken<List<CarMaker>>() {
                    }.getType());
                    CacheUtil.getInstance().setBrands(new ArrayList<>(carMakerList));
                    carMakerList.add(new CarMaker());
                    if (showCarMaker) {
                        view.showCarMakers(carMakerList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    view.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void getCustomerCars() {
        CurbsideApiCall.getInstance().getCustomerCars(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    String items = object.isNull("data") ? "" : object.optString("data");
                    view.showCustomerCars(GsonHelper.gsonFinal().fromJson(items, new TypeToken<List<CustomerCar>>() {
                    }.getType()));
                } catch (JSONException e) {
                    e.printStackTrace();
                    view.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void addNewCar(String carColor, @Nullable Integer carMakerId, @Nullable String carMakerName, String plateNumber, String phoneNumber) {

        boolean valid = true;

        int mobileFormatIndex = 0;
        ArrayList<AllowedCountries> allowedCountries = CacheUtil.getInstance().getConcept().getAllowedCountries();
        for (AllowedCountries allowedCountry : allowedCountries) {
            String dialCode = allowedCountry.getDialCode().replace("+", "");
            if (phoneNumber.startsWith(dialCode)) {
                mobileFormatIndex = allowedCountries.indexOf(allowedCountry);
                break;
            }
        }

        if (phoneNumber.isEmpty()) {
            valid = false;
            view.showError(CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER_REQUIRED, "Mobile number is required"));
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber, mobileFormatIndex)) {
            valid = false;
            view.showError(CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER_INVALID, "Invalid mobile number"));
        }

        if (valid) {

            JsonObject addBody = new JsonObject();
            addBody.addProperty("color", carColor);
            if (carMakerId != null) {
                addBody.addProperty("car-maker-id", carMakerId);
            }
            if (carMakerName != null) {
                addBody.addProperty("car-maker-name", carMakerName);
            }
            if (!plateNumber.isEmpty()) {
                addBody.addProperty("plate-number", plateNumber);
            }
            addBody.addProperty("mobile", phoneNumber);

            CurbsideApiCall.getInstance().postCustomerCar(AuthUtil.getInstance().getCustomer().getId(), addBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        String items = object.isNull("data") ? "" : object.optString("data");
                        CustomerCar customerCar = GsonHelper.gsonFinal().fromJson(items, CustomerCar.class);
                        view.successAdd(customerCar);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        view.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                }
            });

        }
    }

    @Override
    public void editNewCar(String carId, JsonObject object, int position) {
        CurbsideApiCall.getInstance().updateCustomerCar(AuthUtil.getInstance().getCustomer().getId(), carId, object, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    String items = object.isNull("data") ? "" : object.optString("data");
                    CustomerCar customerCar = GsonHelper.gsonFinal().fromJson(items, CustomerCar.class);
                    view.successEdit(customerCar, position);
                } catch (JSONException e) {
                    e.printStackTrace();
                    view.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void deleteCar(String customerCardId, int position) {
        CurbsideApiCall.getInstance().deleteCustomerCar(AuthUtil.getInstance().getCustomer().getId(), customerCardId, null, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                view.successDelete(position);
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                view.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public List<CarColor> getCarColors() {
        return colors;
    }

    @Override
    public List<CarMaker> getCarMakers() {
        return carMakerList;
    }

    @Override
    public void start() {
        view.setupViews();
        view.setupFonts();
        view.setupTranslations();
    }

}
