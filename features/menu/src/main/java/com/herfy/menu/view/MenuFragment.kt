package com.herfy.menu.view

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.R
import com.herfy.menu.adapter.MenuCategoryAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.menu.databinding.FragmentMenuBinding
import com.herfy.menu.viewmodel.MenuViewModel
import com.herfy.menu.widgets.CenterLayoutManager
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.event.AddToCartEvent
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.smoothSnapToPosition
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus


@AndroidEntryPoint
class MenuFragment : BaseFragment<MenuViewModel, FragmentMenuBinding>() {

    private val args: MenuFragmentArgs by navArgs()

    override val viewModel: MenuViewModel by viewModels()
    override val layout: Int = R.layout.fragment_menu
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private var sliderHandler: Handler? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }
    private var isCartEmpty = true

    private var orderTypeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.getMenuCategoriesItems()
        }

    private val menuCategoryAdapter: MenuCategoryAdapter by lazy {
        MenuCategoryAdapter { position, menuCategory ->
            selectMenuCategory(position, menuCategory.id)
        }
    }

    private val menuCategoryItemAdapter: MenuCategoryItemAdapter by lazy {
        MenuCategoryItemAdapter(onSelect = { menuCategory, menuItem ->
            navMenuItem(menuCategory.id, menuItem.id, menuItem)
        }, onFavorite = { menuItem ->
            viewModel.addRemoveFavorite(menuItem)
        }, onAdd = { menuCategory, menuItem ->
            if (menuItem.isMustBeCustomized) {
                navMenuItem(menuCategory.id, menuItem.id, menuItem)
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
                val lastVisibleItem =
                    (binding.rvMenuCategoriesItems.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (firstVisibleItem in viewModel.menuCategoryPositions.values) {
                    viewModel.getMenuCategoryId(firstVisibleItem)?.let { menuCategoryId ->
                        scrollToMenuCategory(menuCategoryId)
                    }
                } else if(menuCategoryItemAdapter.getMenuItemCategory(lastVisibleItem) != null) {
                    menuCategoryItemAdapter.getMenuItemCategory(lastVisibleItem)?.let { menuCategory ->
                        scrollToMenuCategory(menuCategory.id)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showLoading()
        setCartQuantity()
        viewModel.getOrderType()
    }

    override fun initViews() {
        super.initViews()

        activity?.let {
            binding.apply {

                orderTypeToolbar.apply {
                    setNavButton {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                    setCartButtonAction { navCart() }
                    setOrderTypeAction {
                        orderTypeClick()
                    }
                }

                sliderImageAdapter = SliderImageAdapter(sliderListener = viewModel)
                val sliderHeight = (Resources.getSystem().displayMetrics.widthPixels / 2.0).toInt()
                containerSlider.layoutParams.height = sliderHeight
                shimmerSlider.root.layoutParams.height = sliderHeight
                sliderViewPager.adapter = sliderImageAdapter

                etSearch.apply {
                    hint = CacheUtil.getInstance().getTranslations(Text.SEARCH_MENU, "Search menu")
                    addTextChangedListener(object : TextWatcher {
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
                                    viewModel.menuAdapterItems.value?.peekContent()?.let { menuAdapterItems ->
                                        menuCategoryItemAdapter.addData(menuAdapterItems)
                                    }
                                    activity?.apply {
                                        runOnUiThread {
                                            binding.apply {
                                                appBarLayout.setExpanded(true, false)
                                                rvMenuCategoriesItems.isNestedScrollingEnabled = true
                                                layoutSearchEmptyStateView.gone()
                                                layoutFooter.apply {
                                                    if(isCartEmpty) {
                                                        layoutFooter.gone()
                                                    } else {
                                                        layoutFooter.visible()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    btnClear.gone()
                                }
                            }
                        }

                    })
                }

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

                searchEmptyStateView.setTitle(CacheUtil.getInstance().getTranslations(Text.NO_RESULTS_FOUND, "No results found."))

                layoutFooter.rootView.apply {
                    val tvViewBag = it.findViewById<TextView>(R.id.tvViewBag)
                    tvViewBag.text = CacheUtil.getInstance().getTranslations(Text.VIEW_BAG, "View bag")
                }

            }
        }

        viewModel.apply {
            getSlides()
            getMenu()
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
        viewModel.slides.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { slides ->
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
        }

        viewModel.orderType.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { oType ->
                activity?.apply {
                    var type =
                        CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE")
                    var address = CacheUtil.getInstance().getTranslations(
                        Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                        "Please tap here to pick an order type"
                    )
                    var icon: Drawable? = null
                    when (oType) {
                        OrderType.PICKUP -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon = ContextCompat.getDrawable(
                                    this,
                                    R.drawable.ic_walk_in_pickup_small
                                )
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DRIVE_THRU -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon =
                                    ContextCompat.getDrawable(this, R.drawable.ic_drive_thru_small)
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DINE_IN -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon = ContextCompat.getDrawable(this, R.drawable.ic_dine_in_small)
                                type =
                                    CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DELIVERY -> {
                            icon = ContextCompat.getDrawable(this, R.drawable.ic_delivery_small)
                            if (CacheUtil.getInstance().orderAddress != null) {
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.DELIVERY, "Delivery")
                                address =
                                    when (CacheUtil.getInstance().orderAddress.attributes.label.trim()
                                        .lowercase()) {
                                        "home" -> CacheUtil.getInstance()
                                            .getTranslations(Text.HOME, "Home")

                                        "work" -> CacheUtil.getInstance()
                                            .getTranslations(Text.WORK, "Work")

                                        else -> CacheUtil.getInstance()
                                            .getTranslations(Text.OTHER, "Other")
                                    }
                            } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.DELIVERY, "Delivery")
                                address = CacheUtil.getInstance().orderDeliveryAddress
                            }
                        }

                        OrderType.CURBSIDE -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon = ContextCompat.getDrawable(
                                    this,
                                    R.drawable.ic_curbside_pickup_small
                                )
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.CURBSIDE_PICKUP, "Curbside Pickup")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        else -> {}
                    }
                    binding.orderTypeToolbar.setOrderType(type, address, icon)
                }

            }

        }

        viewModel.showMenuCategories.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuCategories ->
                binding.shimmerMenuCategories.root.gone()
                binding.rvMenuCategories.visible()
                menuCategoryAdapter.addData(menuCategories)
                binding.layoutSearch.visible()
            }
        }

        viewModel.menuAdapterItems.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuAdapterItems ->
                binding.shimmerMenuItems.root.gone()
                binding.rvMenuCategoriesItems.visible()
                menuCategoryItemAdapter.addData(menuAdapterItems)
                if (viewModel.initialLoad && args.menuCategoryId.isNotEmpty()) {
                    menuCategoryAdapter.getPosition(args.menuCategoryId)?.let { position ->
                        selectMenuCategory(position, args.menuCategoryId)
                    }
                }
                viewModel.initialLoad = false
            }
        }

        viewModel.searchMenuAdapterItems.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { searchMenuAdapterItems ->
                menuCategoryItemAdapter.addData(searchMenuAdapterItems)
                activity?.apply {
                    runOnUiThread {
                        binding.apply {
                            appBarLayout.setExpanded(false, false)
                            rvMenuCategoriesItems.isNestedScrollingEnabled = false
                            layoutSearchEmptyStateView.apply {
                                if(searchMenuAdapterItems.isEmpty()) {
                                    visible()
                                } else {
                                    gone()
                                }
                            }
                            layoutFooter.gone()
                        }
                    }
                }
            }
        }

        viewModel.showMenuItem.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { showMenuItem ->
                navMenuItem(showMenuItem.first, showMenuItem.second, showMenuItem.third)
            }
        }

        viewModel.showAddToCart.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuItem ->
                activity?.apply {
                    runOnUiThread {
                        val vibrator: Vibrator =
                            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                            this,
                            CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                            Toast.LENGTH_SHORT
                        ).show()
                        FacebookAppEventUtil.addToCartEvent(this, menuItem, MenuUtil.getMenuItemTotalPrice(menuItem))
                        EventBus.getDefault().post(AddToCartEvent())
                        setCartQuantity()
                    }
                }
            }
        }

        viewModel.showMessage.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { message ->
                activity?.apply {
                    runOnUiThread {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.showError.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { error ->
                activity?.apply {
                    runOnUiThread {
                        showAlertDialog("", error)
                    }
                }
            }
        }

        viewModel.showLoading.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { showLoading ->
                activity?.apply {
                    runOnUiThread {
                        if (showLoading) {
                            showProgressDialog(false)
                        } else {
                            dismissProgressDialog()
                        }
                    }
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

        viewModel.showMenuCategory.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { menuCategoryId ->
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
                findNavController().navigateToDeepLink("nav_menu/$menuCategoryId")
            }
        }

    }

    private fun showLoading() {
        binding.apply {
            shimmerMenuCategories.root.visible()
            shimmerMenuItems.root.visible()
            layoutSearch.gone()
            rvMenuCategories.gone()
            rvMenuCategoriesItems.gone()
        }
    }

    private fun orderTypeClick() {
        val type = CacheUtil.getInstance().orderType
        var orderType = OrderType.DELIVERY
        when (type) {
            OrderType.PICKUP, OrderType.DRIVE_THRU, OrderType.CURBSIDE -> {
                orderType = OrderType.PICKUP
            }

            OrderType.DELIVERY -> {
                orderType = OrderType.DELIVERY
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
                orderTypeLauncher.launch(intent)
            }
        }
    }

    fun scrollToMenuCategory(menuCategoryId: String) {
        menuCategoryAdapter.getPosition(menuCategoryId)?.let {
            if(it != menuCategoryAdapter.getSelected() &&
                it < menuCategoryAdapter.itemCount) {
                binding.rvMenuCategories.smoothScrollToPosition(it)
                menuCategoryAdapter.select(it)
            }
        }
    }

    private fun selectMenuCategory(position: Int, menuCategoryId: String) {
        binding.rvMenuCategories.smoothScrollToPosition(position)
        menuCategoryAdapter.select(position)
        viewModel.menuCategoryPositions[menuCategoryId]?.let {
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

    fun setCartQuantity() {
        binding.layoutFooter.rootView.apply {
            val tvCartQuantity = findViewById<TextView>(R.id.tvCartQuantity)
            val btnViewBag = findViewById<ConstraintLayout>(R.id.btnViewBag)
            val cartQuantity = CacheUtil.getInstance().cartQuantity
            binding.orderTypeToolbar.setCart(cartQuantity)
            if(cartQuantity > 0) {
                tvCartQuantity.text = "$cartQuantity"
                btnViewBag.setOnClickListener {
                    findNavController().navigateToDeepLink("nav_cart")
                }
                isCartEmpty = false
                binding.layoutFooter.visible()
            } else {
                btnViewBag.setOnClickListener(null)
                isCartEmpty = true
                binding.layoutFooter.gone()
            }
        }

    }

    private fun navMenuItem(menuCategoryId: String, menuItemId: String, menu: MenuItem) {
        val destinationLink = if(menu.isCombo) "nav_combo" else "nav_menu_item"
        val isSoldOut = CacheUtil.getOrderStoreId() != null && menu.outOfStockLocations.contains(
            CacheUtil.getOrderStoreId()
        )
        findNavController().navigateToDeepLink("$destinationLink/$menuCategoryId/$menuItemId?is_sold_out=$isSoldOut")
    }

    private fun navCart() {
        findNavController().navigateToDeepLink("nav_cart")
    }

}