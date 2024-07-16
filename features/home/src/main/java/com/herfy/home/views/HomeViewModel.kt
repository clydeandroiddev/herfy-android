package com.herfy.home.views

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.herfy.database.entities.MenuItemEntity
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.model.CampaignToAdd
import com.herfy.model.ErrorMsgItem
import com.herfy.model.PromotionCategory
import com.herfy.model.TierProgress
import com.herfy.repository.CmsRepository
import com.herfy.repository.HomeRepository
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.handler.ErrorHandler
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.Event
import com.herfy.utils.helper.ably.AblyUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.OrderData
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ably.lib.types.Message
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val cmsRepository: CmsRepository,
    private val dispatchers: DispatcherProvider
) : BaseViewModel(), PromotionsAdapter.PromotionListener, SliderImageAdapter.SliderListener,
    PromotionCategoryAdapter.PromotionCategoryListener, PromotionDetailBottomSheet.onAction {

    private val _showError = MutableLiveData<Event<ErrorMsgItem>>()
    val showError: LiveData<Event<ErrorMsgItem>> = _showError

    private val _slideList = MutableLiveData<ArrayList<Slide>>()
    val slideList: LiveData<ArrayList<Slide>> = _slideList

    private val _caterList = MutableLiveData<ArrayList<Slide>>()
    val caterList: LiveData<ArrayList<Slide>> = _caterList

    private val _promotionsList = MutableLiveData<List<Campaign>>()
    val promotionsList: LiveData<List<Campaign>> = _promotionsList

    private val _promotionCategoryList = MutableLiveData<List<PromotionCategory>>()
    val promotionCategoryList: LiveData<List<PromotionCategory>> = _promotionCategoryList

    private val _showLink = MutableLiveData<Event<Uri>>()
    val showLink: LiveData<Event<Uri>> = _showLink

    private val _showMenuCategory = MutableLiveData<Event<String>>()
    val showMenuCategory: LiveData<Event<String>> = _showMenuCategory

    private val _showMenuItem = MutableLiveData<Event<Pair<String, String>>>()
    val showMenuItem: LiveData<Event<Pair<String, String>>> = _showMenuItem

    private val _showPromo = MutableLiveData<Event<Campaign>>()
    val showPromo: LiveData<Event<Campaign>> = _showPromo

    private val menuCategories: ArrayList<MenuCategory> = ArrayList()
    var excludedMenu: ArrayList<MenuCategory> = ArrayList()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()

    private val _loyalty = MutableLiveData<Event<Loyalty?>>()
    val loyalty: LiveData<Event<Loyalty?>> = _loyalty

    private val _updatePromoCategoryList = MutableLiveData<Event<PromotionCategory>>()
    val updatePromoCategoryList: LiveData<Event<PromotionCategory>> = _updatePromoCategoryList

    private val _orderTypes = MutableLiveData<Set<OrderType>>()
    val orderTypes: LiveData<Set<OrderType>> = _orderTypes


    private val _tiers = MutableLiveData<Event<List<Tier>>>()
    val tiers: MutableLiveData<Event<List<Tier>>> = _tiers


    private val _tier = MutableLiveData<Tier?>()
    val tier: MutableLiveData<Tier?> = _tier

    private val _totalPointsAccumulated = MutableLiveData<Int>()
    val totalPointsAccumulated: MutableLiveData<Int> = _totalPointsAccumulated

    private val _pointsToNextTier = MutableLiveData<Int>()
    val pointsToNextTier: MutableLiveData<Int> = _pointsToNextTier

    private val _startPoints = MutableLiveData<Int>()
    val startPoints: MutableLiveData<Int> = _startPoints

    private val _promoFilteredList = MutableLiveData<List<Campaign>?>()
    val promoFilteredList: LiveData<List<Campaign>?> = _promoFilteredList
    private var allPromotionsList: List<Campaign>? = null

    private val _bottomSlideList = MutableLiveData<ArrayList<Slide>>()
    val bottomSlideList: LiveData<ArrayList<Slide>> = _bottomSlideList

    private val _showCart = MutableLiveData<Event<Boolean>>()
    val showCart: LiveData<Event<Boolean>> = _showCart

    private val _showOrderTrackerNotif = MutableLiveData<Event<OrderDetails>>()
    val showOrderTrackerNotif: LiveData<Event<OrderDetails>> = _showOrderTrackerNotif

    private val _updateOrderTrackerNotif = MutableLiveData<OrderData>()
    val updateOrderTrackerNotif: LiveData<OrderData> = _updateOrderTrackerNotif

    private val _tierProgress = MutableLiveData<Event<TierProgress>>()
    val tierProgress: LiveData<Event<TierProgress>> = _tierProgress

    private val _catering = MutableLiveData<Event<String?>>()
    val catering: LiveData<Event<String?>> = _catering

    private val _showMenu = MutableLiveData<Event<Boolean>>()
    val showMenu : LiveData<Event<Boolean>> = _showMenu

    private val _showPromotionsError = MutableLiveData<Event<ErrorMsgItem>>()
    val showPromotionsError : LiveData<Event<ErrorMsgItem>> = _showPromotionsError

    fun getSliders() {
        invoke {
            val sliderId = CacheUtil.getInstance().getSliderId(Values.HOME_PAGE_TOP_BANNER_LABEL)
            if (!sliderId.isNullOrEmpty()) {
                homeRepository.getSliders(sliderId) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let {
                            it.cast<ArrayList<Slide>> {
                                _slideList.value = it
                            }
                        } ?: run {
                            _slideList.value = arrayListOf()
                        }
                    } else {
                        _slideList.value = arrayListOf()
                    }
                }
            } else {
                _slideList.value = arrayListOf()
            }
        }
    }

    fun getCaterSlider() {
        invoke {
            val sliderId = CacheUtil.getInstance().getCaterHerfyBannerId(Values.CATER_HERFY_BANNER)
            if (!sliderId.isNullOrEmpty()) {
                homeRepository.getSliders(sliderId) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let {
                            it.cast<ArrayList<Slide>> {
                                _caterList.value = it
                            }
                        } ?: run {
                            _caterList.value = arrayListOf()
                        }
                    } else {
                        _caterList.value = arrayListOf()
                    }
                }
            } else {
                _caterList.value = arrayListOf()
            }
        }
    }

    fun getCustomerOrder() {
        if (AuthUtil.getInstance().isSignedIn) {
            invoke {
                homeRepository.getCustomerOrder(AuthUtil.getInstance().customer.id) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.cast<String> { orderId ->
                            if (orderId.isNotEmpty()) {
                                getLastOrder()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getLastOrder() {
        if (AuthUtil.getInstance().isSignedIn) {
            val lastOrder = CacheUtil.getInstance().lastOrderId
            invoke {
                homeRepository.getLastOrder(lastOrder) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.cast<OrderDetails> { orderDetails ->
                            if (orderDetails.orderStatus.sequence < OrderStatusType.FINISHED.value) {
                                val ablyUtil: AblyUtil = AblyUtil.Companion.instance
                                ablyUtil.subscribeToOrderStatusUpdate(orderDetails.id) { message ->
                                    this@HomeViewModel.onOrderStatusUpdate(message)
                                }
                                _showOrderTrackerNotif.value = Event(orderDetails)
                            } else {
                                CacheUtil.getInstance().promisedTime = ""
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onOrderStatusUpdate(message: Message) {
        try {
            val jsonObject = JSONObject(message.data.toString())
            val orderData = OrderData.parse(jsonObject.getJSONObject("order"))
            _updateOrderTrackerNotif.postValue(orderData)
            if (orderData.orderStatus.sequence >= OrderStatusType.FINISHED.value) {
                detachAbly()
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun detachAbly() {
        AblyUtil.Companion.instance.detachAll()
    }

    fun getBottomSliders() {
        invoke {
            val sliderId = CacheUtil.getInstance().getSliderId(Values.HOME_PAGE_BOTTOM_BANNER_LABEL)
            if (!sliderId.isNullOrEmpty()) {
                homeRepository.getSliders(sliderId) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.cast<ArrayList<Slide>> {
                            _bottomSlideList.value = it
                        } ?: run {
                            _bottomSlideList.value = arrayListOf()
                        }
                    } else {
                        _bottomSlideList.value = arrayListOf()
                    }
                }
            } else {
                _bottomSlideList.value = arrayListOf()
            }
        }
    }

    fun getOrderTypes() {
        invoke {
            homeRepository.getOrderTypes { res ->
                res.data?.cast<Set<OrderType>> { orderTypes ->
                    val types = hashSetOf<OrderType>()
                    types.addAll(orderTypes)
                    if(types.contains(OrderType.PICKUP) && types.contains(OrderType.CURBSIDE)) {
                        types.remove(OrderType.CURBSIDE)
                    }
                    _orderTypes.postValue(types)
                }
            }
        }
    }

    fun getPromotions() {
        invoke {
            homeRepository.getCampaignCancellableAll { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.let {
                        it.cast<ArrayList<Campaign>> {
                            _promotionsList.value = it
                            allPromotionsList = it
                        }
                    } ?: run {
                        _promotionsList.value = arrayListOf()
                    }
                } else {
                    _promotionsList.value = arrayListOf()
                    //_showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }

    private fun excludedMenuCategories(menuCategories: List<MenuCategory>): List<MenuCategory> {
        excludedMenu.clear()
        val orderStore = homeRepository.getOrderStore()
        if (orderStore != null && !excludedCategories.isEmpty() && excludedCategories.containsKey(
                orderStore.id
            )
        ) {
            val categories: MutableList<MenuCategory> = java.util.ArrayList(menuCategories)
            categories.removeAll(excludedCategories[orderStore.id]!!)
            excludedMenu.addAll(categories)
        } else {
            excludedMenu.addAll(menuCategories)
        }
        excludedMenu.sortWith { menuCategory1, menuCategory2 -> menuCategory1.displayOrder - menuCategory2.displayOrder }
        return excludedMenu
    }

    fun getMenuItemWith(menuItemId: String) {
        invokeAtBackground {
            homeRepository.getMenuItemWith(menuItemId) { homeResponse ->
                homeResponse.data?.cast<MenuItemEntity> { menuItemEntity ->
                    val categoryId = menuItemEntity.menuCategoryId
                    if (categoryId.isNotEmpty()) {
                        _showMenuItem.value = Event(Pair(categoryId, menuItemEntity.id))
                    }
                }
            }
        }
    }

    override fun onSelectedPromo(promo: Campaign) {
        _showPromo.value = Event(promo)
    }

    override fun onSelectedImage(slide: Slide) {
        slide.linkValue?.let {
            if (it.isNotEmpty()) {
                if (slide.linkType == "url") {
                    _showLink.value = Event(Uri.parse(slide.linkValue))
                } else if (slide.linkType == "category") {
                    _showMenuCategory.value = Event(slide.linkValue)
                } else if (slide.linkType == "item") {
                    getMenuItemWith(slide.linkValue)
                }
            }
        }
    }

    fun getMenuCategory() {
        invokeAtBackground {
            val orderStore = homeRepository.getOrderStore()
            val storeSelected = orderStore != null
            homeRepository.getMenuCategories { homeResponse ->
                homeResponse.data?.let { data ->
                    data.cast<ArrayList<MenuCategory>> {
                        for (category in it) {
                            if (category.excludedLocations.isNotEmpty()) {
                                for (excluded in category.excludedLocations) {
                                    if (excludedCategories.containsKey(excluded)) {
                                        excludedCategories[excluded]!!.add(category)
                                    } else {
                                        val set = java.util.HashSet<MenuCategory>()
                                        set.add(category)
                                        excludedCategories[excluded!!] = set
                                    }
                                }
                            }
                            if (category.getEnabled() == 1) {
                                invokeAtBackground {
                                    homeRepository.getMenuItemBy(category.id) { response ->
                                        response.data?.cast<List<MenuItemEntity>> { menuItemDbs ->
                                            if (menuItemDbs.isNotEmpty()) {
                                                category.menuItems = menuItemDbs.toDomain()
                                                menuCategories.add(category)
                                                if (!storeSelected || !category.excludedLocations.contains(
                                                        orderStore?.id ?: ""
                                                    )
                                                ) {
                                                    excludedMenu.add(category)
                                                }
                                            }
                                        }
                                        excludedMenu.sortWith { menuCategory1, menuCategory2 -> menuCategory1.displayOrder - menuCategory2.displayOrder }
                                        val hashSet = java.util.HashSet(menuCategories)
                                        menuCategories.clear()
                                        menuCategories.addAll(hashSet)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    fun getLoyalty() {
        invoke {
            AuthUtil.getInstance().customerLoyalty?.let { loyalty ->
                AuthUtil.getInstance().customerLoyaltyTier?.let {
                    _tierProgress.value = Event(
                        TierProgress(
                            it,
                            CacheUtil.getInstance().totalAccumulatedPoints,
                            CacheUtil.getInstance().pointsUntilNextTier,
                            CacheUtil.getInstance().startingPointsOfNextTier,
                            CacheUtil.getInstance().pointsUntilNextTier == 0,
                            loyalty
                        )
                    )
                }
                _loyalty.value = Event(loyalty)
            }

            Single.zip<Loyalty, List<Tier>, Pair<Loyalty, List<Tier>>>(
                getLoyaltyObservable(), getTiersObservable()
            ) { loyalty: Loyalty, tiers: List<Tier> ->
                Pair(loyalty, tiers)
            }.subscribe({ pair: Pair<Loyalty, List<Tier>> ->
                var tiers = pair.second
                val loyalty = pair.first
                val totalPointsAccumulated = loyalty.attributes.currentBalance
                var eligibleTiers = Observable.fromIterable(tiers)
                    .filter { tier: Tier -> totalPointsAccumulated <= tier.endPoint }.toList()
                    .blockingGet()
                var pointsToNextTier = 0
                if (loyalty.attributes.loyaltyTier == null){
                    val tier = tiers[0]
                    val loyaltyTier = LoyaltyTier()
                    loyaltyTier.name = tier.name
                    loyaltyTier.id = tier.id
                    loyaltyTier.imageUrl = tier.imageUrl
                    loyalty.attributes.loyaltyTier = loyaltyTier
                }

                _loyalty.value = Event(loyalty)
                _tiers.value = Event(tiers)
                AuthUtil.getInstance().customerLoyalty = loyalty

                var nextTier: Tier? = null
                if (eligibleTiers.size <= 0 && tiers.isNotEmpty()) {
                    eligibleTiers = ArrayList()
                    eligibleTiers.add(tiers[tiers.size - 1])
                }
                if (eligibleTiers.size >= 1) {
                    nextTier = eligibleTiers[0]
                }
                if (eligibleTiers.size > 1) {
                    nextTier = eligibleTiers[1]
                    pointsToNextTier = nextTier.startPoint - loyalty.attributes.currentBalance
                }
                var isMaxTier = false
                var startPoints = 0
                if (nextTier != null) {
                    startPoints = nextTier.startPoint
                    if (loyalty.attributes.loyaltyTier != null) {
                        isMaxTier = nextTier.name == loyalty.attributes.loyaltyTier.name
                    }
                    if (isMaxTier) {
                        pointsToNextTier = 0
                    }
                }
                _tierProgress.value = Event(
                    TierProgress(
                        nextTier,
                        totalPointsAccumulated,
                        pointsToNextTier,
                        startPoints,
                        isMaxTier,
                        loyalty
                    )
                )


            }) { error: Throwable? ->
                val loyalty = Loyalty()
                val loyaltyAttributes = LoyaltyAttributes()
                val loyaltyTier = LoyaltyTier()
                loyaltyTier.name = "Classic"
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                val currentTier = Tier()
                currentTier.name = "Classic"
                currentTier.startPoint = 0
                val nextTier = Tier()
                nextTier.name = "Silver"
                nextTier.startPoint = 100
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                _tiers.value = Event(ArrayList<Tier>(listOf(currentTier)))
                loyalty.attributes = loyaltyAttributes
                _tierProgress.value = Event(
                    TierProgress(
                        nextTier,
                        0,
                        100,
                        100,
                        false,
                        loyalty
                    )
                )
            }
        }
    }

    /* Remove Single call for loyalty and tiers
    /*// Ensures that there will always be a tier when loyalty is shown
    Single.zip<List<Tier>, Loyalty, android.util.Pair<Loyalty, List<Tier>>>(
        getTiersObservable(), getLoyaltyObservable()
    ) { tiers: List<Tier>, loyalty: Loyalty ->
        android.util.Pair<Loyalty, List<Tier>>(loyalty, tiers)
    }.subscribe({ pair: android.util.Pair<Loyalty, List<Tier>> ->
        var tiers = pair.second
        val loyalty = pair.first
        this._tiers.value = tiers
        val totalPointsAccumulated = loyalty.attributes.currentBalance
        var eligibleTiers = Observable.fromIterable(tiers)
            .filter { tier: Tier -> totalPointsAccumulated <= tier.endPoint }.toList()
            .blockingGet()
        var pointsToNextTier = 0
        _loyalty.value = loyalty
        var nextTier: Tier? = null
        if (eligibleTiers.size <= 0 && tiers.size >= 1) {
            eligibleTiers = java.util.ArrayList()
            eligibleTiers.add(tiers[tiers.size - 1])
        }
        if (eligibleTiers.size >= 1) {
            nextTier = eligibleTiers[0]
        }
        if (eligibleTiers.size > 1) {
            nextTier = eligibleTiers[1]
            pointsToNextTier = nextTier.startPoint - loyalty.attributes.currentBalance
        }
        var startPoints = 0
        if (nextTier != null) {
            startPoints = nextTier.startPoint
        }

        _tier.value = nextTier
        _totalPointsAccumulated.value = totalPointsAccumulated
        _pointsToNextTier.value = pointsToNextTier
        _startPoints.value = startPoints

    }) { error: Throwable? ->
        val loyalty = Loyalty()
        val loyaltyAttributes = LoyaltyAttributes()
        val loyaltyTier = LoyaltyTier()
        loyaltyTier.name = "Classic"
        loyaltyAttributes.loyaltyTier = loyaltyTier
        loyaltyAttributes.currentBalance = 0
        val currentTier = Tier()
        currentTier.name = "Classic"
        currentTier.startPoint = 0
        val nextTier = Tier()
        nextTier.name = "Silver"
        nextTier.startPoint = 100
        loyaltyAttributes.loyaltyTier = loyaltyTier
        loyaltyAttributes.currentBalance = 0
        _tiers.value = java.util.ArrayList<Tier>(Arrays.asList<Tier>(currentTier))
        loyalty.attributes = loyaltyAttributes
        _tier.value = nextTier
        _totalPointsAccumulated.value = 0
        _pointsToNextTier.value = 100
        _startPoints.value = 100

    }*/*/


    private fun getTiersObservable(): Single<List<Tier>?>? {
        return Single.create<List<Tier>?> { source: SingleEmitter<List<Tier>?> ->

            invoke {
                homeRepository.getLoyaltyTiers { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let { it ->
                            it.cast<ArrayList<Tier>> {
                                CacheUtil.getInstance().setLoyaltyTiers(it)
                                source.onSuccess(it)
                            }

                        }
                    } else {
                        //_showError.value = Event(ErrorMsgItem(true, homeResponse.message))
                    }
                }
            }
        }
    }

    fun getLoyaltyObservable(): Single<Loyalty?>? {
        return Single.create<Loyalty?> { source: SingleEmitter<Loyalty?> ->
            invoke {
                homeRepository.getCustomerLoyalty(AuthUtil.getInstance().customer.id) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let { it ->
                            it.cast<Loyalty> {
                                AuthUtil.getInstance().customerLoyalty = it
                                source.onSuccess(it)
                            }
                        } ?: run {
                            _loyalty.value = Event(null)
                        }
                    } else {
                        _loyalty.value = Event(null)
                        //_showError.value = Event(ErrorMsgItem(true, homeResponse.message))
                    }
                }
            }
        }
    }


    override fun selectedCategory(category: PromotionCategory) {
        category.isSelected = true
        _updatePromoCategoryList.value = Event(category)
        if (category.orderType == OrderType.NONE) {
            allPromotionsList?.let {
                _promoFilteredList.value = it
            } ?: run {
                _promoFilteredList.value = null
            }
        } else {
            getPromotionsWith(category.orderType)
        }
    }

    private fun getPromotionsWith(orderType: OrderType) {
        val category = when (orderType) {
            OrderType.NONE -> ""
            OrderType.PICKUP -> "pickup"
            OrderType.DINE_IN -> "eat-in"
            OrderType.DELIVERY -> "deliver"
            OrderType.CURBSIDE -> "curbside"
            OrderType.DRIVE_THRU -> "pickup"
        }


        allPromotionsList?.let { promos ->
            val filteredList = arrayListOf<Campaign>()
            if (promos.isNotEmpty() && category.isNotEmpty()) {

                promos.filter { promo ->
                    promo.promotion.attributes.rules.isNotEmpty() && promo.promotion.attributes.rules.find { rule ->
                        rule.ruleKey.equals(
                            "order-type", true
                        ) && rule.ruleValue?.replace("in:", "")?.split(",")?.find { t ->
                            if (category.equals(
                                    "pickup", ignoreCase = true
                                )
                            ) (t.trim() == "pickup" || t.trim() == "to-go" || t.trim() == "drive-thru") else t.trim()
                                .equals(category, true)
                        } != null
                    } != null
                }.let { promosWithRules ->
                    filteredList.addAll(promosWithRules)
                }
                _promoFilteredList.value = filteredList
            } else {
                _promoFilteredList.value = null
            }
        }
    }

    fun getPromotionCategoryList() {
        val cacheUtil = CacheUtil.getInstance()
        invoke {
            homeRepository.getOrderTypes { res ->
                res.data?.cast<Set<OrderType>> { orderTypes ->
                    val promocateglist = mutableListOf<PromotionCategory>()
                    promocateglist.add(
                        PromotionCategory(
                            cacheUtil.getTranslations(
                                Text.ALL_OFFERS, "All Offers"
                            ), true, OrderType.NONE
                        )
                    )
                    orderTypes.find { type -> type == OrderType.PICKUP }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.PICKUP, "Pickup"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.CURBSIDE }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.CURBSIDE_LABEL, "Curbside"
                                ), false, it
                            )
                        )
                    }
                    orderTypes.find { type -> type == OrderType.DRIVE_THRU }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.DRIVE_THRU, "Drive Thru"
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
                    orderTypes.find { type -> type == OrderType.DELIVERY }?.let {
                        promocateglist.add(
                            PromotionCategory(
                                cacheUtil.getTranslations(
                                    Text.DELIVERY, "Delivery"
                                ), false, it
                            )
                        )
                    }
                    _promotionCategoryList.value = promocateglist
                }
            }
        }
    }

    override fun onClickClaimPromo(promo: Campaign) {
        if (promo.promotion != null && promo.promotion.attributes.type.equals(
                "item", ignoreCase = true
            )
        ) {
            getCampaignPromotionItem(promo)
        }else {
            validateDigitalCoupon(promo)
        }

    }

    override fun onRemovePromo() {
        _showCart.value = Event(false)
    }

    override fun onShowMenu() {
        _showMenu.value = Event(true)
    }

    private fun getCampaignPromotionItem(digitalCoupon: Campaign?) {
        invoke {
            digitalCoupon?.promotion?.let { promotion ->
                MenuApiCall.getInstance()
                    .getItemDetails(promotion.attributes.itemId, object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            try {
                                val response = JSONObject(o.toString())
                                val dataArray = response.getJSONArray("data")
                                if (dataArray.length() > 0) {
                                    val menuItem = GsonHelper.gsonFinal().fromJson(
                                        dataArray.getJSONObject(0).toString(), MenuItem::class.java
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
                                    MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                    CacheUtil.getInstance().setCampaign(digitalCoupon)
                                    CacheUtil.getInstance()
                                        .setCampaignPromotion(digitalCoupon.promotion)
                                    CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem)
                                    _showCart.value = Event(true)
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
                                    _showPromotionsError.value = Event(ErrorMsgItem(true, detail))
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    _showPromotionsError.value = Event(
                                        ErrorMsgItem(
                                            true, CacheUtil.getInstance().getTranslations(
                                                Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                                "You are not eligible to claim the promotion."
                                            )
                                        )
                                    )
                                }
                            } else {
                                _showPromotionsError.value = Event(
                                    ErrorMsgItem(
                                        true, CacheUtil.getInstance().getTranslations(
                                            Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                            "You are not eligible to claim the promotion."
                                        )
                                    )
                                )
                            }
                        }
                    })
            }
        }

    }

    fun validateDigitalCoupon(digitalCoupon: Campaign?) {
        if (digitalCoupon != null) {
            invoke {
                homeRepository.getMenuCartItems {
                    val customer = AuthUtil.getInstance().getCustomer()
                    val concept = CacheUtil.getInstance().getConcept()
                    val store = CacheUtil.getInstance().getOrderStore()
                    val menuItems = it
                    val subtotal: Double = getMenuItemsTotalPrice(it)
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
                    orderBody.addProperty("campaign-id", digitalCoupon.getId())
                    orderBody.addProperty("source", "Android")
                    orderBody.addProperty("notes", CacheUtil.getInstance().cartNotes)
                    orderBody.addProperty(
                        "subtotal", String.format(Locale.ENGLISH, "%.2f", subtotal).toDouble()
                    )

                    var discount = 0.0

                    if (deliveryCharge != 0.0) {
                        var hideDiscount = false
                        try {
                            if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion()
                                    .getAttributes().getType() == "delivery"
                            ) {
                                if (digitalCoupon.getPromotion().getAttributes()
                                        .getDiscountType() == "percentage"
                                ) {
                                    if (digitalCoupon.getPromotion().getAttributes()
                                            .getDiscountValue() >= 100
                                    ) {
                                        hideDiscount = true
                                    } else {
                                        deliveryCharge =
                                            deliveryCharge * (digitalCoupon.getPromotion()
                                                .getAttributes().getDiscountValue() / 100)
                                    }
                                } else {
                                    if (digitalCoupon.getPromotion().getAttributes()
                                            .getDiscountValue() >= deliveryCharge
                                    ) {
                                        hideDiscount = true
                                    } else {
                                        deliveryCharge =
                                            deliveryCharge - digitalCoupon.getPromotion()
                                                .getAttributes().getDiscountValue()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (hideDiscount) deliveryCharge = 0.0
                    }
                    var discountedSubtotal = subtotal - discount
                    if (discountedSubtotal < 0) {
                        discountedSubtotal = 0.0
                        discount = subtotal
                    }
                    orderBody.addProperty(
                        "discount", String.format(Locale.ENGLISH, "%.2f", discount).toDouble()
                    )
                    val validDelivery = CacheUtil.getInstance()
                        .getOrderType() === OrderType.DELIVERY && (CacheUtil.getInstance()
                        .getOrderAddress() != null || CacheUtil.getInstance().orderDeliveryAddress != null)
                    val validPickup = (CacheUtil.getInstance()
                        .getOrderType() === OrderType.PICKUP || CacheUtil.getInstance()
                        .getOrderType() === OrderType.DINE_IN || CacheUtil.getInstance()
                        .getOrderType() === OrderType.CURBSIDE) && CacheUtil.getInstance()
                        .getOrderStore() != null
                    val orderType = CacheUtil.getInstance().orderType
                    when (orderType) {
                        OrderType.PICKUP -> {
                            deliveryCharge = 0.0
                            if (CacheUtil.getInstance()
                                    .getApplication().attributes.getOrderTypes().size == 3 || CacheUtil.getInstance()
                                    .getApplication().attributes.getOrderTypes()
                                    .contains(OrderType.DINE_IN)
                            ) {
                                orderBody.addProperty("type", "to-go")
                            } else {
                                orderBody.addProperty("type", "pickup")
                            }
                            orderBody.addProperty("location", store.id)
                        }

                        OrderType.DINE_IN -> {
                            deliveryCharge = 0.0
                            orderBody.addProperty("type", "eat-in")
                            orderBody.addProperty("location", store.id)
                        }

                        OrderType.DELIVERY -> {
                            orderBody.addProperty("type", "deliver")
                            orderBody.addProperty(
                                "delivery-charge",
                                String.format(Locale.ENGLISH, "%.2f", deliveryCharge).toDouble()
                            )
                            val address = CacheUtil.getInstance().getOrderAddress()
                            if (address != null) {
                                orderBody.addProperty("address", address.id)
                            }
                        }

                        OrderType.CURBSIDE -> if (CacheUtil.getInstance()
                                .getCurbsideCar() != null
                        ) {
                            orderBody.addProperty(
                                "location", CacheUtil.getInstance().getOrderStore().id
                            )
                            orderBody.addProperty(
                                "car-id", CacheUtil.getInstance().getCurbsideCar().id
                            )
                            orderBody.addProperty("type", "curbside")
                        }

                        OrderType.DRIVE_THRU -> {
                            deliveryCharge = 0.0
                            if (CacheUtil.getInstance()
                                    .getApplication().attributes.getOrderTypes().size == 3 || CacheUtil.getInstance()
                                    .getApplication().attributes.getOrderTypes()
                                    .contains(OrderType.DINE_IN)
                            ) {
                                orderBody.addProperty("type", "to-go")
                            } else {
                                orderBody.addProperty("type", "pickup")
                            }
                            orderBody.addProperty("location", store.id)
                        }

                        OrderType.NONE -> deliveryCharge = 0.0
                    }

                    //}
                    val vatAmount = 0.0
                    //        if (concept.getVatType().equalsIgnoreCase("exclusive")) {
//            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//            orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
//        }
                    var total = String.format(
                        Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount
                    ).toDouble()
                    if (total < 0) {
                        total = 0.0
                    }
                    orderBody.addProperty("total", total)
                    val items = JsonArray()
                    var orderStoreId: String? = null
                    if (CacheUtil.getInstance().getOrderStore() != null) {
                        orderStoreId = CacheUtil.getInstance().getOrderStore().id
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
                        for (modifierGroup in menuItem.getModifierGroups()) {
                            val visibilityConfig = modifierGroup.attributes.visibilityConfig
                            if (visibilityConfig == null || !visibilityConfig.initialValue.equals(
                                    "hidden", ignoreCase = true
                                )
                            ) {
                                for (modifierItem in modifierGroup.modifierItems) {
                                    if (modifierItem.attributes.isSelected) {
                                        val modifier = JsonObject()
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
                        item.add("ingredients", ingredients)
                        item.add("modifiers", modifiers)
                        items.add(item)
                    }
                    orderBody.add("items", items)
                    CouponsApiCall.getInstance()
                        .validatePromotion(orderBody, object : ApiRequestListener {
                            override fun onSuccess(o: Any) {
                                try {
                                    val jsonObject = JSONObject(o.toString())
                                    if (jsonObject.has("error") && !jsonObject.isNull("error")) {
                                        _showPromotionsError.value = Event(ErrorMsgItem(true, jsonObject.getString("error")))
                                    } else {
                                        if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion()
                                                .getAttributes().getType()
                                                .equals("item", ignoreCase = true)
                                        ) {
                                            getCampaignPromotionItem(digitalCoupon)
                                        } else {
                                            trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                            CacheUtil.getInstance().setCampaign(digitalCoupon)
                                            CacheUtil.getInstance()
                                                .setCampaignPromotion(digitalCoupon.promotion)
                                            CacheUtil.getInstance()
                                                .setCampaignPromotionMenuItem(null)
                                            CacheUtil.getInstance().setCampaignToAdd(null)
                                            if(CacheUtil.getInstance().getCartQuantity() > 0){
                                                _showCart.value = Event(true)
                                            }else {
                                                _showMenu.value = Event(true)
                                            }

                                        }
                                    }
                                } catch (ex: JSONException) {
                                    ex.printStackTrace()
                                    onError("")
                                }
                            }

                            override fun onError(o: Any) {
                                AppLogger.printRetrofitError(o)
                                _showPromotionsError.value = Event(
                                    ErrorMsgItem(
                                        true, ErrorHandler.getErrorMessage(
                                            o, CacheUtil.getInstance().getTranslations(
                                                Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                                "You are not eligible to claim the promotion."
                                            ), null
                                        )
                                    )
                                )
                            }
                        })
                }
            }

        }
    }

    private fun getMenuItemsTotalPrice(menuItems: ArrayList<MenuItem>): Double {
        var subtotalPrice = 0.0
        for (menuItem in menuItems) {
            subtotalPrice += getMenuItemTotalPrice(menuItem) * menuItem.attributes.quantity
        }
        return subtotalPrice
    }

    private fun getMenuItemTotalPrice(menuItem: MenuItem): Double {
        var orderStoreId: String? = null
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().id
        }
        var totalPrice = menuItem.getPrice(orderStoreId)
        var comboIndex = 0
        var trueIndex = 0
        if (menuItem.sizes.modifierItems.size > 0) {
            if (!menuItem.isPromo) {
                totalPrice = 0.0
            }
            for (modifierItem in menuItem.sizes.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    totalPrice += modifierItem.attributes.price
                    trueIndex = comboIndex
                    break
                }
                comboIndex++
            }
        }
        for (modifierItem in menuItem.drinks.modifierItems) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierItem in menuItem.fries.modifierItems) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (ingredient in menuItem.getIngredients()) {
            if (ingredient.attributes.isSelected) {
                totalPrice += ingredient.attributes.price
            }
        }
        for (modifierGroup in menuItem.getModifierGroups()) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || !visibilityConfig.initialValue.equals(
                    "hidden", ignoreCase = true
                )
            ) {
                for (modifierItem in modifierGroup.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        totalPrice += modifierItem.attributes.price * modifierItem.attributes.quantity
                    }
                }
            }
        }
        try {
            if (menuItem.isCombo) {
                for (components in menuItem.combo[trueIndex].components) {
                    for (item in components.items) {
                        if (item.selected) {
                            if (!menuItem.isSandwichOnly) totalPrice += item.price
                            if (!item.modifierGroups.isEmpty()) {
                                if (components.name.english.lowercase(Locale.getDefault())
                                        .contains("sandwich") || components.name.french.lowercase(
                                        Locale.getDefault()
                                    ).contains("sandwich")
                                ) {
                                    val getIndex: Int = getYourWayModifier(item.modifierGroups)
                                    if (getIndex != -1) {
                                        for (i in item.modifierGroups[getIndex].modifierItems.indices) {
                                            if (item.modifierGroups[getIndex].modifierItems[i].attributes.isSelected) {
                                                totalPrice += item.modifierGroups[getIndex].modifierItems[i].attributes.price
                                            }
                                        }
                                    }
                                } else if (!menuItem.isSandwichOnly && !components.name.english.lowercase(
                                        Locale.getDefault()
                                    ).contains("sandwich") && !components.name.french.lowercase(
                                        Locale.getDefault()
                                    ).contains("sandwich") || !components.name.english.lowercase(
                                        Locale.getDefault()
                                    ).contains("sandwich") && !components.name.french.lowercase(
                                        Locale.getDefault()
                                    )
                                        .contains("sandwich") && menuItem.customFieldObjectMap.isNotItemable
                                ) {
                                    for (modifierGroup in item.modifierGroups) {
                                        for (i in modifierGroup.modifierItems.indices) {
                                            if (modifierGroup.modifierItems[i].attributes.isSelected) {
                                                totalPrice += modifierGroup.modifierItems[i].attributes.price
                                            }
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
        return totalPrice
    }

    private fun getYourWayModifier(modifierGroups: java.util.ArrayList<ModifierGroup>): Int {
        for (i in modifierGroups.indices) {
            if (modifierGroups[i].attributes.customFieldObjectMap.label.equals(
                    "yourway", ignoreCase = true
                )
            ) {
                return i
            }
        }
        return -1
    }

    fun getCatering() {
        invoke {
            cmsRepository.getCatering {
                _catering.value = Event(it)
            }
        }
    }

}
