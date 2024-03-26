package com.herfy.utils.helper

import com.herfy.utils.constants.Values

object DeepLinkUtil {

    @JvmStatic
    fun parse(url: String) {
        if (url.contains("categories") || url.contains("category")) {
            categories(url)
        } else if (url.contains("item") || url.contains("item")) {
            items(url)
        } else if (url.contains("digital-coupon") || url.contains("promotions") || url.contains("promo")) {
            promos(url)
        } else if (url.contains("shoppingcart") || url.contains("shopping-cart")) {
            cart()
        } else if (url.contains("order")) {
            order(url)
        }
    }

    @JvmStatic
    fun categories(url: String) {
        val delimeter = "/"
        val id = url.split(delimeter).toTypedArray()[url.split(delimeter).toTypedArray().size - 1]
        Values.sDeepLink = "categories"
        Values.sDeepLinkId = id
    }

    @JvmStatic
    fun items(url: String) {
        val delimeter = "/"
        val id = url.split(delimeter).toTypedArray()[url.split(delimeter).toTypedArray().size - 1]
        Values.sDeepLink = "items"
        Values.sDeepLinkId = id
    }

    @JvmStatic
    fun promos(url: String) {
        val delimeter = "/"
        val id = url.split(delimeter).toTypedArray()[url.split(delimeter).toTypedArray().size - 1]
        Values.sDeepLink = "promos"
        Values.sDeepLinkId = id
    }

    @JvmStatic
    fun order(url: String) {
        val delimeter = "/"
        val id = url.split(delimeter).toTypedArray()[url.split(delimeter).toTypedArray().size - 1]
        Values.sDeepLink = "order"
        Values.sDeepLinkId = id
    }

    @JvmStatic
    fun cart() {
        Values.sDeepLink = "cart"
    }

}