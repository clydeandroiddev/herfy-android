package com.herfy.branches;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BranchViewHolder extends RecyclerView.ViewHolder implements BranchesContract.View {

    private BranchesContract.Presenter branchesPresenter;

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

    public BranchViewHolder(BranchesContract.Presenter branchesPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.branchesPresenter = branchesPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }


    @Override
    public void setPresenter(BranchesContract.Presenter presenter) {
        branchesPresenter = presenter;
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
    public void initializeLocation() {

    }

    @Override
    public void getLocation() {

    }

    @Override
    public void showLocation(LatLng latLng) {

    }

    @Override
    public void showBranches(ArrayList<Store> stores) {

    }

    @Override
    public void setBranch(Store store, boolean selected) {

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
                    branchesPresenter.selectBranch(getAdapterPosition());
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
                    branchesPresenter.callBranch(getAdapterPosition());
                }
            });

            /*String openingHours = StringUtil.getOpeningHours(store);
            hours.setText(!openingHours.isEmpty() ? openingHours : "-");*/

            card.setVisibility(View.VISIBLE);

        } else {
            card.setVisibility(View.GONE);
        }

    }

    @Override
    public void selectBranch(boolean markerClicked, int position) {

    }


    @Override
    public void callBranch(Store store) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void selectBranch(String id) {

    }


}