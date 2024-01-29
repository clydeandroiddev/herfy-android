package com.herfy.router;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Values;
import com.herfy.main.MainActivity;
import com.herfy.splash.SplashActivity;
import com.herfy.util.DeepLinkUtil;

public class RouterActivity extends BaseActivity {

    private String deeplinkPage = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent().getData())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(@NonNull PendingDynamicLinkData pendingDynamicLinkData) {

                        deeplinkPage = "https://" + getResources().getString(R.string.deeplink_page);

                        if (pendingDynamicLinkData != null) {
                            Uri deepLink = pendingDynamicLinkData.getLink();

                            if (deepLink != null) {
                                String url = deepLink.toString();

                                try {
                                    Bundle utm = pendingDynamicLinkData.getUtmParameters();
                                    Values.sDeepLinkUtm = utm;

                                    if (url.contains(deeplinkPage + "/home")) {
                                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
                                        firebaseAnalytics.logEvent("home", utm);
                                        defaultActivity();
                                    } else if (url.contains(deeplinkPage + "/categories/") || url.contains(deeplinkPage + "/category")) {
                                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
                                        firebaseAnalytics.logEvent("category", utm);
                                        categoriesDeeplink(url);
                                    } else if (url.contains(deeplinkPage + "/item/") || url.contains(deeplinkPage + "/item")) {
                                        itemsDeeplink(url, utm);
                                    } else if (url.contains(deeplinkPage + "/promotions/") || url.contains(deeplinkPage + "/promo")) {
                                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
                                        firebaseAnalytics.logEvent("promo", utm);
                                        promosDeeplink(url);
                                    } else if (url.contains(deeplinkPage + "/shoppingcart/") || url.contains(deeplinkPage + "/shopping-cart")) {
                                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
                                        firebaseAnalytics.logEvent("cart", utm);
                                        cartDeeplink();
                                    } else if (url.contains(deeplinkPage + "/order")) {
                                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
                                        firebaseAnalytics.logEvent("order", utm);
                                        orderDeeplink(url);
                                    } else {
                                        defaultActivity();
                                    }
                                } catch (Exception e) {
                                    defaultActivity();
                                }
                            } else {
                                defaultActivity();
                            }

                        } else {
                            defaultActivity();
                        }

                    }

                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        defaultActivity();
                    }
                });

    }

    /** Opens SplashActivity and ends RouterActivity **/
    private void defaultActivity() {
        Intent splash = new Intent(this, SplashActivity.class);
        splash.putExtra("router", true);
        splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        splash.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(splash);
        finish();
    }

    /** Opens MainActivity and ends RouterActivity **/
    private void mainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * Opens MainActivity and Redirect to Category given a Category ID embedded in the url
     */
    private void categoriesDeeplink(String url) {
        DeepLinkUtil.categories(url);
        mainActivity();
    }

    /**
     * Opens MainActivity and Redirect to Item given a Item ID embedded in the url
     */
    private void itemsDeeplink(String url, Bundle utm) {
        String delimeter = "/";
        String id = url.split(delimeter)[url.split(delimeter).length - 1];
        if(utm != null) {
            utm.putString("product_id", id);
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(RouterActivity.this);
            firebaseAnalytics.logEvent("item", utm);
        }
        DeepLinkUtil.items(url);
        mainActivity();
    }

    /**
     * Opens MainActivity and Redirect to Promo given an Promo ID embedded in the url
     */
    private void promosDeeplink(String url) {
        DeepLinkUtil.promos(url);
        mainActivity();
    }

    /**
     * Opens MainActivity and Redirect to Order given an Order ID embedded in the url
     */
    private void orderDeeplink(String url) {
        DeepLinkUtil.order(url);
        mainActivity();
    }

    /**
     * Opens MainActivity and Redirect to Cart screen
     */
    private void cartDeeplink() {
        DeepLinkUtil.cart();
        mainActivity();
    }

}
