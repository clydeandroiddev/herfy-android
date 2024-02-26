package com.herfy.upsell

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.checkout_temp.CheckoutActivity
import com.herfy.databinding.FragmentUpsellBinding
import com.herfy.payment.views.PaymentActivity
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import java.util.Locale


class UpsellFragment : BaseFragment(), UpsellContract.View {

    private lateinit var binding: FragmentUpsellBinding

    private lateinit var upsellPresenter: UpsellContract.Presenter

    private val args: UpsellFragmentArgs by navArgs()

    private var goodSelectedDateString: String? = null
    private var isScheduled = false
    private var goodDaySelected: String? = null
    private var toSkip = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsellPresenter = UpsellPresenter(this)
        initData()
    }


    private fun initData() {
        isScheduled = args.isScheduled
        if (isScheduled) {
            goodSelectedDateString = args.goodSelectedDate
            goodDaySelected = args.goodDaySelected
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUpsellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CacheUtil.getInstance().upsell = false
        upsellPresenter.start()
        if (upsellPresenter.upsellsList == 0) {
            try {
                val activity = activity as BaseActivity?
                activity?.showLoadingDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            upsellPresenter.getUpsells()
        } else {
            showUpsells(upsellPresenter.currentUpsells)
            upsellPresenter.getUpsellTotalPrice()
        }
    }

    override fun setPresenter(presenter: UpsellContract.Presenter) {
        upsellPresenter = presenter
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

        if (CacheUtil.getInstance().concept.vatType.equals("inclusive", ignoreCase = true)) {
            binding.totalVatLabel.visible()
        } else {
            binding.totalVatLabel.gone()
        }

        binding.placeOrder.setOnClickListener(View.OnClickListener {
            if (toSkip) {
                dismissDialogs()
//                val intent = Intent(requireActivity(), PaymentActivity::class.java) todo
                val intent = Intent(requireActivity(), CheckoutActivity::class.java)
                if (isScheduled) {
                    intent.putExtra("isScheduled", true)
                    intent.putExtra("goodSelectedDate", goodSelectedDateString)
                    intent.putExtra("goodDaySelected", goodDaySelected)
                }
                intent.putExtra(
                    "totalPrice", args.totalPrice.toDouble()
                )
                intent.putExtra(
                    "fromUpsellPage", true
                )
                startActivity(intent)
            } else {
                showLoadingDialog()
                upsellPresenter.continueCheckout()
            }
        })

        binding.upsellList.layoutManager = LinearLayoutManager(requireActivity())
    }

    override fun setupFonts() {
    }

    override fun setupTranslations() {
        binding.totalLabel.text =
            (CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total:"))
        binding.totalVatLabel.text = (CacheUtil.getInstance()
            .getTranslations(Text.TOTAL_INCLUSIVE_VAT, "(Total is inclusive of VAT)"))
        binding.placeOrder.text =
            (CacheUtil.getInstance().getTranslations(Text.SKIP_THIS, "SKIP THIS").uppercase(
                Locale.getDefault()
            ))
        binding.upsellTitle.text = (CacheUtil.getInstance().getTranslations(
            Text.WOULD_YOU_LIKE_TO_ADD_THESE, "Would you like to add these?"
        ).uppercase(
            Locale.getDefault()
        ))
        binding.upsellSubTitle.text = (CacheUtil.getInstance().getTranslations(
            Text.TAP_ON_A_MENU_ITEM_IF_YOU_WANT_TO_ADD_IT_TO_YOUR_CART,
            "Tap on a menu item if you want to add it to your cart"
        ).uppercase(
            Locale.getDefault()
        ))
    }

    override fun showUpsells(upsells: ArrayList<MenuItem>) {
        toSkip = upsellPresenter.checkIfHasNoUpsellSelected()
        if (toSkip) {
            binding.placeOrder.text =
                (CacheUtil.getInstance().getTranslations(Text.SKIP_THIS, "SKIP THIS").uppercase(
                    Locale.getDefault()
                ))
        } else {
            binding.placeOrder.text =
                (CacheUtil.getInstance().getTranslations(Text.ADD_AND_CONTINUE, "ADD AND CONTINUE")
                    .uppercase(
                        Locale.getDefault()
                    ))
        }
        dismissDialogs()
        if (upsells.size > 0) {
            //val adapter = UpsellRecyclerViewAdapter(upsellPresenter)
            //binding.upsellList.adapter = adapter
        } else {
            binding.placeOrder.performClick()
        }
    }

    override fun setUpsell(upsell: MenuItem?) {
    }

    override fun setUpsellTotalPrice(totalPrice: Double) {
        binding.totalPrice.text = (StringUtil.getFormattedPrice(totalPrice))
    }

    override fun continueCheckout() {
        toSkip = true
        binding.placeOrder.performClick()
    }

    override fun showDigitalCoupons(
        checkout: Boolean, digitalCoupons: MutableList<Campaign>?, selected: Int
    ) {
    }

    override fun upsellPromotionSucess(success: Boolean) {
    }

    override fun logFirebaseEvent(event: String?) {
        event?.let {
            if (Values.sDeepLinkUtm != null) {
                val firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())
                firebaseAnalytics.logEvent(it, Values.sDeepLinkUtm)
            }
        }
    }

    override fun goToItemDetails(position: Int, upsell: MenuItem) {
        NavigationUtils.navigateSafe(
            findNavController(), UpsellFragmentDirections.actionUpsellToMenuItem(
                upsell.attributes.categoryId.toString(), upsell.id, -1
            )
        )
    }

}