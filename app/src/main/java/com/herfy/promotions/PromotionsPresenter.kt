@file:OptIn(DelicateCoroutinesApi::class)

package com.herfy.promotions

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.herfy.model.PromotionCategory
import com.herfy.repository.HomeRepository
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CoroutineContextProvider
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Promotion
import com.skylinedynamics.solosdk.util.GsonHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

class PromotionsPresenter @Inject constructor(
    private val view: PromotionsContract.View,
    private val homeRepository: HomeRepository,
    private val cacheUtilHelper: CacheUtilHelper) : PromotionsContract.Presenter {
    private var digitalCoupons = ArrayList<Campaign>()
    private var menuItems = ArrayList<MenuItem>()
    private var digitalCall: Call<ResponseBody>? = null

    private var contextProvider = CoroutineContextProvider()
    private var allPromotionsList: List<Campaign>? = null

    init {
        digitalCall = null
        this.view.setPresenter(this)
    }

    override fun start() {
        view.setupViews()
        view.setupFonts()
        view.setupTranslations()
    }

    val menuItemsTotalPrice: Double
        get() {
            var subtotalPrice = 0.0
            for (menuItem in menuItems) {
                subtotalPrice += getMenuItemTotalPrice(menuItem) * menuItem.attributes.quantity
            }
            return subtotalPrice
        }

    fun getMenuItemTotalPrice(item: MenuItem): Double {
        var orderStoreId: String? = null
        var counter = 0
        var comboIndex = 0
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }
        var totalPrice = item.getPrice(orderStoreId)
        if (item.sizes.modifierItems.size > 0) {
            if (!item.isPromo) {
                totalPrice = 0.0
            }
            for (modifierItem in item.sizes.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    totalPrice += modifierItem.attributes.price
                    comboIndex = counter
                    break
                }
                counter++
            }
        }
        for (modifierItem in item.drinks.modifierItems) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierItem in item.fries.modifierItems) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierGroup in item.modifierGroups) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || !visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
                )
            ) {
                var selectedFreeModifier = 0
                for (modifierItem in modifierGroup.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                            val totalSelectedFreeModifier =
                                selectedFreeModifier + modifierItem.attributes.quantity
                            if (totalSelectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                selectedFreeModifier += modifierItem.attributes.quantity
                            } else {
                                selectedFreeModifier = modifierGroup.attributes.freeModifiersCount
                                totalPrice += modifierItem.attributes.price * (totalSelectedFreeModifier - modifierGroup.attributes.freeModifiersCount)
                            }
                        } else {
                            totalPrice += modifierItem.attributes.price * modifierItem.attributes.quantity
                        }
                    }
                }
            }
        }
        for (ingredient in item.ingredients) {
            if (ingredient.attributes.isSelected) {
                totalPrice += ingredient.attributes.price
            }
        }
        try {
            if (item.isCombo) {
                for (components in item.combo[comboIndex].components) {
                    for (componentModifierItem in components.items) {
                        if (componentModifierItem.selected) {
                            if (!item.isSandwichOnly) totalPrice += componentModifierItem.price
                            if (!componentModifierItem.modifierGroups.isEmpty()) {
                                for (modifierGroup in componentModifierItem.modifierGroups) {
                                    for (i in modifierGroup.modifierItems.indices) {
                                        if (modifierGroup.modifierItems[i].attributes.isSelected) {
                                            totalPrice += modifierGroup.modifierItems[i].attributes.price * modifierGroup.modifierItems[i].attributes.quantity
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalPrice
    }

    override fun loadPromotions() {

        GlobalScope.launch(contextProvider.IO){
            val validDelivery =
                CacheUtil.getInstance().orderType === OrderType.DELIVERY && (CacheUtil.getInstance().orderAddress != null || CacheUtil.getInstance().orderDeliveryAddress != null)
            val validPickup =
                CacheUtil.getInstance().pickupTypes() && CacheUtil.getInstance().orderStore != null
            val customer = AuthUtil.getInstance().customer
            val concept = CacheUtil.getInstance().concept
            val store = CacheUtil.getInstance().orderStore
            cacheUtilHelper.getCartItems {
                menuItems = it

                val subtotal = menuItemsTotalPrice
                var deliveryCharge = concept.deliveryCharge
                try {
                    val minimumOrderAmountFreeDelivery = concept.minimumOrderAmountFreeDelivery
                    if (minimumOrderAmountFreeDelivery != 0.0 && subtotal >= minimumOrderAmountFreeDelivery) {
                        deliveryCharge = concept.specialDeliveryPrice
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val orderBody = JsonObject()
                if (AuthUtil.getInstance().isSignedIn) {
                    orderBody.addProperty("customer", customer.id)
                }
                orderBody.addProperty("source", "Android")
                orderBody.addProperty("notes", CacheUtil.getInstance().cartNotes)
                orderBody.addProperty(
                    "subtotal",
                    String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
                )
                var discount = 0.0
                val couponCode = CacheUtil.getInstance().couponCode
                if (couponCode != null && !couponCode.isEmpty()) {
                    orderBody.addProperty("coupon-code", couponCode)
                }
                if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotion != null) {
                    orderBody.addProperty("campaign-id", CacheUtil.getInstance().campaign.id)
                }
                var discountedSubtotal = subtotal - discount
                if (discountedSubtotal < 0) {
                    discountedSubtotal = 0.0
                    discount = subtotal
                }
                orderBody.addProperty(
                    "discount",
                    String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
                )
                if (validDelivery || validPickup) {
                    when (CacheUtil.getInstance().orderType) {
                        OrderType.PICKUP -> {
                            deliveryCharge = 0.0
                            if (CacheUtil.getInstance().application.attributes.getOrderTypes()
                                    .contains(OrderType.DINE_IN)
                            ) {
                                orderBody.addProperty("type", "to-go")
                            } else {
                                orderBody.addProperty("type", "pickup")
                            }
                            if (store != null) {
                                orderBody.addProperty("location", store.id)
                            }
                        }

                        OrderType.DINE_IN -> {
                            deliveryCharge = 0.0
                            orderBody.addProperty("type", "eat-in")
                            if (store != null) {
                                orderBody.addProperty("location", store.id)
                            }
                        }

                        OrderType.DELIVERY -> {
                            orderBody.addProperty("type", "deliver")
                            val address = CacheUtil.getInstance().orderAddress
                            if (address != null) {
                                orderBody.addProperty("address", address.id)
                            }
                        }

                        OrderType.CURBSIDE -> {
                            deliveryCharge = 0.0
                            if (CacheUtil.getInstance().orderStore != null) {
                                orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
                            }
                            if (CacheUtil.getInstance().curbsideCar != null) {
                                orderBody.addProperty("type", "curbside")
                                orderBody.addProperty("car-id", CacheUtil.getInstance().curbsideCar.id)
                            }
                        }

                        OrderType.NONE -> deliveryCharge = 0.0
                        OrderType.DRIVE_THRU -> {
                            orderBody.addProperty("type", "drive-thru")
                            if (store != null) {
                                orderBody.addProperty("location", store.id)
                            }
                        }
                    }
                }
                val vatAmount = 0.0
                var total =
                    String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount)
                        .toDouble()
                if (total < 0) {
                    total = 0.0
                }
                orderBody.addProperty("total", total)
                val items = JsonArray()
                val orderStoreId = CacheUtil.getOrderStoreId()
                for (menuItem in menuItems) {
                    val item = JsonObject()
                    val modifiers = JsonArray()
                    val ingredients = JsonArray()
                    item.addProperty("id", menuItem.id)
                    item.addProperty(
                        "price",
                        String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId)).toDouble()
                    )
                    item.addProperty("quantity", menuItem.attributes.quantity)
                    item.addProperty("is-reorder", menuItem.attributes.isReorder)
                    item.addProperty("is-promotion", menuItem.isPromo)
                    item.addProperty("notes", menuItem.attributes.notes)
                    for (size in menuItem.sizes.modifierItems) {
                        if (size.attributes.isSelected) {
                            val modifier = JsonObject()
                            modifier.addProperty("id", size.id)
                            modifier.addProperty(
                                "price",
                                if (menuItem.isPromo) 0 else String.format(
                                    Locale.ENGLISH,
                                    "%.2f",
                                    size.attributes.price
                                ).toDouble()
                            )
                            modifier.addProperty("quantity", size.attributes.quantity)
                            modifiers.add(modifier)
                        }
                    }
                    for (drink in menuItem.drinks.modifierItems) {
                        if (drink.attributes.isSelected) {
                            val modifier = JsonObject()
                            modifier.addProperty("id", drink.id)
                            modifier.addProperty(
                                "price",
                                if (menuItem.isPromo) 0 else String.format(
                                    Locale.ENGLISH,
                                    "%.2f",
                                    drink.attributes.price
                                ).toDouble()
                            )
                            modifier.addProperty("quantity", drink.attributes.quantity)
                            modifiers.add(modifier)
                        }
                    }
                    for (fries in menuItem.fries.modifierItems) {
                        if (fries.attributes.isSelected) {
                            val modifier = JsonObject()
                            modifier.addProperty("id", fries.id)
                            modifier.addProperty(
                                "price",
                                if (menuItem.isPromo) 0 else String.format(
                                    Locale.ENGLISH,
                                    "%.2f",
                                    fries.attributes.price
                                ).toDouble()
                            )
                            modifier.addProperty("quantity", fries.attributes.quantity)
                            modifiers.add(modifier)
                        }
                    }
                    for (modifierGroup in menuItem.modifierGroups) {
                        val visibilityConfig = modifierGroup.attributes.visibilityConfig
                        if (visibilityConfig == null || !visibilityConfig.initialValue.equals(
                                "hidden",
                                ignoreCase = true
                            )
                        ) {
                            for (modifierItem in modifierGroup.modifierItems) {
                                if (modifierItem.attributes.isSelected) {
                                    val modifier = JsonObject()
                                    modifier.addProperty("id", modifierItem.id)
                                    modifier.addProperty(
                                        "price", if (menuItem.isPromo) 0 else String.format(
                                            Locale.ENGLISH, "%.2f", modifierItem.attributes.price
                                        ).toDouble()
                                    )
                                    modifier.addProperty("quantity", modifierItem.attributes.quantity)
                                    modifiers.add(modifier)
                                }
                            }
                        }
                    }
                    for (modifierItem in menuItem.modifierItems) {
                        val modifier = JsonObject()
                        modifier.addProperty("id", modifierItem.id)
                        modifier.addProperty(
                            "price",
                            if (menuItem.isPromo) 0 else String.format(
                                Locale.ENGLISH,
                                "%.2f",
                                modifierItem.attributes.price
                            ).toDouble()
                        )
                        modifier.addProperty("quantity", modifierItem.attributes.quantity)
                        modifiers.add(modifier)
                    }
                    for (ingredient in menuItem.ingredients) {
                        if (!ingredient.attributes.isSelected) {
                            val ingredientObject = JsonObject()
                            ingredientObject.addProperty("id", ingredient.id)
                            ingredientObject.addProperty(
                                "price", if (menuItem.isPromo) 0 else String.format(
                                    Locale.ENGLISH, "%.2f", ingredient.attributes.price
                                ).toDouble()
                            )
                            ingredientObject.addProperty("quantity", ingredient.attributes.quantity)
                            ingredients.add(ingredientObject)
                        }
                    }
                    item.add("ingredients", ingredients)
                    item.add("modifiers", modifiers)
                    items.add(item)
                }
                orderBody.add("items", items)
                digitalCall = CouponsApiCall.getInstance()
                    .getCampaignsCancelableAll(orderBody, object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            try {
                                val response = JSONObject(o.toString())
                                val digitalCouponsArray: Array<Campaign> = GsonHelper.gsonFinal().fromJson(
                                    response.getJSONArray("data").toString(),
                                    object : TypeToken<Array<Campaign>>() {}.type
                                )
                                if (response.has("included")) {
                                    val promotions: Array<Promotion> = GsonHelper.gsonFinal().fromJson(
                                        response.getJSONArray("included").toString(),
                                        object : TypeToken<Array<Promotion>>() {}.type
                                    )
                                    val promotionsHashMap = HashMap<String, Promotion>()
                                    for (promotion in promotions) {
                                        promotionsHashMap[promotion.id] = promotion
                                    }
                                    for (digitalCoupon in digitalCouponsArray) {
                                        digitalCoupon.promotion =
                                            promotionsHashMap[digitalCoupon.attributes.promotionId]
                                    }
                                    digitalCoupons.clear()
                                    digitalCoupons.addAll(digitalCouponsArray)
                                }
                                if (digitalCoupons.size > 0) {
                                    view.showPromotions(digitalCoupons)
                                } else {
                                    view.showPromotions(ArrayList())
                                }
                                allPromotionsList = digitalCoupons
                            } catch (ex: JSONException) {
                                ex.printStackTrace()
                                view.showPromotions(ArrayList())
                            }
                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o)
                            view.showPromotions(ArrayList())
                        }
                    })

            }
        }
    }

    private fun getPromotionsWith(orderType: OrderType) {
        val category = when (orderType) {
            OrderType.NONE -> ""
            OrderType.PICKUP -> "pickup"
            OrderType.DINE_IN -> "eat-in"
            OrderType.DELIVERY -> "delivery"
            OrderType.CURBSIDE -> "curbside"
            OrderType.DRIVE_THRU -> "drive-thru"
        }


        allPromotionsList?.let { promos ->
            val filteredList = arrayListOf<Campaign>()
            if (promos.isNotEmpty() && category.isNotEmpty()) {
                promos.filter { promo ->
                    promo.promotion.attributes.rules.isNotEmpty() && promo.promotion.attributes.rules.find { rule ->
                        rule.ruleKey.equals(
                            "order-type",
                            true
                        ) && rule.text.split(Pattern.compile(","), 0).contains(category)
                    } != null
                }.let {
                    if (it.isNotEmpty()) {
                        filteredList.addAll(it)
                    }
                }
                promos.forEach {
                    if (it.promotion.attributes.rules.isEmpty()) {
                        filteredList.add(it)
                    } else if (it.promotion.attributes.rules.find { rules -> rules.ruleKey.equals("order-type") } == null) {
                        filteredList.add(it)
                    }
                }
                view.showPromotions(filteredList)
            } else {
                view.showPromotions(arrayListOf())
            }
        }
    }

    override fun getPromotionCategoryList() {
        val cacheUtil = CacheUtil.getInstance()
        GlobalScope.launch(contextProvider.IO) {
            homeRepository.getOrderTypes { res ->
                res.data?.cast<Set<OrderType>> { orderTypes ->
                    val promocateglist = mutableListOf<PromotionCategory>()
                    promocateglist.add(
                        PromotionCategory(
                            cacheUtil.getTranslations(
                                Text.ALL_LABEL, "All"
                            ), true, OrderType.NONE
                        )
                    )
                    orderTypes.find { type -> type == OrderType.DELIVERY }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.DELIVERY, "Delivery"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.PICKUP }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.PICKUP, "Pickup"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.DINE_IN }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.DINE_IN, "Dine In"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.CURBSIDE }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.CURBSIDE_PICKUP, "Curbside"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.DRIVE_THRU }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.DRIVE_THRU, "Drive Thru"
                                ),
                                false,
                                it
                            )
                        )
                    }
                    view.showPromotionsCategory(promocateglist)
                }
            }
        }
    }

    override fun selectedCategory(category: PromotionCategory) {
        category.isSelected = true
        view.updatePromoCategoryList(category)
        if (category.orderType == OrderType.NONE) {
            allPromotionsList?.let {
                view.showPromotions(ArrayList(it))
            } ?: run {
                view.showPromotions(arrayListOf())
            }
        } else {
            getPromotionsWith(category.orderType)
        }
    }
}