package com.herfy.account;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Address;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface AccountContract {

    interface View extends BaseView<Presenter> {

        void showCustomer(String firstName, String lastName, String gender, String phoneNumber, String email, String provider);

        void showDateOfBirth(Date birthDate, String date);

        void showEmail(boolean show);

        void showPassword(boolean show);

        void showErrorFirstName(String error);

        void showErrorLastName(String error);

        void showErrorPhoneNumber(String error);

        void showErrorEmail(String error);

        void showErrorNewPassword(String error);

        void showErrorConfirmPassword(String error);

        void showErrorConfirmNewPassword(String error);

        void showPasswordUpdated();

        void showError(String error);

        void showAccount();

        void showLocations(ArrayList<Address> locations);

        void hideLocations();

        void setLocation(Address location);

        void showMessage(String message);

        void accountDeleted();

        void showCustomerCards();

        void showRevelRewardPoints(Double pointValue);

    }

    interface Presenter extends BasePresenter {

        void getCustomer();

        List<String> getGenderOptions();

        void getSelectedDateOfBirth(String dateOfBirth);

        int getSelectedGender(String gender, List<String> genderOptions);

        void getPassword();

        void updatePassword(String oldPassword, String newPassword, String confirmPassword);

        void save(String firstName, String lastName, String dateOfBirth, String gender, String phoneNumber, String email, String newPassword, String confirmPassword);

        void updateCustomer();

        void fetchLocations();

        void onBindLocationItemViewAtPosition(int position, LocationViewHolder locationViewHolder);

        int getLocationsCount();
        int getBookmarkLocationsCount();

        void fetchCustomerCards();

        void deleteLocation(Address location);

        List<Address> getLocations();

        String getLocationsAsString();

        void getLocationsFromExtra(String locations);

        void deleteAccount();

        void removeMukafaaRedemption();

        void getRevelRewardPoints();

    }

}
