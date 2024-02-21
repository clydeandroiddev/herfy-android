package com.herfy.menu.view

import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.R
import com.herfy.menu.adapter.MenuCategoryAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.menu.databinding.FragmentMenuBinding
import com.herfy.menu.viewmodel.MenuViewModel
import com.herfy.menu.widgets.CenterLayoutManager
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.smoothSnapToPosition
import com.herfy.utils.helper.CacheUtilHelper
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MenuFragment : BaseFragment<MenuViewModel, FragmentMenuBinding>() {

    override val viewModel: MenuViewModel by viewModels()
    override val layout: Int = R.layout.fragment_menu
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private var sliderHandler: Handler? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }

    private val menuCategoryAdapter: MenuCategoryAdapter by lazy {
        MenuCategoryAdapter { position, menuCategory ->
            binding.rvMenuCategories.smoothScrollToPosition(position)
            menuCategoryAdapter.select(position)
            viewModel.menuCategoryPositions[menuCategory.id]?.let {
                binding.rvMenuCategoriesItems.smoothSnapToPosition(it)
            }
        }
    }

    private val menuCategoryItemAdapter: MenuCategoryItemAdapter by lazy {
        MenuCategoryItemAdapter(
            onSelect = { menuCategory, menuItem ->

            },
            onAdd = { menuCategory, menuItem ->

            }
        )
    }

    override fun initViews() {
        super.initViews()

        viewModel.getMenuCategoriesItems()

        activity?.let {
            binding.apply {
                sliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                val displayMetrics = DisplayMetrics()
                it.windowManager.defaultDisplay.getMetrics(displayMetrics)
                val sliderHeight = (displayMetrics.widthPixels / 2.0).toInt()
                containerSlider.layoutParams.height = sliderHeight
                sliderViewPager.adapter = sliderImageAdapter

                rvMenuCategories.apply {
                    layoutManager =
                        CenterLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
                    val snapHelper = LinearSnapHelper()
                    snapHelper.attachToRecyclerView(this)
                    adapter = menuCategoryAdapter
                }

                rvMenuCategoriesItems.apply {
                    layoutManager = LinearLayoutManager(it)
                    adapter = menuCategoryItemAdapter
                }
            }
        }

        viewModel.getSlides()
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            cacheUtilHelper?.let { cacheHelper ->

            }
        }
    }

    override fun observe() {
        super.observe()
        viewModel.slides.observeNonNull(this) { slides ->
            viewModel.getMenuCategories()
            binding.apply {
                if (slides.size > 0) {
                    sliderViewPager.visibility = View.VISIBLE
                    sliderImageAdapter.updateImageList(slides)
                    if (slides.size > 1) {
                        dotsIndicator.setViewPager2(sliderViewPager)
                        dotsIndicator.visibility = View.VISIBLE
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
                    sliderViewPager.visibility = View.VISIBLE
                    dotsIndicator.visibility = View.GONE
                }
            }
        }

        viewModel.showMenuCategories.observeNonNull(this) { menuCategories ->
            menuCategoryAdapter.addData(menuCategories)
            binding.layoutSearch.visibility = View.VISIBLE
        }

        viewModel.menuAdapterItems.observeNonNull(this) { menuAdapterItems ->
            menuCategoryItemAdapter.addData(menuAdapterItems)
        }

    }

}