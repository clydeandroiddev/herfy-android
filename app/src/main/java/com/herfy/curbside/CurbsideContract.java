package com.herfy.curbside;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;

import java.util.List;

public class CurbsideContract {

    public interface View extends BaseView<Presenter> {

        void showCarColors(List<String> colors);

        void showCarMakers(List<CarMaker> makers);

        void showError(String error);

        void showCustomerCars(List<CustomerCar> cars);

        void successAdd(CustomerCar car);

        void successEdit(CustomerCar car, int position);

        void successDelete(int position);

    }

    public interface Presenter extends BasePresenter {

        void getColorsList(boolean showCarColors);

        void getMakersList(boolean showCarMaker);

        void getCustomerCars();

        void addNewCar(String carColor, @Nullable Integer carMakerId, @Nullable String carMakerName, String plateNumber, String phoneNumber);

        void editNewCar(String carId, JsonObject object, int position);

        void deleteCar(String customerCardId, int position);

        List<String> getCarColors();

        List<CarMaker> getCarMakers();

    }
}

