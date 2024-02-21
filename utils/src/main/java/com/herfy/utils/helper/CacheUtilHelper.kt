package com.herfy.utils.helper

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.skylinedynamics.solosdk.api.models.objects.Application
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONException
import org.json.JSONObject

class CacheUtilHelper(val context : Context) {
    fun pref(): SharedPreferences = context.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
    fun getTranslations(key: String): String {
        val translations: String? = pref()[TRANSLATIONS]
        if (translations != null) {
            try {
                val translationsObject = JSONObject(translations)
                if (translationsObject.has(key)) {
                    val text = translationsObject.getJSONObject(key)
                    if (text != null) {
                        return if (LocaleUtil.getInstance().isEnglish()) {
                            text.getString("en_US")
                        } else if (LocaleUtil.getInstance().isFrench()) {
                            text.getString("fr")
                        } else {
                            text.getString("ar_SA")
                        }
                    }
                } else {
                    return key
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return key
    }
    fun getTranslations(key: String, defaultValue: String): String {
        val translations: String? = pref()[TRANSLATIONS]
        if (translations != null) {
            try {
                val translationsObject = JSONObject(translations)
                if (translationsObject.has(key)) {
                    val text = translationsObject.getJSONObject(key)
                    if (text != null) {
                        return if (LocaleUtil.getInstance().isEnglish()) {
                            text.getString("en_US")
                        } else if (LocaleUtil.getInstance().isFrench()) {
                            text.getString("fr")
                        } else {
                            text.getString("ar_SA")
                        }
                    }
                } else {
                    return defaultValue
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return defaultValue
    }

    fun getApplication(): Application? {
        val applicationValue: String? = pref()[APPLICATION]
        return if (applicationValue != null) {
            GsonHelper.gsonFinal().fromJson<Application>(
                applicationValue,
                object : TypeToken<Application?>() {}.getType()
            )
        } else null
    }

    fun getOrderStore(): Store? {
        val orderStoreValue: String? = pref()[ORDER_STORE, null]
        return if (orderStoreValue != null) {
            GsonHelper.gsonFinal().fromJson(orderStoreValue, object : TypeToken<Store?>() {}.type)
        } else null
    }

    fun getOrderType(): OrderType {
        val orderType: Int? = pref()[ORDER_TYPE, OrderType.NONE.value]
        return when (orderType) {
            1 -> OrderType.PICKUP
            2 -> OrderType.DINE_IN
            3 -> OrderType.DELIVERY
            4 -> OrderType.CURBSIDE
            else -> OrderType.NONE
        }
    }

    fun getCartQuantity(result: (Int) -> Unit) {
        val cartMenuItems: String? = pref()[CART_MENU_ITEMS, null]
        cartMenuItems?.let {
            val menuItems: List<MenuItem> = GsonHelper.gsonFinal().fromJson(
                cartMenuItems,
                object : TypeToken<ArrayList<MenuItem?>?>() {}.type
            );

            var quantity = 0
            for (menuItem in menuItems) {
                quantity += menuItem.attributes.quantity
            }
            if (CacheUtil.getInstance().couponCode != null && CacheUtil.getInstance().couponMenuItem != null) {
                quantity += CacheUtil.getInstance().couponMenuItem.attributes.quantity
            }
            if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotion != null && CacheUtil.getInstance().campaignPromotionMenuItem != null) {
                quantity += CacheUtil.getInstance().campaignPromotionMenuItem.attributes.quantity
            }
            result(quantity)
        } ?: run {
            result(0)
        }
    }

    companion object {

        private const val CACHE = "Cache"
        const val SLIDER_ID = "SliderId"
        const val ORDER_TYPE = "OrderType"
        private const val TRANSLATIONS = "Translations"
        private const val APPLICATION = "Application"
        private const val ORDER_STORE = "OrderStore"
        private const val CART_MENU_ITEMS = "CartMenuItems"
        inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
            val editor = this.edit()
            operation(editor)
            editor.apply()
        }

        /**
         * puts a key value pair in shared prefs if doesn't exists, otherwise updates value on given [key]
         */
        operator fun SharedPreferences.set(key: String, value: Any?) {
            when (value) {
                is String? -> edit { it.putString(key, value) }
                is Int -> edit { it.putInt(key, value) }
                is Boolean -> edit { it.putBoolean(key, value) }
                is Double -> edit { it.putLong(key, value.toRawBits()) }
                is Float -> edit { it.putFloat(key, value) }
                is Long -> edit { it.putLong(key, value) }
                else -> throw UnsupportedOperationException("Not yet implemented")
            }
        }

        /**
         * finds value on given key.
         * [T] is the type of value
         * @param defaultValue optional default value - will take null for strings, false for bool and -1 for numeric values if [defaultValue] is not specified
         */
        inline operator fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T? {
            return when (T::class) {
                String::class -> getString(key, defaultValue as? String) as T?
                Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
                Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
                Double::class -> Double.fromBits(getLong(key, defaultValue as? Long ?: -1)) as T?
                Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
                Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
                else -> throw UnsupportedOperationException("Not yet implemented")
            }
        }
    }
}