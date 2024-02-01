package com.herfy.verification;

import android.os.Handler;

import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginTwitterBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.DeviceUtil;
import com.herfy.util.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.LoginOption;
import com.skylinedynamics.solosdk.api.models.requestbodies.CustomerSMSVerificationBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginFacebookBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginGoogleBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.ResendSMSBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerEmailMobileBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class VerificationPresenter implements VerificationContract.Presenter {

    private final VerificationContract.View verificationView;

    public VerificationPresenter(VerificationContract.View verificationView) {
        this.verificationView = verificationView;
        this.verificationView.setPresenter(this);
    }

    @Override
    public void start() {
        verificationView.setupViews();
        verificationView.setupFonts();
        verificationView.setupTranslations();
    }

    @Override
    public void send(String customerId, String phoneNumber) {

        boolean valid = true;

        if(phoneNumber.isEmpty()) {
            valid = false;
            verificationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if(!StringUtil.isValidPhoneNumber(phoneNumber)) {
            valid = false;
            verificationView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
        } else {
            verificationView.showErrorPhoneNumber("");
        }

        if(valid) {

            ResendSMSBody resendSMSBody = new ResendSMSBody(customerId, phoneNumber);
            CustomersApiCall.getInstance().resendSMS(resendSMSBody, new ApiRequestListener() {

                @Override
                public void onSuccess(Object o) {
                    verificationView.showCode();
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                        verificationView.showResendError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SEND_VERIFICATION_CODE_ERROR), null));
                    } else {
                        verificationView.showResendError(CacheUtil.getInstance().getTranslations(Text.SEND_VERIFICATION_CODE_ERROR));
                    }
                }
            });

        }
    }

    @Override
    public void verify(String customerId, String email, String password, String phoneNumber, String code) {

        boolean valid = true;

        if(code.isEmpty()) {
            valid = false;
            verificationView.showErrorCode(CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED));
        }

        if(valid) {

            CustomerSMSVerificationBody customerSMSVerificationBody = new CustomerSMSVerificationBody(code);
            CustomersApiCall.getInstance().verifyCustomer(customerSMSVerificationBody, String.valueOf(customerId), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    if(o instanceof JSONObject) {

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Mobile Number", phoneNumber);
                        verificationView.logEvent("EnterOTP", eventAttributes, null, 0);

                        if(password.isEmpty()) {
                            update(String.valueOf(customerId), email, phoneNumber.replace("+", ""));
                        } else {
                            if(CacheUtil.getInstance().getConcept().getLoginOption() == LoginOption.EMAIL) {
                                new Handler().postDelayed(() ->   signInEmail(email, password), 400);
                            } else {
                                new Handler().postDelayed(() ->   signInEmail(phoneNumber, password), 400);
                            }
                        }
                    } else {
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                       verificationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                    } else {
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }
                }
            });

        }
    }

    @Override
    public void guest(String phoneNumber, String code) {
        AuthUtil.getInstance().setPassword("");
        LoginBody loginBody = new LoginBody(phoneNumber, code);
        CustomersApiCall.getInstance().loginGuest(loginBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);

                        String token = attributes.getString("token");
                        SoloAuth.getInstance().login(token);

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Guest", phoneNumber);
                        verificationView.logEvent("Login", eventAttributes, null, 0);

                        getCustomer(customer.getId());

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o,CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void update(String customerId, String email, String phoneNumber) {
        UpdateCustomerEmailMobileBody updateCustomerEmailMobileBody = new UpdateCustomerEmailMobileBody(email, phoneNumber);
        CustomersApiCall.getInstance().updateCustomerEmailMobile(updateCustomerEmailMobileBody, customerId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                int provider = AuthUtil.getInstance().getProvider();
                if(provider == Values.PROVIDER_GOOGLE) {
                    signInGoogle(email);
                } else if(provider == Values.PROVIDER_FACEBOOK) {
                    signInFacebook(email);
                } else if(provider == Values.PROVIDER_TWITTER) {
                    signInTwitter(email);
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void signInEmail(String email, String password) {
        LoginBody loginBody = new LoginBody(email, password);
        CustomersApiCall.getInstance().loginCustomer(loginBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        String token = attributes.getString("token");
                        SoloAuth.getInstance().login(token);
                        AuthUtil.getInstance().setPassword(password);

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Manual", email);
                        verificationView.logEvent("SignUp", eventAttributes, null, 0);

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                        getCustomer(customer.getId());

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void signInFacebook(String email) {
        LoginFacebookBody loginFacebookBody = new LoginFacebookBody(Values.PROVIDER_FACEBOOK, AuthUtil.getInstance().getToken());
        CustomersApiCall.getInstance().loginCustomerThroughFacebook(loginFacebookBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attribObj = data.getJSONObject("attributes");

                        String token = attribObj.getString("token");
                        SoloAuth.getInstance().login(token);

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Facebook", email);
                        verificationView.logEvent("SignUp", eventAttributes, null, 0);

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                        getCustomer(customer.getId());

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void signInGoogle(String email) {
        LoginGoogleBody loginGoogleBody = new LoginGoogleBody(Values.PROVIDER_GOOGLE, AuthUtil.getInstance().getToken());
        CustomersApiCall.getInstance().loginCustomerThroughGoogle(loginGoogleBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        String token = attributes.getString("token");
                        SoloAuth.getInstance().login(token);

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Google", email);
                        verificationView.logEvent("SignUp", eventAttributes, null, 0);

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                        getCustomer(customer.getId());

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void signInTwitter(String email) {
        LoginTwitterBody loginTwitterBody = new LoginTwitterBody(Values.PROVIDER_TWITTER, AuthUtil.getInstance().getToken(), AuthUtil.getInstance().getSecret());
        CustomersApiCall.getInstance().loginCustomerThroughTwitter(loginTwitterBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        String token = attributes.getString("token");
                        SoloAuth.getInstance().login(token);

                        HashMap<String, String> eventAttributes = new HashMap<>();
                        eventAttributes.put("Twitter", email);
                        verificationView.logEvent("SignUp", eventAttributes, null, 0);

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                        getCustomer(customer.getId());

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }
        });
    }

    @Override
    public void getCustomer(String id) {
        CustomersApiCall.getInstance().getCustomerById(id, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject object = (JSONObject) o;

                        JSONObject data = object.getJSONObject("data");

                        Customer customer = GsonHelper.gsonFinal().fromJson(data.toString(), Customer.class);
                        AuthUtil.getInstance().setCustomer(customer);

                        getConsumerData();
                        getLoyalty();
                        getIntegrations();

                        if(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && CacheUtil.getInstance().getOrderDeliveryAddress() != null) {

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
                                                LinkedList<Address> addresses = new LinkedList<>();
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

                        if(uniqueID.isEmpty()) {
                            uniqueID = UUID.randomUUID().toString();
                            AuthUtil.getInstance().setUniqueId(uniqueID);
                        }

                        UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customer.getAttributes().getMobile(), customer.getId(),
                                new ApiRequestListener() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        DeviceUtil.save(object.toString());
                                        if(AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

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

                    } catch(JSONException e){
                        e.printStackTrace();
                        verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                    }

                } else {
                    verificationView.showError(CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                verificationView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR), null));
            }
        });
    }

    private void getConsumerData() {
        CacheUtil.getInstance().setConsumerData(null);
        CustomersApiCall.getInstance().getConsumerData(AuthUtil.getInstance().getCustomer().getId(), "desc",1, new ApiRequestListener() {
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
        if(AuthUtil.getInstance().isSignedIn()) {
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
    public void getFavorites(int page) {
        CustomersApiCall.getInstance().getAllCustomerFavoriteItems(AuthUtil.getInstance().getCustomer().getId(), String.valueOf(page), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());

                    Pagination pagination = Pagination.parse(object);
                    if(page == 1) {
                        CacheUtil.getInstance().setFavorites(new HashSet<>());
                    }

                    Set<String> favoriteIds = CacheUtil.getInstance().getFavorites();
                    JSONArray data = object.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++ ) {
                        JSONObject favorite = data.getJSONObject(i);
                        favoriteIds.add(Favorite.parseFavorites(favorite).getAttributes().getItemId());
                    }

                    CacheUtil.getInstance().setFavorites(favoriteIds);

                    if(page == pagination.getTotalPages()) {
                        verificationView.showVerified();
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
                verificationView.showVerified();
            }
        });
    }

}
