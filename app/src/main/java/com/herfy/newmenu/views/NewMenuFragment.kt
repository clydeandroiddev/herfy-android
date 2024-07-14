package com.herfy.newmenu.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.country.CountriesFragment
import com.herfy.databinding.FragmentMenuNewBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.menu.MenuContract
import com.herfy.menu.MenuPresenter
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter
import com.herfy.newmenu.NewMenuFragmentCommonListener
import com.herfy.newmenu.NewMenuTabListener
import com.herfy.newmenu.SliderContract
import com.herfy.newmenu.SliderPresenter
import com.herfy.newmenu.TabModel
import com.herfy.newmenu.adapter.MenuCategoriesAdapter
import com.herfy.newmenu.adapter.SearchMenuRecyclerViewAdapter
import com.herfy.newmenu.adapter.SliderFragmentStateAdapter
import com.herfy.newmenu.viewholder.NewMenuViewHolder
import com.herfy.popup.PopupFragment
import com.herfy.utils.helper.CacheHelper
import com.herfy.util.StringUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.smoothSnapToPosition
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.Favorite
import com.skylinedynamics.solosdk.api.models.objects.Ingredient
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Store
import java.util.Locale


class NewMenuFragment : BaseFragment(), MenuContract.View, SliderContract.View, NewMenuTabListener {

    private lateinit var binding: FragmentMenuNewBinding

    //private val args: NewMenuFragmentArgs by navArgs()

    private lateinit var menuPresenter: MenuContract.Presenter
    private lateinit var sliderPresenter: SliderContract.Presenter

    private var searchAdapter: SearchMenuRecyclerViewAdapter? = null
    private val tabAdapters = mutableListOf<RecyclerView.Adapter<NewMenuViewHolder>>()

    private var sliderHeight = 0
    private var searchLinearLayoutManager: LinearLayoutManager? = null
    private var lastNewBottom = -1
    private var firstLaunch = true
    private var appBarIsLock = false
    private var dontAnimate = false
    private var isCollapsed = true
    private val cardViewList: MutableList<CardView?> = ArrayList()
    private var tabLastPosition = -1
    private var selectedHomeMenuCategoryId = ""
    private var latestButtonAlpha = 1f
    private var listener: NewMenuFragmentCommonListener? = null
    private var sliderHandler: Handler? = null
    private var oldCategoryId = ""
    private var oldCategory = ""
    private val previousCategory = ""
    private var oldPosition = 0
    private var newPosition = 0
    private var isInitialLoad = true
    private var lastCategoryPosition = 0
    private val previousMenuCategories = ArrayList<MenuCategory>()
    private val menuCategories: List<MenuCategory> = ArrayList()
    private var isFromCountries = false
    private var isFromOrderType = false
    private var animDone = true
    private var isVisibletoUser = true
    private var fromMenuItem = false
    private var fromOrderTypeMenuCategoryId = ""
    private var previousSelectedStoreId: String? = null
    private var selectedMenuCategoryPosition = 0
    private var fromInitialLaunch = false
    private var isDiffMenuEnabled = false

    private var selectOrderTypeDialog: SelectOrderTypeDialogFragment? = null

    private var menuIsScrolled = false
    private val menuOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    menuIsScrolled = false
                    onMenuCategoryItemsScrolled(
                        show = true, isLastItemVisible = true
                    )
                }

                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    menuIsScrolled = true
                    onMenuCategoryItemsScrolled(
                        show = false, isLastItemVisible = true
                    )
                }

                else -> menuIsScrolled = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (menuIsScrolled) {
                val firstVisibleItem =
                    (binding.menuRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (firstVisibleItem != selectedMenuCategoryPosition && firstVisibleItem < binding.tabsLayout.tabCount) {
                    binding.tabsLayout.selectTab(
                        binding.tabsLayout.getTabAt(firstVisibleItem), true
                    )
                    selectTab(firstVisibleItem)
                }
            }
        }
    }
    private val menuCategoriesAdapter: MenuCategoriesAdapter by lazy {
        MenuCategoriesAdapter(menuCategories = emptyArray(), onSelect = { menuCategory, menuItem ->
            menuPresenter.selectMenuItem(menuCategory, menuItem)
        }, onAddRemoveFavorite = { isAdd, menuCategory, menuItem ->
            menuPresenter.menuCategory = menuCategory
            menuPresenter.menuItem = menuItem
            menuPresenter.addRemoveFavorite(isAdd)
        }, onAddToCart = { imageView, menuItem, price, quantity ->
            menuPresenter.addCartMenuItem(imageView, menuItem, price, quantity)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //selectedHomeMenuCategoryId = args.menuCategoryId
        activity?.cast<MainActivity> {
            listener = it
        }
        menuPresenter = MenuPresenter(this)
        sliderPresenter = SliderPresenter(this)
        isDiffMenuEnabled = CacheUtil.getInstance().isSeparatedMenus
    }

    override fun isFavorite(isFavorite: Boolean) {

    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("lastCategoryPosition", lastCategoryPosition)
        outState.putInt("tabLastPosition", tabLastPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            lastCategoryPosition = savedInstanceState.getInt("lastCategoryPosition")
            tabLastPosition = savedInstanceState.getInt("tabLastPosition")
        }
    }

    private fun checkBackground() {
        binding.appBarLayout.setBackgroundResource(R.color.page_background_color)
        binding.menuContent.setBackgroundResource(R.color.page_background_color)
        binding.tabsLayout.setBackgroundResource(R.color.page_background_color)
        binding.withSearchLayout.setBackgroundResource(R.color.page_background_color)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (savedInstanceState != null) {
            lastCategoryPosition = savedInstanceState.getInt("lastCategoryPosition")
            tabLastPosition = savedInstanceState.getInt("tabLastPosition")
        }
        binding = FragmentMenuNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //pagerAdapter = NewMenuViewPagerAdapter(this, context, this)
        try {
            tabAdapters.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.menuRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = menuCategoriesAdapter
            addOnScrollListener(menuOnScrollListener)
        }
        checkBackground()
        fromInitialLaunch = true
        Handler().postDelayed({
            menuPresenter.start()
            if (firstLaunch && !isFromCountries) {
                showShimmers()
                loadInitial()
            } else if (!isFromCountries && !isFromOrderType) {
                showMenuCategories(
                    refresh = false,
                    isFromInitialLoad = false,
                    menuCategories = menuPresenter.menuCategories
                )
                if (sliderPresenter.sliders.size > 0 && isAdded) {
                    showSlides(sliderPresenter.sliders)
                } else {
                    sliderPresenter.getSlider()
                }
            }
            if (CacheUtil.getInstance().application.attributes.forceLocation == 0 || CacheUtil.getInstance().orderStore == null && CacheUtil.getInstance().application.attributes.forceLocation != 0 && CacheUtil.getInstance().application.attributes.homeScreenDesign == "default") {
                menuPresenter.setPreselectedBranch()
            } else {
                CacheUtil.getInstance().preselectedBranch = false
            }


            if (BuildConfig.FEATURE_PHASE >= 1.3f) {

                CacheUtil.getInstance().popup?.let {
                    Glide.with(requireContext()).load(CacheUtil.getInstance().popup.imageUri)
                        .preload()
                    setFragmentResultListener(PopupFragment.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                        clearFragmentResultListener(PopupFragment.ARGS_KEY_FRAGMENT_RESULT)
                        when (bundle.getString(PopupFragment.ARGS_KEY_ACTION_TYPE)) {
                            "url" -> showLink(Uri.parse(bundle.getString(PopupFragment.ARGS_KEY_ACTION_VALUE)))
                            "category" -> showMenuCategory(
                                bundle.getString(PopupFragment.ARGS_KEY_ACTION_VALUE) ?: ""
                            )

                            "item", "product" -> showMenuItem(
                                bundle.getString(PopupFragment.ARGS_KEY_ACTION_VALUE) ?: ""
                            )
                        }
                    }
                    //NavigationUtils.navigateSafe(findNavController(), NewMenuFragmentDirections.actionMenuToDialogPopup())
                }
            }
        }, 300)
    }

    private fun loadInitial() {
        if (isRemoving || activity == null || activity?.isFinishing == true) return

        binding.blocker.visibility = View.VISIBLE
        firstLaunch = false
        if (!isFromOrderType) {
            sliderPresenter.getSlider()
        }
        val fromDeeplink =
            Values.sDeepLink.isNotEmpty() && (Values.sDeepLink.equals("categories") || Values.sDeepLink.equals(
                "items"
            ))
        var forceLocationMenuRefresh = false
        activity?.cast<MainActivity> {
            forceLocationMenuRefresh = it.forceLocationMenuRefresh
            it.forceLocationMenuRefresh = false
        }
        menuPresenter.getMenuCategories(
            forceLocationMenuRefresh,
            false,
            fromDeeplink,
            isFromCountries || isFromOrderType,
            true,
            null,
            null
        )
        previousMenuCategories.clear()
        previousMenuCategories.addAll(menuPresenter.menuCategories)
    }

    override fun onResume() {
        super.onResume()
        isVisibletoUser = true

        val orderTypeSelected = CacheUtil.getInstance().orderType
        val isMenuSeparate = CacheUtil.getInstance().isSeparatedMenus
        if (isMenuSeparate && orderTypeSelected == OrderType.NONE) {
            selectOrderTypeDialog?.dismiss()
            val fragmentManager = childFragmentManager
            if (!fragmentManager.isDestroyed) {
                selectOrderTypeDialog =
                    SelectOrderTypeDialogFragment(onOrderTypeSelected = { orderType ->
                        handleDiffMenu(orderType)
                    })
                selectOrderTypeDialog?.show(
                    fragmentManager, SelectOrderTypeDialogFragment::class.java.name
                )
            }
        }

        if (!firstLaunch && CacheUtil.getInstance().reloadDifferentMenu) {
            binding.tabsLayout.forceLayout()
            binding.tabsLayout.invalidate()
            binding.tabsLayout.requestLayout()

            selectedMenuCategoryPosition = 0
            try {
                tabAdapters.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Handler().postDelayed({
                showShimmers()
                loadInitial()
            }, 300)
            CacheUtil.getInstance().reloadDifferentMenu = false
        }

        if (!fromMenuItem) {
            update()
        } else {
            fromMenuItem = false
            menuCategoriesAdapter.updateMenuItems(selectedMenuCategoryPosition)
            updateFloatingTotalPrice()
            updateFloatingTotalContainer()
        }
        if (isFromOrderType) {
            isFromOrderType = false
            if (CacheUtil.getOrderStoreId() != null && previousSelectedStoreId != CacheUtil.getOrderStoreId()) {
                Handler(Looper.getMainLooper()).postDelayed({ showShimmers() }, 50)
                sliderPresenter.getSlider()
                refreshCategory()
                if (fromOrderTypeMenuCategoryId != "") {
                    menuPresenter.selectMenuCategory(fromOrderTypeMenuCategoryId)
                    fromOrderTypeMenuCategoryId = ""
                }
                menuPresenter.updateCart(true)
            }

        } else if (!fromInitialLaunch && BuildUtil.isEkfc() && !isFromCountries) {
            Handler().postDelayed({
                showShimmers()
                loadInitial()
            }, 300)
        }
        fromInitialLaunch = false
    }

    override fun onPause() {
        super.onPause()
        isVisibletoUser = false
    }

    override fun onStop() {
        super.onStop()
        isVisibletoUser = false
    }

    private fun handleDiffMenu(orderType: OrderType) {
        isFromOrderType = true
        previousSelectedStoreId = CacheUtil.getOrderStoreId()
        fromOrderTypeMenuCategoryId =
            menuPresenter.getMenuCategoryByPosition(selectedMenuCategoryPosition)?.id ?: ""
        activity?.cast<MainActivity> { mainActivity ->
            mainActivity.forceLocation = false
        }
        if (CacheUtil.getInstance().orderType != OrderType.CURBSIDE) {
            CacheUtil.getInstance().curbsideCar = null
        }
        val storeId = CacheUtil.getOrderStoreId() ?: ""
        val intent = Intent(activity, NavigationActivity::class.java)
        intent.putExtra("navigation", R.id.nav_order_type)
        intent.putExtra("selectedOrderType", orderType.value)
        intent.putExtra("fromCart", false)
        intent.putExtra("checkout", false)
        intent.putExtra("storeId", storeId)
        activity?.startActivity(intent)
    }

    override fun update() {
        super.update()
        if (binding.orderType != null) {
            menuPresenter.getStoreOpen()
            try {
                activity?.cast<MainActivity> {
                    /*if (isFromShowOrderType && CacheUtil.getInstance().orderStore != null) {
                        isFromShowOrderType = false
                        if (lastCategoryPosition < pagerAdapter!!.itemCount) {
                            pagerAdapter!!.updateFragment(lastCategoryPosition)
                        }
                    }*/
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            when (CacheUtil.getInstance().orderType) {
                OrderType.PICKUP, OrderType.DRIVE_THRU -> {
                    menuPresenter.getStore()
                    if (CacheUtil.getInstance().orderStore != null) {
                        val oT = CacheUtil.getInstance().orderType
                        var headerText = CacheUtil.getInstance()
                            .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup").uppercase(
                                Locale.getDefault()
                            )
                        if (oT == OrderType.DRIVE_THRU) {
                            headerText = CacheUtil.getInstance()
                                .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                                .uppercase(
                                    Locale.getDefault()
                                ) + ":"
                        }
                        binding.orderType.text = headerText
                        binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    } else {
                        binding.branchName.text = CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                            "Please tap here to pick an order type"
                        )
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE").uppercase(
                                Locale.getDefault()
                            )
                    }
                }

                OrderType.DINE_IN -> {
                    menuPresenter.getStore()
                    if (CacheUtil.getInstance().orderStore != null) {
                        binding.orderType.text =
                            CacheUtil.getInstance().getTranslations(Text.DINE_IN_AT, "DINE-IN AT")
                                .uppercase(
                                    Locale.getDefault()
                                )
                        binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    } else {
                        binding.branchName.text = CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                            "Please tap here to pick an order type"
                        )
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE").uppercase(
                                Locale.getDefault()
                            )
                    }
                }

                OrderType.DELIVERY -> {
                    menuPresenter.getAddress()
                    if (CacheUtil.getInstance().orderAddress != null) {
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.DELIVER_TO_CAPS, "DELIVER TO").uppercase(
                                Locale.getDefault()
                            )
                        binding.branchName.text =
                            CacheUtil.getInstance().orderAddress.attributes.line1
                    } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.DELIVER_TO_CAPS, "DELIVER TO").uppercase(
                                Locale.getDefault()
                            )
                        binding.branchName.text = CacheUtil.getInstance().orderDeliveryAddress
                    } else {
                        binding.branchName.text = CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                            "Please tap here to pick an order type"
                        )
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE").uppercase(
                                Locale.getDefault()
                            )
                    }
                }

                OrderType.CURBSIDE -> {
                    menuPresenter.getStore()
                    if (CacheUtil.getInstance().orderStore != null) {
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.CURBSIDE_PICKUP_FROM_CAPS, "CURBSIDE PICKUP FROM")
                            .uppercase(
                                Locale.getDefault()
                            )
                        binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    } else {
                        binding.branchName.text = CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                            "Please tap here to pick an order type"
                        )
                        binding.orderType.text = CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE").uppercase(
                                Locale.getDefault()
                            )
                    }
                }

                OrderType.NONE -> {
                    binding.branchName.text = CacheUtil.getInstance().getTranslations(
                        Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                        "Please tap here to pick an order type"
                    )
                    binding.orderType.text = CacheUtil.getInstance()
                        .getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE").uppercase(
                            Locale.getDefault()
                        )
                }
            }
        }
        if (CacheUtil.getInstance().favoriteMenuCategory != null && CacheUtil.getInstance().favoriteMenuItem != null) {
            menuPresenter.menuCategory = CacheUtil.getInstance().favoriteMenuCategory
            menuPresenter.menuItem = CacheUtil.getInstance().favoriteMenuItem
            setMenuItem(menuPresenter.menuCategory, menuPresenter.menuItem)
            CacheUtil.getInstance().favoriteMenuCategory = null
            CacheUtil.getInstance().favoriteMenuItem = null
            try {
                activity?.let {
                    if (it is BaseActivity) {
                        it.showLoadingDialog()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            menuPresenter.addRemoveFavorite(true)
        }
        if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cartQuantity == 0) {
            binding.cartQuantity.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
        } else if (binding.cartIcon.visibility == View.VISIBLE) {
            binding.cartQuantity.text =
                StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
            binding.cartQuantity.visibility = View.VISIBLE
            binding.totalContainer.visibility = View.VISIBLE
        }
        updateFloatingTotalPrice()
    }

    override fun setupViews() {
        if (!isAdded && isVisibletoUser) {
            // retry setup view until the view is added
            Handler().postDelayed({ setupViews() }, 100)
            return
        }
        if (isRemoving || activity == null || activity?.isFinishing == true) return
        binding.sliderViewPager.isSaveEnabled = false
        binding.tabsLayout.isSmoothScrollingEnabled = true
        binding.cardOrderType.setOnClickListener {
            isFromOrderType = true
            previousSelectedStoreId = CacheUtil.getOrderStoreId()
            //val selectedPosition = pagerAdapter?.selectedPosition ?: binding.viewPager.currentItem
            fromOrderTypeMenuCategoryId =
                menuPresenter.getMenuCategoryByPosition(selectedMenuCategoryPosition)?.id ?: ""
            activity?.cast<MainActivity> { mainActivity ->
                mainActivity.forceLocation = false
            }
            if (CacheUtil.getInstance().orderType != OrderType.CURBSIDE) {
                CacheUtil.getInstance().curbsideCar = null
            }/*setFragmentResultListener(OrderTypeFragment.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                clearFragmentResultListener(requestKey = OrderTypeFragment.ARGS_KEY_FRAGMENT_RESULT)
                if(CacheUtil.getOrderStoreId() != null) {
                    isFromOrderType = true
                    if (!bundle.getBoolean(OrderTypeFragment.ARGS_KEY_IS_SAME_BRANCH, false)) {
                        lastCategoryPosition = -1
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            showShimmers()
                        }, 50)
                    }
                    loadInitial()
                }
            }*/
            val storeId = CacheUtil.getOrderStoreId() ?: ""
            //NavigationUtils.navigateSafe(findNavController(), NewMenuFragmentDirections.actionMenuToOrderType(false, storeId))
            val intent = Intent(activity, NavigationActivity::class.java)
            intent.putExtra("navigation", R.id.nav_order_type)
            if (isDiffMenuEnabled) intent.putExtra(
                "selectedOrderType", CacheUtil.getInstance().orderType.value
            )
            intent.putExtra("fromCart", false)
            intent.putExtra("checkout", false)
            intent.putExtra("storeId", storeId)
            activity?.startActivity(intent)
        }
        binding.cardClose.setOnClickListener {
            if (binding.cardClose.visibility == View.VISIBLE) {
                lockScrollingOfToolbar(true)
                binding.searchEmptyLayout.visibility = View.GONE
                //binding.noSearchLayout.visibility = View.VISIBLE
                binding.withSearchLayout.visibility = View.GONE
                binding.tabsLayout.visibility = View.VISIBLE
                if (!dontAnimate) {
                    binding.appBarLayout.setExpanded(true, true)
                    binding.cardClose.visibility = View.GONE
                    binding.searchText.visibility = View.GONE
                    animateOrderTypeContainer(false)
                    try {
                        activity?.let {
                            animateCard(
                                it.resources.getDimensionPixelSize(R.dimen.search_bar_start_width),
                                false
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    binding.searchText.setText("")
                }
                onMenuCategoryItemsScrolled(show = false, isLastItemVisible = true)
                dontAnimate = false
            }
        }
        binding.cardSearch.setOnClickListener {
            if (binding.cardClose.visibility != View.VISIBLE) {
                binding.searchText.setText("")
                binding.cardClose.visibility = View.VISIBLE
                binding.searchText.visibility = View.VISIBLE
                binding.appBarLayout.setExpanded(false, true)
                animateCard((binding.cardSearchContainer.parent as View).measuredWidth, true)
                animateOrderTypeContainer(true)
            }
        }
        binding.blocker.setOnClickListener { }
        binding.withSearchLayout.setOnClickListener { }
        searchAdapter = SearchMenuRecyclerViewAdapter(activity, menuPresenter)
        searchAdapter?.updateMenuList(ArrayList())
        searchLinearLayoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.menuItemsSearchList.layoutManager = searchLinearLayoutManager
        try {
            activity?.let {
                val displayMetrics = DisplayMetrics()
                it.windowManager.defaultDisplay.getMetrics(displayMetrics)
                sliderHeight = displayMetrics.widthPixels / 2
                if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                        Values.IMAGE_TEXT, ignoreCase = true
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.sliderViewPager.layoutParams.height = sliderHeight
        binding.emptySlider.layoutParams.height = sliderHeight
        binding.newMenuBannerShimmer.layoutParams.height = sliderHeight
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            binding.totalContainer.setBackgroundResource(R.drawable.background_oval_disabled)
            try {
                activity?.let {
                    binding.btnViewBag.setTextColor(ContextCompat.getColor(it, R.color.disabled))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            binding.totalContainer.setBackgroundResource(R.drawable.background_oval)
            binding.btnViewBag.setTextColor(Color.parseColor(CacheUtil.getInstance().colorMain))
        }/*binding.viewPager.reduceDragSensitivity(2)
        binding.viewPager.offscreenPageLimit = 10
        binding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (binding.cardClose.visibility == View.VISIBLE) {
                    dontAnimate = false
                    binding.cardClose.performClick()
                }
                lastCategoryPosition = position
                if(position >= 0 && position < previousMenuCategories.size) {
                    oldCategoryId = previousMenuCategories[position].id
                } else {
                    oldCategoryId = ""
                }
                pagerAdapter!!.updateFragment(position)
                pagerAdapter?.selectedPosition?.let { selectedPosition ->
                    if(selectedPosition < cardViewList.size && selectedPosition < binding.tabsLayout.tabCount) {
                        val tab = binding.tabsLayout.getTabAt(selectedPosition)
                        tab?.customView = null
                        val model = pagerAdapter?.getTabView(selectedPosition, false)
                        cardViewList.add(selectedPosition, model?.card)
                        tab?.customView = model?.tab
                    }
                }
                if(position < cardViewList.size && position < binding.tabsLayout.tabCount) {
                    val tab = binding.tabsLayout.getTabAt(position)
                    tab?.customView = null
                    val model = pagerAdapter?.getTabView(position, true)
                    cardViewList.add(position, model?.card)
                    tab?.customView = model?.tab
                }
                pagerAdapter?.selectedPosition = position
            }
        })*/
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout: AppBarLayout, verticalOffset: Int ->
            try {
                val totalRange = Math.abs(appBarLayout.totalScrollRange)
                val positiveVerticalOffset = Math.abs(verticalOffset)
                try {
                    activity?.let {
                        var startPadding =
                            it.resources.getDimensionPixelSize(R.dimen.search_box_bottom_padding)
                        var dBP =
                            it.resources.getDimensionPixelSize(R.dimen.search_box_desired_bottom_padding)
                        if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                                Values.IMAGE_TEXT, ignoreCase = true
                            )
                        ) {
                            startPadding =
                                it.resources.getDimensionPixelSize(R.dimen.search_box_bottom_padding_text_only)
                            dBP =
                                it.resources.getDimensionPixelSize(R.dimen.search_box_desired_bottom_padding_text_only)
                        }
                        val dividerValue = totalRange / (startPadding - dBP)
                        var newBottom = dBP + ((totalRange - positiveVerticalOffset) / dividerValue)
                        if (newBottom > startPadding) {
                            newBottom = startPadding
                        }
                        lastNewBottom = newBottom
                        val viewDivider = (totalRange / 100f)
                        latestButtonAlpha =
                            ((totalRange - positiveVerticalOffset) / viewDivider) / 100
                        changeCategoryIconsAlpha()
                        setSearchBoxBottomMargin(binding.searchBox, newBottom)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (positiveVerticalOffset >= totalRange) {
                    binding.space.visibility = View.GONE
                    isCollapsed = false
                } else {
                    isCollapsed = true
                    binding.space.visibility = View.VISIBLE
                }
                setEmptyMenuLayoutMargin()
            } catch (e: Exception) {
                if (lastNewBottom != -1) {
                    setSearchBoxBottomMargin(binding.searchBox, lastNewBottom)
                }
                e.printStackTrace()
            }
        })
        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    if (binding.cardClose.visibility == View.VISIBLE) {
                        if (!appBarIsLock) {
                            binding.appBarLayout.setExpanded(false, true)
                        }
                        // We lock the scrolling when in search mode
                        // and we do not show the list of items
                        // with categories
                        lockScrollingOfToolbar(false)
                        //binding.noSearchLayout.visibility = View.GONE
                        binding.withSearchLayout.visibility = View.VISIBLE
                        // We check below if search items size is == 0
                        // then show shimmer and hide empty view
                        // we dont want them to show at the same time.
                        if (menuPresenter.searchItemsSize == 0) {
                            binding.searchEmptyLayout.visibility = View.GONE
                            binding.newMenuItemSearchShimmer.visibility = View.VISIBLE
                        }
                        if (editable.length > 2) {
                            menuPresenter.search(editable.toString())
                        }
                        binding.searchBox.bringToFront()
                        binding.searchBox.invalidate()
                    }
                } else {
                    if (binding.cardClose.visibility == View.VISIBLE) {
                        dontAnimate = true
                        binding.cardClose.performClick()
                    }
                }
            }
        })
        binding.searchText.onFocusChangeListener =
            View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (v === binding.searchText) {
                    if (hasFocus) {
                        showKeyboard(binding.searchText)
                    } else {
                        closeKeyboard(binding.searchText)
                    }
                }
            }
        binding.totalContainer.setOnClickListener(View.OnClickListener {
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                return@OnClickListener
            }
            val intent = Intent(activity, NavigationActivity::class.java)
            intent.putExtra("navigation", R.id.nav_cart)
            intent.putExtra("isBranchPreselected", menuPresenter.isPreselectedBranch)
            activity?.startActivity(intent)
        })/*if(!isFromCountries && !isFromOrderType) {
            refreshCategory()
        }*/
    }

    override fun updateFloatingTotalPrice() {
        activity?.runOnUiThread {
            if (CacheUtil.getInstance().cart.size > 0) {
                if (LocaleUtil.getInstance().isArabic) {
                    binding.totalPrice.text = CacheUtil.getInstance()
                        .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(
                        menuPresenter.menuItemsTotalPrice
                    )
                } else {
                    binding.totalPrice.text = StringUtil.getFormattedPrice(
                        menuPresenter.menuItemsTotalPrice
                    ).toString() + " " + CacheUtil.getInstance()
                        .getTranslations(Text.TOTAL, "TOTAL")
                }
            } else {
                if (LocaleUtil.getInstance().isArabic) {
                    binding.totalPrice.text = CacheUtil.getInstance()
                        .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(
                        0.0
                    )

                } else {
                    binding.totalPrice.text =
                        StringUtil.getFormattedPrice(0.0).toString() + " " + CacheUtil.getInstance()
                            .getTranslations(
                                Text.TOTAL, "TOTAL"
                            )
                }
            }
        }
    }

    fun updateFloatingTotalContainer() {
        if (CacheUtil.getInstance().cartQuantity == 0) {
            binding.cartQuantity.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
        } else {
            binding.cartQuantity.text =
                StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
            binding.cartQuantity.visibility = View.VISIBLE
            if (!CacheUtil.getInstance().application.attributes.menuOnly && !CacheUtil.getInstance().application.attributes.homeScreenDesign.equals(
                    "option-4", ignoreCase = true
                )
            ) {
                binding.totalContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun refreshCategory() {
        showMenuCategories(
            refresh = true, isFromInitialLoad = false, menuCategories = menuPresenter.menuCategories
        )/*Handler().postDelayed({
            try {
                try {
                    binding.viewPager.adapter = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val activity = activity
                if (activity != null) {
                    pagerAdapter!!.updatePresenter(menuPresenter)
                    binding.viewPager.adapter = pagerAdapter
                    val tabLayoutMediator = TabLayoutMediator(
                        (binding.tabsLayout), (binding.viewPager), true
                    ) { tab: TabLayout.Tab, position: Int ->
                        val model: TabModel? =
                            pagerAdapter!!.getTabView(position, position == tabLastPosition)
                        if (model != null) {
                            tab.customView = model.tab
                        }
                    }
                    tabLayoutMediator.attach()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 50)*/
    }

    // enables locking of scroll or appbar layout
    private fun lockScrollingOfToolbar(lock: Boolean) {
        appBarIsLock = lock
        val params = binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        if (params.behavior == null) params.behavior = AppBarLayout.Behavior()
        val behaviour = params.behavior as AppBarLayout.Behavior?
        behaviour!!.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return lock
            }
        })
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.searchEmptyText.text =
            CacheUtil.getInstance().getTranslations(Text.NO_ITEM_FOUND, "No item found")
        binding.searchEmptyTextDesc.text = CacheUtil.getInstance().getTranslations(
            Text.NO_ITEM_FOUND_DESC,
            "Sorry, we were unable to find something that matches your search."
        )
        binding.btnViewBag.text =
            CacheUtil.getInstance().getTranslations(Text.VIEW_BAG_CAPS, "VIEW BAG")
    }

    override fun showStore(store: Store) {}

    override fun navigateToMenuItemDetails(
        menuCategory: MenuCategory?,
        menuItem: MenuItem?
    ) {}

    override fun showStoreOpen(open: Boolean, message: String) {}
    override fun showFrequentBoughtItems(frequentBoughtItems: MutableList<MenuItem>?) {

    }

    override fun showAddress(address: String) {}
    override fun reloadMenu() {
        menuPresenter.reloadMenu()
        updateFloatingBag()
    }

    override fun showMenuCategories(
        refresh: Boolean, isFromInitialLoad: Boolean, menuCategories: List<MenuCategory>
    ) {
        val activity = activity as BaseActivity?
        try {
            activity?.runOnUiThread {
                activity.dismissDialogs()
                if (isInitialLoad && previousMenuCategories.isEmpty()) {
                    previousMenuCategories.addAll(menuCategories)
                }
                createTabs(menuCategories)
                menuCategoriesAdapter.updateMenuCategories(menuCategories.toTypedArray())
                if (previousMenuCategories.size == menuCategories.size) {
                    oldPosition = lastCategoryPosition
                    if (oldPosition >= 0 && oldPosition < previousMenuCategories.size) {
                        oldCategoryId = previousMenuCategories[oldPosition].id
                        oldCategory = previousMenuCategories[oldPosition].getName("en-us")
                    } else {
                        oldCategoryId = ""
                        oldCategory = ""
                    }
                } else {
                    previousMenuCategories.clear()
                    previousMenuCategories.addAll(menuCategories)
                }
                for (cat: MenuCategory in menuCategories) {
                    if ((cat.id == oldCategoryId)) {
                        newPosition = menuCategories.indexOf(cat)
                        break
                    }
                }
                if (CacheUtil.getInstance().sliderMenuCategory.isNotEmpty()) {
                    menuPresenter.selectMenuCategory(CacheUtil.getInstance().sliderMenuCategory)
                    CacheUtil.getInstance().sliderMenuCategory = ""
                }
                setupSliderHeight()
                isFromOrderType = false
                isFromCountries = false
                hideShimmers()
                deepLinkRedirect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSliderHeight() {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                val displayMetrics = DisplayMetrics()
                it.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
                sliderHeight = displayMetrics.widthPixels / 2
                if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                        Values.IMAGE_TEXT, ignoreCase = true
                    )
                ) {
                    binding.mainToolbar.layoutParams.height =
                        sliderHeight + (resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider) - resources.getDimensionPixelSize(
                            R.dimen.seventy_dp
                        ))
                } else {
                    binding.mainToolbar.layoutParams.height =
                        sliderHeight + resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider)
                }
            }
        }
    }

    private fun setEmptyMenuLayoutMargin() {
        menuCategoriesAdapter.setCollapsed(isCollapsed)
    }

    private fun createTabs(menuCategories: List<MenuCategory>) {
        try {
            activity?.let {
                cardViewList.clear()
                binding.tabsLayout.removeAllTabs()
                menuCategories.forEachIndexed { position, menuCategory ->
                    val tabModel =
                        getTabView(position, menuCategory, position == selectedMenuCategoryPosition)
                    cardViewList.add(position, tabModel.card)
                    val tab = binding.tabsLayout.newTab()
                    tab.customView = tabModel.tab
                    binding.tabsLayout.addTab(tab)
                }
            }
            binding.tabsLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (!menuIsScrolled) {
                        tab?.position?.let {
                            if (it != selectedMenuCategoryPosition) {
                                binding.menuRecyclerView.smoothSnapToPosition(it)
                                binding.menuRecyclerView.removeOnScrollListener(menuOnScrollListener)
                                binding.menuRecyclerView.clearOnScrollListeners()
                                selectTab(it)
                            }
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    if (!menuIsScrolled) {
                        tab?.position?.let {
                            if (it != selectedMenuCategoryPosition) {
                                binding.menuRecyclerView.smoothSnapToPosition(it)
                                binding.menuRecyclerView.removeOnScrollListener(menuOnScrollListener)
                                binding.menuRecyclerView.clearOnScrollListeners()
                                selectTab(it)
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectTab(currentTabPosition: Int) {
        if (currentTabPosition != selectedMenuCategoryPosition) {
            if (selectedMenuCategoryPosition < cardViewList.size && selectedMenuCategoryPosition < binding.tabsLayout.tabCount) {
                val previousTab = binding.tabsLayout.getTabAt(selectedMenuCategoryPosition)
                previousTab?.customView = null
                val model = getTabView(
                    selectedMenuCategoryPosition,
                    menuPresenter.getMenuCategoryByPosition(selectedMenuCategoryPosition),
                    false
                )
                cardViewList.add(selectedMenuCategoryPosition, model.card)
                previousTab?.customView = model.tab
            }
            if (currentTabPosition < cardViewList.size && currentTabPosition < binding.tabsLayout.tabCount) {
                val currentTab = binding.tabsLayout.getTabAt(currentTabPosition)
                currentTab?.customView = null
                val model = getTabView(
                    currentTabPosition,
                    menuPresenter.getMenuCategoryByPosition(currentTabPosition),
                    true
                )
                cardViewList.add(currentTabPosition, model.card)
                currentTab?.customView = model.tab
            }
            selectedMenuCategoryPosition = currentTabPosition
            activity?.let { fragmentActivity ->
                fragmentActivity.runOnUiThread {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.menuRecyclerView.addOnScrollListener(menuOnScrollListener)
                    }, 250)
                }
            }
        }
    }

    private fun getTabView(
        position: Int, menuCategory: MenuCategory, isSelected: Boolean
    ): TabModel {
        var view: View? = null
        view = if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                Values.IMAGE_TEXT, ignoreCase = true
            )
        ) {
            LayoutInflater.from(context).inflate(R.layout.tab_category_old_text, null)
        } else {
            LayoutInflater.from(context).inflate(R.layout.tab_category_old, null)
        }
        val cardView = view.findViewById<CardView>(R.id.category_image_card)
        cardView.tag = "" + position
        val tabTitle = view.findViewById<TextView>(R.id.tab_title)
        var imageUrl = menuCategory.image
        tabTitle.text = menuCategory.getName(LocaleUtil.getInstance().language)
        context?.let {
            if (isSelected) {
                tabTitle.setTextColor(ContextCompat.getColor(it, R.color.cod_gray))
            } else {
                tabTitle.setTextColor(ContextCompat.getColor(it, R.color.silver_chalice))
            }
            val tabImageView = view.findViewById<ImageView>(R.id.tab_image)
            if (imageUrl == null || imageUrl.equals(
                    "null", ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
                Glide.with(it).load(R.color.transparent).skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).transform(
                        CenterCrop(), RoundedCorners(
                            it.resources.getDimensionPixelSize(R.dimen.category_icon_rounded_corner)
                        )
                    ).into(tabImageView)
            } else {
                imageUrl = StringUtil.getCloudflareUrl(
                    Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl
                )
                Glide.with(it).load(imageUrl).skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).transform(
                        CenterCrop(), RoundedCorners(
                            it.resources.getDimensionPixelSize(R.dimen.category_icon_rounded_corner)
                        )
                    ).into(tabImageView)
            }
        }


        val model = TabModel()
        model.card = cardView
        model.tab = view
        return model
    }

    override fun selectMenuCategory(position: Int, menuCategory: MenuCategory?) {
        selectedHomeMenuCategoryId = ""
        lastCategoryPosition = position
        if (position < binding.tabsLayout.tabCount) {
            activity?.runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.menuRecyclerView.smoothSnapToPosition(position)
                    binding.tabsLayout.selectTab(
                        binding.tabsLayout.getTabAt(position), true
                    )
                }, 250)
            }
            selectTab(position)
        }
    }

    override fun selectMenuItem(menuCategory: MenuCategory?, menuItem: MenuItem?) {
        if (menuCategory != null && menuItem != null) {
            activity?.let {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
                fromMenuItem = true
                val intent = Intent(it, NavigationActivity::class.java)
                intent.putExtra("navigation", R.id.nav_menu_item)
                intent.putExtra("menu_category_id", menuCategory.id)
                intent.putExtra("menu_item_id", menuItem.id)
                intent.putExtra("cart_menu_item_position", -1)
                it.startActivity(intent)
            }
        }
    }

    override fun setMenuCategory(menuCategory: MenuCategory, selected: Boolean) {}
    override fun setMenuItemDetails(menuItem: MenuItem) {}
    override fun setMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {}
    override fun setFavorite(menuItem: MenuItem) {

    }

    override fun showSearchedMenuItems(searchedMenuItems: ArrayList<MenuItem>) {
        binding.newMenuItemSearchShimmer.visibility = View.GONE
        if (searchedMenuItems.isEmpty()) {
            binding.menuItemsSearchList.visibility = View.GONE
            binding.searchEmptyLayout.visibility = View.VISIBLE
        } else {
            binding.menuItemsSearchList.visibility = View.VISIBLE
            binding.searchEmptyLayout.visibility = View.GONE
            searchAdapter = SearchMenuRecyclerViewAdapter(activity, menuPresenter)
            binding.menuItemsSearchList.adapter = searchAdapter
            searchAdapter?.updateMenuList(searchedMenuItems)
        }
    }

    override fun hideSearchedMenuItems(menuCategory: MenuCategory) {}
    override fun setSizes(menuItem: MenuItem) {}
    override fun setModifierItem(
        modifierGroup: ModifierGroup, modifierItem: ModifierItem, position: Int
    ) {
    }

    override fun setModifierItem(
        modifierGroupPosition: Int,
        modifierGroup: ModifierGroup,
        modifierItemPosition: Int,
        modifierItem: ModifierItem,
        showModifierItemPrice: Boolean
    ) {
    }

    override fun setIngredient(ingredient: Ingredient) {}
    override fun setModifierGroup(
        position: Int,
        modifierGroup: ModifierGroup?,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean,
        modifierItemsRecyclerViewAdapterNewHashMap: java.util.HashMap<Int, ModifierItemsNewRecyclerViewAdapter>
    ) {

    }

    override fun setMenuItemQuantity(quantity: Int) {}
    override fun setMenuItemTotalPrice(totalPrice: Double, menuItemPrice: Double) {
        binding.totalPrice.text = "" + totalPrice
    }

    override fun selectSize() {}
    override fun showOrderType() {}
    override fun addCartItem(
        menuItem: MenuItem,
        isShouldBackPressed: Boolean,
        menuAddedToCartPosition: Int
    ) {
        try {
            val baseActivity = activity as BaseActivity?
            baseActivity?.runOnUiThread {
                val vibrator: Vibrator =
                    baseActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500, VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(100)
                    }
                }
                baseActivity.dismissDialogs()
                Toast.makeText(
                    baseActivity,
                    CacheUtil.getInstance()
                        .getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                    Toast.LENGTH_SHORT
                ).show()
                activity?.cast<MainActivity> {
                    it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
                activity?.cast<NavigationActivity> {
                    it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
                updateFloatingBag()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addCartItem(
        menuItem: MenuItem,
        view: ImageView,
        isShouldBackPressed: Boolean
    ) {
        addCartItem(menuItem, isShouldBackPressed,0)
        listener?.animateViewToCart(view, menuItem.attributes.image, 0)
    }

    private fun showShimmers() {
        binding.emptySlider.visibility = View.GONE
        binding.newMenuBannerShimmer.visibility = View.GONE
        binding.sliderViewPager.visibility = View.GONE
        binding.dotsIndicator.visibility = View.GONE
        binding.tabsLayout.visibility = View.GONE
        binding.menuRecyclerView.visibility = View.GONE
        if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                Values.IMAGE_TEXT, ignoreCase = true
            )
        ) {
            try {
                activity?.let {
                    binding.mainToolbar.layoutParams.height =
                        it.resources.getDimensionPixelSize(R.dimen.menu_appbar_height_no_slider_text)
                    setSearchBoxBottomMargin(
                        binding.searchBox,
                        it.resources.getDimensionPixelSize(R.dimen.search_box_bottom_padding_start_text)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.emptySlider.setBackgroundResource(R.color.page_background_color)
            binding.newMenuTabsShimmerText.visibility = View.VISIBLE
            binding.newMenuTabsShimmer.visibility = View.GONE
        } else {
            binding.newMenuTabsShimmerText.visibility = View.GONE
            binding.newMenuTabsShimmer.visibility = View.VISIBLE
        }
        binding.newMenuItemShimmer.visibility = View.VISIBLE
    }

    private fun hideShimmers() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.newMenuItemSearchShimmer.visibility = View.GONE
            binding.newMenuTabsShimmer.visibility = View.GONE
            binding.newMenuTabsShimmerText.visibility = View.GONE
            binding.spaceTwo.visibility = View.GONE
            binding.newMenuBannerShimmer.visibility = View.GONE
            binding.newMenuItemShimmer.visibility = View.GONE
            binding.tabsLayout.visibility = View.VISIBLE
            //binding.viewPager.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.VISIBLE
            binding.blocker.visibility = View.GONE
            if (isFromOrderType && isAdded) {
                showSlides(sliderPresenter.sliders)
            }
        }, 100)
    }

    override fun updateCartItem() {}
    private fun updateFloatingBag() {
        if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cartQuantity == 0) {
            binding.cartQuantity.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
        } else if (binding.cartIcon.visibility == View.VISIBLE) {
            binding.cartQuantity.text =
                StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
            binding.cartQuantity.visibility = View.VISIBLE
            binding.totalContainer.visibility = View.VISIBLE
        }
        updateFloatingTotalPrice()
    }

    override fun showMessage(message: String) {
        try {
            val baseActivity = activity as BaseActivity?
            baseActivity?.runOnUiThread {
                baseActivity.dismissDialogs()
                Toast.makeText(baseActivity, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showError(error: String) {
        try {
            val activity = activity as BaseActivity?
            activity?.runOnUiThread {
                hideShimmers()
                activity.dismissDialogs()
                activity.showAlertDialog("", error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun updateFavoriteByPosition(
        position: Int,
        menuItem: MenuItem?
    ) {}
    override fun showHideModifierItemPrice(show: Boolean, modifierGroupPosition: Int) {}
    override fun showModifierSelectionIndicator(
        hasAtleastOneSelectedItem: Boolean,
        modifierGroupPosition: Int,
        modifierGroupIndexVisibility: java.util.HashMap<String, Int>?,
        modifierItemPosition: Int
    ) {
    }

    override fun showFavorites(fromAddRemove: Boolean, favorites: ArrayList<Favorite>) {}
    override fun setFavorite(
        favorite: Favorite,
        outOfStockLocations: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun showMenuItem(categoryId: String, menuItemId: String) {
        activity?.let {
            it.runOnUiThread {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
                fromMenuItem = true
                val intent = Intent(it, NavigationActivity::class.java)
                intent.putExtra("navigation", R.id.nav_menu_item)
                intent.putExtra("menu_category_id", categoryId)
                intent.putExtra("menu_item_id", menuItemId)
                intent.putExtra("cart_menu_item_position", -1)
                it.startActivity(intent)
            }
        }
    }

    override fun setPresenter(presenter: MenuContract.Presenter) {
        menuPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setCartExpiry() {
        activity?.cast<BaseActivity> {
            it.setCartExpiry()
        }
    }

    override fun showSlides(slides: ArrayList<Slide>) {
        try {
            binding.newMenuBannerShimmer.visibility = View.GONE
            binding.emptySlider.setBackgroundResource(R.color.page_background_white)
            if (slides.size > 0) {
                activity?.let {
                    val sliderAdapter = SliderFragmentStateAdapter(it, sliderPresenter, slides)
                    binding.sliderViewPager.adapter = sliderAdapter
                    binding.emptySlider.visibility = View.GONE
                    binding.sliderViewPager.visibility = View.VISIBLE
                    if (slides.size > 1) {
                        binding.dotsIndicator.setViewPager2(binding.sliderViewPager)
                        binding.dotsIndicator.visibility = View.VISIBLE
                    } else {
                        binding.dotsIndicator.visibility = View.GONE
                    }
                    sliderHandler?.removeCallbacksAndMessages(null)
                    sliderHandler = Handler(Looper.getMainLooper())
                    sliderHandler?.postDelayed(object : Runnable {
                        override fun run() {
                            var pos = binding.sliderViewPager.currentItem + 1
                            if (pos >= slides.size) {
                                pos = 0
                                binding.sliderViewPager.setCurrentItem(pos, false)
                            } else {
                                binding.sliderViewPager.currentItem = pos
                            }
                            sliderHandler!!.postDelayed(this, 3000)
                        }
                    }, 3000)
                }
            } else {
                binding.emptySlider.visibility = View.VISIBLE
                binding.sliderViewPager.visibility = View.VISIBLE
                binding.dotsIndicator.visibility = View.GONE
                properSliderHeights()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun properSliderHeights() {
        try {
            activity?.let {
                sliderHeight = it.resources.getDimensionPixelSize(R.dimen.pixel_twenty_five)
                if (!CacheUtil.getInstance().application.attributes.settings.categoryDisplay.equals(
                        Values.IMAGE_TEXT, ignoreCase = true
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
        }


    }

    override fun showSlidesError() {
        binding.emptySlider.setBackgroundResource(R.color.page_background_white)
        if (sliderPresenter.sliders.size <= 0) {
            properSliderHeights()
        }
    }

    override fun showMidBanner(banners: ArrayList<Slide>) {}
    override fun showLink(uri: Uri) {
        try {
            val activity = activity as BaseActivity?
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

    override fun showMenuItem(menuItemId: String) {
        menuPresenter.showMenuItem(menuItemId)
    }

    override fun showMenuCategory(menuCategoryId: String) {
        menuPresenter.selectMenuCategory(menuCategoryId)
    }

    private fun changeCategoryIconsAlpha() {
        val size = cardViewList.size
        for (i in 0 until size) {
            cardViewList[i]?.alpha = latestButtonAlpha
            cardViewList[i]?.invalidate()
        }
    }

    private fun setSearchBoxBottomMargin(view: View?, bottom: Int) {
        try {
            activity?.let {
                if (view?.layoutParams is ViewGroup.MarginLayoutParams) {
                    val p = view.layoutParams as ViewGroup.MarginLayoutParams
                    p.setMargins(
                        it.resources.getDimensionPixelSize(R.dimen.pixel_ten),
                        0,
                        it.resources.getDimensionPixelSize(
                            R.dimen.pixel_ten
                        ),
                        bottom
                    )
                    view.invalidate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun animateOrderTypeContainer(hide: Boolean) {
        val alphaAnim: AlphaAnimation = if (hide) {
            AlphaAnimation(1.0f, 0f)
        } else {
            AlphaAnimation(0f, 1.0f)
        }
        alphaAnim.duration = 600
        alphaAnim.fillAfter = true
        binding.cardOrderTypeContainer.startAnimation(alphaAnim)
    }

    private fun animateCard(newWidth: Int, show: Boolean) {
        val widthAnimator = ValueAnimator.ofInt(
            binding.cardSearchContainer.width, newWidth
        )
        widthAnimator.duration = 600
        widthAnimator.interpolator = DecelerateInterpolator()
        widthAnimator.addUpdateListener { animation: ValueAnimator ->
            binding.cardSearchContainer.layoutParams.width = animation.getAnimatedValue() as Int
            binding.cardSearchContainer.requestLayout()
        }
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (show) {
                    binding.searchText.requestFocus()
                } else {
                    binding.searchText.clearFocus()
                }
            }
        })
        widthAnimator.start()
    }

    override fun setTabs(adapter: RecyclerView.Adapter<NewMenuViewHolder>) {
        try {
            tabAdapters?.add(adapter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMenuCategoryItemsScrolled(show: Boolean, isLastItemVisible: Boolean) {
        if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cartQuantity == 0) {
            binding.totalContainer.visibility = View.GONE
            return
        }
        context?.let {
            val anim: Animation
            var animType = R.anim.slide_down
            if (show) {
                animType = R.anim.slide_up
            }
            anim = AnimationUtils.loadAnimation(it, animType)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    animDone = false
                }

                override fun onAnimationEnd(animation: Animation) {
                    if (show) {
                        binding.totalContainer.visibility = View.VISIBLE
                    } else {
                        binding.totalContainer.visibility = View.GONE
                    }
                    animDone = true
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })/*if (animDone && ((isLastItemVisible && binding.totalContainer.isShown) || (show && !isLastItemVisible && !binding.totalContainer.isShown) || (!show && !isLastItemVisible && binding.totalContainer.isShown))) {
                binding.totalContainer.startAnimation(anim)
            }*/
            if (animDone && ((show && !binding.totalContainer.isShown) || (!show && binding.totalContainer.isShown))) {
                binding.totalContainer.startAnimation(anim)
            }
        }
    }

    fun notifyAllSavedAdapters() {
        try {
            if (tabAdapters != null) {
                val size = tabAdapters.size
                for (i in 0 until size) {
                    if (tabAdapters[i] != null) {
                        tabAdapters[i]!!.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showBestSellers(menuCategories: ArrayList<MenuItem>) {}
    override fun showLatestOffers(latestOffers: List<Campaign>) {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {}
    override fun setCoupon(campaign: Campaign) {}
    override fun setBestSeller(
        menuItem: MenuItem,
        outOfStockLocation: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun setRecentOrder(
        menuItem: MenuItem,
        outOfStockLocation: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun logFirebaseEvent(event: String) {
        if (Values.sDeepLinkUtm != null) {
            try {
                activity?.let {
                    val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                    firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun saveToCache(key: String, value: String) {
        CacheHelper.save(requireContext(), key, value)
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    override fun showLoader() {}
    override fun updateComponents(
        componentPosition: Int,
        componentModifierItems: List<ComponentModifierItem>,
        previous: Int,
        selected: Int
    ) {
    }

    override fun updateComponentModifierGroups(
        componentPosition: Int, componentModifierItems: List<ComponentModifierItem>, selected: Int
    ) {
    }

    override fun updateOrderTypeContainer(isPreselected: Boolean) {
        binding.cardOrderType.isEnabled = !isPreselected
        onResume()
    }

    override fun logAddToWishlistEvent(menuItem: MenuItem) {
        FacebookAppEventUtil.addToWishlistEvent(requireContext(), menuItem)
    }

    override fun setOriginalMenuItemValue(value: String?) {

    }

    override fun showRecentOrders(menuItems: ArrayList<MenuItem>) {}

    private fun deepLinkRedirect() {
        if (Values.sDeepLink.isEmpty()) {
            if (selectedHomeMenuCategoryId != "") {
                showMenuCategory(selectedHomeMenuCategoryId)
                selectedHomeMenuCategoryId = ""
            }
            return
        }
        var redirected = false
        if (Values.sDeepLink.contains("categories")) {
            redirected = true
            showMenuCategory(Values.sDeepLinkId)
        } else if (Values.sDeepLink.contains("items")) {
            redirected = true
            showMenuItem(Values.sDeepLinkId)
        }
        if (redirected) {
            Values.sDeepLink = ""
            Values.sDeepLinkId = ""
        }

    }

    override fun onLastItemReach(isScrollable: Boolean, scrollDirection: ScrollDirection) {
        if (CacheUtil.getInstance().cartQuantity == 0) {
            binding.totalContainer.visibility = View.GONE
            return
        }
        val anim: Animation
        var animType = R.anim.slide_down
        if (scrollDirection == ScrollDirection.Down) {
            animType = R.anim.slide_up
        }
        if (!isScrollable && isCollapsed) {
            animType = R.anim.slide_down
        }
        anim = AnimationUtils.loadAnimation(requireContext(), animType)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                animDone = false
            }

            override fun onAnimationEnd(animation: Animation) {
                if (isScrollable && scrollDirection == ScrollDirection.Up) {
                    binding.totalContainer.visibility = View.GONE
                } else if (isScrollable && scrollDirection == ScrollDirection.Down) {
                    if (!CacheUtil.getInstance().application.attributes.menuOnly && CacheUtil.getInstance().cartQuantity > 0) {
                        binding.totalContainer.visibility = View.VISIBLE
                    } else {
                        binding.totalContainer.visibility = View.GONE
                    }
                } else if (!isScrollable && !isCollapsed) {
                    binding.totalContainer.visibility = View.GONE
                } else if (!isScrollable && isCollapsed) {
                    if (!CacheUtil.getInstance().application.attributes.menuOnly && CacheUtil.getInstance().cartQuantity > 0) {
                        binding.totalContainer.visibility = View.VISIBLE
                    } else {
                        binding.totalContainer.visibility = View.GONE
                    }
                }
                animDone = true
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        if (animDone) {
            if (isScrollable && binding.totalContainer.isShown && scrollDirection == ScrollDirection.Up || isScrollable && scrollDirection == ScrollDirection.Down && !binding.totalContainer.isShown) {
                binding.totalContainer.startAnimation(anim)
            }
        }
    }

    fun showCountries() {
        setFragmentResultListener(CountriesFragment.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
            clearFragmentResultListener(requestKey = CountriesFragment.ARGS_KEY_FRAGMENT_RESULT)
            if (bundle.getBoolean(CountriesFragment.ARGS_KEY_FRAGMENT_RELOAD, true)) {
                activity?.cast<MainActivity> {
                    it.setupLanguage()
                    it.multibrandValidation(false)
                }
                isFromCountries = true
                lastCategoryPosition = -1
                loadInitial()
            }
        }/*NavigationUtils.navigateSafe(
            findNavController(), NewMenuFragmentDirections.actionMenuToCountries()
        )*/
    }

    fun showOrder(orderId: String) {/*NavigationUtils.navigateSafe(
            findNavController(), NewMenuFragmentDirections.actionMenuToOrder(orderId)
        )*/
    }

    override fun setMenuCategoryForAnalytics(
        menuItem: MenuItem?, menuCategory: MenuCategory?, price: Double, quantity: Int
    ) {
        FirebaseECommerceEventUtil.addToCartEvent(
            context, menuItem!!, menuCategory, price, quantity
        )
    }

    fun refreshFavorites() {
        menuCategoriesAdapter.notifyDataSetChanged()
    }

}