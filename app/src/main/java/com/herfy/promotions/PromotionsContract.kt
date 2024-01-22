package com.herfy.promotions

import com.herfy.base.BasePresenter
import com.herfy.base.BaseView
import com.skylinedynamics.solosdk.api.models.objects.Campaign

interface PromotionsContract {
    interface View : BaseView<Presenter?> {
        fun showLoadingDialog()
        fun showPromotions(favorites: ArrayList<Campaign>)
        fun showMessage(message: String?)
        fun showError(error: String?)
    }

    interface Presenter : BasePresenter {
        fun loadPromotions()
    }
}