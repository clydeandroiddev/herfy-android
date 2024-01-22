package com.herfy.constants;

import android.os.Bundle;

import java.util.LinkedHashSet;
import java.util.Set;

public class Values {

    public static final String TRANSLATIONS_URL = "https://cdn.getsolo.io/content/solo-mobile-apps2.json";
    public static final String DEFAULT_IMAGE_URL = "https://cdn.getsolo.io/system/default-image.png";
    public static final String CLOUDINARY_IMAGE_URL = "https://res.cloudinary.com/getsolo-io/image/fetch/fl_lossy,f_auto,c_fill,h_(h),w_(w)/";
    public static final String CDN_URL = "https://cdn.getsolo.io/apps/";
    public static final String CDN_HOST = "https://cdn.getsolo.io";
    public static final String CONTENT_URL = "https://cdn.getsolo.io/content/";
    public static final String CLOUDFLARE_IMAGE_URL = "/cdn-cgi/image/fit=cover,quality=80,format=webp,width=(w),height=(h)";
    public static final String CLOUDFLARE_SLIDER_URL = "/cdn-cgi/image/width=(w),height=(h)";
    public static final int MENU_IMAGE_SIZE = 450;
    public static final int SLIDER_IMAGE_WIDTH = 1000;
    public static final int SLIDER_IMAGE_HEIGHT = 500;
    public static boolean fromAddAddressBool = false;
    public static boolean fromUpsell = false;

    public static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";

    public static final String PROD_ABLY_API_KEY = "c8GwjQ.bc7afQ:8y41_MzTKIM5CpDa";
    public static final String STAGING_ABLY_API_KEY = "dDM5uw.vFfl3A:XcSHMk-84eaZSf9k1fbcA4o9KBEeKSGQNJLyS-Ymh78";

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_FACEBOOK = 4;
    public static final int PROVIDER_TWITTER = 2;

    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_UNVERIFIED = "unverified";

    public static final int PERMISSION_LOCATION = 1;
    public static final int PERMISSION_CALL = 2;
    public static final int PERMISSION_CAMERA = 3;
    public static final int PERMISSION_GALLERY = 4;
    public static final int PERMISSION_PHONE_STATE = 5;

    public static final int REQUEST_SMS_CONSENT = 6;
    public static final int REQUEST_ADDRESS = 7;
    public static final int REQUEST_PURCHASE = 8;

    public static final int CACHE_EXPIRY_MINUTES = 5;
    public static final String TABLE_MENU_CATEGORIES = "menu_categories";
    public static final String TABLE_MENU_ITEMS = "menu_items";
    public static final String TABLE_MODIFIER_GROUPS = "modifier_groups";
    public static final String TABLE_MODIFIER_ITEMS = "modifier_items";
    public static final String TABLE_INGREDIENTS = "ingredients";
    public static final String TABLE_ALLERGENS = "allergens";
    public static final String TABLE_STORES = "stores";
    public static final String TABLE_FACILITIES = "facilities";

    public static final String TABLE_MENU_CATEGORIES_ENTITY = "menu_categories_entity";
    public static final String TABLE_MENU_ITEMS_ENTITY = "menu_items_entity";
    public static final String TABLE_MODIFIER_GROUPS_ENTITY = "modifier_groups_entity";
    public static final String TABLE_MODIFIER_ITEMS_ENTITY = "modifier_items_entity";
    public static final String TABLE_INGREDIENTS_ENTITY = "ingredients_entity";
    public static final String TABLE_ALLERGENS_ENTITY = "allergens_entity";
    public static final String TABLE_STORES_ENTITY = "stores_entity";
    public static final String TABLE_TRANSLATED_ENTITY = "translated_entity";
    public static final Set<String> PAYMENT_BRANDS;

    static {
        PAYMENT_BRANDS = new LinkedHashSet<>();
        PAYMENT_BRANDS.add("VISA");
        PAYMENT_BRANDS.add("MASTER");
    }

    public static final boolean NO_PROMOTIONS = false;
    public static final boolean VALIDATE_QUANTITY = false;
    public static final double DEFAULT_LATITUDE = 24.7193534;
    public static final double DEFAULT_LONGITUDE = 46.4861749;

    public static String sDeepLink = "";
    public static String sDeepLinkId = "";
    public static Bundle sDeepLinkUtm = null;

    public static String IMAGE_TEXT = "image_text";

    public static final String AL_TASAJ_CONCEPT_KEY = "8vhbIIrPWF3";
    public static final String AL_TASAJ_FEEDBACK_PAGE = "https://www.taza.com.sa/en/pages/you-are-heard";

    public static final boolean MUKAFA_ENABLED = true;
    public static final String MUKAFAA_PROVIDER = "alrajhi";
    public static final String LOYALTY_PROVIDER = "solo-loyalty";
    public static final String MIXPANEL_TOKEN = "3a172b606e1bd18abfad3bd6f511bbd5";

}
