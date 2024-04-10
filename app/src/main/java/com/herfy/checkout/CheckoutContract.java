package com.herfy.checkout;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;

import java.util.ArrayList;

public interface CheckoutContract {


    interface View extends BaseView<CheckoutPresenter> {
        void showCards();
        void setupTranslations();
        void showCards(ArrayList<CustomerCard> cards);
    }




    interface CheckoutPresenter extends BasePresenter {
        void getCards();

    }
}



