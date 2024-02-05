package com.herfy.account;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.api.models.objects.RevelReward;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerPasswordBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdatePasswordCustomerBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.util.ErrorHandler;
import com.herfy.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AccountPresenter implements AccountContract.Presenter {

    private final AccountContract.View accountView;

    private ArrayList<Address> locations = new ArrayList<>();
    private ArrayList<Address> bookmarkLocations = new ArrayList<>();
    private ArrayList<Address> otherLocations = new ArrayList<>();

    public AccountPresenter(AccountContract.View accountView) {
        this.accountView = accountView;
        this.accountView.setPresenter(this);
    }

    @Override
    public void start() {
        accountView.setupViews();
        accountView.setupFonts();
        accountView.setupTranslations();
        getCustomer();
        fetchLocations();
    }

    @Override
    public void getCustomer() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        HashMap<String, String> eventAttributes = new HashMap<>();
        eventAttributes.put("User ID", customer.getId());
        accountView.logEvent("MyAccountView", eventAttributes, null, 0);
        accountView.showEmail(!customer.getAttributes().getAccountType().equalsIgnoreCase("guest"));
        String provider = "";
        switch (customer.getAttributes().getProvider()) {
            case Values.PROVIDER_FACEBOOK:
                provider = CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_FACEBOOK, "Sign in with Facebook");
                break;
            case Values.PROVIDER_GOOGLE:
                provider = CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_GOOGLE, "Sign in with Google");
        }
        accountView.showCustomer(customer.getAttributes().getFirstName(), customer.getAttributes().getLastName(), customer.getAttributes().getGender(), customer.getAttributes().getMobile(), customer.getAttributes().getEmail(), provider);
        getSelectedDateOfBirth(customer.getAttributes().getBirthDate());
    }

    @Override
    public List<String> getGenderOptions() {
        List<String> genderOptions = new ArrayList<>();
        genderOptions.add(CacheUtil.getInstance().getTranslations(Text.SELECT_GENDER, "Select gender"));
        genderOptions.add(CacheUtil.getInstance().getTranslations(Text.MALE, "Male"));
        genderOptions.add(CacheUtil.getInstance().getTranslations(Text.FEMALE, "Female"));
        return genderOptions;
    }

    @Override
    public void getSelectedDateOfBirth(String dateOfBirth) {
        if(dateOfBirth != null && !dateOfBirth.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date date = sdf.parse(dateOfBirth);
                if(date != null) {
                    sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                    accountView.showDateOfBirth(date, sdf.format(date));
                    return;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        accountView.showDateOfBirth(null, "");
    }

    @Override
    public int getSelectedGender(String gender, List<String> genderOptions) {
        if(gender.equalsIgnoreCase("male")) {
            return 1;
        } else if(gender.equalsIgnoreCase("female")) {
            return 2;
        }
        return 0;
    }

    @Override
    public void fetchCustomerCards() {
        CustomersApiCall.getInstance().getSavedCards(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    List<CustomerCard> cards = GsonHelper.gsonFinal().fromJson(jsonObject.getJSONArray("data").toString(), new TypeToken<List<CustomerCard>>() {
                    }.getType());
                    CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                    accountView.showCustomerCards();
                } catch (JSONException e) {
                    accountView.showCustomerCards();
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                accountView.showCustomerCards();
            }
        });
    }

    @Override
    public void getPassword() {
        String password = AuthUtil.getInstance().getPassword();
        accountView.showPassword(!password.isEmpty());
    }

    @Override
    public void updatePassword(String oldPassword, String newPassword, String confirmPassword) {
        boolean valid = true;

        if (oldPassword.isEmpty()) {
            valid = false;
            accountView.showErrorConfirmPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_REQUIRED));
        } else {
            accountView.showErrorConfirmPassword("");
        }


        if (newPassword.isEmpty()) {
            valid = false;
            accountView.showErrorNewPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_REQUIRED));
        } else if (newPassword.length() < 8) {
            valid = false;
            accountView.showErrorNewPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_MINIMUM));
        } else {
            accountView.showErrorNewPassword("");
        }

        if (confirmPassword.isEmpty()) {
            valid = false;
            accountView.showErrorConfirmNewPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_REQUIRED));
        } else if (!confirmPassword.equals(newPassword)) {
            valid = false;
            accountView.showErrorConfirmNewPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORDS_DO_NOT_MATCH));
        } else {
            accountView.showErrorConfirmNewPassword("");
        }

        if (valid) {
            Customer customer = AuthUtil.getInstance().getCustomer();
            UpdatePasswordCustomerBody updateCustomerBody = new UpdatePasswordCustomerBody(newPassword, oldPassword);
            CustomersApiCall.getInstance().updatePasswordCustomer(updateCustomerBody, customer.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    if (o instanceof JSONObject) {
                        accountView.showPasswordUpdated();
                    } else {
                        accountView.showError(CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR));
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    accountView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                }
            });
        }

    }

    @Override
    public void save(String firstName, String lastName, String dateOfBirth, String gender, String phoneNumber, String email, String newPassword, String confirmPassword) {

        boolean valid = true;

        if (firstName.isEmpty()) {
            valid = false;
            accountView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.FIRST_NAME_EMPTY));
        } else if (!StringUtil.isValidName(firstName)) {
            valid = false;
            accountView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else {
            accountView.showErrorFirstName("");
        }

        if (lastName.isEmpty()) {
            valid = false;
            accountView.showErrorLastName(CacheUtil.getInstance().getTranslations(Text.LAST_NAME_EMPTY));
        } else if (!StringUtil.isValidName(lastName)) {
            valid = false;
            accountView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else {
            accountView.showErrorLastName("");
        }

        String birthDate = "";
        if(!dateOfBirth.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                Date date = sdf.parse(dateOfBirth);
                if(date != null) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    birthDate = sdf.format(date);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (phoneNumber.isEmpty()) {
            valid = false;
            accountView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber)) {
            valid = false;
            accountView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
        } else {
            accountView.showErrorPhoneNumber("");
        }

        String key = CacheUtil.getInstance().getConceptKey();
        if (key.equalsIgnoreCase("CQcFAwgLDA0")) {
            if (email.isEmpty()) {
                valid = false;
                accountView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_REQUIRED));
            } else if (!StringUtil.isValidEmail(email)) {
                valid = false;
                accountView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_INVALID));
            } else {
                accountView.showErrorEmail("");
            }
        }

        if (valid) {

            Customer customer = AuthUtil.getInstance().getCustomer();

            if (newPassword.isEmpty()) {
                UpdateCustomerBody updateCustomerBody = new UpdateCustomerBody(firstName, lastName, birthDate, gender, email, phoneNumber);

                CustomersApiCall.getInstance().updateCustomer(updateCustomerBody, customer.getId(), new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object o) {
                        if (o instanceof JSONObject) {
                            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_UPDATE_ACCOUNT);
                            updateCustomer();
                        } else {
                            accountView.showError(CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR));
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o);
                        accountView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR), null));
                    }
                });

            } else {

                UpdateCustomerPasswordBody updateCustomerPasswordBody = new UpdateCustomerPasswordBody(firstName, lastName, email, phoneNumber, newPassword);

                CustomersApiCall.getInstance().updateCustomerPassword(updateCustomerPasswordBody, customer.getId(), new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object o) {
                        if (o instanceof JSONObject) {
                            updateCustomer();
                        } else {
                            accountView.showError(CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR));
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o);
                        accountView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR), null));
                    }
                });

            }

        }

    }

    @Override
    public void updateCustomer() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        CustomersApiCall.getInstance().getCustomerById(String.valueOf(customer.getId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject json = (JSONObject) o;

                        JSONObject dataObj = json.getJSONObject("data");

                        Customer customer = GsonHelper.gsonFinal().fromJson(dataObj.toString(), Customer.class);
                        AuthUtil.getInstance().setCustomer(customer);
                        accountView.showAccount();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        accountView.showError(CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR));
                    }

                } else {
                    accountView.showError(CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                accountView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR), null));
            }
        });
    }

    @Override
    public void fetchLocations() {
        CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = (JSONObject) o;
                    JSONArray array = object.getJSONArray("data");
                    locations = new ArrayList<>();
                    bookmarkLocations = new ArrayList<>();
                    otherLocations = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        Address address = com.skylinedynamics.solosdk.api.models.objects.Address.parseAddress(array.getJSONObject(i));
                        String label = address.getAttributes().getLabel();
                        locations.add(address);
                        if (label != null && !label.isEmpty()) {
                            bookmarkLocations.add(address);
                        }
                        else {
                            otherLocations.add(address);
                        }
                    }
                    accountView.showLocations(locations);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void onBindLocationItemViewAtPosition(int position, LocationViewHolder locationViewHolder) {
        locationViewHolder.setLocation(bookmarkLocations.get(position));
    }

    @Override
    public int getLocationsCount() {
        return locations.size();
    }

    @Override
    public int getBookmarkLocationsCount() {
        return bookmarkLocations.size();
    }

    @Override
    public void deleteLocation(Address address) {

        boolean bookmark = false;
//        boolean work = false;
        int position = -1;

        com.skylinedynamics.solosdk.api.models.objects.Address deleteLocation = null;

        if (deleteLocation == null) {
            for (com.skylinedynamics.solosdk.api.models.objects.Address bookmarkLocation : bookmarkLocations) {
                if (bookmarkLocation.getId().equalsIgnoreCase(address.getId())) {
                    bookmark = true;
                    deleteLocation = bookmarkLocation;
                    position = bookmarkLocations.indexOf(bookmarkLocation);
                    break;
                }
            }
        }

        if (deleteLocation == null) {
            for (com.skylinedynamics.solosdk.api.models.objects.Address otherLocation : otherLocations) {
                if (otherLocation.getId().equalsIgnoreCase(address.getId())) {
                    bookmark = false;
                    deleteLocation = otherLocation;
                    position = otherLocations.indexOf(otherLocation);
                    break;
                }
            }
        }

        final boolean bookmarkFinal = bookmark;
        final int positionFinal = position;

        if (deleteLocation != null) {
            CustomersApiCall.getInstance().deleteCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), deleteLocation.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    if (bookmarkFinal) {
                        bookmarkLocations.remove(positionFinal);
                    } else {
                        otherLocations.remove(positionFinal);
                    }
                    locations.remove(positionFinal);
                    accountView.showLocations(locations);
                    accountView.showMessage(CacheUtil.getInstance().getTranslations(Text.ADDRESS_DELETED, "Address successfully deleted"));
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    accountView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.DELETING_ADDRESS_ERROR)));
                }
            });
        } else {
            accountView.showMessage(CacheUtil.getInstance().getTranslations(Text.DELETING_ADDRESS_ERROR));
        }
    }

    @Override
    public List<Address> getLocations() {
        if (locations == null || locations.isEmpty())
            return new ArrayList<>();
        else
            return locations;
    }

    @Override
    public String getLocationsAsString() {
        if (locations == null || locations.isEmpty())
            return "";
        else
            return new Gson().toJson(locations);
    }

    @Override
    public void getLocationsFromExtra(String locations) {
        if (locations != null && !locations.isEmpty()) {
            Type type = new TypeToken<ArrayList<Address>>() {
            }.getType();
            ArrayList<Address> addresses = GsonHelper.gsonFinal().fromJson(locations, type);
            accountView.showLocations(addresses);
        } else {
            accountView.hideLocations();
        }
    }

    @Override
    public void deleteAccount() {
        CustomersApiCall.getInstance().delete(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_DELETE_ACCOUNT);
                accountView.accountDeleted();
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                accountView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER, transactionId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onError(Object o) {

                }
            });
        }
    }

    @Override
    public void getRevelRewardPoints() {
        CustomersApiCall.getInstance().getCustomerRevelRewardPoints(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject json = (JSONObject) o;
                    JSONObject dataObj = json.getJSONObject("data");
                    RevelReward revelReward = GsonHelper.gsonFinal().fromJson(dataObj.toString(), RevelReward.class);
                    accountView.showRevelRewardPoints(revelReward.getAttributes().getPointsByPurchase());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

}
