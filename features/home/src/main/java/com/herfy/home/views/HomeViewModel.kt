package com.herfy.home.views

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.home.data.ErrorMsgItem
import com.herfy.home.repository.HomeRepository
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.Event
import com.herfy.utils.base.mvvm.BaseViewModel
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Slide

class HomeViewModel constructor(
    val homeRepository: HomeRepository
) : BaseViewModel(){

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError : LiveData<ErrorMsgItem> = _showError

    private val _slideList = MutableLiveData<ArrayList<Slide>>()
    val slideList : LiveData<ArrayList<Slide>> = _slideList

    private val _promotionsList = MutableLiveData<ArrayList<Campaign>>()
    val promotionsList : LiveData<ArrayList<Campaign>> = _promotionsList

    private val _showLink = MutableLiveData<Event<Uri>>()
    val showLink : LiveData<Event<Uri>> = _showLink

    private val _showMenuItem = MutableLiveData<Event<Pair<String, MenuItem>>>()
    val showMenuItem : LiveData<Event<Pair<String, MenuItem>>> = _showMenuItem

    private val _showPromo = MutableLiveData<Event<Campaign>>()
    val showPromo : LiveData<Event<Campaign>> = _showPromo
    fun getSliders(sliderId : String){
        invoke {
            homeRepository.getSliders(sliderId){homeResponse ->
                if(homeResponse.success){
                    homeResponse.data?.let {
                        it.cast<ArrayList<Slide>> {
                            _slideList.value = it
                        }
                    }
                }else {
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }
    fun getPromotions(orderType: OrderType){
        invoke {
            homeRepository.getGuestDigitalCoupons(orderType){homeResponse ->
                if(homeResponse.success){
                    homeResponse.data?.let {
                        it.cast<ArrayList<Campaign>> {
                            _promotionsList.value = it
                        }
                    }
                }else {
                    _showError.value = ErrorMsgItem(true, homeResponse.message)
                }
            }
        }
    }

    fun selectSliderImage(slide: Slide){
        slide.linkValue?.let {
            if(it.isNotEmpty()){
                if (slide.linkType == "url") {
                    _showLink.value = Event(Uri.parse(slide.linkValue))
                } else if (slide.linkType == "category") {
                    getMenuCategory(slide.linkValue)
                } else if (slide.linkType == "item") {
                    getMenuItem(slide.linkValue)
                }
            }
        }
    }

    private fun getMenuCategory(menuCategoryId: String) {
        invokeAtBackground {

        }
    }

    private fun getMenuItem(menuItemId: String) {
        invokeAtBackground {

        }
    }

    fun selectPromotions(promo : Campaign){

    }
}