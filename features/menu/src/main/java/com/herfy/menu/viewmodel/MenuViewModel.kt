package com.herfy.menu.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.database.entities.MenuItemEntity
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.database.repositories.HomeRepository
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.extensions.cast
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.Slide
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
) : BaseViewModel(), SliderImageAdapter.SliderListener {

    private val _slides = MutableLiveData<ArrayList<Slide>>()
    val slides: LiveData<ArrayList<Slide>> = _slides

    private val menuCategories = mutableListOf<MenuCategory>()
    private val _showMenuCategories = MutableLiveData<List<MenuCategory>>()
    val showMenuCategories: LiveData<List<MenuCategory>> = _showMenuCategories

    val menuCategoryPositions = HashMap<String, Int>()
    private val _menuAdapterItems = MutableLiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>>()
    val menuAdapterItems: LiveData<List<MenuCategoryItemAdapter.MenuAdapterItem>> = _menuAdapterItems

    fun getSlides() {
        invoke {
            homeRepository.getSliders { homeResponse ->
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

    }

    fun getMenuCategoriesItems() {
        invokeAtBackground {
            menuCategories.clear()
            menuCategoryPositions.clear()
            homeRepository.getMenuCategories { homeResponse ->
                homeResponse.data?.let { data ->
                    data.cast<ArrayList<MenuCategory>> {
                        it.forEach { category ->
                            if(category.enabled == 1) {
                                invokeAtBackground {
                                    homeRepository.getMenuItemBy(category.id) { response ->
                                        response.data?.cast<List<MenuItemEntity>> { menuItemDbs ->
                                            if (menuItemDbs.isNotEmpty()) {
                                                category.menuItems = menuItemDbs.toDomain()
                                                menuCategories.add(category)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getMenuCategories() {
        menuCategories.sortBy { menuCategory -> menuCategory.displayOrder }
        var count = 0
        val adapterItems = mutableListOf<MenuCategoryItemAdapter.MenuAdapterItem>()
        menuCategories.forEach { menuCategory ->
            menuCategoryPositions[menuCategory.id] = count
            adapterItems.add(MenuCategoryItemAdapter.MenuCategoryAdapterItem(menuCategory))
            menuCategory.menuItems.sortBy { menuItem -> menuItem.displayOrder }
            menuCategory.menuItems?.forEach { menuItem ->
                adapterItems.add(MenuCategoryItemAdapter.MenuItemAdapterItem(menuCategory, menuItem))
                count++
            }
            count++
        }
        _showMenuCategories.postValue(menuCategories)
        _menuAdapterItems.postValue(adapterItems)
    }

}