package com.herfy.utils.constants

import android.os.Bundle

object Values {

    const val TRANSLATIONS_URL = "https://cdn.getsolo.io/content/solo-mobile-apps2.json"
    const val DEFAULT_IMAGE_URL = "https://cdn.getsolo.io/system/default-image.png"
    const val CLOUDINARY_IMAGE_URL =
        "https://res.cloudinary.com/getsolo-io/image/fetch/fl_lossy,f_auto,c_fill,h_(h),w_(w)/"
    const val CDN_URL = "https://cdn.getsolo.io/apps/"
    const val CDN_HOST = "https://cdn.getsolo.io"
    const val CONTENT_URL = "https://cdn.getsolo.io/content/"
    const val CLOUDFLARE_IMAGE_URL =
        "/cdn-cgi/image/fit=cover,quality=80,format=webp,width=(w),height=(h)"
    const val CLOUDFLARE_SLIDER_URL = "/cdn-cgi/image/width=(w),height=(h)"
    const val MENU_IMAGE_SIZE = 450
    const val SLIDER_IMAGE_WIDTH = 1000
    const val SLIDER_IMAGE_HEIGHT = 500
    @JvmField
    var fromAddAddressBool = false
    @JvmField
    var fromUpsell = false
    const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/"
    const val PROD_ABLY_API_KEY = "c8GwjQ.bc7afQ:8y41_MzTKIM5CpDa"
    const val STAGING_ABLY_API_KEY = "dDM5uw.vFfl3A:XcSHMk-84eaZSf9k1fbcA4o9KBEeKSGQNJLyS-Ymh78"
    const val PROVIDER_GOOGLE = 1
    const val PROVIDER_FACEBOOK = 4
    const val PROVIDER_TWITTER = 2
    const val STATUS_VERIFIED = "verified"
    const val STATUS_UNVERIFIED = "unverified"
    const val PERMISSION_LOCATION = 1
    const val PERMISSION_CALL = 2
    const val PERMISSION_CAMERA = 3
    const val PERMISSION_GALLERY = 4
    const val PERMISSION_PHONE_STATE = 5
    const val REQUEST_SMS_CONSENT = 6
    const val REQUEST_ADDRESS = 7
    const val REQUEST_PURCHASE = 8
    const val CACHE_EXPIRY_MINUTES = 5
    const val TABLE_MENU_CATEGORIES = "menu_categories"
    const val TABLE_MENU_ITEMS = "menu_items"
    const val TABLE_MODIFIER_GROUPS = "modifier_groups"
    const val TABLE_MODIFIER_ITEMS = "modifier_items"
    const val TABLE_INGREDIENTS = "ingredients"
    const val TABLE_ALLERGENS = "allergens"
    const val TABLE_STORES = "stores"
    const val TABLE_FACILITIES = "facilities"
    const val TABLE_FAVOURITES = "favourites"
    const val TABLE_MENU_CATEGORIES_ENTITY = "menu_categories_entity"
    const val TABLE_MENU_ITEMS_ENTITY = "menu_items_entity"
    const val TABLE_MODIFIER_GROUPS_ENTITY = "modifier_groups_entity"
    const val TABLE_MODIFIER_ITEMS_ENTITY = "modifier_items_entity"
    const val TABLE_INGREDIENTS_ENTITY = "ingredients_entity"
    const val TABLE_ALLERGENS_ENTITY = "allergens_entity"
    const val TABLE_STORES_ENTITY = "stores_entity"
    const val TABLE_TRANSLATED_ENTITY = "translated_entity"
    val PAYMENT_BRANDS = linkedSetOf<String>()

    init {
        PAYMENT_BRANDS.add("VISA")
        PAYMENT_BRANDS.add("VISA")
        PAYMENT_BRANDS.add("MASTER")
    }

    const val NO_PROMOTIONS = false
    const val VALIDATE_QUANTITY = false
    const val DEFAULT_LATITUDE = 24.7193534
    const val DEFAULT_LONGITUDE = 46.4861749
    @JvmField
    var sDeepLink = ""
    @JvmField
    var sDeepLinkId = ""
    @JvmField
    var sDeepLinkUtm: Bundle? = null
    @JvmField
    var IMAGE_TEXT = "image_text"
    const val MUKAFA_ENABLED = true
    const val MUKAFAA_PROVIDER = "alrajhi"
    const val LOYALTY_PROVIDER = "solo-loyalty"
    const val MIXPANEL_TOKEN = "3a172b606e1bd18abfad3bd6f511bbd5"
    const val HOME_PAGE_TOP_BANNER_LABEL = "Home Page Top Banner"
    const val HOME_PAGE_BOTTOM_BANNER_LABEL = "Home Page Bottom Banner"
    const val CATEGORY_PAGE_TOP_BANNER_LABEL = "Category Page Top Banner"
    const val SLIDER_INTERVAL = 8_000L
    const val NAV_DEEP_LINK_BASE_URL = "android-app://com.herfy/"
    const val INSTAGRAM_LINK = "https://www.instagram.com/herfyfsc/"
    const val FACEBOOK_LINK = "https://www.facebook.com/herfyfscksa"
    const val TIKTOK_LINK = "https://www.tiktok.com/@herfyfsc"
    const val TWITTER_LINK = "https://twitter.com/herfyfs"

    const val KEY_MENU_ITEM_ID = "menu_item_id"
    const val KEY_IS_PROMO = "is_promo"
    const val KEY_IS_EDIT ="is_edit"
    const val KEY_CART_MENU_ITEM_POSITION = "cart_menu_item_position"
    const val KEY_MENU_CATEGORY_ID ="menu_category_id"
    const val KEY_IS_UPSELL ="is_upsell"
    const val FREQUENTLY_BOUGHT_ITEM_LISTENER = "frequently_bought_item_listener"
    const val KEY_IS_ADDED_TO_FAVORITES = "is_added_to_favorites"
    const val KEY_IS_ADDED_TO_CART = "is_added_to_cart"
    const val KEY_IS_SOLD_OUT = "is_sold_out"
    const val CATER_WITH_HERFY_URL = "https://www.herfy.com/"
    const val SLIDER_HOME_TOP_ASPECT_RATIO_WIDTH = 393.0
    const val SLIDER_HOME_TOP_ASPECT_RATIO_HEIGHT = 472.0
    const val SLIDER_HOME_BOTTOM_ASPECT_RATIO_WIDTH = 361.0
    const val SLIDER_HOME_BOTTOM_ASPECT_RATIO_HEIGHT = 152.0
    const val SLIDER_MENU_ASPECT_RATIO_WIDTH = 361.0
    const val SLIDER_MENU_ASPECT_RATIO_HEIGHT = 128.0
    const val NUTRITION_INFO_ASPECT_RATIO_WIDTH = 334.0
    const val NUTRITION_INFO_ASPECT_RATIO_HEIGHT = 387.0

    const val LANGUAGE_BACKGROUND_VIDEO_URL = "https://customer-k8n892ut7y8lpez5.cloudflarestream.com/79e15cb36a7411cd45513b69abab8fed/manifest/video.mpd"
}