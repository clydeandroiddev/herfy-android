package com.herfy.home.views

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.herfy.home.R
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.home.databinding.FragmentHomeBinding
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.setInteractionDisabled
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.TimeUtil
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class
HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    override val viewModel: HomeViewModel by viewModels()
    override val layout: Int = R.layout.fragment_home
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private lateinit var promotionCategoryAdapter: PromotionCategoryAdapter
    private var sliderHandler: Handler? = null
    private lateinit var bottomSliderImageAdapter: SliderImageAdapter
    private var bottomSliderhandler: Handler? = null
    private var promotionDetailBottomSheet: PromotionDetailBottomSheet? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }

    override fun initViews() {
        super.initViews()
        viewModel.getMenuCategory()
        binding.apply {
            setupSlider()
            setupPromoCategory()
            setupPromotions()
            setupOrderType()
            setupBottomSlider()
            layoutLoyalty.apply {
                //progress.isEnabled = false
                btnContinue.setOnClickListener {
                    context?.let {
                        val intent = Intent()
                        intent.setClassName(it, Navigation.AUTH_ACTIVITY)
                        intent.putExtra("sign_in", true)
                        intent.putExtra("guest", false)
                        intent.putExtra("curbside", false)
                        intent.putExtra("checkout", false)
                        startActivity(intent)
                    }

                }
                clSeeRewards.setOnClickListener {
                    context?.let {
                        val intent = Intent()
                        intent.setClassName(it, Navigation.LOYALTY_ACTIVITY)
                        startActivity(intent)
                    }
                }
            }
            layoutCatering.btnCatering.setOnClickListener {
                activity?.apply {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Values.CATER_WITH_HERFY_URL)
                        )
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateGreetings()
        updateLoyalty()
        setupOrdertracker()
    }

    private fun updateLoyalty() {
        if (AuthUtil.getInstance().isSignedIn) {
            setupCustomerLoyalty()
        } else {
            binding.layoutLoyalty.apply {
                rlLoyaltyContent.gone()
                lnLoyaltyEmptyState.visible()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateGreetings() {
        binding.apply {
            val authUtil = AuthUtil.getInstance()
            if (authUtil.isSignedIn && authUtil.customer != null) {
                tvHomeGreetings.text =
                    TimeUtil.getGreetings(authUtil.customer.attributes.firstName)
                tvHomeGreetingsEmoji.visible()
                tvHomeGreetingsEmoji.text = resources.getString(TimeUtil.getGreetingsIcon())
            } else {
                tvHomeGreetings.text = CacheUtil.getInstance()
                    .getTranslations(Text.WELCOME_TO_HERFY, "Welcome to Herfy!")
                tvHomeGreetingsEmoji.gone()
            }
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            cacheUtilHelper?.let { cacheHelper ->
                layoutPromotions.tvPromotions.text =
                    cacheHelper.getTranslations(Text.PROMOTIONS, "PROMOTIONS")
                layoutCatering.apply {

                    tvCaterTitle.text = cacheHelper.getTranslations(
                        Text.CATER_WITH_HERFY, "Cater with Herfy"
                    )
                    tvCaterDescription.text = cacheHelper.getTranslations(
                        Text.CATER_WITH_HERFY_DESC,
                        "Best event. Best budget. Check out our catering!"
                    )
                    btnCatering.text =
                        cacheHelper.getTranslations(Text.ORDER_CATERING, "Order Catering")
                }
                layoutOrderTypeMain.apply {
                    tvHowWillYouLabel.text = cacheHelper.getTranslations(
                        Text.HOW_WILL_YOU_ORDER, "How will you order today?"
                    )
                }
                layoutBottomBanner.tvBannerBottom.text = CacheUtil.getInstance()
                    .getTranslations(Text.HERFY_SUPPORT, "Herfy supports the cause")
                layoutLoyalty.apply {
                    tvLoyaltyLabel.text =
                        cacheHelper.getTranslations(Text.ASHAB_HERFY, "Ashab Herfy")
                    tvLabelYourPoints.text =
                        cacheHelper.getTranslations(Text.TOTAL_POINTS, "Total Points")
                    tvSarLabel.text =
                        cacheHelper.getTranslations(Text.SAR_VALUE_LABEL, "SR VALUE")
                    tvSeeRewards.text =
                        cacheHelper.getTranslations(Text.SEE_MY_REWARDS, "See my rewards")
                    tvTitleEmptyState.text = cacheHelper.getTranslations(
                        Text.BECOME_ASHAB_HERFY, "Become an Ashab Herfy!"
                    )
                    tvDescriptionEmptyState.text = cacheHelper.getTranslations(
                        Text.BECOME_ASHAB_HERFY_DESCRIPTION,
                        "By creating an account or signing in to your existing one you get point on every order"
                    )
                    btnContinue.text = cacheHelper.getTranslations(Text.CONTINUE_LABEL, "Continue")
                }
            }
        }
    }

    private fun setupSlider() {
        activity?.let {
            viewModel.getSliders()
            binding.apply {
                sliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                val displayMetrics = DisplayMetrics()
                it.windowManager.defaultDisplay.getMetrics(displayMetrics)
                val sliderHeight = (displayMetrics.widthPixels / 1.1).toInt()
                layoutBanner.mainToolbar.layoutParams.height = sliderHeight
                layoutBanner.sliderViewPager.layoutParams.height = sliderHeight
                layoutBanner.newMenuBannerShimmer.layoutParams.height = sliderHeight
                layoutBanner.sliderViewPager.adapter = sliderImageAdapter
            }
        }

    }

    override fun observe() {
        super.observe()
        viewModel.slideList.observeNonNull(this) { slides ->
            binding.apply {
                layoutBanner.apply {
                    newMenuBannerShimmer.visibility = View.GONE
                    emptySlider.setBackgroundResource(R.color.white)
                    if (slides.size > 0) {
                        emptySlider.visibility = View.GONE
                        sliderViewPager.visibility = View.VISIBLE
                        sliderImageAdapter.updateImageList(slides)
                        if (slides.size > 1) {
                            dotsIndicator.setViewPager2(sliderViewPager)
                        } else {
                            dotsIndicator.visibility = View.GONE
                        }
                        sliderHandler?.removeCallbacksAndMessages(null)
                        sliderHandler = Handler(Looper.getMainLooper())
                        sliderHandler?.let {
                            it.apply {
                                postDelayed(object : Runnable {
                                    override fun run() {
                                        var pos = sliderViewPager.currentItem + 1
                                        if (pos >= slides.size) {
                                            pos = 0
                                            sliderViewPager.setCurrentItem(pos, false)
                                        } else {
                                            sliderViewPager.currentItem = pos
                                        }
                                        sliderHandler?.postDelayed(this, 3000)
                                    }
                                }, 3000)
                            }
                        }
                    } else {
                        emptySlider.visibility = View.VISIBLE
                        sliderViewPager.visibility = View.VISIBLE
                        dotsIndicator.visibility = View.GONE
                        properSliderHeights()
                    }
                }
            }
        }

        viewModel.promotionsList.observeNonNull(this) { promos ->
            binding.apply {
                layoutPromotions.apply {
                    placeholderPromotions.clPlaceholderPromo.visibility = View.GONE
                    if (promos.size > 0) {
                        clPromotions.visibility = View.VISIBLE
                        grpPromo.visibility = View.VISIBLE
                        promotionsAdapter.updatePromoList(promos)
                    } else {
                        clPromotions.visibility = View.GONE
                        grpPromo.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.showError.observeNonNull(this) {
            if (it.message.isNotEmpty()) {
                showAlertDialog(
                    title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"),
                    message = it.message
                )
            }
        }

        viewModel.showLink.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { uri ->
                try {
                    val activity = activity
                    if (activity != null) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = uri
                        activity.startActivity(intent)
                        activity.overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        viewModel.showMenuItem.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { pair ->
                activity?.apply {
                    runOnUiThread {
                        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
                        findNavController().navigateToDeepLink("nav_menu_item/${pair.first}/${pair.second}")
                    }
                }
            }
        }
        viewModel.showMenuCategory.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuCategoryId ->
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
                findNavController().navigateToDeepLink("nav_menu/$menuCategoryId")
            }
        }

        viewModel.showPromo.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { promo ->
                promotionDetailBottomSheet = PromotionDetailBottomSheet.newInstance(
                    promo, viewModel
                )
                promotionDetailBottomSheet?.show(
                    childFragmentManager, PromotionDetailBottomSheet::class.java.simpleName
                )
            }
        }

        viewModel.orderTypes.observeNonNull(this) { orderTypes ->
            orderTypes.let {
                if (it.isNotEmpty()) {
                    cacheUtilHelper?.let { cacheHelper ->
                        binding.layoutOrderTypeMain.apply {
                            deliveryContainer.isVisible = false
                            pickupContainer.isVisible = false
                            dineinContainer.isVisible = false
                            layoutTypeList.isVisible = true
                            for (orderType: OrderType in orderTypes) {
                                if (orderType === OrderType.DELIVERY) {
                                    deliveryLabel.text =
                                        cacheHelper.getTranslations(Text.DELIVERY, "Delivery")
                                    deliveryContainer.isVisible = true
                                    deliveryContainer.setOnClickListener {
                                        orderTypeClick(OrderType.DELIVERY)
                                    }
                                } else if (orderType === OrderType.PICKUP) {
                                    pickupLabel.text =
                                        cacheHelper.getTranslations(Text.PICKUP, "Pickup")
                                    pickupContainer.isVisible = true
                                    pickupContainer.setOnClickListener {
                                        orderTypeClick(OrderType.PICKUP)
                                    }
                                } else if (orderType === OrderType.DINE_IN) {
                                    dineinLabel.text =
                                        cacheHelper.getTranslations(Text.DINE_IN, "Dine In")
                                    dineinContainer.isVisible = true
                                    dineinContainer.setOnClickListener {
                                        orderTypeClick(OrderType.DINE_IN)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.loyalty.observe(this) { loyalty ->

            binding.layoutLoyalty.apply {
                loyalty?.let {
                    clCustomerLoyalty.visibility = View.VISIBLE
                    rlLoyaltyContent.visible()
                    lnLoyaltyEmptyState.gone()
                    tvPoints.text = StringUtil.localize(loyalty.attributes.pointsAccumulated.toString())
                    val currentBalanceValue = String.format("%.2f", loyalty.attributes.currentBalanceAmount)
                    tvSarValue.text = StringUtil.localize(currentBalanceValue)
                    var loyaltyTier = loyalty.attributes.loyaltyTier?.name ?: ""
                    if (LocaleUtil.getInstance().isEnglish) {
                        loyaltyTier = loyaltyTier.lowercase().replaceFirstChar(Char::titlecase)
                    }
                    if (loyaltyTier.isNotEmpty()) {
                        var tierBackground = R.drawable.background_bronze_tier
                        if (loyaltyTier.equals(
                                "Silver", ignoreCase = true
                            ) || loyaltyTier == "فضي"
                        ) {
                            tierBackground = R.drawable.background_silver_tier
                        } else if (loyaltyTier.equals(
                                "Gold", ignoreCase = true
                            ) || loyaltyTier == "ذهبي"
                        ) {
                            tierBackground = R.drawable.background_gold_tier
                        } else if (loyaltyTier.equals(
                                "Diamond",
                                true
                            ) || loyaltyTier == "البلاتين"
                        ) {
                            tierBackground = R.drawable.background_diamond_tier
                        }
                        tvLoyaltyTier.text = loyaltyTier
                        tvLoyaltyTier.setBackgroundResource(tierBackground)
                    }

                } ?: run {
                    rlLoyaltyContent.gone()
                    lnLoyaltyEmptyState.visible()
                }
            }
        }

        /*viewModel.pointsToNextTier.observeNonNull(this) { pointsToNextTier ->
            if (pointsToNextTier <= 0) {
                viewModel.tier.observeNonNull(this) { tier ->
                    if (tier == null) {
                    } else {
                    }

                }
            } else {
                viewModel.startPoints.observeNonNull(this) { startPoints ->
                }



                viewModel.tier.observeNonNull(this) { tier ->
                }

            }
        }*/

        viewModel.promotionCategoryList.observeNonNull(this) { promoCategories ->
            binding.layoutPromotions.apply {
                if (promoCategories.size > 0) {
                    promotionCategoryAdapter.updateList(promoCategories)
                }
            }
        }
        viewModel.updatePromoCategoryList.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { selectedCategory ->
                promotionCategoryAdapter.updateCategoryList(selectedCategory)
                binding.layoutPromotions.apply {
                    rvPromotions.visibility = View.GONE
                    placeholderPromotions.apply {
                        shimmerPromotionsTitle.visibility = View.INVISIBLE
                        shimmerPromotionsCategory.visibility = View.INVISIBLE
                        shimmerPromotionsList.visibility = View.VISIBLE
                        clPlaceholderPromo.visibility = View.VISIBLE
                    }

                }
            }
        }

        viewModel.promoFilteredList.observe(this) { promos ->
            binding.apply {
                layoutPromotions.apply {
                    placeholderPromotions.clPlaceholderPromo.visibility = View.GONE
                    promos?.let {
                        if (it.isNotEmpty()) {
                            rvPromotions.visibility = View.VISIBLE
                            promotionsAdapter.updatePromoList(promos)
                        } else {
                            rvPromotions.visibility = View.GONE
                        }
                    } ?: run {
                        rvPromotions.visibility = View.GONE
                    }
                }
            }

        }
        viewModel.bottomSlideList.observeNonNull(this) { slides ->
            binding.apply {
                layoutBottomBanner.apply {
                    shimmerBannerBottom.visibility = View.GONE
                    if (slides.size > 0) {
                        clBottomBanner.visibility = View.VISIBLE
                        grpBannerBottom.visibility = View.VISIBLE
                        bottomSliderImageAdapter.updateImageList(slides)
                        if (slides.size > 1) {
                            sdiBannerBottom.visibility = View.VISIBLE
                            sdiBannerBottom.setViewPager2(vpBannerBottom)
                        } else {
                            sdiBannerBottom.visibility = View.GONE
                        }
                        bottomSliderhandler?.removeCallbacksAndMessages(null)
                        bottomSliderhandler = Handler(Looper.getMainLooper())
                        bottomSliderhandler?.let {
                            it.apply {
                                postDelayed(object : Runnable {
                                    override fun run() {
                                        var pos = vpBannerBottom.currentItem + 1
                                        if (pos >= slides.size) {
                                            pos = 0
                                            vpBannerBottom.setCurrentItem(pos, false)
                                        } else {
                                            vpBannerBottom.currentItem = pos
                                        }
                                        bottomSliderhandler?.postDelayed(this, 3000)
                                    }
                                }, 3000)
                            }
                        }
                    } else {
                        clBottomBanner.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.showCart.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { isAvail ->
                promotionDetailBottomSheet?.dismissAllowingStateLoss()
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                findNavController().navigateToDeepLink("nav_cart/$isAvail")
            }
        }

        viewModel.showProgressBar.observe(this) {
            activity?.window?.setInteractionDisabled(it)
            if (it) {
                showProgressDialog(true)
            } else {
                dismissProgressDialog()
            }
        }

        viewModel.showOrderTrackerNotif.observe(this) {
            it.getContentIfNotHandled()?.let { orderDetails ->
                binding.apply {
                    orderTracker.clOrderNotif.visibility = View.VISIBLE
                    orderTracker.tvStatusLabel.text = orderDetails.orderStatus.description
                    orderTracker.tvOrderTypeLabel.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.YOUR_ORDER_FROM,
                            "Your (x) order from Herfy"
                        ).replace("(x)", orderDetails.orderType)
                    orderTracker.tvEstimateValue.text =
                        CacheUtil.getInstance().getTranslations(Text.MIN, "(x) mins")
                            .replace("(x)", CacheUtil.getInstance().promisedTime)
                    orderTracker.tvEstimateLabel.text =
                        CacheUtil.getInstance().getTranslations(Text.ESTIMATED, "(Estimated)")
                    orderTracker.otherDetails.setOnClickListener {
                        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ORDER_DETAILS)
                        findNavController().navigateToDeepLink("nav_new_order_tracker/${orderDetails.id}")
                    }
                }
            }
        }

        viewModel.updateOrderTrackerNotif.observe(this) { orderData ->
            binding.apply {
                if (orderData.orderStatus.sequence < 9) {
                    orderTracker.tvStatusLabel.text = orderData.orderStatus.description
                } else {
                    CacheUtil.getInstance().promisedTime = ""
                    orderTracker.clOrderNotif.visibility = View.GONE
                }
            }
        }
    }


    private fun orderTypeClick(orderType: OrderType) {
        activity?.let {
            it.runOnUiThread {
                val intent = Intent()
                val storeId = CacheUtil.getOrderStoreId() ?: ""
                intent.setClassName(it, Navigation.NAVIGATION_ACTIVITY)
                intent.putExtra("navigation", "nav_order_type")
                intent.putExtra("selectedOrderType", orderType.value)
                intent.putExtra("fromHome", true)
                intent.putExtra("fromCart", false)
                intent.putExtra("checkout", false)
                intent.putExtra("storeId", storeId)
                it.startActivity(intent)
            }
        }
    }

    private fun properSliderHeights() {
        binding.apply {
            layoutBanner.apply {
                try {
                    activity?.let { act ->
                        val sliderHeight =
                            act.resources.getDimensionPixelSize(R.dimen.pixel_twenty_five)
                        cacheUtilHelper?.getApplication()?.let {
                            if (it.attributes.settings.categoryDisplay.equals(
                                    Values.IMAGE_TEXT, ignoreCase = true
                                )
                            ) {
                                mainToolbar.layoutParams.height =
                                    sliderHeight + (act.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider) - act.resources.getDimensionPixelSize(
                                        R.dimen.seventy_dp
                                    ))
                            } else {
                                mainToolbar.layoutParams.height =
                                    sliderHeight + act.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider)
                            }
                        }
                        sliderViewPager.layoutParams.height = sliderHeight
                        emptySlider.layoutParams.height = sliderHeight
                        newMenuBannerShimmer.layoutParams.height = sliderHeight
                        mainToolbar.invalidate()
                        sliderViewPager.invalidate()
                        emptySlider.invalidate()
                        newMenuBannerShimmer.invalidate()
                        mainToolbar.visibility = View.GONE
                        mainToolbar.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }


    }

    private fun setupPromotions() {
        context?.let { ctx ->
            viewModel.getPromotions()
            activity?.let {
                val itemHeight = (ViewUtil.getScreenWidth(it) / 1.65).toInt()
                promotionsAdapter = PromotionsAdapter(
                    promoListener = viewModel,
                    itemWidth = 0,
                    itemHeight = itemHeight,
                    defaultOrderTypeCount = CacheUtil.getInstance().application.attributes.getOrderTypes().size,
                    cacheUtil = CacheUtil.getInstance()
                )
                binding.apply {
                    layoutPromotions.rvPromotions.apply {
                        adapter = promotionsAdapter
                        layoutManager = GridLayoutManager(ctx, 2)
                        setHasFixedSize(false)
                    }
                }
            }
        }
    }

    private fun setupOrdertracker() {
        if (AuthUtil.getInstance().isSignedIn) {
            if (CacheUtil.getInstance().lastOrderId.isEmpty()) {
                viewModel.getCustomerOrder()
            } else {
                viewModel.getLastOrder()
            }
        } else {
            binding.orderTracker.clOrderNotif.visibility = View.GONE
        }
    }

    private fun setupPromoCategory() {
        context?.let { ctx ->
            viewModel.getPromotionCategoryList()
            binding.apply {
                promotionCategoryAdapter = PromotionCategoryAdapter(categoryListener = viewModel)
                binding.apply {
                    layoutPromotions.rvPromoCategory.apply {
                        adapter = promotionCategoryAdapter
                        layoutManager =
                            LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = true
                    }
                }
            }
        }
    }

    private fun setupOrderType() {
        context?.let {
            viewModel.getOrderTypes()
        }
    }

    private fun setupCustomerLoyalty() {
        context?.let { ctx ->
            viewModel.getLoyalty()
        }
    }

    private fun setupBottomSlider() {
        activity?.let {
            viewModel.getBottomSliders()
            binding.apply {
                bottomSliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                layoutBottomBanner.vpBannerBottom.adapter = bottomSliderImageAdapter
            }
        }
    }

}