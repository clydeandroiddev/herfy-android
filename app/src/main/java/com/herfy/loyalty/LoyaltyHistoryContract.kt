package com.herfy.loyalty


import com.herfy.base.BasePresenter
import com.herfy.base.BaseView
import com.herfy.model.SortingCategory
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransactionV2

interface LoyaltyHistoryContract {

    interface View : BaseView<Presenter> {


        fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransactionV2)
        fun showSortingCategories(sortingCategories: ArrayList<SortingCategory>)

        fun updateSortingCategory(category: SortingCategory)

        fun updateTransactions()

        fun showLoyaltyError(message: String)

        fun showLoyaltyPointsHistory(loyaltyTransactions: ArrayList<LoyaltyTransactionV2>)


    }

    interface Presenter : BasePresenter, HistorySortAdapter.CategoryListener {
        fun getLoyaltyHistoryTransaction2(page: Int)
        fun onBindLoyaltyHistoryViewAtPosition(
            position: Int,
            loyaltyPointsViewHolder: LoyaltyPointsViewHolder
        )
        fun transactionListCount() : Int
        fun getSortingCategories()
        fun getLoyaltyPointsHistoryCount(): Int
        fun cleanup()
    }

}