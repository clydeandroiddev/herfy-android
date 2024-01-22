package com.herfy.subscription.premade;

import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.herfy.subscription.premade.models.DeliveryTime;
import com.herfy.subscription.premade.models.StubPreMadeModel;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreMadeStep1Presenter implements PreMadeStep1Contract.Presenter {

    private PreMadeStep1Contract.View view;

    public PreMadeStep1Presenter(BaseView baseView) {

        view = (PreMadeStep1Contract.View) baseView;
        view.setPresenter(this);

    }

    @Override
    public void getList(boolean single) {
        List<StubPreMadeModel> stubs = new ArrayList<>();
        StubPreMadeModel stub = new StubPreMadeModel();
        if (single) {
            stub.isTogether = true;
            stub.togetherMealPrice = 24;
            stubs.add(stub);
        } else {
            stub.mealTitle = "Breakfast";
            stub.mealPrice = 12;
            stubs.add(stub);
            StubPreMadeModel stub1 = new StubPreMadeModel();
            stub1.mealTitle = "Lunch";
            stub1.mealPrice = 12;
            stubs.add(stub1);
        }
        view.showList(stubs);
    }

    @Override
    public void getDeliveryTime() {
        List<DeliveryTime> stubs = new ArrayList<>();
        DeliveryTime stub1 = new DeliveryTime();
        stub1.time = "9:00 AM - 9:59 AM";
        stub1.isAvailable = true;
        stubs.add(stub1);
        DeliveryTime stub2 = new DeliveryTime();
        stub2.time = "10:00 AM - 10:59 AM";
        stub2.isAvailable = false;
        stubs.add(stub2);
        DeliveryTime stub3 = new DeliveryTime();
        stub3.time = "11:00 AM - 11:59 AM";
        stub3.isAvailable = true;
        stubs.add(stub3);
        DeliveryTime stub4 = new DeliveryTime();
        stub4.time = "12:00 PM - 12:59 PM";
        stub4.isAvailable = true;
        stubs.add(stub4);
        DeliveryTime stub5 = new DeliveryTime();
        stub5.time = "01:00 PM - 01:59 PM";
        stub5.isAvailable = true;
        stubs.add(stub5);
        DeliveryTime stub6 = new DeliveryTime();
        stub6.time = "02:00 PM - 02:59 PM";
        stub6.isAvailable = true;
        stubs.add(stub6);
        DeliveryTime stub7 = new DeliveryTime();
        stub7.time = "03:00 PM - 03:59 PM";
        stub7.isAvailable = true;
        stubs.add(stub7);
        DeliveryTime stub8 = new DeliveryTime();
        stub8.time = "04:00 PM - 04:59 PM";
        stub8.isAvailable = true;
        stubs.add(stub8);
        DeliveryTime stub9 = new DeliveryTime();
        stub9.time = "05:00 PM - 05:59 PM";
        stub9.isAvailable = true;
        stubs.add(stub9);
        view.showDeliveryTime(stubs);
    }

    @Override
    public void getAddresses() {
        List<Address> addresses = new ArrayList<>();
        if (AuthUtil.getInstance().isSignedIn()) {
            CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        for (int i = 0; i < array.length(); i++) {
                            Address address = Address.parseAddress(array.getJSONObject(i));
                            addresses.add(address);
                        }
                        view.showAddresses(addresses);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                        view.showAddresses(addresses);
                    }
                }
                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    view.showAddresses(addresses);
                }
            });
        } else {
            view.showAddresses(addresses);
        }
    }

    @Override
    public void start() {
        view.setupViews();
        view.setupFonts();
        view.setupTranslations();
    }

}
