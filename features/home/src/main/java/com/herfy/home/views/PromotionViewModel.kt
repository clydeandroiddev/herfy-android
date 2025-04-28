package com.herfy.home.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.repository.HomeRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.Event
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PromotionViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : BaseViewModel() {

    private val _showPromo = MutableLiveData<Event<Campaign?>>()
    val showPromo: LiveData<Event<Campaign?>> = _showPromo

    fun getPromotions(campaignId: String) {
        invoke {
            homeRepository.getCampaignCancellableAll { homeResponse ->
                if (homeResponse.success) {
                    homeResponse.data?.let {
                        var showCampaign: Campaign? = null
                        it.cast<ArrayList<Campaign>> { campaigns ->
                            for(campaign in campaigns) {
                                if(campaign.id == campaignId) {
                                    showCampaign = campaign
                                    break
                                }
                            }
                        }
                        _showPromo.value = Event(showCampaign)
                    } ?: run {
                        _showPromo.value = Event(null)
                    }
                } else {
                    _showPromo.value = Event(null)
                }
            }
        }
    }

}
