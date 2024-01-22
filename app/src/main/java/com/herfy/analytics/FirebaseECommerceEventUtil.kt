package com.herfy.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import com.google.firebase.analytics.FirebaseAnalytics
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.herfy.util.AuthUtil
import com.herfy.util.CacheUtil
import com.herfy.util.LocaleUtil

class FirebaseECommerceEventUtil {

    companion object {

        fun selectItemEvent(context: Context?, menuItem: MenuItem, menuCategory: MenuCategory?, price: Double) {
            context?.let {
                val detailsParam = Bundle()
                val bundleParam = Bundle()

                detailsParam.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                detailsParam.putString(FirebaseAnalytics.Param.ITEM_NAME, menuItem.getName(
                    LocaleUtil.getInstance().language))
                detailsParam.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, menuCategory?.getName(
                    LocaleUtil.getInstance().language))
                detailsParam.putDouble(FirebaseAnalytics.Param.PRICE, price)

                bundleParam.putString(FirebaseAnalytics.Param.ITEM_LIST_ID, menuCategory?.id)
                bundleParam.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, menuCategory?.getName(
                    LocaleUtil.getInstance().language))
                bundleParam.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(detailsParam))

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundleParam)
            }
        }

        fun viewItemEvent(context: Context?, menuItem: MenuItem, menuCategory: MenuCategory?) {
            context?.let {
                val bundleParam = Bundle()

                bundleParam.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                bundleParam.putString(FirebaseAnalytics.Param.ITEM_NAME, menuItem.getName(LocaleUtil.getInstance().language))
                bundleParam.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, menuCategory?.getName(
                    LocaleUtil.getInstance().language))
                bundleParam.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.getPrice(CacheUtil.getOrderStoreId()))

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundleParam)
            }
        }

        fun addToCartEvent(context: Context?, menuItem: MenuItem, menuCategory: MenuCategory?, price: Double, quantity: Int) {
            context?.let {
                val detailsParam = Bundle()
                val addToCartBundle = Bundle()
                val bundleParam = Bundle()

                detailsParam.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                detailsParam.putString(FirebaseAnalytics.Param.ITEM_NAME, menuItem.getName(
                    LocaleUtil.getInstance().language))
                detailsParam.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, menuCategory?.getName(
                    LocaleUtil.getInstance().language))
                detailsParam.putDouble(FirebaseAnalytics.Param.PRICE, price)

                addToCartBundle.putLong(FirebaseAnalytics.Param.QUANTITY,quantity.toLong())
                addToCartBundle.getString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                addToCartBundle.getString(FirebaseAnalytics.Param.VALUE, price.toString())
                addToCartBundle.putParcelableArray(FirebaseAnalytics.Param.ITEMS,arrayOf<Parcelable>(addToCartBundle))

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, bundleParam)
            }
        }

        fun removeItemEvent(context: Context?, menuItem: MenuItem, totalPrice: String) {
            context?.let {
                val detailsParam = Bundle()
                val bundleParam = Bundle()

                detailsParam.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                detailsParam.putString(FirebaseAnalytics.Param.ITEM_NAME, menuItem.getName(
                    LocaleUtil.getInstance().language))
                detailsParam.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                detailsParam.putLong(FirebaseAnalytics.Param.QUANTITY, menuItem.attributes.quantity.toLong())

                bundleParam.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                bundleParam.putString(FirebaseAnalytics.Param.VALUE, totalPrice)
                bundleParam.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(detailsParam))

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, bundleParam)
            }
        }

        fun viewCartEvent(context: Context?, menuItems: ArrayList<MenuItem>, totalPrice: Double) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    itemDetails.putLong(FirebaseAnalytics.Param.QUANTITY,menuItem.attributes.quantity.toLong())
                    bundles.add(itemDetails)
                }

                val viewCartParams = Bundle()
                viewCartParams.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                viewCartParams.putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
                viewCartParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_CART, viewCartParams)
            }

        }

        fun checkoutEvent(context: Context?, menuItems: ArrayList<MenuItem>, totalPrice: Double) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()

                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    itemDetails.putLong(FirebaseAnalytics.Param.QUANTITY,menuItem.attributes.quantity.toLong())

                    bundles.add(itemDetails)
                }

                val beginCheckoutParams = Bundle()
                beginCheckoutParams.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                beginCheckoutParams.putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
                beginCheckoutParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, beginCheckoutParams)
            }
        }

        fun addPaymentInfoEvent(context: Context?, menuItems: List<MenuItem>, totalPrice: Double) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    itemDetails.putLong(FirebaseAnalytics.Param.QUANTITY,menuItem.attributes.quantity.toLong())
                    bundles.add(itemDetails)
                }

                val addPaymentParams = Bundle()
                addPaymentParams.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                addPaymentParams.putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
                addPaymentParams.putString(FirebaseAnalytics.Param.PAYMENT_TYPE,
                    CacheUtil.getInstance().orderPayment.value)
                addPaymentParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO, addPaymentParams)
            }
        }

        fun applyPromotionEvent(context: Context?, promotionId: String, promotionName: String, menuItems: List<MenuItem>) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    bundles.add(itemDetails)
                }

                val promoParams = Bundle()
                promoParams.putString(FirebaseAnalytics.Param.PROMOTION_ID, promotionId)
                promoParams.putString(FirebaseAnalytics.Param.PROMOTION_NAME, promotionName)
                promoParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_PROMOTION, promoParams)
            }

        }

        fun purchaseEvent(context: Context?, menuItems: List<MenuItem>, totalPrice: Double) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    itemDetails.putLong(FirebaseAnalytics.Param.QUANTITY,menuItem.attributes.quantity.toLong())
                    bundles.add(itemDetails)
                }

                val purchaseParams = Bundle()
                purchaseParams.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                purchaseParams.putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
                purchaseParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, purchaseParams)
            }

        }

        fun refundevent(context: Context?, menuItems: List<MenuItem>, totalPrice: Double) {
            context?.let {
                val bundles = ArrayList<Bundle>()
                menuItems.forEach { menuItem ->
                    val itemDetails = Bundle()
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_ID, menuItem.id)
                    itemDetails.putString(FirebaseAnalytics.Param.ITEM_NAME,menuItem.getName(
                        LocaleUtil.getInstance().language))
                    itemDetails.putDouble(FirebaseAnalytics.Param.PRICE, menuItem.attributes.price)
                    itemDetails.putLong(FirebaseAnalytics.Param.QUANTITY,menuItem.attributes.quantity.toLong())
                    bundles.add(itemDetails)
                }

                val purchaseParams = Bundle()
                purchaseParams.putString(FirebaseAnalytics.Param.CURRENCY, CacheUtil.getInstance().concept.currencyCode)
                purchaseParams.putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
                purchaseParams.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, bundles)

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.REFUND, purchaseParams)
            }

        }

        fun signInEvent(context: Context?, customerId: String?) {
            context?.let {
                var id = if(customerId.isNullOrEmpty()) AuthUtil.getInstance().customer.id else customerId
                val signInParams = Bundle()
                signInParams.putString("customer_id", id)
                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent("sign_in", signInParams)
            }
        }

        fun abandonedCartEvent(context: Context?, menuItems: List<MenuItem>) {
            context?.let {
                val bundles = ArrayList<String>()
                menuItems.forEach { menuItem ->
//                    val itemDetails = Bundle()
//                    itemDetails.putString("itemname",menuItem.getName(LocaleUtil.getInstance().language))
                    bundles.add(menuItem.getName(LocaleUtil.getInstance().language))
                }

                val abandonedItems = Bundle()
                abandonedItems.putString("Items", bundles.toString())

                @SuppressLint("MissingPermission")
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent("abandoned_cart", abandonedItems)
            }
        }
    }

}