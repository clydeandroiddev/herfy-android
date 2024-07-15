package com.herfy.analytics.facebook

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_ADDED_PAYMENT_INFO
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_ADDED_TO_CART
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_PURCHASED
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_VIEWED_CONTENT
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CONTENT
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CONTENT_ID
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CONTENT_TYPE
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CURRENCY
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_SUCCESS
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_VALUE_YES
import com.facebook.appevents.AppEventsLogger
import com.google.gson.Gson
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import org.json.JSONArray


class FacebookAppEventUtil {

    companion object {

        fun completeTutorialEvent(context: Context?) {
            context?.let {
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_SUCCESS, EVENT_PARAM_VALUE_YES)

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_COMPLETED_TUTORIAL, params)
            }
        }

        fun completeRegistrationEvent(context: Context?, registrationMethod: String) {
            context?.let {
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(
                    EVENT_PARAM_CURRENCY,
                    CacheUtil.getInstance().concept.currencyCode
                )
                params.putString(EVENT_PARAM_VALUE_TO_SUM, "0")
                params.putString(EVENT_PARAM_REGISTRATION_METHOD, registrationMethod)

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_COMPLETED_REGISTRATION, params)
            }
        }

        fun viewMenuItemEvent(context: Context?, menuItem: MenuItem) {
            context?.let {
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_CONTENT_ID, menuItem.id)
                params.putString(EVENT_PARAM_CONTENT_TYPE, "product")
                params.putString(
                    EVENT_PARAM_CURRENCY,
                    CacheUtil.getInstance().concept.currencyCode
                )
                params.putString(
                    EVENT_PARAM_VALUE_TO_SUM,
                    menuItem.getPrice(CacheUtil.getOrderStoreId()).toString()
                )

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_VIEWED_CONTENT, params)
            }
        }

        fun addToWishlistEvent(context: Context?, menuItem: MenuItem) {
            context?.let {
                val jsObject = Gson().toJson(menuItem)
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_CONTENT_TYPE, "product")
                params.putString(EVENT_PARAM_CONTENT, jsObject.toString())
                params.putString(
                    EVENT_PARAM_CURRENCY,
                    CacheUtil.getInstance().concept.currencyCode
                )
                if (CacheUtil.getInstance().orderStore != null) {
                    params.putString(
                        EVENT_PARAM_VALUE_TO_SUM,
                        menuItem.attributes.getPrice(CacheUtil.getInstance().orderStore.id)
                            .toString()
                    )
                }
                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_ADDED_TO_WISHLIST, params)
            }
        }

        fun addToCartEvent(context: Context?, menuItem: MenuItem, total: Double) {
            context?.let {
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_CONTENT_ID, menuItem.id)
                params.putString(EVENT_PARAM_CONTENT_TYPE, "product")
                params.putString(
                    EVENT_PARAM_CURRENCY,
                    CacheUtil.getInstance().concept.currencyCode
                )
                params.putString(EVENT_PARAM_VALUE_TO_SUM, total.toString())

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_ADDED_TO_CART, params)
            }
        }

        fun initiateCheckoutEvent(
            context: Context?,
            menuItems: ArrayList<MenuItem>,
            totalPrice: Double
        ) {
            context?.let {
                val jsArray = JSONArray(menuItems)
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_CONTENT_TYPE, "product")
                params.putString(EVENT_PARAM_CONTENT, jsArray.toString())
                params.putString(
                    EVENT_PARAM_CURRENCY,
                    CacheUtil.getInstance().concept.currencyCode
                )
                params.putString(EVENT_PARAM_VALUE_TO_SUM, totalPrice.toString())

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_INITIATED_CHECKOUT, params)
            }

        }

        fun purchaseEvent(context: Context?, menuItems: List<MenuItem>, totalPrice: Double) {
            context?.let {
                val jsArray = JSONArray(menuItems)
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString(EVENT_PARAM_CONTENT_TYPE, "product")
                params.putString(EVENT_PARAM_CONTENT, jsArray.toString())
                params.putString(EVENT_PARAM_CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                params.putString(EVENT_PARAM_VALUE_TO_SUM, totalPrice.toString())

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_PURCHASED, params)
            }
        }

        fun addPaymentInfoEvent(context: Context?, paymentType: String) {
            context?.let {
                val params = Bundle()
                params.putString("app_name", StringUtil.getAppName(it))
                params.putString("payment_type", paymentType)

                val appEventLogger = AppEventsLogger.newLogger(it)
                appEventLogger.logEvent(EVENT_NAME_ADDED_PAYMENT_INFO, params)
            }
        }

    }
}