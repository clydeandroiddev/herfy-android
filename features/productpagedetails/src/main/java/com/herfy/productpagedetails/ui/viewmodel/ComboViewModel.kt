package com.herfy.productpagedetails.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.database.AppDatabase
import com.herfy.model.ErrorMsgItem
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ComboViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val cacheUtilHelper: CacheUtilHelper,
    private val menuRepository : MenuRepository
) : BaseViewModel() {


    //Objects
    private var menuItem: MenuItem? = null
        get() = field
        set(value) {field = value}
    private var menuCategory : MenuCategory? = null
        get() = field
        set(value) {field = value}
    private var isFromCartEdit : Boolean = false
        get() = field
        set(value) {field = value}

    private var excludedMenu: List<MenuCategory> = arrayListOf()
    private val excludedCategories = HashMap<String, HashSet<MenuCategory>>()
    private val menuCategories: List<MenuCategory> = arrayListOf()

    //Observalbles
    val notes = ObservableField("")

    //LiveData
    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError
    private val _showMenuItemDetails = MutableLiveData<MenuItem>()
    val showMenuItemDetails: LiveData<MenuItem> = _showMenuItemDetails


    fun addQuantity() {

    }

    fun minusQuantity() {

    }

    fun addToCart() {
        notes.get()?.let {

        }
    }

    fun getMenuItemFromDbBy(addToBag : Boolean, id: String) {
        invokeAtBackground {
            menuRepository.getMenuItem(id){
                menuItem = it
                val applicationId = cacheUtilHelper.getApplication()?.id ?: ""
                if(menuCategory == null){
                    invokeAtBackground {
                        menuRepository.getMenuItemEntityBy(id, applicationId){
                            val menuCategory = MenuCategory()
                            menuCategory.id = it
                            this.menuCategory = menuCategory
                        }
                    }
                }
                _showMenuItemDetails.value = it


            }
        }
    }

    fun getMenuItemFromCartBy(position: Int) {
        invokeAtBackground {
            cacheUtilHelper.getCartItems { cartItems ->
                cartItems[position].let {
                    menuItem = it
                    _showMenuItemDetails.value = it
                    isFromCartEdit = true
                    showHideModifierItemPrice(it)
                }
            }
        }
    }

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
                    //menuView.showHideModifierItemPrice(true, modifierGroupPosition)
                } else {
                    var selectedFreeModifier = 0
                    for (modifierItem in modifierGroup.modifierItems) {
                        if (modifierItem.attributes.isSelected) {
                            if (modifierGroup.attributes.freeModifiersCount > 0 && selectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                val totalSelectedFreeModifier =
                                    selectedFreeModifier + modifierItem.attributes.quantity
                                if (totalSelectedFreeModifier < modifierGroup.attributes.freeModifiersCount) {
                                    if (modifierGroupPosition >= 0) {
//                                        menuView.showHideModifierItemPrice(
//                                            false,
//                                            modifierGroupPosition
//                                        )
                                    }
                                    selectedFreeModifier += modifierItem.attributes.quantity
                                } else {
                                    if (modifierGroupPosition >= 0) {
//                                        menuView.showHideModifierItemPrice(
//                                            true,
//                                            modifierGroupPosition
//                                        )
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
                        //menuView.showHideModifierItemPrice(false, modifierGroupPosition)
                    }
                }
            }
        }
    }


    private fun setMenuCategory(menuItem : MenuItem){
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

    fun getFrequentBoughttItems() {

    }


}