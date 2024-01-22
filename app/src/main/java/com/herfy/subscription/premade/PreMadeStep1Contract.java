package com.herfy.subscription.premade;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.herfy.subscription.premade.models.DeliveryTime;
import com.herfy.subscription.premade.models.StubPreMadeModel;

import java.util.List;


public class PreMadeStep1Contract {

    public interface View extends BaseView<Presenter> {

        void showList(List<StubPreMadeModel> stubs);

        void showDeliveryTime(List<DeliveryTime> times);

        void showAddresses(List<Address> addresses);

    }

    public interface Presenter extends BasePresenter {

        void getList(boolean single);

        void getDeliveryTime();

        void getAddresses();

    }
}
