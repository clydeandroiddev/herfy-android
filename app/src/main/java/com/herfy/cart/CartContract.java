package com.herfy.cart;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.cart.views.CartMenuItemViewHolder;
import com.herfy.model.CouponResult;
import com.herfy.upsell.UpsellViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.utils.helper.ApiLogLevel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public interface CartContract {

    interface View extends BaseView<Presenter> {

        void showProgressDialog();

        void dismissProgressDialog();

        void showStore(Store store);

        void showAddress(String address);

        void showMenuItems(List<MenuItem> menuItems, boolean fromPromo, boolean fromRemoveCoupon);

        void refreshMenuItems(int position, boolean fromChangeQuantity);

        void updateMenuItems(List<MenuItem> menuItems);

        void setMenuItem(MenuItem menuItem);

        void setTotalPrice(boolean hasPromotion, double subtotalPrice, double discount, double totalPrice, boolean isDelivery, double deliveryCharge, ArrayList<ServiceCharge> charges, double alrajhiAmount);

        void setOriginalPrice(boolean hasPromotion, double originalPrice);

        void setTax(boolean inclusive, CharSequence taxAmount, double vatAmount);

        void deleteCartItem(int position);

        void editCartItem(int position, MenuItem menuItem);

        void showCoupon(String couponCode, String description);

        void removeCoupon();

        void proceed(boolean signIn);

        void showOrderType(boolean checkout);

        void showSignIn();

        void showMessage(String message);

        void showError(String error);

        void showValidateOrderError(List<String> errors);

        void showUnavailableMenuItems();

        void deleteUnavailableItems(int cartQuantity);

        void showUpsell(ArrayList<MenuItem> upsells);

        void showPayment();

        void setSchedule(boolean showAlert, String scheduleTime);

        void refreshDigitalCoupons();

        void showDigitalCoupons(boolean loading, List<Campaign> digitalCoupons, int selected);

        void showAvailableDigitalCoupons(List<Campaign> digitalCoupons, int selected);

        void setDigitalCoupon(Campaign campaign, boolean selected);

        void selectDigitalCoupon(int position);

        void showDigitalCoupon(@Nullable Campaign campaign);

        void addPromotionItem(MenuItem menuItem);
        void showPromotionItem();

        void showPromoError(String error);

        void removeMokafaaView();

        @Nullable String getFromCache(String key);

        void validatingOrder(double subtotal);

        void removeItemEvent(MenuItem menuItem, double totalPrice);

        void firebaseViewCartEvent();

        void firebaseCheckoutEvent();

        void goToItemDetails(int position, MenuItem upsell);

        void showCartMenuItemsLoading();

        void sendAddToCartFacebookEvent(MenuItem menuItem);

    }

    interface Presenter extends BasePresenter {

        void getApplication(boolean showUpsell);

        void scheduleOrder(Calendar originalCalendar, Calendar calendar, boolean checkingOnly);

        void getIntegrations();

        void getConcept();

        List<Campaign> getCampaignList();

        void getMenuItems();

        void fetchCustomerCards();

        void validateOrder(boolean isCouponValidate, boolean checkout, String scheduleTime);

        void getStore();

        void getAddress();

        void onBindMenuItemViewAtPosition(int position, CartMenuItemViewHolder cartMenuItemViewHolder);

        int getMenuItemsCount();

        double getMenuItemTotalPrice(MenuItem item, int modifierGroupPosition);

        double getMenuItemsTotalPrice();

        void getTotalPrice(boolean validate, boolean isCouponValidate);

        void changeQuantity(int position, boolean add);

        void deleteCartItem(boolean delete, int position);

        void editCartItem(int position, MenuItem menuItem);

        void getCoupon(String couponCode);

        void showCoupon();

        void getCouponMenuItem(String menuItemId, int quantity, double price, @Nullable MenuItemAttributes attributes, @Nullable Campaign campaign);

        void removeCoupon();

        void proceed(@Nullable String scheduleTime);

        void getStoreOpen(String scheduleTime);

        void checkUnavailableMenuItems();

        String getUnavailableMenuItemsMessage();

        ArrayList<MenuItem> getUnavailableMenuItems();

        ArrayList<MenuItem> getMenuItemsList();

        void deleteUnavailableItems();

        void getAddresses(boolean fromSave);

        void updateAddress(Address address);

        void addAddress();

        void getUpsells();

        void showDigitalCoupons(boolean show);

        void getDigitalCoupons(boolean show, boolean fromButtonClick);

        List<Campaign> getDigitalCouponsList();

        int getDigitalCouponsSelected();

        int getDigitalCouponsCount();

        void selectDigitalCoupon(int position);

        void validateDigitalCoupon(boolean fromApply, Campaign campaign);

        void availDigitalCoupon(int position);
        void availDigitalCouponCampaign(Campaign campaign);
        void selectPromo(int position);
        String getNotes(int position);
        void addNotes(int position, String notes);
        void removeMukafaaRedemption();
        double getMukafaaTransactionAmount();
        void removeMukafaa();
        void setMukafaaTransactionAmount(double amount);
        void sendLog(ApiLogLevel level, String message);
        void onBindUpsellViewAtPosition(int position, UpsellViewHolder holder);
        int getUpsellCount();
        void goToItemDetails(int position, MenuItem upsell);
        void addToCart(MenuItem upsell);
        void getCouponFrom(String code, Function1<CouponResult, Unit> result);
        void validateDigitalCoupon(String couponCode, Function1<CouponResult, Unit> result);

        void getDigitalCouponsWith(
                ArrayList<MenuItem> menuItems,
                Boolean show ,
                Boolean fromButtonClick
        );
    }

}
