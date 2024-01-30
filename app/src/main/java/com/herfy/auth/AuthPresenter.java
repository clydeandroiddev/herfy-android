package com.herfy.auth;

import static com.herfy.data.constants.AuthKt.AUTH_USERNAME_INVALID;
import static com.herfy.data.constants.AuthKt.AUTH_USERNAME_REQUIRED;

import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.LoginOption;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.ForgotPasswordBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.GuestBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginFacebookBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginGoogleBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.ResendSMSBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.DeviceUtil;
import com.herfy.util.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthPresenter implements AuthContract.Presenter {

    private final AuthContract.View authorizationView;

    public AuthPresenter(AuthContract.View authorizationView) {
        this.authorizationView = authorizationView;
        this.authorizationView.setPresenter(this);
    }

    private String registrationMethod = "";

    @Override
    public void start() {
        authorizationView.setupViews();
        authorizationView.setupFonts();
        authorizationView.setupTranslations();
    }

    @Override
    public void signIn(String username, String password, int mobileFormatIndex) {

        boolean valid = true;

        LoginOption loginOption = CacheUtil.getInstance().getConcept().getLoginOption();

        if (loginOption == LoginOption.MOBILE_NUMBER) {
            if (username.isEmpty()) {
                valid = false;
                authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
            } else if (mobileFormatIndex >= 0 && !StringUtil.isValidPhoneNumber(username, mobileFormatIndex)) {
                valid = false;
                authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
            } else {
                authorizationView.showErrorPhoneNumber("");
            }
        } else {
            if (username.isEmpty()) {
                valid = false;
                authorizationView.showErrorUsername(CacheUtil.getInstance().getTranslations(AUTH_USERNAME_REQUIRED));
            } else if (!StringUtil.isValidEmail(username) && !StringUtil.isNumeric(username)) {
                valid = false;
                authorizationView.showErrorUsername(CacheUtil.getInstance().getTranslations(AUTH_USERNAME_INVALID));
            } else if (!StringUtil.isValidEmail(username) && CacheUtil.getInstance().getConcept().getAllowedCountries().size() == 1 && !StringUtil.isValidPhoneNumber(username, 0)) {
                valid = false;
                authorizationView.showErrorUsername(CacheUtil.getInstance().getTranslations(AUTH_USERNAME_INVALID));
            } else {
                authorizationView.showErrorUsername("");
            }
        }

        if (password.isEmpty()) {
            valid = false;
            authorizationView.showErrorPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_REQUIRED));
        } else if (password.length() < 8) {
            valid = false;
            authorizationView.showErrorPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_MINIMUM_CHARACTERS));
        } else {
            authorizationView.showErrorPassword("");
        }

        if (valid) {
            AuthUtil.getInstance().setPassword("");
            LoginBody loginBody = new LoginBody(username, password);
            //For Burgerizzr
            if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                loginBody = new LoginBody(username.replace("+", "").replace(CacheUtil.getInstance().getConcept().getDialingCode(), "0"), password);
            }
            CustomersApiCall.getInstance().loginCustomer(loginBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (o instanceof JSONObject) {

                        try {

                            JSONObject object = (JSONObject) o;

                            JSONObject data = object.getJSONObject("data");
                            JSONObject attributes = data.getJSONObject("attributes");

                            String status = attributes.getString("status").toLowerCase().trim();

                            if (status.equals(Values.STATUS_VERIFIED) || status.equals(Values.STATUS_UNVERIFIED)) {
                                Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                                String token = attributes.getString("token");
                                SoloAuth.getInstance().login(token);
                                if (status.equals(Values.STATUS_VERIFIED)) {
                                    AuthUtil.getInstance().setPassword(password);

                                    HashMap<String, String> eventAttributes = new HashMap<>();
                                    eventAttributes.put("Manual", username);
                                    authorizationView.logEvent("Login", eventAttributes, "", 0);
                                    authorizationView.logSigninEvent(customer.getId());

                                    getCustomer(customer.getId());
                                } else {
                                    if (customer.getAttributes().getMobile() != null && StringUtil.isValidPhoneNumber(customer.getAttributes().getMobile())) {
                                        sendVerification(false, customer.getId(), username, password, customer.getAttributes().getMobile());
                                    } else {
                                        //For Burgerizzr
                                        if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                                            authorizationView.showPhoneNumber(customer.getId(), username, password);
                                        } else {
                                            authorizationView.showPhoneNumber(customer.getId(), username, password);
                                        }
                                        //authorizationView.showCode(false, customer.getId(), email, password, "");
                                    }
                                }
                            } else {
                                authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                        }

                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                        authorizationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR), null));
                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                    }
                }
            });
        }

    }

    @Override
    public void signInFacebook(String email, String token) {

        AuthUtil.getInstance().setPassword("");
        AuthUtil.getInstance().setProvider(Values.PROVIDER_FACEBOOK);
        AuthUtil.getInstance().setToken(token);
        AuthUtil.getInstance().setSecret("");
        LoginFacebookBody loginFacebookBody = new LoginFacebookBody(Values.PROVIDER_FACEBOOK, token);
        CustomersApiCall.getInstance().loginCustomerThroughFacebook(loginFacebookBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        String status = attributes.getString("status").toLowerCase().trim();

                        if (status.equals(Values.STATUS_VERIFIED) || status.equals(Values.STATUS_UNVERIFIED)) {
                            Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                            String token = attributes.getString("token");
                            SoloAuth.getInstance().login(token);
                            if (status.equals(Values.STATUS_VERIFIED)) {
                                HashMap<String, String> eventAttributes = new HashMap<>();
                                eventAttributes.put("Facebook", email);
                                authorizationView.logEvent("Login", eventAttributes, "", 0);

                                getCustomer(customer.getId());
                            } else {
                                if (customer.getAttributes().getMobile() != null && StringUtil.isValidPhoneNumber(customer.getAttributes().getMobile())) {
                                    sendVerification(false, customer.getId(), email, "", customer.getAttributes().getMobile());
                                } else {
                                    authorizationView.showPhoneNumber(customer.getId(), email, "");
                                    //authorizationView.showCode(false, customer.getId(), email, "", "");
                                }
                            }
                        } else {
                            authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                    }

                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);

                if (o instanceof JSONObject) {
                    authorizationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR), null ));
                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                }

            }
        });

    }

    @Override
    public void signInGoogle(String email, String token) {
        AuthUtil.getInstance().setPassword("");
        AuthUtil.getInstance().setProvider(Values.PROVIDER_GOOGLE);
        AuthUtil.getInstance().setToken(token);
        AuthUtil.getInstance().setSecret("");
        LoginGoogleBody loginGoogleBody = new LoginGoogleBody(Values.PROVIDER_GOOGLE, token);
        CustomersApiCall.getInstance().loginCustomerThroughGoogle(loginGoogleBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        String status = attributes.getString("status").toLowerCase().trim();

                        if (status.equals(Values.STATUS_VERIFIED) || status.equals(Values.STATUS_UNVERIFIED)) {
                            Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                            String token = attributes.getString("token");
                            SoloAuth.getInstance().login(token);
                            if (status.equals(Values.STATUS_VERIFIED)) {

                                HashMap<String, String> eventAttributes = new HashMap<>();
                                eventAttributes.put("Google", email);
                                authorizationView.logEvent("Login", eventAttributes, "", 0);
                                authorizationView.logSigninEvent(customer.getId());
                                getCustomer(customer.getId());

                            } else {
                                registrationMethod = "google";
                                if (customer.getAttributes().getMobile() != null && StringUtil.isValidPhoneNumber(customer.getAttributes().getMobile())) {
                                    sendVerification(false, customer.getId(), email, "", customer.getAttributes().getMobile());
                                } else {
                                    authorizationView.showPhoneNumber(customer.getId(), email, "");
                                    //authorizationView.showCode(false, customer.getId(), email, "", "");
                                }
                            }
                        } else {
                            authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                    }

                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    authorizationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR), null ));
                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                }
            }
        });
    }

    @Override
    public void sendVerification(boolean guest, String customerId, String email, String password, String phoneNumber) {

        ResendSMSBody resendSMSBody = new ResendSMSBody(customerId, phoneNumber);
        CustomersApiCall.getInstance().resendSMS(resendSMSBody, new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {

                authorizationView.showCode(guest, customerId, email, password, phoneNumber);
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                authorizationView.showCode(guest, customerId, email, password, phoneNumber);
            }
        });


    }

    @Override
    public void forgotPassword(String phoneNumber, int mobileFormatIndex) {

        boolean valid = true;

        /*if(email.isEmpty()) {
            valid = false;
            authorizationView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_REQUIRED));
        } else if(!StringUtil.isValidEmail(email)) {
            valid = false;
            authorizationView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_INVALID));
        } else {
            authorizationView.showErrorEmail("");
        }*/

        if (phoneNumber.isEmpty()) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber, mobileFormatIndex)) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID, "The phone number you entered is invalid"));
        } else {
            authorizationView.showErrorPhoneNumber("");
        }

        if (valid) {
            ForgotPasswordBody forgotPasswordBody = new ForgotPasswordBody(phoneNumber);
            CustomersApiCall.getInstance().forgotPassword(forgotPasswordBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    authorizationView.showSuccess(CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD_SUCCESS));
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    authorizationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null ));
                }
            });
        }
    }

    @Override
    public void getCustomer(String id) {
        CustomersApiCall.getInstance().getCustomerById(id, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject json = (JSONObject) o;

                        JSONObject dataObj = json.getJSONObject("data");

                        Customer customer = GsonHelper.gsonFinal().fromJson(dataObj.toString(), Customer.class);
                        AuthUtil.getInstance().setCustomer(customer);

                        getConsumerData();
                        getLoyalty();
                        getIntegrations();

                        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && CacheUtil.getInstance().getOrderDeliveryAddress() != null) {

                            CreateCustomerAddressBody createCustomerAddressBody = new CreateCustomerAddressBody("", CacheUtil.getInstance().getOrderDeliveryLabel(), "", "",
                                    "", "", "", CacheUtil.getInstance().getOrderDeliveryAddress(), Double.valueOf(CacheUtil.getInstance().getOrderDeliveryLatitude()), Double.valueOf(CacheUtil.getInstance().getOrderDeliveryLongitude()),
                                    CacheUtil.getInstance().getOrderDeliveryTelephone(), CacheUtil.getInstance().getOrderDeliveryInstructions(), "");

                            CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody, customer.getId(), "1", new ApiRequestListener() {
                                @Override
                                public void onSuccess(Object o) {
                                    CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            try {
                                                JSONObject object = new JSONObject(o.toString());
                                                JSONArray array = object.getJSONArray("data");
                                                ArrayList<Address> addresses = new ArrayList<>();
                                                for (int i = 0; i < array.length(); i++) {
                                                    addresses.add(com.skylinedynamics.solosdk.api.models.objects.Address.parseAddress(array.getJSONObject(i)));
                                                }

                                                if (addresses.size() > 0) {
                                                    Address address = addresses.get(0);
                                                    CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                                                    CacheUtil.getInstance().setOrderAddress(address);
                                                    CacheUtil.getInstance().setOrderDeliveryAddress(null);
                                                }

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

                                @Override
                                public void onError(Object o) {
                                    AppLogger.printRetrofitError(o);
                                }
                            });

                        }

                        String uniqueID = AuthUtil.getInstance().getUniqueId();

                        if (uniqueID.isEmpty()) {
                            uniqueID = UUID.randomUUID().toString();
                            AuthUtil.getInstance().setUniqueId(uniqueID);
                        }

                        UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customer.getAttributes().getMobile(), customer.getId(),
                                new ApiRequestListener() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        DeviceUtil.save(object.toString());
                                        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {
                                            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                                                @Override
                                                public void onSuccess(Object obj) {
                                                    DeviceUtil.save(obj.toString());
                                                }

                                                @Override
                                                public void onError(Object error) {
                                                    AppLogger.printRetrofitError(error);
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onError(Object error) {
                                        AppLogger.printRetrofitError(error);
                                    }
                                });

                        getFavorites(1);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                    }

                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    authorizationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR), null));
                } else {
                    authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR));
                }
            }
        });
    }

    @Override
    public void getConsumerData(){
        CacheUtil.getInstance().setConsumerData(null);
        CustomersApiCall.getInstance().getConsumerData(AuthUtil.getInstance().getCustomer().getId(),"desc",1,  new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                if(obj instanceof JSONObject){
                    try{
                        CacheUtil.getInstance().setConsumerData(obj.toString());
                        MixpanelUtil.Companion.registerSuperProperties();
                        MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_LOGIN);
                    }catch (Exception e){
                        e.printStackTrace();
                        MixpanelUtil.Companion.registerSuperProperties();
                        MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_LOGIN);
                    }


                }
            }
            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                MixpanelUtil.Companion.registerSuperProperties();
                MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_LOGIN);
            }
        });
    }

    @Override
    public void getFavorites(int page) {
        CustomersApiCall.getInstance().getAllCustomerFavoriteItems(AuthUtil.getInstance().getCustomer().getId(), String.valueOf(page), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());

                    Pagination pagination = Pagination.parse(object);
                    if (page == 1) {
                        CacheUtil.getInstance().setFavorites(new HashSet<>());
                    }

                    Set<String> favoriteIds = CacheUtil.getInstance().getFavorites();
                    JSONArray data = object.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject favorite = data.getJSONObject(i);
                        favoriteIds.add(Favorite.parseFavorites(favorite).getAttributes().getItemId());
                    }

                    CacheUtil.getInstance().setFavorites(favoriteIds);

                    if (page == pagination.getTotalPages()) {
                        authorizationView.showMain();
                    } else {
                        getFavorites(page + 1);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                authorizationView.showMain();
            }
        });
    }

    private void getLoyalty() {
        CustomersApiCall.getInstance().getCustomerLoyalty(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    Loyalty loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty.class);
                    AuthUtil.getInstance().setCustomerLoyalty(loyalty);
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });

    }

    private void getIntegrations() {
        if (AuthUtil.getInstance().isSignedIn()) {
            String paymentGateway = CacheUtil.getInstance().getApplication().getAttributes().getPaymentGateway();
            if (paymentGateway.equalsIgnoreCase("checkout") || paymentGateway.equalsIgnoreCase("foodics-online")) {
                IntegrationsApiCall.getInstance().getKeys(paymentGateway, new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object o) {
                        try {
                            JSONObject object = new JSONObject(o.toString());
                            if (object.has("data") && !object.isNull("data")) {
                                Integration integration = GsonHelper.gsonFinal().fromJson(object.getJSONObject("data").toString(), Integration.class);
                                CacheUtil.getInstance().setIntegration(integration);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o.toString());
                    }
                });
            }
        }
    }

    @Override
    public void signUp(String fullName, String phoneNumber, String email, String password, int index) {
        boolean valid = true;

        String[] fullname = fullName.split(" ");

        if (fullname.length == 0 || fullName.isEmpty()) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.FIRST_NAME_EMPTY));
        } else if (!StringUtil.isValidName(fullname[0])) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else if (fullname.length == 1) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.LAST_NAME_EMPTY, "Please complete your full name"));
        } else if (!StringUtil.isValidName(fullname[1])) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else {
            authorizationView.showErrorFirstName("");
        }

        if (phoneNumber.isEmpty()) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber, index)) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
        } else {
            authorizationView.showErrorPhoneNumber("");
        }

        boolean isEmailRequired = CacheUtil.getInstance().getConcept().getLoginOption() == LoginOption.EMAIL;
        if (isEmailRequired) {
            if (email.isEmpty()) {
                valid = false;
                authorizationView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_REQUIRED));
            } else if (!StringUtil.isValidEmail(email)) {
                valid = false;
                authorizationView.showErrorEmail(CacheUtil.getInstance().getTranslations(Text.EMAIL_INVALID));
            } else {
                authorizationView.showErrorEmail("");
            }
        }

        if (password.isEmpty()) {
            valid = false;
            authorizationView.showErrorPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_REQUIRED));
        } else if (password.length() < 8) {
            valid = false;
            authorizationView.showErrorPassword(CacheUtil.getInstance().getTranslations(Text.PASSWORD_MINIMUM));
        } else {
            authorizationView.showErrorPassword("");
        }


        if (valid) {
            String firstName = fullname[0];
            String lastName = fullname[1];
            if (fullname.length > 2) {
                StringBuilder lNameString = new StringBuilder();
                for (int i = 0; i < fullname.length; i++) {
                    if (i != 0) {
                        lNameString.append(fullname[i]);
                        lNameString.append(" ");
                    }
                }
                lastName = lNameString.toString();
            }

            String username = email;
            if (CacheUtil.getInstance().getConcept().getLoginOption() == LoginOption.MOBILE_NUMBER) {
                username = phoneNumber.replace("+", "");
            }
            CreateCustomerBody createCustomerBody = new CreateCustomerBody(username, password, firstName, lastName, phoneNumber.replace("+", ""), email, Values.STATUS_UNVERIFIED, "full", "");
            //For Burgerizzr
            if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                createCustomerBody = new CreateCustomerBody(phoneNumber.replace("+", "").replace(CacheUtil.getInstance().getConcept().getDialingCode(), "0"), password, fullname[0], fullname[1], phoneNumber.replace("+", ""), email, Values.STATUS_UNVERIFIED, "full", "");
            }
            CustomersApiCall.getInstance().createNewCustomer(createCustomerBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (o instanceof JSONObject) {

                        try {

                            JSONObject json = (JSONObject) o;

                            JSONObject dataObj = json.getJSONObject("data");

                            String customerId = dataObj.getString("id");

                            registrationMethod = "app";
                            if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                                authorizationView.showCode(false, customerId, phoneNumber.replace("+", "").replace(CacheUtil.getInstance().getConcept().getDialingCode(), "0"), password, phoneNumber);
                            } else {
                                authorizationView.showCode(false, customerId, email, password, phoneNumber);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_UP_ERROR));
                        }

                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_UP_ERROR));
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                        authorizationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SIGN_UP_ERROR), null));
                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.SIGN_UP_ERROR));
                    }
                }
            });
        }

    }

    @Override
    public void guest(String fullName, String phoneNumber, int mobileFormatIndex) {
        boolean valid = true;

        String[] fullname = fullName.split(" ");

        if (fullname.length == 0 || fullName.isEmpty()) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.FIRST_NAME_EMPTY));
        } else if (!StringUtil.isValidName(fullname[0])) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else if (fullname.length == 1) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.LAST_NAME_EMPTY, "Please complete your full name"));
        } else if (!StringUtil.isValidName(fullname[1])) {
            valid = false;
            authorizationView.showErrorFirstName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else {
            authorizationView.showErrorFirstName("");
        }

        /*if(lastName.isEmpty()) {
            valid = false;
            authorizationView.showErrorLastName(CacheUtil.getInstance().getTranslations(Text.LAST_NAME_EMPTY));
        } else {
            authorizationView.showErrorLastName("");
        }*/

        if (phoneNumber.isEmpty()) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber, mobileFormatIndex)) {
            valid = false;
            authorizationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
        } else {
            authorizationView.showErrorPhoneNumber("");
        }

        if (valid) {
            String firstName = fullname[0];
            String lastName = fullname[1];
            if (fullname.length > 2) {
                StringBuilder lNameString = new StringBuilder();
                for (int i = 0; i < fullname.length; i++) {
                    if (i != 0) {
                        lNameString.append(fullname[i]);
                        lNameString.append(" ");
                    }
                }
                lastName = lNameString.toString();
            }

            GuestBody guestBody = new GuestBody(firstName, lastName, phoneNumber.replace("+", ""));
            CustomersApiCall.getInstance().createGuest(guestBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (o instanceof JSONObject) {

                        try {

                            JSONObject json = (JSONObject) o;

                            JSONObject dataObj = json.getJSONObject("data");

                            String customerId = dataObj.getString("id");

                            registrationMethod = "guest";
                            authorizationView.showCode(true, customerId, "", "", phoneNumber);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                        }

                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                        authorizationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null ));
                    } else {
                        authorizationView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }
                }
            });
        }

    }

    @Override
    public void changeLanguage() {

        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    DeviceUtil.save(obj.toString());
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                }
            });

        } else if (!AuthUtil.getInstance().getFirebase().isEmpty()) {
            String uniqueID = AuthUtil.getInstance().getUniqueId();

            if (uniqueID.isEmpty()) {
                uniqueID = UUID.randomUUID().toString();
                AuthUtil.getInstance().setUniqueId(uniqueID);
            }

            String customerId = null;
            String customerMobileNumber = null;
            if (AuthUtil.getInstance().isSignedIn()) {
                customerId = AuthUtil.getInstance().getCustomer().getId();
                //For Burgerizzr
                if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                    customerMobileNumber = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
                } else {
                    customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
                }
            }

            UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customerMobileNumber, customerId,
                    new ApiRequestListener() {
                        @Override
                        public void onSuccess(Object object) {
                            DeviceUtil.save(object.toString());
                            if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

                                UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        DeviceUtil.save(obj.toString());
                                    }

                                    @Override
                                    public void onError(Object error) {
                                        AppLogger.printRetrofitError(error);
                                    }
                                });

                            }
                        }

                        @Override
                        public void onError(Object error) {
                            AppLogger.printRetrofitError(error);
                        }
                    });

        }

    }

    @Override
    public String getRegistrationMethod() {
        return registrationMethod;
    }
}
