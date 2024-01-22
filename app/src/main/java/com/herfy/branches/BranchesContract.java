package com.herfy.branches;

import com.google.android.gms.maps.model.LatLng;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.util.ArrayList;

public interface BranchesContract {

    interface View extends BaseView<Presenter> {

        void initializeLocation();

        void getLocation();

        void showLocation(LatLng latLng);

        void showBranches(ArrayList<Store> branches);

        void setBranch(Store branch, boolean selected);

        void selectBranch(boolean markerClicked, int position);

        void callBranch(Store branch);

        void showError(String error);

        void selectBranch(String id);
    }

    interface Presenter extends BasePresenter {

        void getLocation(FusedLocationProvider fusedLocationProvider);

        void getBranches(int page);

        void getDistances();

        void onBindBranchViewAtPosition(int position, BranchViewHolder branchViewHolder, boolean selected);

        int getBranchesCount();

        Store getBranch(int position);

        void selectBranch(int position);

        void selectBranch(String id);

        void callBranch(int position);

        void callBranch();
    }

}
