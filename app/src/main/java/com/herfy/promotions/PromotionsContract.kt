package com.herfy.promotions

import com.herfy.base.BasePresenter
import com.herfy.base.BaseView
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.model.PromotionCategory
import com.skylinedynamics.solosdk.api.models.objects.Campaign

interface PromotionsContract {
    interface View : BaseView<Presenter> {
        fun showLoadingDialog()
        fun showPromotions(campaigns: ArrayList<Campaign>)
        fun showMessage(message: String?)
        fun showError(error: String?, back: Boolean)
        fun showPromotionsCategory(promocateglist: MutableList<PromotionCategory>)
        fun updatePromoCategoryList(category: PromotionCategory)
        fun showCart(isAvail : Boolean)
        fun showMenu()
    }

    interface Presenter : BasePresenter, PromotionCategoryAdapter.PromotionCategoryListener{
        fun loadPromotions()
        fun getPromotionCategoryList()
        fun validateDigitalCoupon(digitalCoupon : Campaign)
    }
}