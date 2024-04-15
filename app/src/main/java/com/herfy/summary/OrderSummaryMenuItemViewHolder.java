package com.herfy.summary;

import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.herfy.utils.helper.CacheUtil;
import com.moneyhash.sdk.android.ResultType;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.database.AppDatabase;
import com.herfy.payment.PaymentContract;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Pair;

public class OrderSummaryMenuItemViewHolder extends RecyclerView.ViewHolder implements PaymentContract.View {

    private PaymentContract.Presenter paymentPresenter;

    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.quantity)
    TextView quantity;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.modifier_items)
    TextView modifierItems;
    @BindView(R.id.ingredients)
    TextView ingredients;

    public OrderSummaryMenuItemViewHolder(PaymentContract.Presenter paymentPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.paymentPresenter = paymentPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }


    @Override
    public void setupViews() {

    }

    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void setPresenter(PaymentContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void showPaymentTypes(Set<PaymentType> paymentTypes) {

    }

    @Override
    public void showPaymentTypes(ArrayList<PaymentType> paymentTypes) {

    }

    @Override
    public void setStartName(String startName) {

    }

    @Override
    public void setStartAddress(String startAddress) {

    }

    @Override
    public void setEndName(String endName) {

    }

    @Override
    public void setEndAddress(String endAddress) {

    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void updateTime(Calendar calendar) {

    }

    @Override
    public void selectPaymentTypeCash() {

    }

    @Override
    public void showValidateOrderError(List<String> errors) {

    }

    @Override
    public void sendOrder() {

    }

    @Override
    public void initializeTap(String orderId, double total) {

    }

    @Override
    public void showCheckoutPaymentForm(String orderId) {

    }

    @Override
    public void showInputCvvDialog(String orderId) {

    }

    @Override
    public void showCardFailed() {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showCompleted(String orderId, OrderType orderType) {

    }

    @Override
    public void setSchedule() {

    }

    @Override
    public void dismissDialogs() {

    }

    @Override
    public void setTax(boolean inclusive, CharSequence taxAmount) {

    }

    @Override
    public void setPriceDetails(double subtotalPrice, double discount, double totalPrice, double deliveryCharge, double pointsSarValue, int pointsEarnable, int pointsUsed, ArrayList<ServiceCharge> charges) {

    }

    @Override
    public void setMenuItem(MenuItem menuItem) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        String color = "#" + CacheUtil.getInstance().getColorMain().replace("#", "");
        name.setText(Html.fromHtml("<font color=#1E1E1E>" + (menuItem.getName(LocaleUtil.getInstance().getLanguage())) + "</font> <font color=" + color + ">x" + StringUtil.localize(String.valueOf(menuItem.getAttributes().getQuantity())) + "</font>"));

        double itemPrice = paymentPresenter.getMenuItemTotalPrice(menuItem.getAttributes().isUpsell(), menuItem, -1);
        if (!menuItem.isPromo()) {
            price.setText(StringUtil.getFormattedPrice(itemPrice));
        } else {
            if (itemPrice == 0) {
                price.setText(CacheUtil.getInstance().getTranslations(Text.FREE_CAPS, "FREE"));
            } else {
                price.setText(StringUtil.getFormattedPrice(itemPrice));
            }
        }

        boolean first = true;

        StringBuilder stringBuilder = new StringBuilder();

        int comboIndex = 0;
        int trueIndex = 0;
        for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
            if (size.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                trueIndex = comboIndex;
                stringBuilder.append(size.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(size.getAttributes().getQuantity())) + ")");
            }
            comboIndex++;
        }

        for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
            if (drink.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(drink.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(drink.getAttributes().getQuantity())) + ")");
            }
        }

        for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
            if (fries.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(fries.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(fries.getAttributes().getQuantity())) + ")");
            }
        }

        boolean storeSelected = CacheUtil.getInstance().getOrderStore() != null;

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
            if ((!storeSelected || !modifierGroup.getLocationIds().contains(CacheUtil.getInstance().getOrderStore().getId())) && (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden"))) {
                for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                    if ((!storeSelected || !modifierItem.getLocationIds().contains(CacheUtil.getInstance().getOrderStore().getId())) && modifierItem.getAttributes().isSelected()) {
                        if (!first) {
                            stringBuilder.append(", ");
                        } else {
                            first = false;
                        }
                        stringBuilder.append(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())) + ")");
                    }
                }

            }

        }

        for (ModifierItem modifierItem : menuItem.getModifierItems()) {
            if ((!storeSelected || !modifierItem.getLocationIds().contains(CacheUtil.getInstance().getOrderStore().getId())) && modifierItem.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())) + ")");
            }

        }

        try {
            if (menuItem.isCombo) {
                for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                    for (ComponentModifierItem item : components.items) {
                        if (item.selected) {
                            if (!menuItem.isSandwichOnly) {
                                if (!first) {
                                    stringBuilder.append(", ");
                                } else {
                                    first = false;
                                }
                                stringBuilder.append(translated(LocaleUtil.getInstance().getLanguage(), item.name) + "(" + StringUtil.localize(String.valueOf(1)) + ")");
                            }
                            if (!item.modifierGroups.isEmpty()) {
                                for (ModifierGroup modifierGroup : item.modifierGroups) {
                                    for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                        if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                            if (!first) {
                                                stringBuilder.append(", ");
                                            } else {
                                                first = false;
                                            }
                                            stringBuilder.append(modifierGroup.getModifierItems().get(i).getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierGroup.getModifierItems().get(i).getAttributes().getQuantity())) + ")");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (stringBuilder.toString().length() > 0) {
            modifierItems.setText(stringBuilder.toString());
            modifierItems.setVisibility(View.VISIBLE);
        } else {
            modifierItems.setVisibility(View.GONE);
        }

        stringBuilder = new StringBuilder();
        int removedIngredientsCount = 0;
        if (menuItem.getIngredients().size() > 0) {
            stringBuilder.append(CacheUtil.getInstance().getTranslations(Text.REMOVE).replace(":", "") + " ");
            for (Ingredient ingredient : menuItem.getIngredients()) {
                if (!ingredient.getAttributes().isSelected()) {
                    removedIngredientsCount++;
                    stringBuilder.append(ingredient.getAttributes().getName());
                    if (menuItem.getIngredients().indexOf(ingredient) != menuItem.getIngredients().size() - 1) {
                        stringBuilder.append(", ");
                    }
                }
            }
        }

        if (removedIngredientsCount > 0) {
            ingredients.setText(stringBuilder.toString());
            ingredients.setVisibility(View.VISIBLE);
        } else {
            ingredients.setVisibility(View.GONE);
        }
    }

    @Override
    public void logFirebaseEvent(String event, String orderTotalPrice) {
    }

    @Override
    public void showPrayerTime(PrayerTime prayerTime) {

    }

    @Override
    public void showErrorPrayerTime() {

    }

    @Override
    public ActivityResultLauncher<Pair<String, ResultType>> moneyHashContract() {
        return null;
    }

    @Override
    public void setOrderId(String orderId) {

    }

    @Override
    public void showCardDialogList(String orderId) {

    }

    @Override
    public void firebaseECommercePurchaseEvent(List<MenuItem> orderedItem, double total) {

    }

    @Override
    public void firebaseAddPaymentInfoEvent() {

    }

    @Override
    public void firebaseApplyPromotionEvent(String promotionId, String promotionName, List<MenuItem> orderedItems) {

    }

    @Override
    public void updateMenuItems() {

    }

    @Override
    public void showMoyasarSheet(String orderId, double validatedTotal, String menuItems, String apiKey) {

    }

    @Override
    public void showOrderType(Boolean checkout) {

    }

    @Override
    public void showSelectedPayment() {

    }

    public String translated(String language, Translated name) {
        if (language.equalsIgnoreCase("en-us")) {
            return name.getEnglish();
        } else {
            return language.equalsIgnoreCase("fr") ? name.getFrench() : name.getArabic();
        }
    }

}