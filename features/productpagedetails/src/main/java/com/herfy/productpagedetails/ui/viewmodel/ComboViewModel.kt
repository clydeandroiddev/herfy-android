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
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.productpagedetails.ui.viewholder.BaseModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.FrequentlyBoughtItemViewholder
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.herfy.repository.MenuRepository
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.runOnMain
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
import java.util.LinkedList
import javax.inject.Inject


@HiltViewModel
class ComboViewModel @Inject constructor(
    private val cacheUtilHelper: CacheUtilHelper,
    private val menuRepository: MenuRepository
) : BaseViewModel(), SizesItemViewholder.OnClickListener,
    FrequentlyBoughtItemViewholder.OnClickListener,
    BaseModifierItemViewHolder.OnAction, ComboMealModifierItemViewHolder.onClickListener {


    init {
        getCartMenuItemsTotalPrice()
    }


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

    private var frequentlyMenuItem = MenuItem()
    var currentSelectedModifierItem: ComponentModifierItem? = null
    var cartMenuItemsTotalPrice: Double = 0.0
    private var _calorieCount = 0

    private var excludedMenu: List<MenuCategory> = arrayListOf()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()
    private val menuCategories: List<MenuCategory> = arrayListOf()
    private var menuItemComponentList = HashMap<Int, MenuItemComponents>()
    private var menuItemComboList = HashMap<Int, HashMap<Int, MenuItemComponents>>()
    private val selectedComponents = HashMap<String, Any>()
    private val frequentBoughtItemList = arrayListOf<MenuItem>()


    //Observalbles
    val notes = ObservableField("")
    var calories: String = ""

    //LiveData
    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError
    private val _showMenuItemDetails = MutableLiveData<MenuItem>()
    val showMenuItemDetails: LiveData<MenuItem> = _showMenuItemDetails
    private val _showHideModifierItemPrice = MutableLiveData<Pair<Boolean, Int>>()
    val showHideModifierItemPrice: LiveData<Pair<Boolean, Int>> = _showHideModifierItemPrice
    private val _showMenuItemTotal = MutableLiveData<Double>()
    val showMenuItemTotal: LiveData<Double> = _showMenuItemTotal
    private val _showMenuItemQuantity = MutableLiveData<Int>()
    val showMenuItemQuantity: LiveData<Int> = _showMenuItemQuantity
    private val _showComboMenuItemComponents = MutableLiveData<List<MenuItemComponents>>()
    val showMenuItemComponent: LiveData<List<MenuItemComponents>> = _showComboMenuItemComponents
    private val _showComboSizes = MutableLiveData<List<ModifierItem>>()
    val showComboSizes: LiveData<List<ModifierItem>> = _showComboSizes
    private val _updateComponentModifierList = MutableLiveData<Pair<Int, Int>>()
    val updateComponentModifierList: LiveData<Pair<Int, Int>> = _updateComponentModifierList
    private val _showComboMealItemDetails = MutableLiveData<ShowComboMealItemDetails>()
    val showComboMealItemDetails: LiveData<ShowComboMealItemDetails> = _showComboMealItemDetails
    private val _showMenuItemCalories = MutableLiveData<String>()
    val showMenuItemCalories: LiveData<String> = _showMenuItemCalories
    private val _updateComboSizeList = MutableLiveData<Pair<Int, Int>>()
    val updateComboSizeList: LiveData<Pair<Int, Int>> = _updateComboSizeList
    private val _updateComboItemDetailModifier = MutableLiveData<Pair<Boolean, Int>>()
    val updateComboItemDetailModifier: LiveData<Pair<Boolean, Int>> = _updateComboItemDetailModifier
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
    private val _showFrequentBoughtItems = MutableLiveData<ArrayList<MenuItem>>()
    val showFrequentBoughtItems: LiveData<ArrayList<MenuItem>> = _showFrequentBoughtItems
    private val _addFrequentlyBoughtItem = MutableLiveData<Pair<MenuItem, Int>>()
    val addFrequentlyBoughtItem: LiveData<Pair<MenuItem, Int>> = _addFrequentlyBoughtItem
    private val _navigateToMenuItemDetails = MutableLiveData<Event<Pair<MenuCategory, MenuItem>>>()
    val navigateToMenuItemDetails : LiveData<Event<Pair<MenuCategory, MenuItem>>> = _navigateToMenuItemDetails
    private val _updateFavoriteByPosition = MutableLiveData<Pair<Int, MenuItem>>()
    val updateFavoriteByPosition : LiveData<Pair<Int, MenuItem>> = _updateFavoriteByPosition
    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading : LiveData<Boolean> = _showLoading

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
                    setMenuCategory(it)
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
                    frequentBoughtItemList.map { item -> item.isFavorite = favorites.contains(item.id) }
                    invoke {
                        cacheUtilHelper.getCartItems {cartItems ->
                            frequentBoughtItemList.map { item-> item.isAddedToCart = cartItems.find { cartItem -> item.id == cartItem.id } != null }
                            _showFrequentBoughtItems.value = frequentBoughtItemList
                        }
                    }
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
                    "hidden",
                    ignoreCase = true
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
                                selectedFreeModifier = modifierGroup.attributes.freeModifiersCount
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
                    "hidden",
                    ignoreCase = true
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
                it.combo[comboIndex]?.components?.let {
                    _showComboMenuItemComponents.value = it
                }
                if (isFromCartEdit) {
                    it.combo.forEachIndexed { comboIndex, menuItemCombo ->
                        val menuItemComponentList = hashMapOf<Int, MenuItemComponents>()
                        menuItemCombo.components?.forEachIndexed { menuItemComponentIndex, menuItemComponents ->
                            menuItemComponentList[menuItemComponentIndex] = menuItemComponents
                        }
                        menuItemComboList[comboIndex] = menuItemComponentList
                    }
                }
            } else {
                _showComboMenuItemComponents.value = ArrayList()
            }
        } ?: run {
            _showComboMenuItemComponents.value = ArrayList()
        }
    }

    /**Function for fetching the componentModifierItem per menuItemComponentIndex**/
    fun getComponentModifierItemPer(
        menuItemComponentIndex: Int,
        showMenuItemComponent: (MenuItemComponents, Boolean) -> Unit
    ) {
        /** Created a map of menuItem combo list where the menuItemComponent
         * data will be stored and can be retrieved
         * by using the comboIndex and menu item component index**/
        val isContainMenuItemComponentKey =
            menuItemComboList[comboIndex]?.containsKey(menuItemComponentIndex) ?: false
        if (isContainMenuItemComponentKey) {
            menuItemComboList[comboIndex]?.get(menuItemComponentIndex)?.let {
                val showCheckBoxIndicator = it.items.any { it.selected }
                showMenuItemComponent(it, showCheckBoxIndicator)
            }
        } else {
            menuItem?.combo?.get(comboIndex)?.components?.get(menuItemComponentIndex)?.let {
                setupComponentPerPosition(menuItemComponentIndex, it, showMenuItemComponent)
            }
        }
    }

    /**Function for fetching the componentModifierItem data (Modifier group, modifier item and menu item from the database),
     * and stored it in a map of menuItemComponents and then stored to menuItemCombo map, for easy retrieval and modification of data**/
    private fun setupComponentPerPosition(
        componentPosition: Int,
        menuItemComponents: MenuItemComponents,
        showMenuItemComponent: (MenuItemComponents, Boolean) -> Unit
    ) {
        invoke {
            if (menuItemComponents.items.isNotEmpty()) {
                try {
                    menuItemComponents.items.sortWith { menuItem1: ComponentModifierItem, menuItem2: ComponentModifierItem -> menuItem1.displayOrder - menuItem2.displayOrder }
                    for (compoItem in menuItemComponents.items) {
                        val compoItemIndex = menuItemComponents.items.indexOf(compoItem)
                        val modifierGroups = LinkedList<ModifierGroup>()
                        val modifierGroupIds = java.util.HashMap<String, ModifierGroup>()
                        for (modifierGroup in compoItem.modifierGroupsMap) {
                            modifierGroupIds[modifierGroup.id] = modifierGroup
                        }
                        val applicationId = CacheUtil.getInstance().application.id

                        menuRepository.getMenuItemDbBy(
                            applicationId,
                            compoItem.name.getValue(LocaleUtil.getInstance().language)
                        ) { menuItemDb ->
                            menuItemDb?.let {
                                val menuItemData =
                                    GsonHelper.gsonFinal().fromJson(it.data, MenuItem::class.java)
                                menuItemData?.let {
                                    compoItem.menuItem = it
                                }
                            }
                        }
                        menuRepository.getAllModifierGroupBy(
                            applicationId,
                            modifierGroupIds.keys.toTypedArray()
                        ) { modifierGroupDbs ->
                            for (modifierGroupDb in modifierGroupDbs) {
                                if (modifierGroupDb.type != ModifierGroupType.SIZES.value) {
                                    val modifierGroupCdn =
                                        MenuUtil.processModifierGroupCustomObject(
                                            GsonHelper.gsonFinal().fromJson(
                                                modifierGroupDb.data,
                                                ModifierGroupCdn::class.java
                                            )
                                        )
                                    val mapModifierGroup = modifierGroupIds[modifierGroupCdn.id]
                                    val excludeModifiers: Set<String> = HashSet<String>()
                                    if (mapModifierGroup != null && mapModifierGroup.excludeModifiers != null && !mapModifierGroup.excludeModifiers.isEmpty()) {
                                        for (excludeModifierId in mapModifierGroup.excludeModifiers) {
                                            excludeModifiers.plus(excludeModifierId.toString())
                                            //excludeModifiers.add()
                                        }
                                    }
                                    val modifierItems = LinkedList<ModifierItem>()
                                    val modifiers = LinkedList<ModifierItem>()
                                    menuRepository.getModifierItemBy(
                                        applicationId,
                                        modifierGroupCdn.attributes.modifiers.toTypedArray()
                                    ) { modifierItemDbs ->
                                        for (modifierItemDb in modifierItemDbs) {
                                            if (!excludeModifiers.contains(modifierItemDb.id)) {
                                                val modifierItemCdn =
                                                    GsonHelper.gsonFinal().fromJson(
                                                        modifierItemDb.data,
                                                        ModifierItemCdn::class.java
                                                    )
                                                val modifierItem = ModifierItemCdn.convert(
                                                    modifierItemCdn,
                                                    LocaleUtil.getInstance().language
                                                )

                                                modifierItem.attributes.isSelected =
                                                    modifierItem.attributes.minimum == 1

                                                modifierItems.add(modifierItem)

                                            }
                                        }

                                        modifierItems.sortWith { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                                        val modifierGroup = ModifierGroupCdn.convert(
                                            modifierGroupCdn,
                                            LocaleUtil.getInstance().language
                                        )
                                        if (mapModifierGroup != null) {
                                            modifierGroup.attributes.minimum =
                                                mapModifierGroup.min
                                            modifierGroup.attributes.maximum =
                                                mapModifierGroup.max
                                            modifierGroup.attributes.displayOrder =
                                                mapModifierGroup.displayOrder
                                            modifierGroup.attributes.freeModifiersCount =
                                                mapModifierGroup.freeModifiersCount
                                            modifierGroup.setDefaultModifiers(
                                                java.util.ArrayList(
                                                    mapModifierGroup.defaultModifiers
                                                )
                                            )
                                            modifierGroup.attributes.visibilityConfig =
                                                mapModifierGroup.visibilityConfig
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
                                        if (modifierItemsFinal.size > 0) {
                                            val defaultModifiers =
                                                modifierGroup.defaultModifiers
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
                            menuItemComponents.items[compoItemIndex].modifierGroups =
                                ArrayList(modifierGroups)
                            menuItemComponents.items = componentsSelectFirst(menuItemComponents)


                        }
                    }
                    menuItemComponentList[componentPosition] = menuItemComponents
                    menuItemComboList[comboIndex] = menuItemComponentList
                    val showCheckBoxIndicator = menuItemComponents.items.any { it.selected }
                    showMenuItemComponent(menuItemComponents, showCheckBoxIndicator)
                } catch (e: Exception) {
                    runOnMain {
                        _showError.value =
                            ErrorMsgItem(true, "There is a problem parsing the combo meal")
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    /** Function for checking to show the modifier item price per menu item,
     *  and applying it to ComboMealItemModifierItemAdapter
     * **/
    fun showHideModifierItemPrice(menuItem: MenuItem) {
        for (modifierGroup in menuItem.modifierGroups) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
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
        if (!isFromCartEdit || checkIfNoItemSelected(components, components.items)) {
            components.items[0].selected = true
            selectedComponents[components.id] = 0
        }
        return components.items
    }

    private fun checkIfNoItemSelected(
        component: MenuItemComponents,
        items: ArrayList<ComponentModifierItem>
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
        comboIndex = pos
        _updateComboSizeList.value = Pair(pos, previousComboIndex)
        menuItem?.let {
            getMenuItemTotalPrice(it, -1)
        }
        getComboMenuItemComponents()

        //getComboMenuItemComponents()
        //TODO : Unselect Or Set To Default OldComboIndex
    }

    /**Inheriting the function from ComboMealModifierItem.onClickListener
     * This function tells the viewModel to update the current item attributes
     * to selected and previous item attributes to unselected,
     * If the selected ComponentModifierItem has list of ModifierGroup and Modifier items
     * the list will be shown into a separate view.
     * **/
    override fun onSelectComboComponentItem(
        item: ComponentModifierItem,
        menuItemComponentPos: Int,
        componentModifierItemPos: Int,
        previousSelectedIndex: Int
    ) {
        val showModifierDetail = checkToShowModifierDetail(item)

        if (showModifierDetail) {
            _showComboMealItemDetails.value = ShowComboMealItemDetails(
                item,
                componentModifierItemPos,
                previousSelectedIndex,
                menuItemComponentPos
            )
        } else {
            if (componentModifierItemPos != previousSelectedIndex) {
                updateComponentModifierPreviousItem(menuItemComponentPos, previousSelectedIndex)
            }
            updateSelectedComponentModifierItem(menuItemComponentPos, componentModifierItemPos)
            _updateComponentModifierList.value =
                Pair(menuItemComponentPos, componentModifierItemPos)
            menuItem?.let {
                getMenuItemTotalPrice(it, -1)
            }

        }
    }

    private fun updateComponentModifierPreviousItem(
        componentPosition: Int,
        prevSelectedItemPos: Int
    ) {
        menuItemComboList[comboIndex]?.get(componentPosition)?.let {
            val selectedComboComponentItem = it.items[prevSelectedItemPos]
            if (selectedComboComponentItem.modifierGroups.size > 0) {
                selectedComboComponentItem.modifierGroups.forEach {
                    it.modifierItems.forEach {
                        it.attributes.quantity = 0
                        it.attributes.isSelected = false
                    }
                }
            } else {
                selectedComboComponentItem.selected = false
            }
            it.items[prevSelectedItemPos] = selectedComboComponentItem
        }
    }

    private fun updateSelectedComponentModifierItem(componentPosition: Int, position: Int) {
        menuItemComboList[comboIndex]?.get(componentPosition)?.let {
            val selectedComboComponentItem = it.items[position]
            selectedComboComponentItem.selected = true
            it.items[position] = selectedComboComponentItem
        }
    }

    private fun checkToShowModifierDetail(
        components: ComponentModifierItem,
    ): Boolean = components.modifierGroups.size > 1

    override fun onCheckedChanged(
        isSelected: Boolean,
        modifierGroupPosition: Int,
        modifierGroup: ModifierGroup?,
        modifierItemPosition: Int,
        modifierItem: ModifierItem?
    ) {
        var _isSelected = isSelected
        modifierGroup?.let { group ->
            if (group.attributes.maximum == 1) {
                if (group.attributes.minimum == 1) {
                    _isSelected = true
                }
                for (item in modifierGroup.modifierItems) {
                    if (item.id == modifierItem?.id) {
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
                    _showError.value = ErrorMsgItem(
                        true,
                        cacheUtilHelper.getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
                    )
                } else {
                    val item = group.modifierItems[modifierItemPosition]
                    if (item.id == modifierItem!!.id) {
                        item.attributes.isSelected = isSelected
                        if (!isSelected) {
                            item.attributes.quantity = 1
                        }
                    }
                }
            }
            updateModifierGroupSelectionIndicator(isSelected, modifierGroupPosition)
        }
    }

    override fun onQuantityChanged(
        add: Boolean,
        modifierGroupPosition: Int,
        modifierGroup: ModifierGroup,
        modifierItemPosition: Int,
        modifierItem: ModifierItem,
        isQuantityVisible: Boolean,
        clickCount: Int?
    ) {
        if (add) {

            var proceed = true
            var quantity = modifierItem.attributes.quantity
            if(modifierItem.attributes.quantity < modifierItem.attributes.maximum){
                quantity += 1
                if(isAboveMax(modifierGroup, modifierItemQuantityCount(modifierGroup.modifierItems))){
                    proceed = false
                    _showToastMessage.value =  modifierGroup.attributes.name + " " + CacheUtil.getInstance()
                        .getTranslations(
                            Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                        )
                }else {
                    if (modifierItem.attributes.maximum < modifierItem.attributes.quantity + 1) {
                        proceed = false
                        _showToastMessage.value = modifierGroup.attributes.name + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.MODIFIER_LIMIT_REACHED, "Options limit reached"
                            )
                    }
                }
            }else {
                proceed = false
                _showToastMessage.value =  CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
            }

            if(proceed){
                val group = modifierGroup
                val item = group.modifierItems[modifierItemPosition]
                var modifierTotal = 0
                var currentModifierTotal = 0
                if (item.id == modifierItem.id) {
                    modifierItem.attributes.isShouldShowQuantity = true
                    for (eachModifierItem in group.modifierItems) {
                        if (eachModifierItem.attributes.isShouldShowQuantity) {
                            currentModifierTotal += eachModifierItem.attributes.quantity
                        }
                    }
                    if (currentModifierTotal < modifierGroup.attributes.maximum) {
                        if (!modifierItem.attributes.isSelected) {
                            modifierItem.attributes.isSelected = true
                        }
                        modifierItem.attributes.quantity += 1
                    }
                }


                for (eachModifierItem in group.modifierItems) {
                    if (eachModifierItem.attributes.isShouldShowQuantity) {
                        modifierTotal += eachModifierItem.attributes.quantity
                    }
                }

                if (modifierTotal >= modifierGroup.attributes.maximum) {
                    for (eachItems in group.modifierItems) {
                        eachItems.attributes.setModifierItemInteraction(true)
                        eachItems.attributes.setModifierItemQuantityExceedsMaximum(eachItems.attributes.isShouldShowQuantity || eachItems.attributes.isSelected)
                    }
                }
                updateModifierGroupSelectionIndicator(
                    modifierItem.attributes.isSelected,
                    modifierGroupPosition
                )
            }
        } else {
            var selectedItemMinusTotal = 0
            val group = modifierGroup
            val item = group.modifierItems[modifierItemPosition]
            if (modifierItem.attributes.quantity == 1) {
                if (item.id == modifierItem.id) {
                    item.attributes.isShouldShowQuantity = false
                    for (eachModifierItem in group.modifierItems) {
                        if (eachModifierItem.attributes.hasUserInteractedWithModifierItem()) {
                            eachModifierItem.attributes.setModifierItemQuantityExceedsMaximum(false)
                            eachModifierItem.attributes.setModifierItemInteraction(false)
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
                modifierItem.attributes.isSelected,
                modifierGroupPosition
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
        hasSelectedModifierItem: Boolean,
        modifierGroupPosition: Int
    ) {
        val selectedModifierItemCount = countSelectedModifierItems(modifierGroupPosition)
        var shouldShowIndicator = false
        currentSelectedModifierItem?.let {
            if (it.attributes.minimum >= 1) {
                shouldShowIndicator = selectedModifierItemCount >= it.attributes.maximum
            } else {
                shouldShowIndicator = hasSelectedModifierItem || selectedModifierItemCount >= 1
            }
        } ?: run {
            shouldShowIndicator = hasSelectedModifierItem || selectedModifierItemCount >= 1
        }
        _updateComboItemDetailModifier.value = Pair(shouldShowIndicator, modifierGroupPosition)
    }

    private fun countSelectedModifierItems(modifierGroupPosition: Int): Int {
        var selectedModifierItemCount = 0
        currentSelectedModifierItem?.let {
            val group = it.modifierGroups[modifierGroupPosition]
            if (group != null) {
                for (item in group.modifierItems) {
                    if (item.attributes.isSelected) {
                        selectedModifierItemCount++
                    }
                }
            }
        }
        return selectedModifierItemCount
    }

    fun showModifierItemPricePer(componentModifierItem: ComponentModifierItem): HashMap<Int, Boolean> {
        val showModifierItemPrice = hashMapOf<Int, Boolean>()
        componentModifierItem.modifierGroups.forEachIndexed { index, group ->
            showModifierItemPrice[index] = group.attributes.freeModifiersCount == 0
        }
        return showModifierItemPrice
    }

    fun showModifierItemCheckBoxPer(componentModifierItem: ComponentModifierItem): HashMap<Int, Boolean> {
        val shouldShowModifierCheckbox = java.util.HashMap<Int, Boolean>()
        componentModifierItem.modifierGroups.forEachIndexed { index, group ->
            shouldShowModifierCheckbox[index] = group.modifierItems.any { it.attributes.isSelected }
        }
        return shouldShowModifierCheckbox
    }

    fun showComboModifierGroupCheckBox(menuItemComponentList: List<MenuItemComponents>): HashMap<Int, Boolean> {
        val map = hashMapOf<Int, Boolean>()
        menuItemComponentList.forEachIndexed { index, menuItemComponents ->
            map[index] = menuItemComponents.items.any { it.selected }
        }
        return map
    }

    fun showComboModifierGroupCheckBoxPerComponent(
        index: Int,
        menuItemComponent: MenuItemComponents
    ): HashMap<Int, Boolean> {
        val map = hashMapOf<Int, Boolean>()
        map[index] = menuItemComponent.items.any { it.selected }
        return map
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

    fun updatePreviousComponentModifierItem(
        menuItemComponentIndex: Int,
        previousComponentModifierIndex: Int
    ) {
        menuItemComboList[comboIndex]?.get(menuItemComponentIndex)?.let {
            val selectedComboComponentItem = it.items[previousComponentModifierIndex]
            if (selectedComboComponentItem.modifierGroups.size > 0) {
                selectedComboComponentItem.modifierGroups.forEach {
                    it.modifierItems.forEach {
                        it.attributes.quantity = 0
                        it.attributes.isSelected = false
                    }
                }
            } else {
                selectedComboComponentItem.selected = false
            }
            it.items[menuItemComponentIndex] = selectedComboComponentItem
        }

    }

    fun updateSelectedModifierItem(
        menuItemComponentIndex: Int,
        componentModifierItemIndex: Int,
        component: ComponentModifierItem,
        updatedModifierGroupList: List<ModifierGroup>?
    ) {
        menuItemComboList[comboIndex]?.get(menuItemComponentIndex)?.let {
            component.apply {
                updatedModifierGroupList?.let { selectedModifierGroup ->
                    selected = ifHasSelected(selectedModifierGroup) > 0
                    attributes.price = price + selectedModifierPrice(selectedModifierGroup)
                    component.modifierGroups.clear()
                    component.modifierGroups.addAll(selectedModifierGroup)
                }
            }
            it.items[componentModifierItemIndex] = component
            menuItem?.let { getMenuItemTotalPrice(it, -1) }
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
                    } else if (CacheUtil.getInstance().couponCode != null &&
                        CacheUtil.getInstance().couponMenuItem != null
                    ) {
                        CacheUtil.getInstance().couponMenuItem = menuItem
                    } else if (CacheUtil.getInstance().campaign != null &&
                        CacheUtil.getInstance().campaignPromotionMenuItem != null
                    ) {
                        CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                    }
                    _updateCartItem.value = true
                }

            }
        }
    }

    private fun checkComboModifiers(menuItem : MenuItem): Boolean {
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
                for (components in menuItem!!.combo[trueIndex].components) {
                    if (invalid) {
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
                                            "(x)",
                                            modifierGroup.attributes.minimum.toString()
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
                    _showToastMessage.value =
                        CacheUtil.getInstance().getTranslations(
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
            _showError.value = ErrorMsgItem(
                true, CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                    .replace("(x)", store.attributes.name)
            )

        } else {
            _logFirebaseEvent.value = "add_to_cart_event"
            val cartCount = cacheUtilHelper.getCartQuantity()
            menuItem!!.isSandwichOnly = false
            menuItem?.let {
                invoke {
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
                    modifierGroup.attributes.visibilityConfig == null ||
                            modifierGroup.attributes.visibilityConfig.initialValue == null ||
                            !modifierGroup.attributes.visibilityConfig.initialValue.equals(
                                "hidden",
                                ignoreCase = true
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
                "AddToCart",
                eventAttributes,
                "Price",
                totalPrice
            )
        }
    }

    fun addRemoveFavorite() {
        menuItem?.let { item ->
            if (!CacheUtil.getInstance().getFavorites().contains(item.id)) {
                CustomersApiCall.getInstance().addItemToFavorite(
                    AuthUtil.getInstance().getCustomer().id,
                    AddFavoriteItemBody.getCdn(LocaleUtil.getInstance().getLanguage(), menuItem),
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
                                .getTranslations(Text.ITEM_ADDED_TO_FAVORITES)

                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o)
                            _showError.value = ErrorMsgItem(
                                true, getErrorMessage(
                                    o,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.ERROR_ADD_TO_FAVORITES),
                                    null
                                )
                            )
                        }
                    })
            } else {
                CustomersApiCall.getInstance().deleteItemFromFavorites(
                    AuthUtil.getInstance().getCustomer().id,
                    item.id,
                    object : ApiRequestListener {
                        override fun onSuccess(o: Any) {
                            val favorites = CacheUtil.getInstance().getFavorites()
                            favorites.remove(item.id)
                            CacheUtil.getInstance().setFavorites(favorites)
                            _setFavoriteItem.value = item
                            _showToastMessage.value =
                                CacheUtil.getInstance()
                                    .getTranslations(Text.ITEM_REMOVED_FROM_FAVORITES)

                        }

                        override fun onError(o: Any) {
                            AppLogger.printRetrofitError(o)
                            _showError.value = ErrorMsgItem(
                                true,
                                getErrorMessage(
                                    o,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES),
                                    null
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
            _showError.value = ErrorMsgItem(
                true, CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE)
                    .replace("(x)", store.attributes.name)
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
                        "hidden",
                        ignoreCase = true
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
                eventAttributes["Modifier Name"] =
                    modifierItem.attributes.translatedName.getValue(
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
                 frequentlyMenuItem.isAddedToCart,
                 frequentlyMenuItem.id
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
            CustomersApiCall.getInstance().deleteRedemption(
                Values.MUKAFAA_PROVIDER,
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
            var menuCategory : MenuCategory? = null
            var menuItemDb : MenuItemDb? = null
            menuRepository.getMenuItemDbById(menuItemId,applicationId) {result ->
                result?.let {itemDb ->
                    menuItemDb = itemDb
                    frequentlyMenuItem = GsonHelper.gsonFinal().fromJson(itemDb.data, MenuItem::class.java)
                }
            }

            menuRepository.getMenuCategory(menuItemDb?.menuCategoryId?:"", applicationId){
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
                    _navigateToMenuItemDetails.value = Event(Pair(menuCategory!!, frequentlyMenuItem))
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
                    _navigateToMenuItemDetails.value = Event(Pair(menuCategory!!, frequentlyMenuItem))
                } else {
                    getMenuItemFromFrequent(menuItemId, true, position, menuCategory!!)
                }
            }else {
                _showLoading.value = false
            }
        }
    }

    override fun toggleFavoriteFrequentlyBoughtItem(
        favorite: Boolean,
        menuItem: MenuItem,
        position: Int
    ) {
        invoke {
            if(!favorite){
                menuRepository.addItemToFavorite(menuItem){ isSuccess, error ->
                    if(isSuccess){
                        trackEvent(MixpanelUtil.EVENT_FAVORITE_ITEM)
                        trackEvent(EventType.ADD_TO_WISH_LIST)
                        val favorites = CacheUtil.getInstance().getFavorites()
                        favorites.add(menuItem.id)
                        CacheUtil.getInstance().setFavorites(favorites)

                        val eventAttributes = java.util.HashMap<String, String>()
                        eventAttributes["Item ID"] = menuItem.id
                        eventAttributes["Item Name"] = menuItem.attributes.name

                        _logEvent.value = EventAttributesMetric(eventName = "AddToFavorites", eventAttributes =  eventAttributes,  eventMetricValue = 0.0)
                        _logAddToWishlistEvent.value = menuItem
                        menuItem.isFavorite = true
                        _showToastMessage.value = CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_FAVORITES)
                        _updateFavoriteByPosition.value = Pair(position, menuItem)
                    } else {
                        error?.let{
                            AppLogger.printRetrofitError(error)
                            _showToastMessage.value = getErrorMessage(error, CacheUtil.getInstance().getTranslations(Text.ERROR_ADD_TO_FAVORITES), null)
                        }
                    }
                }
            }else {
                menuRepository.deleteItemFromFavorites(menuItem.id){ isSuccess, error ->
                    if(isSuccess){
                        val favorites = CacheUtil.getInstance().getFavorites()
                        favorites.remove(menuItem.id)
                        CacheUtil.getInstance().setFavorites(favorites)
                        menuItem.isFavorite = false
                        _showToastMessage.value = CacheUtil.getInstance().getTranslations(Text.ITEM_REMOVED_FROM_FAVORITES)
                        _updateFavoriteByPosition.value = Pair(position, menuItem)
                    }else {
                        error?.let{
                            AppLogger.printRetrofitError(error)
                            _showToastMessage.value = getErrorMessage(error, CacheUtil.getInstance().getTranslations(Text.ERROR_ADD_TO_FAVORITES), null)
                        }
                    }

                }
            }
        }

    }

    override fun navigateToMenuItemDetails(menuItemId: String) {
        invoke {
            val appId = CacheUtil.getInstance().application.id
            var menuItem : MenuItem? = null
            var menuCategoryId : String = ""
            menuRepository.getMenuItem(menuItemId){item ->
                menuItem = item
            }
            menuRepository.getMenuCategoryIdFromMenuItemEntityBy(menuItemId, appId){
                menuCategoryId = it
            }
            menuRepository.getMenuCategory(menuCategoryId, appId){menuCategory ->
                menuItem?.let {
                    _navigateToMenuItemDetails.value = Event(Pair(menuCategory, it))
                }
            }
        }
    }

    private fun getMenuItemFromFrequent(menuItemId : String, addToBag : Boolean, position: Int, menuCategory : MenuCategory){
        invoke {
            menuRepository.getMenuItem(menuItemId){
                frequentlyMenuItem = it
                prepareFrequentlyMenuItem(frequentlyMenuItem, addToBag, position, menuCategory)
            }
        }
    }

    private fun prepareFrequentlyMenuItem(
        frequentlyMenuItem: MenuItem,
        addToBag: Boolean,
        position: Int,
        menuCategory: MenuCategory
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
                _showToastMessage.value =
                    CacheUtil.getInstance().getTranslations(
                        Text.MODIFIER_MINIMUM_LIMIT,
                        "Minimum number of extras (x) not reached for (y)"
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
            _navigateToMenuItemDetails.value = Event(Pair(menuCategory, frequentlyMenuItem))
        }
    }
}