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
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
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
import com.google.android.material.appbar.AppBarLayout
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.home.adapter.SliderImageAdapter
import com.herfy.menu.R
import com.herfy.menu.adapter.MenuCategoryAdapter
import com.herfy.menu.adapter.MenuCategoryItemAdapter
import com.herfy.menu.databinding.FragmentMenuBinding
import com.herfy.menu.viewmodel.MenuViewModel
import com.herfy.menu.widgets.CenterLayoutManager
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Navigation
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.event.AddToCartEvent
import com.herfy.utils.extensions.canBeCustomized
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.SnackBarUtils
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import org.greenrobot.eventbus.EventBus


@AndroidEntryPoint
class MenuFragment : BaseFragment<MenuViewModel, FragmentMenuBinding>() {

    private val args: MenuFragmentArgs by navArgs()

    override val viewModel: MenuViewModel by viewModels()
    override val layout: Int = R.layout.fragment_menu
    private lateinit var sliderImageAdapter: SliderImageAdapter
    private var sliderHandler: Handler? = null
    private var isCartEmpty = true
    private var areCategoryImagesHidden = false
    private var appBarFullyCollapsed = false
    private var categoryExpandedTopMarginPx = 0

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
            if (menuItem.canBeCustomized()) {
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
            menuIsScrolled = when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> false
                RecyclerView.SCROLL_STATE_DRAGGING -> true
                else -> true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (menuIsScrolled && binding.rvMenuCategories.isVisible()) {
                val firstVisibleItem =
                    (binding.rvMenuCategoriesItems.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                val lastVisibleItem =
                    (binding.rvMenuCategoriesItems.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (firstVisibleItem in viewModel.menuCategoryPositions.values) {
                    viewModel.getMenuCategoryId(firstVisibleItem)?.let { menuCategoryId ->
                        scrollToMenuCategory(menuCategoryId)
                    }
                } else if (menuCategoryItemAdapter.getMenuItemCategory(lastVisibleItem) != null) {
                    menuCategoryItemAdapter.getMenuItemCategory(lastVisibleItem)
                        ?.let { menuCategory ->
                            scrollToMenuCategory(menuCategory.id)
                        }
                }
            }
        }
    }

    private fun setupCategoryHeaderMetrics() {
        categoryExpandedTopMarginPx = ViewUtil.dpToPx(requireContext(), 24)
    }

    private fun setSearchModeUi(isSearching: Boolean) {
        if (isSearching) {
            binding.containerSlider.gone()
            binding.dotsIndicator.gone()
            binding.shimmerSlider.root.gone()
            binding.rvMenuCategories.gone()
            binding.viewMenu.gone()
            binding.viewSearch.visible()
            binding.appBarLayout.setExpanded(true, false)
        } else {
            binding.containerSlider.visible()
            binding.shimmerSlider.root.gone()
            if (sliderImageAdapter.itemCount > 1) {
                binding.dotsIndicator.visible()
            }
            binding.rvMenuCategories.visible()
            binding.viewMenu.visible()
            binding.viewSearch.gone()
            binding.appBarLayout.setExpanded(true, false)
        }
    }

    private fun applyCategoryHeaderState(isCollapsed: Boolean) {
        areCategoryImagesHidden = isCollapsed
        menuCategoryAdapter.setCategoryImagesHidden(isCollapsed)
        binding.rvMenuCategories.visible()
        (binding.rvMenuCategories.layoutParams as? MarginLayoutParams)?.let { params ->
            val targetTopMargin = if (isCollapsed) ViewUtil.dpToPx(requireContext(), 12) else categoryExpandedTopMarginPx
            if (params.topMargin != targetTopMargin) {
                params.topMargin = targetTopMargin
                binding.rvMenuCategories.layoutParams = params
            }
        }
    }

    private fun applyCategoryHeaderProgress(progress: Float) {
        val normalizedProgress = progress.coerceIn(0f, 1f)
        val expandedTopMargin = categoryExpandedTopMarginPx
        val collapsedTopMargin = ViewUtil.dpToPx(requireContext(), 12)
        (binding.rvMenuCategories.layoutParams as? MarginLayoutParams)?.let { params ->
            val targetTopMargin = (expandedTopMargin - ((expandedTopMargin - collapsedTopMargin) * normalizedProgress)).toInt()
            if (params.topMargin != targetTopMargin) {
                params.topMargin = targetTopMargin
                binding.rvMenuCategories.layoutParams = params
            }
        }

        val expandedItemHeight = ViewUtil.dpToPx(requireContext(), 118)
        val collapsedItemHeight = ViewUtil.dpToPx(requireContext(), 60)
        val targetItemHeight = (expandedItemHeight - ((expandedItemHeight - collapsedItemHeight) * normalizedProgress)).toInt()
        val expandedNameTopMargin = ViewUtil.dpToPx(requireContext(), 8)
        val collapsedNameTopMargin = ViewUtil.dpToPx(requireContext(), 0)
        val targetNameTopMargin = (expandedNameTopMargin -
            ((expandedNameTopMargin - collapsedNameTopMargin) * normalizedProgress)).toInt()
        val expandedNameBottomMargin = ViewUtil.dpToPx(requireContext(), 14)
        val collapsedNameBottomMargin = ViewUtil.dpToPx(requireContext(), 0)
        val targetNameBottomMargin = (expandedNameBottomMargin -
            ((expandedNameBottomMargin - collapsedNameBottomMargin) * normalizedProgress)).toInt()

        val childCount = binding.rvMenuCategories.childCount
        for (i in 0 until childCount) {
            val item = binding.rvMenuCategories.getChildAt(i) ?: continue
            item.layoutParams = item.layoutParams.apply {
                if (height != targetItemHeight) {
                    height = targetItemHeight
                }
            }

            item.findViewById<ImageView>(R.id.ivImage)?.let { imageView ->
                imageView.visibility = View.VISIBLE
                imageView.alpha = 1f - normalizedProgress
                val expandedImageSize = ViewUtil.dpToPx(requireContext(), 54)
                val collapsedImageSize = 0
                val targetImageSize = (expandedImageSize -
                    ((expandedImageSize - collapsedImageSize) * normalizedProgress)).toInt().coerceAtLeast(0)
                imageView.layoutParams = imageView.layoutParams.apply {
                    if (width != targetImageSize || height != targetImageSize) {
                        width = targetImageSize
                        height = targetImageSize
                    }
                }
                if (normalizedProgress >= 0.98f) {
                    imageView.visibility = View.GONE
                }
            }

            item.findViewById<TextView>(R.id.tvName)?.let { title ->
                (title.layoutParams as? MarginLayoutParams)?.let { params ->
                    if (params.topMargin != targetNameTopMargin || params.bottomMargin != targetNameBottomMargin) {
                        params.topMargin = targetNameTopMargin
                        params.bottomMargin = targetNameBottomMargin
                        title.layoutParams = params
                    }
                }
            }
        }
    }

    private fun setupAppBarCollapseListener() {
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (binding.viewSearch.isVisible()) {
                return@OnOffsetChangedListener
            }

            val offset = abs(verticalOffset)
            val progress = (offset / appBarLayout.totalScrollRange.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            applyCategoryHeaderProgress(progress)
            val collapseThreshold = appBarLayout.totalScrollRange - ViewUtil.dpToPx(requireContext(), 1)
            val expandThreshold = appBarLayout.totalScrollRange - ViewUtil.dpToPx(requireContext(), 32)
            val shouldCollapse = if (appBarFullyCollapsed) {
                offset >= expandThreshold
            } else {
                offset >= collapseThreshold
            }

            if (appBarFullyCollapsed == shouldCollapse) {
                return@OnOffsetChangedListener
            }

            appBarFullyCollapsed = shouldCollapse
            applyCategoryHeaderState(shouldCollapse)
        })
    }

    override fun onResume() {
        super.onResume()
        showLoading(viewModel.menuCategories.isEmpty())
        setCartQuantity()
        viewModel.getOrderType()
    }

    override fun initViews() {
        super.initViews()

        activity?.let {
            binding.apply {

                coordinatorLayout.apply {
                    layoutParams.cast<MarginLayoutParams> { params ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
                            params.topMargin = 0
                        } else {
                            // For Android 14 and below
                            params.topMargin = ViewUtil.dpToPx(it, 70)
                        }
                    }
                }

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
                val margin = ViewUtil.dpToPx(it, 16)
                val sliderHeight =
                    (Resources.getSystem().displayMetrics.widthPixels / (Values.SLIDER_MENU_ASPECT_RATIO_WIDTH / Values.SLIDER_MENU_ASPECT_RATIO_HEIGHT)).toInt() + margin
                containerSlider.layoutParams.height = sliderHeight
                shimmerSlider.root.layoutParams.height = sliderHeight
                sliderViewPager.adapter = sliderImageAdapter

                etSearch.apply {
                    hint = CacheUtil.getInstance().getTranslations(Text.SEARCH_MENU, "Search menu")
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            p0: CharSequence?,
                            p1: Int,
                            p2: Int,
                            p3: Int
                        ) {

                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                        }

                        override fun afterTextChanged(p0: Editable?) {
                            p0?.let {
                                if (it.isNotEmpty()) {
                                    resetHeaderCollapse()
                                    setSearchModeUi(isSearching = true)
                                    viewModel.searchMenuItems(it.toString())
                                    btnClear.visible()
                                } else {
                                    resetHeaderCollapse()
                                    setSearchModeUi(isSearching = false)
                                    viewModel.menuAdapterItems.value?.peekContent()
                                        ?.let { menuAdapterItems ->
                                            menuCategoryItemAdapter.addData(menuAdapterItems)
                                        }
                                    activity?.apply {
                                        runOnUiThread {
                                            binding.apply {
                                                appBarLayout.setExpanded(true, false)
                                                rvMenuCategoriesItems.isNestedScrollingEnabled =
                                                    true
                                                layoutSearchEmptyStateView.gone()
                                                layoutFooter.apply {
                                                    if (isCartEmpty) {
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
                setupCategoryHeaderMetrics()
                applyCategoryHeaderState(false)
                setupAppBarCollapseListener()

                rvMenuCategoriesItems.apply {
                    layoutManager = LinearLayoutManager(it)
                    adapter = menuCategoryItemAdapter
                    addOnScrollListener(menuOnScrollListener)
                }

                searchEmptyStateView.setTitle(
                    CacheUtil.getInstance()
                        .getTranslations(Text.NO_RESULTS_FOUND, "No results found.")
                )

                layoutFooter.rootView.apply {
                    val tvViewBag = it.findViewById<TextView>(R.id.tvViewBag)
                    tvViewBag.text =
                        CacheUtil.getInstance().getTranslations(Text.VIEW_BAG, "View bag")
                }

            }
        }

        viewModel.apply {
            getSlides()
            getMenuCategoriesItems()
        }
    }

    private fun resetHeaderCollapse() {
        appBarFullyCollapsed = false
        applyCategoryHeaderState(false)
        binding.layoutSearch.alpha = 1f
        binding.layoutSearch.translationY = 0f
        binding.layoutSearch.visibility = View.VISIBLE
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
                                    com.herfy.ordertype.R.drawable.ic_walk_in_pickup_small
                                )
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DRIVE_THRU -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon =
                                    ContextCompat.getDrawable(this, com.herfy.ordertype.R.drawable.ic_drive_thru_small)
                                type = CacheUtil.getInstance()
                                    .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DINE_IN -> {
                            if (CacheUtil.getInstance().orderStore != null) {
                                icon = ContextCompat.getDrawable(this, com.herfy.ordertype.R.drawable.ic_dine_in_small)
                                type =
                                    CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
                                address = CacheUtil.getInstance().orderStore.attributes.name
                            }
                        }

                        OrderType.DELIVERY -> {
                            icon = ContextCompat.getDrawable(this, com.herfy.ordertype.R.drawable.ic_delivery_small)
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
                                    com.herfy.ordertype.R.drawable.ic_curbside_pickup_small
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
                            // Keep slider/search header visible while user is searching.
                            appBarLayout.setExpanded(true, false)
                            rvMenuCategoriesItems.isNestedScrollingEnabled = false
                            layoutSearchEmptyStateView.apply {
                                if (searchMenuAdapterItems.isEmpty()) {
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
                            CacheUtil.getInstance()
                                .getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                            Toast.LENGTH_SHORT
                        ).show()
                        FacebookAppEventUtil.addToCartEvent(
                            this,
                            menuItem,
                            MenuUtil.getMenuItemTotalPrice(menuItem)
                        )
                        KochavaTrackerUtil.trackEventWithData(
                            "Add to Cart",
                            KochavaEventUtil.itemObject(menuItem)
                        )
                        EventBus.getDefault().post(AddToCartEvent())
                        setCartQuantity()
                    }
                }
            }
        }

        viewModel.showMessage.observeNonNull(this) {
            it.getContentIfNotHandled()?.let { message ->
                if (message == CacheUtil.getInstance()
                        .getTranslations(Text.ADDED_TO_FAVORITES) || message == CacheUtil.getInstance()
                        .getTranslations(Text.REMOVED_FROM_FAVORITES)
                ) {
                    isFavorites(
                        message == CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
                    )
                } else {
                    activity?.apply {
                        runOnUiThread {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
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
                findNavController().navigateToDeepLink("nav_menu/$menuCategoryId")
            }
        }

    }

    private fun isFavorites(isFavorite: Boolean) {
        var text = CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
        if (!isFavorite) {
            text = CacheUtil.getInstance().getTranslations(Text.REMOVED_FROM_FAVORITES)
        }
        var background: Int = R.drawable.background_favorites
        if (!isFavorite) {
            background = R.drawable.background_unfavorite
        }

        var color: Int =
            binding.rvMenuCategoriesItems.context.resources.getColor(R.color.fav_color)
        if (!isFavorite) {
            color =
                binding.rvMenuCategoriesItems.context.resources.getColor(R.color.unfav_color)
        }

        SnackBarUtils.showCustomSnackBar(
            binding.layoutFooter,
            text!!,
            isFavorite,
            background,
            color
        )
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                shimmerMenuCategories.root.visible()
                shimmerMenuItems.root.visible()
                layoutSearch.gone()
                rvMenuCategories.gone()
                rvMenuCategoriesItems.gone()
            } else {
                shimmerMenuCategories.root.gone()
                shimmerMenuItems.root.gone()
                layoutSearch.visible()
                rvMenuCategories.visible()
                rvMenuCategoriesItems.visible()
            }
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
            if (it != menuCategoryAdapter.getSelected() &&
                it < menuCategoryAdapter.itemCount
            ) {
                binding.rvMenuCategories.scrollToPosition(it)
                menuCategoryAdapter.select(it)
            }
        }
    }

    private fun selectMenuCategory(position: Int, menuCategoryId: String) {
        activity?.runOnUiThread {
            menuCategoryAdapter.select(position)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.rvMenuCategories.smoothScrollToPosition(position)
            }, 100)
            viewModel.menuCategoryPositions[menuCategoryId]?.let {
                binding.rvMenuCategoriesItems.apply {
                    layoutManager?.cast<LinearLayoutManager> { linearLayoutManager ->
                        linearLayoutManager.scrollToPositionWithOffset(it, layoutParams.height)
                    }
                    removeOnScrollListener(menuOnScrollListener)
                    clearOnScrollListeners()
                    Handler(Looper.getMainLooper()).postDelayed({
                        addOnScrollListener(menuOnScrollListener)
                    }, 250)
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
            if (cartQuantity > 0) {
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
        val isSoldOut = CacheUtil.getOrderStoreId() != null && menu.outOfStockLocations.contains(
            CacheUtil.getOrderStoreId()
        )
        findNavController().navigateToDeepLink("nav_menu_item_detail/$menuCategoryId/$menuItemId?is_sold_out=$isSoldOut")
    }

    private fun navCart() {
        findNavController().navigateToDeepLink("nav_cart")
    }

}