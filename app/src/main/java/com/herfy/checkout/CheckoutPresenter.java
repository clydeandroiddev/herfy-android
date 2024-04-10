package com.herfy.checkout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CheckoutPresenter implements CheckoutContract.CheckoutPresenter {

    private final CheckoutContract.View checkoutView;

    public CheckoutPresenter(CheckoutContract.View checkoutView) {
        this.checkoutView = checkoutView;
        this.checkoutView.setPresenter(this);
    }

    @Override
    public void start() {
        checkoutView.setupViews();
        getCards();
    }

    @Override
    public void getCards() {
        CustomersApiCall.getInstance().getSavedCards(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    List<CustomerCard> cards = GsonHelper.gsonFinal().fromJson(jsonObject.getJSONArray("data").toString(), new TypeToken<List<CustomerCard>>() {
                    }.getType());
                    CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
            }
        });

    }
}
