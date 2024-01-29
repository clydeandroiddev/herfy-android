package com.herfy.digitalcoupons.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentDigitalCouponsBinding
import com.herfy.digitalcoupons.DigitalCouponsContract
import com.herfy.digitalcoupons.DigitalCouponsPresenter
import com.herfy.digitalcoupons.DigitalCouponsRecyclerViewAdapter
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.herfy.util.CacheUtil
import com.herfy.analytics.MixpanelUtil
import com.herfy.util.NavigationUtils
import java.util.HashMap

class DigitalCouponsFragment : BaseFragment(), DigitalCouponsContract.View {

    private lateinit var binding: FragmentDigitalCouponsBinding

    private lateinit var digitalCouponsPresenter: DigitalCouponsContract.Presenter

    var adapter: DigitalCouponsRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        digitalCouponsPresenter = DigitalCouponsPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDigitalCouponsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.isRefreshing = true
        digitalCouponsPresenter.start()
        digitalCouponsPresenter.getDigitalCoupons()
    }

    override fun setPresenter(presenter: DigitalCouponsContract.Presenter) {
        digitalCouponsPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {}
    override fun setupViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.empty.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            digitalCouponsPresenter.getDigitalCoupons()
        }
        try {
            val activity = activity as BaseActivity?
            if (activity != null) {
                adapter = DigitalCouponsRecyclerViewAdapter(
                    digitalCouponsPresenter,
                    object : DigitalCouponsRecyclerViewAdapter.OnAction {
                        override fun view(campaign: Campaign, card: MaterialCardView) {
                            val extras: FragmentNavigator.Extras = FragmentNavigator.Extras.Builder()
                                .addSharedElement(card, card.transitionName)
                                .build()
                            NavigationUtils.navigateSafe(findNavController(), DigitalCouponsFragmentDirections.actionDigitalCouponsToCoupon(Gson().toJson(campaign)), extras)
                        }

                        override fun avail(campaign: Campaign) {
                            if (CacheUtil.getInstance().couponCode != null) {
                                activity.showPromptAlertDialog("",
                                    CacheUtil.getInstance().getTranslations(
                                        Text.ONE_DISCOUNT_ONLY_MESSAGE,
                                        "You can only apply one discount at a time. Currently applied discount will be removed"
                                    ),
                                    CacheUtil.getInstance().getTranslations(
                                        Text.YES_CAPS
                                    ),
                                    DialogInterface.OnClickListener { dialogInterface, i ->
                                        dialogInterface.dismiss()
                                        CacheUtil.getInstance().couponCode = null
                                        CacheUtil.getInstance().couponName = null
                                        CacheUtil.getInstance().couponToAdd = null
                                        activity.showLoadingDialog()
                                        digitalCouponsPresenter.setDigitalCoupon(Gson().toJson(campaign))
                                        digitalCouponsPresenter.validateDigitalCoupon()
                                    },
                                    CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                                    DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() })
                            } else {
                                activity.showLoadingDialog()
                                digitalCouponsPresenter.setDigitalCoupon(Gson().toJson(campaign))
                                digitalCouponsPresenter.validateDigitalCoupon()
                            }
                        }
                    })
                binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
                binding.recyclerView.adapter = adapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.emptyTitle.text = CacheUtil.getInstance().getTranslations(
            Text.NO_PROMO_IS_AVAILABLE,
            "No promo is available"
        )
        binding.emptySubtitle.text = CacheUtil.getInstance().getTranslations(
            Text.NO_PROMO_AVAILABLE_MESSAGE,
            "Sorry, there is no promo available right now, try checking next time."
        )
    }

    override fun showCart() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        NavigationUtils.navigateSafe(
            findNavController(),
            DigitalCouponsFragmentDirections.actionDigitalCouponsToCart(
                isFromPromotions = true
            )
        )
    }

    override fun showDigitalCoupons(digitalCoupons: ArrayList<Campaign>) {
        binding.swipeRefreshLayout.isRefreshing = false
        adapter?.notifyDataSetChanged()
        if (digitalCoupons.size > 0) {
            binding.empty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {}

    override fun showError(error: String, back: Boolean) {
        try {
            val activity = activity as BaseActivity?
            activity?.showAlertDialog(
                "", error, CacheUtil.getInstance().getTranslations(Text.OK)
            ) { dialog, which ->
                if (back) {
                    activity.onBackPressed()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}