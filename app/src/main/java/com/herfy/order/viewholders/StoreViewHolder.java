package com.herfy.order.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.order.OrderContract;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Facility;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StoreViewHolder extends RecyclerView.ViewHolder implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private boolean isDineIn = false;

    @BindView(R.id.card)
    MaterialCardView card;

    @BindView(R.id.container)
    ConstraintLayout container;

    @BindView(R.id.indicator)
    ImageView indicator;

    @BindView(R.id.nearestLabel)
    AppCompatTextView nearestLabel;
    @BindView(R.id.directionsImage)
    ImageView directionsImage;

    @BindView(R.id.kmCount)
    AppCompatTextView kmCount;

    @BindView(R.id.branchName)
    AppCompatTextView branchName;

    @BindView(R.id.branchAddress)
    AppCompatTextView branchAddress;

    @BindView(R.id.branchTime)
    AppCompatTextView branchTime;

    @BindView(R.id.pickupMethodsLabel)
    AppCompatTextView pickupMethodsLabel;

    @BindView(R.id.pickupMethodsContainer)
    ConstraintLayout pickupMethodsContainer;

    @BindView(R.id.walkInLabel)
    AppCompatTextView walkInLabel;

    @BindView(R.id.curbsideLabel)
    AppCompatTextView curbsideLabel;

    @BindView(R.id.driveThruLabel)
    AppCompatTextView driveThruLabel;

    public StoreViewHolder(OrderContract.Presenter orderPresenter, boolean isDineIn, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.orderPresenter = orderPresenter;
        this.isDineIn = isDineIn;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {
        orderPresenter = presenter;
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
    }

    @Override
    public void setupTranslations() {
    }

    @Override
    public void showOrderTypes(Set<OrderType> orderTypes) {

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

        if (store != null) {

            nearestLabel.setText(CacheUtil.getInstance().getTranslations(Text.NEAREST_CAPS, "NEAREST"));
            pickupMethodsLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_METHODS, "Pickup Methods"));
            walkInLabel.setText(CacheUtil.getInstance().getTranslations(Text.WALK_IN, "Walk In"));
            curbsideLabel.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_LABEL, "Curbside"));
            driveThruLabel.setText(CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU, "Drive Thru"));

            if (selected) {
                indicator.setImageResource(R.drawable.ic_store_selected);
                directionsImage.setImageResource(R.drawable.ic_direction_selected);
                pickupMethodsContainer.setVisibility(View.VISIBLE);
                card.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
            } else {
                indicator.setImageResource(R.drawable.ic_store_unselected);
                pickupMethodsContainer.setVisibility(View.GONE);
                directionsImage.setImageResource(R.drawable.ic_direction_unselected);
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
            }

            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            if (day < 0) {
                day = 0;
            }

            boolean pickupHoursAvailable = store.getAttributes().openingHoursPickup != null && day < store.getAttributes().openingHoursPickup.size();
            boolean deliverHoursAvailable = store.getAttributes().openingHoursDeliver != null && day < store.getAttributes().openingHoursDeliver.size();
            boolean hoursAvailable = store.getAttributes().openingHoursUtc != null && day < store.getAttributes().openingHoursUtc.size();

            boolean isOpen = false;
            branchTime.setVisibility(View.VISIBLE);
            if (!pickupHoursAvailable && !deliverHoursAvailable && hoursAvailable) {
                String openingHours = StringUtil.getOpeningHours(store);
                String openingFinal = (!openingHours.isEmpty() ? openingHours : "-");
                branchTime.setText(openingFinal);
//                if (store.getAttributes().getOpen()) {
//                    isOpen = true;
//                    openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
//                    openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
//                    openClosed.setTextColor(Color.parseColor("#10C735"));
//                }
            } else {
//                if (deliverHoursAvailable) {
//                    String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursDeliver.get(day).getOpen(), store.getAttributes().openingHoursDeliver.get(day).getClosed());
//                    deliveryHours.setText(CacheUtil.getInstance().getTranslations("delivery", "Delivery") + ": " + hoursText);
//                    deliveryHours.setVisibility(View.VISIBLE);
//                } else {
//                    deliveryHours.setVisibility(View.GONE);
//                }
                if (pickupHoursAvailable) {
                    String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursPickup.get(day).getOpen(), store.getAttributes().openingHoursPickup.get(day).getClosed());
                    branchTime.setText(hoursText);
//                    pickupHours.setVisibility(View.VISIBLE);
//                    if (store.getAttributes().getOpenPickup()) {
//                        isOpen = true;
//                        openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
//                        openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
//                        openClosed.setTextColor(Color.parseColor("#10C735"));
//                    } else {
//                        openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
//                        openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
//                        openClosed.setTextColor(Color.parseColor("#F22424"));
//                    }
                } else {
                    branchTime.setVisibility(View.GONE);
                    //pickupHours.setVisibility(View.GONE);
                }
            }

//            if (!isOpen) {
//                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
//                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
//                openClosed.setTextColor(Color.parseColor("#F22424"));
//            }

            if (selected) {
                card.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
            } else {
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
            }

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    orderPresenter.selectStore(isDineIn, getAdapterPosition());
                }
            });

            branchName.setText(store.getAttributes().getName());

            String addressValue = store.getAttributes().getLine1();
            branchAddress.setText(!addressValue.isEmpty() ? addressValue : "");

            /*if (store.getAttributes().getOpen()) {
                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
                openClosed.setTextColor(Color.parseColor("#10C735"));
            } else {
                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
                openClosed.setTextColor(Color.parseColor("#F22424"));
            }*/

            if (store.getAttributes().getDistance() != -1) {
                String distanceValue = StringUtil.getDistance(store.getAttributes().getDistance());
                if (!distanceValue.isEmpty()) {
                    kmCount.setText(distanceValue);
                    kmCount.setVisibility(View.VISIBLE);
                } else {
                    kmCount.setVisibility(View.GONE);
                }
            } else {
                kmCount.setVisibility(View.GONE);
            }

//            String telephoneValue = store.getAttributes().getTelephone();
//            telephone.setText(!telephoneValue.isEmpty() ? telephoneValue : "-");
//
//            telephone.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    orderPresenter.callStore(isDineIn, getAdapterPosition());
//                }
//            });


//            if (store.getFacilities().size() > 0) {
//
//                facilities.setVisibility(View.VISIBLE);
//                facilities.removeAllViews();
//
//                ArrayList<Facility> distinctFacilities = new ArrayList<>();
//                for (Facility facility : store.getFacilities()) {
//                    if (!distinctFacilities.contains(facility)) {
//                        distinctFacilities.add(facility);
//                    }
//                }
//
//                for (Facility facility : distinctFacilities) {
//
//                    LayoutInflater inflater = (LayoutInflater) facilities.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    View facilityView = inflater.inflate(R.layout.item_facility, null);
//
//                    MaterialCardView facilityContainer = facilityView.findViewById(R.id.facility_container);
//                    TextView facilityName = facilityView.findViewById(R.id.name);
//                    ImageView facilityIcon = facilityView.findViewById(R.id.icon);
//
//                    String primaryColor = CacheUtil.getInstance().getColorMain().replace("#", "#26");
//
//                    facilityContainer.setCardBackgroundColor(Color.parseColor(primaryColor));
//
//                    facilityName.setTextColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
//                    facilityName.setText(facility.getAttributes().getName());
//
//                    Glide.with(facilities.getContext()).load(facility.getAttributes().getIcon()).apply(RequestOptions.centerCropTransform()).into(facilityIcon);
//
//                    facilities.addView(facilityView);
//
//                }
//
//            } else {
//                facilities.setVisibility(View.GONE);
//            }

            /*String openingHours = StringUtil.getOpeningHours(store);
            hours.setText(!openingHours.isEmpty() ? openingHours : "-");*/

            card.setVisibility(View.VISIBLE);

        } else {
            card.setVisibility(View.GONE);
        }

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

}