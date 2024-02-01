package com.herfy.history.viewholders;

import static com.herfy.util.extensions.TextViewExtKt.DEFAULT_POSITION_VALUE;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.history.OrdersContract;
import com.herfy.history.adapters.OrderModifierPriceAdapter;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderIngredient;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderModifierItem;
import com.herfy.util.AppLogger;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderMenuItemViewHolder extends RecyclerView.ViewHolder implements OrdersContract.View {

    private Context context;

    private OrdersContract.Presenter ordersPresenter;

    //@BindView(R.id.image) ImageView image;

    //@BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.quantity)
    TextView quantity;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.modifierRecycleView)
    RecyclerView modifierItems;
    @BindView(R.id.ingredients)
    TextView ingredients;
    @BindView(R.id.txtAddOn)
    TextView txtAddOn;
    @BindView(R.id.icModifierItemsDropDown)
    ImageView icModifierItemsDropDown;
    @BindView(R.id.txtAddOnTotal)
    TextView txtAddOnTotal;
    @BindView(R.id.modifierItemName)
    TextView txtModifieritemName;
    @BindView(R.id.modifierItemPrice)
    TextView txtModifieritemPrice;
    @BindView(R.id.modifierLineSeparator)
    View modifierLineSeparator;


    public OrderMenuItemViewHolder(Context context, OrdersContract.Presenter ordersPresenter, View itemView) {


        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.ordersPresenter = ordersPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }


    @Override
    public void setPresenter(OrdersContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    public void setupFonts() {
        /*name.setTypeface(FontHandler.getSemiboldTypeface());
        price.setTypeface(FontHandler.getMediumTypeface());
        modifierItems.setTypeface(FontHandler.getRegularTypeface());
        quantity.setTypeface(FontHandler.getRegularTypeface());*/
    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders) {

    }

    @Override
    public void setOrder(OrderHistory order) {

    }

    @Override
    public void updateOrder(int position) {

    }

    @Override
    public void selectOrder(OrderHistory order) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showOrder(OrderDetails order, Store store, Address address) {

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses, boolean isDone) {

    }

    @Override
    public void showMenuItems(OrderDetails order, ArrayList<MenuItem> menuItems) {

    }

    @Override
    public void setTimeLine(boolean isCancelled, OrderStatus orderStatus, boolean noDriver) {

    }

    @Override
    public void setMenuItem(MenuItem menuItem) {
        final Boolean[] isShouldShowModifierItems = {false};
        String color = "#" + CacheUtil.getInstance().getColorMain().replace("#", "");
        MenuItemAttributes menuItemAttributes = menuItem.getAttributes();
        name.setText(context.getString(R.string.modifier_name_with_quantity_text,menuItemAttributes.getName(),String.valueOf(menuItemAttributes.getQuantity())));

        double tPrice = menuItem.getAttributes().getPrice(null);

        boolean first = true;

        StringBuilder stringBuilder = new StringBuilder();

        double modifierTotalPrice = 0;
        int orderModifierItemSize = menuItem.getOrderModifierItems().size();
        if (orderModifierItemSize > 1) {
            txtAddOn.setText(CacheUtil.getInstance().getTranslations(Text.ADD_ONS_CAPS,context.getString(R.string.add_on)));
            txtAddOn.setVisibility(View.VISIBLE);
            txtAddOnTotal.setVisibility(View.VISIBLE);
            txtAddOnTotal.setText(String.valueOf(orderModifierItemSize));
            icModifierItemsDropDown.setVisibility(View.VISIBLE);
            modifierLineSeparator.setVisibility(View.VISIBLE);
            icModifierItemsDropDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShouldShowModifierItems[0] = !isShouldShowModifierItems[0];
                modifierItems.setVisibility(isShouldShowModifierItems[0] ? View.VISIBLE : View.GONE);
                modifierLineSeparator.setVisibility(isShouldShowModifierItems[0] ? View.VISIBLE : View.GONE);
                icModifierItemsDropDown.setImageResource(isShouldShowModifierItems[0] ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);

            }
        });
            OrderModifierPriceAdapter adapter = new OrderModifierPriceAdapter(context,menuItem.getOrderModifierItems());
            modifierItems.setVisibility(isShouldShowModifierItems[0] ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
            modifierItems.setAdapter(adapter);

            for (OrderModifierItem modifierItem : menuItem.getOrderModifierItems()) {
                if(menuItem.getAttributes().getIsPromotion()) {
                    modifierTotalPrice += (modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity());
                }
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
            }
        } else {
            for (OrderModifierItem modifierItem: menuItem.getOrderModifierItems()) {
                if (!modifierItem.getAttributes().getName().isEmpty()) {
                    txtModifieritemName.setVisibility(View.VISIBLE);
                    txtModifieritemPrice.setVisibility(View.VISIBLE);
                    txtModifieritemName.setText(modifierItem.getAttributes().getName());
                    txtModifieritemPrice.setText(String.valueOf(modifierItem.getAttributes().getPrice()));

                }
            }
        }

        first = true;
        stringBuilder = new StringBuilder();
        if (menuItem.getOrderIngredients().size() > 0) {
            stringBuilder.append(CacheUtil.getInstance().getTranslations(Text.REMOVE).replace(":", "")).append(" ");
            for (OrderIngredient ingredient : menuItem.getOrderIngredients()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(ingredient.getAttributes().getName());
            }
        }

        if (stringBuilder.length() > 0) {
            ingredients.setText(stringBuilder.toString());
            ingredients.setVisibility(View.VISIBLE);
        } else {
            ingredients.setVisibility(View.GONE);
        }

        if(menuItem.getAttributes().getIsPromotion()) {
            AppLogger.error("menuItem.getAttributes().getIsPromotion() modifierTotalPrice " + modifierTotalPrice);
            tPrice += modifierTotalPrice;
        }

        if (tPrice * menuItem.getAttributes().getQuantity() <= 0) {
            price.setText(CacheUtil.getInstance().getTranslations(Text.FREE));
        } else {
            price.setText(StringUtil.getFormattedPrice(tPrice * menuItem.getAttributes().getQuantity()));
        }

    }
    public void setDrawable(
            @DrawableRes Integer left,
            @DrawableRes Integer top,
            @DrawableRes Integer right,
            @DrawableRes Integer bottom
    ) {
       txtAddOn.setCompoundDrawablesWithIntrinsicBounds(
                left != null ? left : DEFAULT_POSITION_VALUE,
                top != null ? top : DEFAULT_POSITION_VALUE,
                right != null ? right : DEFAULT_POSITION_VALUE,
                bottom != null ? bottom : DEFAULT_POSITION_VALUE
        );
    }

    @Override
    public void showCart() {

    }

    @Override
    public void updateStatus(OrderDetails orderDetails, OrderStatus orderStatus) {

    }

    @Override
    public void showDriver(LatLng customerLatLng, LatLng driverLatLng) {

    }

    @Override
    public void updateDriver(LatLng latLng, String mobileNumber) {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void customerArrivedSuccess() {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public String getFromCache(String key) {
        return "";
    }

    @Override
    public void showWhatsApp(String mobileNumber) {

    }

}