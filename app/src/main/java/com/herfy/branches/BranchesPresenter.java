package com.herfy.branches;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.reflect.TypeToken;
import com.herfy.data.constants.Text;
import com.herfy.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Facility;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.CacheUtil;
import com.herfy.util.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BranchesPresenter implements BranchesContract.Presenter, FusedLocationProvider.LocationResultCallback {

    private final BranchesContract.View branchesView;

    private FusedLocationProvider fusedLocationProvider;

    private Location location;

    private ArrayList<Store> branches = new ArrayList<>();

    private Store callBranch = new Store();

    public BranchesPresenter(BranchesContract.View branchesView) {
        this.branchesView = branchesView;
        this.branchesView.setPresenter(this);
    }

    @Override
    public void start() {

        branchesView.setupViews();
        branchesView.setupFonts();
        branchesView.setupTranslations();

    }


    @Override
    public void getLocation(FusedLocationProvider fusedLocationProvider) {
        if(this.fusedLocationProvider != null) {
            this.fusedLocationProvider.disconnect();
        }

        this.fusedLocationProvider = fusedLocationProvider;

        this.fusedLocationProvider.setLocationResultCallback(this);

        this.fusedLocationProvider.connect();

    }

    @Override
    public void getBranches(int page) {
        LocationApiCall.getInstance().getStoreLocationsWithLatLng(String.valueOf(page), CacheUtil.getInstance().getOrderLatitude(), CacheUtil.getInstance().getOrderLongitude(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");

                    Pagination pagination = Pagination.parse(object);
                    if (page == 1) {
                        branches = new ArrayList<>();
                    }
                    for (int i = 0; i < array.length(); i++) {
                        branches.add(Store.parseStore(array.getJSONObject(i)));
                    }

                    if (object.has("included") && !object.isNull("included")) {
                        ArrayList<Facility> facilities = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<Facility>>() {
                        }.getType());
                        for (Facility facility : facilities) {
                            for (Store store : branches) {
                                if (store.getFacilitiesIds().contains(facility.getId())) {
                                    List<Facility> items = new ArrayList<>(store.getFacilities());
                                    items.add(facility);
                                    store.setFacilities(items);
                                    break;
                                }
                            }
                        }
                    }

                    if (page == pagination.getTotalPages()) {
                        if (branches.size() > 0) {
                            getDistances();
                        } else {
                            branchesView.showBranches(branches);
                        }
                    } else {
                        getBranches(page + 1);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    branchesView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                branchesView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void getDistances() {
        if(location != null) {
            for(Store store : branches) {

                Location storeLocation = new Location("Store Location");
                storeLocation.setLatitude(store.getAttributes().getLat());
                storeLocation.setLongitude(store.getAttributes().getLng());

                double distance = location.distanceTo(storeLocation);
                store.getAttributes().setDistance(distance);

            }

        }
        Collections.sort(branches, new Comparator<Store>() {
            @Override
            public int compare(Store store1, Store store2) {
                return (int) Math.round(store1.getAttributes().getDistance()) - (int) Math.round(store2.getAttributes().getDistance());
            }
        });
        branchesView.showBranches(branches);
    }

    @Override
    public void onBindBranchViewAtPosition(int position, BranchViewHolder branchViewHolder, boolean selected) {
        if(position < branches.size()) {
            branchViewHolder.setBranch(branches.get(position), selected);
        } else {
            branchViewHolder.setBranch(null, false);
        }
    }

    @Override
    public int getBranchesCount() {
        return branches.size();
    }

    @Override
    public Store getBranch(int position) {
        return branches.get(position);
    }

    @Override
    public void selectBranch(int position) {
        branchesView.selectBranch(false, position);
    }



    @Override
    public void selectBranch(String id) {
        branchesView.selectBranch(id);
    }

    @Override
    public void callBranch(int position) {
        callBranch = branches.get(position);
        branchesView.callBranch(callBranch);
    }

    @Override
    public void callBranch() {
        if(callBranch != null) {
            branchesView.callBranch(callBranch);
        }
    }

    @Override
    public void getNewFusedLocation(Location location) {
        this.location = location;
        CacheUtil.getInstance().setOrderLatitude(String.valueOf(location.getLatitude()));
        CacheUtil.getInstance().setOrderLongitude(String.valueOf(location.getLongitude()));
        if(branches.size() > 0) {
            getBranches(1);
        }
        fusedLocationProvider.disconnect();
        branchesView.showLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }
}
