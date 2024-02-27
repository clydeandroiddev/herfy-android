package com.herfy.main

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.request.RequestOptions
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.cart.views.CartFragment
import com.herfy.databinding.ActivityNavigationBinding
import com.herfy.digitalcoupons.views.DigitalCouponFragmentDirections
import com.herfy.digitalcoupons.views.DigitalCouponsFragmentDirections
import com.herfy.ext.cast
import com.herfy.favorites.FavoritesFragmentDirections
import com.herfy.menu.views.MenuItemFragmentDirections
import com.herfy.newmenu.views.NewMenuHomePageFragmentDirections
import com.herfy.newmenu.views.PopularNowFragmentDirections
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
            //TODO: fix navigation

            navigation =  it.getString("navigation")?.let { nav -> this.resources.getIdentifier(nav, "id", this.packageName) }?: it.getInt("navigation")

           /* it.getString("navigation")?.let { nav ->
                navigation =
            }?: run {
                navigation =
            }*/

            currentDestination = navigation
            graph.setStartDestination(navigation)
            when (navigation) {
                R.id.nav_menu_item -> {
                    bundle.putString("menu_category_id", it.getString("menu_category_id"))
                    bundle.putString("menu_item_id", it.getString("menu_item_id"))
                    bundle.putInt("cart_menu_item_position", it.getInt("cart_menu_item_position"))
                }

                R.id.nav_order_type -> {
                    bundle.putInt("selectedOrderType", it.getInt("selectedOrderType"))
                    bundle.putBoolean("checkout", it.getBoolean("checkout"))
                    bundle.putString("storeId", it.getString("storeId"))
                    bundle.putBoolean("fromHome", it.getBoolean("fromHome"))
                }

                R.id.nav_curbside -> {
                    bundle.putBoolean("curbside", it.getBoolean("curbside"))
                    bundle.putInt("curbsideStorePosition", it.getInt("curbsideStorePosition"))
                }

                R.id.nav_cart -> {
                    bundle.putBoolean("isBranchPreselected", it.getBoolean("isBranchPreselected"))
                }
            }
        }

        navController.setGraph(graph, bundle)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            dismissDialogs()
            currentDestination = destination.id
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
    }

    override fun setupFonts() {
    }

    override fun setupTranslations() {

    }

    override fun setCaloriesPage(caloriesPage: CmsPage?) {

    }


    override fun showRatings(ratings: MutableList<Rating>?) {

    }

    override fun showMessage(message: String?) {

    }

    override fun refreshOrderAgain() {

    }

    override fun showNewNotifIndicator(count: Int) {

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



    fun showScheduleBubble() {
      /*  if (CacheUtil.getInstance().concept.allowAdvanceOrder && currentDestination == R.id.nav_cart) {
            try {
                binding.bubbleOrderLayout.visible()
                binding.bubbleOrderLayout.postDelayed({
                    binding.bubbleOrderLayout.gone()
                }, 3000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    override fun showCart() {
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            return
        }
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        when (currentDestination) {
            /*R.id.nav_menu -> NavigationUtils.navigateSafe(
                navController,
                NewMenuFragmentDirections.actionMenuToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )*/

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

    override fun setCartQuantity(quantity: Int) {
    }

    fun cartExpired() {
        val fragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
        fragment?.cast<CartFragment> {
            it.cartExpired()
        }
    }

}