package com.herfy.home.views

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.database.entities.MenuItemEntity
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.database.repositories.HomeRepository
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.model.ErrorMsgItem
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.Event
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.Slide
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun getCustomerLoyalty(id: String){
        invoke {
            homeRepository.getCustomerLoyalty(id){homeResponse ->
                if(homeResponse.success){
                    homeResponse.data?.let { it ->
                        it.cast<Loyalty> {
                            _loyalty.value = it
                        }
                    }
                }else {
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }
}