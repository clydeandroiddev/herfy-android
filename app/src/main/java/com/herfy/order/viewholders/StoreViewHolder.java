package com.herfy.order.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.order.OrderContract;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Facility;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.CacheUtil;
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
    @BindView(R.id.distance_container)
    MaterialCardView distanceContainer;
    @BindView(R.id.open_closed_container)
    MaterialCardView openClosedContainer;

    @BindView(R.id.container)
    LinearLayout container;

    @BindView(R.id.deliveryHours) TextView deliveryHours;
    @BindView(R.id.pickupHours) TextView pickupHours;

    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.address)
    TextView address;
    @BindView(R.id.distance)
    TextView distance;
    @BindView(R.id.telephone)
    TextView telephone;
    @BindView(R.id.hours)
    TextView hours;
    @BindView(R.id.open_closed)
    TextView openClosed;
    @BindView(R.id.facilities)
    LinearLayout facilities;

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
        int color = Color.parseColor(CacheUtil.getInstance().getColorMain());
        name.setTextColor(color);
        openClosed.setTextColor(color);
        distance.setTextColor(color);
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
            if (selected) {
                card.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
            } else {
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

            /*if (pickupHoursAvailable) {
                String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursPickup.get(day).getOpen(), store.getAttributes().openingHoursPickup.get(day).getClosed());
                pickupHours.setText(CacheUtil.getInstance().getTranslations("pickup", "Pickup") + ": " + hoursText);
            }

            if (deliverHoursAvailable) {
                String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursDeliver.get(day).getOpen(), store.getAttributes().openingHoursDeliver.get(day).getClosed());
                deliveryHours.setText(CacheUtil.getInstance().getTranslations("delivery", "Delivery") + ": " + hoursText);
            }

            if (!pickupHoursAvailable && !deliverHoursAvailable && hoursAvailable) {
                String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursUtc.get(day).getOpen(), store.getAttributes().openingHoursUtc.get(day).getClosed());
                pickupHours.setText(CacheUtil.getInstance().getTranslations("pickup", "Pickup") + ": " + hoursText);
                deliveryHours.setText(CacheUtil.getInstance().getTranslations("delivery", "Delivery") + ": " + hoursText);
            }*/
            boolean isOpen = false;
            if (!pickupHoursAvailable && !deliverHoursAvailable && hoursAvailable) {
                String openingHours = StringUtil.getOpeningHours(store);
                String openingFinal = (!openingHours.isEmpty() ? openingHours : "-");
                hours.setText(openingFinal);
                hours.setVisibility(View.VISIBLE);
                deliveryHours.setVisibility(View.GONE);
                pickupHours.setVisibility(View.GONE);
                if (store.getAttributes().getOpen()) {
                    isOpen = true;
                    openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
                    openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
                    openClosed.setTextColor(Color.parseColor("#10C735"));
                }
            } else {
                hours.setVisibility(View.GONE);
                if (deliverHoursAvailable) {
                    String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursDeliver.get(day).getOpen(), store.getAttributes().openingHoursDeliver.get(day).getClosed());
                    deliveryHours.setText(CacheUtil.getInstance().getTranslations("delivery", "Delivery") + ": " + hoursText);
                    deliveryHours.setVisibility(View.VISIBLE);
                } else {
                    deliveryHours.setVisibility(View.GONE);
                }
                if (pickupHoursAvailable) {
                    String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursPickup.get(day).getOpen(), store.getAttributes().openingHoursPickup.get(day).getClosed());
                    pickupHours.setText(CacheUtil.getInstance().getTranslations("pickup", "Pickup") + ": " + hoursText);
                    pickupHours.setVisibility(View.VISIBLE);
                    if (store.getAttributes().getOpenPickup()) {
                        isOpen = true;
                        openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
                        openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
                        openClosed.setTextColor(Color.parseColor("#10C735"));
                    } else {
                        openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
                        openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
                        openClosed.setTextColor(Color.parseColor("#F22424"));
                    }
                } else {
                    pickupHours.setVisibility(View.GONE);
                }
            }

            if(!isOpen) {
                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
                openClosed.setTextColor(Color.parseColor("#F22424"));
            }

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

            name.setText(store.getAttributes().getName());

            distanceContainer.setCardBackgroundColor(Color.parseColor(String.format("#0D%06X", (0xFFFFFF & ViewUtil.getColorMain(itemView.getContext())))));
            name.setText(store.getAttributes().getName());

            String addressValue = store.getAttributes().getLine1();
            address.setText(!addressValue.isEmpty() ? addressValue : "");

            /*if (store.getAttributes().getOpen()) {
                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0D10C735"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
                openClosed.setTextColor(Color.parseColor("#10C735"));
            } else {
                openClosedContainer.setCardBackgroundColor(Color.parseColor("#0DF22424"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
                openClosed.setTextColor(Color.parseColor("#F22424"));
            }*/

            distanceContainer.setCardBackgroundColor(Color.parseColor(String.format("#0D%06X", (0xFFFFFF & ViewUtil.getColorMain(itemView.getContext())))));

            if(store.getAttributes().getDistance() != -1) {
                String distanceValue = StringUtil.getDistance(store.getAttributes().getDistance());
                if (!distanceValue.isEmpty()) {
                    distance.setText(distanceValue);
                    distance.setVisibility(View.VISIBLE);
                } else {
                    distance.setVisibility(View.GONE);
                }
            } else {
                distance.setVisibility(View.GONE);
            }

            String telephoneValue = store.getAttributes().getTelephone();
            telephone.setText(!telephoneValue.isEmpty() ? telephoneValue : "-");

            telephone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    orderPresenter.callStore(isDineIn, getAdapterPosition());
                }
            });


            if(store.getFacilities().size() > 0) {

                facilities.setVisibility(View.VISIBLE);
                facilities.removeAllViews();

                ArrayList<Facility> distinctFacilities = new ArrayList<>();
                for(Facility facility : store.getFacilities()) {
                    if (!distinctFacilities.contains(facility)) {
                        distinctFacilities.add(facility);
                    }
                }

                for(Facility facility : distinctFacilities) {

                    LayoutInflater inflater = (LayoutInflater) facilities.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View facilityView = inflater.inflate(R.layout.item_facility, null);

                    MaterialCardView facilityContainer = facilityView.findViewById(R.id.facility_container);
                    TextView facilityName = facilityView.findViewById(R.id.name);
                    ImageView facilityIcon = facilityView.findViewById(R.id.icon);
                    
                    String primaryColor = CacheUtil.getInstance().getColorMain().replace("#","#26");

                    facilityContainer.setCardBackgroundColor(Color.parseColor(primaryColor));

                    facilityName.setTextColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                    facilityName.setText(facility.getAttributes().getName());

                    Glide.with(facilities.getContext()).load(facility.getAttributes().getIcon()).apply(RequestOptions.centerCropTransform()).into(facilityIcon);

                    facilities.addView(facilityView);

                }

            } else {
                facilities.setVisibility(View.GONE);
            }

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