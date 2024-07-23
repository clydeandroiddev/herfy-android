package com.herfy.promotions.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.home.views.PromotionDetailBottomSheet
import com.herfy.model.CampaignToAdd
import com.herfy.model.PromotionCategory
import com.herfy.promotions.PromotionsContract
import com.herfy.promotions.adapters.PromotionsAdapter
import com.herfy.promotions.databinding.FragmentHerfyPromotionsBinding
import com.herfy.util.AppLogger
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class PromotionsFragment : BaseFragment(), PromotionsContract.View,
    com.herfy.home.adapter.PromotionsAdapter.PromotionListener {
    @Inject
    lateinit var promotionsPresenter: PromotionsContract.Presenter
    private lateinit var binding: FragmentHerfyPromotionsBinding
    private lateinit var promoCategoryAdapter: PromotionCategoryAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private val digitalCoupons: ArrayList<Campaign> = ArrayList()
    private var promotionDetailBottomSheet: PromotionDetailBottomSheet? = null

    override fun setPresenter(presenter: PromotionsContract.Presenter) {
        promotionsPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHerfyPromotionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        promotionsPresenter.start()
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {
        binding.toolbarTitle.setNavButtonAction {
            findNavController().navigateUp()
        }
        binding.btnGoToMenu.setOnClickListener {
            NavigationUtils.navigateSafe(
                findNavController(),
                PromotionsFragmentDirections.actionPromotionsToMenu()
            )
        }
        setupPromoCategory()
        setupPromotions()

    }


    private fun setupPromotions() {
        promotionsPresenter.loadPromotions()
        context?.let { ctx ->
            activity?.let {
                val itemHeight = (ViewUtil.getScreenWidth(it) / 1.65).toInt()
                promotionsAdapter = PromotionsAdapter(
                    promoListener = this@PromotionsFragment, itemWidth = 0, itemHeight = itemHeight,
                    defaultOrderTypeCount = CacheUtil.getInstance().application.attributes.getOrderTypes().size,
                    cacheUtil = CacheUtil.getInstance()
                )
                binding.apply {
                    rvPromotions.apply {
                        adapter = promotionsAdapter
                        layoutManager = GridLayoutManager(ctx, 2)
                        setHasFixedSize(false)
                    }
                }
            }
        }
    }

    private fun setupPromoCategory() {
        promotionsPresenter.getPromotionCategoryList()
        context?.let { ctx ->
            binding.apply {
                placeholderPromotions.shimmerPromotionsTitle.gone()
                promoCategoryAdapter =
                    PromotionCategoryAdapter(categoryListener = promotionsPresenter)
                binding.apply {
                    rvPromoCategory.apply {
                        adapter = promoCategoryAdapter
                        layoutManager =
                            LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = true
                    }
                }
            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            toolbarTitle.title =
                CacheUtil.getInstance().getTranslations(Text.PROMOTIONS, "Promotions")
            tvEmptyStateTitle.text = CacheUtil.getInstance()
                .getTranslations(Text.STAY_TUNED_PROMO, "Stay Tuned for Culinary Delights!")
            tvEmptyStateContext.text = CacheUtil.getInstance().getTranslations(
                Text.PROMOTION_EMPTY_CONTENT,
                "Our promotions are on a brief pause, but get ready\nfor a mouthwatering comeback in just a few days."
            )
            btnGoToMenu.text =
                CacheUtil.getInstance().getTranslations(Text.GO_TO_MENU, "Go to Menu")
        }
    }

    override fun showPromotions(campaigns: ArrayList<Campaign>) {
        runOnMain {
            binding.apply {
                placeholderPromotions.clPlaceholderPromo.gone()
            }
            digitalCoupons.clear()
            digitalCoupons.addAll(campaigns)
            checkCampaigns()
            promotionsAdapter.updateCampaigns(digitalCoupons)
        }
    }

    override fun showMessage(message: String?) {

    }

    override fun showError(error: String?, back: Boolean) {
        runOnMain {
            checkCampaigns()
            try {
                val activity = activity as BaseActivity?
                activity?.showAlertDialog(
                    "",
                    error,
                    CacheUtil.getInstance().getTranslations(Text.OK)
                ) { dialog, which ->
                    promotionDetailBottomSheet?.dismissLoading()
                    if (back) {
                        activity.onBackPressed()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun showPromotionsCategory(promocateglist: MutableList<PromotionCategory>) {
        runOnMain {
            binding.apply {
                if (promocateglist.size > 0) {
                    promoCategoryAdapter.updateList(promocateglist)
                }
            }
        }
    }

    override fun updatePromoCategoryList(category: PromotionCategory) {
        promoCategoryAdapter.updateCategoryList(category)
        binding.apply {
            rvPromotions.gone()
            placeholderPromotions.apply {
                shimmerPromotionsTitle.invisible()
                shimmerPromotionsCategory.invisible()
                shimmerPromotionsList.visible()
                clPlaceholderPromo.visible()
            }

        }
    }

    private fun showErrorAlertDialog(error: String) {
        activity?.runOnUiThread {
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                it.showAlertDialog("", error)
            }
        }
    }

    private fun checkCampaigns() {
        binding.grpPromo.apply {
            if (digitalCoupons.isNotEmpty()) visible() else gone()
        }
        binding.clEmptyState.apply {
            if (digitalCoupons.isEmpty()) visible() else gone()
        }
    }

    private fun getCampaignPromotionItem(digitalCoupon: Campaign?) {
        digitalCoupon?.promotion?.let { promotion ->
            MenuApiCall.getInstance()
                .getItemDetails(promotion.attributes.itemId, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val response = JSONObject(o.toString())
                            val dataArray = response.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val menuItem = GsonHelper.gsonFinal().fromJson(
                                    dataArray.getJSONObject(0).toString(), MenuItem::class.java
                                )
                                menuItem.isPromo = true
                                if (promotion.attributes.itemQuantity > 0) {
                                    menuItem.attributes.quantity = promotion.attributes.itemQuantity
                                } else {
                                    menuItem.attributes.quantity = 1
                                }
                                var orderStoreId: String? = null
                                if (CacheUtil.getInstance().orderStore != null) {
                                    orderStoreId = CacheUtil.getInstance().orderStore.id
                                }
                                menuItem.attributes.price = promotion.attributes.itemPrice
                                menuItem.toCdn(orderStoreId)
                                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                                if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                                    val campaignToAdd = CampaignToAdd(
                                        digitalCoupon,
                                        digitalCoupon.promotion,
                                        menuItem
                                    )
                                    CacheUtil.getInstance().campaignToAdd = campaignToAdd
                                } else {
                                    CacheUtil.getInstance().campaign = digitalCoupon
                                    CacheUtil.getInstance().campaignPromotion =
                                        digitalCoupon.promotion
                                    CacheUtil.getInstance().campaignPromotionMenuItem = menuItem
                                }
                                showCart(isAvail = true)
                            } else {
                                onError("")
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onError(e)
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        if (o is JSONObject) {
                            try {
                                val error = o["error"]
                                var detail =
                                    CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                                if (error is JSONObject) {
                                    detail = error.getString("detail")
                                } else if (error is JSONArray) {
                                    val errorObject = error.getJSONObject(0)
                                    detail = errorObject.getString("detail")
                                }
                                showErrorAlertDialog(detail)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                showErrorAlertDialog(
                                    CacheUtil.getInstance().getTranslations(
                                        Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                        "You are not eligible to claim the promotion."
                                    )
                                )
                            }
                        } else {
                            showErrorAlertDialog(
                                CacheUtil.getInstance().getTranslations(
                                    Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION,
                                    "You are not eligible to claim the promotion."
                                )
                            )
                        }
                    }
                })
        }

    }

    override fun showCart(isAvail: Boolean) {
        promotionDetailBottomSheet?.dismissAllowingStateLoss()
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        NavigationUtils.navigateSafe(
            findNavController(),
            PromotionsFragmentDirections.actionPromotionsToCart(
                isFromPromotions = isAvail
            )
        )
    }

    override fun showMenu() {
        promotionDetailBottomSheet?.dismissAllowingStateLoss()
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
        NavigationUtils.navigateSafe(
            findNavController(),
            PromotionsFragmentDirections.actionPromotionsToMenu()
        )
    }


    override fun onSelectedPromo(promo: Campaign) {
        promotionDetailBottomSheet = PromotionDetailBottomSheet.newInstance(
            promo,
            object : PromotionDetailBottomSheet.onAction {
                override fun onClickClaimPromo(promo: Campaign) {
//                    if (CacheUtil.getInstance().orderTypeNotBlank()) {
//                        showLoadingDialog()
//                        promotionsPresenter.onAvailDigitalCoupon(promo)
//                    } else {
                    CacheUtil.getInstance().initialCampaign = promo
                    showOrderTypeFromCampaign()
                    // }
                }

                override fun onRemovePromo() {
                    showCart(false)
                }

                override fun onShowMenu() {
                    showMenu()
                }

            }
        )
        promotionDetailBottomSheet?.show(
            childFragmentManager, PromotionDetailBottomSheet::class.java.simpleName
        )
    }

    private fun checkBothType(): Boolean {
        val campaign = CacheUtil.getInstance().initialCampaign
        var types = 0
        if (campaign != null) {
            val promoRules = campaign.promotion.attributes.rules
            if (promoRules.isNotEmpty() && promoRules.find { rule ->
                    rule.ruleKey.equals(
                        "order-type", true
                    )
                } != null) {
                promoRules.find { rule -> rule.ruleKey.equals("order-type", true) }?.let {
                    val orderTypes = it.text.split(Pattern.compile(","))
                    for (orderType in orderTypes) {
                        when (orderType.trim()) {
                            "pickup", "curbside", "drive-thru" -> {
                                types += 1
                                CacheUtil.getInstance().initialOrderType = OrderType.PICKUP
                            }

                            "delivery", "deliver" -> {
                                types += 1
                                CacheUtil.getInstance().initialOrderType = OrderType.DELIVERY
                            }
                        }
                    }
                }
            }
        }
        return types > 1
    }

    private fun showOrderTypeFromCampaign() {
        promotionDetailBottomSheet?.dismissAllowingStateLoss()
        activity?.let {
            it.runOnUiThread {
                val intent = Intent()
                val storeId = CacheUtil.getOrderStoreId() ?: ""
                intent.setClassName(it, Navigation.NAVIGATION_ACTIVITY)
                intent.putExtra("navigation", "nav_order_type")
                intent.putExtra("selectedOrderType", 0)
                intent.putExtra("fromHome", true)
                intent.putExtra("fromCampaign", true)
                intent.putExtra("isBothType", checkBothType())
                intent.putExtra("fromCart", false)
                intent.putExtra("checkout", false)
                intent.putExtra("storeId", storeId)
                it.startActivity(intent)
            }
        }
    }

}