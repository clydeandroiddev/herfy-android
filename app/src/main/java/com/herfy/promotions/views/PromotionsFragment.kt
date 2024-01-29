package com.herfy.promotions.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentPromotionsBinding
import com.herfy.ext.cast
import com.herfy.ext.gone
import com.herfy.ext.visible
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.util.GsonHelper
import com.herfy.util.AppLogger
import com.herfy.util.CacheUtil
import com.herfy.analytics.MixpanelUtil
import com.herfy.promotions.adapters.PromotionsAdapter
import com.herfy.promotions.PromotionsContract
import com.herfy.promotions.PromotionsPresenter
import com.herfy.promotions.data.CampaignToAdd
import com.herfy.util.NavigationUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PromotionsFragment : BaseFragment(), PromotionsContract.View {

    private lateinit var binding: FragmentPromotionsBinding

    private var promotionsPresenter: PromotionsContract.Presenter? = null

    private val adapter: PromotionsAdapter by lazy {
        PromotionsAdapter(emptyArray()) { campaign ->
            selectCampaign(campaign)
        }
    }

    private val digitalCoupons: ArrayList<Campaign> = ArrayList()

    override fun setPresenter(presenter: PromotionsContract.Presenter?) {
        promotionsPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        promotionsPresenter = PromotionsPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        promotionsPresenter?.start()
        if (digitalCoupons.isEmpty()) {
            binding.blocker.visible()
            binding.promotionShimmer.visible()
        }
        promotionsPresenter?.loadPromotions()
    }

    private fun initAdapter() {
        binding.recyclerView.adapter = adapter
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

    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.emptyText.text =
            CacheUtil.getInstance().getTranslations(Text.NO_PROMOS_AVAIlABLE, "No Promos Available")
        binding.emptyTextDesc.text = CacheUtil.getInstance().getTranslations(
            Text.NO_PROMOS_AVAILABLE_DESC,
            "Sorry, there is no promo available right now, try checking next time"
        )
    }

    override fun showPromotions(campaigns: ArrayList<Campaign>) {
        binding.blocker.gone()
        binding.promotionShimmer.gone()
        digitalCoupons.clear()
        digitalCoupons.addAll(campaigns)
        checkCampaigns()
        adapter.updateCampaigns(digitalCoupons.toTypedArray())
    }

    override fun showMessage(message: String?) {

    }

    override fun showError(error: String?) {
        binding.blocker.gone()
        binding.promotionShimmer.gone()
        checkCampaigns()
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
        binding.emptyLayout.isVisible = digitalCoupons.isEmpty()
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
                                    CacheUtil.getInstance().campaignPromotion = digitalCoupon.promotion
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

    private fun showCart(isAvail: Boolean) {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        NavigationUtils.navigateSafe(
            findNavController(),
            PromotionsFragmentDirections.actionPromotionsToCart(
                isFromPromotions = isAvail
            )
        )
    }

    private fun selectCampaign(campaign: Campaign?) {
        campaign?.let {
            setFragmentResultListener(PromotionFragment.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                clearFragmentResultListener(requestKey = PromotionFragment.ARGS_KEY_FRAGMENT_RESULT)
                if (bundle.containsKey(PromotionFragment.ARGS_KEY_ACTION)) {
                    if(bundle.getString(PromotionFragment.ARGS_KEY_ACTION) == "avail") {
                        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO)
                        CacheUtil.getInstance().campaign = it
                        CacheUtil.getInstance().campaignPromotion = it.promotion
                        if (it.promotion?.attributes?.type.equals(
                                "item",
                                ignoreCase = true
                            )
                        ) {
                            showLoadingDialog()
                            getCampaignPromotionItem(it)
                        } else {
                            showCart(isAvail = true)
                        }
                    } else {
                        showCart(isAvail = false)
                    }
                }
            }
            NavigationUtils.navigateSafe(
                findNavController(),
                PromotionsFragmentDirections.actionPromotionsToPromotion(
                    campaign = Gson().toJson(it)
                )
            )
        }
    }
}