package com.herfy.history

import android.location.Geocoder
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.herfy.database.AppDatabase
import com.herfy.database.entities.AllergenDb
import com.herfy.database.entities.IngredientDb
import com.herfy.database.entities.ModifierGroupDb
import com.herfy.database.entities.ModifierItemDb
import com.herfy.history.viewholders.OrderMenuItemViewHolder
import com.herfy.history.viewholders.OrderViewHolder
import com.herfy.history.viewholders.TimeLineViewHolder
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.handler.ErrorHandler.getErrorMessage
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ably.AblyUtil.Companion.instance
import com.herfy.utils.menu.MenuUtil
import com.herfy.utils.poller.CoroutinePoller
import com.location.FusedLocationProvider
import com.location.FusedLocationProvider.LocationResultCallback
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.calls.LocationApiCall
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver
import com.skylinedynamics.solosdk.api.models.kotlin.LyveDriver
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.Allergen
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.Ingredient
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsTransaction
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemCombo
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn
import com.skylinedynamics.solosdk.api.models.objects.OrderData
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderFeedback
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Payment
import com.skylinedynamics.solosdk.api.models.objects.Payments
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.api.models.objects.combo.ComboItem
import com.skylinedynamics.solosdk.api.models.objects.combo.ComboItemMain
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar
import com.skylinedynamics.solosdk.api.models.objects.order.OrderIngredient
import com.skylinedynamics.solosdk.api.models.objects.order.OrderModifierItem
import com.skylinedynamics.solosdk.api.models.requestbodies.OrderFeedbackBody
import com.skylinedynamics.solosdk.util.GsonHelper
import io.ably.lib.types.Message
import io.shipbook.shipbooksdk.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.stream.Collectors

class OrdersPresenter(private val ordersView: OrdersContract.View) : OrdersContract.Presenter,
    LocationResultCallback {
    var hasLocation: Boolean = false

    private var fromAbly = false

    private var isLocationUpdateSubscribed = false
    private var location: Location? = null
    private var fusedLocationProvider: FusedLocationProvider? = null

    private var newestToOldest = true
    private val ordersMap = HashMap<String, Int?>()
    private var orders = ArrayList<OrderHistory>()
    private var order: OrderDetails? = null
    private var orderStatuses = ArrayList<OrderStatus>()
    private var store: Store? = null
    private var address: Address? = null
    private var menuItems = ArrayList<MenuItem>()
    private var menuItemsCount = 0
    private var driver: LyveDriver? = null
    private val comboItems: MutableList<ComboItemMain> = ArrayList()
    private var isDone = false

    private var ratings: MutableList<Rating> = ArrayList()

    private var coroutinePoller: CoroutinePoller? = null
    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private var pollingValue = 5
    private var lyvePollingEnable = false


    private var orderSequence = -1

    override fun start() {
        ordersView.setupTranslations()
        ordersView.setupViews()
        ordersView.setupFonts()
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task: Task<Boolean?> ->
            if (task.isSuccessful) {
                val lyvePolling: String = firebaseRemoteConfig.getString("lyve_polling")
                if (lyvePolling.isNotEmpty()) {
                    val lyvePollingObj = JSONObject(lyvePolling)
                    if (lyvePollingObj.has("pull-driver-api-enable")) {
                        lyvePollingEnable = lyvePollingObj.getBoolean("pull-driver-api-enable")
                    }
                    if (lyvePollingObj.has("ttl")) {
                        pollingValue = lyvePollingObj.getInt("ttl")
                    }
                }
            }
        }
    }


    override fun getLocation(fusedLocationProvider: FusedLocationProvider) {
        this.fusedLocationProvider?.disconnect()
        this.fusedLocationProvider = fusedLocationProvider
        this.fusedLocationProvider?.setLocationResultCallback(this)
        this.fusedLocationProvider?.connect()
    }

    override fun getOrders() {
        val customer = AuthUtil.getInstance().customer
        CustomersApiCall.getInstance()
            .getOrdersNew(customer.id, "1", "10", object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val array = `object`.getJSONArray("data")

                        ordersMap.clear()
                        orders = ArrayList()

                        for (x in 0 until array.length()) {
                            val orderObject = array.getJSONObject(x)
                            val order = OrderHistory.parse(orderObject)
                            val menItems = ArrayList<String>()
                            if (`object`.has("included") && !`object`.isNull("included")) {
                                val included = `object`.getJSONArray("included")
                                for (a in 0 until included.length()) {
                                    val includedObject = included.getJSONObject(a)
                                    if ((includedObject.getString("type") == "order-item")) {
                                        val menuItem = GsonHelper.gsonFinal().fromJson<MenuItem>(
                                            includedObject.toString(),
                                            object : TypeToken<MenuItem?>() {
                                            }.type
                                        )
                                        if (menuItem.relationships.relationshipOrder.links.self.contains(
                                                order.id
                                            )
                                        ) {
                                            menItems.add(menuItem.attributes.image)
                                        }
                                    } else if ((includedObject.getString("type") == "customer-address")) {
                                        val orderItem = GsonHelper.gsonFinal().fromJson<Address>(
                                            includedObject.toString(),
                                            object : TypeToken<Address?>() {
                                            }.type
                                        )
                                        if ((orderItem.id == order.customerAddressId.toString())) {
                                            order.locationName = orderItem.attributes.line1
                                        }
                                    }
                                }
                            }
                            order.menuImages = menItems
                            ordersMap[order.id] = x
                            orders.add(order)
                        }

                        Collections.sort(orders, Comparator { o1, o2 ->
                            try {
                                val simpleDateFormat =
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                                val date1 = simpleDateFormat.parse(o1.createdAt)
                                val date2 = simpleDateFormat.parse(o2.createdAt)
                                if (newestToOldest) {
                                    return@Comparator date2.compareTo(date1)
                                } else {
                                    return@Comparator date1.compareTo(date2)
                                }
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }
                            0
                        })

                        ordersView.showOrders(newestToOldest, orders)
                        val ablyUtil = instance
                        for (orderHistory: OrderHistory in orders) {
                            ablyUtil.subscribeToOrderStatusUpdate(orderHistory.id) { message: Message ->
                                this@OrdersPresenter.onOrderHistoryStatusUpdate(
                                    message
                                )
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        ordersMap.clear()
                        orders = ArrayList()
                        ordersView.showOrders(newestToOldest, orders)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    orders = ArrayList()
                    ordersView.showOrders(newestToOldest, orders)
                }
            })
    }

    override fun getDriverDetail(): LyveDriver? {
        return driver
    }

    private fun onOrderHistoryStatusUpdate(message: Message) {
        try {
            val jsonObject = JSONObject(message.data.toString())
            val orderData = OrderData.parse(jsonObject.getJSONObject("order"))
            val orderId = orderData.id
            if (ordersMap.containsKey(orderId)) {
                ordersMap[orderId]?.let {
                    getOrder(orderId, it, true, false)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return Unit
    }

    override fun onBindOrderViewAtPosition(position: Int, orderViewHolder: OrderViewHolder) {
        orderViewHolder.setOrder(orders[position])
    }

    override fun getOrdersCount(): Int {
        return orders.size
    }

    override fun sortOrders() {
        newestToOldest = !newestToOldest
        Collections.sort(orders, Comparator { o1, o2 ->
            try {
                val simpleDateFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                val date1 = simpleDateFormat.parse(o1.createdAt)
                val date2 = simpleDateFormat.parse(o2.createdAt)
                if (newestToOldest) {
                    return@Comparator date2.compareTo(date1)
                } else {
                    return@Comparator date1.compareTo(date2)
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            0
        })

        ordersView.showOrders(newestToOldest, orders)
    }

    override fun selectOrder(order: OrderHistory) {
        ordersView.selectOrder(order)
    }

    override fun getOrder(id: String, position: Int, statusUpdate: Boolean, reorder: Boolean) {
        cancelDriveLocationUpdates()
        OrdersApiCall.getInstance().getOrderNew(
            id,
            "items,location,customerAddress,campaign.promotion,payments,loyalty-points-transactions,feedbacks,car,combos",
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val jsonObject = o
                            val data = jsonObject.getJSONObject("data")

                            order = OrderDetails.parse(data)
                            order?.let { order ->
                                try {
                                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                                        val included = jsonObject.getJSONArray("included")
                                        for (a in 0 until included.length()) {
                                            val includedObject = included.getJSONObject(a)
                                            if ((includedObject.getString("type") == "payments")) {
                                                order.payment = GsonHelper.gsonFinal().fromJson(
                                                    includedObject.toString(),
                                                    object : TypeToken<Payment?>() {
                                                    }.type
                                                )
                                            } else if ((includedObject.getString("type") == "customer-car")) {
                                                order.customerCar = GsonHelper.gsonFinal().fromJson(
                                                    includedObject.toString(),
                                                    object : TypeToken<CustomerCar?>() {
                                                    }.type
                                                )
                                            }
                                        }
                                    }

                                    if (order.payments.statusDescription.isEmpty()) {
                                        val dataArr = jsonObject.optJSONArray("included")
                                        if (dataArr != null) {
                                            for (i in 0 until dataArr.length()) {
                                                val dataObj = dataArr.optJSONObject(i)
                                                if (dataObj != null) {
                                                    if ((dataObj.optString("type") == "payments")) {
                                                        val attributes =
                                                            checkNotNull(dataObj.optJSONObject("attributes"))
                                                        val payments = Payments(
                                                            attributes.optString("status"),
                                                            attributes.optString("status-description")
                                                        )
                                                        order.setPayments(payments)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val dataArr = jsonObject.optJSONArray("included")
                                    transactions = ArrayList()
                                    if (dataArr != null) {
                                        for (i in 0 until dataArr.length()) {
                                            val dataObj = dataArr.optJSONObject(i)
                                            if (dataObj != null) {
                                                if ((dataObj.getString("type") == "loyalty-points-transaction")) {
                                                    transactions?.add(
                                                        GsonHelper.gsonFinal().fromJson(
                                                            dataObj.toString(),
                                                            object :
                                                                TypeToken<LoyaltyPointsTransaction?>() {
                                                            }.type
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                if (!statusUpdate) {
                                    if (!reorder) {
                                        val orderId = order.getId()
                                        subscribeToOrderStatusUpdate(orderId)
                                        subscribeToOrderDriverAssign(orderId)
                                        getOrderStatuses()
                                        getOrderFeedbackTopics()
                                    }

                                    menuItems = ArrayList()
                                    val comboPricesList = mutableListOf<Double>()
                                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                                        val included = jsonObject.getJSONArray("included")
                                        for (i in 0 until included.length()) {
                                            val includedObject = included.getJSONObject(i)
                                            val includedType = includedObject.getString("type")
                                            if ((includedType == "order-item")) {
                                                val menuItem = GsonHelper.gsonFinal()
                                                    .fromJson<MenuItem>(
                                                        includedObject.toString(),
                                                        object : TypeToken<MenuItem?>() {
                                                        }.type
                                                    )
                                                menuItems.add(menuItem)
                                            } else if ((includedType == "location")) {
                                                store = Store.parseStore(includedObject)
                                            } else if ((includedType == "customer-address")) {
                                                address = Address.parseAddress(includedObject)
                                            } else if ((includedType == "combo")) {
                                                val combo =
                                                    GsonHelper.gsonFinal().fromJson<MenuItem>(
                                                        includedObject.toString(),
                                                        object : TypeToken<MenuItem?>() {
                                                        }.type
                                                    )
                                                comboPricesList.add(combo.attributes.finalPrice)
                                            }
                                        }
                                        var comboIndex = 0
                                        for (menuItem: MenuItem in menuItems) {
                                            if (menuItem.attributes.isCombo) {
                                                val price = if(comboPricesList.isNotEmpty() && comboIndex < comboPricesList.size) {
                                                    comboPricesList[comboIndex]
                                                } else {
                                                    0.0
                                                }
                                                if (price != 0.0) {
                                                    menuItem.price = price
                                                    menuItem.attributes.price = price
                                                }
                                                comboIndex++
                                            }
                                            val modifierGroups = ArrayList<ModifierGroup>()
                                            for (i in 0 until included.length()) {
                                                val includedObject = included.getJSONObject(i)
                                                if ((includedObject.getString("type") == "modifier-group")) {
                                                    val modifierGroup = GsonHelper.gsonFinal()
                                                        .fromJson<ModifierGroup>(
                                                            includedObject.toString(),
                                                            object : TypeToken<ModifierGroup?>() {
                                                            }.type
                                                        )
                                                    var code = ""
                                                    if (modifierGroup.attributes.code != null) {
                                                        code =
                                                            modifierGroup.attributes.code.trim { it <= ' ' }
                                                                .lowercase(Locale.getDefault())
                                                    }
                                                    var type = ""
                                                    if (modifierGroup.attributes.type != null) {
                                                        type =
                                                            modifierGroup.attributes.type.trim { it <= ' ' }
                                                                .lowercase(Locale.getDefault())
                                                    }
                                                    var label = ""
                                                    if (modifierGroup.attributes.label != null) {
                                                        label =
                                                            modifierGroup.attributes.label.trim { it <= ' ' }
                                                                .lowercase(Locale.getDefault())
                                                    }
                                                    if ((code == "sizes") || (type == "sizes") || (label == "sizes")) {
                                                        modifierGroup.type = ModifierGroupType.SIZES
                                                    } else {
                                                        modifierGroup.type =
                                                            ModifierGroupType.OPTIONS
                                                    }
                                                    modifierGroups.add(modifierGroup)
                                                } else if ((includedObject.getString("type") == "modifier")) {
                                                    val modifierItem = GsonHelper.gsonFinal()
                                                        .fromJson<OrderModifierItem>(
                                                            includedObject.toString(),
                                                            object :
                                                                TypeToken<OrderModifierItem?>() {
                                                            }.type
                                                        )

                                                    // for modifiers with empty modifier ID like in combo meal
                                                    if (modifierItem.attributes.id == null || modifierItem.attributes.id.isEmpty()) {
                                                        modifierItem.attributes.id = modifierItem.id
                                                    }

                                                    if (menuItem.modifierItemsIds.contains(
                                                            modifierItem.id
                                                        )
                                                    ) {
                                                        if (menuItem.orderModifierItemsMap.containsKey(
                                                                modifierItem.attributes.id
                                                            )
                                                        ) {
                                                            val modItem =
                                                                menuItem.orderModifierItemsMap[modifierItem.attributes.id]
                                                            if (modItem != null) {
                                                                modItem.attributes.quantity =
                                                                    modItem.attributes.quantity + modifierItem.attributes.quantity
                                                                menuItem.orderModifierItemsMap[modifierItem.attributes.id] =
                                                                    modItem
                                                            }
                                                        } else {
                                                            menuItem.orderModifierItemsMap[modifierItem.attributes.id] =
                                                                modifierItem
                                                        }
                                                    }
                                                } else if ((includedObject.getString("type") == "item-ingredient")) {
                                                    val ingredient = GsonHelper.gsonFinal()
                                                        .fromJson<OrderIngredient>(
                                                            includedObject.toString(),
                                                            object : TypeToken<OrderIngredient?>() {
                                                            }.type
                                                        )
                                                    if (menuItem.ingredientsIds.contains(ingredient.id)) {
                                                        ingredient.attributes.isSelected = false
                                                        val items: MutableList<OrderIngredient> =
                                                            ArrayList(menuItem.orderIngredients)
                                                        items.add(ingredient)
                                                        menuItem.orderIngredients = items
                                                    }
                                                }
                                            }
                                            menuItem.modifierGroups = modifierGroups
                                            menuItem.modifierGroupIds = modifierGroups
                                            menuItem.orderModifierItems =
                                                LinkedList(menuItem.orderModifierItemsMap.values)
                                        }
                                    }
                                    if (!reorder) {
                                        ordersView.showOrder(order, store, address)
                                        ordersView.showMenuItems(order, menuItems)
                                    } else {
                                        checkReorder()
                                    }
                                    if (!reorder && driver == null) {
                                        parseLyveDriver(data)
                                    }
                                } else {
                                    if (position >= 0) {
                                        if (orders.isNotEmpty() && position < orders.size) {
                                            val orderStatus = order.orderStatus
                                            orders[position].code = orderStatus.code
                                            orders[position].sequence = orderStatus.sequence
                                            orders[position].description = orderStatus.description
                                            ordersView.updateOrder(position)
                                        }
                                    } else {
                                        ordersView.updateStatus(order, order.orderStatus)
                                        if (driver == null) {
                                            parseLyveDriver(data)
                                        }
                                        ordersView.showRatings(order.orderStatus, ratings)
                                    }
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            if (!statusUpdate) {
                                ordersView.showError(
                                    CacheUtil.getInstance()
                                        .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR)
                                )
                            }
                        }
                    } else {
                        if (!statusUpdate) {
                            ordersView.showError(
                                CacheUtil.getInstance()
                                    .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR)
                            )
                        }
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    if (!statusUpdate) {
                        ordersView.showError(
                            getErrorMessage(
                                o,
                                CacheUtil.getInstance()
                                    .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR),
                                null
                            )
                        )
                    }
                }
            })
    }

    private fun parseLyveDriver(data: JSONObject) {
        order?.let { order ->
            try {
                val attributes = data.optJSONObject("attributes")
                if (attributes != null) {
                    val lyveDriverObject = attributes.getJSONObject("lyve-driver")
                    val lyveDriver =
                        Gson().fromJson(lyveDriverObject.toString(), LyveDriver::class.java)
                    if (lyveDriver != null) {
                        driver = lyveDriver
                        ordersView.showWhatsApp(driver?.mobile ?: "")
                        val bearing = lyveDriver.bearing
                        if (bearing != null) {
                            val lat = bearing.lat
                            val lng = bearing.lng
                            if (!lat.isNullOrEmpty() && !lng.isNullOrEmpty()) {
                                val customerLatLng = LatLng(
                                    order.customerLat, order.customerLng
                                )
                                val driverLatLng = LatLng(lat.toDouble(), lng.toDouble())
                                ordersView.showDriver(customerLatLng, driverLatLng, bearing.eta)
                            }
                        }
                        if (!isLocationUpdateSubscribed) {
                            subscribeToOrderDriverLocationUpdate(order.id)
                            setLogisticsPolling(order.id, order.orderStatus.sequence)
                        }
                    } else {
                        getDriver()
                    }
                } else {
                    getDriver()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    override fun getStore(reorder: Boolean) {
        order?.let { order ->
            LocationApiCall.getInstance()
                .getStoreDetails(order.locationId.toString(), object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        if (o is JSONObject) {
                            try {
                                val data = o.getJSONObject("data")

                                store = Store.parseStore(data)

                                if (order.customerAddressId.isNotEmpty()) {
                                    getAddress(reorder)
                                } else if (reorder) {
                                    checkReorder()
                                } else {
                                    ordersView.showOrder(order, store, address)
                                    ordersView.showMenuItems(order, menuItems)
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                this.onError("")
                            }
                        } else {
                            this.onError("")
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        store = Store()
                        store?.apply {
                            id = order.locationId.toString()
                            attributes.name = order.locationName
                        }

                        if (order.customerAddressId.isNotEmpty()) {
                            getAddress(reorder)
                        } else if (reorder) {
                            checkReorder()
                        } else {
                            ordersView.showOrder(order, store, address)
                            ordersView.showMenuItems(order, menuItems)
                        }
                    }
                })
        }

    }

    override fun getAddress(reorder: Boolean) {
        order?.let { order ->
            CustomersApiCall.getInstance().getCustomerAddress(
                AuthUtil.getInstance().customer.id,
                order.customerAddressId,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        if (o is JSONObject) {
                            try {
                                val data = o.getJSONObject("data")

                                address = Address.parseAddress(data)

                                if (!reorder) {
                                    ordersView.showOrder(order, store, address)
                                    ordersView.showMenuItems(order, menuItems)
                                } else {
                                    checkReorder()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                ordersView.showError(
                                    CacheUtil.getInstance()
                                        .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR)
                                )
                            }
                        } else {
                            ordersView.showError(
                                CacheUtil.getInstance()
                                    .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR)
                            )
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        ordersView.showError(
                            getErrorMessage(
                                o,
                                "",
                                CacheUtil.getInstance()
                                    .getTranslations(if (!reorder) Text.RETRIEVING_ORDER_DETAILS_ERROR else Text.REORDER_ERROR)
                            )
                        )
                    }
                })
        }

    }

    override fun getOrderStatuses() {
        order?.let { order ->
            OrdersApiCall.getInstance().getOrderStatuses(order.id, object : ApiRequestListener {
                override fun onSuccess(obj: Any) {
                    if (obj is JSONObject) {
                        try {
                            val itemsArray = obj.getJSONArray("data")
                            orderStatuses.clear()
                            for (i in 0 until itemsArray.length()) {
                                val orderStatus = OrderStatus.parse(itemsArray.getJSONObject(i))
                                orderStatuses.add(orderStatus)
                                if (orderStatus.sequence >= OrderStatusType.ARRIVED_AT_CUSTOMER.value && orderStatus.isDone) {
                                    isDone = true
                                    cancelDriveLocationUpdates()
                                }
                            }
                            Collections.sort(orderStatuses) { orderStatus, t1 -> orderStatus.sequence - t1.sequence }
                            val isCancelled =
                                order.orderStatus.sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value
                            if (isCancelled) {
                                orderStatuses = filterOrderStatuses(orderStatuses)
                            }
                            ordersView.showOrderStatuses(orderStatuses, isDone)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            this.onError(e)
                        }
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o.toString())
                    orderStatuses.clear()
                    ordersView.showOrderStatuses(orderStatuses, isDone)
                }
            })
        }
    }

    override fun onBindTimeLineViewAtPosition(
        orderPosition: Int,
        position: Int,
        timeLineViewHolder: TimeLineViewHolder,
        noDriver: Boolean
    ) {
        if (orderPosition == -1) {
            order?.let { order ->
                val isCancelled =
                    order.orderStatus.sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value
                timeLineViewHolder.setTimeLine(isCancelled, orderStatuses[position], noDriver)
            }
        } else {
            val isCancelled =
                orders[orderPosition].sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value
            val orderStatus =
                if (isCancelled) filterOrderStatuses(orders[orderPosition].statuses)[position] else orders[orderPosition].statuses[position]
            timeLineViewHolder.setTimeLine(isCancelled, orderStatus, noDriver)
        }
    }

    override fun getTimeLineCount(orderPosition: Int): Int {
        if (orderPosition == -1) {
            return orderStatuses.size
        } else {
            val isCancelled =
                orders[orderPosition].sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value
            return if (isCancelled) {
                filterOrderStatuses(orders[orderPosition].statuses).size
            } else {
                orders[orderPosition].statuses.size
            }
        }
    }

    private fun filterOrderStatuses(orderStatuses: List<OrderStatus>): ArrayList<OrderStatus> {
        val filteredOrderStatuses = ArrayList<OrderStatus>()
        for (orderStatus in orderStatuses) {
            if (orderStatus.time != null && !orderStatus.time.equals(
                    "null",
                    ignoreCase = true
                ) && !orderStatus.time.isEmpty()
            ) {
                filteredOrderStatuses.add(orderStatus)
            }
        }
        return filteredOrderStatuses
    }

    override fun onBindMenuItemViewAtPosition(
        position: Int,
        orderMenuViewHolder: OrderMenuItemViewHolder
    ) {
        orderMenuViewHolder.setMenuItem(menuItems[position])
    }

    override fun getMenuItemsCount(): Int {
        return menuItems.size
    }

    override fun getSequence(): Int {
        return if (order != null) {
            order!!.orderStatus.sequence
        } else {
            orderSequence
        }
    }

    override fun setSequence(sequence: Int) {
        orderSequence = sequence
    }

    override fun getOrder(): OrderDetails? {
        return order
    }

    override fun getDriver() {
        order?.let { order ->
            OrdersApiCall.getInstance().getDriver(order.id, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val data = o.getJSONArray("data")
                            for (i in 0 until data.length()) {
                                val dataObject = data.getJSONObject(i)
                                val attributes = dataObject.optJSONObject("attributes")
                                val employeeId = dataObject.getString("id")
                                if (attributes != null) {
                                    val lyveDriverObject = attributes.getJSONObject("lyve-driver")
                                    try {
                                        val lyveDriver = Gson().fromJson(
                                            lyveDriverObject.toString(),
                                            LyveDriver::class.java
                                        )
                                        if (lyveDriver != null) {
                                            driver = lyveDriver
                                            ordersView.showWhatsApp(driver?.mobile ?: "")
                                            val bearing = lyveDriver.bearing
                                            if (bearing != null) {
                                                val lat = bearing.lat
                                                val lng = bearing.lng
                                                if ((lat != null) && !lat.isEmpty() && (lng != null) && !lng.isEmpty()) {
                                                    val customerLatLng = LatLng(
                                                        order.customerLat, order.customerLng
                                                    )
                                                    val driverLatLng =
                                                        LatLng(lat.toDouble(), lng.toDouble())
                                                    ordersView.showDriver(
                                                        customerLatLng,
                                                        driverLatLng,
                                                        bearing.eta
                                                    )
                                                }
                                            }
                                            if (!isLocationUpdateSubscribed) {
                                                subscribeToOrderDriverLocationUpdate(order.id)
                                                setLogisticsPolling(
                                                    order.id,
                                                    order.orderStatus.sequence
                                                )
                                            }
                                        } else {
                                            getDriverBearing(employeeId)
                                        }
                                    } catch (e: Exception) {
                                        getDriverBearing(employeeId)
                                        e.printStackTrace()
                                    }
                                }
                                if (driver == null) {
                                    getDriverBearing(employeeId)
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                }
            })
        }

    }

    override fun customerArrived(orderId: String) {
        val body = JsonObject()
        body.addProperty("order-status", "customer-arrived")
        OrdersApiCall.getInstance().updateOrderStatus(orderId, body, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                ordersView.customerArrivedSuccess()
            }

            override fun onError(o: Any) {
                ordersView.showError(
                    getErrorMessage(
                        o,
                        CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED),
                        null
                    )
                )
            }
        })
    }

    override fun getDriverBearing(employeeId: String) {
        OrdersApiCall.getInstance().getDriverBearing(employeeId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val bearingObject = JSONObject(o.toString())
                    val dataObject = bearingObject.optJSONObject("data")
                    if (dataObject != null) {
                        val attributesObject = dataObject.optJSONObject("attributes")
                        if (attributesObject != null) {
                            var lat: Double? = null
                            var lng: Double? = null
                            var eta: Int? = null
                            if (attributesObject.has("lat") && !attributesObject.isNull("lat")) {
                                lat = attributesObject.optDouble("lat")
                            }
                            if (attributesObject.has("long") && !attributesObject.isNull("long")) {
                                lng = attributesObject.optDouble("long")
                            }
                            if (attributesObject.has("eta") && !attributesObject.isNull("eta")) {
                                eta = attributesObject.optInt("eta")
                            }
                            order?.let { order ->
                                if (lat != null && lng != null) {
                                    val customerLatLng = LatLng(
                                        order.customerLat, order.customerLng
                                    )
                                    val driverLatLng = LatLng(lat, lng)
                                    ordersView.showDriver(customerLatLng, driverLatLng, eta)
                                    if (!isLocationUpdateSubscribed) {
                                        subscribeToOrderDriverLocationUpdate(order.id)
                                        setLogisticsPolling(order.id, order.orderStatus.sequence)
                                    }
                                }
                            }

                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
            }
        })
    }

    override fun detachAbly() {
        instance.detachAll()
    }

    override fun getOrderFeedbackTopics() {
        order?.let { order ->
            OrdersApiCall.getInstance()
                .getOrderFeedbackTopics(order.id, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        ratings.clear()
                        try {
                            val jsonObject = JSONObject(o.toString())
                            val dataArray = jsonObject.optJSONArray("data")

                            if (dataArray != null) {
                                ratings.addAll(
                                    GsonHelper.gsonFinal().fromJson(
                                        dataArray.toString(),
                                        object : TypeToken<List<Rating?>?>() {
                                        }.type
                                    )
                                )
                            }

                            getOrderFeedbacks()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            this.onError(e.message ?: "")
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        ratings.clear()
                        ordersView.showRatings(order.orderStatus, ratings)
                    }
                })
        }
    }

    override fun getOrderFeedbackTopics(orderId: String) {
        OrdersApiCall.getInstance().getOrderFeedbackTopics(orderId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                ratings.clear()
                try {
                    val jsonObject = JSONObject(o.toString())
                    val dataArray = jsonObject.optJSONArray("data")

                    if (dataArray != null) {
                        ratings.addAll(
                            GsonHelper.gsonFinal().fromJson(
                                dataArray.toString(),
                                object : TypeToken<List<Rating?>?>() {
                                }.type
                            )
                        )
                    }

                    getOrderFeedbacks(orderId)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    this.onError(e.message ?: "")
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                ratings.clear()
                ordersView.showRatings(null, ratings)
            }
        })
    }

    override fun getOrderFeedbacks(orderId: String) {
        OrdersApiCall.getInstance().getOrderFeedbacks(orderId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val jsonObject = JSONObject(o.toString())
                    val dataArray = jsonObject.optJSONArray("data")
                    val includedArray = jsonObject.optJSONArray("included")

                    val orderFeedbacks: MutableList<OrderFeedback> = ArrayList()

                    if (dataArray != null) {
                        orderFeedbacks.addAll(
                            GsonHelper.gsonFinal().fromJson(
                                dataArray.toString(),
                                object : TypeToken<List<OrderFeedback?>?>() {
                                }.type
                            )
                        )
                    }

                    if (orderFeedbacks.size > 0) {
                        val orderFeedback = orderFeedbacks[orderFeedbacks.size - 1]

                        if (includedArray != null) {
                            for (i in 0 until includedArray.length()) {
                                val rating = GsonHelper.gsonFinal().fromJson<Rating>(
                                    includedArray.getJSONObject(i).toString(),
                                    object : TypeToken<Rating?>() {
                                    }.type
                                )
                                if (orderFeedback.ratingIds.contains(rating.id)) {
                                    for (ratingTopic in ratings) {
                                        if (rating.attributes.topic.equals(
                                                ratingTopic.attributes.name,
                                                ignoreCase = true
                                            )
                                        ) {
                                            ratingTopic.attributes.rating = rating.attributes.rating
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ordersView.showRatings(null, ratings)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    this.onError(e.message ?: "")
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                ordersView.showRatings(null, ratings)
            }
        })
    }

    override fun getOrderFeedbacks() {
        order?.let { order ->
            OrdersApiCall.getInstance().getOrderFeedbacks(order.id, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val jsonObject = JSONObject(o.toString())
                        val dataArray = jsonObject.optJSONArray("data")
                        val includedArray = jsonObject.optJSONArray("included")

                        val orderFeedbacks: MutableList<OrderFeedback> = ArrayList()

                        if (dataArray != null) {
                            orderFeedbacks.addAll(
                                GsonHelper.gsonFinal().fromJson(
                                    dataArray.toString(),
                                    object : TypeToken<List<OrderFeedback?>?>() {
                                    }.type
                                )
                            )
                        }

                        if (orderFeedbacks.size > 0) {
                            val orderFeedback = orderFeedbacks[orderFeedbacks.size - 1]

                            if (includedArray != null) {
                                for (i in 0 until includedArray.length()) {
                                    val rating = GsonHelper.gsonFinal().fromJson<Rating>(
                                        includedArray.getJSONObject(i).toString(),
                                        object : TypeToken<Rating?>() {
                                        }.type
                                    )
                                    if (orderFeedback.ratingIds.contains(rating.id)) {
                                        for (ratingTopic in ratings) {
                                            if (rating.attributes.topic.equals(
                                                    ratingTopic.attributes.name,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                ratingTopic.attributes.rating =
                                                    rating.attributes.rating
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ordersView.showRatings(order.orderStatus, ratings)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        this.onError(e.message ?: "")
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    ordersView.showRatings(order.orderStatus, ratings)
                }
            })
        }

    }

    override fun getRatings(): List<Rating> {
        return ratings
    }

    override fun sendOrderFeedback(ratings: MutableList<Rating>) {
        this.ratings = ratings
        val customer = AuthUtil.getInstance().customer
        val orderFeedbackBody = OrderFeedbackBody(
            customer.attributes.firstName + " " + customer.attributes.lastName,
            customer.attributes.email,
            customer.attributes.mobile,
            "Order Feedback",
            "Order Feedback",
            this.ratings
        )
        order?.let { order ->
            OrdersApiCall.getInstance()
                .sendOrderFeedback(order.id, orderFeedbackBody, null, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        ordersView.showMessage(
                            CacheUtil.getInstance()
                                .getTranslations(Text.ORDER_RATING_SENT, "Order ratings sent")
                        )
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        ordersView.showError(
                            getErrorMessage(
                                o,
                                CacheUtil.getInstance()
                                    .getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR),
                                null
                            )
                        )
                    }
                })
        }

    }

    override fun sendOrderFeedback(
        ratings: MutableList<Rating>,
        orderId: String,
        otherFeedback: String
    ) {
        this.ratings = ratings
        var feedbackNew = otherFeedback
        if (feedbackNew.isNullOrEmpty()) {
            feedbackNew = "Order Feedback"
        }
        val customer = AuthUtil.getInstance().customer
        var email = customer.attributes.email
        if (email.isNullOrEmpty()) {
            email = null
        }
        val orderFeedbackBody = OrderFeedbackBody(
            customer.attributes.firstName + " " + customer.attributes.lastName,
            email,
            customer.attributes.mobile,
            "Order Feedback",
            feedbackNew,
            this.ratings
        )
        OrdersApiCall.getInstance()
            .sendOrderFeedback(orderId, orderFeedbackBody, null, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    ordersView.showMessage(
                        CacheUtil.getInstance()
                            .getTranslations(Text.ORDER_RATING_SENT, "Order ratings sent")
                    )
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    ordersView.showError(
                        getErrorMessage(
                            o,
                            CacheUtil.getInstance()
                                .getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR),
                            null
                        )
                    )
                }
            })
    }

    override fun cancel() {
        order?.let { order ->
            OrdersApiCall.getInstance().cancelOrder(order.id, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    ordersView.showMessage(
                        CacheUtil.getInstance().getTranslations(Text.ORDER_CANCELLED)
                    )
                    ordersView.cancel()
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    ordersView.showMessage(
                        getErrorMessage(
                            o, "", CacheUtil.getInstance().getTranslations(
                                Text.CANCEL_ORDER_NOT_ALLOWED
                            )
                        )
                    )
                }
            })
        }

    }

    override fun checkReorder() {
        order?.let { order ->
            OrdersApiCall.getInstance().getOrderNew(
                order.id,
                "items,campaign.promotion,combo-order,combos",
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val jsonObject = o as JSONObject
                            val included = jsonObject.getJSONArray("included")
                            comboItems.clear()
                            var reorderable = true
                            for (i in 0 until included.length()) {
                                val includedObject = included.getJSONObject(i)
                                val type = includedObject.getString("type")
                                if (type.equals("order-item", ignoreCase = true)) {
                                    val menuItem = GsonHelper.gsonFinal().fromJson<MenuItem>(
                                        includedObject.toString(),
                                        object : TypeToken<MenuItem?>() {
                                        }.type
                                    )
                                    if (!menuItem.attributes.isActive) {
                                        reorderable = false
                                        break
                                    }
                                } else if (type.equals("modifier-group", ignoreCase = true)) {
                                    val modifierGroup =
                                        GsonHelper.gsonFinal().fromJson<ModifierGroup>(
                                            includedObject.toString(),
                                            object : TypeToken<ModifierGroup?>() {
                                            }.type
                                        )
                                    if (modifierGroup.attributes.enabled == 0) {
                                        reorderable = false
                                        break
                                    }
                                } else if (type.equals("modifier", ignoreCase = true)) {
                                    val modifierItem =
                                        GsonHelper.gsonFinal().fromJson<ModifierItem>(
                                            includedObject.toString(),
                                            object : TypeToken<ModifierItem?>() {
                                            }.type
                                        )
                                    if (modifierItem.attributes.enabled == 0) {
                                        reorderable = false
                                        break
                                    }
                                } else if (type.equals("item-ingredient", ignoreCase = true)) {
                                    val ingredient = GsonHelper.gsonFinal().fromJson<Ingredient>(
                                        includedObject.toString(),
                                        object : TypeToken<Ingredient?>() {
                                        }.type
                                    )
                                    if (ingredient.attributes.enabled == 0) {
                                        reorderable = false
                                        break
                                    }
                                } else if (type.equals("combo", ignoreCase = true)) {
                                    try {
                                        val comboItem =
                                            GsonHelper.gsonFinal().fromJson<ComboItemMain>(
                                                includedObject.toString(),
                                                object : TypeToken<ComboItemMain?>() {
                                                }.type
                                            )
                                        comboItems.add(comboItem)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            if (!reorderable) {
                                this.onError("")
                            } else {
                                reorder()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            this.onError("")
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o.toString())
                        ordersView.showError(
                            getErrorMessage(
                                o, "", CacheUtil.getInstance().getTranslations(
                                    Text.REORDER_ERROR,
                                    "Some items are no longer available in this order."
                                )
                            )
                        )
                    }
                })
        }

    }

    override fun reorder() {
        CacheUtil.getInstance().orderStore = store
        order?.let { order ->
            if (order.deliveryOption.equals(
                    "to-go",
                    ignoreCase = true
                ) || order.deliveryOption.equals(
                    "pickup",
                    ignoreCase = true
                ) || order.deliveryOption.equals("امسك", ignoreCase = true)
            ) {
                CacheUtil.getInstance().orderType = OrderType.PICKUP
            } else if (order.deliveryOption.equals(
                    "dine-in",
                    ignoreCase = true
                ) || order.deliveryOption.equals(
                    "eat-in",
                    ignoreCase = true
                ) || order.deliveryOption.equals("أكلت داخل", ignoreCase = true)
            ) {
                CacheUtil.getInstance().orderType = OrderType.DINE_IN
            } else if (order.deliveryOption.equals(
                    "deliver",
                    ignoreCase = true
                ) || order.deliveryOption.equals("ايصال", ignoreCase = true)
            ) {
                CacheUtil.getInstance().orderType = OrderType.DELIVERY
                CacheUtil.getInstance().orderAddress = address
            } else if (order.deliveryOption.equals("drive-thru", ignoreCase = true)) {
                CacheUtil.getInstance().orderType = OrderType.DRIVE_THRU
            }
        }


        val reorderMenuItem = menuItems[menuItemsCount]
        if (reorderMenuItem.isPromo || reorderMenuItem.attributes.isPromotion) {
            menuItemsCount++
            if (menuItemsCount < menuItems.size) {
                reorder()
            } else {
                menuItemsCount = 0
                if (!CacheUtil.getInstance().cart.isEmpty()) {
                    ordersView.showCart()
                }
            }
        } else {
            getMenuItem(
                reorderMenuItem.attributes.itemId.toString(),
                reorderMenuItem.attributes.quantity
            )
        }
    }

    fun getMenuItemData(appDatabase: AppDatabase, id: String?): String? {
        var data: String? = ""
        try {
            if (BuildUtil.soloAppKey().equals("7r5YQwmSgZCtM0ux", ignoreCase = true)) {
                data = ordersView.getFromCache(id)
            } else {
                val applicationId = CacheUtil.getInstance().application.id
                val menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id)
                data = menuItemDb.data
            }
        } catch (e: Exception) {
            data = ordersView.getFromCache(id)
        }
        return data
    }

    fun getMenuItem(menuItemId: String?, quantity: Int) {
        val executor: Executor = Executors.newSingleThreadExecutor()
        val appDatabase = AppDatabase.getInstance()
        executor.execute {
            val data = getMenuItemData(appDatabase, menuItemId)
            if (data != null && !data.isEmpty()) {
                val applicationId = CacheUtil.getInstance().application.id

                val menuItem = MenuUtil.processMenuItemCustomObject(
                    GsonHelper.gsonFinal().fromJson(data, MenuItem::class.java)
                )

                menuItem.attributes.quantity = quantity
                val allergens = LinkedList<Allergen>()
                for (allergenDb: AllergenDb in appDatabase.allergenDao().findAllByApplicationAndIds(
                    applicationId,
                    menuItem.allergensIds.toTypedArray<String>()
                )) {
                    val allergenCdn =
                        GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn::class.java)
                    allergens.add(
                        AllergenCdn.convert(
                            allergenCdn,
                            LocaleUtil.getInstance().language
                        )
                    )
                }
                Collections.sort(allergens) { allergen1, allergen2 -> allergen1.attributes.displayOrder - allergen2.attributes.displayOrder }
                menuItem.allergens = allergens

                val ingredients = LinkedList<Ingredient>()
                val ingredientIds = HashMap<String, Ingredient>()
                for (ingredient: Ingredient in menuItem.ingredientIds) {
                    ingredientIds[ingredient.ingredientId] = ingredient
                }
                for (ingredientDb: IngredientDb in appDatabase.ingredientDao()
                    .findAllByApplicationAndIds(
                        applicationId,
                        ingredientIds.keys.toTypedArray<String>()
                    )) {
                    val ingredientCdn = GsonHelper.gsonFinal()
                        .fromJson(ingredientDb.data, IngredientCdn::class.java)
                    val ingredient =
                        IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().language)
                    ingredient.id = ingredientIds[ingredientCdn.id]?.id
                    ingredients.add(ingredient)
                }
                Collections.sort(ingredients) { ingredient1, ingredient2 -> ingredient1.attributes.displayOrder - ingredient2.attributes.displayOrder }
                menuItem.ingredients = ingredients

                //menuItemModifierGroups.clear();
                var sizes: ModifierGroup? = ModifierGroup()
                val modifierGroups = LinkedList<ModifierGroup>()
                val modifierGroupIds = HashMap<String, ModifierGroup>()
                for (modifierGroup: ModifierGroup in menuItem.modifierGroupIds) {
                    modifierGroupIds[modifierGroup.id] = modifierGroup
                }
                for (modifierGroupDb: ModifierGroupDb in appDatabase.modifierGroupDao()
                    .findAllByApplicationAndIds(
                        applicationId,
                        modifierGroupIds.keys.toTypedArray<String>()
                    )) {
                    val modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(
                        GsonHelper.gsonFinal()
                            .fromJson(modifierGroupDb.data, ModifierGroupCdn::class.java)
                    )
                    val modifierItems = LinkedList<ModifierItem>()
                    val modifierGroupModifierItems =
                        modifierGroupIds[modifierGroupCdn.id]?.modifiers
                    val modifierGroupModifierItemIds = ArrayList<String>()
                    val excludeModifierItems =
                        modifierGroupIds[modifierGroupCdn.id]?.excludeModifiers
                    val excludedModifierItems: MutableSet<String> = HashSet()
                    excludeModifierItems?.let {
                        for (exclude: Int in it) {
                            excludedModifierItems.add(exclude.toString())
                        }
                    }
                    modifierGroupModifierItems?.let {
                        for (modifierGroupModifierItem: ModifierItem in it) {
                            if (!excludedModifierItems.contains(modifierGroupModifierItem.id)) {
                                modifierGroupModifierItemIds.add(modifierGroupModifierItem.id)
                            }
                        }
                    }
                    for (modifierItemDb: ModifierItemDb in appDatabase.modifierItemDao()
                        .findAllByApplicationAndIds(
                            applicationId,
                            modifierGroupModifierItemIds.toTypedArray<String>()
                        )) {
                        val modifierItemCdn = GsonHelper.gsonFinal()
                            .fromJson(modifierItemDb.data, ModifierItemCdn::class.java)
                        val modifierItem = ModifierItemCdn.convert(
                            modifierItemCdn,
                            LocaleUtil.getInstance().language
                        )
                        modifierItem.attributes.isSelected = modifierItem.attributes.minimum == 1
                        modifierItems.add(modifierItem)
                    }
                    Collections.sort(modifierItems) { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                    val modifierGroup = ModifierGroupCdn.convert(
                        modifierGroupCdn,
                        LocaleUtil.getInstance().language
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
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.value) {
                        sizes = modifierGroup
                    } else {
                        //menuItemModifierGroups.put(modifierGroup.getId(), modifierGroup);
                        modifierGroups.add(modifierGroup)
                    }
                }
                menuItem.sizes = sizes

                Collections.sort(modifierGroups) { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }
                menuItem.modifierGroups = modifierGroups

                prepareMenuItem(menuItem)
            } else {
                menuItemsCount++
                if (menuItemsCount < menuItems.size) {
                    reorder()
                } else {
                    menuItemsCount = 0
                    ordersView.showCart()
                }
            }
        }
    }

    fun prepareMenuItem(menuItem: MenuItem) {
        val filteredModifierGroups = LinkedList<ModifierGroup>()

        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
            if (modifierGroup.modifierItems.size > 0) {
                filteredModifierGroups.add(modifierGroup)
            }
        }

        Collections.sort(
            filteredModifierGroups,
            Comparator { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder })

        menuItem.modifierGroups = filteredModifierGroups

        val selectedModifierItems = menuItems[menuItemsCount].orderModifierItems
        val selectedModifierItemIds: MutableSet<String> = HashSet()
        for (modifierItem: OrderModifierItem in selectedModifierItems) {
            selectedModifierItemIds.add(modifierItem.attributes.id)
        }

        var selectedCount = 0

        var comboIndex = 0
        var trueIndex = 0
        for (modifierItem: ModifierItem in menuItem.sizes.modifierItems) {
            val selected = selectedModifierItemIds.contains(modifierItem.id)
            if (selected) {
                selectedCount++
                trueIndex = comboIndex
            }
            comboIndex++
            modifierItem.attributes.isSelected = selected
        }

        if (selectedCount == 0 && menuItem.sizes.modifierItems.size > 0) {
            menuItem.sizes.modifierItems[0].attributes.isSelected = true
        }

        var visibilityConfig = menuItem.sizes.attributes.visibilityConfig
        if (visibilityConfig != null) {
            val rules = visibilityConfig.rules
            if (rules != null && rules.size > 0) {
                val rule = rules[0]
                if (rule.targetType != null && rule.targetType.equals(
                        "modifier-group",
                        ignoreCase = true
                    )
                ) {
                    for (modGroup: ModifierGroup in menuItem.modifierGroups) {
                        if (menuItem.sizes.id != modGroup.id && (rule.targetId == modGroup.id)) {
                            if (rule.targetIs != null && rule.targetIs.equals(
                                    "selected",
                                    ignoreCase = true
                                )
                            ) {
                                var selected = 0
                                for (modItem: ModifierItem in modGroup.modifierItems) {
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

        for (group: ModifierGroup in menuItem.modifierGroups) {
            visibilityConfig = group.attributes.visibilityConfig
            if (visibilityConfig != null) {
                val rules = visibilityConfig.rules
                if (rules != null && rules.size > 0) {
                    val rule = rules[0]
                    if (rule.targetType != null) {
                        if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
                            if (group.id != menuItem.sizes.id && (rule.targetId == menuItem.sizes.id)) {
                                if (rule.targetIs != null && rule.targetIs.equals(
                                        "selected",
                                        ignoreCase = true
                                    )
                                ) {
                                    var selected = 0
                                    for (modItem: ModifierItem in menuItem.sizes.modifierItems) {
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

                            for (modGroup: ModifierGroup in menuItem.modifierGroups) {
                                if (group.id != modGroup.id && (rule.targetId == modGroup.id)) {
                                    if (rule.targetIs != null && rule.targetIs.equals(
                                            "selected",
                                            ignoreCase = true
                                        )
                                    ) {
                                        var selected = 0
                                        for (modItem: ModifierItem in modGroup.modifierItems) {
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
                            for (modItem: ModifierItem in menuItem.sizes.modifierItems) {
                                if (modItem.attributes.isSelected) {
                                    selectedModifiers.add(modItem.id)
                                    break
                                }
                            }

                            for (modGroup: ModifierGroup in menuItem.modifierGroups) {
                                if (group.id != modGroup.id) {
                                    for (modItem: ModifierItem in modGroup.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                        }
                                    }
                                }
                            }

                            if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                if (rule.targetIs != null && rule.targetIs.equals(
                                        "selected",
                                        ignoreCase = true
                                    )
                                ) {
                                    group.attributes.visibilityConfig.initialValue = ""
                                } else {
                                    group.attributes.visibilityConfig.initialValue = "hidden"
                                }
                            } else if (!rule.targetId.isEmpty()) {
                                group.attributes.visibilityConfig.initialValue = "hidden"
                            }
                        }
                    }
                }
            }
        }

        val selectedCounts = HashMap<String, Int>()

        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
            selectedCount = 0
            for (modifierItem: ModifierItem in modifierGroup.modifierItems) {
                if (selectedModifierItemIds.contains(modifierItem.id)) {
                    selectedCount++
                    modifierItem.attributes.isSelected = true
                    for (selectedModifierItem: OrderModifierItem in selectedModifierItems) {
                        if (selectedModifierItem.attributes.id.equals(
                                modifierItem.id,
                                ignoreCase = true
                            )
                        ) {
                            modifierItem.attributes.quantity =
                                selectedModifierItem.attributes.quantity
                            break
                        }
                    }
                }
            }
            selectedCounts[modifierGroup.id] = selectedCount
        }

        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
            if ((modifierGroup.modifierItems.size > 0) && selectedCounts.containsKey(modifierGroup.id) && (selectedCounts[modifierGroup.id]!! < modifierGroup.attributes.minimum)) {
                for (i in 0 until modifierGroup.attributes.minimum) {
                    if (i < modifierGroup.modifierItems.size) {
                        modifierGroup.modifierItems[i].attributes.isSelected = true
                    }
                }
            }
        }

        val removedIngredientIds = menuItems[menuItemsCount].ingredientsIds

        for (ingredient: Ingredient in menuItem.ingredients) {
            if (removedIngredientIds.contains(ingredient.id)) {
                ingredient.attributes.isSelected = false
            }
        }

        var proceedAddingCombo = false
        // Reorder Combo Item
        if (menuItem.isCombo) {
            val main = ifItemHasComboItem(menuItem.id)
            if (main != null) {
                if (!menuItem.combo.isEmpty()) {
                    for (combo: MenuItemCombo in menuItem.combo) {
                        if (!combo.components.isEmpty()) {
                            for (components: MenuItemComponents in combo.components) {
                                if (!components.items.isEmpty()) {
                                    try {
                                        for (compoItem: ComponentModifierItem in components.items) {
                                            val comboItem = main.attributes.comboItems.stream()
                                                .filter { it: ComboItem -> (it.id == compoItem.id) }
                                                .collect(Collectors.toList())
                                            if (!comboItem.isEmpty()) {
                                                compoItem.modifierGroups =
                                                    compoItem.modifierGroupsMap
                                                if (!compoItem.modifierGroups.isEmpty()) {
                                                    for (modifierGroup: ModifierGroup in compoItem.modifierGroups) {
                                                        modifierGroup.modifierItems =
                                                            modifierGroup.modifiers
                                                        for (i in modifierGroup.modifierItems.indices) {
                                                            for (modifierItem: ModifierItem in comboItem[0].modifiers) {
                                                                if ((modifierGroup.modifierItems[i].id == modifierItem.id)) {
                                                                    modifierGroup.modifierItems[i].price =
                                                                        modifierItem.price
                                                                    modifierGroup.modifierItems[i].quantity =
                                                                        modifierItem.quantity
                                                                    modifierGroup.modifierItems[i].attributes.isSelected =
                                                                        true
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Collections.sort(components.items) { menuItem1: ComponentModifierItem, menuItem2: ComponentModifierItem -> menuItem1.displayOrder - menuItem2.displayOrder }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    try {
                        for (x in menuItem.combo[trueIndex].components.indices) {
                            for (i in menuItem.combo[trueIndex].components[x].items.indices) {
                                var comboItemMainIndex = -1
                                for (a in main.attributes.comboItems.indices) {
                                    if (menuItem.combo[trueIndex].components[x].items[i].id.equals(
                                            main.attributes.comboItems[a].id, ignoreCase = true
                                        )
                                    ) {
                                        menuItem.combo[trueIndex].components[x].items[i].selected =
                                            true
                                        comboItemMainIndex = a
                                        break
                                    }
                                }
                                if (comboItemMainIndex != -1) {
                                    for (m in menuItem.combo[trueIndex].components[x].items[i].modifierGroups.indices) {
                                        for (mm in menuItem.combo[trueIndex].components[x].items[i].modifierGroups[m].modifierItems.indices) {
                                            for (xx in main.attributes.comboItems[comboItemMainIndex].modifiers.indices) {
                                                if (menuItem.combo[trueIndex].components[x].items[i].modifierGroups[m].modifierItems[mm].id.equals(
                                                        main.attributes.comboItems[comboItemMainIndex].modifiers[xx].id,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    menuItem.combo[trueIndex].components[x].items[i].modifierGroups[m].modifierItems[mm].attributes.isSelected =
                                                        true
                                                    menuItem.combo[trueIndex].components[x].items[i].modifierGroups[m].modifierItems[mm].attributes.quantity =
                                                        main.attributes.comboItems[comboItemMainIndex].modifiers[xx].quantity
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
                    proceedAddingCombo = true
                }
            }
        }


        val store = CacheUtil.getInstance().orderStore
        if (store == null || !menuItem.disabledStores.containsKey(store.id)) {
            if (!menuItem.isCombo) {
                addMenuItem(menuItem)
            } else if (proceedAddingCombo) {
                addMenuItem(menuItem)
            }
        }

        menuItemsCount++
        if (menuItemsCount < menuItems.size) {
            reorder()
        } else {
            menuItemsCount = 0
            ordersView.showCart()
        }
    }

    private fun addMenuItem(menuItem: MenuItem) {
        menuItem.attributes.isReorder = true
        val cartCount = CacheUtil.getInstance().cartQuantity
        ordersView.logFirebaseEvent("add_to_cart_event")
        CacheUtil.getInstance().addToCart(menuItem)
        removeMukafaaRedemption()
        if (cartCount == 0) {
            ordersView.setCartExpiry()
        }
    }

    private fun ifItemHasComboItem(id: String): ComboItemMain? {
        for (main in comboItems) {
            if ((main.attributes != null) && main.attributes.itemId != null && main.attributes.itemId.equals(
                    id,
                    ignoreCase = true
                )
            ) {
                return main
            }
        }
        return null
    }

    override fun getModifiers(menuCategoryId: String, menuItem: MenuItem) {
        MenuApiCall.getInstance().getModifierGroups(
            CacheUtil.getInstance().menuId,
            menuCategoryId,
            menuItem.id,
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())

                        val modifierGroups = GsonHelper.gsonFinal()
                            .fromJson<ArrayList<ModifierGroup>>(
                                `object`.getJSONArray("data").toString(),
                                object : TypeToken<ArrayList<ModifierGroup?>?>() {
                                }.type
                            )

                        for (modifierGroup: ModifierGroup in modifierGroups) {
                            var code = ""
                            if (modifierGroup.attributes.code != null) {
                                code = modifierGroup.attributes.code.trim { it <= ' ' }
                                    .lowercase(Locale.getDefault())
                            }
                            var type = ""
                            if (modifierGroup.attributes.type != null) {
                                type = modifierGroup.attributes.type.trim { it <= ' ' }
                                    .lowercase(Locale.getDefault())
                            }
                            var label = ""
                            if (modifierGroup.attributes.label != null) {
                                label = modifierGroup.attributes.label.trim { it <= ' ' }
                                    .lowercase(Locale.getDefault())
                            }

                            if (menuItem.sizes.type == ModifierGroupType.OPTIONS && ((code == "sizes") || (type == "sizes") || (label == "sizes"))) {
                                modifierGroup.type = ModifierGroupType.SIZES
                                menuItem.sizes = modifierGroup
                            } else {
                                modifierGroup.type = ModifierGroupType.OPTIONS
                                val items: MutableList<ModifierGroup> =
                                    ArrayList(menuItem.modifierGroups)
                                items.add(modifierGroup)
                                menuItem.modifierGroups = items
                            }
                        }

                        if (`object`.has("included")) {
                            val modifierItems = GsonHelper.gsonFinal()
                                .fromJson<ArrayList<ModifierItem>>(
                                    `object`.getJSONArray("included").toString(),
                                    object : TypeToken<ArrayList<ModifierItem?>?>() {
                                    }.type
                                )
                            for (modifierItem: ModifierItem in modifierItems) {
                                if (menuItem.sizes.modifierItemsIds.contains(modifierItem.id)) {
                                    val items: MutableList<ModifierItem> =
                                        ArrayList(menuItem.sizes.modifierItems)
                                    items.add(modifierItem)
                                    menuItem.sizes.modifierItems = items
                                } else {
                                    for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
                                        if (modifierGroup.modifierItemsIds.contains(modifierItem.id)) {
                                            val items: MutableList<ModifierItem> =
                                                ArrayList(modifierGroup.modifierItems)
                                            items.add(modifierItem)
                                            modifierGroup.modifierItems = items
                                            break
                                        }
                                    }
                                }
                            }
                        }

                        val filteredModifierGroups = LinkedList<ModifierGroup>()
                        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
                            if (modifierGroup.modifierItems.size > 0) {
                                filteredModifierGroups.add(modifierGroup)
                            }
                        }

                        Collections.sort(
                            filteredModifierGroups,
                            object : Comparator<ModifierGroup> {
                                override fun compare(
                                    modifierGroup1: ModifierGroup,
                                    modifierGroup2: ModifierGroup
                                ): Int {
                                    return modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder
                                }
                            })

                        menuItem.modifierGroups = filteredModifierGroups

                        val selectedModifierItems = menuItems[menuItemsCount].orderModifierItems
                        val selectedModifierItemIds: MutableSet<String> = HashSet()
                        for (modifierItem: OrderModifierItem in selectedModifierItems) {
                            selectedModifierItemIds.add(modifierItem.attributes.id)
                        }

                        var selectedCount = 0

                        for (modifierItem: ModifierItem in menuItem.sizes.modifierItems) {
                            val selected = selectedModifierItemIds.contains(modifierItem.id)
                            if (selected) {
                                selectedCount++
                            }
                            modifierItem.attributes.isSelected = selected
                        }

                        if (selectedCount == 0 && menuItem.sizes.modifierItems.size > 0) {
                            menuItem.sizes.modifierItems[0].attributes.isSelected = true
                        }

                        val selectedCounts = HashMap<String, Int>()

                        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
                            selectedCount = 0
                            for (modifierItem: ModifierItem in modifierGroup.modifierItems) {
                                if (selectedModifierItemIds.contains(modifierItem.id)) {
                                    selectedCount++
                                    modifierItem.attributes.isSelected = true
                                    for (selectedModifierItem: OrderModifierItem in selectedModifierItems) {
                                        if (selectedModifierItem.attributes.id.equals(
                                                modifierItem.id,
                                                ignoreCase = true
                                            )
                                        ) {
                                            modifierItem.attributes.quantity =
                                                selectedModifierItem.attributes.quantity
                                            break
                                        }
                                    }
                                }
                            }
                            selectedCounts[modifierGroup.id] = selectedCount
                        }

                        for (modifierGroup: ModifierGroup in menuItem.modifierGroups) {
                            if ((modifierGroup.modifierItems.size > 0) && selectedCounts.containsKey(
                                    modifierGroup.id
                                ) && (selectedCounts[modifierGroup.id]!! < modifierGroup.attributes.minimum)
                            ) {
                                for (i in 0 until modifierGroup.attributes.minimum) {
                                    if (i < modifierGroup.modifierItems.size) {
                                        modifierGroup.modifierItems[i].attributes.isSelected = true
                                    }
                                }
                            }
                        }

                        for (group: ModifierGroup in menuItem.modifierGroups) {
                            val visibilityConfig = group.attributes.visibilityConfig
                            if (visibilityConfig != null) {
                                val rules = visibilityConfig.rules
                                if (rules != null && rules.size > 0) {
                                    val rule = rules[0]
                                    if (rule.targetType != null && rule.targetType.equals(
                                            "modifier-group",
                                            ignoreCase = true
                                        )
                                    ) {
                                        for (modGroup: ModifierGroup in menuItem.modifierGroups) {
                                            if (group.id != modGroup.id && (rule.targetId == modGroup.id)) {
                                                if (rule.targetIs != null && rule.targetIs.equals(
                                                        "selected",
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    var selected = 0
                                                    for (modItem: ModifierItem in modGroup.modifierItems) {
                                                        if (modItem.attributes.isSelected) {
                                                            selected += 1
                                                        }
                                                    }
                                                    if (selected > 0) {
                                                        group.attributes.visibilityConfig.initialValue =
                                                            ""
                                                    } else {
                                                        group.attributes.visibilityConfig.initialValue =
                                                            "hidden"
                                                    }
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        getIngredients(menuCategoryId, menuItem)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        this.onError(e)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    menuItemsCount++
                    if (menuItemsCount < menuItems.size) {
                        reorder()
                    } else {
                        menuItemsCount = 0
                        ordersView.showCart()
                    }
                }
            })
    }

    override fun getIngredients(menuCategoryId: String, menuItem: MenuItem) {
        MenuApiCall.getInstance().getCategoryItemIngredients(
            CacheUtil.getInstance().menuId,
            menuCategoryId,
            menuItem.id,
            "1",
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())

                        val ingredients = GsonHelper.gsonFinal().fromJson<LinkedList<Ingredient>>(
                            `object`.getJSONArray("data").toString(),
                            object : TypeToken<LinkedList<Ingredient?>?>() {
                            }.type
                        )

                        val removedIngredientIds = menuItems[menuItemsCount].ingredientsIds

                        for (ingredient in ingredients) {
                            if (removedIngredientIds.contains(ingredient.id)) {
                                ingredient.attributes.isSelected = false
                            }
                        }

                        menuItem.ingredients = ingredients

                        val store = CacheUtil.getInstance().orderStore
                        if (store == null || !menuItem.disabledStores.containsKey(store.id)) {
                            menuItem.attributes.isReorder = true
                            val cartCount = CacheUtil.getInstance().cartQuantity
                            removeMukafaaRedemption()
                            ordersView.logFirebaseEvent("add_to_cart_event")
                            CacheUtil.getInstance().addToCart(menuItem)
                            if (cartCount == 0) {
                                ordersView.setCartExpiry()
                            }
                        }

                        menuItemsCount++
                        if (menuItemsCount < menuItems.size) {
                            reorder()
                        } else {
                            menuItemsCount = 0
                            ordersView.showCart()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        this.onError(e)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    menuItemsCount++
                    if (menuItemsCount < menuItems.size) {
                        reorder()
                    } else {
                        menuItemsCount = 0
                        ordersView.showCart()
                    }
                }
            })
    }

    fun getModifierGroups(itemId: Int): ArrayList<ModifierGroup> {
        val appDatabase = AppDatabase.getInstance()
        val data = getMenuItemData(appDatabase, itemId.toString())
        if (data != null && !data.isEmpty()) {
            try {
                val applicationId = CacheUtil.getInstance().application.id
                val menuItemNew = MenuUtil.processMenuItemCustomObject(
                    GsonHelper.gsonFinal().fromJson(data, MenuItem::class.java)
                )
                val allergens = LinkedList<Allergen>()
                for (allergenDb in appDatabase.allergenDao().findAllByApplicationAndIds(
                    applicationId,
                    menuItemNew.allergensIds.toTypedArray<String>()
                )) {
                    val allergenCdn =
                        GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn::class.java)
                    allergens.add(
                        AllergenCdn.convert(
                            allergenCdn,
                            LocaleUtil.getInstance().language
                        )
                    )
                }
                Collections.sort(allergens) { allergen1, allergen2 -> allergen1.attributes.displayOrder - allergen2.attributes.displayOrder }
                menuItemNew.allergens = allergens

                val ingredients = LinkedList<Ingredient>()
                val ingredientIds = ArrayList<String>()
                for (ingredient in menuItemNew.ingredientIds) {
                    ingredientIds.add(ingredient.id)
                }
                for (ingredientDb in appDatabase.ingredientDao().findAllByApplicationAndIds(
                    applicationId,
                    ingredientIds.toTypedArray<String>()
                )) {
                    val ingredientCdn = GsonHelper.gsonFinal()
                        .fromJson(ingredientDb.data, IngredientCdn::class.java)
                    ingredients.add(
                        IngredientCdn.convert(
                            ingredientCdn,
                            LocaleUtil.getInstance().language
                        )
                    )
                }
                Collections.sort(ingredients) { ingredient1, ingredient2 -> ingredient1.attributes.displayOrder - ingredient2.attributes.displayOrder }
                menuItemNew.ingredients = ingredients

                var sizes: ModifierGroup? = ModifierGroup()
                val modifierGroups = LinkedList<ModifierGroup>()
                val modifierGroupIds = HashMap<String, ModifierGroup>()
                for (modifierGroup in menuItemNew.modifierGroupIds) {
                    modifierGroupIds[modifierGroup.id] = modifierGroup
                }
                for (modifierGroupDb in appDatabase.modifierGroupDao().findAllByApplicationAndIds(
                    applicationId,
                    modifierGroupIds.keys.toTypedArray<String>()
                )) {
                    val modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(
                        GsonHelper.gsonFinal()
                            .fromJson(modifierGroupDb.data, ModifierGroupCdn::class.java)
                    )
                    val modifierItems = LinkedList<ModifierItem>()
                    for (modifierItemDb in appDatabase.modifierItemDao().findAllByApplicationAndIds(
                        applicationId,
                        modifierGroupCdn.attributes.modifiers.toTypedArray<String>()
                    )) {
                        val modifierItemCdn = GsonHelper.gsonFinal()
                            .fromJson(modifierItemDb.data, ModifierItemCdn::class.java)
                        val modifierItem = ModifierItemCdn.convert(
                            modifierItemCdn,
                            LocaleUtil.getInstance().language
                        )
                        modifierItem.attributes.isSelected = modifierItem.attributes.minimum == 1
                        modifierItems.add(modifierItem)
                    }
                    Collections.sort(modifierItems) { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                    val modifierGroup = ModifierGroupCdn.convert(
                        modifierGroupCdn,
                        LocaleUtil.getInstance().language
                    )
                    val mapModifierGroup = modifierGroupIds[modifierGroup.id]
                    if (mapModifierGroup != null) {
                        modifierGroup.attributes.minimum = mapModifierGroup.min
                        modifierGroup.attributes.maximum = mapModifierGroup.max
                        modifierGroup.excludeModifiers =
                            ArrayList(mapModifierGroup.excludeModifiers)
                        modifierGroup.setDefaultModifiers(ArrayList(mapModifierGroup.defaultModifiers))
                    }
                    val size = modifierItems.size
                    val modifierItemsFinal = ArrayList<ModifierItem>()
                    for (i in 0 until size) {
                        var found = false
                        for (a in modifierGroup.excludeModifiers.indices) {
                            if (modifierItems[i].id.equals(
                                    modifierGroup.excludeModifiers[a].toString(),
                                    ignoreCase = true
                                )
                            ) {
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            modifierItemsFinal.add(modifierItems[i])
                        }
                    }
                    modifierGroup.modifierItems = modifierItems
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.value) {
                        sizes = modifierGroup
                    } else {
                        modifierGroups.add(modifierGroup)
                    }
                }
                menuItemNew.sizes = sizes
                Collections.sort(modifierGroups) { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }
                menuItemNew.modifierGroups = modifierGroups

                val filteredModifierGroups = LinkedList<ModifierGroup>()

                for (modifierGroup in menuItemNew.modifierGroups) {
                    if (modifierGroup.modifierItems.size > 0) {
                        filteredModifierGroups.add(modifierGroup)
                    }
                }

                Collections.sort(filteredModifierGroups) { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }

                if (menuItemNew.sizes.modifierItems.size > 0) {
                    menuItemNew.sizes.modifierItems[0].attributes.isSelected = true
                }

                menuItemNew.modifierGroups = filteredModifierGroups

                for (modifierGroup in menuItemNew.modifierGroups) {
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

                var visibilityConfig = menuItemNew.sizes.attributes.visibilityConfig
                if (visibilityConfig != null) {
                    val rules = visibilityConfig.rules
                    if (rules != null && rules.size > 0) {
                        val rule = rules[0]
                        if (rule.targetType != null && rule.targetType.equals(
                                "modifier-group",
                                ignoreCase = true
                            )
                        ) {
                            for (modGroup in menuItemNew.modifierGroups) {
                                if ((menuItemNew.sizes.id != modGroup.id) && rule.targetId == modGroup.id) {
                                    if (rule.targetIs != null && rule.targetIs.equals(
                                            "selected",
                                            ignoreCase = true
                                        )
                                    ) {
                                        var selected = 0
                                        for (modItem in modGroup.modifierItems) {
                                            if (modItem.attributes.isSelected) {
                                                selected += 1
                                            }
                                        }
                                        if (selected > 0) {
                                            menuItemNew.sizes.attributes.visibilityConfig.initialValue =
                                                ""
                                        } else {
                                            menuItemNew.sizes.attributes.visibilityConfig.initialValue =
                                                "hidden"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (group in menuItemNew.modifierGroups) {
                    visibilityConfig = group.attributes.visibilityConfig
                    if (visibilityConfig != null) {
                        val rules = visibilityConfig.rules
                        if (rules != null && rules.size > 0) {
                            val rule = rules[0]
                            if (rule.targetType != null) {
                                if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
                                    if ((group.id != menuItemNew.sizes.id) && rule.targetId == menuItemNew.sizes.id) {
                                        if (rule.targetIs != null && rule.targetIs.equals(
                                                "selected",
                                                ignoreCase = true
                                            )
                                        ) {
                                            var selected = 0
                                            for (modItem in menuItemNew.sizes.modifierItems) {
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
                                    }

                                    for (modGroup in menuItemNew.modifierGroups) {
                                        if (group.id != modGroup.id && rule.targetId == modGroup.id) {
                                            if (rule.targetIs != null && rule.targetIs.equals(
                                                    "selected",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                var selected = 0
                                                for (modItem in modGroup.modifierItems) {
                                                    if (modItem.attributes.isSelected) {
                                                        selected += 1
                                                    }
                                                }
                                                if (selected > 0) {
                                                    group.attributes.visibilityConfig.initialValue =
                                                        ""
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
                                    for (modItem in menuItemNew.sizes.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                            break
                                        }
                                    }

                                    for (modGroup in menuItemNew.modifierGroups) {
                                        if (group.id != modGroup.id) {
                                            for (modItem in modGroup.modifierItems) {
                                                if (modItem.attributes.isSelected) {
                                                    selectedModifiers.add(modItem.id)
                                                }
                                            }
                                        }
                                    }

                                    //for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                    //if (!group.getId().equals(modGroup.getId())) {
                                    if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                        if (rule.targetIs != null && rule.targetIs.equals(
                                                "selected",
                                                ignoreCase = true
                                            )
                                        ) {
                                            group.attributes.visibilityConfig.initialValue = ""
                                        } else {
                                            group.attributes.visibilityConfig.initialValue =
                                                "hidden"
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
                return ArrayList(menuItemNew.modifierGroups)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ArrayList()
    }

    override fun isMukafaaEnabled(): Boolean {
        var isMukafaaEnabledInApp = false
        val app = CacheUtil.getInstance().application
        isMukafaaEnabledInApp = if (CacheUtil.getInstance().orderType == OrderType.DELIVERY) {
            app.attributes.allowedPaymentsDeliver.contains("alrajhi")
        } else {
            app.attributes.allowedPaymentsPickup.contains("alrajhi")
        }
        return isMukafaaEnabledInApp
    }

    override fun removeMukafaaRedemption() {
        val transactionId = CacheUtil.getInstance().mukafaaTransactionId
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa()
            CustomersApiCall.getInstance().deleteRedemption(
                Values.MUKAFAA_PROVIDER,
                transactionId,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                    }

                    override fun onError(o: Any) {
                    }
                })
        }
    }

    override fun getFromAbly(): Boolean {
        return fromAbly
    }

    override fun setFromAblyToFalse() {
        fromAbly = false
    }

    private var transactions: ArrayList<LoyaltyPointsTransaction>? = null

    override fun getMukafaaTransaction(): LoyaltyPointsTransaction? {
        transactions?.let { transactions ->
            for (transaction in transactions) {
                if (transaction.attributes != null && transaction.attributes.provider != null && Values.MUKAFAA_PROVIDER.equals(
                        transaction.attributes.provider,
                        ignoreCase = true
                    )
                ) {
                    return transaction
                }
            }
        }
        return null
    }

    override fun getLoyaltyPointsTransaction(): ArrayList<LoyaltyPointsTransaction>? {
        return transactions
    }

    override fun getSoloLoyaltyTransaction(): LoyaltyPointsTransaction? {
        transactions?.let { transactions ->
            for (transaction in transactions) {
                if ((transaction.attributes != null) && transaction.attributes.provider != null && Values.LOYALTY_PROVIDER.equals(
                        transaction.attributes.provider,
                        ignoreCase = true
                    ) && transaction.attributes.type.equals("redeem", ignoreCase = true)
                ) {
                    return transaction
                }
            }
        }
        return null
    }

    override fun getOrderItems(): List<MenuItem> {
        return menuItems
    }

    override fun getOrderHistories(): ArrayList<OrderHistory> {
        return orders
    }

    override fun setOrders(orders: ArrayList<OrderHistory>) {
        this.orders = orders
    }

    override fun cancelDriveLocationUpdates() {
        coroutinePoller?.cancel()
        coroutinePoller = null
    }

    private fun subscribeToOrderStatusUpdate(orderId: String) {
        val ablyUtil = instance
        ablyUtil.subscribeToOrderStatusUpdate(orderId) { message: Message ->
            this@OrdersPresenter.onOrderDetailsStatusUpdate(
                message
            )
        }
    }

    private fun subscribeToOrderDriverAssign(orderId: String) {
        order?.let { order ->
            if (order.deliveryOption.equals(
                    "deliver",
                    ignoreCase = true
                ) || order.deliveryOption.equals("ايصال", ignoreCase = true)
            ) {
                val ablyUtil = instance
                val driverId = order.driverId
                if (driverId == null || driverId.isEmpty()) {
                    ablyUtil.subscribeToOrderDriverAssign(orderId) { message: Message ->
                        this@OrdersPresenter.onOrderDriverAssign(
                            message
                        )
                    }
                } else {
                    subscribeToOrderDriverLocationUpdate(orderId)
                    setLogisticsPolling(orderId, order.orderStatus.sequence)
                }
            }
        }

    }

    private fun subscribeToOrderDriverLocationUpdate(orderId: String) {
        isLocationUpdateSubscribed = true
        val ablyUtil = instance
        ablyUtil.subscribeToOrderDriverLocationUpdate(orderId) { message: Message ->
            this@OrdersPresenter.onOrderDriverLocationUpdate(
                message
            )
        }
    }

    private fun setLogisticsPolling(orderId: String, orderStatus: Int) {
        if (lyvePollingEnable && coroutinePoller == null
            && orderStatus >= OrderStatusType.DELIVERY_IN_PROGRESS.value
            && orderStatus < OrderStatusType.FINISHED.value
        ) {
            coroutinePoller = CoroutinePoller(Dispatchers.IO) {
                sendLogisticsPoll(orderId)
            }
            val flow = coroutinePoller?.poll(pollingValue * 1000L)
            GlobalScope.launch(Dispatchers.IO) {
                flow?.collectLatest { it.invoke() }
            }
        }
    }

    private fun onOrderDetailsStatusUpdate(message: Message) {
        fromAbly = true
        getOrderStatuses()
        order?.let { order ->
            getOrder(order.id, -1, true, false)
            try {
                val jsonObject = JSONObject(message.data.toString())
                val orderObject = jsonObject.optJSONObject("order")
                if (orderObject != null) {

                    val currentStatusSequence = when {
                        orderObject.has("current-status") -> {
                            val currentStatus = orderObject.getJSONObject("current-status")
                            if (currentStatus.has("sequence")) {
                                currentStatus.getInt("sequence")
                            } else {
                                0
                            }
                        }

                        else -> 0
                    }
                    val lyveDriverObject = orderObject.optJSONObject("lyve-driver")
                    if (lyveDriverObject != null) {
                        val lyveDriver =
                            Gson().fromJson(lyveDriverObject.toString(), LyveDriver::class.java)
                        if (lyveDriver != null) {
                            val bearing = lyveDriver.bearing
                            driver = lyveDriver
                            ordersView.showWhatsApp(driver?.mobile ?: "")
                            if (bearing != null) {
                                val lat = bearing.lat
                                val lng = bearing.lng
                                if ((lat != null) && !lat.isEmpty() && (lng != null) && !lng.isEmpty()) {
                                    val customerLatLng = LatLng(
                                        order.customerLat, order.customerLng
                                    )
                                    val driverLatLng = LatLng(lat.toDouble(), lng.toDouble())
                                    ordersView.showDriver(customerLatLng, driverLatLng, bearing.eta)
                                    if (!isLocationUpdateSubscribed) {
                                        subscribeToOrderDriverLocationUpdate(order.id)
                                        setLogisticsPolling(order.id, currentStatusSequence)
                                    }
                                }
                            } else {
                                getDriver()
                            }
                        } else {
                            getDriver()
                        }
                    } else {
                        getDriver()
                    }
                } else {
                    getDriver()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                getDriver()
            }
        }
    }

    private fun onOrderDriverAssign(message: Message) {
        order?.let { order ->
            fromAbly = true
            val ablyUtil = instance
            ablyUtil.detachToOrderDriverAssign(order.id)
            getDriver()
        }
    }

    private fun sendLogisticsPoll(orderId: String) {
        OrdersApiCall.getInstance().sendLogisticsPoll(orderId, object : ApiRequestListener {
            override fun onSuccess(obj: Any) {
            }

            override fun onError(error: Any) {
            }
        })
    }

    private fun onOrderDriverLocationUpdate(message: Message) {
        try {
            val jsonObject = JSONObject(message.data.toString())
            val orderObject = jsonObject.optJSONObject("order")
            if (orderObject != null) {
                val currentStatusObject = orderObject.optJSONObject("current-status")
                if (currentStatusObject != null && currentStatusObject.has("sequence") && !currentStatusObject.isNull(
                        "sequence"
                    )
                ) {
                    val sequence = currentStatusObject.optInt("sequence", -1)
                    if (((sequence != -1) && sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value) && sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                        val appDriverObject = JSONObject(message.data.toString())
                        val appDriver =
                            Gson().fromJson(appDriverObject.toString(), DeliveryDriver::class.java)
                        ordersView.updateDriver(appDriver)
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return Unit
    }

    override fun getNewFusedLocation(location: Location) {
        hasLocation = true
        fusedLocationProvider?.disconnect()
        this.location = location
        CacheUtil.getInstance().orderLatitude = location.latitude.toString()
        CacheUtil.getInstance().orderLongitude = location.longitude.toString()
        CacheUtil.getInstance().orderDeliveryLatitude = location.latitude.toString()
        CacheUtil.getInstance().orderDeliveryLongitude = location.longitude.toString()
        val latLng = LatLng(location.latitude, location.longitude)
        ordersView.showLocation(false, latLng, getAddressLoc(latLng))
    }


    override fun setGeocoder(geocoder: Geocoder) {
        this.geocoder = geocoder
    }

    private var geocoder: Geocoder? = null

    private var addressNew: android.location.Address? = null

    private var place: Place? = null

    init {
        ordersView.setPresenter(this)
    }

    private fun getAddressLoc(latLng: LatLng): String {
        if (geocoder != null) {
            try {
                val addresses = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
                addressNew = (if (addresses.isNullOrEmpty()) null else addresses[0])
                if (addressNew != null) {
                    place = null
                    return addressNew!!.getAddressLine(0)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ""
    }


}
