package com.herfy.home.views

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.herfy.home.R
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.home.databinding.FragmentHomeBinding
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.NavigationUtils
import com.herfy.utils.helper.ViewUtil
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    override val viewModel: HomeViewModel by viewModels()
    override val layout: Int = R.layout.fragment_home
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private var sliderHandler : Handler? = null
    private var promotionDetailBottomSheet : PromotionDetailBottomSheet? = null
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
            setupPromotions()
            setupCustomerLoyalty("customerId")
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            cacheUtilHelper?.let {cacheHelper ->
                layoutPromotions.tvPromotions.text = cacheHelper.getTranslations(Text.PROMOS, "PROMOTIONS")
                layoutCatering.apply {
                    tvCatering.text = cacheHelper.getTranslations(Text.CATER_WITH_HERFY, "Cater with Herfy")
                    tvCaterTitle.text = cacheHelper.getTranslations(Text.CATER_WITH_US, "Cater with us for your next event.")
                    tvCaterDescription.text = cacheHelper.getTranslations(Text.CATER_WITH_HERFY_DESC, "We make sure that your event be the best and in the best budget. More text to tell about catering.")
                    btnCatering.text = cacheHelper.getTranslations(Text.ORDER_CATERING, "ORDER CATERING")
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
        viewModel.slideList.observeNonNull(this){slides ->
            binding.apply {
                layoutBanner.apply {
                    newMenuBannerShimmer.visibility = View.GONE
                    emptySlider.setBackgroundResource(R.color.white)
                    if(slides.size > 0){
                        emptySlider.visibility = View.GONE
                        sliderViewPager.visibility = View.VISIBLE
                        sliderImageAdapter.updateImageList(slides)
                        if(slides.size > 1){
                            dotsIndicator.setViewPager2(sliderViewPager)
                        }else {
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
                                },3000)
                            }
                        }
                    }else {
                        emptySlider.visibility = View.VISIBLE
                        sliderViewPager.visibility = View.VISIBLE
                        dotsIndicator.visibility = View.GONE
                        properSliderHeights()
                    }
                }
            }
        }

        viewModel.promotionsList.observeNonNull(this){promos ->
            binding.apply {
                layoutPromotions.apply {
                    placeholderPromotions.clPlaceholderPromo.visibility = View.GONE
                    if(promos.size > 0){
                        grpPromo.visibility = View.VISIBLE
                        promotionsAdapter.updatePromoList(promos)
                    }else {
                        clPromotions.visibility = View.GONE
                        grpPromo.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.showError.observeNonNull(this){
            showAlertDialog(title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"), message = it.message)
        }

        viewModel.showLink.observeNonNull(this){
            it.getContentIfNotHandled()?.let {
                try {
                    val activity = activity
                    if (activity != null) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = it
                        activity.startActivity(intent)
                        activity.overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        viewModel.showMenuItem.observeNonNull(this){
            it.getContentIfNotHandled()?.let { pair->
                activity?.let {
                    it.runOnUiThread {
                        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
                        //fromMenuItem = true
                        val intent = Intent()
                        intent.setClassName(it, Navigation.NAVIGATION_ACTIVITY)
                        intent.putExtra("navigation", "nav_menu_item")
                        intent.putExtra("menu_category_id", pair.first)
                        intent.putExtra("menu_item_id", pair.second)
                        intent.putExtra("cart_menu_item_position", -1)
                        it.startActivity(intent)
                    }
                }
            }
        }
        viewModel.showMenuCategory.observeNonNull(this){
            it.getContentIfNotHandled()?.let {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
                val request = NavDeepLinkRequest.Builder
                    .fromUri(Uri.parse("android-app://com.herfy/nav_menu/${it.second.id}"))
                    .build()
                NavigationUtils.navigateSafe(findNavController(),request)
            }
        }

        viewModel.showPromo.observeNonNull(this){
            it.getContentIfNotHandled()?.let {
                promotionDetailBottomSheet = PromotionDetailBottomSheet.newInstance(it)
                promotionDetailBottomSheet?.show(childFragmentManager, PromotionDetailBottomSheet::class.java.simpleName)
            }
        }
    }

    private fun properSliderHeights() {
        binding.apply {
            layoutBanner.apply {
                try {
                    activity?.let {act->
                        val sliderHeight = act.resources.getDimensionPixelSize(R.dimen.pixel_twenty_five)
                        cacheUtilHelper?.getApplication()?.let {
                            if(it.attributes.settings.categoryDisplay.equals(Values.IMAGE_TEXT, ignoreCase = true)){
                                mainToolbar.layoutParams.height =  sliderHeight + (act.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider) - act.resources.getDimensionPixelSize(R.dimen.seventy_dp))
                            }else {
                                mainToolbar.layoutParams.height = sliderHeight + act.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider)
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

    private fun setupPromotions(){
        context?.let { ctx ->
            viewModel.getPromotions()
            activity?.let{
                val itemHeight = (ViewUtil.getScreenWidth(it) / 1.5).toInt()
                promotionsAdapter = PromotionsAdapter(promoListener = viewModel,itemWidth = 0, itemHeight =  itemHeight)
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

    private fun setupCustomerLoyalty(id: String){
        context?.let { ctx ->
            viewModel.getCustomerLoyalty(id)
            binding.apply {
                layoutLoyalty.apply {
                    tvPoints.text = viewModel.loyalty.value?.attributes?.pointsAccumulated.toString()
                    tierName.text = viewModel.loyalty.value?.attributes?.loyaltyTier.toString()
                    progress.max = 100
                    progress.progress = viewModel.loyalty.value?.attributes?.currentBalance ?: 0
                }
            }
        }
    }

}