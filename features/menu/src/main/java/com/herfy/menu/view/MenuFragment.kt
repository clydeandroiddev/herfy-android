package com.herfy.menu.view

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.R
import com.herfy.menu.adapter.MenuCategoryAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.menu.databinding.FragmentMenuBinding
import com.herfy.menu.viewmodel.MenuViewModel
import com.herfy.menu.widgets.CenterLayoutManager
import com.herfy.utils.base.mvvm.BaseActivity
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.smoothSnapToPosition
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
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

    override fun onResume() {
        super.onResume()
        viewModel.getOrderType()
    }

    private val menuCategoryAdapter: MenuCategoryAdapter by lazy {
        MenuCategoryAdapter { position, menuCategory ->
            binding.rvMenuCategories.smoothScrollToPosition(position)
            menuCategoryAdapter.select(position)
            viewModel.menuCategoryPositions[menuCategory.id]?.let {
                binding.rvMenuCategoriesItems.apply {
                    smoothSnapToPosition(it)
                    removeOnScrollListener(menuOnScrollListener)
                    clearOnScrollListeners()
                    activity?.apply {
                        runOnUiThread {
                            Handler(Looper.getMainLooper()).postDelayed({
                                addOnScrollListener(menuOnScrollListener)
                            }, 250)
                        }
                    }
                }
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

    private var menuIsScrolled = false
    private val menuOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            menuIsScrolled = when(newState) {
                RecyclerView.SCROLL_STATE_IDLE -> false
                RecyclerView.SCROLL_STATE_DRAGGING -> true
                else -> true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if(menuIsScrolled && binding.rvMenuCategories.isVisible()) {
                val firstVisibleItem =
                    (binding.rvMenuCategoriesItems.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (firstVisibleItem in viewModel.menuCategoryPositions.values) {
                    viewModel.getMenuCategoryId(firstVisibleItem)?.let { menuCategoryId ->
                        menuCategoryAdapter.getPosition(menuCategoryId)?.let {
                            if(it != menuCategoryAdapter.getSelected() &&
                                it < menuCategoryAdapter.itemCount) {
                                binding.rvMenuCategories.smoothScrollToPosition(it)
                                menuCategoryAdapter.select(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun initViews() {
        super.initViews()

        activity?.let {
            binding.apply {

                orderTypeToolbar.setNavButton {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }

                orderTypeToolbar.setOrderTypeAction {
                    orderTypeClick()
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
                    addOnScrollListener(menuOnScrollListener)
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

        viewModel.orderType.observeNonNull(this) { oType ->
            var type =
                CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE")
            var address = CacheUtil.getInstance().getTranslations(
                Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE, "Please tap here to pick an order type"
            )
            var icon = resources.getDrawable(com.herfy.ordertype.R.drawable.ic_blank_type)
            when (oType) {
                OrderType.EAT_IN -> {
                    if (CacheUtil.getInstance().orderStore != null) {
                        icon =
                            resources.getDrawable(com.herfy.ordertype.R.drawable.ic_walk_in_pickup_small)
                        type = CacheUtil.getInstance()
                            .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
                        address = CacheUtil.getInstance().orderStore.attributes.name
                    }
                }

                OrderType.DRIVE_THRU -> {
                    if (CacheUtil.getInstance().orderStore != null) {
                        icon =
                            resources.getDrawable(com.herfy.ordertype.R.drawable.ic_drive_thru_small)
                        type = CacheUtil.getInstance()
                            .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                        address = CacheUtil.getInstance().orderStore.attributes.name
                    }
                }

                OrderType.PICKUP -> {
                    if (CacheUtil.getInstance().orderStore != null) {
                        icon = resources.getDrawable(com.herfy.ordertype.R.drawable.ic_pickup_small)
                        type = CacheUtil.getInstance().getTranslations(Text.PICKUP_AT, "Pickup At")
                        address = CacheUtil.getInstance().orderStore.attributes.name
                    }
                }

                OrderType.DINE_IN -> {
                    if (CacheUtil.getInstance().orderStore != null) {
                        icon =
                            resources.getDrawable(com.herfy.ordertype.R.drawable.ic_dine_in_small)
                        type = CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
                        address = CacheUtil.getInstance().orderStore.attributes.name
                    }
                }

                OrderType.DELIVERY -> {
                    icon = resources.getDrawable(com.herfy.ordertype.R.drawable.ic_delivery_small)
                    if (CacheUtil.getInstance().orderAddress != null) {
                        type = CacheUtil.getInstance().getTranslations(Text.DELIVERY, "Delivery")
                        address = CacheUtil.getInstance().orderAddress.attributes.line1
                    } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                        type = CacheUtil.getInstance().getTranslations(Text.DELIVERY, "Delivery")
                        address = CacheUtil.getInstance().orderDeliveryAddress
                    }
                }

                OrderType.CURBSIDE -> {
                    if (CacheUtil.getInstance().orderStore != null) {
                        icon =
                            resources.getDrawable(com.herfy.ordertype.R.drawable.ic_curbside_pickup_small)
                        type = CacheUtil.getInstance()
                            .getTranslations(Text.CURBSIDE_PICKUP, "Curbside Pickup")
                        address = CacheUtil.getInstance().orderStore.attributes.name
                    }
                }

                else -> {}
            }
            binding.orderTypeToolbar.setOrderType(type, address, icon)
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

    private fun orderTypeClick() {
        val type = CacheUtil.getInstance().orderType
        var orderType = OrderType.DELIVERY
        when (type) {
            OrderType.PICKUP, OrderType.EAT_IN, OrderType.DRIVE_THRU, OrderType.CURBSIDE -> {
                orderType = OrderType.PICKUP
            }

            OrderType.DELIVERY -> {
                orderType = OrderType.PICKUP
            }

            OrderType.DINE_IN -> {
                orderType = OrderType.DINE_IN
            }

            else -> {}
        }
        activity?.let {
            it.runOnUiThread {
                val intent = Intent()
                val storeId = CacheUtil.getOrderStoreId() ?: ""
                intent.setClassName(it, Navigation.NAVIGATION_ACTIVITY)
                intent.putExtra("navigation", "nav_order_type")
                intent.putExtra("selectedOrderType", orderType.value)
                intent.putExtra("fromHome", false)
                intent.putExtra("fromCart", false)
                intent.putExtra("checkout", false)
                intent.putExtra("storeId", storeId)
                it.startActivity(intent)
            }
        }
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