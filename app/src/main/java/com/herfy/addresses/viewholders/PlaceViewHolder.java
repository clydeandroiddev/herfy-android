package com.herfy.addresses.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.addresses.AddressesContract;
import com.herfy.font.FontHandler;
import com.herfy.util.ViewUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaceViewHolder extends RecyclerView.ViewHolder implements AddressesContract.View {

    private Context context;

    private AddressesContract.Presenter addressesPresenter;

    @BindView(R.id.card) MaterialCardView card;

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.name) TextView name;
    @BindView(R.id.address) TextView address;

    public PlaceViewHolder(Context context, AddressesContract.Presenter addressesPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.addressesPresenter = addressesPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(AddressesContract.Presenter presenter) {
        addressesPresenter = presenter;
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

        name.setTypeface(FontHandler.getSemiboldTypeface());
        address.setTypeface(FontHandler.getMediumTypeface());

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
    public void showPlaces(ArrayList<AutocompletePrediction> places) {

    }

    @Override
    public void setPlace(int position, AutocompletePrediction place, boolean selected) {

        if(selected) {
            card.setStrokeWidth(ViewUtil.dpToPx(context, 2));
            card.setStrokeColor(ViewUtil.getColorMain(context));
        } else {
            card.setStrokeWidth(0);
        }

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!selected) {
                    addressesPresenter.selectPlace(position);
                } else {
                    addressesPresenter.selectPlace(-1);
                }
            }
        });

        name.setText(place.getPrimaryText(null).toString());
        address.setText(place.getFullText(null).toString());
    }

    @Override
    public void selectPlace(int position) {

    }

    @Override
    public void showAddresses(ArrayList<Address> homeAddresses, ArrayList<Address> workAddresses, ArrayList<Address> otherAddresses) {

    }

    @Override
    public void setAddress(Address address, boolean selected) {

    }

    @Override
    public void showPlace(Place place) {

    }

    @Override
    public void selectAddressHome(int position) {

    }

    @Override
    public void selectAddressWork(int position) {

    }

    @Override
    public void selectAddressOther(int position) {

    }

    @Override
    public void addAddress(Place place, android.location.Address address) {

    }

    @Override
    public void updateAddress(Place place, Address address) {

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void showOrderType() {

    }

    @Override
    public void showMessage(String message) {

    }

}