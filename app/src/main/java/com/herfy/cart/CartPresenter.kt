package com.herfy.cart

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.herfy.BuildConfig
import com.herfy.cart.views.CartMenuItemViewHolder
import com.herfy.database.AppDatabase
import com.herfy.database.entities.MenuCategoryEntity.Companion.toDomain
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.model.CampaignToAdd
import com.herfy.model.CouponResult
import com.herfy.model.CouponToAdd
import com.herfy.upsell.UpsellViewHolder
import com.herfy.util.ApiLogLevel
import com.herfy.util.ApiLogUtil.Companion.sendMessageLog
import com.herfy.util.AppLogger
import com.herfy.util.StringUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.handler.ErrorHandler
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CoroutineContextProvider
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.TimeUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall
import com.skylinedynamics.solosdk.api.calls.LocationApiCall
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.Allergen
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn
import com.skylinedynamics.solosdk.api.models.objects.Application
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Concept
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard
import com.skylinedynamics.solosdk.api.models.objects.Ingredient
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn
import com.skylinedynamics.solosdk.api.models.objects.Integration
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn
import com.skylinedynamics.solosdk.api.models.objects.OrderData
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Promotion
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.api.models.objects.Translated
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody
import com.skylinedynamics.solosdk.util.GsonHelper
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

@DelicateCoroutinesApi
@OptIn(DelicateCoroutinesApi::class)
class CartPresenter @Inject constructor(
    private val cartView: CartContract.View, private val cacheUtilHelper: CacheUtilHelper
) : CartContract.Presenter {
    private var menuItems = ArrayList<MenuItem>()
    private val menuItemsCount = 0
    private val disabledMenuItems = HashMap<String, MenuItem>()
    private val unavailableMenuItems = HashMap<String, MenuItem>()
    private val digitalCouponsResponse = ""
    private var digitalCoupons: MutableList<Campaign> = ArrayList()
    private var digitalCall: Call<ResponseBody>? = null
    private val notes = HashMap<Int, String>()
    private var validatePromotionDisposable: Disposable? = null
    private var validateDigitalCouponDisposable: Disposable? = null
    private var validatedPromoItemPrice = 0.0
    private var validatedPromoItemQuantity = 0
    private var validatedOrderData: OrderData? = null
    private var upsells: MutableList<MenuItem> = ArrayList()
    private var currUpsells = arrayListOf<MenuItem>()

    val contextPool = CoroutineContextProvider()
    override fun start() {
        cartView.setupViews()
        cartView.setupFonts()
        cartView.setupTranslations()
    }

    override fun getIntegrations() {
        if (AuthUtil.getInstance().isSignedIn) {
            val paymentGateway = CacheUtil.getInstance().application.attributes.paymentGateway
            if (paymentGateway.equals(
                    "checkout", ignoreCase = true
                ) || paymentGateway.equals("foodics-online", ignoreCase = true)
            ) {
                IntegrationsApiCall.getInstance()
                    .getKeys(paymentGateway, object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            try {
                                val `object` = JSONObject(o.toString())
                                if (`object`.has("data") && !`object`.isNull("data")) {
                                    val integration = GsonHelper.gsonFinal().fromJson(
                                        `object`.getJSONObject("data").toString(),
                                        Integration::class.java
                                    )
                                    CacheUtil.getInstance().integration = integration
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o.toString())
                        }
                    })
            }
        }
    }

    override fun getApplication(showUpsell: Boolean) {
        ApplicationsApiCall.getInstance().getApplication(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val jsonObject = JSONObject(o.toString())
                    val data = jsonObject.getJSONObject("data")
                    val application = GsonHelper.gsonFinal().fromJson<Application>(
                        data.toString(), object : TypeToken<Application?>() {}.type
                    )
                    CacheUtil.getInstance().application = application
                    CacheUtil.getInstance().colorMain = application.attributes.theme.primaryColor
                    CacheUtil.getInstance().sliderId = application.attributes.sliderId.toString()
                    CacheUtil.getInstance().menuId = application.attributes.defaultMenuId.toString()
                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                        val included = jsonObject.getJSONArray("included")
                        if (included.length() > 0) {
                            for (i in 0 until included.length()) {
                                if (included.getJSONObject(i).optString("type")
                                        .equals("concept", ignoreCase = true)
                                ) {
                                    var concept = Concept()
                                    concept = concept.parse(included.getJSONObject(i))
                                    CacheUtil.getInstance().concept = concept
                                } else if (included.getJSONObject(i).optString("type")
                                        .equals("popup", ignoreCase = true)
                                ) {
                                    CacheUtil.getInstance().setPopup(included.getJSONObject(i))
                                }
                            }
                        }
                    }
                    cartView.showPayment()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    cartView.showPayment()
                }
            }

            override fun onError(o: Any) {
                cartView.showPayment()
            }
        })
    }

    override fun getConcept() {
        ApplicationsApiCall.getInstance().getApplication(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val jsonObject = JSONObject(o.toString())
                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                        val included = jsonObject.getJSONArray("included")
                        if (included.length() > 0) {
                            for (i in 0 until included.length()) {
                                if (included.getJSONObject(i).optString("type")
                                        .equals("concept", ignoreCase = true)
                                ) {
                                    var concept = Concept()
                                    concept = concept.parse(included.getJSONObject(i))
                                    CacheUtil.getInstance().concept = concept
                                }
                            }
                            showCoupon()
                        } else {
                            onError("")
                        }
                    } else {
                        onError("")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError(e)
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                showCoupon()
            }
        })

        /*ConceptsService.INSTANCE.getConcept(CacheUtil.getInstance().getConceptKey(), new Callback<SoloResponse<SoloResponseData<Concept>>>() {
            @Override
            public void onResponse(Call<SoloResponse<SoloResponseData<Concept>>> call, Response<SoloResponse<SoloResponseData<Concept>>> response) {
                if (response.isSuccessful()) {
                    Concept concept = response.body().getData().getAttributes();
                    concept.setId(response.body().getData().getId());
                    CacheUtil.getInstance().setConcept(concept);
                    getFinancials();
                }
                showCoupon();
            }

            @Override
            public void onFailure(Call<SoloResponse<SoloResponseData<Concept>>> call, Throwable t) {
                AppLogger.printRetrofitError(t);
                showCoupon();
            }
        });*/
    }

    override fun getCampaignList(): List<Campaign>? {
        return null
    }


    override fun getMenuItems() {
        GlobalScope.launch(contextPool.IO) {
            menuItems.clear()
            cacheUtilHelper.getCartItems {
                menuItems = it
                if (CacheUtil.getInstance().concept.isCartItemsNotesEnabled) {
                    notes.clear()
                    for (i in menuItems.indices) {
                        notes[i] = menuItems[i].attributes.notes
                    }
                }
                GlobalScope.launch(contextPool.Main) {
                    cartView.showMenuItems(menuItems, false, false)
                }

            }
        }
    }

    override fun fetchCustomerCards() {
        if (AuthUtil.getInstance().isSignedIn) {
            CustomersApiCall.getInstance()
                .getSavedCards(AuthUtil.getInstance().customer.id, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val jsonObject = JSONObject(o.toString())
                            val cards = GsonHelper.gsonFinal().fromJson<List<CustomerCard>>(
                                jsonObject.getJSONArray("data").toString(),
                                object : TypeToken<List<CustomerCard?>?>() {}.type
                            )
                            CacheUtil.getInstance().setCustomerCards(Gson().toJson(cards))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(o: Any) {}
                })
        }
    }

    override fun validateOrder(
        isCouponValidate: Boolean, checkout: Boolean, scheduledTime: String?
    ) {
        val customer = AuthUtil.getInstance().customer
        val concept = CacheUtil.getInstance().concept
        GlobalScope.launch(contextPool.IO) {
            cacheUtilHelper.getCartItems {
                menuItems = it
                val subtotal = menuItemsTotalPrice
                if (!checkout) {
                    cartView.validatingOrder(subtotal)
                }
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
                if (checkout) {
                    if (scheduledTime != null) {
                        orderBody.addProperty("scheduled-time", scheduledTime)
                    }
                }
                val mukafaaTransactionId = CacheUtil.getInstance().mukafaaTransactionId
                if (mukafaaTransactionId != null && !mukafaaTransactionId.isEmpty()) {
                    val loyalty = JsonObject()
                    loyalty.addProperty("provider", Values.MUKAFAA_PROVIDER)
                    loyalty.addProperty("transaction-id", mukafaaTransactionId)
                    orderBody.add("loyalty", loyalty)
                }

                orderBody.addProperty("source", "Android")
                orderBody.addProperty("notes", CacheUtil.getInstance().cartNotes)
                orderBody.addProperty(
                    "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
                )
                var discount = 0.0
                val promoPrice = 0.0
                val followDelivery = false
                val discountZero = false
                val couponCode = CacheUtil.getInstance().couponCode
                if (couponCode != null && !couponCode.isEmpty()) {
                    orderBody.addProperty("coupon-code", couponCode)
                }
                if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotion != null) {
                    orderBody.addProperty("campaign-id", CacheUtil.getInstance().campaign.id)
                }
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
                        if (CacheUtil.getInstance().orderStore != null) {
                            orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
                        }
                    }

                    OrderType.DRIVE_THRU -> {
                        deliveryCharge = 0.0
                        orderBody.addProperty("type", "drive-thru")
                        if (CacheUtil.getInstance().orderStore != null) {
                            orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
                        }
                    }

                    OrderType.DINE_IN -> {
                        deliveryCharge = 0.0
                        orderBody.addProperty("type", "eat-in")
                        if (CacheUtil.getInstance().orderStore != null) {
                            orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
                        }
                    }

                    OrderType.DELIVERY -> {
                        orderBody.addProperty("type", "deliver")
                        //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                        val orderStoreId = CacheUtil.getOrderStoreId()
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
                }
                var discountedSubtotal = subtotal - discount
                if (discountedSubtotal < 0) {
                    discountedSubtotal = 0.0
                    discount = subtotal
                }
                if (followDelivery) {
                    deliveryCharge = 0.0
                }
                orderBody.addProperty(
                    "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
                )
                val vatAmount = 0.0

                var total = String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    discountedSubtotal + deliveryCharge + vatAmount + promoPrice
                ).toDouble()
                if (total < 0) {
                    total = 0.0
                }
                orderBody.addProperty("total", total)
                val items = JsonArray()
                val orderStoreId = CacheUtil.getOrderStoreId()
                for (menuItem in menuItems) {
                    if (!menuItem.isPromo) {
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
                                    String.format(Locale.ENGLISH, "%.2f", size.attributes.price)
                                        .toDouble()
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
                                    String.format(Locale.ENGLISH, "%.2f", drink.attributes.price)
                                        .toDouble()
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
                                    String.format(Locale.ENGLISH, "%.2f", fries.attributes.price)
                                        .toDouble()
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
                                var selectedFreeModifier = 0
                                for (modifierItem in modifierGroup.modifierItems) {
                                    if (modifierItem.attributes.isSelected) {
                                        var freeModifier: JsonObject? = null
                                        var modifier: JsonObject? = null
                                        if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                            val totalSelectedFreeModifier =
                                                selectedFreeModifier + modifierItem.attributes.quantity
                                            if (totalSelectedFreeModifier <= modifierGroup.attributes.freeModifiersCount) {
                                                selectedFreeModifier += modifierItem.attributes.quantity
                                                freeModifier = JsonObject()
                                                freeModifier.addProperty("id", modifierItem.id)
                                                freeModifier.addProperty("price", 0)
                                                freeModifier.addProperty(
                                                    "quantity", modifierItem.attributes.quantity
                                                )
                                            } else {
                                                selectedFreeModifier =
                                                    modifierGroup.attributes.freeModifiersCount
                                                freeModifier = JsonObject()
                                                freeModifier.addProperty("id", modifierItem.id)
                                                freeModifier.addProperty("price", 0)
                                                freeModifier.addProperty(
                                                    "quantity",
                                                    modifierItem.attributes.quantity - (totalSelectedFreeModifier - modifierGroup.attributes.freeModifiersCount)
                                                )
                                                modifier = JsonObject()
                                                modifier.addProperty("id", modifierItem.id)
                                                modifier.addProperty(
                                                    "price", String.format(
                                                        Locale.ENGLISH,
                                                        "%.2f",
                                                        modifierItem.attributes.price
                                                    ).toDouble()
                                                )
                                                modifier.addProperty(
                                                    "quantity",
                                                    totalSelectedFreeModifier - modifierGroup.attributes.freeModifiersCount
                                                )
                                            }
                                        } else {
                                            modifier = JsonObject()
                                            modifier.addProperty("id", modifierItem.id)
                                            modifier.addProperty(
                                                "price", String.format(
                                                    Locale.ENGLISH,
                                                    "%.2f",
                                                    modifierItem.attributes.price
                                                ).toDouble()
                                            )
                                            modifier.addProperty(
                                                "quantity", modifierItem.attributes.quantity
                                            )
                                        }
                                        if (freeModifier != null) {
                                            modifiers.add(freeModifier)
                                        }
                                        if (modifier != null) {
                                            modifiers.add(modifier)
                                        }
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
                                    "price", String.format(
                                        Locale.ENGLISH, "%.2f", ingredient.attributes.price
                                    ).toDouble()
                                )
                                ingredientObject.addProperty(
                                    "quantity", ingredient.attributes.quantity
                                )
                                ingredients.add(ingredientObject)
                            }
                        }
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
                                            comboItemObject.addProperty("quantity", 1)
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
                        item.add("ingredients", ingredients)
                        item.add("modifiers", modifiers)
                        items.add(item)
                    }
                }
                orderBody.add("items", items)

                // Cancel older calls
//        if (digitalCall != null && digitalCall.isExecuted()) digitalCall.cancel();
                if (validatePromotionDisposable != null) validatePromotionDisposable!!.dispose()
                val finalOrderStoreId = orderStoreId
                validatePromotionDisposable = validatePromotionObservable(
                    orderBody, checkout, isCouponValidate
                ).subscribe({ onNext: Any ->
                    var isCouponError = false
                    val isCouponPromoItem = false
                    try {
                        val jsonObject = onNext as JSONObject
                        val itemsArray = jsonObject.getJSONArray("items")
                        var hasChanged = false
                        for (i in 0 until itemsArray.length()) {
                            val itemObject = itemsArray.getJSONObject(i)
                            val item = GsonHelper.gsonFinal()
                                .fromJson(itemObject.toString(), MenuItem::class.java)
                            validatedPromoItemPrice = 0.0
                            validatedPromoItemQuantity = 0
                            for (menuItem in menuItems) {
                                if (item.isPromotion) {
                                    validatedPromoItemPrice = item.price
                                    validatedPromoItemQuantity = item.quantity
                                }
                                if (!item.isPromotion && menuItem.id == item.id) {
                                    if (BuildUtil.isEkfc() && (item.total != item.price || getMenuItemTotalPrice(
                                            menuItems[menuItems.indexOf(menuItem)], -1
                                        ) != item.total)
                                    ) {
                                        menuItems[menuItems.indexOf(menuItem)].total = item.total
                                    } else {
                                        menuItem.price = item.getPrice(null)
                                        menuItem.attributes.price = item.getPrice(null)
                                    }
                                    hasChanged = true
                                }
                            }
                        }
                        if (hasChanged) {
                            CacheUtil.getInstance().cart = menuItems
                            cartView.updateMenuItems(menuItems)
                        }
                        var mokafaaAmount = 0.0
                        if (jsonObject.has("loyalty")) {
                            val loyalty = jsonObject.getJSONObject("loyalty")
                            if (loyalty.has("amount")) {
                                mokafaaAmount = loyalty.getDouble("amount")
                                mukafaaTransactionAmount = loyalty.getDouble("amount")
                            }
                        }
                        var isInclusive = false
                        if (concept.vatType != null && concept.vatType.equals(
                                "inclusive", ignoreCase = true
                            )
                        ) {
                            isInclusive = concept.vatType.equals("inclusive", ignoreCase = true)
                        }
                        val hasError = jsonObject.has("error") && !jsonObject.isNull("error")
                        val hasPromotion = !hasError && CacheUtil.getInstance().hasPromotion()
                        val orderData = OrderData.parse(jsonObject)
                        cartView.setTax(
                            isInclusive,
                            StringUtil.getFormattedPrice(orderData.vatAmount),
                            orderData.vatAmount
                        )
                        if (orderData.discount > 0) {
                            cartView.setOriginalPrice(
                                hasPromotion, orderData.subtotal + orderData.deliverCharge
                            )
                        }
                        showPrice(hasPromotion, orderData, mokafaaAmount)
                        if (hasError) {
                            //if (isCouponValidate) {
                            isCouponError = true
                            CacheUtil.getInstance().couponCode = null
                            CacheUtil.getInstance().couponName = null
                            CacheUtil.getInstance().couponMenuItem = null
                            CacheUtil.getInstance().couponToAdd = null
                            cartView.removeCoupon()
                            digitalCoupons.clear()
                            CacheUtil.getInstance().digitalCoupons = ""
                            CacheUtil.getInstance().campaign = null
                            CacheUtil.getInstance().campaignPromotion = null
                            CacheUtil.getInstance().campaignPromotionMenuItem = null
                            CacheUtil.getInstance().campaignToAdd = null
                            cartView.showDigitalCoupon(null)
                            removeMukafaaRedemption()
                            //}
                            cartView.showPromoError(jsonObject.getString("error"))
                        } else {
                            if (CacheUtil.getInstance().couponCode != null && orderData.promotionName != null || orderData.promotionName == null && !orderData.promotionName.isEmpty()) {
                                if (isCouponValidate) {
                                    trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                }
                                CacheUtil.getInstance().couponName = orderData.promotionName
                                var isFreeItem = false
                                for (i in 0 until itemsArray.length()) {
                                    val itemObject = itemsArray.getJSONObject(i)
                                    val item = GsonHelper.gsonFinal().fromJson(
                                        itemObject.toString(), MenuItem::class.java
                                    )
                                    if (item.isPromotion) {
                                        isFreeItem = true
                                        if (isCouponValidate || CacheUtil.getInstance().couponMenuItem == null) {
                                            getCouponMenuItem(
                                                item.id, item.quantity, item.price, null, null
                                            )
                                        } else {
                                            val couponName = CacheUtil.getInstance().couponName
                                            if (couponName != null) {
                                                cartView.showCoupon(couponCode, couponName)
                                                cartView.addPromotionItem(CacheUtil.getInstance().couponMenuItem)
                                            }
                                        }
                                        break
                                    }
                                }
                                if (!isFreeItem) {
                                    showCoupon()
                                }
                            } else {
                                CacheUtil.getInstance().couponCode = null
                                CacheUtil.getInstance().couponName = null
                            }
                            if (checkout) {
                                cartView.showPayment()
                            } else {
                                cartView.dismissProgressDialog()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) { onError: Throwable? -> }
            }
        }
    }

    private fun showPrice(hasPromotion: Boolean, orderData: OrderData, mokafaaAmount: Double) {
        var promoItemPrice = 0.0
        var subtotalForDisplay =
            orderData.subtotal - validatedPromoItemPrice * validatedPromoItemQuantity
        var totalForDisplay = orderData.total - validatedPromoItemPrice * validatedPromoItemQuantity
        if (CacheUtil.getInstance().couponMenuItem != null) {
            promoItemPrice = getMenuItemTotalPrice(CacheUtil.getInstance().couponMenuItem, -1)
        } else if (CacheUtil.getInstance().campaignPromotionMenuItem != null) {
            promoItemPrice =
                getMenuItemTotalPrice(CacheUtil.getInstance().campaignPromotionMenuItem, -1)
        }
        subtotalForDisplay += promoItemPrice * validatedPromoItemQuantity
        totalForDisplay += promoItemPrice * validatedPromoItemQuantity
        cartView.setTotalPrice(
            hasPromotion,
            subtotalForDisplay,
            orderData.discountDisplay,
            totalForDisplay,
            CacheUtil.getInstance().orderType == OrderType.DELIVERY,
            orderData.deliverCharge,
            orderData.charges,
            mokafaaAmount
        )
    }

    // TODO: remove side effects
    fun validatePromotionObservable(
        orderBody: JsonObject?, checkout: Boolean, isCouponValidate: Boolean
    ): Observable<Any> {
        return Observable.create { subscriber: ObservableEmitter<Any> ->
            CouponsApiCall.getInstance().validatePromotion(orderBody, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    subscriber.onNext(o)
                    subscriber.onComplete()
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    cartView.dismissProgressDialog()
                    cartView.setTotalPrice(
                        CacheUtil.getInstance().hasPromotion(),
                        menuItemsTotalPrice,
                        0.0,
                        menuItemsTotalPrice,
                        CacheUtil.getInstance().orderType == OrderType.DELIVERY,
                        0.0,
                        ArrayList(),
                        0.0
                    )
                    if (checkout || isCouponValidate) {
                        if (o is JSONObject) {
                            try {
                                val `object` = o
                                if (`object`.has("error") && !`object`.isNull("error")) {
                                    cartView.showError(
                                        ErrorHandler.getErrorMessage(
                                            `object`, CacheUtil.getInstance().getTranslations(
                                                Text.AN_ERROR_OCCURRED
                                            ), null
                                        )
                                    )
                                } else if (`object`.has("messages") && !`object`.isNull("messages")) {
                                    val messages = `object`["messages"]
                                    if (messages is JSONObject) {
                                        val message = messages
                                        val errorList: MutableList<String> = ArrayList()
                                        val keys = message.keys()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            if (message[key] is JSONArray) {
                                                val array = message[key] as JSONArray
                                                val type =
                                                    object : TypeToken<List<String?>?>() {}.type
                                                val error = GsonHelper.gsonFinal()
                                                    .fromJson<List<String>>(array.toString(), type)
                                                errorList.addAll(error)
                                            }
                                        }
                                        cartView.showValidateOrderError(errorList)
                                    }
                                } else {
                                    cartView.showError(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED)
                                    )
                                }
                                sendLog(ApiLogLevel.ERROR, `object`.toString())
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                cartView.showError(
                                    CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                                )
                            }
                        } else {
                            cartView.showError(
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        }
                    }
                }
            })
        }
    }

    override fun getStore() {
        val store = CacheUtil.getInstance().orderStore
        if (store != null) {
            LocationApiCall.getInstance().getStoreDetails(store.id, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val data = o.getJSONObject("data")
                            val newStore = Store.parseStore(data)
                            CacheUtil.getInstance().orderStore = newStore
                            cartView.showStore(newStore)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onError(e)
                        }
                    } else {
                        onError("")
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    cartView.showStore(store)
                }
            })
        }
    }

    override fun getAddress() {
        val address = CacheUtil.getInstance().orderAddress
        if (AuthUtil.getInstance().isSignedIn && address != null) {
            CustomersApiCall.getInstance().getCustomerAddress(
                AuthUtil.getInstance().customer.id,
                address.id,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        if (o is JSONObject) {
                            try {
                                val data = o.getJSONObject("data")
                                val newAddress = Address.parseAddress(data)
                                CacheUtil.getInstance().orderAddress = newAddress
                                cartView.showAddress(address.attributes.line1)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                onError(e)
                            }
                        } else {
                            onError("")
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        cartView.showAddress(address.attributes.line1)
                    }
                })
        } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
            cartView.showAddress(CacheUtil.getInstance().orderDeliveryAddress)
        }
    }

    override fun onBindMenuItemViewAtPosition(
        position: Int, cartMenuItemViewHolder: CartMenuItemViewHolder
    ) {
        cartMenuItemViewHolder.setMenuItem(menuItems[position])
    }

    override fun getMenuItemsCount(): Int {
        return menuItems.size
    }

    override fun getMenuItemTotalPrice(item: MenuItem, modifierGroupPosition: Int): Double {
        var orderStoreId: String? = null
        var counter = 0
        var comboIndex = 0
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }
        val price = item.getPrice(orderStoreId)
        var totalPrice = if(price == 0.0) item.attributes.price else price
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return totalPrice
    }

    override fun getMenuItemsTotalPrice(): Double {
        var subtotalPrice = 0.0
        for (menuItem in menuItems) {
            subtotalPrice += getMenuItemTotalPrice(menuItem, -1) * menuItem.attributes.quantity
        }
        return subtotalPrice
    }

    override fun getTotalPrice(validate: Boolean, isCouponValidate: Boolean) {
        validateOrder(isCouponValidate, false, null)
    }

    override fun changeQuantity(position: Int, add: Boolean) {
        if (add) {
            trackEvent(MixpanelUtil.EVENT_INCREASE_ITEM_QUANTITY_CART)
        } else {
            trackEvent(MixpanelUtil.EVENT_DECREASE_ITEM_QUANTITY_CART)
        }
        val menuItem = menuItems[position]
        var addingItem = false
        if (!add && menuItem.attributes.quantity != 1) {
            (menuItem.attributes.quantity - 1).also { menuItem.attributes.quantity = it }
            if (menuItem.attributes.isUpsell && menuItem.attributes.upsellQuantity != 1) {
                menuItem.attributes.upsellQuantity -= 1
            }
        } else if (add) {
            addingItem = true
            menuItem.attributes.quantity += 1
            if (menuItem.attributes.isUpsell) {
                menuItem.attributes.upsellQuantity += 1
            }
        }
        removeMukafaaRedemption()
        CacheUtil.getInstance().cart = menuItems
        getTotalPrice(Values.VALIDATE_QUANTITY, false)
        cartView.refreshMenuItems(position, true)
        if (!Values.NO_PROMOTIONS) {
            if (CacheUtil.getInstance().campaign != null)
                validateDigitalCoupon(false, CacheUtil.getInstance().campaign)

        }
    }

    override fun deleteCartItem(delete: Boolean, position: Int) {
        if (!delete) {
            cartView.deleteCartItem(position)
        } else if (position >= 0 && position < menuItems.size) {
            trackEvent(MixpanelUtil.EVENT_REMOVE_FROM_CART)
            if (menuItems[position].isPromo) {
                removeCoupon()
            } else {
                val removeItem = menuItems[position]
                cartView.removeItemEvent(
                    removeItem,
                    getMenuItemTotalPrice(menuItems[position], -1) * removeItem.attributes.quantity
                )
                val deletedItem = menuItems[position]
                upsells.find { u -> u.id == deletedItem.id && deletedItem.attributes.isUpsell }
                    ?.let {
                        currUpsells.add(it)
                        cartView.showUpsell(currUpsells)
                    }
                menuItems.removeAt(position)
                removeMukafaaRedemption()
                CacheUtil.getInstance().cart = menuItems
                if (menuItems.size == 0) {
                    CacheUtil.getInstance().upsell = true
                    removeCoupon()
                    digitalCoupons.clear()
                    CacheUtil.getInstance().digitalCoupons = ""
                    CacheUtil.getInstance().campaign = null
                    CacheUtil.getInstance().campaignPromotion = null
                    CacheUtil.getInstance().campaignPromotionMenuItem = null
                    cartView.showDigitalCoupon(null)
                    cartView.setTotalPrice(
                        false,
                        0.0,
                        0.0,
                        0.0,
                        CacheUtil.getInstance().orderType == OrderType.DELIVERY,
                        0.0,
                        ArrayList(),
                        0.0
                    )
                } else {
                    cartView.refreshMenuItems(-1, true)
                    getTotalPrice(true, true)
                }
            }
        }
    }

    override fun editCartItem(
        position: Int, menuItem: MenuItem
    ) {
        trackEvent(MixpanelUtil.EVENT_EDIT_ITEM_FROM_CART)
        cartView.editCartItem(position, menuItem)
    }

    override fun getCoupon(couponCode: String) {
        if (!couponCode.isEmpty()) {
            CacheUtil.getInstance().couponCode = couponCode
            validateOrder(true, false, null)/*CouponsApiCall.getInstance().getCoupon(couponCode, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        if (object.has("data") && !object.isNull("data")) {
                            JSONArray dataArray = object.getJSONArray("data");
                            if (dataArray.length() > 0) {
                                if (object.has("included") && !object.isNull("included")) {
                                    JSONArray includedArray = object.getJSONArray("included");
                                    if (includedArray.length() > 0) {
                                        Coupon coupon = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), new TypeToken<Coupon>() {
                                        }.getType());
                                        Promotion promotion = GsonHelper.gsonFinal().fromJson(includedArray.getJSONObject(0).toString(), new TypeToken<Promotion>() {
                                        }.getType());

                                        CacheUtil.getInstance().setCoupon(coupon);
                                        CacheUtil.getInstance().setPromotion(promotion);

                                        String type = promotion.getAttributes().getType().toLowerCase();
                                        if (type.equalsIgnoreCase("item") && promotion.getAttributes().getItemId() != null) {
                                            getPromotionMenuItem();
                                        } else if (type.equalsIgnoreCase("discount")) {
                                            validateOrder(true, false, null);
                                        } else {
                                            validateOrder(true, false, null);
                                        }
                                    } else {
                                        this.onError("");
                                    }
                                } else {
                                    this.onError("");
                                }
                            } else {
                                this.onError("");
                            }
                        } else {
                            this.onError("");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    Coupon coupon = new Coupon();
                    coupon.getAttributes().setCode(couponCode);
                    CacheUtil.getInstance().setCoupon(coupon);
                    validateOrder(true, false, null);
                }
            });*/
        } else {
            cartView.showMessage(
                CacheUtil.getInstance().getTranslations(
                    Text.PLEASE_INPUT_A_COUPON_CODE_FIRST, "Coupon code is required"
                )
            )
        }
    }

    override fun showCoupon() {
        val couponCode = CacheUtil.getInstance().couponCode
        val couponName = CacheUtil.getInstance().couponName
        if (couponCode != null && couponName != null) {
            cartView.showCoupon(couponCode, couponName)
        } else {
            cartView.removeCoupon()
        }
    }

    override fun getCouponMenuItem(
        menuItemId: String,
        quantity: Int,
        price: Double,
        attributes: MenuItemAttributes?,
        campaign: Campaign?
    ) {
        MenuApiCall.getInstance().getItemDetails(menuItemId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val dataArray = `object`.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val menuItem = GsonHelper.gsonFinal()
                            .fromJson(dataArray.getJSONObject(0).toString(), MenuItem::class.java)
                        menuItem.isPromo = true
                        if (quantity > 0) {
                            menuItem.attributes.quantity = quantity
                        } else {
                            menuItem.attributes.quantity = 1
                        }
                        menuItem.attributes.price = price
                        getMenuItem(menuItem, attributes, campaign)
                    } else {
                        onError("")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError(e)
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                removeCoupon()
                cartView.showError(
                    ErrorHandler.getErrorMessage(
                        o, "", CacheUtil.getInstance().getTranslations(
                            Text.COUPON_DEFAULT_ERROR,
                            "Sorry but the coupon code you have entered is invalid. It may have already expired or the coupon is erroneous."
                        )
                    )
                )
            }
        })
    }

    private var promoMenuItem = MenuItem()

    init {
        digitalCall = null
        this.cartView.setPresenter(this)
    }

    private fun getMenuItemData(appDatabase: AppDatabase, id: String?): String? {
        var data: String? = ""
        data = try {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equals("7r5YQwmSgZCtM0ux", ignoreCase = true)) {
                cartView.getFromCache(id)
            } else {
                val applicationId = CacheUtil.getInstance().application.id
                val menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id)
                menuItemDb.data
            }
        } catch (e: Exception) {
            cartView.getFromCache(id)
        }
        return data
    }

    fun getMenuItem(menuItem: MenuItem, attributes: MenuItemAttributes?, campaign: Campaign?) {
        val executor: Executor = Executors.newSingleThreadExecutor()
        val appDatabase = AppDatabase.getInstance()
        executor.execute {
            val data = getMenuItemData(appDatabase, menuItem.id)
            if (data != null && !data.isEmpty()) {
                val applicationId = CacheUtil.getInstance().application.id
                promoMenuItem = MenuUtil.processMenuItemCustomObject(
                    GsonHelper.gsonFinal().fromJson(data, MenuItem::class.java)
                )
                if (attributes != null && campaign != null) {
                    menuItem.attributes.price = attributes.price
                    menuItem.attributes.quantity = attributes.quantity
                }
                val allergens = LinkedList<Allergen>()
                for (allergenDb in appDatabase.allergenDao().findAllByApplicationAndIds(
                    applicationId, promoMenuItem.allergensIds.toTypedArray<String>()
                )) {
                    val allergenCdn =
                        GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn::class.java)
                    allergens.add(
                        AllergenCdn.convert(
                            allergenCdn, LocaleUtil.getInstance().language
                        )
                    )
                }
                Collections.sort(allergens) { allergen1, allergen2 -> allergen1.attributes.displayOrder - allergen2.attributes.displayOrder }
                menuItem.allergens = allergens
                val ingredients = LinkedList<Ingredient>()
                val ingredientIds = HashMap<String, Ingredient>()
                for (ingredient in promoMenuItem.ingredientIds) {
                    ingredientIds[ingredient.ingredientId] = ingredient
                }
                for (ingredientDb in appDatabase.ingredientDao().findAllByApplicationAndIds(
                    applicationId, ingredientIds.keys.toTypedArray<String>()
                )) {
                    val ingredientCdn = GsonHelper.gsonFinal()
                        .fromJson(ingredientDb.data, IngredientCdn::class.java)
                    val ingredient =
                        IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().language)
                    ingredient.id = ingredientIds[ingredientCdn.id]!!.id
                    ingredients.add(ingredient)
                }
                ingredients.sortWith { ingredient1, ingredient2 -> ingredient1.attributes.displayOrder - ingredient2.attributes.displayOrder }
                menuItem.ingredients = ingredients

                //menuItemModifierGroups.clear();
                var sizes: ModifierGroup? = ModifierGroup()
                val modifierGroups = LinkedList<ModifierGroup>()
                val modifierGroupIds = HashMap<String, ModifierGroup>()
                val modifierGroupDisplayOrders = HashMap<String, Int>()
                for (modifierGroup in promoMenuItem.modifierGroupIds) {
                    modifierGroupIds[modifierGroup.id] = modifierGroup
                    //modifierGroupDisplayOrders.put(modifierGroup.getId(), menuItem.getModifierGroupIds().indexOf(modifierGroup));
                }
                for (modifierGroupDb in appDatabase.modifierGroupDao().findAllByApplicationAndIds(
                    applicationId, modifierGroupIds.keys.toTypedArray<String>()
                )) {
                    val modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(
                        GsonHelper.gsonFinal()
                            .fromJson(modifierGroupDb.data, ModifierGroupCdn::class.java)
                    )
                    val modifierItems = LinkedList<ModifierItem>()
                    val modifierGroupModifierItems =
                        modifierGroupIds[modifierGroupCdn.id]!!.modifiers
                    val modifierGroupModifierItemIds = ArrayList<String>()
                    val excludeModifierItems =
                        modifierGroupIds[modifierGroupCdn.id]!!.excludeModifiers
                    val excludedModifierItems: MutableSet<String> = HashSet()
                    for (exclude in excludeModifierItems) {
                        excludedModifierItems.add(exclude.toString())
                    }

                    // Exclude by location
                    val orderStore = CacheUtil.getInstance().orderStore
                    val excludedItemsByLocationId = Observable.fromArray(
                        modifierGroupIds[modifierGroupCdn.id]
                    ).map { modifierGroup: ModifierGroup? ->
                        if (orderStore == null) return@map ArrayList<ModifierItem>()
                        // if group contains store id in included location, add all modifiers within it to the excluded items
                        // else check modifier item excluded location if it contains store id
                        if (Observable.fromIterable(modifierGroup!!.excludedLocations)
                                .map { obj: Int -> obj.toString() }
                                .any { excludedLocation: String -> excludedLocation == orderStore.id }
                                .blockingGet()
                        ) {
                            return@map modifierGroup.modifiers
                        } else {
                            return@map Observable.fromIterable<ModifierItem>(
                                modifierGroup.modifierItems
                            ).filter { modifierItem: ModifierItem ->
                                Observable.fromIterable<Int>(modifierItem.attributes.excludedLocations)
                                    .any { item: Int -> item.toString() == orderStore.id }
                                    .blockingGet()
                            }.toList().blockingGet()
                        }
                    }.flatMap { source: List<ModifierItem>? -> Observable.fromIterable(source) }
                        .map { obj: ModifierItem -> obj.id }.toList().blockingGet()
                    excludedModifierItems.addAll(excludedItemsByLocationId)
                    val modifierGroupModifierItemDisplayOrders = HashMap<String, Int?>()
                    for (modifierGroupModifierItem in modifierGroupModifierItems) {
                        if (!excludedModifierItems.contains(modifierGroupModifierItem.id)) {
                            modifierGroupModifierItemIds.add(modifierGroupModifierItem.id)
                            modifierGroupModifierItemDisplayOrders[modifierGroupModifierItem.id] =
                                modifierGroupModifierItems.indexOf(modifierGroupModifierItem)
                        }
                    }
                    for (modifierItemDb in appDatabase.modifierItemDao().findAllByApplicationAndIds(
                        applicationId, modifierGroupModifierItemIds.toTypedArray<String>()
                    )) {
                        val modifierItemCdn = GsonHelper.gsonFinal()
                            .fromJson(modifierItemDb.data, ModifierItemCdn::class.java)
                        val modifierItem = ModifierItemCdn.convert(
                            modifierItemCdn, LocaleUtil.getInstance().language
                        )
                        modifierItem.attributes.isSelected = modifierItem.attributes.minimum == 1
                        if (modifierGroupModifierItemDisplayOrders.containsKey(modifierItem.id) && modifierGroupModifierItemDisplayOrders[modifierItem.id] != null) {
                            modifierItem.attributes.displayOrder =
                                modifierGroupModifierItemDisplayOrders[modifierItem.id]!!
                        }
                        val isExcluded =
                            orderStore != null && Observable.fromIterable(modifierItem.attributes.excludedLocations)
                                .any { item: Int -> item.toString() == orderStore.id }.blockingGet()
                        if (!isExcluded) {
                            modifierItems.add(modifierItem)
                        }
                    }
                    Collections.sort(modifierItems) { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                    val modifierGroup = ModifierGroupCdn.convert(
                        modifierGroupCdn, LocaleUtil.getInstance().language
                    )
                    val mapModifierGroup = modifierGroupIds[modifierGroup.id]
                    if (mapModifierGroup != null) {
                        modifierGroup.attributes.minimum = mapModifierGroup.min
                        modifierGroup.attributes.maximum = mapModifierGroup.max
                        modifierGroup.attributes.displayOrder = mapModifierGroup.displayOrder
                        modifierGroup.attributes.freeModifiersCount =
                            mapModifierGroup.freeModifiersCount
                        modifierGroup.setDefaultModifiers(ArrayList(mapModifierGroup.defaultModifiers))
                        modifierGroup.attributes.visibilityConfig =
                            mapModifierGroup.visibilityConfig
                    }
                    modifierGroup.modifierItems = modifierItems

                    //hamburgini ingredients handling
                    if (BuildConfig.SOLO_CONCEPT === "1") {
                        for (modifierItem in modifierGroup.modifierItems) {
                            for (modifierId in modifierGroup.defaultModifiers) {
                                if (modifierItem.id.equals(modifierId, ignoreCase = true)) {
                                    modifierItem.attributes.quantity = 1
                                    modifierItem.attributes.isSelected = true
                                }
                            }
                        }
                    }
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.value) {
                        sizes = modifierGroup
                    } else {
                        modifierGroups.add(modifierGroup)
                    }
                }
                menuItem.sizes = sizes
                Collections.sort(modifierGroups) { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }
                menuItem.modifierGroups = modifierGroups
                prepareMenuItem(menuItem, campaign)
            } else {
                var orderStoreId: String? = null
                if (CacheUtil.getInstance().orderStore != null) {
                    orderStoreId = CacheUtil.getInstance().orderStore.id
                }
                menuItem.toCdn(orderStoreId)
                menuItem.isPromo = true
                CacheUtil.getInstance().couponMenuItem = menuItem
            }
        }
    }

    fun prepareMenuItem(menuItem: MenuItem, campaign: Campaign?) {
        val filteredModifierGroups = LinkedList<ModifierGroup>()
        for (modifierGroup in menuItem.modifierGroups) {
            if (modifierGroup.modifierItems.size > 0) {
                filteredModifierGroups.add(modifierGroup)
            }
        }
        if (menuItem.sizes.modifierItems.size > 0) {
            menuItem.sizes.modifierItems[0].attributes.isSelected = true
        }
        menuItem.modifierGroups = filteredModifierGroups
        for (modifierGroup in menuItem.modifierGroups) {
            val defaultModifiers = modifierGroup.defaultModifiers
            if (modifierGroup.modifierItems.size > 0 && modifierGroup.attributes.minimum > 0) {
                if (defaultModifiers.size > 0) {
                    var selected = 0
                    for (i in modifierGroup.modifierItems.indices) {
                        val modifierItem = modifierGroup.modifierItems[i]
                        if (selected < modifierGroup.attributes.maximum && defaultModifiers.contains(
                                modifierItem.id
                            )
                        ) {
                            modifierGroup.modifierItems[i].attributes.isSelected = true
                            selected++
                        }
                    }
                } else if (modifierGroup.attributes.minimum == 1) {
                    modifierGroup.modifierItems[0].attributes.isSelected = true
                }
            }
        }
        var visibilityConfig = menuItem.sizes.attributes.visibilityConfig
        if (visibilityConfig != null) {
            val rules = visibilityConfig.rules
            if (rules != null && rules.size > 0) {
                val rule = rules[0]
                if (rule.targetType != null && rule.targetType.equals(
                        "modifier-group", ignoreCase = true
                    )
                ) {
                    for (modGroup in menuItem.modifierGroups) {
                        if (menuItem.sizes.id != modGroup.id && rule.targetId == modGroup.id) {
                            if (rule.targetIs != null && rule.targetIs.equals(
                                    "selected", ignoreCase = true
                                )
                            ) {
                                var selected = 0
                                for (modItem in modGroup.modifierItems) {
                                    if (modItem.attributes.isSelected) {
                                        selected += 1
                                    }
                                }
                                if (selected > 0) {
                                    menuItem.sizes.attributes.visibilityConfig.initialValue = ""
                                } else {
                                    menuItem.sizes.attributes.visibilityConfig.initialValue =
                                        "hidden"
                                }
                            }
                        }
                    }
                }
            }
        }
        for (group in menuItem.modifierGroups) {
            visibilityConfig = group.attributes.visibilityConfig
            if (visibilityConfig != null) {
                val rules = visibilityConfig.rules
                if (rules != null && rules.size > 0) {
                    val rule = rules[0]
                    if (rule.targetType != null) {
                        if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
                            if (group.id != menuItem.sizes.id && rule.targetId == menuItem.sizes.id) {
                                if (rule.targetIs != null && rule.targetIs.equals(
                                        "selected", ignoreCase = true
                                    )
                                ) {
                                    var selected = 0
                                    for (modItem in menuItem.sizes.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selected += 1
                                        }
                                    }
                                    if (selected > 0) {
                                        group.attributes.visibilityConfig.initialValue = ""
                                    } else {
                                        group.attributes.visibilityConfig.initialValue = "hidden"
                                    }
                                }
                            }
                            for (modGroup in menuItem.modifierGroups) {
                                if (group.id != modGroup.id && rule.targetId == modGroup.id) {
                                    if (rule.targetIs != null && rule.targetIs.equals(
                                            "selected", ignoreCase = true
                                        )
                                    ) {
                                        var selected = 0
                                        for (modItem in modGroup.modifierItems) {
                                            if (modItem.attributes.isSelected) {
                                                selected += 1
                                            }
                                        }
                                        if (selected > 0) {
                                            group.attributes.visibilityConfig.initialValue = ""
                                        } else {
                                            group.attributes.visibilityConfig.initialValue =
                                                "hidden"
                                        }
                                    }
                                    break
                                }
                            }
                        } else if (rule.targetType.equals("modifier", ignoreCase = true)) {
                            val selectedModifiers: MutableSet<String> = HashSet()
                            for (modItem in menuItem.sizes.modifierItems) {
                                if (modItem.attributes.isSelected) {
                                    selectedModifiers.add(modItem.id)
                                    break
                                }
                            }
                            for (modGroup in menuItem.modifierGroups) {
                                if (group.id != modGroup.id) {
                                    for (modItem in modGroup.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                        }
                                    }
                                }
                            }

                            //for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                            //if (!group.getId().equals(modGroup.getId())) {
                            if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                if (rule.targetIs != null && rule.targetIs.equals(
                                        "selected", ignoreCase = true
                                    )
                                ) {
                                    group.attributes.visibilityConfig.initialValue = ""
                                } else {
                                    group.attributes.visibilityConfig.initialValue = "hidden"
                                }
                            } else if (!rule.targetId.isEmpty()) {
                                group.attributes.visibilityConfig.initialValue = "hidden"
                            }
                            //}
                            //}
                        }
                    }
                }
            }
        }
        val visibleModifierGroups = ArrayList<ModifierGroup>()
        for (group in menuItem.modifierGroups) {
            visibilityConfig = group.attributes.visibilityConfig
            if (visibilityConfig != null) {
                if (visibilityConfig.initialValue != null && !visibilityConfig.initialValue.equals(
                        "hidden", ignoreCase = true
                    )
                ) {
                    visibleModifierGroups.add(group)
                }
            } else {
                visibleModifierGroups.add(group)
            }
        }
        for (modifierGroup in visibleModifierGroups) {
            var selected = 0
            for (modifierItem in modifierGroup.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    selected += modifierItem.attributes.quantity
                }
            }
        }
        var orderStoreId: String? = null
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }
        menuItem.toCdn(orderStoreId)
        menuItem.isPromo = true
        if (campaign == null) {
            if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                val couponToAdd = CouponToAdd(
                    CacheUtil.getInstance().couponCode, CacheUtil.getInstance().couponName, menuItem
                )
                CacheUtil.getInstance().couponToAdd = couponToAdd
                CacheUtil.getInstance().couponCode = null
                CacheUtil.getInstance().couponName = null
            } else {
                CacheUtil.getInstance().couponMenuItem = menuItem
                val couponCode = CacheUtil.getInstance().couponCode
                val couponName = CacheUtil.getInstance().couponName
                if (couponCode != null && couponName != null) {
                    cartView.showCoupon(couponCode, couponName)
                    cartView.addPromotionItem(menuItem)
                }
            }
        } else {
            if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                val campaignToAdd = CampaignToAdd(
                    CacheUtil.getInstance().campaign,
                    CacheUtil.getInstance().campaignPromotion,
                    menuItem
                )
                CacheUtil.getInstance().campaignToAdd = campaignToAdd
                CacheUtil.getInstance().campaign = null
                CacheUtil.getInstance().campaignPromotion = null
            } else {
                CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
            }
            cartView.showDigitalCoupon(campaign)
        }
        if (validatedOrderData != null) {
            showPrice(true, validatedOrderData!!, 0.0)
        }
        cartView.showPromotionItem()
    }

    override fun removeCoupon() {
        CacheUtil.getInstance().couponCode = null
        CacheUtil.getInstance().couponName = null
        CacheUtil.getInstance().couponMenuItem = null
        CacheUtil.getInstance().couponToAdd = null
        CacheUtil.getInstance().digitalCoupons = ""
        CacheUtil.getInstance().campaign = null
        CacheUtil.getInstance().campaignPromotion = null
        CacheUtil.getInstance().campaignPromotionMenuItem = null
        CacheUtil.getInstance().campaignToAdd = null

        if (validatePromotionDisposable != null) validatePromotionDisposable!!.dispose()
        if (validateDigitalCouponDisposable != null) validateDigitalCouponDisposable!!.dispose()
        if (digitalCall != null && digitalCall!!.isExecuted()) digitalCall!!.cancel();
        menuItems = CacheUtil.getInstance().cart
        cartView.showMenuItems(menuItems, true, true)
        cartView.removeCoupon()
        removeMukafaa()
    }

    override fun proceed(scheduleTime: String?) {
        var _scheduleTime: String? = scheduleTime
        if (CacheUtil.getInstance().cartQuantity > 0) {
            if (AuthUtil.getInstance().isSignedIn) {
                val orderType = CacheUtil.getInstance().orderType
                if (orderType != OrderType.NONE && ((orderType == OrderType.PICKUP || orderType == OrderType.DINE_IN || orderType == OrderType.CURBSIDE || orderType == OrderType.DRIVE_THRU) && CacheUtil.getInstance().orderStore != null || orderType == OrderType.DELIVERY && (CacheUtil.getInstance().orderAddress != null || CacheUtil.getInstance().orderDeliveryAddress != null))) {
                    if (orderType == OrderType.PICKUP || orderType == OrderType.DRIVE_THRU || orderType == OrderType.DINE_IN || orderType == OrderType.CURBSIDE || orderType == OrderType.DELIVERY && CacheUtil.getInstance().orderAddress != null) {
                        if (CacheUtil.getOrderStoreId() != null) {
                            if (!CacheUtil.getInstance().concept.allowAdvanceOrder) {
                                _scheduleTime = null
                            }
                            getStoreOpen(_scheduleTime)
                        } else {
                            validateOrder(false, true, scheduleTime!!)
                        }
                    } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                        getAddresses(false)
                    } else {
                        cartView.showOrderType(true)
                    }
                } else {
                    cartView.showOrderType(true)
                }
            } else {
                cartView.showSignIn()
            }
        } else {
            cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.CART_IS_EMPTY))
        }
    }

    override fun getStoreOpen(scheduleTime: String?) {
        var newScheduleTime = scheduleTime
        if (scheduleTime == null || scheduleTime.isEmpty()) {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
            newScheduleTime = simpleDateFormat.format(Date())
        }
        LocationApiCall.getInstance()
            .isOpen(CacheUtil.getOrderStoreId(), newScheduleTime, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = o as JSONObject
                        val data = `object`.getJSONObject("data")
                        val orderType = CacheUtil.getInstance().orderType
                        val openStore = Store.parseStore(data)
                        if (orderType == OrderType.DELIVERY && openStore.attributes.openDeliver || openStore.attributes.openPickup) {
                            validateOrder(false, true, scheduleTime)
                        } else {
                            cartView.showError(
                                CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED)
                            )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError(e)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    cartView.showError(
                        ErrorHandler.getErrorMessage(
                            o, "", CacheUtil.getInstance().getTranslations(
                                Text.STORE_IS_CLOSED
                            )
                        )
                    )
                }
            })
    }

    @SuppressLint("CheckResult")
    override fun checkUnavailableMenuItems() {
        if (CacheUtil.getInstance().orderStore != null) {
            val orderStoreId = CacheUtil.getInstance().orderStore.id
            if (orderStoreId != null) {
                Observable.fromIterable(menuItems).filter { item: MenuItem ->
                    Observable.fromIterable(item.modifierGroups)
                        .filter { modifierGroup: ModifierGroup ->
                            Observable.fromIterable(modifierGroup.modifierItems)
                                .any { modifierItem: ModifierItem -> modifierItem.attributes.isSelected }
                                .blockingGet()
                        }.any { modifierGroup: ModifierGroup ->
                            val isModifierGroupDisabledForLocation =
                                Observable.fromIterable(modifierGroup.attributes.excludedLocations)
                                    .map { obj: Int -> obj.toString() }
                                    .any { modifierGroupExcludedLocation: String -> modifierGroupExcludedLocation == orderStoreId }
                                    .blockingGet()
                            val modifierGroupContainsModifierItemThatIsDisabledForLocation =
                                Observable.fromIterable(modifierGroup.modifierItems)
                                    .filter { modifierItem: ModifierItem -> modifierItem.attributes.isSelected }
                                    .any { modifierItem: ModifierItem ->
                                        Observable.fromIterable(modifierItem.excludedLocations)
                                            .map { obj: Int -> obj.toString() }
                                            .any { modifierItemExcludedLocation: String -> modifierItemExcludedLocation == orderStoreId }
                                            .blockingGet()
                                    }.blockingGet()
                            isModifierGroupDisabledForLocation || modifierGroupContainsModifierItemThatIsDisabledForLocation
                        }.blockingGet()
                }.forEach { menuItem: MenuItem -> unavailableMenuItems[menuItem.id] = menuItem }
                val executor: Executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    val applicationId = CacheUtil.getInstance().application.id
                    val appDatabase = AppDatabase.getInstance()
                    for (item in menuItems) {
                        if (!item.isPromo) {
                            val menuItemEntity =
                                appDatabase.menuItemEntityDao().findById(applicationId, item.id)
                            if (menuItemEntity != null) {
                                var isUnavailable = false
                                val menuCategoryEntity = appDatabase.menuCategoryEntityDao()
                                    .findById(applicationId, menuItemEntity.menuCategoryId)
                                if (menuCategoryEntity != null) {
                                    val menuCategory: MenuCategory = menuCategoryEntity.toDomain()
                                    if (menuCategory.excludedLocations.contains(orderStoreId)) {
                                        isUnavailable = true
                                        unavailableMenuItems[item.id] = item
                                    }
                                }
                                if (!isUnavailable) {
                                    val menuItem: MenuItem = menuItemEntity.toDomain()
                                    var disable = false
                                    if (CacheUtil.getInstance().orderType == OrderType.DELIVERY) {
                                        disable = menuItem.disableForDelivery == 1
                                    } else if (CacheUtil.getInstance().orderType == OrderType.PICKUP || CacheUtil.getInstance().orderType == OrderType.DINE_IN || CacheUtil.getInstance().orderType == OrderType.CURBSIDE || CacheUtil.getInstance().orderType == OrderType.DRIVE_THRU) {
                                        disable = menuItem.disableForPickup == 1
                                    }
                                    if (disable || menuItem.excludedLocations.contains(orderStoreId) || menuItem.outOfStockLocations.contains(
                                            orderStoreId
                                        )
                                    ) {
                                        unavailableMenuItems[item.id] = item
                                    }
                                }
                            }
                        }
                    }
                    if (unavailableMenuItems.size > 0) {
                        cartView.showUnavailableMenuItems()
                    } else {
                        getApplication(false)
                    }
                }
            }
        }
    }

    override fun getUnavailableMenuItemsMessage(): String {
        var message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_DELIVERY)
        if (CacheUtil.getInstance().orderType != OrderType.DELIVERY) {
            message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_PICKUP)
        }
        if (disabledMenuItems.size > 0) {
            message = CacheUtil.getInstance().getTranslations(
                Text.DISABLED_ITEMS,
                "Sorry, the items listed below are no longer available. Kindly remove them to proceed to checkout."
            )
        }
        return message
    }

    override fun getUnavailableMenuItems(): ArrayList<MenuItem> {
        return if (disabledMenuItems.size > 0) {
            ArrayList(
                disabledMenuItems.values
            )
        } else ArrayList(
            unavailableMenuItems.values
        )
    }

    override fun getMenuItemsList(): ArrayList<MenuItem> {
        return menuItems
    }

    override fun deleteUnavailableItems() {
        val disabledItemIds: Set<String> = HashSet(disabledMenuItems.keys)
        val unavailableItemIds: Set<String> = HashSet(unavailableMenuItems.keys)
        val updatedCartItems = ArrayList<MenuItem>()
        if (disabledItemIds.size > 0) {
            for (menuItem in menuItems) {
                if (!disabledItemIds.contains(menuItem.id)) {
                    updatedCartItems.add(menuItem)
                }
            }
        } else {
            for (menuItem in menuItems) {
                if (!unavailableItemIds.contains(menuItem.id)) {
                    updatedCartItems.add(menuItem)
                }
            }
        }
        removeMukafaaRedemption()
        CacheUtil.getInstance().cart = updatedCartItems
        menuItems = updatedCartItems
        if (menuItems.size == 0) {
            CacheUtil.getInstance().upsell = true
        }
        cartView.deleteUnavailableItems(CacheUtil.getInstance().cartQuantity)
    }

    override fun getAddresses(fromSave: Boolean) {
        if (AuthUtil.getInstance().isSignedIn) {
            CustomersApiCall.getInstance().getCustomerAddresses(
                AuthUtil.getInstance().customer.id,
                "1",
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val `object` = JSONObject(o.toString())
                            val array = `object`.getJSONArray("data")
                            val addresses = ArrayList<Address>()
                            for (i in 0 until array.length()) {
                                addresses.add(Address.parseAddress(array.getJSONObject(i)))
                            }
                            if (fromSave) {
                                if (addresses.size > 0) {
                                    val address = addresses[0]
                                    CacheUtil.getInstance().orderAddress = address
                                    cartView.showPayment()
                                } else {
                                    cartView.showMessage(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED)
                                    )
                                }
                            } else {
                                for (address in addresses) {
                                    if (address.attributes.lat == CacheUtil.getInstance().orderDeliveryLatitude && address.attributes.longt == CacheUtil.getInstance().orderDeliveryLongitude) {
                                        CacheUtil.getInstance().orderAddress = address
                                        break
                                    }
                                }
                                //unsync call causing duplicate address being save
                                Handler().postDelayed({
                                    if (CacheUtil.getInstance().orderAddress != null) {
                                        cartView.showPayment()
                                    } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                                        addAddress()
                                    } else {
                                        cartView.showMessage(
                                            CacheUtil.getInstance()
                                                .getTranslations(Text.AN_ERROR_OCCURRED)
                                        )
                                    }
                                }, 1500)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onError(e.message!!)
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        cartView.showMessage(
                            ErrorHandler.getErrorMessage(
                                o, CacheUtil.getInstance().getTranslations(
                                    Text.AN_ERROR_OCCURRED
                                ), null
                            )
                        )
                    }
                })
        }
    }

    override fun updateAddress(address: Address) {}
    override fun addAddress() {
        val createCustomerAddressBody = CreateCustomerAddressBody(
            "",
            CacheUtil.getInstance().orderDeliveryLabel,
            "",
            "",
            "",
            "",
            "",
            CacheUtil.getInstance().orderDeliveryAddress,
            java.lang.Double.valueOf(CacheUtil.getInstance().orderDeliveryLatitude),
            java.lang.Double.valueOf(CacheUtil.getInstance().orderDeliveryLongitude),
            CacheUtil.getInstance().orderDeliveryTelephone,
            CacheUtil.getInstance().orderDeliveryInstructions,
            ""
        )
        CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody,
            AuthUtil.getInstance().customer.id,
            "1",
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    getAddresses(true)
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    cartView.showMessage(
                        ErrorHandler.getErrorMessage(
                            o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null
                        )
                    )
                }
            })
    }

    override fun getUpsells() {
        val menuId = MenuUtil.getDiffMenuId()
        MenuApiCall.getInstance().getUpsells(menuId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                if (o is JSONObject) {
                    try {
                        val jsonObject = o
                        val dataArray = jsonObject.getJSONArray("data")
                        val modGroups = java.util.ArrayList<ModifierGroup>()
                        val modItems = java.util.ArrayList<ModifierItem>()
                        if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                            val includedArray = jsonObject.getJSONArray("included")
                            for (i in 0 until includedArray.length()) {
                                val inclObj = includedArray.getJSONObject(i)
                                if ("modifier" == inclObj.getString("type")) {
                                    val mod = GsonHelper.gsonFinal().fromJson<ModifierItem>(
                                        inclObj.toString(),
                                        object : TypeToken<ModifierItem?>() {}.type
                                    )
                                    val name = mod.attributes.name
                                    val translated = Translated()
                                    translated.english = name
                                    translated.arabic = name
                                    translated.french = name
                                    mod.attributes.translatedName = translated
                                    modItems.add(mod)
                                } else if ("modifier-group" == inclObj.getString("type")) {
                                    val group = GsonHelper.gsonFinal().fromJson<ModifierGroup>(
                                        inclObj.toString(),
                                        object : TypeToken<ModifierGroup?>() {}.type
                                    )
                                    modGroups.add(group)
                                }
                            }
                        }
                        var upsells =
                            GsonHelper.gsonFinal().fromJson<java.util.ArrayList<MenuItem>?>(
                                dataArray.toString(),
                                object : TypeToken<java.util.ArrayList<MenuItem>?>() {}.type
                            )
                        upsells?.let {
                            for (upsell in it) {
                                upsell.attributes.quantity = 0
                                val upsellGroups = LinkedList<ModifierGroup>()
                                for (group in upsell.relationships.modifierGroups.modifierGroups) {
                                    for (inclGroup in modGroups) {
                                        if (group.id == inclGroup.id) {
                                            try {
                                                val inclModItems =
                                                    inclGroup.relationships.modifiers.modifierItems
                                                val upsellMods = LinkedList<ModifierItem>()
                                                for (inclModItem in inclModItems) {
                                                    for (modItem in modItems) {
                                                        if (inclModItem.id == modItem.id) {
                                                            upsellMods.add(modItem)
                                                        }
                                                    }
                                                }
                                                try {
                                                    var x = 0
                                                    while (x < inclGroup.attributes.minimum && upsellMods.size > x) {
                                                        upsellMods[x].attributes.isSelected = true
                                                        x++
                                                    }
                                                } catch (e: ArrayIndexOutOfBoundsException) {
                                                }
                                                inclGroup.modifierItems = upsellMods
                                            } catch (e: NullPointerException) {
                                            }
                                            if ("sizes" == inclGroup.attributes.code) {
                                                if (!inclGroup.modifierItems.isEmpty()) inclGroup.modifierItems[0].attributes.isSelected =
                                                    true
                                                upsell.sizes = inclGroup
                                            } else {
                                                upsellGroups.add(inclGroup)
                                            }
                                        }
                                    }
                                }
                                upsell.modifierGroups = upsellGroups
                                upsell.toCdn(CacheUtil.getOrderStoreId())
                                upsell.attributes.isUpsell = true
                            }
                            it.sortWith { menuItem1, menuItem2 -> menuItem1.attributes.displayOrder - menuItem2.attributes.displayOrder }
                        } ?: run {
                            upsells = arrayListOf()
                        }
                        this@CartPresenter.upsells.apply {
                            clear()
                            addAll(upsells)
                        }
                        currUpsells.clear()
                        for (upsell in upsells) {
                            val isFound =
                                menuItems.find { m -> m.id == upsell.id && m.attributes.isUpsell } != null

                            if (!isFound) {
                                currUpsells.add(upsell)
                            }
                        }

                        cartView.showUpsell(currUpsells)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError(e)
                        cartView.showUpsell(arrayListOf())
                    }
                } else {
                    onError("")
                    cartView.showUpsell(arrayListOf())
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                cartView.showUpsell(arrayListOf())
            }
        })
    }

    override fun showDigitalCoupons(show: Boolean) {
        //cartView.showProgressDialog();
        getDigitalCoupons(show, true)
    }

    override fun getDigitalCouponsCount(): Int {
        return digitalCoupons.size
    }

    override fun selectDigitalCoupon(position: Int) {
        cartView.selectDigitalCoupon(position)
    }

    override fun validateDigitalCoupon(fromApply: Boolean, digitalCoupon: Campaign) {

        GlobalScope.launch(contextPool.IO) {
            cacheUtilHelper.getCartItems {
                val customer = AuthUtil.getInstance().customer
                val concept = CacheUtil.getInstance().concept
                val store = CacheUtil.getInstance().orderStore
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
                if (customer != null) {
                    orderBody.addProperty("customer", customer.id)
                }
                orderBody.addProperty("campaign-id", digitalCoupon.id)
                orderBody.addProperty("source", "Android")
                orderBody.addProperty("notes", CacheUtil.getInstance().cartNotes)
                orderBody.addProperty(
                    "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
                )
                var discount = 0.0
                var discountedSubtotal = subtotal - discount
                if (discountedSubtotal < 0) {
                    discountedSubtotal = 0.0
                    discount = subtotal
                }
                orderBody.addProperty(
                    "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
                )
                when (CacheUtil.getInstance().orderType) {
                    OrderType.PICKUP -> {
                        deliveryCharge = 0.0
                        //                if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
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

                    OrderType.DRIVE_THRU -> {
                        deliveryCharge = 0.0
                        orderBody.addProperty("type", "drive-thru")
                        if (CacheUtil.getInstance().orderStore != null) {
                            orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
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
                        //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
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
                var orderStoreId: String? = null
                if (CacheUtil.getInstance().orderStore != null) {
                    orderStoreId = CacheUtil.getInstance().orderStore.id
                }
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
                    for (size in menuItem.sizes.modifierItems) {
                        if (size.attributes.isSelected) {
                            val modifier = JsonObject()
                            modifier.addProperty("id", size.id)
                            modifier.addProperty(
                                "price",
                                String.format(Locale.ENGLISH, "%.2f", size.attributes.price)
                                    .toDouble()
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
                                String.format(Locale.ENGLISH, "%.2f", drink.attributes.price)
                                    .toDouble()
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
                                String.format(Locale.ENGLISH, "%.2f", fries.attributes.price)
                                    .toDouble()
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
                                    modifier.addProperty(
                                        "quantity",
                                        modifierItem.attributes.quantity
                                    )
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
                    items.add(item)
                }
                orderBody.add("items", items)

                if (validateDigitalCouponDisposable != null) validateDigitalCouponDisposable!!.dispose()
                validateDigitalCouponDisposable =
                    validateDigitalCouponObservable(orderBody).subscribe { onNext: Any ->
                        try {
                            val jsonObject = JSONObject(onNext.toString())
                            if (jsonObject.has("error") && !jsonObject.isNull("error")) {
                                digitalCoupons.clear()
                                CacheUtil.getInstance().digitalCoupons = ""
                                CacheUtil.getInstance().campaign = null
                                CacheUtil.getInstance().campaignPromotion = null
                                CacheUtil.getInstance().campaignPromotionMenuItem = null
                                CacheUtil.getInstance().campaignToAdd = null
                                cartView.showDigitalCoupon(null)
                                removeCoupon()
                                cartView.showPromoError(jsonObject.getString("error"))
                            } else if (fromApply) {
                                trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                CacheUtil.getInstance().campaign = digitalCoupon
                                CacheUtil.getInstance().campaignPromotion = digitalCoupon.promotion
                                CacheUtil.getInstance().campaignPromotionMenuItem = null
                                CacheUtil.getInstance().campaignToAdd = null
                                val orderData = OrderData.parse(JSONObject(onNext.toString()))
                                if (orderData.total < 0) {
                                    orderData.total = 0.0
                                }
                                var isPromoItem = false
                                if (CacheUtil.getInstance().cartQuantity > 0) {
                                    try {
                                        val cartItems =
                                            if (jsonObject.isNull("items")) "" else jsonObject.optString(
                                                "items"
                                            )
                                        val itemsList =
                                            GsonHelper.gsonFinal().fromJson<List<MenuItem>>(
                                                cartItems,
                                                object : TypeToken<List<MenuItem?>?>() {}.type
                                            )
                                        val size = itemsList.size
                                        validatedPromoItemPrice = 0.0
                                        validatedPromoItemQuantity = 0
                                        for (i in 0 until size) {
                                            if (itemsList[i].isPromotion) {
                                                isPromoItem = true
                                                val mItem = itemsList[i]
                                                validatedPromoItemPrice = mItem.price
                                                validatedPromoItemQuantity = mItem.quantity
                                                val attributes = MenuItemAttributes()
                                                attributes.price =
                                                    digitalCoupon.promotion.attributes.itemPrice
                                                if (digitalCoupon.promotion != null && digitalCoupon.promotion.attributes.itemQuantity > 0) {
                                                    attributes.quantity =
                                                        digitalCoupon.promotion.attributes.itemQuantity
                                                } else {
                                                    attributes.quantity = 1
                                                }
                                                getCouponMenuItem(
                                                    mItem.id,
                                                    attributes.quantity,
                                                    attributes.price,
                                                    attributes,
                                                    digitalCoupon
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    if(fromApply) {
                                        cartView.showMenuItems(menuItems, true, false)
                                    }
                                    cartView.setTax(
                                        concept.vatType.equals("inclusive", ignoreCase = true),
                                        StringUtil.getFormattedPrice(orderData.vatAmount),
                                        orderData.vatAmount
                                    )
                                    if (orderData.discount > 0) {
                                        cartView.setOriginalPrice(
                                            true, orderData.subtotal + orderData.deliverCharge
                                        )
                                    }
                                    if (!isPromoItem) {
                                        cartView.setTotalPrice(
                                            true,
                                            orderData.subtotal,
                                            orderData.discountDisplay,
                                            orderData.total,
                                            CacheUtil.getInstance().orderType == OrderType.DELIVERY,
                                            orderData.deliverCharge,
                                            orderData.charges,
                                            0.0
                                        )
                                        cartView.showDigitalCoupon(digitalCoupon)
                                    } else {
                                        validatedOrderData = orderData
                                    }
                                } else {
                                    digitalCoupons.clear()
                                    CacheUtil.getInstance().digitalCoupons = ""
                                    CacheUtil.getInstance().campaign = null
                                    CacheUtil.getInstance().campaignPromotion = null
                                    CacheUtil.getInstance().campaignPromotionMenuItem = null
                                    CacheUtil.getInstance().campaignToAdd = null
                                    cartView.showDigitalCoupon(null)
                                    removeCoupon()
                                }
                            } else {
                                val orderData = OrderData.parse(JSONObject(onNext.toString()))
                                if (orderData.total < 0) {
                                    orderData.total = 0.0
                                }
                                cartView.setTax(
                                    concept.vatType.equals("inclusive", ignoreCase = true),
                                    StringUtil.getFormattedPrice(orderData.vatAmount),
                                    orderData.vatAmount
                                )
                                if (orderData.discount > 0) {
                                    cartView.setOriginalPrice(
                                        true, orderData.subtotal + orderData.deliverCharge
                                    )
                                }
                                showPrice(true, orderData, 0.0)
                                cartView.showDigitalCoupon(digitalCoupon)
                            }
                        } catch (ex: JSONException) {
                            validateDigitalCouponError(ex)
                        }
                    }

            }
        }


    }

    private fun validateDigitalCouponError(couponError: Any) {
        if (couponError is JSONException) {
            couponError.printStackTrace()
        } else {
            AppLogger.printRetrofitError(couponError)
            cartView.showDigitalCoupon(null)
            if (couponError is JSONObject) {
                try {
                    val error = couponError["error"]
                    var detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                    if (error is JSONObject) {
                        detail = error.getString("detail")
                    } else if (error is JSONArray) {
                        val errorObject = error.getJSONObject(0)
                        detail = errorObject.getString("detail")
                    }
                    cartView.showError(detail)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    cartView.showError(
                        CacheUtil.getInstance().getTranslations(
                            Text.NO_INTERNET_ERROR,
                            "Sorry we can't process your request at the moment. Please try again later."
                        )
                    )
                }
            } else {
                cartView.showError(
                    CacheUtil.getInstance().getTranslations(
                        Text.NO_INTERNET_ERROR,
                        "Sorry we can't process your request at the moment. Please try again later."
                    )
                )
            }
        }
    }

    // TODO: remove side effects
    private fun validateDigitalCouponObservable(orderBody: JsonObject?): Observable<Any> {
        return Observable.create { subscriber: ObservableEmitter<Any> ->
            CouponsApiCall.getInstance().validatePromotion(orderBody, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    subscriber.onNext(o)
                    subscriber.onComplete()
                }

                override fun onError(o: Any) {
                    validateDigitalCouponError(o)
                }
            })
        }
    }

    override fun selectPromo(position: Int) {}
    override fun getDigitalCoupons(show: Boolean, fromButtonClick: Boolean) {
        if (CacheUtil.getInstance().cartQuantity > 0) {
            val validDelivery =
                CacheUtil.getInstance().orderType == OrderType.DELIVERY && (CacheUtil.getInstance().orderAddress != null || CacheUtil.getInstance().orderDeliveryAddress != null)
            val validPickup =
                (CacheUtil.getInstance().orderType == OrderType.PICKUP || CacheUtil.getInstance().orderType == OrderType.DRIVE_THRU || CacheUtil.getInstance().orderType == OrderType.DINE_IN || CacheUtil.getInstance().orderType == OrderType.CURBSIDE) && CacheUtil.getInstance().orderStore != null
            val customer = AuthUtil.getInstance().customer
            val concept = CacheUtil.getInstance().concept
            val store = CacheUtil.getInstance().orderStore
            menuItems = CacheUtil.getInstance().cart
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
                "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
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
                "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
            )
            if (validDelivery || validPickup) {
                when (CacheUtil.getInstance().orderType) {
                    OrderType.PICKUP -> {
                        deliveryCharge = 0.0
                        //                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
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

                    OrderType.DRIVE_THRU -> {
                        deliveryCharge = 0.0
                        orderBody.addProperty("type", "drive-thru")
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
                        //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
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
                }
            }
            val vatAmount = 0.0

//            if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("exclusive")) {
//                vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//                orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
//            }
            var total = String.format(
                Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount
            ).toDouble()
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
                    }
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
                                    "price", String.format(
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
                items.add(item)
            }
            orderBody.add("items", items)
            if (digitalCall != null && digitalCall!!.isExecuted) digitalCall!!.cancel()
            digitalCall = CouponsApiCall.getInstance()
                .getCampaignsCancelableAll(orderBody, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val `object` = JSONObject(o.toString())
                            digitalCoupons.clear()
                            digitalCoupons = GsonHelper.gsonFinal().fromJson(
                                `object`.getJSONArray("data").toString(),
                                object : TypeToken<List<Campaign?>?>() {}.type
                            )
                            if (`object`.has("included")) {
                                val promotions =
                                    GsonHelper.gsonFinal().fromJson<ArrayList<Promotion>>(
                                        `object`.getJSONArray("included").toString(),
                                        object : TypeToken<ArrayList<Promotion?>?>() {}.type
                                    )
                                val promotionsHashMap = HashMap<String, Promotion>()
                                for (promotion in promotions) {
                                    promotionsHashMap[promotion.id] = promotion
                                }
                                for (digitalCoupon in digitalCoupons) {
                                    digitalCoupon.promotion =
                                        promotionsHashMap[digitalCoupon.attributes.promotionId]
                                }
                            }
                            val digitalCouponString = Gson().toJson(digitalCoupons)
                            if (digitalCoupons.size > 0) {
                                if (digitalCoupons.size == 1) {
                                    val promotion = digitalCoupons[0].promotion
                                    if (promotion != null && promotion.attributes.autoApply != null && promotion.attributes.autoApply) {
                                        if (CacheUtil.getInstance().campaign == null) {
                                            if (digitalCoupons[0].attributes.isEligible) {
                                                if (fromButtonClick) {
                                                    cartView.showDigitalCoupons(
                                                        false, digitalCoupons, 0
                                                    )
                                                } else {
                                                    availDigitalCoupon(0)
                                                }
                                            } else if (show && CacheUtil.getInstance().campaign == null) {
                                                cartView.showDigitalCoupons(
                                                    false, digitalCoupons, -1
                                                )
                                            }
                                            CacheUtil.getInstance().digitalCoupons =
                                                digitalCouponString
                                        } else {
                                            if (CacheUtil.getInstance().cartQuantity > 0) {
                                                var selected = -1
                                                if (CacheUtil.getInstance().campaign != null) {
                                                    for (i in digitalCoupons.indices) {
                                                        if (digitalCoupons[i].id.equals(
                                                                CacheUtil.getInstance().campaign.id,
                                                                ignoreCase = true
                                                            )
                                                        ) {
                                                            selected = i
                                                            break
                                                        }
                                                    }
                                                }
                                                if (show) {
                                                    cartView.showDigitalCoupons(
                                                        false, digitalCoupons, selected
                                                    )
                                                }
                                                if (CacheUtil.getInstance().campaign != null) validateDigitalCoupon(
                                                    false, CacheUtil.getInstance().campaign
                                                )
                                                CacheUtil.getInstance().digitalCoupons =
                                                    digitalCouponString
                                                cartView.showDigitalCoupon(CacheUtil.getInstance().campaign)
                                            } else {
                                                CacheUtil.getInstance().digitalCoupons = ""
                                                CacheUtil.getInstance().campaign = null
                                                CacheUtil.getInstance().campaignPromotion = null
                                                CacheUtil.getInstance().campaignPromotionMenuItem =
                                                    null
                                                CacheUtil.getInstance().campaignToAdd = null
                                                cartView.showDigitalCoupon(null)
                                            }
                                        }
                                    } else if (CacheUtil.getInstance().cartQuantity > 0) {
                                        var selected = -1
                                        if (CacheUtil.getInstance().campaign != null) {
                                            for (i in digitalCoupons.indices) {
                                                if (digitalCoupons[i].id.equals(
                                                        CacheUtil.getInstance().campaign.id,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    selected = i
                                                    break
                                                }
                                            }
                                        }
                                        if (show) {
                                            cartView.showDigitalCoupons(
                                                false, digitalCoupons, selected
                                            )
                                        }
                                        if (CacheUtil.getInstance().campaign != null) validateDigitalCoupon(
                                            false, CacheUtil.getInstance().campaign
                                        )
                                        CacheUtil.getInstance().digitalCoupons = digitalCouponString
                                        cartView.showDigitalCoupon(CacheUtil.getInstance().campaign)
                                    } else {
                                        CacheUtil.getInstance().digitalCoupons = ""
                                        CacheUtil.getInstance().campaign = null
                                        CacheUtil.getInstance().campaignPromotion = null
                                        CacheUtil.getInstance().campaignPromotionMenuItem = null
                                        CacheUtil.getInstance().campaignToAdd = null
                                        cartView.showDigitalCoupon(null)
                                    }
                                } else {
                                    var proceedBottom = true
                                    var selected = -1
                                    if (CacheUtil.getInstance().campaign != null) {
                                        for (i in digitalCoupons.indices) {
                                            if (digitalCoupons[i].id.equals(
                                                    CacheUtil.getInstance().campaign.id,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                selected = i
                                                break
                                            }
                                        }
                                    } else {
                                        var counter = 0
                                        var position = -1
                                        val dummy: MutableList<Campaign> = ArrayList()
                                        for (i in digitalCoupons.indices) {
                                            val promotion = digitalCoupons[i].promotion
                                            if (promotion != null && promotion.attributes.autoApply != null && promotion.attributes.autoApply) {
                                                if (digitalCoupons[i].attributes.isEligible) {
                                                    dummy.add(digitalCoupons[i])
                                                    counter++
                                                    position = i
                                                }
                                            }
                                        }
                                        if (counter >= 1) {
                                            proceedBottom = false
                                            if (counter == 1) {
                                                if (fromButtonClick) {
                                                    cartView.showDigitalCoupons(
                                                        false, digitalCoupons, position
                                                    )
                                                } else {
                                                    availDigitalCoupon(position)
                                                }
                                            } else {
                                                cartView.showDigitalCoupons(
                                                    false, digitalCoupons, position
                                                )
                                            }
                                        }
                                    }
                                    if (proceedBottom) {
                                        if (CacheUtil.getInstance().cartQuantity > 0) {
                                            if (show && (fromButtonClick || digitalCoupons.size > 0)) {
                                                cartView.showDigitalCoupons(
                                                    false, digitalCoupons, selected
                                                )
                                            }
                                            if (CacheUtil.getInstance().campaign != null) validateDigitalCoupon(
                                                false, CacheUtil.getInstance().campaign
                                            )
                                            CacheUtil.getInstance().digitalCoupons =
                                                digitalCouponString
                                            cartView.showDigitalCoupon(CacheUtil.getInstance().campaign)
                                        } else {
                                            CacheUtil.getInstance().campaign = null
                                            CacheUtil.getInstance().campaignPromotion = null
                                            CacheUtil.getInstance().campaignPromotionMenuItem = null
                                            CacheUtil.getInstance().campaignToAdd = null
                                            cartView.showDigitalCoupon(null)
                                        }
                                    }
                                }
                            } else {
                                CacheUtil.getInstance().digitalCoupons = digitalCouponString
                                onError("")
                            }
                        } catch (ex: JSONException) {
                            ex.printStackTrace()
                            onError("")
                        }
                        cartView.refreshDigitalCoupons()
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        CacheUtil.getInstance().campaign = null
                        CacheUtil.getInstance().campaignPromotion = null
                        CacheUtil.getInstance().campaignPromotionMenuItem = null
                        CacheUtil.getInstance().campaignToAdd = null
                        cartView.showDigitalCoupon(null)
                        if (show && (fromButtonClick || digitalCoupons.size > 0)) {
                            cartView.showDigitalCoupons(false, digitalCoupons, -1)
                        }
                    }
                })
        }
    }

    override fun availDigitalCoupon(position: Int) {
        if (CacheUtil.getInstance().couponCode != null) {
            cartView.showMessage(
                CacheUtil.getInstance().getTranslations(
                    Text.REMOVE_COUPON_MESSAGE,
                    "Please remove coupon code first before applying another promo."
                )
            )
        } else if (position != -1 && position < digitalCoupons.size) {
            val digitalCoupon = digitalCoupons[position]
            removeMukafaaRedemption()
            if (digitalCoupon.promotion != null && digitalCoupon.promotion.attributes.type.equals(
                    "item", ignoreCase = true
                )
            ) {
                val promotion = digitalCoupon.promotion
                if (promotion != null) {
                    MenuApiCall.getInstance()
                        .getItemDetails(promotion.attributes.itemId, object : ApiRequestListener {
                            override fun onSuccess(o: Any) {
                                try {
                                    val `object` = JSONObject(o.toString())
                                    val dataArray = `object`.getJSONArray("data")
                                    if (dataArray.length() > 0) {
                                        val menuItem = GsonHelper.gsonFinal().fromJson(
                                            dataArray.getJSONObject(0).toString(),
                                            MenuItem::class.java
                                        )
                                        menuItem.isPromo = true
                                        if (promotion.attributes.itemQuantity > 0) {
                                            menuItem.attributes.quantity =
                                                promotion.attributes.itemQuantity
                                        } else {
                                            menuItem.attributes.quantity = 1
                                        }
                                        var orderStoreId: String? = null
                                        if (CacheUtil.getInstance().orderStore != null) {
                                            orderStoreId = CacheUtil.getInstance().orderStore.id
                                        }
                                        menuItem.attributes.price = promotion.attributes.itemPrice
                                        menuItem.toCdn(orderStoreId)
                                        CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                                        validateDigitalCoupon(true, digitalCoupon)
                                    } else {
                                        onError("")
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    onError(e)
                                }
                            }

                            override fun onError(o: Any) {
                                AppLogger.printRetrofitError(o)
                                cartView.showDigitalCoupon(null)
                                cartView.showError(
                                    ErrorHandler.getErrorMessage(
                                        o, CacheUtil.getInstance().getTranslations(
                                            Text.AN_ERROR_OCCURRED
                                        ), null
                                    )
                                )
                            }
                        })
                }
            } else {
                validateDigitalCoupon(true, digitalCoupon)
            }
        }
    }

    override fun availDigitalCouponCampaign(campaign: Campaign) {
        for (i in digitalCoupons.indices) {
            if (digitalCoupons[i].id.equals(campaign.id, ignoreCase = true)) {
                availDigitalCoupon(i)
                break
            }
        }
    }

    override fun getDigitalCouponsList(): List<Campaign> {
        return digitalCoupons
    }

    override fun getDigitalCouponsSelected(): Int {
        var selected = -1
        if (CacheUtil.getInstance().campaign != null) {
            for (i in digitalCoupons.indices) {
                if (digitalCoupons[i].id.equals(
                        CacheUtil.getInstance().campaign.id, ignoreCase = true
                    )
                ) {
                    selected = i
                    break
                }
            }
        }
        return selected
    }

    override fun scheduleOrder(
        originalCalendar: Calendar, calendar: Calendar, checkingOnly: Boolean
    ) {
        var minSchedule = CacheUtil.getInstance().concept.defaultScheduleDeliveryTime
        var maxSchedule = CacheUtil.getInstance().concept.maxDaysAllowedInAdvanceOrder
        if (minSchedule == 0) {
            minSchedule = 15
        }
        if (maxSchedule == 0) {
            maxSchedule = 1
        }
        val calendarMinimum = Calendar.getInstance()
        calendarMinimum.time = originalCalendar.time
        calendarMinimum.add(Calendar.MINUTE, minSchedule)
        val calendarMaxDays = Calendar.getInstance()
        calendarMaxDays.time = originalCalendar.time
        calendarMaxDays.add(Calendar.DAY_OF_MONTH, maxSchedule)
        calendarMaxDays[Calendar.SECOND] = 59
        calendar[Calendar.SECOND] = 59
        if (calendar.before(calendarMinimum) && calendar != calendarMinimum) {
            cartView.showMessage(
                CacheUtil.getInstance().getTranslations(
                    Text.MINIMUM_SCHEDULED_ORDER,
                    "Unable to process order, scheduled order is allowed minimum of (x) min from current time."
                ).replace("(x)", "" + minSchedule)
            )
        } else if (calendar.after(calendarMaxDays) && calendar !== calendarMaxDays) {
            if (maxSchedule == 1) {
                cartView.showMessage(
                    CacheUtil.getInstance().getTranslations(Text.SCHEDULED_ORDER_MAXIMUM)
                )
            } else {
                cartView.showMessage(
                    CacheUtil.getInstance().getTranslations(
                        Text.MAXIMUM_SCHEDULED_ORDER,
                        "Unable to process order, scheduled order is allowed maximum of (x) days from current time."
                    ).replace("(x)", "" + maxSchedule)
                )
            }
        } else {
            val nowCalendar = Calendar.getInstance()
            nowCalendar.time = Date()
            nowCalendar.add(Calendar.MINUTE, 15)
            nowCalendar.add(Calendar.SECOND, 30)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            var scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(calendar.time))
            if (calendar.equals(minSchedule)) {
                scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(nowCalendar.time))
            }
            cartView.setSchedule(true, scheduledTime)
        }
    }

    override fun getNotes(position: Int): String {
        return if (notes.containsKey(position)) {
            notes[position]!!
        } else ""
    }

    override fun addNotes(position: Int, notesValue: String) {
        notes[position] = notesValue
        val cartItems = CacheUtil.getInstance().cart
        cartItems[position].attributes.notes = notesValue
        CacheUtil.getInstance().cart = cartItems
    }

    override fun removeMukafaaRedemption() {
        val transactionId = CacheUtil.getInstance().mukafaaTransactionId
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa()
            cartView.removeMokafaaView()
            CustomersApiCall.getInstance().deleteRedemption(
                Values.MUKAFAA_PROVIDER,
                transactionId,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {}
                    override fun onError(o: Any) {}
                })
        }
    }

    override fun getMukafaaTransactionAmount(): Double {
        var mukafaaTransactionAmount = CacheUtil.getInstance().mukafaaTransactionAmount
        if (mukafaaTransactionAmount < 0) mukafaaTransactionAmount = 0.0
        return mukafaaTransactionAmount
    }

    override fun removeMukafaa() {
        removeMukafaaRedemption()
        validateOrder(false, false, null)
    }

    override fun setMukafaaTransactionAmount(amount: Double) {
        CacheUtil.getInstance().mukafaaTransactionAmount = amount
    }

    override fun sendLog(level: ApiLogLevel, message: String) {
        sendMessageLog(level, message)
    }

    override fun onBindUpsellViewAtPosition(position: Int, holder: UpsellViewHolder) {
        holder.setUpsell(currUpsells[position])
    }

    override fun getUpsellCount(): Int {
        return currUpsells.size
    }

    override fun goToItemDetails(position: Int, upsell: MenuItem?) {
        cartView.goToItemDetails(position, upsell)
    }

    override fun addToCart(upsell: MenuItem) {
        val store = CacheUtil.getInstance().orderStore

        if (store != null && upsell.disabledStores.containsKey(store.id)) {
            cartView.showError(
                CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                    .replace("(x)", store.attributes.name)
            )
        } else {
            cartView.sendAddToCartFacebookEvent(upsell)
            CacheUtil.getInstance().addToCart(upsell)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currUpsells.removeIf { u -> u.id == upsell.id }
            } else {
                val addedUpsellIndex = upsells.indexOfFirst { i -> i.id == upsell.id }
                if (addedUpsellIndex != -1) {
                    currUpsells.removeAt(addedUpsellIndex)
                }
            }
            cartView.showCartMenuItemsLoading()
            cartView.showUpsell(currUpsells)
            getMenuItems()
        }
    }


    override fun getCouponFrom(code: String, result: ((CouponResult) -> Unit)) {
        GlobalScope.launch(contextPool.IO) {
            cacheUtilHelper.getCartItems {
                menuItems = it
                val orderBody = createOrderBody(menuItems, code)
                if (digitalCall != null && digitalCall!!.isExecuted) digitalCall!!.cancel()
                digitalCall = CouponsApiCall.getInstance()
                    .getCampaignsCancelableAll(orderBody, object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            try {
                                val `object` = JSONObject(o.toString())
                                val digitalCoupons = GsonHelper.gsonFinal()
                                    .fromJson<List<Campaign>?>(
                                        `object`.getJSONArray("data").toString(),
                                        object : TypeToken<List<Campaign>?>() {}.type
                                    )
                                digitalCoupons?.let {
                                    if (`object`.has("included")) {
                                        val promotions =
                                            GsonHelper.gsonFinal()
                                                .fromJson<ArrayList<Promotion>>(
                                                    `object`.getJSONArray("included")
                                                        .toString(),
                                                    object :
                                                        TypeToken<ArrayList<Promotion>>() {}.type
                                                )
                                        val promotionsHashMap = HashMap<String, Promotion>()
                                        for (promotion in promotions) {
                                            promotionsHashMap[promotion.id] = promotion
                                        }
                                        for (digitalCoupon in digitalCoupons) {
                                            digitalCoupon.promotion =
                                                promotionsHashMap[digitalCoupon.attributes.promotionId]
                                        }
                                    }
                                    CacheUtil.getInstance().couponCode = code
                                    result(CouponResult(true, digitalCoupons[0], ""))
                                } ?: run {
                                    result(CouponResult(false, digitalCoupons[0], "Failed"))
                                }

                            } catch (ex: JSONException) {
                                ex.printStackTrace()
                                onError("")
                                result(CouponResult(false, null, "ex"))
                            }
                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o)
                            cartView.showDigitalCoupon(null)
                            if (o is JSONObject) {
                                try {
                                    val error = o["error"]
                                    var detail = CacheUtil.getInstance()
                                        .getTranslations(Text.AN_ERROR_OCCURRED)
                                    if (error is JSONObject) {
                                        detail = error.getString("detail")
                                    } else if (error is JSONArray) {
                                        val errorObject = error.getJSONObject(0)
                                        detail = errorObject.getString("detail")
                                    }
                                    cartView.showError(detail)
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    result(
                                        CouponResult(
                                            false,
                                            null,
                                            CacheUtil.getInstance().getTranslations(
                                                Text.NO_INTERNET_ERROR,
                                                "Sorry we can't process your request at the moment. Please try again later."
                                            )
                                        )
                                    )
                                }
                            } else {
                                result(
                                    CouponResult(
                                        false, null, CacheUtil.getInstance().getTranslations(
                                            Text.NO_INTERNET_ERROR,
                                            "Sorry we can't process your request at the moment. Please try again later."
                                        )
                                    )
                                )
                            }

                        }
                    })
            }
        }
    }

    override fun validateDigitalCoupon(couponCode: String, result: ((CouponResult) -> Unit)) {
        GlobalScope.launch(contextPool.IO) {
            val concept = CacheUtil.getInstance().concept
            cacheUtilHelper.getCartItems {
                menuItems = it
                val orderBody = createValidateCouponBody(it, couponCode)
                if (validateDigitalCouponDisposable != null) validateDigitalCouponDisposable!!.dispose()
                validateDigitalCouponDisposable =
                    validateDigitalCouponObservable(orderBody, result).subscribe { onNext: Any ->
                        try {
                            val jsonObject = onNext as JSONObject
                            val itemsArray = jsonObject.getJSONArray("items")
                            var hasChanged = false
                            for (i in 0 until itemsArray.length()) {
                                val itemObject = itemsArray.getJSONObject(i)
                                val item = GsonHelper.gsonFinal()
                                    .fromJson(itemObject.toString(), MenuItem::class.java)
                                validatedPromoItemPrice = 0.0
                                validatedPromoItemQuantity = 0
                                for (menuItem in menuItems) {
                                    if (item.isPromotion) {
                                        validatedPromoItemPrice = item.price
                                        validatedPromoItemQuantity = item.quantity
                                    }
                                    if (!item.isPromotion && menuItem.id == item.id) {
                                        if (BuildUtil.isEkfc() && (item.total != item.price || getMenuItemTotalPrice(
                                                menuItems[menuItems.indexOf(menuItem)], -1
                                            ) != item.total)
                                        ) {
                                            menuItems[menuItems.indexOf(menuItem)].total =
                                                item.total
                                        } else {
                                            menuItem.price = item.getPrice(null)
                                            menuItem.attributes.price = item.getPrice(null)
                                        }
                                        hasChanged = true
                                    }
                                }
                            }
                            if (hasChanged) {
                                CacheUtil.getInstance().cart = menuItems
                                cartView.updateMenuItems(menuItems)
                            }
                            var mokafaaAmount = 0.0
                            if (jsonObject.has("loyalty")) {
                                val loyalty = jsonObject.getJSONObject("loyalty")
                                if (loyalty.has("amount")) {
                                    mokafaaAmount = loyalty.getDouble("amount")
                                    mukafaaTransactionAmount = loyalty.getDouble("amount")
                                }
                            }
                            var isInclusive = false
                            if (concept.vatType != null && concept.vatType.equals(
                                    "inclusive", ignoreCase = true
                                )
                            ) {
                                isInclusive = concept.vatType.equals("inclusive", ignoreCase = true)
                            }
                            val hasError = jsonObject.has("error") && !jsonObject.isNull("error")
                            val hasPromotion = !hasError && CacheUtil.getInstance().hasPromotion()
                            val orderData = OrderData.parse(jsonObject)
                            cartView.setTax(
                                isInclusive,
                                StringUtil.getFormattedPrice(orderData.vatAmount),
                                orderData.vatAmount
                            )
                            if (orderData.discount > 0) {
                                cartView.setOriginalPrice(
                                    hasPromotion, orderData.subtotal + orderData.deliverCharge
                                )
                            }
                            showPrice(hasPromotion, orderData, mokafaaAmount)
                            if (hasError) {
                                CacheUtil.getInstance().couponCode = null
                                CacheUtil.getInstance().couponName = null
                                CacheUtil.getInstance().couponMenuItem = null
                                CacheUtil.getInstance().couponToAdd = null
                                CacheUtil.getInstance().digitalCoupons = ""
                                CacheUtil.getInstance().campaign = null
                                CacheUtil.getInstance().campaignPromotion = null
                                CacheUtil.getInstance().campaignPromotionMenuItem = null
                                CacheUtil.getInstance().campaignToAdd = null
                                removeMukafaaRedemption()
                                result(
                                    CouponResult(
                                        false,
                                        null,
                                        jsonObject.getString("error"),
                                        true
                                    )
                                )
                            } else {
                                if (orderData.promotionName != null || orderData.promotionName == null && orderData.promotionName.isNotEmpty()) {
                                    trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                    CacheUtil.getInstance().couponCode = couponCode
                                    CacheUtil.getInstance().couponName = orderData.promotionName
                                    var isFreeItem = false
                                    for (i in 0 until itemsArray.length()) {
                                        val itemObject = itemsArray.getJSONObject(i)
                                        val item = GsonHelper.gsonFinal().fromJson(
                                            itemObject.toString(), MenuItem::class.java
                                        )
                                        if (item.isPromotion) {
                                            isFreeItem = true
                                            if (CacheUtil.getInstance().couponMenuItem == null) {
                                                getCouponMenuItem(
                                                    item.id, item.quantity, item.price, null, null
                                                )
                                            } else {
                                                val couponName = CacheUtil.getInstance().couponName
                                                if (couponName != null) {
                                                    cartView.showCoupon(couponCode, couponName)
                                                    cartView.addPromotionItem(CacheUtil.getInstance().couponMenuItem)
                                                }
                                            }
                                            break
                                        }
                                    }
                                    if (!isFreeItem) {
                                        showCoupon()
                                    }
                                    result(CouponResult(true, null, ""))
                                } else {
                                    CacheUtil.getInstance().couponCode = null
                                    CacheUtil.getInstance().couponName = null
                                    result(CouponResult(false, null, ""))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            result(CouponResult(false, null, ""))
                        }

                    }
            }
        }


    }

    private fun createOrderBody(menuItems: List<MenuItem>, couponCode: String): JsonObject {

        val validDelivery =
            CacheUtil.getInstance().orderType == OrderType.DELIVERY && (CacheUtil.getInstance().orderAddress != null || CacheUtil.getInstance().orderDeliveryAddress != null)
        val validPickup =
            (CacheUtil.getInstance().orderType == OrderType.PICKUP || CacheUtil.getInstance().orderType == OrderType.DRIVE_THRU || CacheUtil.getInstance().orderType == OrderType.DINE_IN || CacheUtil.getInstance().orderType == OrderType.CURBSIDE) && CacheUtil.getInstance().orderStore != null
        val customer = AuthUtil.getInstance().customer
        val concept = CacheUtil.getInstance().concept
        val store = CacheUtil.getInstance().orderStore

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
            "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
        )
        var discount = 0.0
        if (couponCode.isNotEmpty()) {
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
            "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
        )
        if (validDelivery || validPickup) {
            when (CacheUtil.getInstance().orderType) {
                OrderType.PICKUP -> {
                    deliveryCharge = 0.0
                    //                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
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

                OrderType.DRIVE_THRU -> {
                    deliveryCharge = 0.0
                    orderBody.addProperty("type", "drive-thru")
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
                    //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
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
                else -> {}
            }
        }
        val vatAmount = 0.0
        var total = String.format(
            Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount
        ).toDouble()
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
                }
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
                                "price", String.format(
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
            items.add(item)
        }
        orderBody.add("items", items)

        return orderBody
    }

    private fun createValidateCouponBody(
        menuItems: ArrayList<MenuItem>,
        code: String
    ): JsonObject {
        val customer = AuthUtil.getInstance().customer
        val concept = CacheUtil.getInstance().concept
        val store = CacheUtil.getInstance().orderStore
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
        if (customer != null) {
            orderBody.addProperty("customer", customer.id)
        }
        orderBody.addProperty("coupon-code", code)
        orderBody.addProperty("source", "Android")
        orderBody.addProperty("notes", CacheUtil.getInstance().cartNotes)
        orderBody.addProperty(
            "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
        )
        var discount = 0.0
        var discountedSubtotal = subtotal - discount
        if (discountedSubtotal < 0) {
            discountedSubtotal = 0.0
            discount = subtotal
        }
        orderBody.addProperty(
            "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
        )
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

            OrderType.DRIVE_THRU -> {
                deliveryCharge = 0.0
                orderBody.addProperty("type", "drive-thru")
                if (CacheUtil.getInstance().orderStore != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().orderStore.id)
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
        var orderStoreId: String? = null
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }
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
                        String.format(Locale.ENGLISH, "%.2f", size.attributes.price).toDouble()
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
                                String.format(Locale.ENGLISH, "%.2f", modifierItem.attributes.price)
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
                    String.format(Locale.ENGLISH, "%.2f", modifierItem.attributes.price).toDouble()
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
            items.add(item)
        }
        orderBody.add("items", items)

        return orderBody
    }

    private fun validateDigitalCouponObservable(
        orderBody: JsonObject?,
        result: (CouponResult) -> Unit
    ): Observable<Any> {
        return Observable.create { subscriber: ObservableEmitter<Any> ->
            CouponsApiCall.getInstance().validatePromotion(orderBody, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    subscriber.onNext(o)
                    subscriber.onComplete()
                }

                override fun onError(o: Any) {
                    validateDigitalCouponError(o, result)
                }
            })
        }
    }

    private fun validateDigitalCouponError(couponError: Any, result: (CouponResult) -> Unit) {
        if (couponError is JSONException) {
            couponError.printStackTrace()
        } else {
            AppLogger.printRetrofitError(couponError)
            cartView.showDigitalCoupon(null)
            if (couponError is JSONObject) {
                try {
                    val error = couponError["error"]
                    var detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                    if (error is JSONObject) {
                        detail = error.getString("detail")
                    } else if (error is JSONArray) {
                        val errorObject = error.getJSONObject(0)
                        detail = errorObject.getString("detail")
                    }
                    result(CouponResult(false, null, detail))
                } catch (e: JSONException) {
                    e.printStackTrace()
                    result(
                        CouponResult(
                            false, null, CacheUtil.getInstance().getTranslations(
                                Text.NO_INTERNET_ERROR,
                                "Sorry we can't process your request at the moment. Please try again later."
                            )
                        )
                    )
                }
            } else {
                result(
                    CouponResult(
                        false, null, CacheUtil.getInstance().getTranslations(
                            Text.NO_INTERNET_ERROR,
                            "Sorry we can't process your request at the moment. Please try again later."
                        )
                    )
                )
            }
        }
    }

    override fun getDigitalCouponsWith(
        menuItems: ArrayList<MenuItem>,
        show: Boolean,
        fromButtonClick: Boolean
    ) {
        GlobalScope.launch(contextPool.IO) {
            if (cacheUtilHelper.getCartQuantity() > 0) {
                val validDelivery = CacheUtil.getInstance()
                    .getOrderType() === OrderType.DELIVERY && (CacheUtil.getInstance()
                    .getOrderAddress() != null || CacheUtil.getInstance().orderDeliveryAddress != null)
                val validPickup = (CacheUtil.getInstance()
                    .getOrderType() === OrderType.PICKUP || CacheUtil.getInstance()
                    .getOrderType() === OrderType.DINE_IN || CacheUtil.getInstance()
                    .getOrderType() === OrderType.CURBSIDE) && CacheUtil.getInstance()
                    .getOrderStore() != null

                val customer = AuthUtil.getInstance().getCustomer()
                val concept = CacheUtil.getInstance().getConcept()
                val store = CacheUtil.getInstance().getOrderStore()

                val subtotal = getMenuItemsTotalPrice()
                var deliveryCharge = concept.deliveryCharge

                try {
                    val minimumOrderAmountFreeDelivery = concept.minimumOrderAmountFreeDelivery
                    if (minimumOrderAmountFreeDelivery != 0.0 && subtotal >= minimumOrderAmountFreeDelivery) {
                        deliveryCharge = concept.specialDeliveryPrice
                    }
                } catch (e: java.lang.Exception) {
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

                if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance()
                        .getCampaignPromotion() != null
                ) {
                    orderBody.addProperty("campaign-id", CacheUtil.getInstance().getCampaign().id)
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
                    val orderType = CacheUtil.getInstance().orderType
                    when (orderType) {
                        OrderType.PICKUP -> {
                            deliveryCharge = 0.0
                            //                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                            if (CacheUtil.getInstance().getApplication().attributes
                                    .getOrderTypes().contains(OrderType.DINE_IN)
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
                            //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                            val address = CacheUtil.getInstance().getOrderAddress()
                            if (address != null) {
                                orderBody.addProperty("address", address.id)
                            }
                        }

                        OrderType.CURBSIDE -> {
                            deliveryCharge = 0.0
                            if (CacheUtil.getInstance().getOrderStore() != null) {
                                orderBody.addProperty(
                                    "location",
                                    CacheUtil.getInstance().getOrderStore().id
                                )
                            }
                            if (CacheUtil.getInstance().getCurbsideCar() != null) {
                                orderBody.addProperty("type", "curbside")
                                orderBody.addProperty(
                                    "car-id",
                                    CacheUtil.getInstance().getCurbsideCar().id
                                )
                            }
                        }

                        OrderType.DRIVE_THRU -> {
                            deliveryCharge = 0.0
                            orderBody.addProperty("type", "drive-thru")
                            if (store != null) {
                                orderBody.addProperty("location", store.id)
                            }
                        }

                        OrderType.NONE -> deliveryCharge = 0.0
                    }
                }

                val vatAmount = 0.0
                var total = String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    discountedSubtotal + deliveryCharge + vatAmount
                ).toDouble()
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
                    for (size in menuItem.sizes.modifierItems) {
                        if (size.attributes.isSelected) {
                            val modifier = JsonObject()
                            modifier.addProperty("id", size.id)
                            modifier.addProperty(
                                "price",
                                String.format(Locale.ENGLISH, "%.2f", size.attributes.price)
                                    .toDouble()
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
                                String.format(Locale.ENGLISH, "%.2f", drink.attributes.price)
                                    .toDouble()
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
                                String.format(Locale.ENGLISH, "%.2f", fries.attributes.price)
                                    .toDouble()
                            )
                            modifier.addProperty("quantity", fries.attributes.quantity)
                            modifiers.add(modifier)
                        }
                    }
                    for (modifierGroup in menuItem.getModifierGroups()) {
                        val visibilityConfig = modifierGroup.attributes.visibilityConfig
                        if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                                "hidden",
                                ignoreCase = true
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
                                        ).toDouble()
                                    )
                                    modifier.addProperty(
                                        "quantity",
                                        modifierItem.attributes.quantity
                                    )
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
                    for (ingredient in menuItem.getIngredients()) {
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
                    items.add(item)
                }
                orderBody.add("items", items)

                if (digitalCall != null && digitalCall!!.isExecuted) digitalCall!!.cancel()
                digitalCall = CouponsApiCall.getInstance()
                    .getCampaignsCancelableAll(orderBody, object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            try {
                                val `object` = JSONObject(o.toString())
                                digitalCoupons.clear()
                                digitalCoupons = GsonHelper.gsonFinal().fromJson(
                                    `object`.getJSONArray("data").toString(),
                                    object : TypeToken<MutableList<Campaign>>() {}.type
                                )
                                if (`object`.has("included")) {
                                    val promotions = GsonHelper.gsonFinal()
                                        .fromJson<java.util.ArrayList<Promotion>>(
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
                                val digitalCouponString = Gson().toJson(digitalCoupons)
                                val cartQuantity = cacheUtilHelper.getCartQuantity()
                                if (digitalCoupons.size > 0) {
                                    if (digitalCoupons.size == 1) {
                                        val promotion = digitalCoupons[0].promotion
                                        if (promotion != null && promotion.attributes.autoApply != null && promotion.attributes.autoApply) {
                                            if (CacheUtil.getInstance().getCampaign() == null) {
                                                if (digitalCoupons[0].attributes.isEligible) {
                                                    if (fromButtonClick) {
                                                        cartView.showDigitalCoupons(
                                                            false,
                                                            digitalCoupons,
                                                            0
                                                        )
                                                    } else {
                                                        availDigitalCoupon(0)
                                                    }
                                                } else if (show && CacheUtil.getInstance()
                                                        .getCampaign() == null
                                                ) {
                                                    cartView.showDigitalCoupons(
                                                        false,
                                                        digitalCoupons,
                                                        -1
                                                    )
                                                }
                                                CacheUtil.getInstance()
                                                    .setDigitalCoupons(digitalCouponString)
                                            } else {

                                                if (cartQuantity > 0) {
                                                    var selected = -1
                                                    if (CacheUtil.getInstance()
                                                            .getCampaign() != null
                                                    ) {
                                                        for (i in digitalCoupons.indices) {
                                                            if (digitalCoupons[i].id.equals(
                                                                    CacheUtil.getInstance()
                                                                        .getCampaign().id,
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                selected = i
                                                                break
                                                            }
                                                        }
                                                    }
                                                    if (show) {
                                                        cartView.showDigitalCoupons(
                                                            false,
                                                            digitalCoupons,
                                                            selected
                                                        )
                                                    }
                                                    if (CacheUtil.getInstance()
                                                            .getCampaign() != null
                                                    ) validateDigitalCoupon(
                                                        false,
                                                        CacheUtil.getInstance().getCampaign()
                                                    )
                                                    CacheUtil.getInstance()
                                                        .setDigitalCoupons(digitalCouponString)
                                                    cartView.showDigitalCoupon(
                                                        CacheUtil.getInstance().getCampaign()
                                                    )
                                                } else {
                                                    CacheUtil.getInstance().setDigitalCoupons("")
                                                    CacheUtil.getInstance().setCampaign(null)
                                                    CacheUtil.getInstance()
                                                        .setCampaignPromotion(null)
                                                    CacheUtil.getInstance()
                                                        .setCampaignPromotionMenuItem(null)
                                                    CacheUtil.getInstance().setCampaignToAdd(null)
                                                    cartView.showDigitalCoupon(null)
                                                }
                                            }
                                        } else if (cartQuantity > 0) {
                                            var selected = -1
                                            if (CacheUtil.getInstance().getCampaign() != null) {
                                                for (i in digitalCoupons.indices) {
                                                    if (digitalCoupons[i].id.equals(
                                                            CacheUtil.getInstance()
                                                                .getCampaign().id, ignoreCase = true
                                                        )
                                                    ) {
                                                        selected = i
                                                        break
                                                    }
                                                }
                                            }
                                            if (show) {
                                                cartView.showDigitalCoupons(
                                                    false,
                                                    digitalCoupons,
                                                    selected
                                                )
                                            }
                                            if (CacheUtil.getInstance()
                                                    .getCampaign() != null
                                            ) validateDigitalCoupon(
                                                false,
                                                CacheUtil.getInstance().getCampaign()
                                            )
                                            CacheUtil.getInstance()
                                                .setDigitalCoupons(digitalCouponString)
                                            cartView.showDigitalCoupon(
                                                CacheUtil.getInstance().getCampaign()
                                            )
                                        } else {
                                            CacheUtil.getInstance().setDigitalCoupons("")
                                            CacheUtil.getInstance().setCampaign(null)
                                            CacheUtil.getInstance().setCampaignPromotion(null)
                                            CacheUtil.getInstance()
                                                .setCampaignPromotionMenuItem(null)
                                            CacheUtil.getInstance().setCampaignToAdd(null)
                                            cartView.showDigitalCoupon(null)
                                        }
                                    } else {
                                        var proceedBottom = true
                                        var selected = -1
                                        if (CacheUtil.getInstance().getCampaign() != null) {
                                            for (i in digitalCoupons.indices) {
                                                if (digitalCoupons[i].id.equals(
                                                        CacheUtil.getInstance().getCampaign().id,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    selected = i
                                                    break
                                                }
                                            }
                                        } else {
                                            var counter = 0
                                            var position = -1
                                            val dummy: MutableList<Campaign> = java.util.ArrayList()
                                            for (i in digitalCoupons.indices) {
                                                val promotion = digitalCoupons[i].promotion
                                                if (promotion != null && promotion.attributes.autoApply != null && promotion.attributes.autoApply) {
                                                    if (digitalCoupons[i].attributes.isEligible) {
                                                        dummy.add(digitalCoupons[i])
                                                        counter++
                                                        position = i
                                                    }
                                                }
                                            }
                                            if (counter >= 1) {
                                                proceedBottom = false
                                                if (counter == 1) {
                                                    if (fromButtonClick) {
                                                        cartView.showDigitalCoupons(
                                                            false,
                                                            digitalCoupons,
                                                            position
                                                        )
                                                    } else {
                                                        availDigitalCoupon(position)
                                                    }
                                                } else {
                                                    cartView.showDigitalCoupons(
                                                        false,
                                                        digitalCoupons,
                                                        position
                                                    )
                                                }
                                            }
                                        }
                                        if (proceedBottom) {
                                            if (cartQuantity > 0) {
                                                if (show && (fromButtonClick || digitalCoupons.size > 0)) {
                                                    cartView.showDigitalCoupons(
                                                        false,
                                                        digitalCoupons,
                                                        selected
                                                    )
                                                }
                                                if (CacheUtil.getInstance()
                                                        .getCampaign() != null
                                                ) validateDigitalCoupon(
                                                    false,
                                                    CacheUtil.getInstance().getCampaign()
                                                )
                                                CacheUtil.getInstance()
                                                    .setDigitalCoupons(digitalCouponString)
                                                cartView.showDigitalCoupon(
                                                    CacheUtil.getInstance().getCampaign()
                                                )
                                            } else {
                                                CacheUtil.getInstance().setCampaign(null)
                                                CacheUtil.getInstance().setCampaignPromotion(null)
                                                CacheUtil.getInstance()
                                                    .setCampaignPromotionMenuItem(null)
                                                CacheUtil.getInstance().setCampaignToAdd(null)
                                                cartView.showDigitalCoupon(null)
                                            }
                                        }
                                    }
                                } else {
                                    CacheUtil.getInstance().setDigitalCoupons(digitalCouponString)
                                    onError("")
                                }
                            } catch (ex: JSONException) {
                                ex.printStackTrace()
                                onError("")
                            }
                            cartView.refreshDigitalCoupons()
                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o)
                            CacheUtil.getInstance().setCampaign(null)
                            CacheUtil.getInstance().setCampaignPromotion(null)
                            CacheUtil.getInstance().setCampaignPromotionMenuItem(null)
                            CacheUtil.getInstance().setCampaignToAdd(null)
                            cartView.showDigitalCoupon(null)
                            if (show && (fromButtonClick || digitalCoupons.size > 0)) {
                                cartView.showDigitalCoupons(false, digitalCoupons, -1)
                            }
                        }
                    })
            }
        }
    }
}
