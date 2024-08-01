package com.herfy.productpagedetails.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.analytics.firebase.FirebaseEventUtilHelper
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil.trackEvent
import com.herfy.database.entities.MenuItemDb
import com.herfy.model.ErrorMsgItem
import com.herfy.model.EventAttributesMetric
import com.herfy.model.MenuItemComponentItem
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.model.combo.ComboComponentAdapterItem
import com.herfy.model.combo.ComponentAdapterItem
import com.herfy.model.combo.ComponentModifierAdapterItem
import com.herfy.model.modifiers.ModifierAdapterItem
import com.herfy.model.modifiers.ModifierGroupAdapterItem
import com.herfy.model.modifiers.ModifierItemAdapterItem
import com.herfy.model.modifiers.ModifierLineAdapterItem
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierAdapter
import com.herfy.productpagedetails.ui.adapter.ComboMealComponentAdapter
import com.herfy.productpagedetails.ui.viewholder.FrequentlyBoughtItemViewholder
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.herfy.repository.MenuRepository
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.deepCopy
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.handler.ErrorHandler.getErrorMessage
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.Event
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.menu.MenuUtil
import com.kochava.tracker.events.EventType
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn
import com.skylinedynamics.solosdk.api.models.requestbodies.AddFavoriteItemBody
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import java.util.LinkedList
import javax.inject.Inject


@HiltViewModel
class ComboViewModel @Inject constructor(
    private val cacheUtilHelper: CacheUtilHelper, private val menuRepository: MenuRepository
) : BaseViewModel(), SizesItemViewholder.OnClickListener,
    FrequentlyBoughtItemViewholder.OnClickListener,
    ComboItemDetailsModifierAdapter.ComboItemDetailsModifierListener,
    ComboMealComponentAdapter.ComboComponentListener {


    //Objects
    var menuItem: MenuItem? = null
        get() = field
        set(value) {
            field = value
        }
    var menuCategory: MenuCategory? = null
        get() = field
        set(value) {
            field = value
        }
    var isFromCartEdit: Boolean = false
        get() = field
        set(value) {
            field = value
        }
    private var comboIndex: Int = 0
        get() = field
        set(value) {
            field = value
        }
    private var previousComboIndex: Int = 0
        get() = field
        set(value) {
            field = value
        }

    var isFromFavorite = false

    private var frequentlyMenuItem = MenuItem()
    var currentSelectedModifierItem: ComponentModifierItem? = null
    var cartMenuItemsTotalPrice: Double = 0.0
    private var _calorieCount = 0

    private var excludedMenu: List<MenuCategory> = arrayListOf()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()
    private val menuCategories: List<MenuCategory> = arrayListOf()
    private var menuItemComponentList = HashMap<Int, MenuItemComponents>()
    private var menuItemComboList = HashMap<Int, HashMap<Int, MenuItemComponents>>()
    private var selectedComponents = HashMap<String, Any>()
    val frequentBoughtItemList = arrayListOf<MenuItem>()
    private val modifierGroupVisibilityHash = HashMap<String, String>()


    //Observalbles
    val notes = ObservableField("")
    var calories: String = ""

    //LiveData
    private val _showError = MutableLiveData<Event<ErrorMsgItem>>()
    val showError: LiveData<Event<ErrorMsgItem>> = _showError
    private val _showMenuItemDetails = MutableLiveData<MenuItem>()
    val showMenuItemDetails: LiveData<MenuItem> = _showMenuItemDetails
    private val _showHideModifierItemPrice = MutableLiveData<Pair<Boolean, Int>>()
    val showHideModifierItemPrice: LiveData<Pair<Boolean, Int>> = _showHideModifierItemPrice
    private val _showMenuItemTotal = MutableLiveData<Double>()
    val showMenuItemTotal: LiveData<Double> = _showMenuItemTotal
    private val _showMenuItemQuantity = MutableLiveData<Int>()
    val showMenuItemQuantity: LiveData<Int> = _showMenuItemQuantity
    private val _showComboMenuItemComponents = MutableLiveData<List<ComboComponentAdapterItem>>()
    val showComboMenuItemComponents: LiveData<List<ComboComponentAdapterItem>> =
        _showComboMenuItemComponents
    private val _showComboSizes = MutableLiveData<List<ModifierItem>>()
    val showComboSizes: LiveData<List<ModifierItem>> = _showComboSizes
    private val _showComboMealItemDetails = MutableLiveData<ShowComboMealItemDetails>()
    val showComboMealItemDetails: LiveData<ShowComboMealItemDetails> = _showComboMealItemDetails
    private val _showMenuItemCalories = MutableLiveData<String>()
    val showMenuItemCalories: LiveData<String> = _showMenuItemCalories
    private val _updateComboSizeList = MutableLiveData<Pair<Int, Int>>()
    val updateComboSizeList: LiveData<Pair<Int, Int>> = _updateComboSizeList
    private val _updateComboItemDetailModifier =
        MutableLiveData<Triple<Boolean, ModifierGroup, ModifierItem>>()
    val updateComboItemDetailModifier: LiveData<Triple<Boolean, ModifierGroup, ModifierItem>> =
        _updateComboItemDetailModifier
    private val _showToastMessage = MutableLiveData<String>()
    val showToastMessage: LiveData<String> = _showToastMessage
    private val _showComponentModifierItemTotal = MutableLiveData<Double>()
    val showComponentModifierItemTotal: LiveData<Double> = _showComponentModifierItemTotal
    private val _updateCartItem = MutableLiveData<Boolean>()
    val updateCartItem: LiveData<Boolean> = _updateCartItem
    private val _addCartItem = MutableLiveData<MenuItem>()
    val addCartItem: LiveData<MenuItem> = _addCartItem
    private val _showCartExpiry = MutableLiveData<Boolean>()
    val showCartExpiry: LiveData<Boolean> = _showCartExpiry
    private val _logFirebaseEvent = MutableLiveData<String>()
    val logFirebaseEvent: LiveData<String> = _logFirebaseEvent
    private val _setFavoriteItem = MutableLiveData<MenuItem>()
    val setFavoriteItem: LiveData<MenuItem> = _setFavoriteItem
    private val _logEvent = MutableLiveData<EventAttributesMetric>()
    val logEvent: LiveData<EventAttributesMetric> = _logEvent
    private val _logAddToWishlistEvent = MutableLiveData<MenuItem>()
    val logAddToWishlistEvent: LiveData<MenuItem> = _logAddToWishlistEvent
    private val _showFrequentBoughtItems = MutableLiveData<Event<ArrayList<MenuItem>>>()
    val showFrequentBoughtItems: LiveData<Event<ArrayList<MenuItem>>> = _showFrequentBoughtItems
    private val _addFrequentlyBoughtItem = MutableLiveData<Pair<MenuItem, Int>>()
    val addFrequentlyBoughtItem: LiveData<Pair<MenuItem, Int>> = _addFrequentlyBoughtItem
    private val _navigateToMenuItemDetails =
        MutableLiveData<Event<Triple<MenuCategory, MenuItem, Int>>>()
    val navigateToMenuItemDetails: LiveData<Event<Triple<MenuCategory, MenuItem, Int>>> =
        _navigateToMenuItemDetails
    private val _updateFavoriteByPosition = MutableLiveData<Pair<Int, MenuItem>>()
    val updateFavoriteByPosition: LiveData<Pair<Int, MenuItem>> = _updateFavoriteByPosition
    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean> = _showLoading
    private val _modifierAdapterItems = MutableLiveData<Event<List<ModifierAdapterItem>>>()
    val modifierAdapterItems: LiveData<Event<List<ModifierAdapterItem>>> = _modifierAdapterItems
    private val _updateComboComponents =
        MutableLiveData<Triple<Boolean, MenuItemComponents, ComponentModifierItem>>()
    val updateComboComponents: LiveData<Triple<Boolean, MenuItemComponents, ComponentModifierItem>> =
        _updateComboComponents


    /**Function for retrieving data of menu item from db**/
    fun getMenuItemFromDbBy(addToBag: Boolean, id: String, isUpsell: Boolean) {
        invokeAtBackground {
            menuRepository.getMenuItem(id) {
                it.attributes.isUpsell = isUpsell
                menuItem = it
                val applicationId = cacheUtilHelper.getApplication()?.id ?: ""
                if (menuCategory == null) {
                    invokeAtBackground {
                        menuRepository.getMenuCategoryIdFromMenuItemEntityBy(id, applicationId) {
                            val menuCategory = MenuCategory()
                            menuCategory.id = it
                            this.menuCategory = menuCategory
                        }
                    }
                }


                var ifHasSelectedMealSize = false
                if (it.sizes.modifierItems.size > 0) {
                    var counter = 0
                    for (size in it.sizes.modifierItems) {
                        if (size.attributes.isSelected) {
                            ifHasSelectedMealSize = true
                            comboIndex = counter
                            break
                        }
                        counter++
                    }
                }
                if (!ifHasSelectedMealSize && it.sizes.modifierItems.size > 0) {
                    it.sizes.modifierItems[0].attributes.isSelected = true
                    comboIndex = 0
                }
                previousComboIndex = comboIndex
                _showMenuItemDetails.value = it
                _showMenuItemQuantity.value = it.attributes.quantity
                _showComboSizes.value = it.sizes.modifierItems
                getComboMenuItemComponents()
                getMenuItemTotalPrice(it, -1)
            }
        }
    }

    /**Function for retrieving data of menu item from cart cache**/
    fun getMenuItemFromCartBy(position: Int, isUpsell: Boolean) {
        invokeAtBackground {
            cacheUtilHelper.getCartItems { cartItems ->
                cartItems[position].let {
                    it.attributes.isUpsell = isUpsell
                    menuItem = it
                    _showMenuItemDetails.value = it
                    isFromCartEdit = true
                    val applicationId = cacheUtilHelper.getApplication()?.id ?: ""
                    if (menuCategory == null) {
                        invokeAtBackground {
                            menuRepository.getMenuCategoryIdFromMenuItemEntityBy(
                                it.id,
                                applicationId
                            ) {
                                val menuCategory = MenuCategory()
                                menuCategory.id = it
                                this.menuCategory = menuCategory
                            }
                        }
                    }
                    if (it.sizes.modifierItems.size > 0) {
                        var counter = 0
                        for (size in it.sizes.modifierItems) {
                            if (size.attributes.isSelected) {
                                comboIndex = counter
                                break
                            }
                            counter++
                        }
                    }


                    previousComboIndex = comboIndex
                    _showMenuItemDetails.value = it
                    _showMenuItemQuantity.value = it.attributes.quantity
                    _showComboSizes.value = it.sizes.modifierItems
                    getComboMenuItemComponents()
                    showHideModifierItemPrice(it)
                    getMenuItemTotalPrice(it, -1)
                }
            }
        }
    }

    /**Function for setting menu item from promotion menu item**/
    fun setPromoMenuItem(menuItem: MenuItem) {
        this.menuItem = menuItem
        setMenuCategory(menuItem)
        _showMenuItemDetails.value = menuItem
        _showMenuItemQuantity.value = menuItem.attributes.quantity
        _showComboSizes.value = menuItem.sizes.modifierItems
        val applicationId = cacheUtilHelper.getApplication()?.id ?: ""
        if (menuCategory == null) {
            invokeAtBackground {
                menuRepository.getMenuCategoryIdFromMenuItemEntityBy(
                    menuItem.id,
                    applicationId
                ) {
                    val menuCategory = MenuCategory()
                    menuCategory.id = it
                    this.menuCategory = menuCategory
                }
            }
        }
        if (menuItem.sizes.modifierItems.size > 0) {
            var counter = 0
            for (size in menuItem.sizes.modifierItems) {
                if (size.attributes.isSelected) {
                    comboIndex = counter
                    break
                }
                counter++
            }
        }

        getComboMenuItemComponents()
        showHideModifierItemPrice(menuItem)
        getMenuItemTotalPrice(menuItem, -1)
    }

    /**Function for frequent bought items api call and setting it to a recycler view**/
    fun getFrequentBoughtItems(page: Int, menuId: String) {
        invoke {
            menuRepository.getFrequentlyBoughtItems(page, menuId) { items, totalPages ->
                if (page == 1) {
                    frequentBoughtItemList.clear()
                }

                frequentBoughtItemList.addAll(items)
                if (page == totalPages) {
                    val favorites = CacheUtil.getInstance().favorites
                    frequentBoughtItemList.map { item ->
                        item.isFavorite = favorites.contains(item.id)
                    }
                    _showFrequentBoughtItems.value = Event(frequentBoughtItemList)
                } else {
                    getFrequentBoughtItems(page + 1, menuId)
                }
            }
        }
    }

    /**Function for setting the menu item calories this is called every time when getMenuItemTotalPrice is called**/
    private fun setMenuItemCalories() {
        _calorieCount = 0
        menuItem?.let {
            if (it.sizes.modifierItems.size <= 3) {
                for (modifierItem in it.sizes.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        _calorieCount = modifierItem.attributes.calorieCount
                        break
                    }
                }
            }
            if (_calorieCount == 0) {
                val caloriesValue = it.calories
                val calorieCount = it.calorieCount
                if (calorieCount != 0) {
                    _calorieCount = calorieCount
                } else if (caloriesValue != null && caloriesValue.isNotEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                    calories = caloriesValue
                }
            }
            var caloriesFromModifiers = 0
            for (modifierGroup in it.modifierGroups) {
                for (modifierItem in modifierGroup.modifierItems) {
                    var calorieCountPerModifier = 0

                    if (modifierItem.attributes.isSelected && modifierItem.attributes.calorieCount > 0) {
                        if (modifierItem.attributes.quantity > 0) {
                            calorieCountPerModifier =
                                modifierItem.attributes.calorieCount * modifierItem.attributes.quantity
                        } else {
                            calorieCountPerModifier = modifierItem.attributes.calorieCount
                        }
                    }
                    if (calorieCountPerModifier > 0) {
                        caloriesFromModifiers += calorieCountPerModifier
                    }
                }
            }
            if (caloriesFromModifiers > 0) {
                _calorieCount += caloriesFromModifiers;
            }
            if (_calorieCount > 0) {
                calories = _calorieCount.toString()
            }
            _showMenuItemCalories.value = calories
        }
    }

    /**Function for calculating the menu item total price and setting it in ui view**/
    fun getMenuItemTotalPrice(item: MenuItem, modifierGroupPosition: Int) {
        setMenuItemCalories()
        var orderStoreId: String? = null
        var counter = 0
        var comboIndex = 0
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }

        var totalPrice: Double = item.getPrice(orderStoreId)
        if (item.sizes.modifierItems.size > 0) {
            if (!item.isPromotion) {
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

            if (item.isCombo && item.isPromo) {
                totalPrice = item.attributes.price
            }
        }
        for (modifierItem in item.drinks.modifierItems) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierItem in item.getFries().getModifierItems()) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierGroup in item.getModifierGroups()) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                    "hidden", ignoreCase = true
                )
            ) {
                if (modifierGroup.attributes.freeModifiersCount == 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                        modifierGroup
                    )
                ) {
                    _showHideModifierItemPrice.value = Pair(true, modifierGroupPosition)
                }
                var selectedFreeModifier = 0
                for (modifierItem in modifierGroup.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                            val totalSelectedFreeModifier =
                                selectedFreeModifier + modifierItem.attributes.quantity
                            if (totalSelectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                if (modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                                        modifierGroup
                                    )
                                ) {
                                    _showHideModifierItemPrice.value =
                                        Pair(false, modifierGroupPosition)
                                }
                                selectedFreeModifier += modifierItem.attributes.quantity
                            } else {
                                if (modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                                        modifierGroup
                                    )
                                ) {
                                    _showHideModifierItemPrice.value =
                                        Pair(true, modifierGroupPosition)
                                }
                                selectedFreeModifier =
                                    modifierGroup.attributes.freeModifiersCount
                                totalPrice += modifierItem.attributes.price * (totalSelectedFreeModifier - modifierGroup.attributes.freeModifiersCount)
                            }
                        } else {
                            totalPrice += modifierItem.attributes.price * modifierItem.attributes.quantity
                        }
                    }
                }
                if (selectedFreeModifier == 0 && modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                        modifierGroup
                    )
                ) {
                    _showHideModifierItemPrice.value = Pair(false, modifierGroupPosition)
                }
            }
        }
        for (ingredient in item.getIngredients()) {
            if (ingredient.attributes.isSelected) {
                totalPrice += ingredient.attributes.price
            }
        }

        try {
            if (item.isCombo) {
                val componentsList =
                    menuItemComboList[comboIndex]?.values?.toList() ?: arrayListOf()
                for (components in componentsList) {
                    for (componentModifierItem in components.items) {
                        if (componentModifierItem.selected) {
                            if (!item.isSandwichOnly) totalPrice += componentModifierItem.price * componentModifierItem.quantity
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

        val itemTotalPrice = totalPrice * item.attributes.quantity
        _showMenuItemTotal.value = itemTotalPrice
    }

    private fun getCartMenuItemsTotalPrice() {
        var cartTotal = 0.0
        invokeAtBackground {
            cacheUtilHelper.getCartItems {
                for (menuItem in it) {
                    cartTotal += getCartTotalPricePer(menuItem, -1)
                }
                cartMenuItemsTotalPrice = cartTotal
            }
        }
    }

    private fun getCartTotalPricePer(item: MenuItem, modifierGroupPosition: Int): Double {
        var orderStoreId: String? = null
        var counter = 0
        var comboIndex = 0
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }

        var totalPrice: Double = item.getPrice(orderStoreId)
        if (item.sizes.modifierItems.size > 0) {
            if (!item.isPromotion) {
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
        for (modifierItem in item.getFries().getModifierItems()) {
            if (modifierItem.attributes.isSelected) {
                totalPrice += modifierItem.attributes.price
                break
            }
        }
        for (modifierGroup in item.getModifierGroups()) {
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
                                if (modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                                        modifierGroup
                                    )
                                ) {
                                    _showHideModifierItemPrice.value =
                                        Pair(false, modifierGroupPosition)
                                }
                                selectedFreeModifier += modifierItem.attributes.quantity
                            } else {
                                if (modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == item.modifierGroups.indexOf(
                                        modifierGroup
                                    )
                                ) {
                                    _showHideModifierItemPrice.value =
                                        Pair(true, modifierGroupPosition)
                                }
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
        for (ingredient in item.getIngredients()) {
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
        val itemTotalPrice = totalPrice * item.attributes.quantity
        return itemTotalPrice
    }

    /**Function for fetching the combo menu item components from the current selected menu item **/
    private fun getComboMenuItemComponents() {
        menuItem?.let {
            if (it.combo.size > 0) {
                if (isFromCartEdit) {
                    it.combo.forEachIndexed { comboIndex, menuItemCombo ->
                        val menuItemComponentList = hashMapOf<Int, MenuItemComponents>()
                        menuItemCombo.components?.forEachIndexed { menuItemComponentIndex, menuItemComponents ->
                            if (menuItemComponents.items.isNotEmpty()) {
                                menuItemComponentList[menuItemComponentIndex] = menuItemComponents
                            }
                        }
                        menuItemComboList[comboIndex] = menuItemComponentList
                    }
                }
                val comboItems =
                    it.combo[comboIndex]?.components?.deepCopy(MenuItemComponents::class.java)
                comboItems?.let { combos ->
                    val menuItemComponentItems = arrayListOf<MenuItemComponentItem>()
                    combos.forEachIndexed { index, menuItemComponents ->
                        if (menuItemComponents.items.isNotEmpty()) {
                            menuItemComponentItems.add(
                                MenuItemComponentItem(
                                    index,
                                    menuItemComponents
                                )
                            )
                        }
                    }
                    getComboComponentAdapterItems(menuItemComponentItems)
                }

            } else {
                _showComboMenuItemComponents.value = ArrayList()
            }
        } ?: run {
            _showComboMenuItemComponents.value = ArrayList()
        }
    }

    private fun getComboComponentAdapterItems(menuItemComponents: ArrayList<MenuItemComponentItem>) {
        val applicationId = CacheUtil.getInstance().application.id
        invoke {
            val comboComponentAdapterList = arrayListOf<ComboComponentAdapterItem>()
            menuItemComponents.forEachIndexed { menuItemComponentPos, menuItemComponentItem ->
                val menuItemComponent = menuItemComponentItem.menuItemComponents
                val isContainMenuItemComponentKey =
                    menuItemComboList[comboIndex]?.containsKey(menuItemComponentPos) ?: false
                if (isContainMenuItemComponentKey) {
                    menuItemComboList[comboIndex]?.get(menuItemComponentPos)?.let {
                        if (it.items.isNotEmpty()) {
                            it.items = componentsSelectFirst(it)
                            val showCheckBoxIndicator = it.items.any { it.selected }
                            comboComponentAdapterList.add(
                                ComponentAdapterItem(
                                    it,
                                    showCheckBoxIndicator,
                                    menuItemComponentPos
                                )
                            )
                            val isAboveMax =
                                isAboveMaxComponent(it, componentModifierCount(it.items))
                            it.items.forEachIndexed { componentModifierItemPos, componentModifierItem ->
                                val modifierGroupIds = hashMapOf<String, ModifierGroup>()
                                for (modifierGroup in componentModifierItem.modifierGroupsMap) {
                                    modifierGroupIds[modifierGroup.id] = modifierGroup
                                }

                                if (componentModifierItem.modifierGroups.isEmpty() && modifierGroupIds.isNotEmpty()) {
                                    val modifierGroups =
                                        parseComponentModifierGroup(applicationId, modifierGroupIds)
                                    componentModifierItem.modifierGroups = ArrayList(modifierGroups)
                                    menuItemComponent.items[componentModifierItemPos] =
                                        componentModifierItem
                                }

                                comboComponentAdapterList.add(
                                    ComponentModifierAdapterItem(
                                        it,
                                        componentModifierItem,
                                        isAboveMax,
                                        menuItemComponentPos,
                                        componentModifierItemPos
                                    )
                                )
                            }
                        }
                    }
                } else {
                    if (menuItemComponent.items.isNotEmpty()) {
                        menuItemComponent.items = componentsSelectFirst(menuItemComponent)
                        val showCheckBoxIndicator = menuItemComponent.items.any { it.selected }
                        comboComponentAdapterList.add(
                            ComponentAdapterItem(
                                menuItemComponent,
                                showCheckBoxIndicator,
                                menuItemComponentPos
                            )
                        )

                        menuItemComponent.items.sortWith { menuItem1: ComponentModifierItem, menuItem2: ComponentModifierItem -> menuItem1.displayOrder - menuItem2.displayOrder }
                        menuItemComponent.items = componentsSelectFirst(menuItemComponent)
                        val isAboveMax = isAboveMaxComponent(
                            menuItemComponent,
                            componentModifierCount(menuItemComponent.items)
                        )
                        menuItemComponent.items.forEachIndexed { componentModifierPos, compoItem ->

                            menuRepository.getMenuItemDbBy(
                                applicationId,
                                compoItem.name.getValue(LocaleUtil.getInstance().language)
                            ) { menuItemDb ->
                                menuItemDb?.let {
                                    val menuItemData =
                                        GsonHelper.gsonFinal()
                                            .fromJson(it.data, MenuItem::class.java)
                                    menuItemData?.let {
                                        compoItem.menuItem = it
                                    }
                                }
                            }


                            val modifierGroupIds = hashMapOf<String, ModifierGroup>()
                            for (modifierGroup in compoItem.modifierGroupsMap) {
                                modifierGroupIds[modifierGroup.id] = modifierGroup
                            }
                            if (modifierGroupIds.isNotEmpty()) {
                                val modifierGroups =
                                    parseComponentModifierGroup(applicationId, modifierGroupIds)
                                compoItem.modifierGroups = ArrayList(modifierGroups)
                                menuItemComponent.items[componentModifierPos] = compoItem
                            }

                            comboComponentAdapterList.add(
                                ComponentModifierAdapterItem(
                                    menuItemComponent,
                                    compoItem,
                                    isAboveMax,
                                    menuItemComponentPos,
                                    componentModifierPos
                                )
                            )

                        }


                        if (isAboveMax) {
                            comboComponentAdapterList.map { adapterItem ->
                                if (adapterItem is ComponentModifierAdapterItem && adapterItem.menuItemComponentPos == menuItemComponentPos) {
                                    adapterItem.isAboveMax = true
                                }
                            }
                        }
                        menuItemComponentList[menuItemComponentPos] = menuItemComponent
                        menuItemComboList[comboIndex] = menuItemComponentList
                    }


                }
            }
            _showComboMenuItemComponents.value = comboComponentAdapterList
        }

    }

    /**Function for fetching the componentModifierItem data (Modifier group, modifier item and menu item from the database),
     * and stored it in a map of menuItemComponents and then stored to menuItemCombo map, for easy retrieval and modification of data**/
    private fun parseComponentModifierGroup(
        applicationId: String,
        modifierGroupIds: HashMap<String, ModifierGroup>
    ): LinkedList<ModifierGroup> {
        val modifierGroups = LinkedList<ModifierGroup>()
        menuRepository.getAllModifierGroupBy(
            applicationId, modifierGroupIds.keys.toTypedArray()
        ) { modifierGroupDbs ->
            for (modifierGroupDb in modifierGroupDbs) {
                if (modifierGroupDb.type != ModifierGroupType.SIZES.value) {
                    val modifierGroupCdn =
                        MenuUtil.processModifierGroupCustomObject(
                            GsonHelper.gsonFinal().fromJson(
                                modifierGroupDb.data, ModifierGroupCdn::class.java
                            )
                        )
                    val mapModifierGroup = modifierGroupIds[modifierGroupCdn.id]
                    val modifierGroupModifierItems =
                        modifierGroupIds[modifierGroupCdn.id]!!.modifiers
                    val modifierGroupModifierItemIds = arrayListOf<String>()
                    val excludedModifierItems = HashSet<String>()
                    mapModifierGroup?.let {
                        val excludedModifierIds =
                            it.excludeModifiers.map { t -> t.toString() }
                        excludedModifierItems.addAll(excludedModifierIds)
                    }


                    // Exclude by location
                    val orderStore = CacheUtil.getInstance().getOrderStore()
                    val excludedItemsByLocationId = Observable.fromArray(
                        modifierGroupIds[modifierGroupCdn.id]
                    ).map { modifierGroup: ModifierGroup? ->
                        if (orderStore == null) return@map java.util.ArrayList<ModifierItem>()
                        // if group contains store id in included location, add all modifiers within it to the excluded items
                        // else check modifier item excluded location if it contains store id
                        if (Observable.fromIterable(modifierGroup!!.excludedLocations)
                                .map { obj: Int -> obj.toString() }
                                .any { excludedLocation: String -> excludedLocation == orderStore.id }
                                .blockingGet()
                        ) {
                            return@map modifierGroup!!.modifiers
                        } else {
                            return@map Observable.fromIterable<ModifierItem>(
                                modifierGroup!!.modifierItems
                            ).filter { modifierItem: ModifierItem ->
                                Observable.fromIterable<Int>(
                                    modifierItem.attributes.excludedLocations
                                )
                                    .any { item: Int -> item.toString() == orderStore.id }
                                    .blockingGet()
                            }.toList().blockingGet()
                        }
                    }.flatMap { source: List<ModifierItem>? ->
                        Observable.fromIterable(
                            source
                        )
                    }.map { obj: ModifierItem -> obj.id }.toList().blockingGet()
                    excludedModifierItems.addAll(excludedItemsByLocationId)


                    val modifierGroupModifierItemDisplayOrders =
                        hashMapOf<String, Int>()
                    for (modifierGroupModifierItem in modifierGroupModifierItems) {
                        if (!excludedModifierItems.contains(
                                modifierGroupModifierItem.id
                            )
                        ) {
                            modifierGroupModifierItemIds.add(
                                modifierGroupModifierItem.id
                            )
                            modifierGroupModifierItemDisplayOrders[modifierGroupModifierItem.id] =
                                modifierGroupModifierItems.indexOf(
                                    modifierGroupModifierItem
                                )
                        }
                    }

                    val modifierItems = LinkedList<ModifierItem>()
                    menuRepository.getModifierItemBy(
                        applicationId, modifierGroupModifierItemIds.toTypedArray()
                    ) { modifierItemDbs ->
                        for (modifierItemDb in modifierItemDbs) {
                            val modifierItemCdn = GsonHelper.gsonFinal().fromJson(
                                modifierItemDb.data, ModifierItemCdn::class.java
                            )

                            val modifierItem = ModifierItemCdn.convert(
                                modifierItemCdn, LocaleUtil.getInstance().language
                            )
                            modifierItem.attributes.isSelected =
                                modifierItem.attributes.minimum == 1
                            if (modifierGroupModifierItemDisplayOrders.containsKey(
                                    modifierItem.id
                                ) && modifierGroupModifierItemDisplayOrders[modifierItem.id] != null
                            ) {
                                modifierGroupModifierItemDisplayOrders[modifierItem.id]?.let {
                                    modifierItem.attributes.displayOrder = it
                                }
                            }
                            val isExcluded =
                                orderStore != null && Observable.fromIterable(
                                    modifierItem.attributes.excludedLocations
                                )
                                    .any { item: Int -> item.toString() == orderStore.id }
                                    .blockingGet()

                            if (!isExcluded) {
                                modifierItems.add(modifierItem)
                            }
                        }

                        modifierItems.sortWith { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                        val modifierGroup = ModifierGroupCdn.convert(
                            modifierGroupCdn, LocaleUtil.getInstance().language
                        )
                        if (mapModifierGroup != null) {
                            modifierGroup.attributes.minimum = mapModifierGroup.min
                            modifierGroup.attributes.maximum = mapModifierGroup.max
                            modifierGroup.attributes.displayOrder =
                                mapModifierGroup.displayOrder
                            modifierGroup.attributes.freeModifiersCount =
                                mapModifierGroup.freeModifiersCount
                            modifierGroup.setDefaultModifiers(
                                java.util.ArrayList(
                                    mapModifierGroup.defaultModifiers
                                )
                            )
                            if (modifierGroup.attributes.visibilityConfig == null && mapModifierGroup.visibilityConfig != null) {
                                modifierGroup.attributes.visibilityConfig =
                                    mapModifierGroup.visibilityConfig
                            }
                        }
                        val modifierItemsFinal =
                            ArrayList<ModifierItem>(modifierItems)
                        if (modifierItemsFinal.size > 0) {
                            val defaultModifiers = modifierGroup.defaultModifiers
                            if (modifierItemsFinal.size > 0 && modifierGroup.attributes.minimum > 0) {
                                if (defaultModifiers.size > 0) {
                                    var selected = 0
                                    for (i in modifierItemsFinal.indices) {
                                        val modifierItem = modifierItemsFinal[i]
                                        if (selected < modifierGroup.attributes.maximum && defaultModifiers.contains(
                                                modifierItem.id
                                            )
                                        ) {
                                            modifierItemsFinal[i].attributes.isSelected =
                                                true
                                            selected++
                                        }
                                    }
                                } else if (modifierGroup.attributes.minimum == 1) {
                                    modifierItemsFinal[0].attributes.isSelected =
                                        true
                                }
                            }
                            modifierGroup.modifierItems = modifierItemsFinal
                            modifierGroup.modifiers = modifierItemsFinal
                            modifierGroups.add(modifierGroup)
                        }
                    }
                }
            }
            modifierGroups.sortWith { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }
            updateInitialModifierVisibility(modifierGroups)
        }
        return modifierGroups
    }


    /** Function for checking to show the modifier item price per menu item,
     *  and applying it to ComboMealItemModifierItemAdapter
     * **/
    fun showHideModifierItemPrice(menuItem: MenuItem) {
        for (modifierGroup in menuItem.modifierGroups) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                    "hidden", ignoreCase = true
                )
            ) {
                val modifierGroupPosition = menuItem.modifierGroups.indexOf(modifierGroup)
                if (modifierGroup.attributes.freeModifiersCount == 0) {
                    _showHideModifierItemPrice.value = Pair(true, modifierGroupPosition)
                } else {
                    var selectedFreeModifier = 0
                    for (modifierItem in modifierGroup.modifierItems) {
                        if (modifierItem.attributes.isSelected) {
                            if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                val totalSelectedFreeModifier =
                                    selectedFreeModifier + modifierItem.attributes.quantity
                                if (totalSelectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                    if (modifierGroupPosition >= 0) {
                                        _showHideModifierItemPrice.value =
                                            Pair(false, modifierGroupPosition)
                                    }
                                    selectedFreeModifier += modifierItem.attributes.quantity
                                } else {
                                    if (modifierGroupPosition >= 0) {
                                        _showHideModifierItemPrice.value =
                                            Pair(true, modifierGroupPosition)
                                    }
                                    selectedFreeModifier =
                                        modifierGroup.attributes.freeModifiersCount
                                }
                            }
                        }
                    }
                    if (selectedFreeModifier == 0 && modifierGroup.attributes.freeModifiersCount > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.modifierGroups.indexOf(
                            modifierGroup
                        )
                    ) {
                        _showHideModifierItemPrice.value = Pair(false, modifierGroupPosition)
                    }
                }
            }
        }
    }

    fun setMenuCategory(menuCategoryId: String?) {
        val menuCategory = MenuCategory()
        menuCategory.id = menuCategoryId
        this.menuCategory = menuCategory
    }

    fun setMenuCategory(menuItem: MenuItem) {
        var found = false
        for (menuCategory in excludedMenuCategories(menuCategories)) {
            if (!found) {
                for (menuCategoryItem in menuCategory.menuItems) {
                    if (menuCategoryItem.id == menuItem.id) {
                        this.menuCategory = menuCategory
                        found = true
                        break
                    }
                }
            } else {
                break
            }
        }
    }

    private fun excludedMenuCategories(menuCategories: List<MenuCategory>): List<MenuCategory> {
        excludedMenu = arrayListOf()
        if (CacheUtil.getOrderStoreId() != null && excludedCategories.isNotEmpty() && excludedCategories.containsKey(
                CacheUtil.getOrderStoreId()
            )
        ) {
            val categories: MutableList<MenuCategory> = ArrayList(menuCategories)
            excludedCategories[CacheUtil.getOrderStoreId()]?.let { categories.removeAll(it) }
            this.excludedMenu = categories
        } else {
            this.excludedMenu = menuCategories
        }
        excludedMenu.sortedWith { menuCategory1, menuCategory2 -> menuCategory1.displayOrder - menuCategory2.displayOrder }
        return excludedMenu
    }

    /**Function for setting the value of selected to true of the first item of ComponentModifier per MenuItemComponent
     * **/
    private fun componentsSelectFirst(components: MenuItemComponents): ArrayList<ComponentModifierItem> {
        val noItemSelected = checkIfNoItemSelected(components, components.items)
        if (noItemSelected) {
            if (components.minimum == 1 && components.items.isNotEmpty()) {
                components.items[0].selected = true
                components.items[0].quantity = 1
                selectedComponents[components.id] = 0
            }
        }
        return components.items
    }

    private fun checkIfNoItemSelected(
        component: MenuItemComponents, items: ArrayList<ComponentModifierItem>
    ): Boolean {
        val size = items.size
        for (i in 0 until size) {
            if (items[i].selected) {
                selectedComponents[component.id] = i
                return false
            }
        }
        return true
    }

    /**Function for changing the quantity of the current menu item,
     *after updating the quantity getMenuItemTotalPrice is always called
     * **/
    fun changeMenuItemQuantity(add: Boolean) {
        menuItem?.let {
            if (!add && it.attributes.quantity != 1) {
                it.attributes.quantity = menuItem!!.attributes.quantity - 1
            } else if (add) {
                it.attributes.quantity = menuItem!!.attributes.quantity + 1
            }
            _showMenuItemQuantity.value = it.attributes.quantity
            getMenuItemTotalPrice(it, -1)
        }
    }

    /**Function updating the selected size of the combo meal menu item,
     * update also the combo index, and update the list of combo menuItemComponents and reload it
     * **/
    override fun onSelectSize(size: ModifierItem, pos: Int) {
        previousComboIndex = comboIndex
        selectedComponents = hashMapOf()
        menuItemComponentList = hashMapOf()
        menuItemComboList = hashMapOf()
        comboIndex = pos
        _updateComboSizeList.value = Pair(pos, previousComboIndex)
        getComboMenuItemComponents()
        menuItem?.let {
            getMenuItemTotalPrice(it, -1)
        }
    }

    /**Inheriting the function fromComboMealComponentAdapter.ComboComponentListener
     * This function tells the viewModel to update the current item attributes
     * to selected and previous item attributes to unselected,
     * If the selected ComponentModifierItem has list of ModifierGroup and Modifier items
     * the list will be shown into a separate view.
     * **/
    override fun onComponentCheckedChanged(
        isSelected: Boolean,
        menuItemComponent: MenuItemComponents,
        componentModifierItem: ComponentModifierItem,
        menuItemComponentPos: Int,
        componentModifierItemPos: Int
    ) {
        var _isSelected = isSelected
        val showModifierDetail = checkToShowModifierDetail(componentModifierItem)
        menuItemComponent.apply {
            if (showModifierDetail) {
                _showComboMealItemDetails.value = ShowComboMealItemDetails(
                    menuItemComponent,
                    componentModifierItem, menuItemComponentPos, isSelected
                )
            } else {
                if (maximum == 1) {
                    if (minimum == 1) {
                        _isSelected = true
                    }

                    items.forEach { item ->
                        if (item.id == componentModifierItem.id) {
                            item.selected = _isSelected
                            item.quantity = 1
                        } else {
                            item.selected = false
                            item.quantity = 1
                        }
                    }
                } else {
                    var selectedItems = 0
                    for (item in items) {
                        if (item.selected) {
                            selectedItems += item.quantity
                        }
                    }

                    if (isSelected && selectedItems >= maximum) {
                        _showToastMessage.value =
                            name.toTranslate() + " " + CacheUtil.getInstance()
                                .getTranslations(
                                    Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                                )
                    } else {
                        items.map { _componentModifierItem ->
                            if (_componentModifierItem.id == componentModifierItem.id) {
                                _componentModifierItem.selected = isSelected
                                if (!isSelected) {
                                    componentModifierItem.quantity = 1
                                }
                            }
                        }
                    }
                }
                menuItemComboList[comboIndex]?.set(menuItemComponentPos, menuItemComponent)
                updateMenuItemComponentIndicator(menuItemComponent, componentModifierItem)
            }

        }
    }

    /**Inheriting the function fromComboMealComponentAdapter.ComboComponentListener
     * This function tells the viewModel to update the current item quantity
     * and check if meets the condition of minimum and maximum setting
     * **/
    override fun onComponentQuantityChanged(
        add: Boolean,
        menuItemComponent: MenuItemComponents,
        componentModifierItem: ComponentModifierItem,
        isQuantityVisible: Boolean,
        menuItemComponentPos: Int,
        componentModifierItemPos: Int
    ) {
        if (add) {
            var proceed = true
            if (componentModifierItem.quantity < componentModifierItem.maximum) {
                if (isAboveMaxComponent(
                        menuItemComponent,
                        componentModifierCount(menuItemComponent.items)
                    )
                ) {
                    proceed = false
                    _showToastMessage.value =
                        menuItemComponent.name.toTranslate() + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                            )
                } else {
                    if (componentModifierItem.maximum < componentModifierItem.quantity + 1) {
                        proceed = false
                        _showToastMessage.value =
                            menuItemComponent.name.toTranslate() + " " + CacheUtil.getInstance()
                                .getTranslations(
                                    Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                                )
                    }
                }
            } else {
                proceed = false
                _showToastMessage.value =
                    CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
            }

            if (proceed) {
                val item = menuItemComponent.items.find { it.id == componentModifierItem.id }
                var currentModifierTotal = 0
                if (item != null && item.id == componentModifierItem.id) {


                    currentModifierTotal += menuItemComponent.items.sumOf { if (it.selected) it.quantity else 0 }

                    if (currentModifierTotal < menuItemComponent.maximum) {
                        if (!item.selected && item.quantity <= 1) {
                            item.quantity = 1
                        } else {
                            item.quantity += 1
                        }

                        if (!item.selected) {
                            item.selected = true
                        }
                    }
                }
            }
            menuItemComboList[comboIndex]?.set(menuItemComponentPos, menuItemComponent)
            updateMenuItemComponentIndicator(menuItemComponent, componentModifierItem)
        } else {
            val item = menuItemComponent.items.find { it.id == componentModifierItem.id }

            if (componentModifierItem.quantity == 1) {
                if (item != null && MenuUtil.areComponentModifierSame(
                        item,
                        componentModifierItem
                    )
                ) {
                    item.selected = false
                    if (item.modifierGroups.size > 0) {
                        item.modifierGroups.forEach {
                            it.modifierItems.forEach {
                                it.attributes.quantity = 0
                                it.attributes.isSelected = false
                            }
                        }
                    }
                }
            } else {
                item?.let {
                    it.quantity -= 1
                    if (it.quantity == 0) {
                        it.selected = false
                    }
                }
            }
            menuItemComboList[comboIndex]?.set(menuItemComponentPos, menuItemComponent)
            updateMenuItemComponentIndicator(menuItemComponent, componentModifierItem)
        }
    }

    private fun updateMenuItemComponentIndicator(
        menuItemComponent: MenuItemComponents, componentModifierItem: ComponentModifierItem
    ) {
        val shouldShowIndicator = menuItemComponent.items.any { it.selected }
        _updateComboComponents.value =
            Triple(shouldShowIndicator, menuItemComponent, componentModifierItem)
        menuItem?.let {
            getMenuItemTotalPrice(it, -1)
        }
    }

    private fun checkToShowModifierDetail(
        components: ComponentModifierItem,
    ): Boolean = components.modifierGroups.size >= 1


    override fun onCheckedChanged(
        isSelected: Boolean, modifierGroup: ModifierGroup, modifierItem: ModifierItem
    ) {
        var _isSelected = isSelected
        modifierGroup.let { group ->
            if (group.attributes.maximum == 1) {
                if (group.attributes.minimum == 1) {
                    _isSelected = true
                }
                for (item in modifierGroup.modifierItems) {
                    if (item.id == modifierItem?.id) {
                        item.attributes.isSelected = _isSelected
                        item.attributes.quantity = 1
                        modifierItem?.attributes?.isSelected = _isSelected
                        modifierItem?.attributes?.quantity = 1
                    } else {
                        item.attributes.isSelected = false
                        item.attributes.quantity = 0
                    }
                }

            } else {
                var selectedItems = 0
                for (item in group.modifierItems) {
                    if (item.attributes.isSelected) {
                        selectedItems += item.attributes.quantity
                    }
                }

                if (isSelected && selectedItems >= modifierGroup.attributes.maximum) {
                    _showToastMessage.value =
                        modifierGroup.attributes.name + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                            )
                } else {
                    group.modifierItems.map { _modifierItem ->
                        if (_modifierItem.id == modifierItem.id) {
                            _modifierItem.attributes.isSelected = isSelected
                            if (!isSelected) {
                                _modifierItem.attributes.quantity = 1
                            }
                        }
                    }
                }
            }
            updateModifierGroupSelectionIndicator(isSelected, modifierGroup, modifierItem)
        }
    }


    override fun onQuantityChanged(
        add: Boolean,
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem,
        isQuantityVisible: Boolean,
    ) {
        if (add) {
            var proceed = true
            var quantity = modifierItem.attributes.quantity
            if (modifierItem.attributes.quantity < modifierItem.attributes.maximum) {
                quantity += 1
                if (isAboveMax(
                        modifierGroup, modifierItemQuantityCount(modifierGroup.modifierItems)
                    )
                ) {
                    proceed = false
                    _showToastMessage.value =
                        modifierGroup.attributes.name + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                            )
                } else {
                    if (modifierItem.attributes.maximum < modifierItem.attributes.quantity + 1) {
                        proceed = false
                        _showToastMessage.value =
                            modifierGroup.attributes.name + " " + CacheUtil.getInstance()
                                .getTranslations(
                                    Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                                )
                    }
                }
            } else {
                proceed = false
                _showToastMessage.value =
                    CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
            }

            if (proceed) {
                val item = modifierGroup.modifierItems.find { it.id == modifierItem.id }
                var modifierTotal = 0
                var currentModifierTotal = 0
                if (item != null && item.id == modifierItem.id) {
                    modifierItem.attributes.isShouldShowQuantity = true
                    for (eachModifierItem in modifierGroup.modifierItems) {
                        if (eachModifierItem.attributes.isShouldShowQuantity) {
                            currentModifierTotal += eachModifierItem.attributes.quantity
                        }
                    }
                    if (currentModifierTotal < modifierGroup.attributes.maximum) {

                        if (!modifierItem.attributes.isSelected && modifierItem.attributes.quantity <= 1) modifierItem.attributes.quantity =
                            1 else modifierItem.attributes.quantity += 1

                        if (!modifierItem.attributes.isSelected) {
                            modifierItem.attributes.isSelected = true
                        }

                    }

                }


                for (eachModifierItem in modifierGroup.modifierItems) {
                    if (eachModifierItem.attributes.isShouldShowQuantity) {
                        modifierTotal += eachModifierItem.attributes.quantity
                    }
                }

                if (modifierTotal >= modifierGroup.attributes.maximum) {
                    for (eachItems in modifierGroup.modifierItems) {
                        eachItems.attributes.setModifierItemInteraction(true)
                        eachItems.attributes.setModifierItemQuantityExceedsMaximum(eachItems.attributes.isShouldShowQuantity || eachItems.attributes.isSelected)
                    }
                }
                updateModifierGroupSelectionIndicator(
                    modifierItem.attributes.isSelected, modifierGroup, modifierItem
                )
            }
        } else {
            var selectedItemMinusTotal = 0
            val group = modifierGroup
            val modifierGroupMax = modifierGroup.attributes.maximum
            val item = group.modifierItems.find { it.id == modifierItem.id }
            if (modifierItem.attributes.quantity == 1) {
                if (item != null && MenuUtil.areModifiersSame(item, modifierItem)) {
                    modifierItem.attributes.isSelected = false
                    modifierItem.attributes.isShouldShowQuantity = false

                    val selectedItemQuantity = MenuUtil.modifierItemQuantityCount(group)
                    if (selectedItemQuantity < modifierGroupMax) {
                        for (eachModifierItem in group.modifierItems) {
                            if (eachModifierItem.attributes.maximum > 1 && modifierGroupMax > 1 && eachModifierItem.id != item.id && !modifierItem.attributes.isSelected) {
                                eachModifierItem.attributes.setModifierItemQuantityExceedsMaximum(
                                    true
                                )
                                eachModifierItem.attributes.setModifierItemInteraction(true)
                            }
                        }
                    }
                }
            } else {
                modifierItem.attributes.quantity -= 1
                for (eachItem in group.modifierItems) {
                    if (eachItem.attributes.isShouldShowQuantity) {
                        selectedItemMinusTotal += eachItem.attributes.quantity
                    }
                }
                if (selectedItemMinusTotal < modifierGroup.attributes.maximum) {
                    for (eachModifierItem in group.modifierItems) {
                        if (eachModifierItem.attributes.hasUserInteractedWithModifierItem()) {
                            eachModifierItem.attributes.setModifierItemQuantityExceedsMaximum(false)
                            eachModifierItem.attributes.setModifierItemInteraction(false)
                        }
                    }
                }
                if (modifierItem.attributes.quantity == 0) {
                    modifierItem.attributes.isSelected = false
                }
            }
            updateModifierGroupSelectionIndicator(
                modifierItem.attributes.isSelected, modifierGroup, modifierItem
            )
        }
    }


    fun isAboveMax(group: ModifierGroup, count: Int): Boolean {
        return group.attributes.maximum - 1 < count
    }

    fun modifierItemQuantityCount(modifiers: List<ModifierItem>): Int {
        var counter = 0
        for (modifierItem in modifiers) {
            if (modifierItem.attributes.isSelected) counter += modifierItem.attributes.quantity
        }
        return counter
    }

    private fun updateModifierGroupSelectionIndicator(
        hasSelectedModifierItem: Boolean, modifierGroup: ModifierGroup, modifierItem: ModifierItem
    ) {

        val shouldShowIndicator = hasSelectedModifierItemsBy(modifierGroup)

        _updateComboItemDetailModifier.value =
            Triple(shouldShowIndicator, modifierGroup, modifierItem)
    }


    fun getComponentModifierItemTotalPrice(updateModifierGroupList: List<ModifierGroup>) {
        var totalPrice = 0.0
        for (modifierGroup in updateModifierGroupList) {
            for (i in modifierGroup.modifierItems.indices) {
                if (modifierGroup.modifierItems[i].attributes.isSelected) {
                    totalPrice += modifierGroup.modifierItems[i].attributes.price * modifierGroup.modifierItems[i].attributes.quantity
                }
            }
        }
        _showComponentModifierItemTotal.value = totalPrice
    }

    fun updateSelectedModifierItem(
        isSelected: Boolean,
        menuItemComponent: MenuItemComponents,
        menuItemComponentIndex: Int,
        component: ComponentModifierItem,
        updatedModifierGroupList: List<ModifierGroup>?
    ) {
        var _isSelected = isSelected
        menuItemComponent.apply {
            if (maximum == 1) {
                if (minimum == 1) {
                    _isSelected = true
                }

                items.forEach { item ->
                    if (item.id == component.id) {
                        if (item.modifierGroups.isNotEmpty()) {
                            updatedModifierGroupList?.let { selectedModifierGroup ->
                                item.selected = ifHasSelectedModifierItems(selectedModifierGroup)
                                item.attributes.price =
                                    item.price + selectedModifierPrice(selectedModifierGroup)
                                item.modifierGroups.clear()
                                item.modifierGroups.addAll(selectedModifierGroup)
                            }
                        } else {
                            item.selected = _isSelected
                            if (item.quantity < 1) {
                                item.quantity = 1
                            }
                        }
                    } else {
                        item.selected = false
                        item.quantity = 1
                        if (item.modifierGroups.size > 0) {
                            item.modifierGroups.forEach {
                                it.modifierItems.forEach {
                                    it.attributes.quantity = 0
                                    it.attributes.isSelected = false
                                }
                            }
                        }
                    }
                }
            } else {
                var selectedItems = 0
                for (item in items) {
                    if (item.selected) {
                        selectedItems += item.quantity
                    }
                }

                if (isSelected && selectedItems >= maximum) {
                    _showToastMessage.value =
                        name.toTranslate() + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                            )
                } else {
                    items.map { _componentModifierItem ->
                        if (_componentModifierItem.id == component.id) {
                            if (_componentModifierItem.modifierGroups.isNotEmpty()) {
                                updatedModifierGroupList?.let { selectedModifierGroup ->
                                    _componentModifierItem.selected =
                                        ifHasSelectedModifierItems(selectedModifierGroup)
                                    _componentModifierItem.attributes.price =
                                        _componentModifierItem.price + selectedModifierPrice(
                                            selectedModifierGroup
                                        )
                                    _componentModifierItem.modifierGroups.clear()
                                    _componentModifierItem.modifierGroups.addAll(
                                        selectedModifierGroup
                                    )
                                }
                            } else {
                                _componentModifierItem.selected = isSelected
                            }

                            if (!isSelected) {
                                if (_componentModifierItem.quantity < 1) {
                                    _componentModifierItem.quantity = 1
                                }
                            }
                        }
                    }
                }
            }

            menuItemComboList[comboIndex]?.set(menuItemComponentIndex, menuItemComponent)
            updateMenuItemComponentIndicator(menuItemComponent, component)
        }
    }

    private fun selectedModifierPrice(selectedModifierGroup: List<ModifierGroup>): Double {
        var modifierPrice = 0.0
        selectedModifierGroup.forEach { mdg ->
            mdg.modifierItems.filter { t -> t.attributes.isSelected }.forEach {
                modifierPrice += (it.attributes.price * it.attributes.quantity.toDouble())
            }
        }

        return modifierPrice
    }

    private fun ifHasSelected(items: List<ModifierGroup>): Int {
        var counter = 0
        items.forEach { mdg ->
            mdg.modifierItems.filter { t -> t.attributes.isSelected }.let {
                counter += it.size
            }
        }

        return counter
    }

    private fun ifHasSelectedModifierItems(items: List<ModifierGroup>): Boolean {
        var counter = 0
        items.forEach { mdg ->
            if (mdg.attributes.minimum < 1) {
                counter++
            } else {
                mdg.modifierItems.filter { t -> t.attributes.isSelected }.let {
                    counter += it.size
                }
            }

        }

        return counter > 0
    }

    fun updateCartItem(isPromo: Boolean, cartMenuItemPos: Int) {
        if (menuItem == null) {
            return
        }
        invoke {
            var invalid = false
            for (modifierGroup in menuItem!!.modifierGroups) {
                var selected = 0
                for (modifierItem in modifierGroup.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        selected += modifierItem.attributes.quantity
                    }
                }
                if (selected < modifierGroup.attributes.minimum) {
                    _showToastMessage.value = CacheUtil.getInstance().getTranslations(
                        Text.MODIFIER_MINIMUM_LIMIT,
                        "Minimum number of extras (x) not reached for (y)"
                    ).replace("(x)", modifierGroup.attributes.minimum.toString())
                        .replace("(y)", modifierGroup.attributes.name)
                    invalid = true
                    break
                }
            }
            if (!invalid) {
                invalid = checkComboModifiers(menuItem!!)
            }
            if (!invalid) {
                menuItem?.let { item ->
                    if (item.isCombo) {
                        menuItemComboList[comboIndex]?.values?.let { components ->
                            menuItem!!.combo[comboIndex].components = ArrayList(components)
                        }
                    }
                }

                menuItem!!.attributes.notes = ""
                if (!isPromo) {
                    cacheUtilHelper.updateCart(cartMenuItemPos, menuItem!!) {
                        _updateCartItem.value = true
                    }
                } else {
                    if (CacheUtil.getInstance().couponToAdd != null) {
                        val (couponCode, couponName) = CacheUtil.getInstance().couponToAdd
                        CacheUtil.getInstance().couponCode = couponCode
                        CacheUtil.getInstance().couponName = couponName
                        CacheUtil.getInstance().couponMenuItem = menuItem
                        CacheUtil.getInstance().couponToAdd = null
                    } else if (CacheUtil.getInstance().campaignToAdd != null) {
                        val (campaign, promotion) = CacheUtil.getInstance().campaignToAdd
                        CacheUtil.getInstance().campaign = campaign
                        CacheUtil.getInstance().campaignPromotion = promotion
                        CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                        CacheUtil.getInstance().campaignToAdd = null
                    } else if (CacheUtil.getInstance().couponCode != null && CacheUtil.getInstance().couponMenuItem != null) {
                        CacheUtil.getInstance().couponMenuItem = menuItem
                    } else if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotionMenuItem != null) {
                        CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                    }
                    _updateCartItem.value = true
                }

            }
        }
    }

    private fun checkComboModifiers(menuItem: MenuItem): Boolean {
        var invalid = false
        if (menuItem!!.isCombo) {
            var comboIndex = 0
            var trueIndex = 0
            for (size in menuItem!!.sizes.modifierItems) {
                if (size.attributes.isSelected) {
                    trueIndex = comboIndex
                }
                comboIndex++
            }
            if (menuItem!!.isCombo) {
                val componentsList = menuItemComboList[trueIndex]?.values?.toList() ?: arrayListOf()
                for (components in componentsList) {
                    if (invalid) {
                        break
                    }

                    var selected = 0
                    components.items.filter { item -> item.selected }.forEach {
                        selected += it.quantity
                    }

                    if (selected < components.minimum) {
                        _showToastMessage.value = CacheUtil.getInstance().getTranslations(
                            Text.MODIFIER_MINIMUM_LIMIT,
                            "Minimum number of extras (x) not reached for (y)"
                        ).replace("(x)", components.minimum.toString())
                            .replace("(y)", components.name.toTranslate() ?: "")

                        invalid = true
                        break
                    }

                    components.items.filter { componentModifierItem -> componentModifierItem.selected && componentModifierItem.modifierGroups.isNotEmpty() }
                        .forEach { item ->
                            if (invalid) {
                                return@forEach
                            }

                            for (modifierGroup in item.modifierGroups) {
                                var selected = 0
                                modifierGroup.modifierItems.filter { modifierItem -> modifierItem.attributes.isSelected }
                                    .forEach {
                                        selected += it.attributes.quantity
                                    }
                                if (selected < modifierGroup.attributes.minimum) {
                                    _showToastMessage.value =
                                        CacheUtil.getInstance().getTranslations(
                                            Text.MODIFIER_MINIMUM_LIMIT,
                                            "Minimum number of extras (x) not reached for (y)"
                                        ).replace(
                                            "(x)", modifierGroup.attributes.minimum.toString()
                                        ).replace("(y)", modifierGroup.attributes.name)
                                    invalid = true
                                    break
                                }
                            }
                        }
                }
            }
        }
        return invalid
    }

    fun getCartMenuItem() {
        invoke {
            var invalid = false
            val visibleModifierGroups = ArrayList<ModifierGroup>()
            for (group in menuItem!!.modifierGroups) {
                val visibilityConfig = group.attributes.visibilityConfig
                if (visibilityConfig != null && visibilityConfig.initialValue != null) {
                    if (!visibilityConfig.initialValue.equals("hidden", ignoreCase = true)) {
                        visibleModifierGroups.add(group)
                    }
                } else {
                    visibleModifierGroups.add(group)
                }
            }
            for (modifierGroup in visibleModifierGroups) {
                var selected = 0
                modifierGroup.modifierItems.filter { item -> item.attributes.isSelected }.forEach {
                    selected += it.attributes.quantity
                }
                if (selected < modifierGroup.attributes.minimum) {
                    _showToastMessage.value = CacheUtil.getInstance().getTranslations(
                        Text.MODIFIER_MINIMUM_LIMIT,
                        "Minimum number of extras (x) not reached for (y)"
                    ).replace("(x)", modifierGroup.attributes.minimum.toString())
                        .replace("(y)", modifierGroup.attributes.name)

                    invalid = true
                    break
                }
            }
            if (!invalid) {
                invalid = checkComboModifiers(menuItem!!)
            }
            if (!invalid) {
                menuItem?.let { item ->
                    if (item.isCombo) {
                        menuItemComboList[comboIndex]?.values?.let { components ->
                            menuItem!!.combo[comboIndex].components = ArrayList(components)
                        }
                    }
                }
                menuItem!!.attributes.notes = notes.get()
                addCartItem()
            }
        }
    }

    fun addCartItem() {
        if (menuItem == null) {
            return
        }
        var orderStoreId: String? = null
        if (CacheUtil.getInstance().orderStore != null) {
            orderStoreId = CacheUtil.getInstance().orderStore.id
        }
        val store = CacheUtil.getInstance().orderStore
        if (store != null && menuItem!!.disabledStores.containsKey(store.id)) {
            _showError.value = Event(
                ErrorMsgItem(
                    true,
                    CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                        .replace("(x)", store.attributes.name)
                )
            )

        } else {
            _logFirebaseEvent.value = "add_to_cart_event"
            val cartCount = cacheUtilHelper.getCartQuantity()
            menuItem!!.isSandwichOnly = false
            menuItem?.let {
                invoke {
                    it.isFavorite = isFromFavorite
                    cacheUtilHelper.addToCart(it) {
                        if (cartCount == 0) {
                            _showCartExpiry.value = true
                        }
                        _addCartItem.value = it
                    }
                }
            }
            val eventAttributes = java.util.HashMap<String, String>()
            eventAttributes["Item Name"] = menuItem!!.getName(LocaleUtil.getInstance().language)
            eventAttributes["Item Quantity"] = menuItem!!.attributes.quantity.toString()
            var totalPrice = menuItem!!.getPrice(orderStoreId)

            menuItem?.apply {
                sizes.modifierItems.filter { size -> size.attributes.isSelected }
                    .forEach { modifierItem ->
                        eventAttributes["Modifier Name"] =
                            modifierItem.attributes.translatedName.getValue(LocaleUtil.getInstance().language)
                        eventAttributes["Modifier Quantity"] =
                            modifierItem.attributes.quantity.toString()
                        totalPrice += modifierItem.attributes.price
                    }

                drinks.modifierItems.filter { drink -> drink.attributes.isSelected }
                    .forEach { modifierItem ->
                        eventAttributes["Modifier Name"] =
                            modifierItem.attributes.translatedName.getValue(LocaleUtil.getInstance().language)
                        eventAttributes["Modifier Quantity"] =
                            modifierItem.attributes.quantity.toString()
                        totalPrice += modifierItem.attributes.price
                    }

                fries.modifierItems.filter { fries -> fries.attributes.isSelected }
                    .forEach { modifierItem ->
                        eventAttributes["Modifier Name"] =
                            modifierItem.attributes.translatedName.getValue(LocaleUtil.getInstance().language)
                        eventAttributes["Modifier Quantity"] =
                            modifierItem.attributes.quantity.toString()
                        totalPrice += modifierItem.attributes.price
                    }

                modifierGroups.filter { modifierGroup ->
                    modifierGroup.attributes.visibilityConfig == null || modifierGroup.attributes.visibilityConfig.initialValue == null || !modifierGroup.attributes.visibilityConfig.initialValue.equals(
                        "hidden", ignoreCase = true
                    )
                }.forEach { modifierGroup ->
                    var selectedFreeModifier = 0
                    modifierGroup.modifierItems.filter { modifierItem -> modifierItem.attributes.isSelected }
                        .forEach { modifierItem ->
                            eventAttributes["Modifier Name"] =
                                modifierItem.attributes.translatedName.getValue(LocaleUtil.getInstance().language)
                            eventAttributes["Modifier Quantity"] =
                                modifierItem.attributes.quantity.toString()
                            if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                selectedFreeModifier += modifierItem.attributes.quantity
                            } else {
                                totalPrice += modifierItem.attributes.price * modifierItem.attributes.quantity
                            }
                        }
                }

                modifierItems.forEach { modifierItem ->
                    eventAttributes["Modifier Name"] =
                        modifierItem.attributes.translatedName.getValue(LocaleUtil.getInstance().language)
                    eventAttributes["Modifier Quantity"] =
                        modifierItem.attributes.quantity.toString()
                    totalPrice += modifierItem.attributes.price
                }

                ingredients.filter { ingredient -> !ingredient.attributes.isSelected }
                    .forEach { ingredient ->
                        eventAttributes["Ingredient Name"] = ingredient.attributes.name
                        eventAttributes["Ingredient Quantity"] =
                            ingredient.attributes.quantity.toString()
                        totalPrice += ingredient.attributes.price
                    }
            }
            totalPrice += totalPrice * menuItem!!.attributes.quantity.toDouble()
            FirebaseEventUtilHelper.logEventAttributesMetric(
                "AddToCart", eventAttributes, "Price", totalPrice
            )
        }
    }

    fun addRemoveFavorite() {
        menuItem?.let { item ->
            if (!CacheUtil.getInstance().getFavorites().contains(item.id)) {
                CustomersApiCall.getInstance()
                    .addItemToFavorite(AuthUtil.getInstance().getCustomer().id,
                        AddFavoriteItemBody.getCdn(
                            LocaleUtil.getInstance().getLanguage(),
                            menuItem
                        ),
                        object : ApiRequestListener {
                            override fun onSuccess(o: Any) {
                                trackEvent(MixpanelUtil.EVENT_FAVORITE_ITEM)
                                KochavaTrackerUtil.trackEvent(EventType.ADD_TO_WISH_LIST)
                                val favorites = CacheUtil.getInstance().getFavorites()
                                favorites.add(item.id)
                                CacheUtil.getInstance().setFavorites(favorites)
                                val eventAttributes = java.util.HashMap<String, String>()
                                eventAttributes["Item ID"] = item.id
                                eventAttributes["Item Name"] = item.attributes.name
                                _logEvent.value = EventAttributesMetric(
                                    eventName = "AddToFavorites",
                                    eventAttributes = eventAttributes,
                                    eventMetricValue = 0.0
                                )
                                _logAddToWishlistEvent.value = item
                                _setFavoriteItem.value = item
                                _showToastMessage.value = CacheUtil.getInstance()
                                    .getTranslations(Text.ADDED_TO_FAVORITES)

                            }

                            override fun onError(o: Any) {
                                AppLogger.printRetrofitError(o)
                                _showError.value = Event(
                                    ErrorMsgItem(
                                        true, getErrorMessage(
                                            o,
                                            CacheUtil.getInstance()
                                                .getTranslations(Text.ERROR_ADD_TO_FAVORITES),
                                            null
                                        )
                                    )
                                )
                            }
                        })
            } else {
                CustomersApiCall.getInstance()
                    .deleteItemFromFavorites(AuthUtil.getInstance().getCustomer().id,
                        item.id,
                        object : ApiRequestListener {
                            override fun onSuccess(o: Any) {
                                val favorites = CacheUtil.getInstance().getFavorites()
                                favorites.remove(item.id)
                                CacheUtil.getInstance().setFavorites(favorites)
                                _setFavoriteItem.value = item
                                _showToastMessage.value = CacheUtil.getInstance()
                                    .getTranslations(Text.REMOVED_FROM_FAVORITES)

                            }

                            override fun onError(o: Any) {
                                AppLogger.printRetrofitError(o)
                                _showError.value = Event(
                                    ErrorMsgItem(
                                        true, getErrorMessage(
                                            o,
                                            CacheUtil.getInstance()
                                                .getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES),
                                            null
                                        )
                                    )
                                )
                            }
                        })
            }
        }

    }

    private fun addFrequentBoughtItem(addToBag: Boolean, menuAddedToCartPosition: Int) {
        var orderStoreId: String? = null
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().id
        }
        val store = CacheUtil.getInstance().getOrderStore()
        if (store != null && frequentlyMenuItem.disabledStores.containsKey(store.id)) {
            _showLoading.value = false
            _showError.value = Event(
                ErrorMsgItem(
                    true,
                    CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                        .replace("(x)", store.attributes.name)
                )
            )
        } else {
            val eventAttributes = HashMap<String, String>()
            eventAttributes["Item Name"] =
                frequentlyMenuItem.getName(LocaleUtil.getInstance().getLanguage())
            eventAttributes["Item Quantity"] =
                java.lang.String.valueOf(frequentlyMenuItem.attributes.quantity)
            var totalPrice: Double = frequentlyMenuItem.getPrice(orderStoreId)
            for (modifierItem in frequentlyMenuItem.sizes.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    eventAttributes["Modifier Name"] =
                        modifierItem.attributes.translatedName.getValue(
                            LocaleUtil.getInstance().getLanguage()
                        )
                    eventAttributes["Modifier Quantity"] =
                        modifierItem.attributes.quantity.toString()
                    totalPrice += modifierItem.attributes.price
                }
            }
            for (modifierItem in frequentlyMenuItem.drinks.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    eventAttributes["Modifier Name"] =
                        modifierItem.attributes.translatedName.getValue(
                            LocaleUtil.getInstance().getLanguage()
                        )
                    eventAttributes["Modifier Quantity"] =
                        modifierItem.attributes.quantity.toString()
                    totalPrice += modifierItem.attributes.price
                }
            }
            for (modifierItem in frequentlyMenuItem.fries.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    eventAttributes["Modifier Name"] =
                        modifierItem.attributes.translatedName.getValue(
                            LocaleUtil.getInstance().getLanguage()
                        )
                    eventAttributes["Modifier Quantity"] =
                        modifierItem.attributes.quantity.toString()
                    totalPrice += modifierItem.attributes.price
                }
            }
            for (modifierGroup in frequentlyMenuItem.getModifierGroups()) {
                val visibilityConfig = modifierGroup.attributes.visibilityConfig
                if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                        "hidden", ignoreCase = true
                    )
                ) {
                    var selectedFreeModifier = 0
                    for (modifierItem in modifierGroup.modifierItems) {
                        if (modifierItem.attributes.isSelected) {
                            eventAttributes["Modifier Name"] =
                                modifierItem.attributes.translatedName.getValue(
                                    LocaleUtil.getInstance().getLanguage()
                                )
                            eventAttributes["Modifier Quantity"] =
                                modifierItem.attributes.quantity.toString()
                            if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                selectedFreeModifier += modifierItem.attributes.quantity
                            } else {
                                totalPrice += modifierItem.attributes.price * modifierItem.attributes.quantity
                            }
                        }
                    }
                }
            }
            for (modifierItem in frequentlyMenuItem.getModifierItems()) {
                eventAttributes["Modifier Name"] = modifierItem.attributes.translatedName.getValue(
                    LocaleUtil.getInstance().getLanguage()
                )
                eventAttributes["Modifier Quantity"] = modifierItem.attributes.quantity.toString()
                totalPrice += modifierItem.attributes.price
            }
            for (ingredient in frequentlyMenuItem.getIngredients()) {
                if (!ingredient.attributes.isSelected) {
                    eventAttributes["Ingredient Name"] = ingredient.attributes.name
                    eventAttributes["Ingredient Quantity"] =
                        ingredient.attributes.quantity.toString()
                    totalPrice += ingredient.attributes.price
                }
            }
            totalPrice += frequentlyMenuItem.attributes.quantity
            _logEvent.value = EventAttributesMetric(
                "AddToCart", eventAttributes, "Price", totalPrice
            )
            _logFirebaseEvent.value = "add_to_cart_event"
            val cartCount = cacheUtilHelper.getCartQuantity()
            frequentlyMenuItem.isSandwichOnly = false
            frequentlyMenuItem.isAddedToCart = true
            CacheUtil.getInstance().setFrequentlyQuantityShouldShow(
                frequentlyMenuItem.isAddedToCart, frequentlyMenuItem.id
            )
            removeMukafaaRedemption()
            CacheUtil.getInstance().addToCart(frequentlyMenuItem)
            if (cartCount == 0) {
                _showCartExpiry.value = true
            }
            _addFrequentlyBoughtItem.value = Pair(frequentlyMenuItem, menuAddedToCartPosition)
            _showLoading.value = false
        }
    }

    private fun removeMukafaaRedemption() {
        val transactionId = CacheUtil.getInstance().mukafaaTransactionId
        if (transactionId != null && transactionId.isNotEmpty()) {
            CacheUtil.getInstance().clearMukafaa()
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER,
                transactionId,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {}
                    override fun onError(o: Any) {}
                })
        }
    }

    /**
     * Inherit the function from FrequentlyBoughtItemViewholder
     * This function tells the viewModel what frequently bought item is selected
     */
    override fun addFrequentlyBoughtItemToCart(menuItemId: String, position: Int) {
        invoke {
            _showLoading.value = true
            val applicationId = CacheUtil.getInstance().getApplication().id
            var menuCategory: MenuCategory? = null
            var menuItemDb: MenuItemDb? = null
            menuRepository.getMenuItemDbById(menuItemId, applicationId) { result ->
                result?.let { itemDb ->
                    menuItemDb = itemDb
                    frequentlyMenuItem =
                        GsonHelper.gsonFinal().fromJson(itemDb.data, MenuItem::class.java)
                }
            }

            menuRepository.getMenuCategory(menuItemDb?.menuCategoryId ?: "", applicationId) {
                menuCategory = it
            }

            if (menuCategory == null) {
                for (menuCategoryId in CacheUtil.getInstance().getMenuCategories().keys) {
                    if (CacheUtil.getInstance()
                            .getMenuCategories()[menuCategoryId] != null && CacheUtil.getInstance()
                            .getMenuCategories()[menuCategoryId]!!.second.contains(menuItemId)
                    ) {
                        menuCategory =
                            CacheUtil.getInstance().getMenuCategories()[menuCategoryId]!!.first
                    }
                }
            }

            if (menuCategory != null && frequentlyMenuItem != null) {
                if (frequentlyMenuItem.isMustBeCustomized || frequentlyMenuItem.isCombo) {
                    if (menuCategory == null) {
                        for (category in menuCategories) {
                            for (item in category.getMenuItems()) {
                                if (item.id == frequentlyMenuItem.id) {
                                    menuCategory = category
                                    break
                                }
                            }
                            if (menuCategory != null) {
                                break
                            }
                        }
                    }
                    _showLoading.value = false
                    _navigateToMenuItemDetails.value =
                        Event(Triple(menuCategory!!, frequentlyMenuItem, position))
                } else {
                    getMenuItemFromFrequent(menuItemId, true, position, menuCategory!!)
                }
            } else {
                _showLoading.value = false
            }
        }
    }

    override fun toggleFavoriteFrequentlyBoughtItem(
        favorite: Boolean, menuItem: MenuItem, position: Int
    ) {
        invoke {
            if (!favorite) {
                menuRepository.addItemToFavorite(menuItem) { isSuccess, error ->
                    if (isSuccess) {
                        trackEvent(MixpanelUtil.EVENT_FAVORITE_ITEM)
                        trackEvent(EventType.ADD_TO_WISH_LIST)
                        val favorites = CacheUtil.getInstance().getFavorites()
                        favorites.add(menuItem.id)
                        CacheUtil.getInstance().setFavorites(favorites)

                        val eventAttributes = java.util.HashMap<String, String>()
                        eventAttributes["Item ID"] = menuItem.id
                        eventAttributes["Item Name"] = menuItem.attributes.name

                        _logEvent.value = EventAttributesMetric(
                            eventName = "AddToFavorites",
                            eventAttributes = eventAttributes,
                            eventMetricValue = 0.0
                        )
                        _logAddToWishlistEvent.value = menuItem
                        menuItem.isFavorite = true
                        _showToastMessage.value =
                            CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
                        _updateFavoriteByPosition.value = Pair(position, menuItem)
                    } else {
                        error?.let {
                            AppLogger.printRetrofitError(error)
                            _showToastMessage.value = getErrorMessage(
                                error,
                                CacheUtil.getInstance()
                                    .getTranslations(Text.ERROR_ADD_TO_FAVORITES),
                                null
                            )
                        }
                    }
                }
            } else {
                menuRepository.deleteItemFromFavorites(menuItem.id) { isSuccess, error ->
                    if (isSuccess) {
                        val favorites = CacheUtil.getInstance().getFavorites()
                        favorites.remove(menuItem.id)
                        CacheUtil.getInstance().setFavorites(favorites)
                        menuItem.isFavorite = false
                        _showToastMessage.value = CacheUtil.getInstance()
                            .getTranslations(Text.REMOVED_FROM_FAVORITES)
                        _updateFavoriteByPosition.value = Pair(position, menuItem)
                    } else {
                        error?.let {
                            AppLogger.printRetrofitError(error)
                            _showToastMessage.value = getErrorMessage(
                                error,
                                CacheUtil.getInstance()
                                    .getTranslations(Text.ERROR_ADD_TO_FAVORITES),
                                null
                            )
                        }
                    }

                }
            }
        }

    }

    override fun navigateToMenuItemDetails(menuItemId: String, position: Int) {
        invoke {
            val appId = CacheUtil.getInstance().application.id
            var menuItem: MenuItem? = null
            var menuCategoryId: String = ""
            menuRepository.getMenuItem(menuItemId) { item ->
                menuItem = item
            }
            menuRepository.getMenuCategoryIdFromMenuItemEntityBy(menuItemId, appId) {
                menuCategoryId = it
            }
            menuRepository.getMenuCategory(menuCategoryId, appId) { menuCategory ->
                menuItem?.let {
                    _navigateToMenuItemDetails.value = Event(Triple(menuCategory, it, position))
                }
            }
        }
    }

    private fun getMenuItemFromFrequent(
        menuItemId: String, addToBag: Boolean, position: Int, menuCategory: MenuCategory
    ) {
        invoke {
            menuRepository.getMenuItem(menuItemId) {
                frequentlyMenuItem = it
                prepareFrequentlyMenuItem(frequentlyMenuItem, addToBag, position, menuCategory)
            }
        }
    }

    private fun prepareFrequentlyMenuItem(
        frequentlyMenuItem: MenuItem, addToBag: Boolean, position: Int, menuCategory: MenuCategory
    ) {
        var invalid = false
        val visibleModifierGroups = ArrayList<ModifierGroup>()
        for (group in frequentlyMenuItem.getModifierGroups()) {
            val visibilityConfig = group.attributes.visibilityConfig
            if (visibilityConfig != null && visibilityConfig.initialValue != null) {
                if (!visibilityConfig.initialValue.equals("hidden", ignoreCase = true)) {
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
                _showLoading.value = false
                _showToastMessage.value = CacheUtil.getInstance().getTranslations(
                    Text.MODIFIER_MINIMUM_LIMIT, "Minimum number of extras (x) not reached for (y)"
                ).replace("(x)", modifierGroup.attributes.minimum.toString())
                    .replace("(y)", modifierGroup.attributes.name)

                invalid = true
                break
            }
        }
        if (!invalid) {
            invalid = checkComboModifiers(frequentlyMenuItem)
        }
        if (!invalid) {
            addFrequentBoughtItem(addToBag, position)
        } else {
            _showLoading.value = false
            _navigateToMenuItemDetails.value =
                Event(Triple(menuCategory, frequentlyMenuItem, position))
        }
    }

    fun updateFrequentlyBoughtItems(position: Int, isAddedToCart: Boolean) {
        if (frequentBoughtItemList.isNotEmpty()) {
            if (isAddedToCart) {
                frequentBoughtItemList[position].isAddedToCart = true
            }
            val favorites = CacheUtil.getInstance().favorites
            frequentBoughtItemList.map { item ->
                item.isFavorite = favorites.contains(item.id)
            }
            _showFrequentBoughtItems.value = Event(frequentBoughtItemList)
        }
    }

    fun getModifierAdapterItem(modifierGroups: ArrayList<ModifierGroup>) {
        val adapterItems = mutableListOf<ModifierAdapterItem>()
        modifierGroups.forEachIndexed { index, modifierGroup ->
            adapterItems.add(
                ModifierGroupAdapterItem(
                    modifierGroup, hasSelectedModifierItemsBy(modifierGroup)
                )
            )
            modifierGroup.modifierItems.forEach { modifierItem ->
                adapterItems.add(
                    ModifierItemAdapterItem(modifierGroup, modifierItem)
                )
            }
            if (index + 1 < modifierGroups.size) {
                adapterItems.add(
                    ModifierLineAdapterItem(
                        modifierGroup
                    )
                )
            }

        }
        _modifierAdapterItems.value = Event(adapterItems)
    }

    private fun hasSelectedModifierItemsBy(modifierGroup: ModifierGroup): Boolean =
        modifierGroup.modifierItems.any { it.attributes.isSelected }

    private fun updateInitialModifierVisibility(
        modifierGroups: List<ModifierGroup>
    ) {
        for (group in modifierGroups) {
            val visibilityConfig = group.attributes.visibilityConfig
            if (visibilityConfig != null) {
                val rules = visibilityConfig.rules
                if (rules != null && rules.size > 0) {
                    val rule = rules[0]
                    if (rule.targetType != null) {
                        if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
                            for (modGroup in modifierGroups) {
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
                                    if (group.attributes.visibilityConfig.initialValue != null) {
                                        modifierGroupVisibilityHash[group.id] =
                                            group.attributes.visibilityConfig.initialValue
                                    }
                                    break
                                }
                            }

                        } else if (rule.targetType.equals("modifier", ignoreCase = true)) {
                            val selectedModifiers: MutableSet<String> = java.util.HashSet()
                            for (modGroup in modifierGroups) {
                                if (group.id != modGroup.id) {
                                    for (modItem in modGroup.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                        }
                                    }
                                }
                            }

                            if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                if (rule.targetIs != null && rule.targetIs.equals(
                                        "selected", ignoreCase = true
                                    )
                                ) {
                                    group.attributes.visibilityConfig.initialValue = ""
                                } else {
                                    group.attributes.visibilityConfig.initialValue = "hidden"
                                }
                            } else if (rule.targetId.isNotEmpty()) {
                                group.attributes.visibilityConfig.initialValue = "hidden"
                            }
                            if (group.attributes.visibilityConfig.initialValue != null) {
                                modifierGroupVisibilityHash[group.id] =
                                    group.attributes.visibilityConfig.initialValue
                            }
                        }
                    }
                }

            }

        }
    }

    fun updateModifierVisibilityBy(
        modGroup: ModifierGroup, result: (HashMap<String, String>) -> Unit
    ) {
        currentSelectedModifierItem?.let { componentModifierItem ->
            for (group in componentModifierItem.modifierGroups) {
                val visibilityConfig = group.attributes.visibilityConfig
                if (visibilityConfig != null) {
                    val rules = visibilityConfig.rules
                    if (rules != null && rules.size > 0) {
                        val rule = rules[0]
                        if (rule.targetType != null) {
                            if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
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
                                    if (group.attributes.visibilityConfig.initialValue != null) {
                                        modifierGroupVisibilityHash[group.id] =
                                            group.attributes.visibilityConfig.initialValue
                                    }
                                }
                            } else if (rule.targetType.equals("modifier", ignoreCase = true)) {
                                val selectedModifiers: MutableSet<String> = java.util.HashSet()
                                if (group.id != modGroup.id) {
                                    for (modItem in modGroup.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                        }
                                    }
                                }
                                if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                    if (rule.targetIs != null && rule.targetIs.equals(
                                            "selected", ignoreCase = true
                                        )
                                    ) {
                                        group.attributes.visibilityConfig.initialValue = ""
                                    } else {
                                        group.attributes.visibilityConfig.initialValue = "hidden"
                                    }
                                } else if (rule.targetId.isNotEmpty()) {
                                    group.attributes.visibilityConfig.initialValue = "hidden"
                                }
                                if (group.attributes.visibilityConfig.initialValue != null) {
                                    modifierGroupVisibilityHash[group.id] =
                                        group.attributes.visibilityConfig.initialValue
                                }
                            }
                        }
                    }

                }

            }
            result(modifierGroupVisibilityHash)
        }
    }

    fun isAboveMaxComponent(menuItemComponents: MenuItemComponents, count: Int): Boolean {
        return menuItemComponents.maximum - 1 < count
    }

    fun componentModifierCount(componentModifierItem: List<ComponentModifierItem>): Int {
        var counter = 0
        for (component in componentModifierItem) {
            if (component.selected) counter += component.quantity
        }
        return counter
    }
}