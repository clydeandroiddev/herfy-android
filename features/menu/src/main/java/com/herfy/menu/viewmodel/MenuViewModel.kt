package com.herfy.menu.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.database.entities.MenuItemEntity
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.repository.FavoriteRepository
import com.herfy.repository.HomeRepository
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.Event
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Slide
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val menuRepository: MenuRepository,
    private val favoriteRepository: FavoriteRepository
) : BaseViewModel(), SliderImageAdapter.SliderListener {

    var initialLoad = true

    private val _slides = MutableLiveData<ArrayList<Slide>>()
    val slides: LiveData<ArrayList<Slide>> = _slides

    private val menuCategories = mutableListOf<MenuCategory>()
    private val _showMenuCategories = MutableLiveData<List<MenuCategory>>()
    val showMenuCategories: LiveData<List<MenuCategory>> = _showMenuCategories

    val menuCategoryPositions = HashMap<String, Int>()
    private val _menuAdapterItems = MutableLiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>>()
    val menuAdapterItems: LiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>> =
        _menuAdapterItems

    private val _searchMenuAdapterItems =
        MutableLiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>>()
    val searchMenuAdapterItems: LiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>> =
        _searchMenuAdapterItems

    private val _showMenuItem = MutableLiveData<Pair<String, String>>()
    val showMenuItem: LiveData<Pair<String, String>> = _showMenuItem

    private val _showAddToCart = MutableLiveData<MenuItem>()
    val showAddToCart: LiveData<MenuItem> = _showAddToCart

    private val _showMessage = MutableLiveData<String>()
    val showMessage: LiveData<String> = _showMessage

    private val _showError = MutableLiveData<String>()
    val showError: LiveData<String> = _showError

    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean> = _showLoading

    private val _orderType = MutableLiveData<OrderType>()
    val orderType: LiveData<OrderType> = _orderType

    private val _showLink = MutableLiveData<Event<Uri>>()
    val showLink: LiveData<Event<Uri>> = _showLink

    private val _showMenuCategory = MutableLiveData<Event<String>>()
    val showMenuCategory: LiveData<Event<String>> = _showMenuCategory

    fun getOrderType() {
        invoke {
            _orderType.value = CacheUtil.getInstance().orderType
        }
    }

    fun getSlides() {
        invoke {
            homeRepository.getSliders(Values.CATEGORY_PAGE_TOP_BANNER) { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.let {
                        it.cast<ArrayList<Slide>> {
                            _slides.value = it
                        }
                    }
                } else {
                    _slides.value = ArrayList()
                }
            }
        }
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

    private fun getMenuItemWith(menuItemId: String) {
        invokeAtBackground {
            homeRepository.getMenuItemWith(menuItemId) { homeResponse ->
                homeResponse.data?.cast<MenuItemEntity> { menuItemEntity ->
                    val categoryId = menuItemEntity.menuCategoryId
                    if (categoryId.isNotEmpty()) {
                        _showMenuItem.value = Pair(categoryId, menuItemEntity.id)
                    }
                }
            }
        }
    }

    fun getMenuCategoriesItems() {
        invoke {
            val adapterItems = mutableListOf<MenuCategoryItemAdapter.MenuAdapterItem>()
            menuRepository.getMenuCategoriesItems {
                menuCategories.clear()
                menuCategories.addAll(it)
                var count = 0
                it.forEach { menuCategory ->
                    menuCategoryPositions[menuCategory.id] = count
                    adapterItems.add(MenuCategoryItemAdapter.MenuCategoryAdapterItem(menuCategory))
                    menuCategory.menuItems.sortBy { menuItem -> menuItem.displayOrder }
                    menuCategory.menuItems?.forEach { menuItem ->
                        adapterItems.add(
                            MenuCategoryItemAdapter.MenuItemAdapterItem(
                                menuCategory, menuItem
                            )
                        )
                        count++
                    }
                    count++
                }
            }
            _showMenuCategories.postValue(menuCategories)
            _menuAdapterItems.postValue(adapterItems)
        }
    }

    fun getMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {
        invoke {
            _showLoading.postValue(true)
            menuRepository.getMenuItem(menuItemId = menuItem.id, result = {

                var invalid = false

                val visibleModifierGroups = java.util.ArrayList<ModifierGroup>()

                for (group in it.modifierGroups) {
                    val visibilityConfig = group.attributes.visibilityConfig
                    if (visibilityConfig?.initialValue != null) {
                        if (!visibilityConfig.initialValue.equals(
                                    "hidden",
                                    ignoreCase = true
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
                    if (selected < modifierGroup.attributes.minimum) {
                        _showLoading.postValue(false)
                        _showMessage.postValue(
                            CacheUtil.getInstance().getTranslations(
                                Text.MODIFIER_MINIMUM_LIMIT,
                                "Minimum number of extras (x) not reached for (y)"
                            ).replace("(x)", modifierGroup.attributes.minimum.toString())
                                .replace("(y)", modifierGroup.attributes.name)
                        )
                        invalid = true
                        break
                    }
                }

                if (!invalid) {
                    invalid = checkComboModifiers(menuItem)
                }

                if (!invalid) {
                    addToCart(menuItem)
                } else {
                    _showLoading.postValue(false)
                    _showMenuItem.postValue(Pair(menuCategory.id, menuItem.id))
                }
            })
        }
    }

    private fun checkComboModifiers(menuItem: MenuItem): Boolean {
        var invalid = false
        if (menuItem.isCombo) {
            var comboIndex = 0
            var trueIndex = 0
            for (size in menuItem.sizes.modifierItems) {
                if (size.attributes.isSelected) {
                    trueIndex = comboIndex
                }
                comboIndex++
            }
            if (menuItem.isCombo) {
                for (components in menuItem.combo.get(trueIndex).components) {
                    if (invalid) {
                        break
                    }
                    for (item in components.items) {
                        if (invalid) {
                            break
                        }
                        if (item.selected) {
                            if (item.modifierGroups.isNotEmpty()) {
                                for (modifierGroup in item.modifierGroups) {
                                    var selected = 0
                                    for (modifierItem in modifierGroup.modifierItems) {
                                        if (modifierItem.attributes.isSelected) {
                                            selected += modifierItem.attributes.quantity
                                        }
                                    }
                                    if (selected < modifierGroup.attributes.minimum) {
                                        _showLoading.postValue(false)
                                        _showMessage.postValue(
                                            CacheUtil.getInstance().getTranslations(
                                                Text.MODIFIER_MINIMUM_LIMIT,
                                                "Minimum number of extras (x) not reached for (y)"
                                            ).replace(
                                                "(x)", modifierGroup.attributes.minimum.toString()
                                            ).replace("(y)", modifierGroup.attributes.name)
                                        )
                                        invalid = true
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return invalid
    }

    private fun addToCart(menuItem: MenuItem) {

        AppLogger.error("addToCart")

        val store = CacheUtil.getInstance().orderStore

        if (store != null && menuItem.disabledStores.containsKey(store.id)) {
            _showLoading.postValue(false)
            _showError.postValue(
                CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                    .replace("(x)", store.attributes.name)
            )
        } else {
            CacheUtil.getInstance().addToCart(menuItem)
            _showLoading.postValue(false)
            AppLogger.error("266")
            _showAddToCart.postValue(menuItem)
        }
    }

    fun addRemoveFavorite(menuItem: MenuItem) {
        invoke {
            if (!CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                favoriteRepository.addToFavorite(menuItem) {
                    _showMessage.postValue(it)
                }
            } else {
                favoriteRepository.removeFromFavorite(menuItem) {
                    _showMessage.postValue(it)
                }
            }
        }
    }

    fun searchMenuItems(query: String) {
        invoke {
            val adapterItems = mutableListOf<MenuCategoryItemAdapter.MenuAdapterItem>()
            menuCategories.forEach { menuCategory ->
                menuCategory.menuItems.forEach { menuItem ->
                    if (menuItem.getName(LocaleUtil.getInstance().language)
                            .contains(query, ignoreCase = true)
                    ) {
                        adapterItems.add(
                            MenuCategoryItemAdapter.MenuItemAdapterItem(
                                menuCategory, menuItem
                            )
                        )
                    }
                }
            }
            _searchMenuAdapterItems.postValue(adapterItems)
        }
    }

    fun getMenuCategoryId(position: Int): String? {
        for((key, value) in menuCategoryPositions) {
            if(position == value) {
                return key
            }
        }
        return null
    }

}