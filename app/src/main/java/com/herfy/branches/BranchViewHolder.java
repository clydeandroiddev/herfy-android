package com.herfy.branches;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BranchViewHolder extends RecyclerView.ViewHolder implements BranchesContract.View {

    private BranchesContract.Presenter branchesPresenter;

    private boolean isDineIn;

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

    @BindView(R.id.driveThruContainer)
    LinearLayout driveThruContainer;

    @BindView(R.id.walkInContainer)
    LinearLayout walkInContainer;

    @BindView(R.id.curbsideContainer)
    LinearLayout curbsideContainer;

    @BindView(R.id.walkInLabel)
    AppCompatTextView walkInLabel;

    @BindView(R.id.curbsideLabel)
    AppCompatTextView curbsideLabel;

    @BindView(R.id.driveThruLabel)
    AppCompatTextView driveThruLabel;

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
            nearestLabel.setText(CacheUtil.getInstance().getTranslations(Text.NEAREST_CAPS, "NEAREST"));
            pickupMethodsLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_METHODS, "Pickup Methods"));
            walkInLabel.setText(CacheUtil.getInstance().getTranslations(Text.WALK_IN, "Walk In"));
            curbsideLabel.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE, "Curbside"));
            driveThruLabel.setText(CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU, "Drive Thru"));

            if (getAdapterPosition() == 0) {
                nearestLabel.setVisibility(View.VISIBLE);
            } else {
                nearestLabel.setVisibility(View.GONE);
            }

            if (store.getAttributes().getIsDriveThruEnabled()) {
                driveThruContainer.setVisibility(View.VISIBLE);
            } else {
                driveThruContainer.setVisibility(View.GONE);
            }

            if (store.getAttributes().getPickupEnabled() == 1) {
                walkInContainer.setVisibility(View.VISIBLE);
            } else {
                walkInContainer.setVisibility(View.GONE);
            }

            if (store.getAttributes().getCurbsideEnabled() == 1) {
                curbsideContainer.setVisibility(View.VISIBLE);
            } else {
                curbsideContainer.setVisibility(View.GONE);
            }

            if (selected) {
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), com.herfy.ordertype.R.color.card_bg_selected));
                indicator.setImageResource(R.drawable.ic_store_selected);
                directionsImage.setImageResource(R.drawable.ic_direction_selected);
                pickupMethodsContainer.setVisibility(View.VISIBLE);
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), com.herfy.ordertype.R.color.card_bg_unselected));
                indicator.setImageResource(R.drawable.ic_store_unselected);
                pickupMethodsContainer.setVisibility(View.GONE);
                directionsImage.setImageResource(R.drawable.ic_direction_unselected);
            }

            pickupMethodsContainer.setVisibility(View.GONE);

            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            if (day < 0) {
                day = 0;
            }

            boolean pickupHoursAvailable = store.getAttributes().openingHoursPickup != null && day < store.getAttributes().openingHoursPickup.size();
            boolean deliverHoursAvailable = store.getAttributes().openingHoursDeliver != null && day < store.getAttributes().openingHoursDeliver.size();
            boolean hoursAvailable = store.getAttributes().openingHoursUtc != null && day < store.getAttributes().openingHoursUtc.size();

            branchTime.setVisibility(View.VISIBLE);
            if (!pickupHoursAvailable && !deliverHoursAvailable && hoursAvailable) {
                String openingHours = StringUtil.getOpeningHours(store);
                String openingFinal = (!openingHours.isEmpty() ? openingHours : "-");
                branchTime.setText(openingFinal);
            } else {
                if (pickupHoursAvailable) {
                    String hoursText = StringUtil.formatOpenHours(store.getAttributes().openingHoursPickup.get(day).getOpen(), store.getAttributes().openingHoursPickup.get(day).getClosed());
                    branchTime.setText(hoursText);
                } else {
                    branchTime.setVisibility(View.GONE);
                }
            }

            if (selected) {
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), com.herfy.ordertype.R.color.card_stroke));
            } else {
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
            }

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    branchesPresenter.selectBranch(getAdapterPosition());
                }
            });

            branchName.setText(store.getAttributes().getName());

            String addressValue = store.getAttributes().getLine1();
            branchAddress.setText(!addressValue.isEmpty() ? addressValue : "");

            if (store.getAttributes().getDistance() != -1) {
                String distanceValue = StringUtil.getDistance(store.getAttributes().getDistance());
                if (!distanceValue.isEmpty()) {
                    kmCount.setText(distanceValue.toLowerCase());
                    kmCount.setVisibility(View.VISIBLE);
                } else {
                    kmCount.setVisibility(View.GONE);
                }
            } else {
                kmCount.setVisibility(View.GONE);
            }

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