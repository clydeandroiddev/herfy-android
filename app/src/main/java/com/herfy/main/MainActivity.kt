package com.herfy.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup.OnPositionChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.account.views.AccountActivity
import com.herfy.animation.ActivitySwitcher
import com.herfy.auth.views.AuthActivity
import com.herfy.base.BaseActivity
import com.herfy.browser.BrowserActivity
import com.herfy.cart.views.CartFragment
import com.herfy.contactsupport.ContactSupportBottomSheetDialogFragment
import com.herfy.country.CountriesFragment
import com.herfy.databinding.ActivityMainBinding
import com.herfy.digitalcoupons.views.DigitalCouponFragmentDirections
import com.herfy.digitalcoupons.views.DigitalCouponsFragmentDirections
import com.herfy.ext.cast
import com.herfy.ext.gone
import com.herfy.ext.readAssetsFile
import com.herfy.ext.visible
import com.herfy.favorites.FavoritesFragmentDirections
import com.herfy.feedback.FeedbackActivity
import com.herfy.loyalty.LoyaltyActivity
import com.herfy.main.adapter.LanguageAdapter
import com.herfy.main.adapter.SwitchServerAdapter
import com.herfy.menu.views.MenuItemFragmentDirections
import com.herfy.newmenu.NewMenuFragmentCommonListener
import com.herfy.newmenu.views.*
import com.herfy.promotions.views.PromotionsFragmentDirections
import com.herfy.ratings.views.RatingsBottomSheetDialogFragment
import com.herfy.splash.SplashActivity
import com.herfy.util.*
import com.herfy.util.extensions.loadImageFromUrl
import com.herfy.util.obfuscator.Keychain
import com.herfy.util.obfuscator.reveal
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.jakewharton.processphoenix.ProcessPhoenix
import com.skylinedynamics.solosdk.api.SoloApi
import com.skylinedynamics.solosdk.api.SoloAuth
import com.skylinedynamics.solosdk.api.models.objects.*
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainActivity : BaseActivity(), MainContract.View, NewMenuFragmentCommonListener {

    companion object {
        var currentMainActivity: MainActivity? = null
        var orders = false
        var order = false
        var fromButtonClick = false
        var curbside = false
        var isFromShowOrderType = false
    }

    private var mCurrentLocale: Locale? = null

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    private lateinit var mainPresenter: MainContract.Presenter

    private var isDefaultHomeScreenDesign = false

    var backClicked = false
    private var hasLinks = false
    private val manual = true
    private var signIn = false
    private var orderType = false

    private var goToSubs = false
    private var goToMenu = false
    private var isSubMode = false
    private var newMenuHomeFragmentClass: NewMenuHomePageFragment? = null
    private var newMenuFragmentClass: NewMenuFragment? = null
    var fromShowOrderType = false
    private var cQuantity = 0
    private var notifCount = 0
    private var fromReOrder = false
    private var cartFragment: CartFragment? = null
    private var orderTypeArgs: Bundle? = null
    private var fromPayment = false
    private var fromFavorites = false
    private var fromFavoritesCart = false
    private var longPressTime: Long = 0
    var forceLocation = false
    var forceLocationMenuRefresh = false

    private var isFromLogOut = false
    private var isFromFavorites = false
    private var fromChangeLanguage = false
    var currentDestination = -1
    private var languageAdapter : LanguageAdapter? = null

    private var authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (AuthUtil.getInstance().isSignedIn) {
                    refreshMenu()
                }
            }
        }

    override fun onStart() {
        super.onStart()
        mCurrentLocale = resources.configuration.locale
    }

    override fun onRestart() {
        super.onRestart()
        val locale = LocaleUtil.getLocale()
        if (locale != mCurrentLocale) {
            mCurrentLocale = locale
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (AuthUtil.getInstance().isSignedIn) {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            firebaseAnalytics.setUserId(AuthUtil.getInstance().customer.id)
        }

        val bundle = intent.extras
        if (bundle != null) {
            if (bundle.containsKey("fromChangeLanguage")) {
                fromChangeLanguage = bundle.getBoolean("fromChangeLanguage")
            }

            forceLocation = if (bundle.containsKey("forceLocation")) {
                bundle.getBoolean("forceLocation", false)
            } else {
                false
            }
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController

        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_main)
        isDefaultHomeScreenDesign = false

        quickOrderSelected()
        graph.setStartDestination(R.id.nav_home)
        currentDestination = R.id.nav_home

        navController.setGraph(graph, intent.extras)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            dismissDialogs()
            if (currentDestination == R.id.nav_auth || currentDestination == R.id.nav_guest) {
                setupSideBar()
            }
            currentDestination = destination.id
            /*val layoutParams = binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            if (destination.id == R.id.nav_auth || destination.id == R.id.nav_guest) {
                layoutParams.height = 0
            } else {
                layoutParams.height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
            }
            binding.appBarLayout.layoutParams = layoutParams
            showToolbarActionButton(false)
            showToolbarWhatsappButton(false)
            showToolbarCustomActionButton(false)
            when (destination.id) {
                R.id.nav_home, R.id.nav_subscriptions -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.mainToolbar.menu.visibility = View.VISIBLE
                    binding.mainToolbar.back.visibility = View.GONE
                    showNotifIcon()
                }

                R.id.nav_menu -> {
                    binding.drawerLayout.setDrawerLockMode(if (isDefaultHomeScreenDesign) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.mainToolbar.menu.visibility =
                        if (isDefaultHomeScreenDesign) View.VISIBLE else View.GONE
                    binding.mainToolbar.back.visibility =
                        if (isDefaultHomeScreenDesign) View.GONE else View.VISIBLE
                    showNotifIcon()
                }

                R.id.nav_dialog_popup -> binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                else -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.mainToolbar.menu.visibility = View.GONE
                    if (destination.id != R.id.nav_dialog_order_type) {
                        if ((destination.id == R.id.nav_order_type && forceLocation) || destination.id == R.id.nav_orders || destination.id == R.id.nav_favorites || destination.id == R.id.nav_fragment_countries) {
                            binding.mainToolbar.back.visibility = View.GONE
                        } else {
                            binding.mainToolbar.back.visibility = View.VISIBLE
                        }
                    }
                    binding.mainToolbar.notif.visibility = View.GONE
                }
            }
            binding.mainToolbar.close.visibility =
                if (destination.id == R.id.nav_orders || destination.id == R.id.nav_favorites || destination.id == R.id.nav_fragment_countries)
                    View.VISIBLE
                else View.GONE
            binding.mainToolbar.title.isAllCaps = destination.id != R.id.nav_notifications
            if (destination.id != R.id.nav_dialog_popup) {
                binding.mainToolbar.title.text =
                    CacheUtil.getInstance().getTranslations(destination.label.toString())
            }
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
                R.id.nav_upsell,
                R.id.nav_promotions,
                R.id.nav_promotion,
                R.id.nav_fragment_countries -> binding.mainToolbar.cart.visibility = View.GONE

                else -> {
                    binding.mainToolbar.cart.visibility = View.VISIBLE
                    setCartQuantity()
                }
            }
            binding.mainToolbar.schedule.visibility = View.GONE
            binding.mainToolbar.whatsapp.visibility = View.GONE*/
        }

        checkBackground()

        mainPresenter = MainPresenter(this@MainActivity)
        mainPresenter.start()

        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            binding.sidebar.brands.visibility = View.VISIBLE
            binding.sidebar.concept.setOnClickListener { view: View? ->
                showCountries()
            }
            binding.sidebar.brands.setOnClickListener { view: View? ->
                showCountries()
            }
        } else {
            binding.sidebar.brands.visibility = View.GONE
            binding.sidebar.concept.setOnClickListener(null)
            binding.sidebar.brands.setOnClickListener(null)
        }

        setupLanguage()
    }

    override fun onBackPressed() {
        if (forceLocation && currentDestination == R.id.nav_order_type) {
            forceLocation = false
            forceLocationMenuRefresh = true
        }
        super.onBackPressed()
    }

    fun setupLanguage() {
        binding.sidebar.apply {
            val secondaryLanguage = CacheUtil.getInstance().concept.secondaryLanguage
            if (secondaryLanguage == null || secondaryLanguage.isEmpty()) {
                cvLang.visibility = View.INVISIBLE
            } else {
                val languages = CacheUtil.getInstance().concept.languages.toList()
                /*binding.sidebar.language.apply {
                    french.isVisible =
                        !(languages.find { lang -> lang.equals(LocaleUtil.FR, true) }.isNullOrEmpty())
                    english.isVisible = !(languages.find { lang -> lang.equals(LocaleUtil.EN_US, true) }
                        .isNullOrEmpty())
                    arabic.isVisible = !(languages.find { lang -> lang.equals(LocaleUtil.AR_SA, true) }
                        .isNullOrEmpty())
                }*/

               /* binding.sidebar.language.languageSegmented.apply {
                    val position = when {
                        LocaleUtil.getInstance().isArabic -> 0
                        LocaleUtil.getInstance().isFrench -> 1
                        else -> 2
                    }
                    setPosition(position, true)
                    onPositionChangedListener = OnPositionChangedListener { position ->
                        var language: String = ""

                        when (getButton(position)) {
                            binding.sidebar.language.english -> language =
                                if (!LocaleUtil.getInstance().isEnglish) "en" else ""

                            binding.sidebar.language.arabic -> language =
                                if (!LocaleUtil.getInstance().isArabic) "ar" else ""

                            binding.sidebar.language.french -> language =
                                if (!LocaleUtil.getInstance().isFrench) "fr" else ""
                        }
                        if (language.isNotEmpty()) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                            val eventAttributes: HashMap<String, String> = HashMap()
                            LocaleUtil.getInstance().language = language
                            eventAttributes["ChangeLanguage"] = language.uppercase(Locale.getDefault())
                            logEvent("ChangeLanguage", eventAttributes, null, 0.0)
                            LocaleUtil.getInstance().setLocale(this@MainActivity)
                            val config: Configuration = Configuration()
                            config.locale = Locale(language)
                            resources.updateConfiguration(config, resources.displayMetrics)
                            SoloAuth.getInstance()
                                .setLanguage(LocaleUtil.getInstance().languageSymbol)
                            mainPresenter.changeLanguage()

                            val intent: Intent = Intent(this@MainActivity, SplashActivity::class.java)
                            intent.putExtra("fromChangeLanguage", true)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            ActivitySwitcher.animationOut(
                                this@MainActivity.findViewById(R.id.drawer_layout),
                                windowManager,
                                object : ActivitySwitcher.AnimationListener {
                                    override fun onAnimationStarted() {
                                        startActivity(intent)
                                        overridePendingTransition(0, 0)
                                        finish()
                                    }

                                    override fun onAnimationFinished() {}
                                })
                        }


                    }
                }*/
                cvLang.visibility = View.VISIBLE
            }
        }

    }

    private fun showCountries() {
        val fragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
        fragment?.cast<NewMenuFragment> {
            it.showCountries()
        }
        fragment?.cast<NewMenuHomePageFragment> {
            it.showCountries()
        }
    }

    private fun checkBackground() {
        binding.navigationLayout.setBackgroundResource(R.color.page_background_color)
    }

    override fun preloadImage(imageUrl: String, requestOptions: RequestOptions) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this)
                    .downloadOnly()
                    .load(imageUrl)
                    .apply(requestOptions)
                    .submit()
            }
        }
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        dismissDialogs()
        if (isDefaultHomeScreenDesign) {
            NavigationUtils.navigateSafe(
                navController, NewMenuFragmentDirections.actionMenuToDigitalCoupon(
                    Gson().toJson(digitalCoupon)
                )
            )
        } else {
            NavigationUtils.navigateSafe(
                navController, NewMenuHomePageFragmentDirections.actionHomeToDigitalCoupon(
                    Gson().toJson(digitalCoupon)
                )
            )
        }
    }

    fun upsellEvent(menuItem: MenuItem, menuCategory: MenuCategory) {
        dismissDialogs()
        if (isDefaultHomeScreenDesign) {
            NavigationUtils.navigateSafe(
                navController, NewMenuFragmentDirections.actionMenuToItem(
                    menuCategory.id, menuItem.id
                )
            )
        } else {
            NavigationUtils.navigateSafe(
                navController, NewMenuHomePageFragmentDirections.actionHomeToMenuItem(
                    menuCategory.id, menuItem.id
                )
            )
        }
    }

    fun refreshFavoritesPage() {
        try {
            if (newMenuHomeFragmentClass != null) {
                newMenuHomeFragmentClass!!.refreshFavorites()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showSocialMediaLinks() {
        hasLinks = false
        if (CacheUtil.getInstance().concept.facebookLink != null && !CacheUtil.getInstance().concept.facebookLink.isEmpty()) {
            binding.sidebar.cvFacebook.visibility = View.VISIBLE
            hasLinks = true
            binding.sidebar.cvFacebook.setOnClickListener { openSocialMediaLink(CacheUtil.getInstance().concept.facebookLink) }
        } else {
            binding.sidebar.cvFacebook.visibility = View.GONE
        }
        if (CacheUtil.getInstance().concept.twitterLink != null && !CacheUtil.getInstance().concept.twitterLink.isEmpty()) {
            binding.sidebar.cvTwitter.visibility = View.VISIBLE
            hasLinks = true
            binding.sidebar.cvTwitter.setOnClickListener { openSocialMediaLink(CacheUtil.getInstance().concept.twitterLink) }
        } else {
            binding.sidebar.cvTwitter.visibility = View.GONE
        }
        if (CacheUtil.getInstance().concept.instagramLink != null && !CacheUtil.getInstance().concept.instagramLink.isEmpty()) {
            binding.sidebar.cvIg.visibility = View.VISIBLE
            hasLinks = true
            binding.sidebar.cvIg.setOnClickListener { openSocialMediaLink(CacheUtil.getInstance().concept.instagramLink) }
        } else {
            binding.sidebar.cvIg.visibility = View.GONE
        }
        if (CacheUtil.getInstance().concept.youtubeLink != null && !CacheUtil.getInstance().concept.youtubeLink.isEmpty()) {
            binding.sidebar.cvYoutube.visibility = View.VISIBLE
            hasLinks = true
            binding.sidebar.cvYoutube.setOnClickListener { openSocialMediaLink(CacheUtil.getInstance().concept.youtubeLink) }
        } else {
            binding.sidebar.cvYoutube.visibility = View.GONE
        }
        if (CacheUtil.getInstance().concept.websiteLink != null && !CacheUtil.getInstance().concept.websiteLink.isEmpty()) {
            binding.sidebar.cvWeb.visibility = View.VISIBLE
            hasLinks = true
            binding.sidebar.cvWeb.setOnClickListener { openSocialMediaLink(CacheUtil.getInstance().concept.websiteLink) }
        } else {
            binding.sidebar.cvWeb.visibility = View.GONE
        }
        binding.sidebar.cvTiktok.visibility = View.GONE
        binding.sidebar.cvShare.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, BuildConfig.TAG)
                val shareMessage =
                    "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "Select One"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openSocialMediaLink(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (fromChangeLanguage) {
            fromChangeLanguage = false
            ActivitySwitcher.animationIn(findViewById(R.id.drawer_layout), windowManager)
        }
        currentMainActivity = this@MainActivity
        if (intent.extras?.getBoolean(CountriesFragment.ARGS_KEY_FRAGMENT_RELOAD, false) == true) {
            intent.removeExtra(CountriesFragment.ARGS_KEY_FRAGMENT_RELOAD)
            mainPresenter.multibrandValidation(false)
        }
        showNotifIcon()
        dismissDialogs()
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            CacheUtil.getInstance().cartExpired = false
            CacheUtil.getInstance().clearOrder()
        }
        if (fromPayment) {
            fromPayment = false
            CacheUtil.getInstance().cartExpired = false
        }
        if (CacheUtil.getInstance().cartExpired) {
            CacheUtil.getInstance().cartExpired = false
            clearCart()
        } else {
            mainPresenter.getCartQuantity()
            mainPresenter.getOrders()
        }
        CacheUtil.shouldDismiss = false
        fromShowOrderType = false
        setupSideBar()

        if (curbside) {
            curbside = false
        }

        if (Values.sDeepLink.isNotEmpty()) {
            deepLinkRedirect()
        } else {
            if (intent.extras?.containsKey("order_id") == true) {
                fromPayment = true
                val orderId = intent.getStringExtra("order_id") ?: ""
                intent.removeExtra("order_id")

                CacheUtil.getInstance().lastOrderId = orderId

                if (orderId != "") {
                    MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ORDER_DETAILS)
                    val fragment =
                        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
                    fragment?.cast<NewMenuFragment> {
                        it.showOrder(orderId)
                    }
                    fragment?.cast<NewMenuHomePageFragment> {
                        it.showOrder(orderId)
                    }
                    fragment?.cast<CartFragment> {
                        it.showOrder(orderId)
                    }
                }
            } else if (forceLocation) {
                if (!isDefaultHomeScreenDesign) {
                    NavigationUtils.navigateSafe(
                        navController,
                        NewMenuHomePageFragmentDirections.actionHomeToOrderType()
                    )
                } else {
                    NavigationUtils.navigateSafe(
                        navController,
                        NewMenuFragmentDirections.actionMenuToOrderType()
                    )
                }
            }
        }

        val bundle = intent.extras
        if (bundle != null) {
            isFromLogOut = bundle.getBoolean("isFromLogOut", false)
            bundle.remove("isFromLogOut")
            if (isFromLogOut && !AuthUtil.getInstance().isSignedIn) {
                isFromLogOut = false
                refreshMenu()
            }
        }

    }

    private fun setupSideBar() {
        val customer = AuthUtil.getInstance().customer
        val isSubs = CacheUtil.getInstance().mode == 2
        val isSignedIn = AuthUtil.getInstance().isSignedIn
        val isGuest =
            (isSignedIn && !customer.attributes.accountType.equals("full", ignoreCase = true))
        val signedOut = CacheUtil.getInstance().getTranslations(
            Text.YOU_ARE_CURRENTLY_NOT_SIGNED_IN,
            "You are currently not signed in"
        )
        val signedInFull =
            CacheUtil.getInstance().getTranslations(Text.WELCOME_BACK, "Welcome back,")
        val signedInGuest = CacheUtil.getInstance().getTranslations(
            Text.YOU_ARE_CURRENTLY_SIGNED_IN_AS_A_GUEST,
            "You are currently signed in as a guest"
        )
        binding.sidebar.customerName.visibility =
            if (isSignedIn && !isGuest) View.VISIBLE else View.GONE
        binding.sidebar.customerName.text =
            if (isSignedIn) customer.attributes.firstName + " " + customer.attributes.lastName else ""
        binding.sidebar.account.visibility = if (isSignedIn) View.VISIBLE else View.GONE
        binding.sidebar.activeSubscription.visibility =
            if (isSubs && isSignedIn) View.VISIBLE else View.GONE
        if (!isSignedIn && CacheUtil.getInstance().concept.socialLoginOption.contains("solo-guest")) {
            binding.sidebar.guest.visibility = View.VISIBLE
        } else {
            binding.sidebar.guest.visibility = View.GONE
        }
        binding.sidebar.signin.visibility = if (isSignedIn) View.GONE else View.VISIBLE
        binding.sidebar.register.visibility =
            if (isSignedIn && !isGuest) View.GONE else View.VISIBLE
        val loggedInRules = if (isSignedIn) (if (isSubs) View.GONE else View.VISIBLE) else View.GONE
        binding.sidebar.history.visibility = loggedInRules
        val containsLoyalty =
            CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes().contains(
                PaymentType.LOYALTY
            ) ||
                    CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                        .contains(PaymentType.LOYALTY)
        val isFullSignedIn =
            (isSignedIn && !customer.attributes.accountType.equals("guest", ignoreCase = true))
        val isNotificationCenterEnabled =
            CacheUtil.getInstance().concept.isNotificationCenterEnabled
        if (containsLoyalty && isFullSignedIn) {
            binding.sidebar.loyalty.visibility = View.VISIBLE
        } else {
            binding.sidebar.loyalty.visibility = View.GONE
        }
        binding.sidebar.notification.visibility = loggedInRules
        binding.sidebar.favorites.visibility = loggedInRules
        binding.sidebar.selectionLayout.visibility =
            if (BuildConfig.SUBSCRIPTION_ENABLED) View.VISIBLE else View.GONE
        processLiveChat()
        if (CacheUtil.getInstance().isContactUsEnabled) {
            binding.sidebar.contactSupportLabel.apply {
                visible()
                setOnClickListener {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    val contactSupportBottomSheet = ContactSupportBottomSheetDialogFragment()
                    contactSupportBottomSheet.show(
                        supportFragmentManager,
                        ContactSupportBottomSheetDialogFragment::class.java.name
                    )
                }
            }
        } else {
            binding.sidebar.contactSupportLabel.gone()
        }
        setupLogo()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onPause() {
        super.onPause()
        mainPresenter.detachAbly()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainPresenter.detachAbly()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun setPresenter(presenter: MainContract.Presenter) {
        mainPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(
            eventName ?: "",
            eventAttributes ?: HashMap(),
            eventMetricName ?: "",
            eventMetricValue
        )
    }

    private fun setupLogo() {
        binding.sidebar.concept.visibility = View.VISIBLE
        val conceptObject = CacheUtil.getInstance().concept

        if (BuildConfig.MULTI_COUNTRY_ENABLED || BuildConfig.FEATURE_PHASE < 1.3f) {
            val brandLogoImage = CacheUtil.getInstance().application.attributes.logo
            if (brandLogoImage.isNotEmpty()) {
                binding.sidebar.concept.loadImageFromUrl(brandLogoImage)
            } else {
                binding.sidebar.concept.setImageResource(R.drawable.header)
            }
        } else {
            val conceptLogo: String = conceptObject.logo
            val conceptAppBrandLogoUrl = conceptObject.appBrandLogo
            if (conceptLogo != null && conceptLogo.isNotEmpty()) {
                binding.sidebar.concept.visibility = View.VISIBLE
                binding.sidebar.concept.loadImageFromUrl(conceptLogo)
            } else {
                binding.sidebar.concept.setImageResource(R.drawable.header)
            }

            /*if (conceptObject.removeBranding) {
                binding.sidebar.imageSolo.visibility = View.GONE
            } else if (conceptAppBrandLogoUrl != null && conceptObject.appBrandLogo.isNotEmpty()) {
                binding.sidebar.imageSolo.loadImageFromUrl(conceptAppBrandLogoUrl)
                binding.sidebar.imageSolo.visibility = View.VISIBLE
            } else {
                if (BuildUtil.soloLogo()) {
                    binding.sidebar.imageSolo.setImageResource(R.drawable.solo)
                    binding.sidebar.imageSolo.visibility = View.VISIBLE
                } else if (BuildUtil.foodicsLogo()) {
                    binding.sidebar.imageSolo.setImageResource(R.drawable.img_foodics_new)
                    binding.sidebar.imageSolo.visibility = View.VISIBLE
                } else {
                    binding.sidebar.imageSolo.visibility = View.GONE
                }
            }*/
        }
    }


    override fun setupViews() {
        binding.sidebar.concept.setImageResource(R.drawable.header)
        mainPresenter.getCaloriesPage()
        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
        showSocialMediaLinks()
        /*if (BuildUtil.soloAppKey().equals("7r5YQwmSgZCtM0ux", ignoreCase = true)) {
            binding.mainToolbar.title.visibility = View.GONE
            binding.mainToolbar.topLogo.visibility = View.VISIBLE
        }
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            binding.mainToolbar.viewOnlyLayout.visibility = View.VISIBLE
        } else {
            binding.mainToolbar.viewOnlyLayout.visibility = View.GONE
        }*/
        val actionBarDrawerToggle: ActionBarDrawerToggle = object :
            ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                R.string.app_name,
                R.string.app_name
            ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                val slideX = drawerView.width * slideOffset
                if (LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench) {
                    binding.drawerMain.translationX = slideX
                } else {
                    binding.drawerMain.translationX = -slideX
                }
            }
        }
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        binding.sidebar.signin.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("sign_in", true)
            intent.putExtra("guest", false)
            intent.putExtra("curbside", false)
            intent.putExtra("checkout", false)
            authLauncher.launch(intent)
        }
        binding.sidebar.liveChatLayout.setOnClickListener { view: View? -> openWhatsApp() }
        binding.sidebar.register.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("sign_in", false)
            intent.putExtra("guest", false)
            intent.putExtra("curbside", false)
            intent.putExtra("checkout", false)
            authLauncher.launch(intent)
        }

        binding.sidebar.guest.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("sign_in", false)
            intent.putExtra("guest", true)
            intent.putExtra("curbside", false)
            intent.putExtra("checkout", false)
            authLauncher.launch(intent)
        }
        binding.sidebar.history.setOnClickListener { showHistory() }
        binding.sidebar.loyalty.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this@MainActivity, LoyaltyActivity::class.java)
            startActivity(intent)
            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
        }
        binding.sidebar.feedback.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent: Intent
            val feedbackUrl: String = CacheUtil.getInstance().concept.feedbackRedirectUrl
            if (feedbackUrl != null && feedbackUrl.isNotEmpty()) {
                if (StringUtil.isValidEmail(feedbackUrl)) {
                    intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackUrl))
                    intent.type = "text/plain"
                } else {
                    intent = Intent(this@MainActivity, BrowserActivity::class.java)
                    intent.putExtra("title", CacheUtil.getInstance().getTranslations(
                        Text.FEEDBACK))
                    intent.putExtra("url", feedbackUrl)
                }
            } else {
                intent = Intent(this@MainActivity, FeedbackActivity::class.java)
            }
            if (feedbackUrl != null && StringUtil.isValidEmail(feedbackUrl)) {
                startActivity(Intent.createChooser(intent, "Select your Email app"))
            } else startActivity(intent)
            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
        }
        binding.sidebar.clNotification.setOnClickListener {
            notifCount = 0
            //binding.mainToolbar.notifCounter.setText(null)
            //binding.mainToolbar.notifCounter.visibility = View.GONE
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showNotifications()
        }
        binding.sidebar.account.setOnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ACCOUNT)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this@MainActivity, AccountActivity::class.java)
            startActivity(intent)
            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
        }
        binding.sidebar.favorites.setOnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_FAVORITE_ITEMS)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            if (isDefaultHomeScreenDesign) {
                NavigationUtils.navigateSafe(
                    navController,
                    NewMenuFragmentDirections.actionMenuToFavorites()
                )
            } else {
                NavigationUtils.navigateSafe(
                    navController,
                    NewMenuHomePageFragmentDirections.actionHomeToFavorites()
                )
            }
        }
        binding.sidebar.quickOrder.setOnClickListener { v: View? ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            if (!isSubMode) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                return@setOnClickListener
            }
            CacheUtil.getInstance().setUpMode(1)
            val intent = Intent(this@MainActivity, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.sidebar.subscriptions.setOnClickListener { v: View? ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            if (isSubMode) {
                return@setOnClickListener
            }
            CacheUtil.getInstance().setUpMode(2)
            val intent = Intent(this@MainActivity, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.sidebar.activeSubscription.setOnClickListener { }
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = pInfo.versionName
            val versionCode = pInfo.versionCode.toString()
            binding.sidebar.versionBuild.text =
                CacheUtil.getInstance().getTranslations(Text.VERSION, "Version (x)")
                    .replace("(x)", StringUtil.localize(versionName))
            binding.sidebar.versionBuild.visibility = View.VISIBLE
            if (!BuildConfig.BUILD_TYPE.equals("release", true)) {
                binding.sidebar.versionBuild.setOnTouchListener { view: View?, event: MotionEvent ->
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        longPressTime = System.currentTimeMillis()
                    }
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if ((System.currentTimeMillis() - longPressTime) >= 4000) {
                            showEnvSwitchWarningDialog()
                            longPressTime = 0
                        }
                    }
                    true
                }
            }

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        /*binding.mainToolbar.menu.setOnClickListener {
            val selectedCountry = CacheUtil.getInstance().country
            selectedCountry?.let { country ->
                val brandImageUrl = country.imageUrl
                if (brandImageUrl.isNotEmpty()) {
                    binding.sidebar.concept.loadImageFromUrl(brandImageUrl)
                }
            }
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.mainToolbar.back.setOnClickListener {
            onBackPressed()
        }
        binding.mainToolbar.cart.setOnClickListener(View.OnClickListener {
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                return@OnClickListener
            }
            showCart()
        })
        binding.mainToolbar.notif.setOnClickListener {
            if (CacheUtil.getInstance().concept.isNotificationCenterEnabled) {
                notifCount = 0
                binding.mainToolbar.notifCounter.text = null
                binding.mainToolbar.notifCounter.visibility = View.GONE
                showNotifications()
            }
        }*/

        binding.sidebar.promotions.setOnClickListener { view: View? ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            if (isDefaultHomeScreenDesign) {
                NavigationUtils.navigateSafe(
                    navController,
                    NewMenuFragmentDirections.actionMenuToPromotions()
                )
            } else {
                NavigationUtils.navigateSafe(
                    navController,
                    NewMenuHomePageFragmentDirections.actionHomeToPromotions()
                )
            }
        }
    }

    private fun showNotifications() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_NOTIFICATIONS)
        if (isDefaultHomeScreenDesign || currentDestination == R.id.nav_menu) {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuFragmentDirections.actionMenuToNotifications()
            )
        } else {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuHomePageFragmentDirections.actionHomeToNotifications()
            )
        }
    }

    private fun openWhatsApp() {
        try {
            val url =
                "https://api.whatsapp.com/send?phone=" + CacheUtil.getInstance().concept.whatsappNumber
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW)
            val intentUrl = "https://play.google.com/store/apps/details?id=com.whatsapp"
            intent.data = Uri.parse(intentUrl)
            startActivity(intent)
        }
    }

    private fun processLiveChat() {
        val callCenterNumber: String = CacheUtil.getInstance().concept.whatsappNumber
        try {
            if (callCenterNumber != null) {
                if (callCenterNumber.isNotEmpty()) {
                    if (!callCenterNumber.equals("null", ignoreCase = true)) {
                        binding.sidebar.liveChatLayout.visibility = View.VISIBLE
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showEnvSwitchWarningDialog() {
        try {
            dismissDialogs()
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            val dialogView =
                LayoutInflater.from(this).inflate(R.layout.dialog_env_switch_warning, null, false)
            dialog.setContentView(dialogView)
            val cancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val ok = dialogView.findViewById<TextView>(R.id.btn_continue)
            val password = dialogView.findViewById<EditText>(R.id.edit_password)
            cancel.setOnClickListener { view: View? -> dialog.dismiss() }
            ok.setOnClickListener { view: View? ->
                if ((reveal(Keychain.ENV_PASSWORD) == password.getText().toString())) {
                    showEnvSwitchDialog()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Wrong password, could not continue.", Toast.LENGTH_LONG)
                        .show()
                }
            }
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showEnvSwitchDialog() {
        try {


            var selectedServer: com.herfy.model.ServerType? = null;
            val serverTypeStr = assets.readAssetsFile("servertypes.json")

            val envList = arrayListOf<com.herfy.model.ServerType>()
            envList.addAll(
                GsonHelper.gsonFinal().fromJson(
                    serverTypeStr, object : TypeToken<ArrayList<com.herfy.model.ServerType>>() {}.type
                )
            )
            val env = envList.find { s ->
                s.url.equals(
                    EnvUtil.getEnvironmentUrl(),
                    ignoreCase = true
                )
            }?.displayName ?: ""
            val envMessage = "App is currently pointed to $env."
            Toast.makeText(this@MainActivity, envMessage, Toast.LENGTH_SHORT).show()
            dismissDialogs()
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            val dialogView =
                LayoutInflater.from(this).inflate(R.layout.dialog_env_switch, null, false)
            dialog.setContentView(dialogView)
            val apply = dialogView.findViewById<TextView>(R.id.btn_apply)
            val cancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val rvServer = dialogView.findViewById<RecyclerView>(R.id.rv_server)
            val currServerIndex = envList.indexOfFirst { t ->
                t.url.equals(EnvUtil.getEnvironmentUrl())
            }
            val adapter = SwitchServerAdapter(envList, currServerIndex) { serverType ->
                selectedServer = serverType
            }

            rvServer.adapter = adapter
            adapter.notifyDataSetChanged()


            apply.setOnClickListener { view: View? ->
                selectedServer?.let { serverType ->
                    if (serverType.displayName.equals("custom", true)) {
                        if (serverType.url.isEmpty()) {
                            showAlertDialog("Warning", "Custom url is required")
                            return@setOnClickListener
                        } else {
                            serverType.url = adapter.getCustomUrl()
                        }
                    }


                    dialog.dismiss()
                    EnvUtil.setCustomEnvironment(serverType)
                    triggerRebirth()
                }

            }
            cancel.setOnClickListener { view: View? ->
                dialog.dismiss()
                if (!EnvUtil.getEnvironmentUrl().equals(BuildConfig.BASE_URL, true)) {
                    EnvUtil.setCustomEnvironment(null)
                    triggerRebirth()
                }
            }
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
            dialog.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun logoutUser() {
        LoginManager.getInstance().logOut()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildUtil.googleInClient(this@MainActivity))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        googleSignInClient.signOut()
        SoloAuth.getInstance().logout()
        MixpanelUtil.logout()
        AuthUtil.getInstance().customer = null
        CacheUtil.getInstance().setConsumerData(null)
    }

    override fun gateWay() {
        finishAffinity()
        val intent = Intent(this, GatewayActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun multibrandValidation(proceed: Boolean) {
        mainPresenter.multibrandValidation(proceed)
    }

    fun triggerMainRebirth() {
        val multiCountry =
            BuildConfig.APPLICATION_ID.contains("com.labeeb") || BuildConfig.MULTI_COUNTRY_ENABLED
        if (!multiCountry) {
            logoutUser()
        }
        CacheUtil.getInstance().clearOrder()
        SoloApi.init(
            this,
            EnvUtil.getEnvironmentUrl(),
            BuildUtil.soloAppKey(),
            CacheUtil.getInstance().conceptKey
        )
        if (!multiCountry) {
            gateWay()
        } else {
            if (AuthUtil.getInstance().isSignedIn) {
                mainPresenter.multibrandValidation(true);
            } else {
                logoutUser()
                gateWay()
            }
        }
    }

    @JvmOverloads
    fun triggerRebirth(conceptChange: Boolean = false) {
        mainPresenter.clearMenuCache()
        val multiCountry =
            BuildConfig.APPLICATION_ID.contains("com.labeeb") || BuildConfig.MULTI_COUNTRY_ENABLED
        if (!multiCountry) {
            LoginManager.getInstance().logOut()
            val googleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildUtil.googleInClient(this@MainActivity))
                    .requestEmail()
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            googleSignInClient.signOut()
            SoloAuth.getInstance().logout()
            MixpanelUtil.logout()
            AuthUtil.getInstance().customer = null
        }
        CacheUtil.getInstance().clearOrder()
        SoloApi.init(
            this,
            EnvUtil.getEnvironmentUrl(),
            BuildUtil.soloAppKey(),
            CacheUtil.getInstance().conceptKey
        )
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        if (conceptChange) {
            ProcessPhoenix.triggerRebirth(this)
        } else {
            finish()
        }
    }

    fun setCartQuantity() {
        /*try {
            if (binding.mainToolbar.notif.visibility == View.GONE) {
                binding.mainToolbar.cart.visibility = View.VISIBLE
                if (CacheUtil.getInstance().cartQuantity == 0) {
                    binding.mainToolbar.cartQuantity.visibility = View.GONE
                } else {
                    binding.mainToolbar.cartQuantity.visibility = View.VISIBLE
                    binding.mainToolbar.cartQuantity.text =
                        StringUtil.localize(CacheUtil.getInstance().cartQuantity.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

    fun cartExpired() {
        val fragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
        fragment?.cast<NewMenuFragment> {
            it.updateFloatingTotalPrice()
            it.updateFloatingTotalContainer()
        }
        fragment?.cast<NewMenuHomePageFragment> {
            it.updateFloatingTotalPrice()
            it.updateFloatingBag()
        }
    }

    fun showNotifIcon() {
        /*if (!AuthUtil.getInstance().isSignedIn || !CacheUtil.getInstance().concept.isNotificationCenterEnabled) {
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
        }*/
    }

    private fun subscriptionSelected() {
//        Customer customer = AuthUtil.getInstance().getCustomer();
//        boolean isSignedIn = AuthUtil.getInstance().isSignedIn();
//        boolean isGuest = (isSignedIn && !customer.getAttributes().getAccountType().equalsIgnoreCase("full"));

        //if (isSignedIn && !isGuest) {
        isSubMode = true
        binding.sidebar.quickOrderContainer.strokeColor =
            ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
        binding.sidebar.quickOrderLabel.setTextColor(Color.parseColor("#1E1E1E"))
        binding.sidebar.subscriptionsContainer.strokeColor =
            Color.parseColor(CacheUtil.getInstance().colorMain)
        binding.sidebar.subscriptionsLabel.setTextColor(Color.parseColor(CacheUtil.getInstance().colorMain))
        goToSubs = true
        goToMenu = false
        /*binding.mainToolbar.cartIcon.visibility = View.GONE
        binding.mainToolbar.cartQuantity.visibility = View.GONE
        binding.mainToolbar.scheduleIcon.visibility =
            if (CacheUtil.getInstance().concept.allowAdvanceOrder) View.VISIBLE else View.GONE
        binding.mainToolbar.title.text = CacheUtil.getInstance()
            .getTranslations(Text.SUBSCRIPTION_MENU, "SUBSCRIPTION MENU")*/
    }

    private fun quickOrderSelected() {
        isSubMode = false
        binding.sidebar.quickOrderContainer.strokeColor =
            Color.parseColor(CacheUtil.getInstance().colorMain)
        binding.sidebar.quickOrderLabel.setTextColor(Color.parseColor(CacheUtil.getInstance().colorMain))
        binding.sidebar.subscriptionsContainer.strokeColor =
            ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
        binding.sidebar.subscriptionsLabel.setTextColor(Color.parseColor("#1E1E1E"))
        goToSubs = false
        goToMenu = true
    }

    override fun setupFonts() {
    }

    override fun setupTranslations() {
        //binding.mainToolbar.close.text = CacheUtil.getInstance().getTranslations(Text.CLOSE)
        binding.sidebar.brands.text =
            CacheUtil.getInstance().getTranslations(Text.BRANDS, "Brands")
        binding.sidebar.promotions.text =
            CacheUtil.getInstance().getTranslations(Text.PROMOTIONS, "Promotions")
        /*binding.mainToolbar.viewOnlyLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.VIEW_ONLY, "View Only")*/
        binding.sidebar.liveChat.text = CacheUtil.getInstance()
            .getTranslations(Text.LIVE_CHAT, "Live Chat")
        /*binding.mainToolbar.back.text =
            CacheUtil.getInstance().getTranslations(Text.BACK, "Back")*/
        binding.sidebar.signin.text = CacheUtil.getInstance().getTranslations(Text.SIGN_IN)
        binding.sidebar.register.text = CacheUtil.getInstance()
            .getTranslations(Text.REGISTER, "Register")
        binding.scheduleOrderLabel.text = CacheUtil.getInstance().getTranslations(
            Text.TAP_HERE_TO_SCHEDULE_YOUR_ORDER,
            "Tap here to schedule your order"
        )
        //registerGuest.setText(CacheUtil.getInstance().getTranslations(Text.REGISTER, "Register"));
        //registerTwo.setText(CacheUtil.getInstance().getTranslations(Text.REGISTER, "Register"));
        binding.sidebar.guest.text = CacheUtil.getInstance()
            .getTranslations(Text.SIGN_IN_AS_GUEST, "Sign in as guest")
        binding.sidebar.history.text =
            CacheUtil.getInstance().getTranslations(Text.ORDER_HISTORY)
        binding.sidebar.loyalty.text = CacheUtil.getInstance()
            .getTranslations(Text.LOYALTY, "Loyalty")
        binding.sidebar.feedback.text = CacheUtil.getInstance()
            .getTranslations(Text.FEEDBACK, "Feedback")
        binding.sidebar.account.text =
            CacheUtil.getInstance().getTranslations(Text.ACCOUNT)
        binding.sidebar.favorites.text = CacheUtil.getInstance().getTranslations(Text.FAVORITES)
        binding.sidebar.activeSubscription.text = CacheUtil.getInstance().getTranslations(
            Text.ACTIVE_SUBSCRIPTION,
            "Active Subscription"
        )
        binding.sidebar.quickOrderLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.QUICK_ORDER, "Quick Order")
        binding.sidebar.subscriptionsLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.SUBSCRIPTIONS, "Subscriptions")
        binding.sidebar.notification.text = CacheUtil.getInstance()
            .getTranslations(Text.NOTIFICATIONS, "Notifications")
        if (hasLinks) {
            binding.sidebar.tvReachUs.text = CacheUtil.getInstance()
                .getTranslations(Text.REACH_US_THROUGH, "Reach us through:")
        } else {
            binding.sidebar.tvReachUs.text = CacheUtil.getInstance()
                .getTranslations(Text.SHARE_OUR_APP, "Share our app:")
        }
        binding.sidebar.contactSupportLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.CONTACT_SUPPORT, "Contact Support")
    }

    override fun setCaloriesPage(caloriesPage: CmsPage) {
        val pageTitle = caloriesPage.attributes.title.getValue(LocaleUtil.getInstance().language)
        binding.sidebar.caloriesPageLabel.text = pageTitle
        binding.sidebar.caloriesPageLabel.paintFlags =
            binding.sidebar.caloriesPageLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.sidebar.caloriesPageLabel.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this@MainActivity, BrowserActivity::class.java)
            intent.putExtra("title", pageTitle)
            intent.putExtra("url", caloriesPage.attributes.url)
            startActivity(intent)
        }
        binding.sidebar.caloriesPageLabel.visibility = View.VISIBLE
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

            R.id.nav_promotions -> NavigationUtils.navigateSafe(
                navController,
                PromotionsFragmentDirections.actionPromotionsToCart()
            )
        }
    }

    override fun showFavorites() {}

    override fun setCartQuantity(quantity: Int) {
        /*try {
            cQuantity = quantity
            if (quantity == 0) {
                binding.mainToolbar.cartQuantity.visibility = View.GONE
            } else if (binding.mainToolbar.cartIcon.visibility == View.VISIBLE) {
                binding.mainToolbar.cartQuantity.text = StringUtil.localize(quantity.toString())
                binding.mainToolbar.cartQuantity.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

    override fun showRatings(ratings: List<Rating>) {
        runOnUiThread {
            try {
                val ratingsBottomSheetDialogFragment =
                    RatingsBottomSheetDialogFragment.newInstance({ dialog, ratings ->
                        dialog.dismiss()
                        showLoadingDialog()
                        mainPresenter!!.sendOrderFeedback(ratings)
                    }, ratings)
                ratingsBottomSheetDialogFragment.show(
                    supportFragmentManager,
                    RatingsBottomSheetDialogFragment::class.java.simpleName
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun cartQuantityFetch() {
        try {
            mainPresenter.getCartQuantity()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showMessage(message: String) {
        dismissDialogs()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun refreshOrderAgain() {
        if (newMenuHomeFragmentClass?.isVisible == true) {
            newMenuHomeFragmentClass?.refreshOrderAgain()
        }
    }

    override fun showNewNotifIndicator(count: Int) {
        /*if (count > 0 && CacheUtil.getInstance().concept.isNotificationCenterEnabled && (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) && binding.mainToolbar.notif.visibility == View.VISIBLE && Values.sDeepLink.isEmpty()) {
            notifCount = count
            binding.mainToolbar.notifCounter.text = "" + count
            binding.sidebar.ivNewNotifIndicator.visibility = View.VISIBLE
            binding.mainToolbar.notifCounter.visibility =
                if (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) View.VISIBLE else View.GONE
        } else {
            binding.sidebar.ivNewNotifIndicator.visibility = View.INVISIBLE
            binding.mainToolbar.notifCounter.visibility = View.GONE
        }*/
    }

    override fun proceedRestart() {
        dismissDialogs()
        triggerRebirth(true)
    }

    override fun proceedMainRestart() {
        dismissDialogs()
        triggerMainRebirth()
    }

    override fun saveToCache(key: String, value: String) {
        runOnUiThread { CacheHelper.save(applicationContext, key, value) }
    }

    override fun animateViewToCart(itemView: View, imageUrl: String, offset: Int) {}

    fun showScheduleButton(onClick: View.OnClickListener) {
        /*if (currentDestination == R.id.nav_cart) {
            try {
                binding.mainToolbar.cart.visibility = View.GONE
                binding.mainToolbar.schedule.visibility =
                    if (CacheUtil.getInstance().concept.allowAdvanceOrder) View.VISIBLE else View.GONE
                binding.mainToolbar.scheduleIcon.setOnClickListener(onClick)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    fun showScheduleBubble() {
        /*if (CacheUtil.getInstance().concept.allowAdvanceOrder && currentDestination == R.id.nav_cart && binding.mainToolbar.schedule.visibility == View.VISIBLE) {
            try {
                binding.bubbleOrderLayout.visibility = View.VISIBLE
                binding.bubbleOrderLayout.postDelayed({
                    binding.bubbleOrderLayout.visibility = View.GONE
                }, 3000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    private fun deepLinkRedirect() {
        if (Values.sDeepLink.isEmpty()) {
            return
        }
        var redirected = false
        if (Values.sDeepLink.contains("promos")) {
            redirected = true
            showLoadingDialog()
            mainPresenter.getDigitalCoupons(Values.sDeepLinkId)
        } else if (Values.sDeepLink.contains("cart")) {
            redirected = true
            showCart()
        } else if (Values.sDeepLink.contains("order")) {
            redirected = true
        }
        if (redirected) {
            Values.sDeepLink = ""
            Values.sDeepLinkId = ""
        }
    }

    fun showHistory() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ORDER_HISTORY)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        if (!isDefaultHomeScreenDesign) {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuHomePageFragmentDirections.actionHomeToOrders()
            )
        } else {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuFragmentDirections.actionMenuToOrders()
            )
        }
    }

    fun popUpsell() {
        if (currentDestination == R.id.nav_upsell)
            navController.popBackStack()
    }

    fun setCloseButton(onClick: OnClickListener?) {
        //binding.mainToolbar.close.setOnClickListener(onClick)
    }

    fun setToolbarTitle(title: String) {
        //binding.mainToolbar.title.text = title
    }

    fun showToolbarActionButton(isShow: Boolean) {
        //binding.mainToolbar.action.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarActionText(action: String) {
        //binding.mainToolbar.action.text = action
    }

    fun setToolbarActionButton(onClick: OnClickListener?) {
        //binding.mainToolbar.action.setOnClickListener(onClick)
    }

    fun enableToolbarActionButton(isEnabled: Boolean) {
        //binding.mainToolbar.action.isEnabled = isEnabled
    }

    fun showToolbarWhatsappButton(isShow: Boolean) {
        //binding.mainToolbar.whatsapp.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarWhatsappButton(onClick: OnClickListener) {
        //binding.mainToolbar.whatsapp.setOnClickListener(onClick)
    }

    fun showToolbarCustomActionButton(isShow: Boolean) {
        //binding.mainToolbar.customAction.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setToolbarCustomActionText(action: String) {
        //binding.mainToolbar.customAction.text = action
    }

    fun setToolbarCustomActionButton(onClick: OnClickListener) {
        //binding.mainToolbar.customAction.setOnClickListener(onClick)
    }

    private fun refreshMenu() {
        val fragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
        fragment?.cast<NewMenuFragment> {
            it.refreshFavorites()
        }
        fragment?.cast<NewMenuHomePageFragment> {
            it.refreshFavorites()
        }
    }
}