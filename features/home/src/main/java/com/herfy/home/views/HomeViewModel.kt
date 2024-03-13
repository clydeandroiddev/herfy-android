package com.herfy.home.views

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.database.entities.MenuItemEntity
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.model.CampaignToAdd
import com.herfy.model.ErrorMsgItem
import com.herfy.model.PromotionCategory
import com.herfy.repository.HomeRepository
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.Event
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val dispatchers: DispatcherProvider
) : BaseViewModel(), PromotionsAdapter.PromotionListener, SliderImageAdapter.SliderListener,
    PromotionCategoryAdapter.PromotionCategoryListener, PromotionDetailBottomSheet.onAction {

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError

    private val _slideList = MutableLiveData<ArrayList<Slide>>()
    val slideList: LiveData<ArrayList<Slide>> = _slideList

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

    private val _loyalty = MutableLiveData<Loyalty?>()
    val loyalty: LiveData<Loyalty?> = _loyalty

    private val _updatePromoCategoryList = MutableLiveData<Event<PromotionCategory>>()
    val updatePromoCategoryList: LiveData<Event<PromotionCategory>> = _updatePromoCategoryList

    private val _orderTypes = MutableLiveData<Set<OrderType>>()
    val orderTypes: LiveData<Set<OrderType>> = _orderTypes


    private val _tiers = MutableLiveData<List<Tier>>()
    val tiers: MutableLiveData<List<Tier>> = _tiers


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
    val showCart : LiveData<Event<Boolean>> = _showCart


    fun getSliders() {
        invoke {
            homeRepository.getSliders { homeResponse ->
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
        }
    }

    fun getBottomSliders() {
        invoke {
            homeRepository.getBottomSliders { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.cast<ArrayList<Slide>> {
                        _bottomSlideList.value = it
                    } ?: run {
                        _bottomSlideList.value = arrayListOf()
                    }
                } else {
                    _bottomSlideList.value = arrayListOf()
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }

    fun getOrderTypes() {
        invoke {
            homeRepository.getOrderTypes { res ->
                res.data?.cast<Set<OrderType>> { orderTypes ->
                    _orderTypes.value = orderTypes
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
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
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

    private fun getMenuItemWith(menuItemId: String) {
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
        // Ensures that there will always be a tier when loyalty is shown
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

        }
    }

    private fun getTiersObservable(): Single<List<Tier>?>? {
        return Single.create<List<Tier>?> { source: SingleEmitter<List<Tier>?> ->

            invoke {
                homeRepository.getLoyaltyTiers { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let { it ->
                            it.cast<ArrayList<Tier>> {
                                _tiers.value = it
                                source.onSuccess(it)
                            }
                        }
                    } else {
                        source.onError(
                            Throwable(
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
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
                                _loyalty.value = it
                                AuthUtil.getInstance().customerLoyalty = it
                                source.onSuccess(it)
                            }
                        } ?: run {
                            _loyalty.value = null
                        }
                    } else {
                        _loyalty.value = null
                        source.onError(
                            Throwable(
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
                        _showError.value = ErrorMsgItem(true, homeResponse.message)
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
            OrderType.DELIVERY -> "delivery"
            OrderType.CURBSIDE -> "curbside"
            OrderType.DRIVE_THRU -> "drive-thru"
            OrderType.EAT_IN -> "eat-in"
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
                    _promotionCategoryList.value = promocateglist
                }
            }
        }
    }

    override fun onClickClaimPromo(promo: Campaign) {
        trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
        CacheUtil.getInstance().campaign = promo
        CacheUtil.getInstance().campaignPromotion = promo.promotion
        if (promo.promotion?.attributes?.type.equals(
                "item",
                ignoreCase = true
            )
        ) {
            getCampaignPromotionItem(promo)
        } else {
            _showCart.value = Event(true)
        }
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
                                        menuItem.attributes.quantity = promotion.attributes.itemQuantity
                                    } else {
                                        menuItem.attributes.quantity = 1
                                    }
                                    var orderStoreId: String? = null
                                    if (CacheUtil.getInstance().orderStore != null) {
                                        orderStoreId = CacheUtil.getInstance().orderStore.id
                                    }
                                    menuItem.attributes.price = promotion.attributes.itemPrice
                                    menuItem.toCdn(orderStoreId)
                                    MixpanelUtil.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                    if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                                        val campaignToAdd = CampaignToAdd(
                                            digitalCoupon,
                                            digitalCoupon.promotion,
                                            menuItem
                                        )
                                        CacheUtil.getInstance().campaignToAdd = campaignToAdd
                                    } else {
                                        CacheUtil.getInstance().campaign = digitalCoupon
                                        CacheUtil.getInstance().campaignPromotion =
                                            digitalCoupon.promotion
                                        CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                                    }
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
                                    var detail =
                                        CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                                    if (error is JSONObject) {
                                        detail = error.getString("detail")
                                    } else if (error is JSONArray) {
                                        val errorObject = error.getJSONObject(0)
                                        detail = errorObject.getString("detail")
                                    }
                                    _showError.value = ErrorMsgItem(true, detail)
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    _showError.value = ErrorMsgItem(true,CacheUtil.getInstance().getTranslations(
                                        Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                        "You are not eligible to claim the promotion."
                                    ))
                                }
                            } else {
                                _showError.value = ErrorMsgItem(true,CacheUtil.getInstance().getTranslations(
                                    Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                    "You are not eligible to claim the promotion."
                                ))
                            }
                        }
                    })
            }
        }

    }

}
