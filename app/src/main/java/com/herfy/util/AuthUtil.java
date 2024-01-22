package com.herfy.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.skylinedynamics.solosdk.util.GsonHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AuthUtil {

    private static final String AUTH_SHARED_PREFERENCES = "Generic_SharedPreferences";

    private static AuthUtil instance;
    private SharedPreferences authSharedPreferences;

    private String CUSTOMER = "customer";
    private String PASSWORD = "password";
    private String FIREBASE = "firebase";
    private String PROVIDER = "provider";
    private String TOKEN = "token";
    private String SECRET = "secret";
    private String UNIQUE_ID= "unique_id";
    private String DEVICE_ID= "device_id";
    private String DEVICE = "device";
    private String CUSTOMER_LOYALTY = "customer_loyalty";
    private String CUSTOMER_LOYALTY_TIER = "customer_loyalty_tier";
    private static Context mContext;
    private AuthUtil(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            authSharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    AUTH_SHARED_PREFERENCES,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
//        authSharedPreferences = context.getSharedPreferences(AUTH_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new AuthUtil(context);
            mContext = context;
        }
    }

    public synchronized static AuthUtil getInstance()
  {
    return instance;
  }

    public synchronized boolean isSignedIn() {
    return getCustomer() != null;
  }

    public synchronized void setCustomer(Customer customer) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();

        if(customer != null) {

            String customerValue = new Gson().toJson(customer);
            editor.putString(CUSTOMER, customerValue);

        } else {

            editor.remove(CUSTOMER);
            editor.remove(PASSWORD);
        }

        editor.apply();
    }

    public synchronized Customer getCustomer() {

        String customerValue = authSharedPreferences.getString(CUSTOMER, null);

        if (customerValue != null) {
            return GsonHelper.gsonFinal().fromJson(customerValue, new TypeToken<Customer>(){}.getType());
        }

        return null;

    }

    public synchronized String getPassword() {
        return authSharedPreferences.getString(PASSWORD, "");
    }

    public synchronized void setPassword(String password) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(PASSWORD, password);
        editor.apply();

    }

    public synchronized String getFirebase() {
        return authSharedPreferences.getString(FIREBASE, "");
    }

    public synchronized void setFirebase(String firebase) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(FIREBASE, firebase);
        editor.apply();

    }

    public synchronized int getProvider() {
        return authSharedPreferences.getInt(PROVIDER, 0);
    }

    public synchronized void setProvider(int provider) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putInt(PROVIDER, provider);
        editor.apply();

    }

    public synchronized String getToken() {
        return authSharedPreferences.getString(TOKEN, "");
    }

    public synchronized void setToken(String token) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(TOKEN, token);
        editor.apply();

    }

    public synchronized String getSecret() {
        return authSharedPreferences.getString(SECRET, "");
    }

    public synchronized void setSecret(String secret) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(SECRET, secret);
        editor.apply();

    }

    public synchronized String getUniqueId() {
        return authSharedPreferences.getString(UNIQUE_ID, "");
    }

    public synchronized void setUniqueId(String uniqueId) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(UNIQUE_ID, uniqueId);
        editor.apply();

    }

    public synchronized String getDeviceId() {
        return authSharedPreferences.getString(DEVICE_ID, "");
    }

    public synchronized void setDeviceId(String deviceId) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(DEVICE_ID, deviceId);
        editor.apply();

    }

    public synchronized String getDevice() {
        return authSharedPreferences.getString(DEVICE, "");
    }

    public synchronized void setDevice(String device) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();
        editor.putString(DEVICE, device);
        editor.apply();

    }

    public synchronized void setCustomerLoyalty(Loyalty loyalty) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();

        if(loyalty != null) {

            String customerValue = new Gson().toJson(loyalty);
            editor.putString(CUSTOMER_LOYALTY, customerValue);

        } else {

            editor.remove(CUSTOMER_LOYALTY);
        }

        editor.apply();
    }

    public synchronized Loyalty getCustomerLoyalty() {

        String customerValue = authSharedPreferences.getString(CUSTOMER_LOYALTY, null);

        if (customerValue != null) {
            return GsonHelper.gsonFinal().fromJson(customerValue, new TypeToken<Loyalty>(){}.getType());
        }

        return null;

    }


    public synchronized void setCustomerLoyaltyTier(Tier tier) {

        SharedPreferences.Editor editor = authSharedPreferences.edit();

        if(tier != null) {

            String customerValue = new Gson().toJson(tier);
            editor.putString(CUSTOMER_LOYALTY_TIER, customerValue);

        } else {

            editor.remove(CUSTOMER_LOYALTY_TIER);
        }

        editor.apply();
    }

    public synchronized Tier getCustomerLoyaltyTier() {

        String customerValue = authSharedPreferences.getString(CUSTOMER_LOYALTY_TIER, null);

        if (customerValue != null) {
            return GsonHelper.gsonFinal().fromJson(customerValue, new TypeToken<Tier>(){}.getType());
        }

        return null;

    }

    public synchronized void migrate() {
        SharedPreferences sp = mContext.getSharedPreferences(AUTH_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String pw = sp.getString(PASSWORD,"");
        String uniqueID = sp.getString(UNIQUE_ID,"");
        String deviceID = sp.getString(DEVICE_ID,"");
        String firebase = sp.getString(FIREBASE, "");
        String device = sp.getString(DEVICE, "");
        int provider = sp.getInt(PROVIDER, 0);
        String token = sp.getString(TOKEN, "");

        String customerLoyalty = sp.getString(CUSTOMER_LOYALTY, null);
        if (customerLoyalty != null) {
            Loyalty customerLoyaltyData = GsonHelper.gsonFinal().fromJson(customerLoyalty, Loyalty.class);
            getInstance().setCustomerLoyalty(customerLoyaltyData);
        }

        String customer = sp.getString(CUSTOMER, null);
        if (customer != null) {
            Customer customerData = GsonHelper.gsonFinal().fromJson(customer, Customer.class);
            getInstance().setCustomer(customerData);
        }

        sp.edit().remove(PASSWORD).commit();
        sp.edit().remove(UNIQUE_ID).commit();
        sp.edit().remove(DEVICE_ID).commit();
        sp.edit().remove(CUSTOMER_LOYALTY).commit();
        sp.edit().remove(CUSTOMER).commit();
        sp.edit().remove(FIREBASE).commit();
        sp.edit().remove(DEVICE).commit();
        sp.edit().remove(PROVIDER).commit();
        sp.edit().remove(TOKEN).commit();

        getInstance().setUniqueId(uniqueID);
        getInstance().setDeviceId(deviceID);
        getInstance().setFirebase(firebase);
        getInstance().setDevice(device);
        getInstance().setProvider(provider);
        getInstance().setPassword(pw);
        getInstance().setToken(token);

    }

}
