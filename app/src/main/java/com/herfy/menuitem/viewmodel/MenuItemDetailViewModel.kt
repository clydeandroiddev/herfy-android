package com.herfy.menuitem.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.menu.adapters.ModifierGroupNewRecycleAdapterKotlin
import com.herfy.model.ErrorMsgItem
import com.herfy.model.EventAttributesMetric
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.model.combo.ComboComponentAdapterItem
import com.herfy.model.modifiers.ModifierAdapterItem
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierAdapter
import com.herfy.productpagedetails.ui.adapter.ComboMealComponentAdapter
import com.herfy.productpagedetails.ui.viewholder.FrequentlyBoughtItemViewholder
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.Event
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuItemDetailViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val cacheUtilHelper: CacheUtilHelper
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
    private val _showMenuItemSizes = MutableLiveData<List<ModifierItem>>()
    val showMenuItemSizes: LiveData<List<ModifierItem>> = _showMenuItemSizes
    private val _showMenuItemCalories = MutableLiveData<String>()
    val showMenuItemCalories: LiveData<String> = _showMenuItemCalories
    private val _showModifierGroupAdapterItems = MutableLiveData<List<ModifierGroupNewRecycleAdapterKotlin.ModifierAdapterType>>()
    val showModifierGroupAdapterItems: LiveData<List<ModifierGroupNewRecycleAdapterKotlin.ModifierAdapterType>> = _showModifierGroupAdapterItems




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

    private val modifierGroupList = arrayListOf<ModifierGroup>()

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
                _showMenuItemSizes.value = it.sizes.modifierItems
                if(it.isCombo && it.combo.isNotEmpty()){
                    //Fetch combo
                    //getComboMenuItemComponents()
                }else {
                    //Fetch modifier group and items
                    getMenuItemModifiers(it)
                }
                //getMenuItemTotalPrice(it, -1)
            }
        }
    }

    private fun getMenuItemModifiers(menuItem: MenuItem) {
        val adapterItems = arrayListOf<ModifierGroupNewRecycleAdapterKotlin.ModifierAdapterType>()
        menuItem.modifierGroups.forEachIndexed { modifierGroupIndex, eachModifierGroup ->
            adapterItems.add(
                ModifierGroupNewRecycleAdapterKotlin.ModifierGroupAdapterItem(
                    modifierGroup = eachModifierGroup,
                    modifierGroupIndex = modifierGroupIndex,
                    hasSelectedItems = hasSelectedModifierItemsBy(eachModifierGroup),
                    listOfModifierGroups = menuItem.modifierGroups
                )
            )
            eachModifierGroup.modifierItems.forEachIndexed { itemIndex, modifierItem ->
                adapterItems.add(
                    ModifierGroupNewRecycleAdapterKotlin.ModifierItemAdapterItem(
                        modifierItem = modifierItem,
                        modifierGroup = eachModifierGroup,
                        modifierGroupPosition = modifierGroupIndex,
                        listOfModifierGroups = menuItem.modifierGroups
                    )
                )
            }
        }
        _showModifierGroupAdapterItems.value = adapterItems
    }

    private fun hasSelectedModifierItemsBy(modifierGroup: ModifierGroup): Boolean =
        modifierGroup.modifierItems.any { it.attributes.isSelected }

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
                    _showMenuItemSizes.value = it.sizes.modifierItems
                    if(it.isCombo && it.combo.isNotEmpty()){
                        //Fetch combo
                        //getComboMenuItemComponents()
                    }
                  /*  getComboMenuItemComponents()
                    showHideModifierItemPrice(it)*/
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
        _showMenuItemSizes.value = menuItem.sizes.modifierItems
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

        /*getComboMenuItemComponents()
        showHideModifierItemPrice(menuItem)*/
        getMenuItemTotalPrice(menuItem, -1)
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

    fun modifierGroupList() = modifierGroupList

    override fun onSelectSize(size: ModifierItem, pos: Int) {
        TODO("Not yet implemented")
    }

    override fun addFrequentlyBoughtItemToCart(menuItemId: String, position: Int) {
        TODO("Not yet implemented")
    }

    override fun toggleFavoriteFrequentlyBoughtItem(
        favorite: Boolean,
        menuItem: MenuItem,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun navigateToMenuItemDetails(menuItemId: String, position: Int) {
        TODO("Not yet implemented")
    }

    override fun onCheckedChanged(
        isSelected: Boolean,
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem
    ) {
        TODO("Not yet implemented")
    }

    override fun onQuantityChanged(
        add: Boolean,
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem,
        isQuantityVisible: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun onComponentCheckedChanged(
        isSelected: Boolean,
        menuItemComponent: MenuItemComponents,
        componentModifierItem: ComponentModifierItem,
        menuItemComponentPos: Int,
        componentModifierItemPos: Int,
        fromArrowRight: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun onComponentQuantityChanged(
        add: Boolean,
        menuItemComponent: MenuItemComponents,
        componentModifierItem: ComponentModifierItem,
        isQuantityVisible: Boolean,
        menuItemComponentPos: Int,
        componentModifierItemPos: Int
    ) {
        TODO("Not yet implemented")
    }


}