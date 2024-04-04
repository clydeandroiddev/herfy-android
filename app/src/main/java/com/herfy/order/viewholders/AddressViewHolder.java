package com.herfy.order.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.order.OrderContract;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddressViewHolder extends RecyclerView.ViewHolder implements OrderContract.View {

    private Context context;

    private OrderContract.Presenter orderPresenter;

    @BindView(R.id.card)
    MaterialCardView card;

    @BindView(R.id.container)
    ConstraintLayout container;

    //@BindView(R.id.icon) ImageView icon;

    @BindView(R.id.label)
    TextView label;
    @BindView(R.id.icon_label)
    ImageView iconLabel;
    @BindView(R.id.icon_selected)
    ImageView iconSelected;
    @BindView(R.id.line)
    TextView line;
    //@BindView(R.id.instructions) TextView instructions;

    //@BindView(R.id.delete) MaterialButton delete;

    @BindView(R.id.bookmark)
    FloatingActionButton bookmark;

    public AddressViewHolder(Context context, OrderContract.Presenter orderPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.orderPresenter = orderPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {

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

        //line.setTypeface(FontHandler.getMediumTypeface());
        //instructions.setTypeface(FontHandler.getRegularTypeface());

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showOrderTypes(Set<OrderType> orderTypes) {

    }

    @Override
    public void addToCartFavorite(MenuItem menuItem) {

    }

    @Override
    public void createOrderType(OrderType orderType) {

    }

    @Override
    public void selectOrderType(OrderType orderType) {

    }

    @Override
    public TabLayout.Tab createTab(String tag, String text, Drawable icon) {
        return null;
    }

    @Override
    public void initializeLocation() {

    }

    @Override
    public void getLocation() {

    }

    @Override
    public void showLocation(boolean isPlace, LatLng latLng, String address) {

    }

    @Override
    public void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses) {

    }

    @Override
    public void setAddress(Address address, boolean selected) {

        /*String label = address.getAttributes().getLabel();

        if(selected) {
            card.setStrokeColor(ViewUtil.getColorMain(context));
            card.setStrokeWidth(ViewUtil.dpToPx(context, 2));

            icon.setImageResource(R.drawable.address_selected);
            icon.setVisibility(View.VISIBLE);

        } else {
            card.setStrokeColor(Color.parseColor("#BEBEBE"));
            card.setStrokeWidth(ViewUtil.dpToPx(context, 1));

            if(label.equalsIgnoreCase("Home") || label.equalsIgnoreCase("الصفحة الرئيسية")) {

                icon.setImageResource(R.drawable.address_home);
                icon.setVisibility(View.VISIBLE);

            } else if(label.equalsIgnoreCase("Work") || label.equalsIgnoreCase("عمل")) {
                icon.setImageResource(R.drawable.address_work);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }

        }*/



        String labelValue = address.getAttributes().getLabel();

        if (labelValue != null && !labelValue.isEmpty() && !labelValue.equalsIgnoreCase("null")) {
            bookmark.setImageResource(R.drawable.bookmark_selected);
            bookmark.setColorFilter(Color.parseColor("#F22424"));
            label.setText(address.getAttributes().getLabel());
            label.setVisibility(View.VISIBLE);
            iconLabel.setVisibility(View.VISIBLE);
            iconLabel.setImageResource(getIconLabel(labelValue));
        } else {
            bookmark.setImageResource(R.drawable.bookmark_unselected);
            bookmark.setColorFilter(Color.parseColor("#BABABA"));
            label.setVisibility(View.GONE);
            iconLabel.setVisibility(View.GONE);
        }
        line.setText(address.getAttributes().getLine1());

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (labelValue != null && !labelValue.isEmpty() && !labelValue.equalsIgnoreCase("null")) {
                    iconLabel.setVisibility(View.INVISIBLE);
                    iconSelected.setVisibility(View.VISIBLE);
                } else {
                    iconSelected.setVisibility(View.GONE);
                }
                orderPresenter.selectAddress(getAdapterPosition(), address);
            }
        });

        bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (labelValue != null && !labelValue.isEmpty() && !labelValue.equalsIgnoreCase("null")) {
                    bookmark.setImageResource(R.drawable.bookmark_unselected);
                    bookmark.setColorFilter(Color.parseColor("#BABABA"));
                    address.getAttributes().setLabel("");
                    orderPresenter.updateAddress(false, address, false);
                } else {
                    orderPresenter.bookmarkAddress(address);
                }
            }
        });

        /*String instructionsValue = address.getAttributes().getInstructions();

        if(instructionsValue != null && !instructionsValue.isEmpty()) {
            instructions.setText(instructionsValue);
            instructions.setVisibility(View.VISIBLE);
        } else {
            instructions.setVisibility(View.GONE);
        }

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((OrderTypeActivity) itemView.getContext()).showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.ARE_YOU_SURE_YOU_WANT_TO_DELETE_ADDRESS), CacheUtil.getInstance().getTranslations(Text.DELETE), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        ((OrderTypeActivity) itemView.getContext()).showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                        orderPresenter.deleteAddress(address);
                    }
                }, CacheUtil.getInstance().getTranslations(Text.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            }
        });*/

    }

    @Override
    public void selectAddressHome(Address address, boolean redownloadMenu) {

    }

    @Override
    public void selectAddressWork(int position) {

    }

    @Override
    public void selectAddressOther(Address address, boolean redownloadMenu) {

    }

    @Override
    public void updateAddAddress(Address updateAddress, android.location.Address saveAddress) {

    }

    @Override
    public void showStores(ArrayList<Store> pickupStores, ArrayList<Store> curbsideStores) {

    }

    @Override
    public void setStore(Store store, boolean selected) {

    }

    @Override
    public void callStore(Store store) {

    }

    @Override
    public void selectStore(int position, Store store, boolean initialLoad) {

    }

    @Override
    public void showStore(boolean markerClicked, int position) {

    }

    @Override
    public void showStore(boolean open, Store store, String error) {

    }

    @Override
    public void showOrderType(OrderType orderType) {

    }

    @Override
    public void selectDelivery() {

    }

    @Override
    public void selectPickup() {

    }

    @Override
    public void selectDineIn() {

    }

    @Override
    public void selectCurbside() {

    }

    @Override
    public void saveAddress(String address) {

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void updateAddress(String address) {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public void showUnavailableMenuItems() {

    }

    @Override
    public void deleteUnavailableItems() {

    }

    @Override
    public void showPhoneNumberError(String error) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void showOrder(OrderDetails order) {

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses) {

    }

    @Override
    public void setTimeLine(OrderStatus orderStatus) {

    }

    @Override
    public void showStatus(OrderStatus orderStatus) {

    }

    @Override
    public void showDriver(LatLng latLng) {

    }

    @Override
    public void updateDriver(LatLng latLng) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showOrders() {

    }

    @Override
    public void showMenuItem(String categoryId, String menuItemId) {

    }

    @Override
    public void enableConfirmButton(boolean enable, String storeName) {

    }

    @Override
    public void saveToCache(String key, String value) {

    }

    @Override
    public void preloadImage(String imageUrl, RequestOptions requestOptions) {

    }

    @Override
    public void diffMenuSaved(Address address) {

    }

    @Override
    public void showPromptChangeOrderType(Address address) {
    }

    @Override
    public void showMinimumDeliveryForAreaMessage(String error) {

    }

    @Override
    public void setCloseTime(String time) {

    }

    private int getIconLabel(String label) {
        if (label.equalsIgnoreCase("Home") || label.equalsIgnoreCase("البيت")) {
            return R.drawable.ic_house;
        } else if (label.equalsIgnoreCase("Work") || label.equalsIgnoreCase("عمل")) {
            return R.drawable.ic_work_address;
        } else if (label.equalsIgnoreCase("Friends House") || label.equalsIgnoreCase("منزل صديقي")) {
            return R.drawable.ic_friend_house;
        } else  {
            return R.drawable.ic_control;
        }
    }

}