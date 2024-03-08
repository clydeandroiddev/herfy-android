package com.herfy.loyalty


import com.herfy.base.BasePresenter
import com.herfy.base.BaseView
import com.herfy.loyalty.objects.ListItem
import com.herfy.model.SortingCategory
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction

interface LoyaltyHistoryContract {

    interface View : BaseView<Presenter> {


        fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransaction)
        fun showSortingCategories(sortingCategories: ArrayList<SortingCategory>)

        fun updateSortingCategory(category: SortingCategory)

        fun updateTransactions()

        fun showLoyaltyError(message: String)

        fun showLoyaltyPointsHistory(loyaltyTransactions: ArrayList<LoyaltyTransaction>)


    }

    interface Presenter : BasePresenter, HistorySortAdapter.CategoryListener {
        fun getLoyaltyHistoryTransaction2(page: Int)

        fun onBindLoyaltyHistoryViewAtPosition(
            position: Int,
            loyaltyPointsViewHolder: LoyaltyPointsViewHolder
        )

        fun getConsolidatedList(): ArrayList<ListItem>

        fun getSortingCategories()

        fun getLoyaltyPointsHistoryCount(): Int

        fun cleanup()
    }

}