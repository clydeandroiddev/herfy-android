package com.herfy.home.views

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.herfy.database.entities.MenuItemEntity
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.database.repositories.HomeRepository
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.model.ErrorMsgItem
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.Event
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.util.AppLogger
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.Disposable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
) : BaseViewModel(), PromotionsAdapter.PromotionListener, SliderImageAdapter.SliderListener {

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError

    private val _slideList = MutableLiveData<ArrayList<Slide>>()
    val slideList: LiveData<ArrayList<Slide>> = _slideList

    private val _promotionsList = MutableLiveData<ArrayList<Campaign>>()
    val promotionsList: LiveData<ArrayList<Campaign>> = _promotionsList

    private val _showLink = MutableLiveData<Event<Uri>>()
    val showLink: LiveData<Event<Uri>> = _showLink

    private val _showMenuCategory = MutableLiveData<Event<Pair<Int, MenuCategory>>>()
    val showMenuCategory : LiveData<Event<Pair<Int, MenuCategory>>> = _showMenuCategory

    private val _showMenuItem = MutableLiveData<Event<Pair<String, String>>>()
    val showMenuItem: LiveData<Event<Pair<String, String>>> = _showMenuItem

    private val _showPromo = MutableLiveData<Event<Campaign>>()
    val showPromo: LiveData<Event<Campaign>> = _showPromo

    private val menuCategories: ArrayList<MenuCategory> = ArrayList()
    var excludedMenu: ArrayList<MenuCategory> = ArrayList()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()

    private val _loyalty = MutableLiveData<Loyalty>()
    val loyalty : LiveData<Loyalty> = _loyalty


    private val _tiers = MutableLiveData<ArrayList<Tier>>()
    val tiers: MutableLiveData<ArrayList<Tier>> = _tiers


    private val _tier = MutableLiveData<Tier?>()
    val tier : MutableLiveData<Tier?> = _tier

    private val _totalPointsAccumulated = MutableLiveData<Int>()
    val totalPointsAccumulated : MutableLiveData<Int> = _totalPointsAccumulated

    private val _pointsToNextTier= MutableLiveData<Int>()
    val pointsToNextTier : MutableLiveData<Int> = _pointsToNextTier

    private val _startPoints = MutableLiveData<Int>()
    val startPoints : MutableLiveData<Int> = _startPoints

    fun getSliders() {
        invoke {
            homeRepository.getSliders { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.let {
                        it.cast<ArrayList<Slide>> {
                            _slideList.value = it
                        }
                    }
                } else {
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }

    fun getPromotions() {
        invoke {
            homeRepository.getGuestDigitalCoupons(homeRepository.getGetOrderType()) { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.let {
                        it.cast<ArrayList<Campaign>> {
                            _promotionsList.value = it
                        }
                    }
                } else {
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }
    private fun getMenuCategoryWith(menuCategoryId: String) {
        invoke {
            val menu: List<MenuCategory> = excludedMenuCategories(menuCategories)
            menu.sortedWith { menuCategory1, menuCategory2 -> menuCategory1.displayOrder - menuCategory2.displayOrder }
            menu.find { menuCategory ->  menuCategory.id.equals(menuCategoryId, ignoreCase = true) }?.let {
                _showMenuCategory.value = Event(Pair(menu.indexOf(it), it))
            }
        }
    }

    private fun excludedMenuCategories(menuCategories: List<MenuCategory>): List<MenuCategory> {
        excludedMenu.clear()
        val orderStore = homeRepository.getOrderStore()
        if (orderStore != null && !excludedCategories.isEmpty() && excludedCategories.containsKey(orderStore.id)) {
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
          homeRepository.getMenuItemWith(menuItemId){ homeResponse ->
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
                    getMenuCategoryWith(slide.linkValue)
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
                homeResponse.data?.let {data->
                    data.cast<ArrayList<MenuCategory>> {
                        for(category in it){
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
                                    homeRepository.getMenuItemBy(category.id){ response ->
                                        response.data?.cast<List<MenuItemEntity>> { menuItemDbs ->
                                            if (menuItemDbs.isNotEmpty()) {
                                                category.menuItems = menuItemDbs.toDomain()
                                                menuCategories.add(category)
                                                if (!storeSelected || !category.excludedLocations.contains(orderStore?.id ?: "")) {
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
        ) { tiers: List<Tier>, loyalty: Loyalty -> android.util.Pair<Loyalty, List<Tier>>(loyalty, tiers)
        }.subscribe(
            { pair: android.util.Pair<Loyalty, List<Tier>> ->
                var tiers = pair.second
                val loyalty = pair.first
                tiers = tiers
                val totalPointsAccumulated = loyalty.attributes.currentBalance
                var eligibleTiers =
                    Observable.fromIterable(tiers)
                        .filter { tier: Tier -> totalPointsAccumulated <= tier.endPoint }
                        .toList().blockingGet()
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
                    pointsToNextTier =
                        nextTier.startPoint - loyalty.attributes.currentBalance
                }
                var startPoints = 0
                if (nextTier != null) {
                    startPoints = nextTier.startPoint
                }

                _tier.value = nextTier
                _totalPointsAccumulated.value = totalPointsAccumulated
                _pointsToNextTier.value = pointsToNextTier
                _startPoints.value = startPoints

            }
        ) { error: Throwable? ->
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

    fun getTiersObservable(): Single<List<Tier>?>? {
        return Single.create<List<Tier>?> { source: SingleEmitter<List<Tier>?> ->

            invoke {
                homeRepository.getLoyaltyTiers(){homeResponse ->
                    if(homeResponse.success){
                        homeResponse.data?.let { it ->
                            it.cast<ArrayList<Tier>> {
                                _tiers.value = it
                                source.onSuccess(it)
                            }
                        }
                    }else {
                        source.onError(
                            Throwable(
                                CacheUtil.getInstance()
                                    .getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
                        _showError.value = ErrorMsgItem(true, homeResponse.message)
                    }
                }
            }
        }
    }

    fun getLoyaltyObservable(): Single<Loyalty?>? {
        return Single.create<Loyalty?> { source: SingleEmitter<Loyalty?> ->
            invoke {
                homeRepository.getCustomerLoyalty(AuthUtil.getInstance().customer.id){homeResponse ->
                    if(homeResponse.success){
                        homeResponse.data?.let { it ->
                            it.cast<Loyalty> {
                                _loyalty.value = it
                                AuthUtil.getInstance().customerLoyalty = it
                                source.onSuccess(it)
                            }
                        }
                    }else {
                        source.onError(
                            Throwable(
                                CacheUtil.getInstance()
                                    .getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
                        _showError.value = ErrorMsgItem(true, homeResponse.message)
                    }
                }
            }
        }
    }

}