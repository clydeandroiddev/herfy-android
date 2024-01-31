package com.herfy.analytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.herfy.BuildConfig
import com.herfy.data.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.herfy.util.AuthUtil
import com.herfy.util.BuildUtil
import com.herfy.util.CacheUtil
import com.herfy.util.EnvUtil
import org.json.JSONObject


class MixpanelUtil(val context: Context) {

    companion object {
        private var instance: MixpanelAPI? = null
        fun get(context: Context): MixpanelAPI? {
            if (instance == null) {
                instance = MixpanelAPI.getInstance(
                    context,
                    Values.MIXPANEL_TOKEN,
                    true
                )
            }
            return instance
        }

        const val EVENT_VIEW_ACCOUNT = "view_account"
        const val EVENT_UPDATE_ACCOUNT = "update_account"
        const val EVENT_DELETE_ACCOUNT = "delete_account"
        const val EVENT_LOGIN = "login"
        const val EVENT_LOGOUT = "logout"
        const val EVENT_ADD_ADDRESS = "add_address"
        const val EVENT_VIEW_ORDER_HISTORY = "view_order_history"
        const val EVENT_VIEW_ORDER_DETAILS = "view_order_details"
        const val EVENT_REORDER = "re_order_this"
        const val EVENT_CANCEL_ORDER = "cancel_order"
        const val EVENT_SEND_FEEDBACK = "send_feedback"
        const val EVENT_VIEW_NOTIFICATIONS = "view_notifications"
        const val EVENT_SEARCH_ITEM = "search_item"
        const val EVENT_VIEW_MENU = "view_menu"
        const val EVENT_VIEW_ITEM = "view_item"
        const val EVENT_FAVORITE_ITEM = "favorite_item"
        const val EVENT_VIEW_FAVORITE_ITEMS = "view_favorite_items"
        const val EVENT_ADD_TO_CART = "add_to_cart"
        const val EVENT_VIEW_CART = "view_cart"
        const val EVENT_REMOVE_FROM_CART = "remove_from_cart"
        const val EVENT_EDIT_ITEM_FROM_CART = "edit_item_from_cart"
        const val EVENT_INCREASE_ITEM_QUANTITY_CART = "increase_item_quantity_cart"
        const val EVENT_DECREASE_ITEM_QUANTITY_CART = "decrease_item_quantity_cart"
        const val EVENT_APPLY_PROMO = "apply_promo"
        const val EVENT_PLACE_ORDER = "place_order"
        const val EVENT_CHECKOUT = "\$checkout"

        fun registerSuperProperties() {
            if(EnvUtil.isProduction()) {
                val concept = CacheUtil.getInstance().concept
                val props = JSONObject()
                if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                    val country = CacheUtil.getInstance().country
                    if (country != null) {
                        props.put("App Key", country.applicationKey)
                        props.put("business_reference", country.applicationKey)
                    }
                } else {
                    props.put("App Key", BuildUtil.soloAppKey())
                    props.put("business_reference", BuildUtil.soloAppKey())
                }
                props.put("business_name", concept.label)
                props.put("business_country", concept.country)
                if (AuthUtil.getInstance().isSignedIn) {
                    props.put("consumer_id", AuthUtil.getInstance().customer.id)
                }

                val consumerMeta = CacheUtil.getInstance().consumerData.meta
                props.put("consumer_total_spent", consumerMeta.totalSpent)
                props.put("consumer_number_of_orders", consumerMeta.numberOfOrders)
                props.put("consumer_number_of_canceled_orders", consumerMeta.numberOfCanceledOrders)

                instance?.registerSuperProperties(props)
            }
        }


        fun trackEvent(eventName: String) {
            if(EnvUtil.isProduction()) {
                instance?.track(eventName)
            }
        }

        fun checkout(
            orderId: String,
            store: Store,
            orderType: String,
            paymentMethod: String
        ) {
            if(EnvUtil.isProduction()) {
                val props = JSONObject()
                props.put("branch_name", store.attributes.name)
                props.put("branch_reference", store.id)
                props.put("order_id", orderId)
                props.put("order_source", "Android")
                props.put("order_type", orderType)
                props.put("payment_method", paymentMethod)
                instance?.track(EVENT_CHECKOUT, props)
            }
        }

        fun logout() {
            if(EnvUtil.isProduction()) {
                trackEvent(EVENT_LOGOUT)
                instance?.reset()
            }
        }

    }

    init {
        if(EnvUtil.isProduction()) {
            get(context)
        }
    }

}