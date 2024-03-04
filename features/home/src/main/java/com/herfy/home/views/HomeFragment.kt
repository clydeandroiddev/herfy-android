package com.herfy.home.views

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkRequest
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
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.setInteractionDisabled
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.NavigationUtils
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    override val viewModel: HomeViewModel by viewModels()
    override val layout: Int = R.layout.fragment_home
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private lateinit var promotionCategoryAdapter: PromotionCategoryAdapter
    private var sliderHandler: Handler? = null
    private lateinit var bottomSliderImageAdapter : SliderImageAdapter
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
            if(AuthUtil.getInstance().isSignedIn) {
                setupCustomerLoyalty()
            }else{
              layoutLoyalty.clCustomerLoyalty.visibility = View.GONE
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

                    tvCaterTitle.text  = cacheHelper.getTranslations(Text.CATER_WITH_HERFY, "Cater with Herfy"
                    )
                    tvCaterDescription.text = cacheHelper.getTranslations(
                        Text.CATER_WITH_HERFY_DESC,
                        "Best event. Best budget. Check out our catering!"
                    )
                    btnCatering.text =
                        cacheHelper.getTranslations(Text.ORDER_CATERING, "Order Catering")
                }
                layoutOrderTypeMain.apply {
                    if (AuthUtil.getInstance().isSignedIn) {
                        sun.visibility = View.VISIBLE
                        tvGoodMorningUser.visibility = View.VISIBLE
                        val name = AuthUtil.getInstance().customer.attributes.firstName
                        tvGoodMorningUser.text = cacheHelper.getTranslations(
                            Text.GOOD_MORNING_LABEL, "Good morning, (x)!"
                        ).replace("(x)", name)
                    } else {
                        sun.visibility = View.GONE
                        tvGoodMorningUser.visibility = View.GONE
                    }
                    tvHowWillYouLabel.text = cacheHelper.getTranslations(
                        Text.HOW_WILL_YOU_ORDER_TODAY, "How will you order today?"
                    )
                }

                layoutBottomBanner.tvBannerBottom.text = CacheUtil.getInstance().getTranslations(Text.HERFY_SUPPORT, "Herfy supports the cause")
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
                val sliderHeight = (displayMetrics.widthPixels / 1.5).toInt()
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
            if(it.message.isNotEmpty()){
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
                        val request = NavDeepLinkRequest.Builder.fromUri(
                            Uri.parse(
                                "${Values.NAV_DEEP_LINK_BASE_URL}nav_menu_item/${pair.first}/${pair.second}"
                            )
                        ).build()
                        NavigationUtils.navigateSafe(findNavController(), request)
                    }
                }
            }
        }
        viewModel.showMenuCategory.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuCategoryId ->
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
                val request =
                    NavDeepLinkRequest.Builder.fromUri(Uri.parse("android-app://com.herfy/nav_menu/$menuCategoryId"))
                        .build()
                NavigationUtils.navigateSafe(findNavController(), request)
            }
        }

        viewModel.showPromo.observeNonNull(this) {
            it.getContentIfNotHandled()?.let {promo ->
                promotionDetailBottomSheet = PromotionDetailBottomSheet.newInstance(promo, viewModel)
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

        viewModel.loyalty.observe(this){ loyalty ->

            binding.layoutLoyalty.apply {
                loyalty?.let {
                    clCustomerLoyalty.visibility = View.VISIBLE
                    tvPoints.text = loyalty.attributes.pointsAccumulated.toString()
                    tierName.text = loyalty.attributes.loyaltyTier?.name.toString()
                }?: run {
                    clCustomerLoyalty.visibility = View.GONE
                }
            }
        }

        viewModel.pointsToNextTier.observeNonNull(this){pointsToNextTier->
            if(pointsToNextTier<= 0){
                viewModel.tier.observeNonNull(this){tier->
                    if(tier==null){
                        binding.layoutLoyalty.apply {
                            progress.max = 100
                            progress.progress = 0
                            progressLabel.text = "You are now a Tier member"
                            progressTextIndicator.text = "${progress.progress}/${ progress.max}"
                        }
                    }else{
                        binding.layoutLoyalty.apply {
                            progress.max = 100
                            progress.progress = 100
                            progressLabel.text = CacheUtil.getInstance().getTranslations(
                                Text.TOTAL_POINTS_MAX_OUT,
                                "You have maxed out on our tier levels"
                            )
                            progressTextIndicator.text = "${progress.progress}/${ progress.max}"
                        }
                    }

                }
            }else{
                viewModel.startPoints.observeNonNull(this){startPoints->
                    binding.layoutLoyalty.apply {
                        progress.max = startPoints
                  }

                    viewModel.totalPointsAccumulated.observeNonNull(this){totalPointsAccumulated->
                        binding.layoutLoyalty.apply {
                            progress.progress = totalPointsAccumulated
                            progressTextIndicator.text = "${progress.progress}/${ progress.max}"
                        }
                    }

                }



                viewModel.tier.observeNonNull(this) { tier ->
                    binding.layoutLoyalty.apply {
                        progressLabel.text = HtmlCompat.fromHtml(
                                CacheUtil.getInstance().getTranslations(
                                    Text.LOYALTY_POINTS_UNTIL_TIER,
                                    "(x) points till you reach (y) Tier").replace("(x)", pointsToNextTier.toString()).replace(
                                    "(y)", "<b>" + tier!!.name.substring(0, 1).uppercase(
                                        Locale.getDefault()
                                    ) + tier.name.substring(1).lowercase(Locale.getDefault())
                                ) + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                }

            }
        }

        viewModel.promotionCategoryList.observeNonNull(this){ promoCategories ->
            binding.layoutPromotions.apply {
                if(promoCategories.size > 0){
                    promotionCategoryAdapter.updateList(promoCategories)
                }
            }
        }
        viewModel.updatePromoCategoryList.observeNonNull(this){
            it.getContentIfNotHandled()?.let {selectedCategory ->
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

        viewModel.promoFilteredList.observe(this){promos ->
            binding.apply {
                layoutPromotions.apply {
                    placeholderPromotions.clPlaceholderPromo.visibility = View.GONE
                    promos?.let {
                        if(it.isNotEmpty()){
                            rvPromotions.visibility = View.VISIBLE
                            promotionsAdapter.updatePromoList(promos)
                        }else {
                            rvPromotions.visibility = View.GONE
                        }
                    }?: run {
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

        viewModel.showCart.observeNonNull(this){
            it.getContentIfNotHandled()?.let { isAvail ->
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                val request = NavDeepLinkRequest.Builder.fromUri(
                    Uri.parse(
                        "${Values.NAV_DEEP_LINK_BASE_URL}nav_cart/${isAvail}"
                    )
                ).build()
                NavigationUtils.navigateSafe(findNavController(), request)
            }
        }

        viewModel.showProgressBar.observe(this){
            activity?.window?.setInteractionDisabled(it)
            if(it){
                showProgressDialog(true)
            }else {
                dismissProgressDialog()
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
                    promoListener = viewModel, itemWidth = 0, itemHeight = itemHeight,
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

    private fun setupCustomerLoyalty(){
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