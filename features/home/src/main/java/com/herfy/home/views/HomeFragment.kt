package com.herfy.home.views

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.view.isVisible
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.herfy.common.getPromisedMinutes
import com.herfy.home.R
import com.herfy.home.adapter.PromotionCategoryAdapter
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.home.databinding.FragmentHomeBinding
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.loadSliderImage
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.runOnMain
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
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern


@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    override val viewModel: HomeViewModel by viewModels()
    override val layout: Int = R.layout.fragment_home
    private var orderType = ""
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private lateinit var promotionCategoryAdapter: PromotionCategoryAdapter
    private var sliderHandler: Handler? = null
    private lateinit var bottomSliderImageAdapter: SliderImageAdapter
    private var bottomSliderhandler: Handler? = null
    private var promotionDetailBottomSheet: PromotionDetailBottomSheet? = null
    private var countDownTimer: CountDownTimer? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }

    override fun initViews() {
        super.initViews()
        viewModel.apply {
            getCatering()
            getMenuCategory()
            getOrderTypes()
        }
        binding.apply {
            showPopup()
            setupSlider()
            setupCustomerLoyalty()
            setupPromoCategory()
            setupPromotions()
            setupCaterWithHerfy()
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
        }
    }

    private fun showPopup() {
        CacheUtil.getInstance().popup?.let {
            Glide.with(requireContext()).load(CacheUtil.getInstance().popup.imageUri).preload()
            setFragmentResultListener(Values.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                clearFragmentResultListener(Values.ARGS_KEY_FRAGMENT_RESULT)
                val actionValue = bundle.getString(Values.ARGS_KEY_ACTION_VALUE) ?: ""
                if (actionValue.isNotEmpty()) {
                    when (bundle.getString(Values.ARGS_KEY_ACTION_TYPE)) {
                        "url" -> {
                            try {
                                val activity = activity
                                if (activity != null) {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse(actionValue)
                                    activity.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        "category" -> {
                            findNavController().navigateToDeepLink("nav_menu/$actionValue")
                        }

                        "item", "product" -> viewModel.getMenuItemWith(actionValue)
                    }
                }

            }
            findNavController().navigateToDeepLink("nav_dialog_popup")
        }
    }

    override fun onResume() {
        super.onResume()
        updateGreetings()
        updateCustomerLoyalty()
        setupPromotions()
        setupOrdertracker()
    }

    private fun updateCustomerLoyalty() {
        val containsLoyalty =
            CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes().contains(
                PaymentType.LOYALTY
            ) || CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                .contains(PaymentType.LOYALTY)
        if (containsLoyalty) {
            val isSignedIn = AuthUtil.getInstance().isSignedIn
            val loyalty = AuthUtil.getInstance().customerLoyalty
            if (isSignedIn) {
                binding.layoutLoyalty.apply {
                    clCustomerLoyalty.visible()
                    cvShimmerLoyalty.visible()
                    cvLoyaltyDetails.gone()
                    cvEmptyState.gone()
                }
                viewModel.getLoyalty()
            } else {
                binding.layoutLoyalty.apply {
                    clCustomerLoyalty.visible()
                    cvShimmerLoyalty.gone()
                    cvLoyaltyDetails.gone()
                    cvEmptyState.visible()
                }
            }
        } else {
            binding.layoutLoyalty.clCustomerLoyalty.gone()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateGreetings() {
        binding.apply {
            val authUtil = AuthUtil.getInstance()
            if (authUtil.isSignedIn && authUtil.customer != null) {
                tvHomeGreetings.text = TimeUtil.getGreetings(authUtil.customer.attributes.firstName)
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
                    tvAshabHerfy.text = cacheHelper.getTranslations(Text.ASHAB_HERFY, "Ashab Herfy")
                    tvTotalPointsLabel.text =
                        cacheHelper.getTranslations(Text.TOTAL_POINTS, "Total Points")
                    tvSarValueLabel.text =
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
                    btnContinue.text = cacheHelper.getTranslations(Text.JOIN_LABEL, "Join")
                }
            }
        }
    }

    private fun setupSlider() {
        activity?.let {
            viewModel.getSliders()
            binding.apply {
                sliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                val sliderHeight =
                    (Resources.getSystem().displayMetrics.widthPixels / (Values.SLIDER_HOME_TOP_ASPECT_RATIO_WIDTH / Values.SLIDER_HOME_TOP_ASPECT_RATIO_HEIGHT)).toInt()
                layoutBanner.apply {
                    mainToolbar.layoutParams.height = sliderHeight
                    sliderViewPager.layoutParams.height = sliderHeight
                    newMenuBannerShimmer.layoutParams.height = sliderHeight
                    sliderViewPager.adapter = sliderImageAdapter
                }
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

        viewModel.caterList.observeNonNull(this) { slides ->
            binding.apply {
                layoutBanner.apply {
                    if (slides.size > 0) {
                        loadSliderImage(binding.layoutCatering.ivCater, slides[0].imageUri)
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
            it.getContentIfNotHandled()?.let { error ->
                if (error.message.isNotEmpty()) {
                    dismissProgressDialog()
                    showAlertDialog(
                        title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"),
                        message = error.message
                    )
                }
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
                        findNavController().navigateToDeepLink("nav_menu_item_detail/${pair.first}/${pair.second}")
                    }
                }
            }
        }
        viewModel.showMenuCategory.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuCategoryId ->
                findNavController().navigateToDeepLink("nav_menu/$menuCategoryId")
            }
        }
        viewModel.showMenu.observeNonNull(this) {
            it.getContentIfNotHandled()?.let {
                promotionDetailBottomSheet?.dismissAllowingStateLoss()
                findNavController().navigateToDeepLink("nav_menu")
            }
        }

        viewModel.showOrderTypeFromCampaign.observeNonNull(this) {
            it.getContentIfNotHandled()?.let {
                promotionDetailBottomSheet?.dismissAllowingStateLoss()
                showOrderTypeFromCampaign()
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
                            var hasPickup = false
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
                                } else if (!hasPickup && (orderType === OrderType.PICKUP || orderType === OrderType.CURBSIDE)) {
                                    hasPickup = true
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
                setupOrderType()
            }
        }

        viewModel.loyalty.observe(this) {
            it.getContentIfNotHandled()?.let { loyalty ->
                binding.layoutLoyalty.apply {
                    loyalty.let {
                        tvTotalPoints.text =
                            StringUtil.localize(loyalty.attributes.currentBalance.toString())
                        val currentBalanceValue =
                            String.format("%.2f", loyalty.attributes.currentBalanceAmount)
                        tvSarValue.text = StringUtil.localize(currentBalanceValue)
                        var loyaltyTier = loyalty.attributes.loyaltyTier?.name ?: ""
                        val tierImageUrl = loyalty.attributes.loyaltyTier?.imageUrl ?: ""
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
                                    "Diamond", true
                                ) || loyaltyTier == "البلاتين"
                            ) {
                                tierBackground = R.drawable.background_diamond_tier
                            }
                            tvTier.setBackgroundResource(tierBackground)
                            tvTier.text = loyaltyTier
                            tvProgressTier.text = loyaltyTier
                        }

                        if (tierImageUrl.isNotEmpty()) {
                            binding.layoutLoyalty.ivLoyaltyCard.apply {
                                this@HomeFragment.context?.let {
                                    Glide.with(it).load(tierImageUrl).skipMemoryCache(false)
                                        .apply(RequestOptions.noTransformation())
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(object : DrawableImageViewTarget(this) {
                                            override fun setResource(resource: Drawable?) {
                                                super.setResource(resource)
                                                binding.layoutLoyalty.apply {
                                                    if (resource == null) {
                                                        ivLoyaltyCard.setImageResource(R.drawable.background_loyalty_card)
                                                    }
                                                    Handler(Looper.getMainLooper()).postDelayed(
                                                        {
                                                            cvEmptyState.gone()
                                                            cvShimmerLoyalty.gone()
                                                            cvLoyaltyDetails.visible()
                                                        }, 500
                                                    )
                                                }

                                            }
                                        })
                                }
                            }
                        } else {
                            binding.layoutLoyalty.apply {
                                ivLoyaltyCard.setImageResource(R.drawable.background_loyalty_card)
                                cvEmptyState.gone()
                                cvShimmerLoyalty.gone()
                                cvLoyaltyDetails.visible()
                            }
                        }
                    }
                }
            }
        }

        viewModel.tierProgress.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { tierProgress ->
                AuthUtil.getInstance().setCustomerLoyaltyTier(tierProgress.nextTier)
                CacheUtil.getInstance()
                    .setTotalAccumulatedPoints(tierProgress.totalPointsAccumulated)
                CacheUtil.getInstance().setPointsUntilNextTier(tierProgress.pointsToNextTier)
                CacheUtil.getInstance().setStartingPointsOfNextTier(tierProgress.startPoints)
                binding.layoutLoyalty.apply {
                    if (tierProgress.pointsToNextTier <= 0) {
                        val tierName = tierProgress.loyalty?.attributes?.loyaltyTier?.name ?: ""
                        if (tierProgress.isMaxTier) {
                            progress.max = 100
                            progress.progress = 100
                            tvTierProgressDesc.text = CacheUtil.getInstance().getTranslations(
                                Text.CONGRATS_YOUVE_REACHED_X, "Congrats! You’ve reached (x)"
                            ).replace("(x)", tierName)
                        }
                    } else {
                        var loyaltyTier = tierProgress.nextTier?.name ?: ""
                        if (!loyaltyTier.isNullOrEmpty() && LocaleUtil.getInstance().isEnglish) {
                            loyaltyTier = loyaltyTier.lowercase().replaceFirstChar(Char::titlecase)
                        }
                        progress.max = tierProgress.startPoints
                        progress.progress = tierProgress.totalPointsAccumulated
                        cacheUtilHelper?.let {
                            tvTierProgressDesc.text = it.getTranslations(
                                Text.POINTS_UNTIL_NEXT_TIER, "(x) points until (y)"
                            ).replace("(x)", tierProgress.pointsToNextTier.toString())
                                .replace("(y)", loyaltyTier)
                        }

                        progressTextIndicator.text = "${progress.progress}/${progress.max}"
                    }
                }
            }
        }

        viewModel.tiers.observeNonNull(this) {
            it.getContentIfNotHandled().let { tiers ->
                tiers?.forEach { tier ->
                    Glide.with(this@HomeFragment).load(tier.imageUrl).preload()
                }
            }
        }

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
                orderType = orderDetails.orderType
                binding.layoutBanner.orderTracker.apply {
                    if (orderDetails.orderType.equals(
                            OrderType.convertOrderTypeToString(OrderType.DELIVERY),
                            ignoreCase = true
                        ) || orderDetails.orderType.equals(
                            "deliver", ignoreCase = true
                        )
                    ) {
                        tvOrderTypeLabel.text = CacheUtil.getInstance().getTranslations(
                            Text.YOUR_DELIVERY_ORDER, "Your delivery order from Herfy"
                        )
                        if (orderDetails.orderStatus.sequence <= OrderStatusType.DISPATCHED.value) {
                            updateStatusGIF(R.drawable.make_order)
                        } else if (orderDetails.orderStatus.sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value && orderDetails.orderStatus.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                            updateStatusGIF(R.drawable.delivered_order)
                        } else {
                            updateStatusGIF(R.drawable.delivered_order)
                        }
                    } else {
                        tvOrderTypeLabel.text = CacheUtil.getInstance().getTranslations(
                            Text.YOUR_ORDER, "Your order from Herfy"
                        )
                        if (orderDetails.orderStatus.sequence <= OrderStatusType.IN_KITCHEN.value) {
                            updateStatusGIF(R.drawable.make_order)
                        } else if (orderDetails.orderStatus.sequence >= OrderStatusType.READY.value && orderDetails.orderStatus.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                            updateStatusGIF(R.drawable.collected_order)
                        } else {
                            updateStatusGIF(R.drawable.collected_order)
                        }
                    }

                    clOrderNotif.visibility = View.VISIBLE
                    tvStatusLabel.text = orderDetails.orderStatus.description

                    val promisedTime = orderDetails.promisedTime.getPromisedMinutes()

                    if (promisedTime <= 0) {
                        tvEstimateValue.gone()
                        tvEstimateLabel.gone()
                    } else {
                        tvEstimateValue.visible()
                        tvEstimateLabel.visible()
                        tvEstimateValue.text =
                            CacheUtil.getInstance().getTranslations(Text.MIN, "(x) mins")
                                .replace("(x)", promisedTime.toString())
                        tvEstimateLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.ESTIMATED, "(Estimated)")
                        initTimer(orderDetails.promisedTime)
                    }

                    otherDetails.setOnClickListener {
                        findNavController().navigateToDeepLink("nav_new_order_tracker/false/${orderDetails.id}")
                    }
                }
            }
        }

        viewModel.updateOrderTrackerNotif.observe(this) { orderData ->
            binding.apply {
                if (orderData.orderStatus.sequence < OrderStatusType.FINISHED.value) {
                    layoutBanner.orderTracker.tvStatusLabel.text = orderData.orderStatus.description
                } else if (orderData.orderStatus.sequence == OrderStatusType.FINISHED.value) {
                    binding.layoutBanner.orderTracker.tvEstimateValue.gone()
                    binding.layoutBanner.orderTracker.tvEstimateLabel.gone()
                    binding.layoutBanner.orderTracker.tvStatusLabel.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.THANK_YOU_ORDERING, "Thank you for ordering at Herfy mobile app."
                        )
                    binding.layoutBanner.orderTracker.tvOrderTypeLabel.text =
                        CacheUtil.getInstance()
                            .getTranslations(Text.ORDER_COMPLETED, "Order completed!.")
                    updateCustomerLoyalty()
                } else {
                    binding.layoutBanner.orderTracker.tvEstimateValue.gone()
                    binding.layoutBanner.orderTracker.tvEstimateLabel.gone()
                    layoutBanner.orderTracker.clOrderNotif.visibility = View.GONE
                }

                if (orderType.equals(
                        OrderType.convertOrderTypeToString(OrderType.DELIVERY), ignoreCase = true
                    ) || orderType.equals(
                        "deliver", ignoreCase = true
                    )
                ) {
                    if (orderData.orderStatus.sequence <= OrderStatusType.DISPATCHED.value) {
                        updateStatusGIF(R.drawable.make_order)
                    } else if (orderData.orderStatus.sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value && orderData.orderStatus.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                        updateStatusGIF(R.drawable.delivered_order)
                    } else if (orderData.orderStatus.sequence == OrderStatusType.FINISHED.value) {
                        removeTimer()
                        binding.layoutBanner.orderTracker.tvEstimateValue.isVisible = false
                        binding.layoutBanner.orderTracker.ivOrderNotif.setImageResource(R.drawable.ic_full_order_done)
                    } else {
                        updateStatusGIF(R.drawable.delivered_order)
                    }
                } else {
                    if (orderData.orderStatus.sequence <= OrderStatusType.IN_KITCHEN.value) {
                        updateStatusGIF(R.drawable.make_order)
                    } else if (orderData.orderStatus.sequence >= OrderStatusType.READY.value && orderData.orderStatus.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                        updateStatusGIF(R.drawable.collected_order)
                    } else if (orderData.orderStatus.sequence == OrderStatusType.FINISHED.value) {
                        binding.layoutBanner.orderTracker.ivOrderNotif.setImageResource(R.drawable.ic_full_order_done)
                    } else {
                        updateStatusGIF(R.drawable.collected_order)
                    }
                }
            }
        }

        viewModel.catering.observe(this) {
            it.getContentIfNotHandled()?.let { catering ->
                if (catering.isNotEmpty()) {
                    binding.layoutCatering.btnCatering.setOnClickListener {
                        activity?.apply {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW, Uri.parse(catering)
                                )
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        viewModel.showPromotionsError.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { error ->
                runOnMain {
                    if (error.message.isNotEmpty()) {
                        promotionDetailBottomSheet?.dismissLoading()
                        showAlertDialog(
                            title = "", message = error.message
                        )
                    }
                }
            }
        }

        viewModel.showMenuItemCombo.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { pair ->
                runOnMain {
                    findNavController().navigateToDeepLink("nav_combo/${pair.first}/${pair.second}")
                }
            }
        }
    }

    private fun removeTimer() {
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
        }
    }

    private fun checkEstimatedTime(promisedTime: String) {
        val minutes = promisedTime.getPromisedMinutes()
        binding.layoutBanner.orderTracker.apply {
            if (minutes <= 0) {
                tvEstimateValue.gone()
                tvEstimateLabel.gone()
            } else {
                tvEstimateValue.text = CacheUtil.getInstance().getTranslations(Text.MIN, "(x) mins")
                    .replace("(x)", minutes.toString())
                tvEstimateLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.ESTIMATED, "(Estimated)")
            }
        }
    }

    private fun initTimer(promisedTime: String) {
        removeTimer()
        val minutes = promisedTime.getPromisedMinutes()
        val countDownTime = minutes * 1000
        if (minutes > 0) {
            countDownTimer = object : CountDownTimer(countDownTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    checkEstimatedTime(promisedTime)
                }

                override fun onFinish() {}
            }
            countDownTimer?.start()
        }
    }

    private fun updateStatusGIF(image: Int) {
        try {
            activity?.let {
                Glide.with(it).asGif().load(image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.layoutBanner.orderTracker.ivOrderNotif)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                intent.putExtra("fromCampaign", false)
                intent.putExtra("isBothType", false)
                intent.putExtra("checkout", false)
                intent.putExtra("storeId", storeId)
                it.startActivity(intent)
            }
        }
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
            binding.layoutBanner.orderTracker.clOrderNotif.visibility = View.GONE
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
            val denominator = viewModel.orderTypes.value?.size ?: 0
            if (denominator > 0) {
                val itemHeight = if (denominator >= Values.MAX_ORDERTYPE_COUNT) {
                    (Resources.getSystem().displayMetrics.widthPixels / 3) - ViewUtil.dpToPx(
                        it, Values.TOTAL_ORDERTYPE_SPACING
                    ) * (Values.THREE_ORDERTYPE_ASPECT_RATIO_HEIGHT / Values.THREE_ORDERTYPE_ASPECT_RATIO_WIDTH)
                } else {
                    ((Resources.getSystem().displayMetrics.widthPixels / 2) - ViewUtil.dpToPx(
                        it, Values.TOTAL_ORDERTYPE_SPACING
                    )) * (Values.TWO_ORDERTYPE_ASPECT_RATIO_HEIGHT / Values.TWO_ORDERTYPE_ASPECT_RATIO_WIDTH)
                }
                binding.layoutOrderTypeMain.apply {
                    deliveryType.layoutParams.height = itemHeight.toInt()
                    pickupType.layoutParams.height = itemHeight.toInt()
                    dineType.layoutParams.height = itemHeight.toInt()
                }
            }
        }
    }

    private fun setupCustomerLoyalty() {
        context?.let {
            val imageHeight =
                (Resources.getSystem().displayMetrics.widthPixels / Values.LOYALTY_CARD_ASPECT_RATIO)
            val imageEmptyLoyalty =
                (Resources.getSystem().displayMetrics.widthPixels / Values.LOYALTY_EMPTY_CARD_ASPECT_RATION)
            binding.layoutLoyalty.apply {
                progress.isEnabled = false
                ivLoyaltyCard.apply {
                    layoutParams.height = imageHeight.toInt()
                }
                clLoyaltyEmptyState.apply {
                    layoutParams.height = imageEmptyLoyalty.toInt()
                }
                clDetailsLoyaltyContainer.clipToOutline = true
                clDetailsLoyaltyContainer.outlineProvider = ViewOutlineProvider.BACKGROUND
                ivLoyaltyCard.apply {
                    clipToOutline = true
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                }
            }
        }
    }

    private fun setupCaterWithHerfy() {
        viewModel.getCaterSlider()
        activity?.let {
            binding.apply {
                val sliderHeight =
                    (Resources.getSystem().displayMetrics.widthPixels / (Values.SLIDER_HOME_BOTTOM_ASPECT_RATIO_WIDTH / Values.SLIDER_HOME_BOTTOM_ASPECT_RATIO_HEIGHT)).toInt()
                layoutCatering.apply {
                    cvCateringContent.layoutParams.height = sliderHeight
                    ivCater.layoutParams.height = sliderHeight
                    ivCater.scaleX = 1f
//                    if (LocaleUtil.getInstance().isArabic) {
//                        ivCater.scaleX = -1f
//                    } else {
//                        ivCater.scaleX = 1f
//                    }
                }

            }
        }
    }

    private fun setupBottomSlider() {
        activity?.let {
            viewModel.getBottomSliders()
            binding.apply {
                bottomSliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                layoutBottomBanner.apply {
                    val sliderHeight =
                        (Resources.getSystem().displayMetrics.widthPixels / (Values.SLIDER_HOME_BOTTOM_ASPECT_RATIO_WIDTH / Values.SLIDER_HOME_BOTTOM_ASPECT_RATIO_HEIGHT)).toInt()
                    layoutBottomBanner.apply {
                        cvBannerBottom.layoutParams.height = sliderHeight
                        shimmerBannerBottom.layoutParams.height = sliderHeight
                    }
                    vpBannerBottom.adapter = bottomSliderImageAdapter
                }
            }
        }
    }

}