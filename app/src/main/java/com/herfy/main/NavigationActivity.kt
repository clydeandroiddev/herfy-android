package com.herfy.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.request.RequestOptions
import com.herfy.R
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.base.BaseActivity
import com.herfy.cart.views.CartFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.databinding.ActivityNavigationBinding
import com.herfy.digitalcoupons.views.DigitalCouponFragmentDirections
import com.herfy.digitalcoupons.views.DigitalCouponsFragmentDirections
import com.herfy.ext.cast
import com.herfy.ext.gone
import com.herfy.ext.visible
import com.herfy.favorites.FavoritesFragmentDirections
import com.herfy.menu.views.MenuItemFragmentDirections
import com.herfy.newmenu.views.NewMenuFragmentDirections
import com.herfy.newmenu.views.NewMenuHomePageFragmentDirections
import com.herfy.newmenu.views.PopularNowFragmentDirections
import com.herfy.utils.helper.*
import com.herfy.util.StringUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage

class NavigationActivity : BaseActivity(), MainContract.View {

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var navController: NavController
    var navigation: Int = -1
    var currentDestination: Int = -1
    private lateinit var mainPresenter: MainContract.Presenter
    private var notifCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainPresenter = MainPresenter(this)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController

        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_main)

        val bundle = Bundle()
        intent.extras?.let {
            it.getString("navigation")?.let {
                val navString = it
                navigation = this.resources.getIdentifier(navString, "id", this.packageName)
            }?: {
                navigation = it.getInt("navigation")
            }

            currentDestination = navigation
            graph.setStartDestination(navigation)
            binding.mainToolbar.root.visible()
            when (navigation) {
                R.id.nav_menu_item -> {
                    binding.mainToolbar.cart.visible()
                    bundle.putString("menu_category_id", it.getString("menu_category_id"))
                    bundle.putString("menu_item_id", it.getString("menu_item_id"))
                    bundle.putInt("cart_menu_item_position", it.getInt("cart_menu_item_position"))
                }

                R.id.nav_order_type -> {
                    binding.mainToolbar.root.gone()
                    binding.mainToolbar.notif.gone()
                    binding.mainToolbar.cart.gone()
                    bundle.putInt("selectedOrderType", it.getInt("selectedOrderType"))
                    bundle.putBoolean("checkout", it.getBoolean("checkout"))
                    bundle.putString("storeId", it.getString("storeId"))
                }

                R.id.nav_curbside -> {
                    binding.mainToolbar.notif.gone()
                    binding.mainToolbar.cart.gone()
                    bundle.putBoolean("curbside", it.getBoolean("curbside"))
                    bundle.putInt("curbsideStorePosition", it.getInt("curbsideStorePosition"))
                }

                R.id.nav_cart -> {
                    binding.mainToolbar.notif.gone()
                    binding.mainToolbar.cart.gone()
                    bundle.putBoolean("isBranchPreselected", it.getBoolean("isBranchPreselected"))
                }
            }
        }

        navController.setGraph(graph, bundle)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            dismissDialogs()
            currentDestination = destination.id
            if (destination.id == R.id.nav_auth || destination.id == R.id.nav_guest || destination.id == R.id.nav_order_type) {
                binding.mainToolbar.root.gone()
            } else {
                binding.mainToolbar.root.visible()
            }
            showToolbarActionButton(false)
            showToolbarWhatsappButton(false)
            showToolbarCustomActionButton(false)
            binding.mainToolbar.title.text =
                CacheUtil.getInstance().getTranslations(destination.label.toString())
            binding.mainToolbar.title.isAllCaps = destination.id != R.id.nav_notifications
            binding.mainToolbar.menu.visibility = View.GONE
            if (destination.id != R.id.nav_dialog_order_type) {
                if (destination.id == R.id.nav_orders || destination.id == R.id.nav_favorites || destination.id == R.id.nav_fragment_countries) {
                    binding.mainToolbar.back.gone()
                } else {
                    binding.mainToolbar.back.visible()
                }
            }
            if (destination.id == R.id.nav_menu) {
                showNotifIcon()
            } else {
                binding.mainToolbar.notif.gone()
            }
            binding.mainToolbar.close.visibility =
                if (destination.id == R.id.nav_orders || destination.id == R.id.nav_favorites || destination.id == R.id.nav_fragment_countries)
                    View.VISIBLE
                else View.GONE
            when (destination.id) {
                R.id.nav_home,
                R.id.nav_menu,
                R.id.nav_order_type,
                R.id.nav_curbside,
                R.id.nav_cart,
                R.id.nav_orders,
                R.id.nav_order,
                R.id.nav_notifications,
                R.id.nav_notification,
                R.id.nav_dialog_popup,
                R.id.nav_dialog_order_type,
                R.id.nav_fragment_countries -> binding.mainToolbar.cart.gone()

                else -> {
                    binding.mainToolbar.cart.visible()
                    setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
            }
            binding.mainToolbar.schedule.gone()
            binding.mainToolbar.whatsapp.gone()
        }

        setupViews()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun setPresenter(presenter: MainContract.Presenter?) {

    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setupViews() {
        binding.mainToolbar.back.visible()
        binding.mainToolbar.back.text = CacheUtil.getInstance().getTranslations(Text.BACK, "Back")
        binding.mainToolbar.back.setOnClickListener { onBackPressed() }
        binding.mainToolbar.cart.setOnClickListener {
            showCart()
        }
    }

    override fun setupFonts() {
    }

    override fun setupTranslations() {

    }

    override fun setCaloriesPage(caloriesPage: CmsPage?) {

    }

    override fun setCartQuantity(quantity: Int) {
        if (quantity == 0) {
            binding.mainToolbar.cartQuantity.gone()
        } else if (binding.mainToolbar.cartIcon.isVisible) {
            binding.mainToolbar.cartQuantity.text = StringUtil.localize(quantity.toString())
            binding.mainToolbar.cartQuantity.visible()
        }
    }

    override fun showRatings(ratings: MutableList<Rating>?) {

    }

    override fun showMessage(message: String?) {

    }

    override fun refreshOrderAgain() {

    }

    override fun showNewNotifIndicator(count: Int) {
        if (count > 0 && CacheUtil.getInstance().concept.isNotificationCenterEnabled && (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) && binding.mainToolbar.notif.visibility == View.VISIBLE && Values.sDeepLink.isEmpty()) {
            notifCount = count
            binding.mainToolbar.notifCounter.text = "" + count
            binding.mainToolbar.notifCounter.visibility =
                if (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) View.VISIBLE else View.GONE
        } else {
            binding.mainToolbar.notifCounter.visibility = View.GONE
        }
    }

    override fun proceedRestart() {

    }

    override fun proceedMainRestart() {

    }

    override fun saveToCache(key: String?, value: String?) {

    }

    override fun preloadImage(imageUrl: String?, requestOptions: RequestOptions?) {

    }

    override fun showDigitalCoupon(digitalCoupon: Campaign?) {

    }

    override fun gateWay() {

    }

    override fun multibrandValidation(proceed: Boolean) {

    }

    fun setCloseButton(onClick: View.OnClickListener?) {
        binding.mainToolbar.close.setOnClickListener(onClick)
    }

    fun setToolbarTitle(title: String) {
        binding.mainToolbar.title.text = title
    }

    fun showToolbarActionButton(isShow: Boolean) {
        binding.mainToolbar.action.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarActionText(action: String) {
        binding.mainToolbar.action.text = action
    }

    fun setToolbarActionButton(onClick: View.OnClickListener?) {
        binding.mainToolbar.action.setOnClickListener(onClick)
    }

    fun enableToolbarActionButton(isEnabled: Boolean) {
        binding.mainToolbar.action.isEnabled = isEnabled
    }

    fun showToolbarWhatsappButton(isShow: Boolean) {
        binding.mainToolbar.whatsapp.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarWhatsappButton(onClick: View.OnClickListener) {
        binding.mainToolbar.whatsapp.setOnClickListener(onClick)
    }

    fun showToolbarCustomActionButton(isShow: Boolean) {
        binding.mainToolbar.customAction.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarCustomActionText(action: String) {
        binding.mainToolbar.customAction.text = action
    }

    fun setToolbarCustomActionButton(onClick: View.OnClickListener) {
        binding.mainToolbar.customAction.setOnClickListener(onClick)
    }

    fun showScheduleButton(onClick: View.OnClickListener) {
        if (currentDestination == R.id.nav_cart) {
            try {
                binding.mainToolbar.cart.gone()
                binding.mainToolbar.schedule.visibility =
                    if (CacheUtil.getInstance().concept.allowAdvanceOrder) View.VISIBLE else View.GONE
                binding.mainToolbar.scheduleIcon.setOnClickListener(onClick)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showScheduleBubble() {
        if (CacheUtil.getInstance().concept.allowAdvanceOrder && currentDestination == R.id.nav_cart && binding.mainToolbar.schedule.visibility == View.VISIBLE) {
            try {
                binding.bubbleOrderLayout.visible()
                binding.bubbleOrderLayout.postDelayed({
                    binding.bubbleOrderLayout.gone()
                }, 3000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun showCart() {
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            return
        }
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        when (currentDestination) {
            R.id.nav_menu -> NavigationUtils.navigateSafe(
                navController,
                NewMenuFragmentDirections.actionMenuToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_home -> NavigationUtils.navigateSafe(
                navController,
                NewMenuHomePageFragmentDirections.actionHomeToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_menu_item -> NavigationUtils.navigateSafe(
                navController,
                MenuItemFragmentDirections.actionMenuItemToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_favorites -> NavigationUtils.navigateSafe(
                navController,
                FavoritesFragmentDirections.actionFavoritesToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_popular_now -> NavigationUtils.navigateSafe(
                navController,
                PopularNowFragmentDirections.actionPopularNowToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_digital_coupons -> NavigationUtils.navigateSafe(
                navController,
                DigitalCouponsFragmentDirections.actionDigitalCouponsToCart()
            )

            R.id.nav_digital_coupon -> NavigationUtils.navigateSafe(
                navController,
                DigitalCouponFragmentDirections.actionDigitalCouponToCart()
            )
        }
    }

    override fun showFavorites() {

    }

    private fun showNotifIcon() {
        if (!AuthUtil.getInstance().isSignedIn || !CacheUtil.getInstance().concept.isNotificationCenterEnabled) {
            binding.mainToolbar.notif.visibility = View.GONE
            binding.mainToolbar.notifCounter.visibility = View.GONE
            return
        }
        if (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) {
            binding.mainToolbar.notif.visibility = View.VISIBLE
            binding.mainToolbar.notificationIcon.visibility = View.VISIBLE
            if (CacheUtil.getInstance().concept.isNotificationCenterEnabled) {
                if (notifCount > 0) {
                    binding.mainToolbar.notifCounter.text = notifCount.toString()
                    binding.mainToolbar.notifCounter.visibility = View.VISIBLE
                } else {
                    binding.mainToolbar.notifCounter.visibility = View.GONE
                }
                if (::mainPresenter.isInitialized) {
                    mainPresenter.geUnreadNotification()
                }
            }
            binding.mainToolbar.cart.visibility = View.GONE
            binding.mainToolbar.schedule.visibility = View.GONE
        } else {
            binding.mainToolbar.notif.visibility = View.GONE
        }
    }

    fun hideBackButton() {
        binding.mainToolbar.back.gone()
    }

    fun isBackButtonVisible(): Boolean = binding.mainToolbar.back.isVisible

    fun cartExpired() {
        val fragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
        fragment?.cast<CartFragment> {
            it.cartExpired()
        }
    }

}