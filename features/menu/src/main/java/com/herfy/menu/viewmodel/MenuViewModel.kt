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

    private val _slides = MutableLiveData<Event<ArrayList<Slide>>>()
    val slides: LiveData<Event<ArrayList<Slide>>> = _slides

    private val menuCategories = mutableListOf<MenuCategory>()
    private val _showMenuCategories = MutableLiveData<Event<List<MenuCategory>>>()
    val showMenuCategories: LiveData<Event<List<MenuCategory>>> = _showMenuCategories

    val menuCategoryPositions = HashMap<String, Int>()
    private val _menuAdapterItems = MutableLiveData<Event<List<MenuCategoryItemAdapter.MenuAdapterItem>>>()
    val menuAdapterItems: LiveData<Event<List<MenuCategoryItemAdapter.MenuAdapterItem>>> =
        _menuAdapterItems

    private val _searchMenuAdapterItems =
        MutableLiveData<Event<List<MenuCategoryItemAdapter.MenuAdapterItem>>>()
    val searchMenuAdapterItems: LiveData<Event<List<MenuCategoryItemAdapter.MenuAdapterItem>>> =
        _searchMenuAdapterItems

    private val _showMenuItem = MutableLiveData<Event<Triple<String, String, Boolean>>>()
    val showMenuItem: LiveData<Event<Triple<String, String, Boolean>>> = _showMenuItem

    private val _showAddToCart = MutableLiveData<Event<MenuItem>>()
    val showAddToCart: LiveData<Event<MenuItem>> = _showAddToCart

    private val _showMessage = MutableLiveData<Event<String>>()
    val showMessage: LiveData<Event<String>> = _showMessage

    private val _showError = MutableLiveData<Event<String>>()
    val showError: LiveData<Event<String>> = _showError

    private val _showLoading = MutableLiveData<Event<Boolean>>()
    val showLoading: LiveData<Event<Boolean>> = _showLoading

    private val _orderType = MutableLiveData<Event<OrderType>>()
    val orderType: LiveData<Event<OrderType>> = _orderType

    private val _showLink = MutableLiveData<Event<Uri>>()
    val showLink: LiveData<Event<Uri>> = _showLink

    private val _showMenuCategory = MutableLiveData<Event<String>>()
    val showMenuCategory: LiveData<Event<String>> = _showMenuCategory

    fun getOrderType() {
        invoke {
            _orderType.value = Event(CacheUtil.getInstance().orderType)
        }
    }

    fun getSlides() {
        invoke {
            val sliderId = CacheUtil.getInstance().getSliderId(Values.CATEGORY_PAGE_TOP_BANNER_LABEL)
            if(!sliderId.isNullOrEmpty()) {
                homeRepository.getSliders(sliderId) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let {
                            it.cast<ArrayList<Slide>> { slidesList ->
                                _slides.postValue(Event(slidesList))
                            }
                        }
                    } else {
                        _slides.postValue(Event(ArrayList()))
                    }
                }
            } else {
                _slides.postValue(Event(ArrayList()))
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
                        _showMenuItem.value = Event(Triple(categoryId, menuItemEntity.id, menuItemEntity.isCombo))
                    }
                }
            }
        }
    }

    fun getMenu() {
        invoke {
            menuRepository.getMenu {
                getMenuCategoriesItems()
            }
        }
    }

    private fun getMenuCategoriesItems() {
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
            _showMenuCategories.postValue(Event(menuCategories))
            _menuAdapterItems.postValue(Event(adapterItems))
        }
    }

    fun getMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {
        invoke {
            _showLoading.postValue(Event(true))
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
                        _showLoading.postValue(Event(false))
                        _showMessage.postValue(Event(
                            CacheUtil.getInstance().getTranslations(
                                Text.MODIFIER_MINIMUM_LIMIT,
                                "Minimum number of extras (x) not reached for (y)"
                            ).replace("(x)", modifierGroup.attributes.minimum.toString())
                                .replace("(y)", modifierGroup.attributes.name)
                        ))
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
                    _showLoading.postValue(Event(false))
                    _showMenuItem.postValue(Event(Triple(menuCategory.id, menuItem.id, menuItem.isCombo)))
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
                                        _showLoading.postValue(Event(false))
                                        _showMessage.postValue(Event(
                                            CacheUtil.getInstance().getTranslations(
                                                Text.MODIFIER_MINIMUM_LIMIT,
                                                "Minimum number of extras (x) not reached for (y)"
                                            ).replace(
                                                "(x)", modifierGroup.attributes.minimum.toString()
                                            ).replace("(y)", modifierGroup.attributes.name)
                                        ))
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

        val store = CacheUtil.getInstance().orderStore

        if (store != null && menuItem.disabledStores.containsKey(store.id)) {
            _showLoading.postValue(Event(false))
            _showError.postValue(Event(
                CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                    .replace("(x)", store.attributes.name)
            ))
        } else {
            CacheUtil.getInstance().addToCart(menuItem)
            _showLoading.postValue(Event(false))
            _showAddToCart.postValue(Event(menuItem))
        }
    }

    fun addRemoveFavorite(menuItem: MenuItem) {
        invoke {
            if (!CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                favoriteRepository.addToFavorite(menuItem) {
                    _showMessage.postValue(Event(it))
                }
            } else {
                favoriteRepository.removeFromFavorite(menuItem) {
                    _showMessage.postValue(Event(it))
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
            _searchMenuAdapterItems.postValue(Event(adapterItems))
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