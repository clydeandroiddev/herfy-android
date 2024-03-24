package com.herfy.productpagedetails.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.model.ErrorMsgItem
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.productpagedetails.ui.adapter.FrequentlyBoughtItemAdapter
import com.herfy.productpagedetails.ui.viewholder.BaseModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class ComboViewModel @Inject constructor(
    private val cacheUtilHelper: CacheUtilHelper,
    private val menuRepository: MenuRepository
) : BaseViewModel(), SizesItemViewholder.OnClickListener,
    FrequentlyBoughtItemAdapter.OnClickListener,
    BaseModifierItemViewHolder.OnAction, ComboMealModifierItemViewHolder.onClickListener {


    init {
        getCartMenuItemsTotalPrice()
    }

    //Objects
    private var menuItem: MenuItem? = null
        get() = field
        set(value) {
            field = value
        }
    private var menuCategory: MenuCategory? = null
        get() = field
        set(value) {
            field = value
        }
    internal var isFromCartEdit: Boolean = false
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
    internal var currentSelectedModifierItem : ComponentModifierItem? = null
    internal var cartMenuItemsTotalPrice: Double = 0.0
    private var _calorieCount = 0

    private var excludedMenu: List<MenuCategory> = arrayListOf()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()
    private val menuCategories: List<MenuCategory> = arrayListOf()
    private var menuItemComponentList = HashMap<Int, MenuItemComponents>()
    private var menuItemComboList = HashMap<Int, HashMap<Int, MenuItemComponents>>()
    private val selectedComponents = HashMap<String, Any>()

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

    /**Function for retrieving data of menu item from db**/
    fun getMenuItemFromDbBy(addToBag: Boolean, id: String, isUpsell: Boolean) {
        invokeAtBackground {
            menuRepository.getMenuItem(id) {
                it.attributes.isUpsell = isUpsell
                menuItem = it
                val applicationId = cacheUtilHelper.getApplication()?.id ?: ""
                if (menuCategory == null) {
                    invokeAtBackground {
                        menuRepository.getMenuItemEntityBy(id, applicationId) {
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
    fun getFrequentBoughtItems() {

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
        setMenuItemCalories()
        val itemTotalPrice = totalPrice * item.attributes.quantity
        _showMenuItemTotal.value = itemTotalPrice
    }

    fun getCartMenuItemsTotalPrice() {
        var cartTotal = 0.0
        invokeAtBackground {
            cacheUtilHelper.getCartItems {
                for (menuItem in it) {
                    cartTotal += getCartTotalPricePer(menuItem, -1)
                }
                cartMenuItemsTotalPrice
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
        showMenuItemComponent: (MenuItemComponents) -> Unit
    ) {
        /** Created a map of menuItem combo list where the menuItemComponent
         * data will be stored and can be retrieved
         * by using the comboIndex and menu item component index**/
        val isContainMenuItemComponentKey =
            menuItemComboList[comboIndex]?.containsKey(menuItemComponentIndex) ?: false
        if (isContainMenuItemComponentKey) {
            menuItemComboList[comboIndex]?.get(menuItemComponentIndex)?.let {
                showMenuItemComponent(it)
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
        showMenuItemComponent: (MenuItemComponents) -> Unit
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
                                                            modifierItemsFinal[i].attributes.quantity =
                                                                1
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
                    showMenuItemComponent(menuItemComponents)
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

    internal fun setMenuCategory(menuCategoryId: String?) {
        val menuCategory = MenuCategory()
        menuCategory.id = menuCategoryId
        this.menuCategory = menuCategory
    }

    internal fun setMenuCategory(menuItem: MenuItem) {
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

    /**Function adding the menu item in to cart and caching it
     * **/
    fun addToCart() {
        notes.get()?.let {

        }
    }

    /**Function updating the selected size of the combo meal menu item,
     * update also the combo index, and update the list of combo menuItemComponents and reload it
     * **/
    override fun onSelectSize(size: ModifierItem, pos: Int) {
        previousComboIndex = comboIndex
        comboIndex = pos
        _updateComboSizeList.value = Pair(pos, previousComboIndex)
        //getComboMenuItemComponents()
        //TODO : Unselect Or Set To Default OldComboIndex
    }


    override fun onSelectItem(menuItem: MenuItem) {
        TODO("Not yet implemented")
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
                    if(!modifierItem.attributes.isSelected) {
                        modifierItem.attributes.isSelected = true
                    }
                    modifierItem.attributes.quantity  += 1
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
            updateModifierGroupSelectionIndicator(modifierItem.attributes.isSelected, modifierGroupPosition)
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
                if(modifierItem.attributes.quantity == 0){
                    modifierItem.attributes.isSelected = false
                }
            }
            updateModifierGroupSelectionIndicator(modifierItem.attributes.isSelected, modifierGroupPosition)
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
            if(it.attributes.maximum > 1){
                shouldShowIndicator = selectedModifierItemCount >= it.attributes.maximum
            }else {
                shouldShowIndicator = hasSelectedModifierItem || selectedModifierItemCount >= 1
            }
        }?: run {
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

}