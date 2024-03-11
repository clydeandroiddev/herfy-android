package com.herfy.loyalty

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityLoyaltyPointsHistoryBinding
import com.herfy.model.SortingCategory
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.OnLayoutLoadingCompleted
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.CacheUtil
import com.moneyhash.sdk.android.extensions.inflater
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.Tier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoyaltyPointsHistory : BaseActivity(), LoyaltyHistoryContract.View {

    @Inject
    lateinit var loyaltyPresenter: LoyaltyHistoryContract.Presenter
    private val loyaltyPointsRewardTierAdapter: LoyaltyPointsRecyclerViewAdapter by lazy {
        LoyaltyPointsRecyclerViewAdapter(this, loyaltyPresenter)
    }
    private val historySortAdapter: HistorySortAdapter by lazy {
        HistorySortAdapter(categoryListener = loyaltyPresenter)
    }

    private val binding by lazy {
        ActivityLoyaltyPointsHistoryBinding.inflate(inflater)
    }

    private var canLoadMoreItems = false
    private var isLoadingHistory = false
    private var page = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        showLoadingDialog()
        loyaltyPresenter.start()
        loyaltyPresenter.getLoyaltyHistoryTransaction2(page)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(AnimUtil.outF(), AnimUtil.outS(), R.color.white)
            } else {
                overridePendingTransition(AnimUtil.outF(), AnimUtil.outS())
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
    }

    override fun setupViews() {
        binding.apply {
            toolbarTitle.apply {
                setNavButtonAction {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
            loyaltyPresenter.getSortingCategories()
            rvSorting.apply {
                adapter = historySortAdapter
                setHasFixedSize(true)
            }

            swipeRefreshLayout.setOnRefreshListener {
                empty.gone()
                rvSorting.visible()
                recyclerView.gone()
                placeholderLoyaltyHistory.shimmerLoyaltyHistory.visible()
                page = 1
                loyaltyPresenter.getLoyaltyHistoryTransaction2(page)
            }

            val linearLayoutManager = LinearLayoutManager(this@LoyaltyPointsHistory)
            recyclerView.apply {
                layoutManager = linearLayoutManager
                adapter = loyaltyPointsRewardTierAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (!isLoadingHistory && canLoadMoreItems && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= linearLayoutManager.itemCount - 1) {
                            isLoadingHistory = true
                            loyaltyPresenter.getLoyaltyHistoryTransaction2(page)
                        }
                    }
                })
            }
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.apply {
            toolbarTitle.title =
                CacheUtil.getInstance().getTranslations(Text.HISTORY_LABEL, "History")
            empty.setTitle(
                CacheUtil.getInstance()
                    .getTranslations(Text.NO_POINTS_HISTORY, "No points history found.")
            )
        }
    }


    override fun showLoyaltyError(message: String) {
        isLoadingHistory = false
        canLoadMoreItems = false
        dismissDialogs()
        binding.apply {
            empty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            rvSorting.visibility = View.GONE
            recyclerView.visibility = View.GONE
            placeholderLoyaltyHistory.shimmerLoyaltyHistory.gone()
        }
    }

    override fun setPresenter(presenter: LoyaltyHistoryContract.Presenter?) {

    }


    override fun showLoyaltyPointsHistory(loyaltyTransactions: ArrayList<LoyaltyTransaction>) {
        isLoadingHistory = false
        page += 1
        canLoadMoreItems = loyaltyTransactions.size != 0 && loyaltyTransactions.size % 10 == 0
        binding.apply {
            swipeRefreshLayout.isRefreshing = false
            loyaltyPointsRewardTierAdapter.notifyDataSetChanged()
            if (loyaltyTransactions.size > 0) {
                empty.visibility = View.GONE
                rvSorting.visibility = View.VISIBLE
                recyclerView.OnLayoutLoadingCompleted {
                    placeholderLoyaltyHistory.shimmerLoyaltyHistory.gone()
                    recyclerView.visibility = View.VISIBLE
                }
            } else {
                empty.visibility = View.VISIBLE
                rvSorting.visibility = View.GONE
                recyclerView.visibility = View.GONE
                placeholderLoyaltyHistory.shimmerLoyaltyHistory.gone()
            }
        }

        dismissDialogs()
    }

    override fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransaction) {}
    override fun showSortingCategories(sortingCategories: ArrayList<SortingCategory>) {
        binding.rvSorting.visible()
        historySortAdapter.updateList(sortingCategories)
    }


    override fun updateSortingCategory(category: SortingCategory) {
        historySortAdapter.updateCategoryList(category)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun updateTransactions() {
        loyaltyPointsRewardTierAdapter.notifyDataSetChanged()
    }

}
