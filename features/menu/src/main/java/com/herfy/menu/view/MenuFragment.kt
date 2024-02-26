package com.herfy.menu.view

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.R
import com.herfy.menu.adapter.MenuCategoryAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.menu.databinding.FragmentMenuBinding
import com.herfy.menu.viewmodel.MenuViewModel
import com.herfy.menu.widgets.CenterLayoutManager
import com.herfy.utils.base.mvvm.BaseActivity
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.smoothSnapToPosition
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
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
        MenuCategoryItemAdapter(onSelect = { menuCategory, menuItem ->
            navMenuItem(menuCategory, menuItem)
        }, onFavorite = { menuItem ->
            viewModel.addRemoveFavorite(menuItem)
        }, onAdd = { menuCategory, menuItem ->
            if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                navMenuItem(menuCategory, menuItem)
            } else {
                viewModel.getMenuItem(menuCategory, menuItem)
            }
        })
    }

    override fun initViews() {
        super.initViews()

        activity?.let {
            binding.apply {

                orderTypeToolbar.setNavButton {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }

                orderTypeToolbar.setOrderTypeAction {
                    navToOrderTypePage()
                }

                sliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                val sliderHeight = (Resources.getSystem().displayMetrics.widthPixels / 2.0).toInt()
                containerSlider.layoutParams.height = sliderHeight
                shimmerSlider.root.layoutParams.height = sliderHeight
                sliderViewPager.adapter = sliderImageAdapter

                etSearch.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                    }

                    override fun afterTextChanged(p0: Editable?) {
                        p0?.let {
                            if (it.isNotEmpty()) {
                                rvMenuCategories.gone()
                                viewMenu.gone()
                                viewSearch.visible()
                                toolbar.layoutParams.height =
                                    resources.getDimensionPixelSize(R.dimen.toolbar_height_search)
                                viewModel.searchMenuItems(it.toString())
                                btnClear.visible()
                            } else {
                                rvMenuCategories.visible()
                                viewMenu.visible()
                                viewSearch.gone()
                                toolbar.layoutParams.height =
                                    resources.getDimensionPixelSize(R.dimen.toolbar_height_menu)
                                viewModel.menuAdapterItems.value?.let { menuAdapterItems ->
                                    menuCategoryItemAdapter.addData(menuAdapterItems)
                                }
                                btnClear.gone()
                            }
                        }
                    }

                })

                btnClear.setOnClickListener {
                    etSearch.setText("")
                }

                rvMenuCategories.apply {
                    layoutManager = CenterLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
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

        viewModel.apply {
            getSlides()
            getMenuCategoriesItems()
        }
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
            binding.apply {
                if (slides.size > 0) {
                    sliderViewPager.visible()
                    sliderImageAdapter.updateImageList(slides)
                    dotsIndicator.apply {
                        if (slides.size > 1) {
                            setViewPager2(sliderViewPager)
                            visible()
                        } else {
                            gone()
                        }
                    }
                    sliderHandler?.removeCallbacksAndMessages(null)
                    sliderHandler = Handler(Looper.getMainLooper())
                    sliderHandler?.apply {
                        postDelayed(object : Runnable {
                            override fun run() {
                                var pos = sliderViewPager.currentItem + 1
                                if (pos >= slides.size) {
                                    pos = 0
                                    sliderViewPager.setCurrentItem(pos, false)
                                } else {
                                    sliderViewPager.currentItem = pos
                                }
                                postDelayed(this, Values.SLIDER_INTERVAL)
                            }
                        }, Values.SLIDER_INTERVAL)
                    }
                } else {
                    sliderViewPager.visible()
                    dotsIndicator.gone()
                }
                shimmerSlider.root.gone()
            }
        }

        viewModel.showMenuCategories.observeNonNull(this) { menuCategories ->
            binding.shimmerMenuCategories.root.gone()
            menuCategoryAdapter.addData(menuCategories)
            binding.layoutSearch.visible()
        }

        viewModel.menuAdapterItems.observeNonNull(this) { menuAdapterItems ->
            binding.shimmerMenuItems.root.gone()
            menuCategoryItemAdapter.addData(menuAdapterItems)
        }

        viewModel.searchMenuAdapterItems.observeNonNull(this) { searchMenuAdapterItems ->
            menuCategoryItemAdapter.addData(searchMenuAdapterItems)
        }

        viewModel.showMenuItem.observeNonNull(this) {
            navMenuItem(it.first, it.second)
        }

        viewModel.showAddToCart.observeNonNull(this) {
            activity?.cast<BaseActivity<*, *>> {
                it.runOnUiThread {
                    val vibrator: Vibrator =
                        it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500, VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(100)
                    }
                    Toast.makeText(
                        it,
                        CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.orderTypeToolbar.setCart(CacheUtil.getInstance().cartQuantity)
                }
            }
        }

        viewModel.showMessage.observeNonNull(this) { message ->
            activity?.cast<BaseActivity<*, *>> {
                it.runOnUiThread {
                    Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.showError.observeNonNull(this) { error ->
            activity?.cast<BaseActivity<*, *>> {
                it.runOnUiThread {
                    it.showAlertDialog("", error)
                }
            }
        }

        viewModel.showLoading.observeNonNull(this) { showLoading ->
            activity?.cast<BaseActivity<*, *>> {
                it.runOnUiThread {
                    if (showLoading) {
                        it.showProgressDialog(false)
                    } else {
                        it.dismissProgressDialog()
                    }
                }
            }
        }

    }

    private fun navToOrderTypePage() {
        val storeId = CacheUtil.getOrderStoreId() ?: ""
        val request = NavDeepLinkRequest.Builder.fromUri(
            Uri.parse(
                "${Values.NAV_DEEP_LINK_BASE_URL}nav_order_type/$storeId"
            )
        ).build()
        NavigationUtils.navigateSafe(findNavController(), request)
    }

    private fun navMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {
        val request = NavDeepLinkRequest.Builder.fromUri(
            Uri.parse(
                "${Values.NAV_DEEP_LINK_BASE_URL}nav_menu_item/${menuCategory.id}/${menuItem.id}"
            )
        ).build()
        NavigationUtils.navigateSafe(findNavController(), request)
    }

}