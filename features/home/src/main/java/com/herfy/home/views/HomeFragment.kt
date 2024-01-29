package com.herfy.home.views

import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.herfy.home.R
import com.herfy.home.adapter.PromotionsAdapter
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.home.databinding.FragmentHomeBinding
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.ViewUtil


class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    override val viewModel: HomeViewModel by viewModels()
    override val layout: Int = R.layout.fragment_home
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private lateinit var promotionsAdapter: PromotionsAdapter
    private var sliderHandler : Handler? = null
    override fun initViews() {
        super.initViews()
        binding.apply {
            setupSlider()
            setupPromotions()
        }
    }

    override fun initTranslations() {
        super.initTranslations()
    }

    private fun setupSlider() {
        activity?.let {
            binding.apply {
                sliderImageAdapter = SliderImageAdapter()
                val displayMetrics = DisplayMetrics()
                it.windowManager.defaultDisplay.getMetrics(displayMetrics)
                val sliderHeight = displayMetrics.widthPixels / 2
                layoutBanner.clSliderContainer.layoutParams.height = sliderHeight
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
                            indicatorView.setupWithViewPager(sliderViewPager)
                        }else {
                            indicatorView.visibility = View.GONE
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
                        indicatorView.visibility = View.GONE
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

        }
    }

    private fun properSliderHeights() {
       /* try {
            activity?.let {
                sliderHeight = it.resources.getDimensionPixelSize(R.dimen.pixel_twenty_five)
                if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                        Values.IMAGE_TEXT,
                        ignoreCase = true
                    )
                ) {
                    binding.mainToolbar.layoutParams.height =
                        sliderHeight + (it.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider) - it.resources.getDimensionPixelSize(
                            R.dimen.seventy_dp
                        ))
                } else {
                    binding.mainToolbar.layoutParams.height =
                        sliderHeight + it.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider)
                }
                binding.sliderViewPager.layoutParams.height = sliderHeight
                binding.emptySlider.layoutParams.height = sliderHeight
                binding.newMenuBannerShimmer.layoutParams.height = sliderHeight
                binding.mainToolbar.invalidate()
                binding.sliderViewPager.invalidate()
                binding.emptySlider.invalidate()
                binding.newMenuBannerShimmer.invalidate()
                binding.mainToolbar.visibility = View.GONE
                binding.mainToolbar.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/


    }

    private fun setupPromotions(){
        context?.let { ctx ->

            val itemWidth = (ViewUtil.getScreenWidth(activity) - ViewUtil.dpToPx(ctx, 32))
            val itemHeight = (ViewUtil.getScreenWidth(activity) - ViewUtil.dpToPx(ctx, 32))
            promotionsAdapter = PromotionsAdapter(itemWidth = itemWidth, itemHeight =  itemHeight)
            binding.apply {
                layoutPromotions.rvPromotions.apply {
                    adapter = promotionsAdapter
                    layoutManager = GridLayoutManager(ctx, 2)
                    setHasFixedSize(true)
                }
            }
        }

    }

}