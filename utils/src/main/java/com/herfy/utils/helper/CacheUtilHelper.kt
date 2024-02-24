package com.herfy.utils.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Pair
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.skylinedynamics.solosdk.api.models.objects.Application
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemCombo
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
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

    suspend fun getCartItems(items: (ArrayList<MenuItem>) -> Unit) {
        val cartMenuItems: String? = pref()[CART_MENU_ITEMS, null]

        cartMenuItems?.let {
            val cartItems = GsonHelper.gsonFinal().fromJson<ArrayList<MenuItem>>(
                cartMenuItems,
                object : TypeToken<java.util.ArrayList<MenuItem?>?>() {}.type
            )
            items(cartItems)
        } ?: run {
            items(arrayListOf())
        }
    }

    fun setCartQuantity(menuItems: List<MenuItem>) {
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
        pref()[CART_QUANTITY] = quantity
    }

    fun getCartQuantity(): Int {
        return pref()[CART_QUANTITY, 0]?: 0
    }

     suspend fun addToCart(menuItem: MenuItem, result: () -> Unit) {
        compareMenuItem(-1, menuItem){ pair ->
            if (!pair.first) {
                trackEvent(MixpanelUtil.EVENT_ADD_TO_CART)
                pair.second.add(menuItem)
            }
            if (menuItem.attributes.notes.isNotEmpty() && pair.second.size == 1) {
                pair.second[0].attributes.notes = menuItem.attributes.notes
            }
            setCartQuantity(pair.second)
            val updatedCartMenuItems = Gson().toJson(pair.second)
            pref()[CART_MENU_ITEMS, updatedCartMenuItems]
            result.invoke()
        }
    }

    suspend fun updateCart(position: Int, menuItem: MenuItem, result: () -> Unit) {
        compareMenuItem(position, menuItem){ pair ->
            CacheUtil.getInstance().clearMukafaa()
            pair.second.removeAt(position)
            if (!pair.first) {
                pair.second.add(position, menuItem)
            }
            setCartQuantity(pair.second)
            val updatedCartMenuItems = Gson().toJson(pair.second)
            pref()[CART_MENU_ITEMS, updatedCartMenuItems]
            result.invoke()
        }

    }

    private suspend fun compareMenuItem(
        position: Int,
        menuItem: MenuItem,
        returnValue: (Pair<Boolean, ArrayList<MenuItem>>) -> Unit
    ) {
        var same = false
        getCartItems { menuItems ->
            for (cartMenuItem in menuItems) {
                if (position != menuItems.indexOf(cartMenuItem)) {
                    if (menuItem.id == cartMenuItem.id) {
                        var sizeIndex = 0
                        var trueIndex = 0
                        if (menuItem.sizes.modifierItems.size == 0 && cartMenuItem.sizes.modifierItems.size == 0) {
                            same = true
                        } else {
                            for (modifierItem in menuItem.sizes.modifierItems) {
                                if (modifierItem.attributes.isSelected) {
                                    for (cartModifierItem in cartMenuItem.sizes.modifierItems) {
                                        if (cartModifierItem.attributes.isSelected) {
                                            if (cartModifierItem.id == modifierItem.id) {
                                                same = true
                                                trueIndex = sizeIndex
                                                break
                                            }
                                        }
                                        sizeIndex++
                                    }
                                }
                            }
                        }
                        if (same) {
                            same = false
                            if (menuItem.drinks.modifierItems.size == 0 && cartMenuItem.drinks.modifierItems.size == 0) {
                                same = true
                            } else {
                                for (modifierItem in menuItem.drinks.modifierItems) {
                                    if (modifierItem.attributes.isSelected) {
                                        for (cartModifierItem in cartMenuItem.drinks.modifierItems) {
                                            if (cartModifierItem.attributes.isSelected) {
                                                if (cartModifierItem.id == modifierItem.id) {
                                                    same = true
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (same) {
                            same = false
                            if (menuItem.fries.modifierItems.size == 0 && cartMenuItem.fries.modifierItems.size == 0) {
                                same = true
                            } else {
                                for (modifierItem in menuItem.fries.modifierItems) {
                                    if (modifierItem.attributes.isSelected) {
                                        for (cartModifierItem in cartMenuItem.fries.modifierItems) {
                                            if (cartModifierItem.attributes.isSelected) {
                                                if (cartModifierItem.id == modifierItem.id) {
                                                    same = true
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (same) {
                            same = false
                            if (menuItem.modifierGroups.size == 0 && cartMenuItem.modifierGroups.size == 0) {
                                same = true
                            } else {
                                val menuItemSelectedModifierItems = HashMap<String, ModifierItem>()
                                val cartMenuItemSelectedModifierItems =
                                    HashMap<String, ModifierItem>()
                                for (modifierGroup in menuItem.modifierGroups) {
                                    val visibilityConfig = modifierGroup.attributes.visibilityConfig
                                    if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                                            "hidden",
                                            ignoreCase = true
                                        )
                                    ) {
                                        for (modifierItem in modifierGroup.modifierItems) {
                                            if (modifierItem.attributes.isSelected) {
                                                menuItemSelectedModifierItems[modifierItem.id] =
                                                    modifierItem
                                            }
                                        }
                                    }
                                }
                                for (modifierGroup in cartMenuItem.modifierGroups) {
                                    val visibilityConfig = modifierGroup.attributes.visibilityConfig
                                    if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                                            "hidden",
                                            ignoreCase = true
                                        )
                                    ) {
                                        for (modifierItem in modifierGroup.modifierItems) {
                                            if (modifierItem.attributes.isSelected) {
                                                cartMenuItemSelectedModifierItems[modifierItem.id] =
                                                    modifierItem
                                            }
                                        }
                                    }
                                }
                                same =
                                    menuItemSelectedModifierItems.size == cartMenuItemSelectedModifierItems.size && menuItemSelectedModifierItems.keys.containsAll(
                                        cartMenuItemSelectedModifierItems.keys
                                    ) && cartMenuItemSelectedModifierItems.keys.containsAll(
                                        menuItemSelectedModifierItems.keys
                                    )
                                if (same) {
                                    for (id in menuItemSelectedModifierItems.keys) {
                                        val menuModifierItem = menuItemSelectedModifierItems[id]
                                        val cartModifierItem = cartMenuItemSelectedModifierItems[id]
                                        same =
                                            menuModifierItem!!.attributes.quantity == cartModifierItem!!.attributes.quantity
                                        if (!same) {
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        if (same) {
                            same = false
                            same =
                                if (menuItem.ingredients.size == 0 && cartMenuItem.ingredients.size == 0) {
                                    true
                                } else {
                                    val menuItemRemovedIngredients: MutableSet<String> = HashSet()
                                    val cartMenuItemRemovedIngredients: MutableSet<String> =
                                        HashSet()
                                    for (ingredient in menuItem.ingredients) {
                                        if (!ingredient.attributes.isSelected) {
                                            menuItemRemovedIngredients.add(ingredient.id)
                                        }
                                    }
                                    for (ingredient in cartMenuItem.ingredients) {
                                        if (!ingredient.attributes.isSelected) {
                                            cartMenuItemRemovedIngredients.add(ingredient.id)
                                        }
                                    }
                                    menuItemRemovedIngredients.size == cartMenuItemRemovedIngredients.size && menuItemRemovedIngredients.containsAll(
                                        cartMenuItemRemovedIngredients
                                    ) && cartMenuItemRemovedIngredients.containsAll(
                                        menuItemRemovedIngredients
                                    )
                                }
                        }
                        try {
                            if (menuItem.isCombo && cartMenuItem.isCombo) {
                                same = compareComboItem(trueIndex, menuItem.combo, cartMenuItem.combo)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (same) {
                        val quantity =
                            cartMenuItem.attributes.quantity + menuItem.attributes.quantity
                        menuItems[menuItems.indexOf(cartMenuItem)].attributes.quantity = quantity
                        if (cartMenuItem.attributes.isUpsell) {
                            if (!cartMenuItem.attributes.isUpsell) {
                                cartMenuItem.attributes.upsellQuantity =
                                    menuItem.attributes.quantity
                            } else {
                                cartMenuItem.attributes.upsellQuantity =
                                    cartMenuItem.attributes.upsellQuantity + menuItem.attributes.quantity
                            }
                            cartMenuItem.attributes.isUpsell = true
                        }
                        break
                    }
                }
            }
            returnValue(Pair(same, menuItems))
        }
    }

    private fun compareComboItem(
        trueIndex: Int,
        menuItemCombo: MutableList<MenuItemCombo>,
        cartItemCombo: MutableList<MenuItemCombo>
    ): Boolean{
        var isSameComboItem = false
        var sameIsFalseAlready = false
        val sameItems = HashMap<String , Boolean>()
        if (menuItemCombo.size == cartItemCombo.size) {
            if (menuItemCombo[trueIndex].components.isNotEmpty() && cartItemCombo[trueIndex].components.isNotEmpty()) {
                if (menuItemCombo[trueIndex].components.size == cartItemCombo[trueIndex].components.size) {
                    val size = menuItemCombo[trueIndex].components.size
                    for (i in 0 until size) {
                        if (menuItemCombo[trueIndex].components[i].items.isNotEmpty() && cartItemCombo[trueIndex].components[i].items.isNotEmpty()) {
                            if (menuItemCombo[trueIndex].components[i].items.size == cartItemCombo[trueIndex].components[i].items.size) {
                                val iSize = menuItemCombo[trueIndex].components[i].items.size
                                for (a in 0 until iSize) {
                                    if (menuItemCombo[trueIndex].components[i].items[a].selected && cartItemCombo[trueIndex].components[i].items[a].selected) {
                                        if (menuItemCombo[trueIndex].components[i].items[a].id.equals(cartItemCombo[trueIndex].components[i].items[a].id, ignoreCase = true)) {
                                            if (menuItemCombo[trueIndex].components[i].items[a].modifierGroups.isNotEmpty()
                                                && cartItemCombo[trueIndex].components[i].items[a].modifierGroups.isNotEmpty()
                                            ) {
                                                if (menuItemCombo[trueIndex].components[i].items[a].modifierGroups.size ==
                                                    cartItemCombo[trueIndex].components[i].items[a].modifierGroups.size
                                                ) {
                                                    val mGSize =
                                                        menuItemCombo[trueIndex].components[i].items[a].modifierGroups.size
                                                    for (x in 0 until mGSize) {
                                                        val menuItemSelected: MutableSet<String> =
                                                            HashSet()
                                                        val cartMenuItemSelected: MutableSet<String> =
                                                            HashSet()
                                                        val menuItems1: MutableList<ModifierItem> =
                                                            java.util.ArrayList()
                                                        val menuItems2: MutableList<ModifierItem> =
                                                            java.util.ArrayList()
                                                        for (b in cartItemCombo[trueIndex].components[i].items[a].modifierGroups[x].modifierItems.indices) {
                                                            val menuModifierItem =
                                                                menuItemCombo[trueIndex].components[i].items[a].modifierGroups[x].modifierItems[b]
                                                            val cartMenuModifierItem =
                                                                cartItemCombo[trueIndex].components[i].items[a].modifierGroups[x].modifierItems[b]
                                                            if (menuModifierItem.attributes.isSelected && menuModifierItem.attributes.quantity != 0) {
                                                                menuItemSelected.add(
                                                                    menuModifierItem.id
                                                                )
                                                                menuItems1.add(
                                                                    menuModifierItem
                                                                )
                                                            }
                                                            if (cartMenuModifierItem.attributes.isSelected && cartMenuModifierItem.attributes.quantity != 0) {
                                                                cartMenuItemSelected.add(
                                                                    cartMenuModifierItem.id
                                                                )
                                                                menuItems2.add(
                                                                    cartMenuModifierItem
                                                                )
                                                            }
                                                        }
                                                        if (!sameIsFalseAlready) {
                                                            isSameComboItem = menuItemSelected.size == cartMenuItemSelected.size && menuItemSelected.containsAll(cartMenuItemSelected)
                                                            if (!isSameComboItem) {
                                                                sameIsFalseAlready =
                                                                    true
                                                            } else {
                                                                for (h in menuItems1.indices) {
                                                                    var theSame =
                                                                        true
                                                                    val itemM =
                                                                        menuItems1[h]
                                                                    for (l in menuItems2.indices) {
                                                                        if (itemM.attributes.quantity != menuItems2[l].attributes.quantity) {
                                                                            theSame =
                                                                                false
                                                                            break
                                                                        }
                                                                    }
                                                                    if (!theSame) {
                                                                        isSameComboItem =
                                                                            false
                                                                        sameIsFalseAlready =
                                                                            true
                                                                        break
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    isSameComboItem = false
                                                    break
                                                }
                                            } else {
                                                if (cartItemCombo[trueIndex].components[i].items[a].modifierGroups.size != menuItemCombo[trueIndex].components[i].items[a].modifierGroups.size) {
                                                    isSameComboItem = false
                                                    break
                                                }
                                            }
                                        } else {
                                            isSameComboItem = false
                                            break
                                        }
                                    } else {
                                        if (menuItemCombo[trueIndex].components[i].items[a].selected &&
                                            !cartItemCombo[trueIndex].components[i].items[a].selected
                                        ) {
                                            isSameComboItem = false
                                            break
                                        }
                                        if (!menuItemCombo[trueIndex].components[i].items[a].selected &&
                                            cartItemCombo[trueIndex].components[i].items[a].selected
                                        ) {
                                            isSameComboItem = false
                                            break
                                        }

                                    }

                                }
                            } else {
                                isSameComboItem = false
                                break
                            }
                        } else {
                            if (cartItemCombo[trueIndex].components[i].items.size != menuItemCombo[trueIndex].components[i].items.size) {
                                isSameComboItem = false
                                break
                            }
                        }
                        sameItems[menuItemCombo[trueIndex].components[i].id] = isSameComboItem
                    }
                } else {
                    isSameComboItem = false
                }
            }
        } else {
            isSameComboItem = false
        }
        var isSameItem = true
        if(sameItems.size > 0){
            for(hasSame in sameItems.values){
                if(!hasSame){
                    isSameItem = hasSame
                    break
                }
            }
        }else {
            isSameItem = isSameComboItem
        }


        return isSameItem
    }

    companion object {

        private const val CACHE = "Cache"
        const val SLIDER_ID = "SliderId"
        const val ORDER_TYPE = "OrderType"
        private const val TRANSLATIONS = "Translations"
        private const val APPLICATION = "Application"
        private const val ORDER_STORE = "OrderStore"
        private const val CART_MENU_ITEMS = "CartMenuItems"
        private const val CART_QUANTITY = "CartQuantity"
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