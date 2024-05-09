package com.herfy.history.viewholders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.analytics.firebase.FirebaseService;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.history.OrdersContract;
import com.herfy.utils.extensions.DoubleExtKt;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.utils.helper.CurrencyUtils;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderViewHolder extends RecyclerView.ViewHolder implements OrdersContract.View {

    private Context context;

    private OrdersContract.Presenter ordersPresenter;

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.icon) ImageView icon;

    @BindView(R.id.status) TextView status;
    @BindView(R.id.id) TextView id;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.price) TextView price;

    @BindView(R.id.price_container) MaterialCardView priceContainer;

    @BindView(R.id.reorder) MaterialButton reorder;
    @BindView(R.id.details) MaterialButton details;
    @BindView(R.id.ivTable) ImageView ivTable;
    @BindView(R.id.table_number) TextView tableNumber;

    @BindView(R.id.spacer) View spacer;

    public OrderViewHolder(Context context, OrdersContract.Presenter ordersPresenter, View itemView) {

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

    @Override
    public void setupFonts() {
        /*status.setTypeface(FontHandler.getSemiboldTypeface());
        datetime.setTypeface(FontHandler.getRegularTypeface());
        id.setTypeface(FontHandler.getRegularTypeface());
        price.setTypeface(FontHandler.getMediumTypeface());*/
    }

    @Override
    public void setupTranslations() {
        reorder.setText(CacheUtil.getInstance().getTranslations(Text.REORDER_THIS_CAPS, "REORDER THIS"));
        details.setText(CacheUtil.getInstance().getTranslations(Text.MORE_DETAILS_CAPS, "MORE DETAILS"));
    }

    @Override
    public void showLocation(boolean isPlace, LatLng latLng, String address) {

    }

    @Override
    public void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders) {

    }

    @Override
    public void setOrder(OrderHistory order) {

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ordersPresenter.selectOrder(order);
            }
        });

        int sequence = order.getSequence();

        if(sequence >= OrderStatusType.INITIATED.getValue() && sequence <= OrderStatusType.DELIVERY_IN_PROGRESS.getValue()) {
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_order_status_pending));
            status.setTextColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        } else if(sequence >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && sequence <= OrderStatusType.FINISHED.getValue()) {
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_order_status_finished));
            status.setTextColor(Color.parseColor("#10C735"));
        } else {
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_order_status_cancelled));
            status.setTextColor(Color.parseColor("#F22424"));
        }

        status.setText(order.getDescription());

        String displayId = order.getDisplayId();

        if(displayId == null || displayId.isEmpty() || displayId.equalsIgnoreCase("null")) {
            displayId = order.getCode();
        }

        if(displayId == null || displayId.isEmpty() || displayId.equalsIgnoreCase("null")) {
            displayId = order.getId();
        }

        id.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_ID_LABEL, "Order ID (x)").replace("(x)", StringUtil.localize(displayId)));

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            Date dateValue = simpleDateFormat.parse(order.getCreatedAt());
            simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            simpleDateFormat.setTimeZone(TimeZone.getDefault());
            String[] dateValues = simpleDateFormat.format(dateValue).split(" ");
            date.setText(dateValues[0]);
            time.setText(dateValues[1] + " " + dateValues[2]);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        priceContainer.setCardBackgroundColor(Color.parseColor(String.format("#0D%06X", (0xFFFFFF & ViewUtil.getColorMain(itemView.getContext())))));

        boolean hasPromotion = DoubleExtKt.hasDecimalPart(order.getDiscountDisplay());
        price.setText(CurrencyUtils.Companion.updateCurrency(order.getTotal(), true, hasPromotion));

		boolean showReorderable = order.isReorderable() && order.getSequence() >= OrderStatusType.FINISHED.getValue();
        reorder.setVisibility(showReorderable ? View.VISIBLE : View.GONE);

        if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
            reorder.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.disabled_tint)));
        } else {
            reorder.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_tint)));
        }

        //hamburgini handle table number
        if(BuildConfig.SOLO_CONCEPT == "1"){
            ivTable.setVisibility(View.GONE);
            tableNumber.setVisibility(View.GONE);
        }else{
            if (!order.getTableLabel().isEmpty()) {
                ivTable.setVisibility(View.VISIBLE);
                tableNumber.setVisibility(View.VISIBLE);
                tableNumber.setText(CacheUtil.getInstance().getTranslations(Text.TABLE, "Table (x)").replace("(x)", order.getTableLabel()));
            } else {
                ivTable.setVisibility(View.GONE);
                tableNumber.setVisibility(View.GONE);
            }
        }

        reorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                    FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), reorder.getContext());
                    return;
                }
                ((BaseActivity) itemView.getContext()).showLoadingDialog();
                MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_REORDER);
                ordersPresenter.getOrder(order.getId(), getAdapterPosition(), false, true);
            }
        });

        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ordersPresenter.selectOrder(order);
            }
        });

        spacer.setVisibility(order.isReorderable() ? View.VISIBLE : View.GONE);

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
    public void updateDriver(DeliveryDriver appDriver) {

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
