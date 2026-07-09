package com.herfy.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.herfy.database.AppDatabase
import com.herfy.database.entities.MenuCategoryEntity.Companion.toDomain
import com.herfy.database.entities.MenuItemEntity
import com.herfy.model.HomeResponse
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CacheUtilHelper.Companion.get
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CmsApiCall
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall
import com.skylinedynamics.solosdk.api.calls.RewardApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Promotion
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.Locale
import javax.inject.Inject


interface HomeRepository {
    suspend fun getSliders(sliderId: String, response: (HomeResponse) -> Unit)

    suspend fun getGuestDigitalCoupons(orderType: OrderType, response: (HomeResponse) -> Unit)

    suspend fun getSliderId(): String

    suspend fun getGetOrderType(): OrderType

    suspend fun getMenuCategories(response: (HomeResponse) -> Unit)

    suspend fun getMenuItemBy(categoryId: String, response: (HomeResponse) -> Unit)
    fun getOrderStore(): Store?

    suspend fun getMenuItemWith(menuItemId: String, response: (HomeResponse) -> Unit)

    suspend fun getCustomerLoyalty(id: String, response: (HomeResponse) -> Unit)

    suspend fun getCampaignCancellableAll(response: (HomeResponse) -> Unit)

    suspend fun getOrderTypes(response: (HomeResponse) -> Unit)
    suspend fun getLoyaltyTiers(response: (HomeResponse) -> Unit)
    suspend fun getLastOrder(orderId: String, response: (HomeResponse) -> Unit)
    suspend fun getCustomerOrder(customerId: String, response: (HomeResponse) -> Unit)
    suspend fun getMenuCartItems(menuItems: (ArrayList<MenuItem>) -> Unit)

    suspend fun getLoyaltyIframeToken(customerId: String, response: (HomeResponse) -> Unit)
}

class HomeRepositoryImpl @Inject constructor(
    val appDatabase: AppDatabase, val cacheUtilHelper: CacheUtilHelper
) : HomeRepository {
    override suspend fun getSliders(sliderId: String, response: (HomeResponse) -> Unit) {
        CmsApiCall.getInstance().getSlider(sliderId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                val slides = arrayListOf<Slide>()
                try {
                    val `object` = JSONObject(o.toString())
                    val dataArray = `object`.getJSONArray("data")
                    for (i in 0 until dataArray.length()) {
                        val slideObject = dataArray.getJSONObject(i)
                        val slide = Slide.parse(slideObject)
                        slides.add(slide)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                response(HomeResponse(true, slides, ""))
            }

            override fun onError(o: Any) {
                response(HomeResponse(false, null, o.toString()))
            }
        })
    }

    override suspend fun getGuestDigitalCoupons(
        orderType: OrderType, response: (HomeResponse) -> Unit
    ) {
        CouponsApiCall.getInstance().getGuestCampaigns(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {

                    val `object` = JSONObject(o.toString())

                    val digitalCoupons: ArrayList<Campaign> = GsonHelper.gsonFinal().fromJson(
                        `object`.getJSONArray("data").toString(),
                        object : TypeToken<ArrayList<Campaign>>() {}.type
                    )
                    if (`object`.has("included")) {
                        val promotions = GsonHelper.gsonFinal().fromJson<ArrayList<Promotion>>(
                            `object`.getJSONArray("included").toString(),
                            object : TypeToken<ArrayList<Promotion>>() {}.type
                        )
                        val promotionsHashMap = HashMap<String, Promotion>()
                        for (promotion in promotions) {
                            promotionsHashMap[promotion.id] = promotion
                        }
                        for (digitalCoupon in digitalCoupons) {
                            digitalCoupon.promotion =
                                promotionsHashMap[digitalCoupon.attributes.promotionId]
                        }
                        val filteredDigitalCoupons: MutableList<Campaign> = ArrayList()
                        try {
                            for (digitalCoupon in digitalCoupons) {
                                if (digitalCoupon.promotion != null) {
                                    if (digitalCoupon.promotion.attributes != null) {
                                        if (digitalCoupon.promotion.attributes.type.equals(
                                                "delivery", ignoreCase = true
                                            )
                                        ) {
                                            if (orderType === OrderType.DELIVERY) {
                                                filteredDigitalCoupons.add(digitalCoupon)
                                            }
                                        } else {
                                            filteredDigitalCoupons.add(digitalCoupon)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        digitalCoupons.clear()
                        digitalCoupons.addAll(filteredDigitalCoupons)
                    }
                    response(HomeResponse(true, digitalCoupons, ""))
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                    onError("")
                }
            }

            override fun onError(o: Any) {
                response(HomeResponse(false, null, o.toString()))
            }
        })
    }

    override suspend fun getCustomerLoyalty(id: String, response: (HomeResponse) -> Unit) {
        CustomersApiCall.getInstance().getLoyaltyCustomerV2(id, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val data = `object`.getJSONObject("data")
                    val loyalty =
                        GsonHelper.gsonFinal().fromJson(data.toString(), LoyaltyV2::class.java)
                    response(HomeResponse(true, loyalty, ""))
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError(e)
                }
            }

            override fun onError(o: Any) {
                response(HomeResponse(false, null, o.toString()))
            }
        })
    }

    override suspend fun getSliderId(): String {
        val pref = cacheUtilHelper.pref()
        return pref[CacheUtilHelper.SLIDER_ID, ""] ?: ""
    }

    override suspend fun getGetOrderType(): OrderType {
        val pref = cacheUtilHelper.pref()
        return when (pref[CacheUtilHelper.ORDER_TYPE, 0]) {
            1 -> OrderType.PICKUP;
            2 -> OrderType.DINE_IN;
            3 -> OrderType.DELIVERY;
            4 -> OrderType.CURBSIDE;
            else -> OrderType.NONE;
        }
    }

    override suspend fun getMenuCategories(response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuCategoryDbs =
                appDatabase.menuCategoryEntityDao().getAllByApplication(application.id)
            val menuCategoryList = arrayListOf<MenuCategory>()
            menuCategoryDbs.forEach {
                menuCategoryList.add(it.toDomain())
            }
            response(HomeResponse(true, menuCategoryList, ""))
        }
    }

    override suspend fun getMenuItemBy(categoryId: String, response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuItemDb =
                appDatabase.menuItemEntityDao().findAllByMenuCategoryId(application.id, categoryId)
            response(HomeResponse(true, menuItemDb, ""))
        }
    }

    override fun getOrderStore(): Store? {
        return cacheUtilHelper.getOrderStore()
    }

    override suspend fun getMenuItemWith(menuItemId: String, response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuItemEntity: MenuItemEntity? =
                appDatabase.menuItemEntityDao().findById(application.id, menuItemId)
            response(HomeResponse(true, menuItemEntity, ""))
        }
    }

    override suspend fun getCampaignCancellableAll(
        response: (HomeResponse) -> Unit
    ) {
        createOrderBody(cacheUtilHelper.getOrderType()) { params ->
            CouponsApiCall.getInstance()
                .getCampaignsCancelableAll(params, object : ApiRequestListener {
                    override fun onSuccess(o: Any?) {
                        val `object` = JSONObject(o.toString())
                        val digitalCoupons = GsonHelper.gsonFinal().fromJson<List<Campaign>>(
                            `object`.getJSONArray("data").toString(),
                            object : TypeToken<List<Campaign?>?>() {}.type
                        )
                        digitalCoupons?.let {
                            if (`object`.has("included")) {
                                val promotions =
                                    GsonHelper.gsonFinal().fromJson<java.util.ArrayList<Promotion>>(
                                        `object`.getJSONArray("included").toString(),
                                        object :
                                            TypeToken<java.util.ArrayList<Promotion?>?>() {}.type
                                    )
                                val promotionsHashMap = java.util.HashMap<String, Promotion>()
                                for (promotion in promotions) {
                                    promotionsHashMap[promotion.id] = promotion
                                }
                                for (digitalCoupon in digitalCoupons) {
                                    digitalCoupon.promotion =
                                        promotionsHashMap[digitalCoupon.attributes.promotionId]
                                }
                            }
                            response(HomeResponse(true, digitalCoupons))
                        }
                    }

                    override fun onError(error: Any?) {
                        response(HomeResponse(false, error))
                    }
                })
        }

    }

    override suspend fun getOrderTypes(response: (HomeResponse) -> Unit) {
        response(
            HomeResponse(
                true, cacheUtilHelper.getApplication()?.attributes?.getOrderTypes(), ""
            )
        )
    }

    private suspend fun createOrderBody(category: OrderType, body: (JsonObject) -> Unit) {
        val authUtil = AuthUtil.getInstance()
        val cacheUtil = CacheUtil.getInstance()

        val customer = authUtil.customer
        val concept = cacheUtil.concept
        val store = cacheUtil.orderStore
        cacheUtilHelper.getCartItems { menuItems ->

            val subtotal: Double = getMenuItemsTotalPrice(menuItems)
            var deliveryCharge = 0.0

            try {
                val minimumOrderAmountFreeDelivery = concept.minimumOrderAmountFreeDelivery
                if (minimumOrderAmountFreeDelivery != 0.0 && subtotal >= minimumOrderAmountFreeDelivery) {
                    deliveryCharge = concept.specialDeliveryPrice
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val orderBody = JsonObject()
            if (authUtil.isSignedIn) {
                orderBody.addProperty("customer", customer.id)
            }

            orderBody.addProperty("source", "Android")
            orderBody.addProperty("notes", cacheUtil.cartNotes)
            orderBody.addProperty(
                "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
            )

            var discount = 0.0

            val couponCode = cacheUtil.couponCode
            if (couponCode != null && !couponCode.isEmpty()) {
                orderBody.addProperty("coupon-code", couponCode)
            }

            if (cacheUtil.campaign != null && cacheUtil.campaignPromotion != null) {
                orderBody.addProperty("campaign-id", cacheUtil.campaign.id)
            }

            var discountedSubtotal = subtotal - discount
            if (discountedSubtotal < 0) {
                discountedSubtotal = 0.0
                discount = subtotal
            }

            orderBody.addProperty(
                "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
            )
            orderBody.addProperty("type", "")
            when (category) {
                OrderType.PICKUP -> {
                    if (CacheUtil.getInstance().application.attributes.getOrderTypes().size == 3 || CacheUtil.getInstance().application.attributes.getOrderTypes()
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

                OrderType.DRIVE_THRU -> {
                    if (CacheUtil.getInstance().application.attributes.getOrderTypes().size == 3 || CacheUtil.getInstance().application.attributes.getOrderTypes()
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
                    orderBody.addProperty("type", "eat-in")
                    if (store != null) {
                        orderBody.addProperty("location", store.id)
                    }
                }

                OrderType.DELIVERY -> {
                    deliveryCharge = concept.deliveryCharge
                    orderBody.addProperty("type", "deliver")
                    val address = cacheUtil.orderAddress
                    if (address != null) {
                        orderBody.addProperty("address", address.id)
                    }
                }

                OrderType.CURBSIDE -> {
                    if (cacheUtil.orderStore != null) {
                        orderBody.addProperty("location", cacheUtil.orderStore.id)
                    }
                    if (cacheUtil.curbsideCar != null) {
                        orderBody.addProperty("type", "curbside")
                        orderBody.addProperty("car-id", cacheUtil.curbsideCar.id)
                    }
                }

                OrderType.NONE -> {}
            }

            val vatAmount = 0.0
            var total =
                String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    discountedSubtotal + deliveryCharge + vatAmount
                )
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
                    String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId))
                        .toDouble()
                )
                item.addProperty("quantity", menuItem.attributes.quantity)
                item.addProperty("is-reorder", menuItem.attributes.isReorder)
                item.addProperty("is-promotion", menuItem.isPromo)
                item.addProperty("notes", menuItem.attributes.notes)
                var comboIndex = 0
                var trueIndex = 0
                for (size in menuItem.sizes.modifierItems) {
                    if (size.attributes.isSelected) {
                        val modifier = JsonObject()
                        modifier.addProperty("id", size.id)
                        modifier.addProperty(
                            "price",
                            String.format(Locale.ENGLISH, "%.2f", size.attributes.price).toDouble()
                        )
                        modifier.addProperty("quantity", size.attributes.quantity)
                        modifiers.add(modifier)
                        trueIndex = comboIndex
                    }
                    comboIndex++
                }
                for (drink in menuItem.drinks.modifierItems) {
                    if (drink.attributes.isSelected) {
                        val modifier = JsonObject()
                        modifier.addProperty("id", drink.id)
                        modifier.addProperty(
                            "price",
                            String.format(Locale.ENGLISH, "%.2f", drink.attributes.price).toDouble()
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
                            String.format(Locale.ENGLISH, "%.2f", fries.attributes.price).toDouble()
                        )
                        modifier.addProperty("quantity", fries.attributes.quantity)
                        modifiers.add(modifier)
                    }
                }
                for (modifierGroup in menuItem.modifierGroups) {
                    val visibilityConfig = modifierGroup.attributes.visibilityConfig
                    if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                            "hidden", ignoreCase = true
                        )
                    ) {
                        for (modifierItem in modifierGroup.modifierItems) {
                            if (modifierItem.attributes.isSelected) {
                                val modifier = JsonObject()
                                modifier.addProperty("id", modifierItem.id)
                                modifier.addProperty(
                                    "price",
                                    String.format(
                                        Locale.ENGLISH,
                                        "%.2f",
                                        modifierItem.attributes.price
                                    )
                                        .toDouble()
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
                        String.format(Locale.ENGLISH, "%.2f", modifierItem.attributes.price)
                            .toDouble()
                    )
                    modifier.addProperty("quantity", modifierItem.attributes.quantity)
                    modifiers.add(modifier)
                }
                for (ingredient in menuItem.ingredients) {
                    if (!ingredient.attributes.isSelected) {
                        val ingredientObject = JsonObject()
                        ingredientObject.addProperty("id", ingredient.id)
                        ingredientObject.addProperty(
                            "price",
                            String.format(Locale.ENGLISH, "%.2f", ingredient.attributes.price)
                                .toDouble()
                        )
                        ingredientObject.addProperty("quantity", ingredient.attributes.quantity)
                        ingredients.add(ingredientObject)
                    }
                }
                item.add("ingredients", ingredients)
                item.add("modifiers", modifiers)
                try {
                    if (menuItem.isCombo) {
                        val comboItems = JsonArray()
                        for (components in menuItem.combo[trueIndex].components) {
                            for (compoItem in components.items) {
                                if (compoItem.selected) {
                                    val comboItemObject = JsonObject()
                                    comboItemObject.addProperty("id", compoItem.id)
                                    comboItemObject.addProperty(
                                        "price", String.format(
                                            Locale.ENGLISH, "%.2f", compoItem.price
                                        ).toDouble()
                                    )
                                    comboItemObject.addProperty("quantity", compoItem.quantity)
                                    comboItemObject.add("ingredients", JsonArray())
                                    val modifierCombos = JsonArray()
                                    if (!compoItem.modifierGroups.isEmpty()) {
                                        for (modifierGroup in compoItem.modifierGroups) {
                                            for (i in modifierGroup.modifierItems.indices) {
                                                if (modifierGroup.modifierItems[i].attributes.isSelected) {
                                                    val comoModItemObject = JsonObject()
                                                    comoModItemObject.addProperty(
                                                        "id",
                                                        modifierGroup.modifierItems[i].id
                                                    )
                                                    comoModItemObject.addProperty(
                                                        "price", String.format(
                                                            Locale.ENGLISH,
                                                            "%.2f",
                                                            modifierGroup.modifierItems[i].attributes.price
                                                        ).toDouble()
                                                    )
                                                    comoModItemObject.addProperty(
                                                        "quantity",
                                                        modifierGroup.modifierItems[i].attributes.quantity
                                                    )
                                                    modifierCombos.add(comoModItemObject)
                                                }
                                            }
                                        }
                                    }
                                    comboItemObject.add("modifiers", modifierCombos)
                                    comboItems.add(comboItemObject)
                                }
                            }
                        }
                        item.add("combo-items", comboItems)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                items.add(item)
            }
            orderBody.add("items", items)
            body(orderBody)
        }
    }

    private fun getMenuItemsTotalPrice(menuItems: ArrayList<MenuItem>?): Double {
        menuItems?.let {
            var subtotalPrice = 0.0
            for (menuItem in it) {
                subtotalPrice += getMenuItemTotalPrice(menuItem) * menuItem.attributes.quantity
            }
            return subtotalPrice
        } ?: run {
            return 0.0
        }
    }

    fun getMenuItemTotalPrice(item: MenuItem): Double {
        var orderStoreId: String? = null
        var counter = 0
        var comboIndex = 0
        val store = CacheUtil.getInstance().orderStore
        if (store != null) {
            orderStoreId = store.id
        }
        var totalPrice = item.getPrice(orderStoreId)
        if (BuildUtil.isEkfc() && item.total != 0.0) {
            totalPrice = item.total
        } else {
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
                if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                        "hidden", ignoreCase = true
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
                                    selectedFreeModifier =
                                        modifierGroup.attributes.freeModifiersCount
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
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return totalPrice
    }

    override suspend fun getLoyaltyTiers(response: (HomeResponse) -> Unit) {
        CustomersApiCall.getInstance().getLoyaltyTiersV2(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val tiersArray = `object`.getJSONArray("data")
                    val type = object : TypeToken<List<LoyaltyTierV2?>?>() {}.type
                    val tiers =
                        GsonHelper.gsonFinal()
                            .fromJson<List<LoyaltyTierV2>>(tiersArray.toString(), type)
                    // TODO: Should set Tier as Comparable in SDK
                    Collections.sort(
                        tiers,
                        java.util.Comparator { o1: LoyaltyTierV2, o2: LoyaltyTierV2 -> o1.startingPoints - o2.startingPoints } as java.util.Comparator<LoyaltyTierV2>)
                    response(HomeResponse(true, tiers, ""))
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
            }

            override fun onError(o: Any) {
                response(HomeResponse(false, null, o.toString()))
            }
        })
    }

    override suspend fun getLastOrder(orderId: String, response: (HomeResponse) -> Unit) {
        OrdersApiCall.getInstance().getOrder(orderId, object : ApiRequestListener {
            override fun onSuccess(obj: Any?) {
                if (obj is JSONObject) {
                    try {
                        val jsonObject = obj as JSONObject
                        val data = jsonObject.getJSONObject("data")
                        val order = OrderDetails.parse(data)
                        response(HomeResponse(true, order, ""))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onError(error: Any?) {
                response(HomeResponse(false, null, error.toString()))
            }

        })
    }

    override suspend fun getCustomerOrder(customerId: String, response: (HomeResponse) -> Unit) {
        CustomersApiCall.getInstance().getOrders(customerId, "1", "1", object : ApiRequestListener {
            override fun onSuccess(obj: Any?) {
                if (obj is JSONObject) {
                    try {
                        val jsonObject = obj as JSONObject
                        val array = jsonObject.optJSONArray("data")
                        if (array != null && array.length() > 0) {
                            val order = OrderHistory.parse(array.getJSONObject(0))
                            CacheUtil.getInstance().lastOrderId = order.id
                            response(HomeResponse(true, order.id, ""))
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onError(error: Any?) {
                response(HomeResponse(false, null, error.toString()))
            }
        })
    }

    override suspend fun getMenuCartItems(menuItems: (ArrayList<MenuItem>) -> Unit) {
        cacheUtilHelper.getCartItems {
            menuItems(it)
        }
    }

    override suspend fun getLoyaltyIframeToken(
        customerId: String,
        response: (HomeResponse) -> Unit
    ) {
        RewardApiCall.getInstance().getLoyaltyIframeToken(customerId, object : ApiRequestListener {
            override fun onSuccess(obj: Any?) {
                if (obj is JSONObject) {
                    try {
                        val data = obj.getJSONObject("data")
                        if (data != null) {
                            val token = data.getString("token")
                            if (token != null) {
                                CacheUtil.getInstance().loyaltyIframeToken = token
                                response(HomeResponse(true, data, ""))
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onError(error: Any?) {
                response(HomeResponse(false, null, error.toString()))
            }
        })
    }
}