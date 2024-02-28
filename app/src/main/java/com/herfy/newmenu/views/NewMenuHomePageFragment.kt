package com.herfy.newmenu.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.FacebookAppEventUtil
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.browser.BrowserActivity
import com.herfy.country.CountriesFragment
import com.herfy.databinding.FragmentMenuNewHomePageBinding
import com.herfy.ext.cast
import com.herfy.feedback.FeedbackActivity
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.menu.MenuContract
import com.herfy.menu.MenuPresenter
import com.herfy.newmenu.NewMenuFragmentCommonListener
import com.herfy.newmenu.NewMenuTabListener
import com.herfy.newmenu.SliderContract
import com.herfy.newmenu.SliderPresenter
import com.herfy.newmenu.adapter.MenuCouponRecyclerViewAdapter
import com.herfy.newmenu.adapter.MenuFavoritesRecyclerViewAdapter
import com.herfy.newmenu.adapter.MenuPopularRecyclerViewAdapter
import com.herfy.newmenu.adapter.MenuRecentOrderRecyclerViewAdapter
import com.herfy.newmenu.adapter.NewHomeMenuAdapter
import com.herfy.newmenu.adapter.NewHomeMenuAdapter.NewHomeMenuAdapterListener
import com.herfy.newmenu.adapter.SearchMenuRecyclerViewAdapter
import com.herfy.newmenu.adapter.SliderFragmentStateAdapter
import com.herfy.newmenu.viewholder.NewMenuViewHolder
import com.herfy.popup.PopupFragment
import com.herfy.util.CacheHelper
import com.herfy.util.StringUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
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

class NewMenuHomePageFragment : BaseFragment(), MenuContract.View, SliderContract.View,
    NewMenuTabListener, NewHomeMenuAdapterListener {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_HISTORY = "history"
        const val ARGS_KEY_ACCOUNT = "account"
    }

    private lateinit var binding: FragmentMenuNewHomePageBinding

    private lateinit var menuPresenter: MenuContract.Presenter
    private lateinit var sliderPresenter: SliderContract.Presenter

    private var searchAdapter: SearchMenuRecyclerViewAdapter? = null
    var position = 0

    private var newHomeMenuAdapter: NewHomeMenuAdapter? = null
    private var searchLinearLayoutManager: LinearLayoutManager? = null
    private var firstLaunch = true
    private val appBarIsLock = false
    private var dontAnimate = false
    private val cardViewList: MutableList<CardView> = ArrayList()
    private val latestButtonAlpha = 1f
    private var listener: NewMenuFragmentCommonListener? = null
    private var sliderHandler: Handler? = null
    private var midSliderHandler: Handler? = null
    private var menuFavoritesRecyclerViewAdapter: MenuFavoritesRecyclerViewAdapter? = null
    private var menuPopularRecyclerViewAdapter: MenuPopularRecyclerViewAdapter? = null
    private var menuCouponRecyclerViewAdapter: MenuCouponRecyclerViewAdapter? = null
    private var menuRecentOrderRecyclerViewAdapter: MenuRecentOrderRecyclerViewAdapter? = null
    private var hasMidSlides = false
    private var sliderHeight = 0
    private var isFromCountries = false
    private var isFromOrderType = false
    private var previousSelectedStoreId: String? = null
    private var fromInitialLaunch = false
    private var selectOrderTypeDialog: SelectOrderTypeDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = activity as MainActivity?
        menuPresenter = MenuPresenter(this)
        sliderPresenter = SliderPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuNewHomePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun checkBackground() {
        binding.nestedScrollView.setBackgroundResource(R.color.page_background_color)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkBackground()
        menuPresenter!!.start()
        fromInitialLaunch = true
        if (firstLaunch) {
            loadInitial()
        } else if (!isFromCountries && !isFromOrderType) {
            showLatestOffers(menuPresenter!!.latestOffersList)
            showBestSellers(menuPresenter!!.bestSellers)
            val isMenuSeparate = CacheUtil.getInstance().isSeparatedMenus
            var forceLocationMenuRefresh = false
            activity?.cast<MainActivity> {
                forceLocationMenuRefresh = it.forceLocationMenuRefresh
                it.forceLocationMenuRefresh = false
            }
            val fromDeeplink =
                Values.sDeepLink.isNotEmpty() && (Values.sDeepLink.equals("categories") || Values.sDeepLink.equals(
                    "items"
                ))
            var reloadDifferentMenu = CacheUtil.getInstance().reloadDifferentMenu
            if (isMenuSeparate && reloadDifferentMenu) {
                binding.newMenuItemSearchShimmer.visibility = View.VISIBLE
                binding.newMenuItemSearchShimmer.startShimmer()
                menuPresenter!!.getMenuCategories(
                    forceLocationMenuRefresh,
                    true,
                    fromDeeplink,
                    isFromCountries || isFromOrderType,
                    true,
                    null,
                    null
                )
                CacheUtil.getInstance().reloadDifferentMenu = false
            } else {
                showMenuCategories(
                    false,
                    isFromInitialLoad = false,
                    menuCategories = menuPresenter!!.menuCategories
                )
            }


            if (sliderPresenter!!.sliders.size > 0) {
                showSlides(sliderPresenter!!.sliders)
            } else {
                sliderPresenter!!.getSlider()
            }
            if (sliderPresenter!!.midSliders.size > 0) {
                showMidBanner(sliderPresenter!!.midSliders)
            } else {
                sliderPresenter!!.getMidSlider()
            }
        } else {
            if (isFromOrderType && menuPresenter != null) {
                menuPresenter.updateCart(true)
            }
            isFromCountries = false
            isFromOrderType = false
        }

        if (CacheUtil.getInstance().orderStore == null && CacheUtil.getInstance().application.attributes.forceLocation != 0 || CacheUtil.getInstance().application.attributes.forceLocation == 0) {
            menuPresenter.setPreselectedBranch()
        } else {
            CacheUtil.getInstance().preselectedBranch = false
        }

        if (BuildConfig.FEATURE_PHASE >= 1.3f) {
            CacheUtil.getInstance().popup?.let {
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
                NavigationUtils.navigateSafe(
                    findNavController(), NewMenuHomePageFragmentDirections.actionHomeToDialogPopup()
                )
            }
        }
    }

    private fun loadInitial() {
        binding.blocker.visibility = View.VISIBLE
        var forceLocationMenuRefresh = false
        activity?.cast<MainActivity> {
            forceLocationMenuRefresh = it.forceLocationMenuRefresh
            it.forceLocationMenuRefresh = false
        }
        val fromDeeplink =
            Values.sDeepLink.isNotEmpty() && (Values.sDeepLink.equals("categories") || Values.sDeepLink.equals(
                "items"
            ))

        menuPresenter!!.getMenuCategories(
            forceLocationMenuRefresh,
            true,
            fromDeeplink,
            isFromCountries || isFromOrderType,
            true,
            null,
            null
        )
        if (!isFromOrderType) {
            menuPresenter!!.getBestSeller()
            menuPresenter!!.getGuestDigitalCoupons()
            sliderPresenter!!.getSlider()
            sliderPresenter!!.getMidSlider()
        } else {
            showLatestOffers(menuPresenter!!.latestOffersList)
            showBestSellers(menuPresenter!!.bestSellers)
            showSlides(sliderPresenter!!.sliders)
            showMidBanner(sliderPresenter!!.midSliders)
        }
    }

    override fun onResume() {
        super.onResume()
        checkDiffMenu()
        update()
        if (!isFromOrderType) {
            if (AuthUtil.getInstance().isSignedIn) {
                if (firstLaunch) {
                    menuPresenter!!.getFavorites(false, 1)
                    menuPresenter!!.getRecentOrder()
                    firstLaunch = false
                } else {
                    showFavorites(false, menuPresenter!!.favorites)
                    menuPresenter!!.getFavorites(false, 1)
                    showRecentOrders(menuPresenter!!.recentOrders)
                    menuPresenter!!.getRecentOrder()
                }
            } else {
                binding.favoritesItemList.visibility = View.GONE
                binding.tvViewAllFavorites.visibility = View.GONE
                binding.favoritesLabel.visibility = View.GONE
                binding.recentOrderLabelViewAll.visibility = View.GONE
                binding.recentOrderLabel.visibility = View.GONE
                binding.orderAgainItemList.visibility = View.GONE
            }
        } else {
            isFromOrderType = false
            val isMenuSeparate = CacheUtil.getInstance().isSeparatedMenus
            if (isMenuSeparate || (!fromInitialLaunch && BuildUtil.isEkfc() && !isFromCountries)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showLoadingDialog()
                    loadInitial()
                }, 300)
            } else {
                if (CacheUtil.getOrderStoreId() != null && previousSelectedStoreId != CacheUtil.getOrderStoreId()) {
                    activity?.runOnUiThread {
                        newHomeMenuAdapter?.updateMenuCategories(menuPresenter.menuCategories)
                    }
                }
            }
        }
        fromInitialLaunch = false
    }

    private fun checkDiffMenu() {
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
    }

    private fun handleDiffMenu(orderType: OrderType) {
        isFromOrderType = true
        previousSelectedStoreId = CacheUtil.getOrderStoreId()
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

    @SuppressLint("SetTextI18n")
    override fun update() {
        super.update()
        menuPresenter.getStoreOpen()
        when (val oT = CacheUtil.getInstance().orderType) {
            OrderType.PICKUP, OrderType.EAT_IN, OrderType.DRIVE_THRU -> if (activity != null) {
                menuPresenter.getStore()
                if (CacheUtil.getInstance().orderStore != null) {
                    var headerText = CacheUtil.getInstance()
                        .getTranslations(Text.PICK_UP_FROM, "PICK UP FROM").uppercase(
                            Locale.getDefault()
                        ) + ":"
                    if (oT == OrderType.EAT_IN) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup").uppercase(
                                Locale.getDefault()
                            ) + ":"
                    } else if (oT == OrderType.DRIVE_THRU) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                            .uppercase(
                                Locale.getDefault()
                            ) + ":"
                    }
                    binding.orderType.text = headerText
                    binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    binding.orderType.visibility = View.VISIBLE
                } else {
                    binding.orderType.visibility = View.GONE
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

            OrderType.DINE_IN -> if (activity != null) {
                menuPresenter.getStore()
                if (CacheUtil.getInstance().orderStore != null) {
                    binding.orderType.text =
                        CacheUtil.getInstance().getTranslations(Text.DINE_IN_AT, "DINE-IN AT")
                            .uppercase(
                                Locale.getDefault()
                            ) + ":"
                    binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    binding.orderType.visibility = View.VISIBLE
                } else {
                    binding.orderType.visibility = View.GONE
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

            OrderType.DELIVERY -> if (activity != null) {
                menuPresenter!!.getAddress()
                if (CacheUtil.getInstance().orderAddress != null) {
                    binding.orderType.text = CacheUtil.getInstance()
                        .getTranslations(Text.DELIVER_TO_CAPS, "DELIVER TO").uppercase(
                            Locale.getDefault()
                        ) + ":"
                    binding.branchName.text =
                        CacheUtil.getInstance().orderAddress.attributes.line1
                    binding.orderType.visibility = View.VISIBLE
                } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                    binding.orderType.text = CacheUtil.getInstance()
                        .getTranslations(Text.DELIVER_TO_CAPS, "DELIVER TO").uppercase(
                            Locale.getDefault()
                        ) + ":"
                    binding.branchName.text = CacheUtil.getInstance().orderDeliveryAddress
                    binding.orderType.visibility = View.VISIBLE
                } else {
                    binding.orderType.visibility = View.GONE
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

            OrderType.CURBSIDE -> if (activity != null) {
                menuPresenter!!.getStore()
                if (CacheUtil.getInstance().orderStore != null) {
                    binding.orderType.text = CacheUtil.getInstance()
                        .getTranslations(Text.CURBSIDE_PICKUP_FROM_CAPS, "CURBSIDE PICKUP FROM")
                        .uppercase(
                            Locale.getDefault()
                        ) + ":"
                    binding.branchName.text = CacheUtil.getInstance().orderStore.attributes.name
                    binding.orderType.visibility = View.VISIBLE
                } else {
                    binding.orderType.visibility = View.GONE
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
                binding.orderType.visibility = View.GONE
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
        if (CacheUtil.getInstance().favoriteMenuCategory != null && CacheUtil.getInstance().favoriteMenuItem != null) {
            menuPresenter!!.menuCategory = CacheUtil.getInstance().favoriteMenuCategory
            menuPresenter!!.menuItem = CacheUtil.getInstance().favoriteMenuItem
            setMenuItem(menuPresenter!!.menuCategory, menuPresenter!!.menuItem)
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
            menuPresenter!!.addRemoveFavorite(true)
        }
        if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cartQuantity == 0 || CacheUtil.getInstance().application.attributes.homeScreenDesign.equals(
                "option-4", ignoreCase = true
            )
        ) {
            binding.cartQuantity.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
        } else if (binding.cartIcon.visibility == View.VISIBLE) {
            binding.cartQuantity.text =
                StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
            binding.cartQuantity.visibility = View.VISIBLE
            binding.totalContainer.visibility = View.VISIBLE
        }
        if (CacheUtil.getInstance().cart.size > 0) {
            if (LocaleUtil.getInstance().isArabic) {
                binding.totalPrice.text = CacheUtil.getInstance()
                    .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(
                    menuPresenter!!.menuItemsTotalPrice
                )
            } else {
                binding.totalPrice.text = StringUtil.getFormattedPrice(
                    menuPresenter!!.menuItemsTotalPrice
                ).toString() + " " + CacheUtil.getInstance().getTranslations(Text.TOTAL, "TOTAL")
            }
        } else {
            if (LocaleUtil.getInstance().isArabic) {
                binding.totalPrice.text = CacheUtil.getInstance()
                    .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(0.0)
            } else {
                binding.totalPrice.text =
                    StringUtil.getFormattedPrice(0.0).toString() + " " + CacheUtil.getInstance()
                        .getTranslations(
                            Text.TOTAL, "TOTAL"
                        )
            }
        }
        if (menuFavoritesRecyclerViewAdapter != null) {
            binding.favoritesItemList.recycledViewPool.clear()
            menuFavoritesRecyclerViewAdapter!!.notifyDataSetChanged()
        }
        if (menuRecentOrderRecyclerViewAdapter != null) {
            if (menuRecentOrderRecyclerViewAdapter!!.itemCount == 0) {
                binding.recentOrderLabelViewAll.visibility = View.GONE
                binding.recentOrderLabel.visibility = View.GONE
                binding.orderAgainItemList.visibility = View.GONE
            } else {
                binding.recentOrderLabelViewAll.visibility = View.VISIBLE
                binding.recentOrderLabel.visibility = View.VISIBLE
                binding.orderAgainItemList.visibility = View.VISIBLE
            }
            menuRecentOrderRecyclerViewAdapter!!.notifyDataSetChanged()
        }
        if (menuPopularRecyclerViewAdapter != null) {
            menuPopularRecyclerViewAdapter!!.notifyDataSetChanged()
        }
    }

    override fun setupViews() {
        binding.cardOrderType.setOnClickListener {
            isFromOrderType = true
            previousSelectedStoreId = CacheUtil.getOrderStoreId()
            activity?.let {
                if (it is MainActivity) {
                    it.forceLocation = false
                }
            }
            if (CacheUtil.getInstance().orderType != OrderType.CURBSIDE) {
                CacheUtil.getInstance().curbsideCar = null
            }/*setFragmentResultListener(OrderTypeFragment.ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                clearFragmentResultListener(requestKey = OrderTypeFragment.ARGS_KEY_FRAGMENT_RESULT)
                if (CacheUtil.getOrderStoreId() != null) {
                    isFromOrderType = true
                    if (!bundle.getBoolean(OrderTypeFragment.ARGS_KEY_IS_SAME_BRANCH, false)) {
                        loadInitial()
                    }
                }
            }*/
            val storeId = CacheUtil.getOrderStoreId() ?: ""/*NavigationUtils.navigateSafe(
                findNavController(),
                NewMenuHomePageFragmentDirections.actionHomeToOrderType(
                    fromCart = false,
                    checkout = false,
                    storeId = storeId
                )
            )*/
            val intent = Intent(activity, NavigationActivity::class.java)
            intent.putExtra("navigation", R.id.nav_order_type)
            intent.putExtra("fromCart", false)
            intent.putExtra("checkout", false)
            intent.putExtra("storeId", storeId)
            activity?.startActivity(intent)
        }
        binding.cardClose.setOnClickListener {
            if (binding.cardClose.visibility == View.VISIBLE) {
                binding.searchEmptyLayout.visibility = View.GONE
                //binding.noSearchLayout.visibility = View.VISIBLE
                binding.withSearchLayout.visibility = View.GONE
                if (!dontAnimate) {
                    //appBarLayout.setExpanded(true, true);
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
                dontAnimate = false
            }
        }
        binding.cardSearch.setOnClickListener {
            if (binding.cardClose.visibility != View.VISIBLE) {
                binding.searchText.setText("")
                binding.cardClose.visibility = View.VISIBLE
                binding.searchText!!.visibility = View.VISIBLE
                //appBarLayout.setExpanded(false, true);
                animateCard((binding.cardSearchContainer.parent as View).measuredWidth, true)
                animateOrderTypeContainer(true)
            }
        }
        binding.blocker.setOnClickListener { }
        binding.withSearchLayout.setOnClickListener { }
        if (CacheUtil.getInstance().application.attributes.homeScreenDesign.equals(
                "option-4", ignoreCase = true
            )
        ) {
            binding.appBarLayout.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
            binding.homeOption4.root.visibility = View.VISIBLE
        } else {
            binding.appBarLayout.visibility = View.VISIBLE
            if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cart.isEmpty()) {
                binding.totalContainer.visibility = View.GONE
            } else {
                binding.totalContainer.visibility = View.VISIBLE
            }
        }
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
                binding.midSliderContainer.layoutParams.height = sliderHeight
                binding.midSliderViewPager.layoutParams.height = sliderHeight
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.searchText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    if (binding.cardClose.visibility == View.VISIBLE) {
                        if (!appBarIsLock) {
                            //  appBarLayout.setExpanded(false, true);
                        }
                        // We lock the scrolling when in search mode
                        // and we do not show the list of items
                        // with categories
                        //binding.noSearchLayout!!.visibility = View.GONE
                        binding.withSearchLayout!!.visibility = View.VISIBLE
                        // We check below if search items size is == 0
                        // then show shimmer and hide empty view
                        // we dont want them to show at the same time.
                        if (menuPresenter!!.searchItemsSize == 0) {
                            binding.searchEmptyLayout.visibility = View.GONE
                            binding.newMenuItemSearchShimmer.visibility = View.VISIBLE
                        }
                        menuPresenter!!.search(editable.toString())
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
        binding.tvViewAllFavorites.setOnClickListener {
            NavigationUtils.navigateSafe(
                findNavController(), NewMenuHomePageFragmentDirections.actionHomeToFavorites()
            )
        }
        binding.history.setOnClickListener {
            if (AuthUtil.getInstance().isSignedIn) {
                NavigationUtils.navigateSafe(
                    findNavController(), NewMenuHomePageFragmentDirections.actionHomeToOrders()
                )
            } else {
                setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                    clearFragmentResultListener(requestKey = ARGS_KEY_FRAGMENT_RESULT)
                    if (AuthUtil.getInstance().isSignedIn && bundle.containsKey(ARGS_KEY_HISTORY) && bundle.getBoolean(
                            ARGS_KEY_HISTORY
                        )
                    ) {
                        NavigationUtils.navigateSafe(
                            findNavController(),
                            NewMenuHomePageFragmentDirections.actionHomeToOrders()
                        )
                    }
                }
                NavigationUtils.navigateSafe(
                    findNavController(), NewMenuHomePageFragmentDirections.actionHomeToAuth(
                        signIn = true,
                        guest = false,
                        history = true,
                        account = false,
                        curbside = false,
                        checkout = false
                    )
                )
            }
        }
        binding.feedback.setOnClickListener {
            val intent: Intent
            val feedbackUrl = CacheUtil.getInstance().concept.feedbackRedirectUrl
            if (feedbackUrl != null && feedbackUrl.isNotEmpty()) {
                if (StringUtil.isValidEmail(feedbackUrl)) {
                    intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackUrl))
                    intent.type = "text/plain"
                } else {
                    intent = Intent(activity, BrowserActivity::class.java)
                    intent.putExtra(
                        "title", CacheUtil.getInstance().getTranslations(
                            Text.FEEDBACK
                        )
                    )
                    intent.putExtra("url", feedbackUrl)
                }
            } else {
                intent = Intent(activity, FeedbackActivity::class.java)
            }
            if (feedbackUrl != null && StringUtil.isValidEmail(feedbackUrl)) {
                startActivity(Intent.createChooser(intent, "Select your Email app"))
            } else startActivity(intent)
        }
        binding.homeOption4.leaveReview.setOnClickListener {
            val intent: Intent
            val feedbackUrl = CacheUtil.getInstance().concept.feedbackRedirectUrl
            if (feedbackUrl != null && !feedbackUrl.isEmpty()) {
                if (StringUtil.isValidEmail(feedbackUrl)) {
                    intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackUrl))
                    intent.type = "text/plain"
                } else {
                    intent = Intent(activity, BrowserActivity::class.java)
                    intent.putExtra(
                        "title", CacheUtil.getInstance().getTranslations(
                            Text.FEEDBACK
                        )
                    )
                    intent.putExtra("url", feedbackUrl)
                }
            } else {
                intent = Intent(activity, FeedbackActivity::class.java)
            }
            if (feedbackUrl != null && StringUtil.isValidEmail(feedbackUrl)) {
                startActivity(Intent.createChooser(intent, "Select your Email app"))
            } else startActivity(intent)
            activity?.let {
                it.overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
            }
        }
        binding.account.setOnClickListener {
            if (AuthUtil.getInstance().isSignedIn) {
                NavigationUtils.navigateSafe(
                    findNavController(), NewMenuHomePageFragmentDirections.actionHomeToAccount()
                )
            } else {
                setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                    clearFragmentResultListener(requestKey = ARGS_KEY_FRAGMENT_RESULT)
                    if (AuthUtil.getInstance().isSignedIn && bundle.containsKey(ARGS_KEY_ACCOUNT) && bundle.getBoolean(
                            ARGS_KEY_ACCOUNT
                        )
                    ) {
                        NavigationUtils.navigateSafe(
                            findNavController(),
                            NewMenuHomePageFragmentDirections.actionHomeToAccount()
                        )
                    }
                }
                NavigationUtils.navigateSafe(
                    findNavController(), NewMenuHomePageFragmentDirections.actionHomeToAuth(
                        signIn = true,
                        guest = false,
                        history = false,
                        account = true,
                        curbside = false,
                        checkout = false
                    )
                )
            }
        }
        binding.cart.setOnClickListener(View.OnClickListener {
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                return@OnClickListener
            }
            val intent = Intent(activity, NavigationActivity::class.java)
            intent.putExtra("navigation", R.id.nav_cart)
            intent.putExtra("isBranchPreselected", menuPresenter.isPreselectedBranch)
            activity?.startActivity(intent)
        })
        binding.homeOption4.checkOutMenu.setOnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
            val request =
                NavDeepLinkRequest.Builder.fromUri(Uri.parse("${Values.NAV_DEEP_LINK_BASE_URL}nav_menu"))
                    .build()
            NavigationUtils.navigateSafe(findNavController(), request)
        }
        binding.homeOption4.findBranches.setOnClickListener {
            NavigationUtils.navigateSafe(
                findNavController(), NewMenuHomePageFragmentDirections.actionHomeToBranches()
            )
        }
        binding.browseMenu.setOnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
            val request =
                NavDeepLinkRequest.Builder.fromUri(Uri.parse("${Values.NAV_DEEP_LINK_BASE_URL}nav_menu"))
                    .build()
            NavigationUtils.navigateSafe(findNavController(), request)
        }
        binding.totalContainer.setOnClickListener(View.OnClickListener {
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                return@OnClickListener
            }
            val intent = Intent(activity, NavigationActivity::class.java)
            intent.putExtra("navigation", R.id.nav_cart)
            intent.putExtra("isBranchPreselected", menuPresenter.isPreselectedBranch)
            activity?.startActivity(intent)
        })
        binding.popularNowViewAll.setOnClickListener {
            val popularNowItems = Gson().toJson(menuPresenter!!.bestSellers)
            NavigationUtils.navigateSafe(
                findNavController(),
                NewMenuHomePageFragmentDirections.actionHomeToPopularNow(popularNowItems)
            )
        }
        binding.recentOrderLabelViewAll!!.setOnClickListener {
            try {
                val activity = activity as MainActivity?
                activity?.showHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.homeOption4.findPromo.setOnClickListener {
            NavigationUtils.navigateSafe(
                findNavController(), NewMenuHomePageFragmentDirections.actionHomeToDigitalCoupons()
            )
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.homeOption4.checkOut.text =
            CacheUtil.getInstance().getTranslations(Text.CHECK_OUT_CAPS)
        binding.homeOption4.ourMenu.text =
            CacheUtil.getInstance().getTranslations(Text.OUR_MENU_CAPS)
        binding.homeOption4.findA.text = CacheUtil.getInstance().getTranslations(Text.FIND_A_CAPS)
        binding.homeOption4.promo.text = CacheUtil.getInstance().getTranslations(Text.PROMO_CAPS)
        binding.homeOption4.findOur.text =
            CacheUtil.getInstance().getTranslations(Text.FIND_OUR_CAPS)
        binding.homeOption4.branches.text = CacheUtil.getInstance().getTranslations(Text.BRANCHES)
        binding.homeOption4.leaveUs.text =
            CacheUtil.getInstance().getTranslations(Text.LEAVE_US_CAPS)
        binding.homeOption4.aReview.text =
            CacheUtil.getInstance().getTranslations(Text.A_REVIEW_CAPS)
        binding.searchEmptyText.text =
            CacheUtil.getInstance().getTranslations(Text.NO_ITEM_FOUND, "No item found")
        binding.searchEmptyTextDesc.text = CacheUtil.getInstance().getTranslations(
            Text.NO_ITEM_FOUND_DESC,
            "Sorry, we were unable to find something that matches your search."
        )
        binding.browseMenuLabel.text =
            CacheUtil.getInstance().getTranslations(Text.BROWSE_MENU_CAPS)
        binding.categoriesLabel.text =
            CacheUtil.getInstance().getTranslations(Text.BROWSE_MENU_CAPS)
        binding.favoritesLabel.text = CacheUtil.getInstance().getTranslations(Text.FAVORITES_CAPS)
        binding.tvViewAllFavorites.text =
            CacheUtil.getInstance().getTranslations(Text.VIEW_ALL_CAPS)
        binding.couponLabel.text = CacheUtil.getInstance().getTranslations(Text.COUPONS_CAPS)
        binding.recentOrderLabel.text =
            CacheUtil.getInstance().getTranslations(Text.ORDER_AGAIN_CAPS, "ORDER AGAIN")
        binding.recentOrderLabelViewAll.text =
            CacheUtil.getInstance().getTranslations(Text.VIEW_ALL_CAPS)
        binding.popularNowLabel.text =
            CacheUtil.getInstance().getTranslations(Text.POPULAR_NOW_CAPS, "POPULAR NOW")
        binding.popularNowViewAll.text = CacheUtil.getInstance().getTranslations(Text.VIEW_ALL_CAPS)
        binding.historyLabel.text = CacheUtil.getInstance().getTranslations(Text.HISTORY_CAPS)
        binding.feedbackLabel.text = CacheUtil.getInstance().getTranslations(Text.FEEDBACK_CAPS)
        binding.accountLabel.text = CacheUtil.getInstance().getTranslations(Text.ACCOUNT_CAPS)
        binding.btnViewBag.text = CacheUtil.getInstance().getTranslations(Text.VIEW_BAG_CAPS)
    }

    override fun showStore(store: Store) {}
    override fun showStoreOpen(open: Boolean, message: String) {}
    override fun showAddress(address: String) {}
    override fun reloadMenu() {
        menuPresenter!!.reloadMenu()
    }

    override fun showMenuCategories(
        refresh: Boolean, isFromInitialLoad: Boolean, menuCategories: MutableList<MenuCategory>?
    ) {
        try {
            activity?.cast<BaseActivity> {
                it.runOnUiThread {
                    if (menuCategories?.isNotEmpty() == true) {
                        hideShimmers()
                        binding.menuCategoryItems.visibility = View.VISIBLE
                        if (newHomeMenuAdapter == null) {
                            newHomeMenuAdapter = NewHomeMenuAdapter(
                                it, menuCategories, this@NewMenuHomePageFragment
                            )
                            newHomeMenuAdapter!!.selected = position
                            binding.menuCategoryItems.adapter = newHomeMenuAdapter
                            newHomeMenuAdapter!!.notifyDataSetChanged()
                        } else {
                            if (binding.menuCategoryItems.adapter == null) {
                                binding.menuCategoryItems.adapter = newHomeMenuAdapter
                            }
                            newHomeMenuAdapter?.updateMenuCategories(menuCategories)
                        }
                        deepLinkRedirect()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun selectMenuCategory(position: Int, menuCategory: MenuCategory) {
        selectItem(menuCategory, position)
    }

    override fun selectMenuItem(menuCategory: MenuCategory?, menuItem: MenuItem?) {
        if (menuCategory != null && menuItem != null) {
            activity?.let {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
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
        modifierGroup: ModifierGroup,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean
    ) {
    }

    override fun setMenuItemQuantity(quantity: Int) {}
    override fun setMenuItemTotalPrice(totalPrice: Double) {}
    override fun selectSize() {}
    override fun showOrderType() {}
    override fun addCartItem(menuItem: MenuItem) {
        try {
            val activity = activity as MainActivity?
            activity?.runOnUiThread {
                val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                activity.dismissDialogs()
                Toast.makeText(
                    activity,
                    CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART),
                    Toast.LENGTH_SHORT
                ).show()
                activity.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                updateFloatingBag()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addCartItem(menuItem: MenuItem, view: ImageView) {
        addCartItem(menuItem)
        listener?.animateViewToCart(view, menuItem.attributes.image, 0)
    }

    private fun hideShimmers() {
        dismissDialogs()
        binding.newMenuItemSearchShimmer.visibility = View.GONE
        binding.blocker.visibility = View.GONE
    }

    fun updateFloatingBag() {
        if (CacheUtil.getInstance().application.attributes.menuOnly || CacheUtil.getInstance().cartQuantity == 0) {
            binding.cartQuantity.visibility = View.GONE
            binding.totalContainer.visibility = View.GONE
        } else if (binding.cartIcon.visibility == View.VISIBLE) {
            binding.cartQuantity.text =
                StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
            binding.cartQuantity.visibility = View.VISIBLE
            if (!CacheUtil.getInstance().application.attributes.homeScreenDesign.equals(
                    "option-4", ignoreCase = true
                )
            ) {
                binding.totalContainer.visibility = View.VISIBLE
            }
        }
        if (CacheUtil.getInstance().cart.size > 0) {
            if (LocaleUtil.getInstance().isArabic) {
                binding.totalPrice.text = CacheUtil.getInstance()
                    .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(
                    menuPresenter!!.menuItemsTotalPrice
                )
            } else {
                binding.totalPrice.text = StringUtil.getFormattedPrice(
                    menuPresenter!!.menuItemsTotalPrice
                ).toString() + " " + CacheUtil.getInstance().getTranslations(Text.TOTAL, "TOTAL")
            }
        } else {
            if (LocaleUtil.getInstance().isArabic) {
                binding.totalPrice.text = CacheUtil.getInstance()
                    .getTranslations(Text.TOTAL, "TOTAL") + " " + StringUtil.getFormattedPrice(0.0)
            } else {
                binding.totalPrice.text =
                    StringUtil.getFormattedPrice(0.0).toString() + " " + CacheUtil.getInstance()
                        .getTranslations(
                            Text.TOTAL, "TOTAL"
                        )
            }
        }
    }

    override fun updateCartItem() {}
    override fun showMessage(message: String) {
        try {
            val mainActivity = activity as MainActivity?
            mainActivity?.runOnUiThread {
                mainActivity.dismissDialogs()
                Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showError(error: String) {
        try {
            val mainActivity = activity as MainActivity?
            mainActivity?.runOnUiThread {
                hideShimmers()
                mainActivity.dismissDialogs()
                mainActivity.showAlertDialog("", error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showHideModifierItemPrice(show: Boolean, modifierGroupPosition: Int) {}
    override fun showModifierSelectionIndicator(
        hasAtleastOneSelectedItem: Boolean, modifierGroupPosition: Int
    ) {
        TODO("Not yet implemented")
    }

    fun refreshFavorites() {
        try {
            menuFavoritesRecyclerViewAdapter?.notifyDataSetChanged()
            menuPopularRecyclerViewAdapter?.notifyDataSetChanged()
            menuRecentOrderRecyclerViewAdapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showFavorites(fromAddRemove: Boolean, favoriteItems: ArrayList<Favorite>) {
        if (favoriteItems.isNotEmpty()) {
            binding.favoritesItemList.visibility = View.VISIBLE
            binding.tvViewAllFavorites.visibility = View.VISIBLE
            binding.favoritesLabel.visibility = View.VISIBLE
            menuFavoritesRecyclerViewAdapter =
                MenuFavoritesRecyclerViewAdapter(activity, menuPresenter)
            binding.favoritesItemList.layoutManager = LinearLayoutManager(
                activity, LinearLayoutManager.HORIZONTAL, false
            )
            binding.favoritesItemList.adapter = menuFavoritesRecyclerViewAdapter
            menuFavoritesRecyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            binding.favoritesItemList.visibility = View.GONE
            binding.tvViewAllFavorites.visibility = View.GONE
            binding.favoritesLabel.visibility = View.GONE
        }
        if (fromAddRemove) {
            if (menuPopularRecyclerViewAdapter != null) {
                menuPopularRecyclerViewAdapter!!.notifyDataSetChanged()
            }
            if (menuRecentOrderRecyclerViewAdapter != null) {
                menuRecentOrderRecyclerViewAdapter!!.notifyDataSetChanged()
            }
        }
    }

    override fun setFavorite(
        favorite: Favorite,
        outOfStockLocations: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun setPresenter(presenter: MenuContract.Presenter) {
        menuPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }

    override fun setCartExpiry() {
        if (activity != null) {
            (activity as MainActivity?)!!.setCartExpiry()
        }
    }

    override fun showSlides(slides: ArrayList<Slide>) {
        if (slides.size > 0) {
            binding.sliderViewPager.layoutParams.height = sliderHeight
            binding.homeOption4.sliderPlaceholder.layoutParams.height = sliderHeight
            try {
                activity?.let {
                    binding.homeOption4.sliderPlaceholder.setBackgroundColor(
                        ContextCompat.getColor(
                            it, R.color.transparent
                        )
                    )
                    val sliderAdapter = SliderFragmentStateAdapter(it, sliderPresenter, slides)
                    binding.sliderViewPager.adapter = sliderAdapter

                }
                binding.sliderViewPager.visibility = View.VISIBLE
                if (slides.size > 1) {
                    binding.dotsIndicator.setViewPager2(binding.sliderViewPager)
                    binding.dotsIndicator.visibility = View.VISIBLE
                    binding.homeOption4.dotsIndicatorOption4.visibility =
                        binding.homeOption4.root.visibility
                    binding.homeOption4.dotsIndicatorOption4.setViewPager2(binding.sliderViewPager)
                } else {
                    binding.dotsIndicator.visibility = View.GONE
                    binding.homeOption4.dotsIndicatorOption4.visibility = View.GONE
                }
                sliderHandler?.removeCallbacksAndMessages(null)
                sliderHandler = Handler(Looper.getMainLooper())
                sliderHandler?.postDelayed(object : Runnable {
                    override fun run() {
                        var pos = binding.sliderViewPager.currentItem + 1
                        if (pos >= slides.size) {
                            pos = 0
                            binding.sliderViewPager.setCurrentItem(pos, true)
                        } else {
                            binding.sliderViewPager.currentItem = pos
                        }
                        sliderHandler!!.postDelayed(this, 3000)
                    }
                }, 3000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            binding.sliderViewPager.layoutParams.height = 0
            binding.sliderViewPager.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            binding.homeOption4.dotsIndicatorOption4.visibility = View.GONE
            binding.homeOption4.sliderPlaceholder.visibility = View.GONE
        }
    }

    override fun showSlidesError() {
        if (sliderPresenter!!.sliders.size <= 0) {
            binding.sliderViewPager.visibility = View.GONE
            binding.sliderViewPager.layoutParams.height = 0
            binding.homeOption4.sliderPlaceholder.visibility = View.GONE
        }
    }

    override fun showMidBanner(banners: ArrayList<Slide>) {
        try {
            if (banners != null && banners.size > 0) {
                hasMidSlides = true
                try {
                    activity?.let {
                        val sliderAdapter = SliderFragmentStateAdapter(it, sliderPresenter, banners)
                        binding.midSliderViewPager.adapter = sliderAdapter
                        binding.midSliderContainer.visibility = View.VISIBLE
                        binding.midSliderViewPager.visibility = View.VISIBLE
                        if (banners.size > 1) {
                            binding.midDotsIndicator.setViewPager2(binding.midSliderViewPager)
                            binding.midDotsIndicator.visibility = View.VISIBLE
                        } else {
                            binding.midDotsIndicator.visibility = View.GONE
                        }
                        if (midSliderHandler != null) {
                            midSliderHandler!!.removeCallbacksAndMessages(null)
                        }
                        midSliderHandler = Handler(Looper.getMainLooper())
                        midSliderHandler!!.postDelayed(object : Runnable {
                            override fun run() {
                                var pos = binding.midSliderViewPager.currentItem + 1
                                if (pos >= banners.size) {
                                    pos = 0
                                    binding.midSliderViewPager.setCurrentItem(pos, true)
                                } else {
                                    binding.midSliderViewPager.currentItem = pos
                                }
                                midSliderHandler!!.postDelayed(this, 5000)
                            }
                        }, 3000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                hasMidSlides = false
                binding.midSliderContainer.visibility = View.GONE
                binding.midSliderViewPager.visibility = View.GONE
                binding.midDotsIndicator.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showLink(uri: Uri) {
        try {
            val activity = activity as MainActivity?
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
        menuPresenter!!.selectMenuCategory(menuCategoryId)
    }

    private fun changeCategoryIconsAlpha() {
        val size = cardViewList.size
        for (i in 0 until size) {
            cardViewList[i].alpha = latestButtonAlpha
            cardViewList[i].invalidate()
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
        binding.cardPickupContainer.startAnimation(alphaAnim)
    }

    private fun animateCard(newWidth: Int, show: Boolean) {
        val widthAnimator = ValueAnimator.ofInt(
            binding.cardSearchContainer.width, newWidth
        )
        widthAnimator.duration = 600
        widthAnimator.interpolator = DecelerateInterpolator()
        widthAnimator.addUpdateListener { animation: ValueAnimator ->
            binding.cardSearchContainer.layoutParams.width = animation.animatedValue as Int
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

    }

    override fun onMenuCategoryItemsScrolled(show: Boolean, isLastItemVisible: Boolean) {

    }

    override fun onLastItemReach(isScrollable: Boolean, scrollDirection: ScrollDirection) {}

    override fun showMenuItem(categoryId: String, menuItemId: String) {
        activity?.runOnUiThread {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
            NavigationUtils.navigateSafe(
                findNavController(),
                NewMenuHomePageFragmentDirections.actionHomeToMenuItem(categoryId, menuItemId, -1)
            )
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showBestSellers(menuItems: ArrayList<MenuItem>) {
        if (menuItems.isNotEmpty()) {
            binding.popularNowLabel.visibility = View.VISIBLE
            binding.popularNowViewAll.visibility = View.VISIBLE
            binding.popularNowItemList.visibility = View.VISIBLE
            menuPopularRecyclerViewAdapter = MenuPopularRecyclerViewAdapter(activity, menuPresenter)
            binding.popularNowItemList.layoutManager = LinearLayoutManager(
                activity, LinearLayoutManager.HORIZONTAL, false
            )
            binding.popularNowItemList.adapter = menuPopularRecyclerViewAdapter
            menuPopularRecyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            binding.popularNowLabel.visibility = View.GONE
            binding.popularNowViewAll.visibility = View.GONE
            binding.popularNowItemList.visibility = View.GONE
        }
    }

    override fun showLatestOffers(latestOffers: List<Campaign>) {
        if (latestOffers.isNotEmpty()) {
            binding.couponLabel.visibility = View.VISIBLE
            binding.couponsItemList.visibility = View.VISIBLE
            menuCouponRecyclerViewAdapter = MenuCouponRecyclerViewAdapter(activity, menuPresenter)
            binding.couponsItemList.layoutManager = LinearLayoutManager(
                activity, LinearLayoutManager.HORIZONTAL, false
            )
            binding.couponsItemList.adapter = menuCouponRecyclerViewAdapter
            menuCouponRecyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            binding.couponLabel.visibility = View.GONE
            binding.couponsItemList.visibility = View.GONE
        }
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        activity?.runOnUiThread {
            NavigationUtils.navigateSafe(
                findNavController(), NewMenuHomePageFragmentDirections.actionHomeToDigitalCoupon(
                    Gson().toJson(digitalCoupon)
                )
            )
        }
    }

    override fun showRecentOrders(menuItems: ArrayList<MenuItem>) {
        if (menuItems.isNotEmpty()) {
            binding.recentOrderLabel!!.visibility = View.VISIBLE
            binding.recentOrderLabelViewAll!!.visibility = View.VISIBLE
            binding.orderAgainItemList.visibility = View.VISIBLE
            menuRecentOrderRecyclerViewAdapter =
                MenuRecentOrderRecyclerViewAdapter(activity, menuPresenter)
            binding.orderAgainItemList.layoutManager = LinearLayoutManager(
                activity, LinearLayoutManager.HORIZONTAL, false
            )
            binding.orderAgainItemList.adapter = menuRecentOrderRecyclerViewAdapter
            menuRecentOrderRecyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            binding.recentOrderLabelViewAll.visibility = View.GONE
            binding.recentOrderLabel.visibility = View.GONE
            binding.orderAgainItemList.visibility = View.GONE
        }
    }

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
                val activity: Activity? = activity
                if (activity != null) {
                    val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
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

    override fun selectItem(menuCategory: MenuCategory, position: Int) {
        activity?.runOnUiThread {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_MENU)
            val request =
                NavDeepLinkRequest.Builder.fromUri(Uri.parse("${Values.NAV_DEEP_LINK_BASE_URL}nav_menu/${menuCategory.id}"))
                    .build()
            NavigationUtils.navigateSafe(findNavController(), request)
        }
    }

    private fun deepLinkRedirect() {
        if (Values.sDeepLink.isEmpty()) {
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

    fun refreshOrderAgain() {
        if (menuPresenter != null) {
            menuPresenter!!.getRecentOrder()
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
                loadInitial()
            }
        }
        NavigationUtils.navigateSafe(
            findNavController(), NewMenuHomePageFragmentDirections.actionHomeToCountries()
        )
    }

    fun showOrder(orderId: String) {
        NavigationUtils.navigateSafe(
            findNavController(), NewMenuHomePageFragmentDirections.actionHomeToOrder(orderId)
        )
    }

    override fun setMenuCategoryForAnalytics(
        menuItem: MenuItem?, menuCategory: MenuCategory?, price: Double, quantity: Int
    ) {
        FirebaseECommerceEventUtil.addToCartEvent(
            context, menuItem!!, menuCategory, price, quantity
        )
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

}