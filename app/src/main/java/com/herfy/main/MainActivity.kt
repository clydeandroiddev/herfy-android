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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.capillary.hydra.android.sdk.HydraAPI
import com.capillary.hydra.android.sdk.HydraAPI.Companion.getDefaultInstance
import com.capillary.hydra.firebase.HydraFirebaseAPI
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.animation.ActivitySwitcher
import com.herfy.auth.views.AuthActivity
import com.herfy.base.BaseActivity
import com.herfy.browser.BrowserActivity
import com.herfy.cart.views.CartFragment
import com.herfy.cmspage.CmsPageActivity
import com.herfy.country.CountriesFragment
import com.herfy.databinding.ActivityMainBinding
import com.herfy.digitalcoupons.views.DigitalCouponFragmentDirections
import com.herfy.digitalcoupons.views.DigitalCouponsFragmentDirections
import com.herfy.ext.cast
import com.herfy.ext.readAssetsFile
import com.herfy.favorites.FavoritesFragment
import com.herfy.favorites.FavoritesFragmentDirections
import com.herfy.feedback.FeedbackActivity
import com.herfy.home.views.HomeFragment
import com.herfy.home.views.HomeFragmentDirections
import com.herfy.loyalty.LoyaltyActivity
import com.herfy.main.adapter.LanguageAdapter
import com.herfy.main.adapter.SwitchServerAdapter
import com.herfy.menu.view.MenuFragment
import com.herfy.menuitem.view.MenuItemDetailFragmentDirections
import com.herfy.newmenu.NewMenuFragmentCommonListener
import com.herfy.newmenu.views.NewMenuFragment
import com.herfy.newmenu.views.NewMenuHomePageFragment
import com.herfy.newmenu.views.NewMenuHomePageFragmentDirections
import com.herfy.newmenu.views.PopularNowFragmentDirections
import com.herfy.promotions.views.PromotionsFragmentDirections
import com.herfy.splash.SplashActivity
import com.herfy.util.AppLogger
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil
import com.herfy.util.obfuscator.Keychain
import com.herfy.util.obfuscator.reveal
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheHelper
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CoroutineContextProvider
import com.herfy.utils.helper.EnvUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.herfy.utils.theme.DynamicTheme
import com.jakewharton.processphoenix.ProcessPhoenix
import com.skylinedynamics.solosdk.api.SoloApi
import com.skylinedynamics.solosdk.api.SoloAuth
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

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
    private var languageAdapter: LanguageAdapter? = null
    var isDrawerOpen = false

    private var authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (AuthUtil.getInstance().isSignedIn) {
                    setupSideBar()
                }
            }
        }

    private val cacheUtilHelper by lazy {
        CacheUtilHelper(this)
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
            val i = Intent(this@MainActivity, MainActivity::class.java)
            startActivity(i)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }else {
                overridePendingTransition(0, 0)
            }
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
        graph.setStartDestination(R.id.nav_herfy_home)
        currentDestination = R.id.nav_herfy_home

        navController.setGraph(graph, intent.extras)
        mainPresenter = MainPresenter(this@MainActivity)
        mainPresenter.start()

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            dismissDialogs()
            if (currentDestination == R.id.nav_auth || currentDestination == R.id.nav_guest) {
                setupSideBar()
            }


            currentDestination = destination.id
            binding.toolbarMain.apply {
                if (currentDestination == R.id.nav_herfy_home || currentDestination == R.id.nav_dialog_popup) {
                    setStatusBarColor(ContextCompat.getColor(this@MainActivity, com.herfy.commonstyles.R.color.toolbar_color))
                    hide(false)
                    setCartQuantity()
                    setupSideBar()
                } else {
                    setStatusBarColor(DynamicTheme.getDefaultPrimaryColor())
                    hide(true)
                }
            }
        }

        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            binding.sidebar.fmBrands.visibility = View.VISIBLE
            binding.sidebar.concept.setOnClickListener { view: View? ->
                showCountries()
            }
            binding.sidebar.brands.setOnClickListener { view: View? ->
                showCountries()
            }
        } else {
            binding.sidebar.fmBrands.visibility = View.GONE
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
                spinnerLang.visibility = View.INVISIBLE
            } else {
                var currentItemIndex = 0
                val availableLanguages = CacheUtil.getInstance().concept.languages.toList()
                val languages = arrayListOf<String>()
                availableLanguages.find { lang -> lang.equals(LocaleUtil.EN_US, ignoreCase = true) }
                    ?.let {
                        languages.add(getString(R.string.eng))
                    }
                availableLanguages.find { lang -> lang.equals(LocaleUtil.AR_SA, ignoreCase = true) }
                    ?.let {
                        languages.add(getString(R.string.arabic))
                    }
                availableLanguages.find { lang -> lang.equals(LocaleUtil.FR, ignoreCase = true) }
                    ?.let {
                        languages.add(getString(R.string.fr))
                    }
                val currentSelectedLanguage = LocaleUtil.getLocale().language
                for (i in languages.indices) {
                    if (currentSelectedLanguage.equals("en", true) && languages[i].equals(
                            getString(
                                R.string.eng
                            )
                        )
                    ) {
                        currentItemIndex = i
                    } else if (currentSelectedLanguage.equals("ar", true) && languages[i].equals(
                            getString(R.string.arabic)
                        )
                    ) {
                        currentItemIndex = i
                    } else if (currentSelectedLanguage.equals("fr", true) && languages[i].equals(
                            getString(R.string.fr)
                        )
                    ) {
                        currentItemIndex = i
                    }
                }

                languageAdapter = LanguageAdapter(
                    this@MainActivity,
                    R.layout.item_language,
                    languages,
                    currentItemIndex,
                    R.id.text1
                )
                spinnerLang.adapter = languageAdapter
                spinnerLang.setSelection(currentItemIndex)
                spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        if (p2 == currentItemIndex) {
                            return
                        }

                        val language: String = when (languages[p2]) {
                            getString(R.string.eng) -> "en"
                            getString(R.string.arabic) -> "ar"
                            getString(R.string.fr) -> "fr"
                            else -> ""
                        }

                        if (language.isNotEmpty()) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                            val eventAttributes: HashMap<String, String> = HashMap()
                            LocaleUtil.getInstance().language = language
                            eventAttributes["ChangeLanguage"] =
                                language.uppercase(Locale.getDefault())
                            logEvent("ChangeLanguage", eventAttributes, null, 0.0)
                            LocaleUtil.getInstance().setLocale(this@MainActivity)
                            val config: Configuration = Configuration()
                            config.locale = Locale(language)
                            resources.updateConfiguration(config, resources.displayMetrics)
                            SoloAuth.getInstance()
                                .setLanguage(LocaleUtil.getInstance().languageSymbol)
                            mainPresenter.changeLanguage()

                            val intent: Intent =
                                Intent(this@MainActivity, SplashActivity::class.java)
                            intent.putExtra("fromChangeLanguage", true)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                            finish()
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
                spinnerLang.visibility = View.VISIBLE
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

    override fun preloadImage(imageUrl: String, requestOptions: RequestOptions) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).downloadOnly().load(imageUrl).apply(requestOptions).submit()
            }
        }
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        dismissDialogs()/*if (isDefaultHomeScreenDesign) {
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
        }*/
    }

    fun upsellEvent(menuItem: MenuItem, menuCategory: MenuCategory) {
        dismissDialogs()/*if (isDefaultHomeScreenDesign) {
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
        }*/
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

    private fun showSocialMediaLinks() {
        binding.sidebar.cvIg.setOnClickListener {
            openSocialMediaLink(Values.INSTAGRAM_LINK)
        }
        binding.sidebar.cvFacebook.setOnClickListener {
            openSocialMediaLink(Values.FACEBOOK_LINK)
        }
        binding.sidebar.cvTiktok.setOnClickListener {
            openSocialMediaLink(Values.TIKTOK_LINK)
        }
        binding.sidebar.cvTwitter.setOnClickListener {
            openSocialMediaLink(Values.TWITTER_LINK)
        }
    }

    private fun openSocialMediaLink(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            navCmsPage(
                CacheUtil.getInstance()
                    .getTranslations(Text.SOCIAL_MEDIA_LABEL, "Follow Us On Social Media"), url
            )
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
        mainPresenter.apply {
            getFaqs()
            getAboutUs()
            getTermsAndConditions()
        }
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
                    val fragment =
                        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
                    showOrderDetails("true", orderId)
                    fragment?.cast<HomeFragment> {
                        setCartQuantity(CacheUtil.getInstance().cartQuantity)
                    }
                    /*fragment?.cast<NewMenuFragment> {
                        it.showOrder(orderId)
                    }
                    fragment?.cast<NewMenuHomePageFragment> {
                        it.showOrder(orderId)
                    }
                    fragment?.cast<CartFragment> {
                        it.showOrder(orderId)
                    }*/
                }
            } else if (forceLocation) {/*if (!isDefaultHomeScreenDesign) {
                    NavigationUtils.navigateSafe(
                        navController,
                        NewMenuHomePageFragmentDirections.actionHomeToOrderType()
                    )
                } else {
                    NavigationUtils.navigateSafe(
                        navController,
                        NewMenuFragmentDirections.actionMenuToOrderType()
                    )
                }*/
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
        setCartQuantity()

        signUpHydra()

        //HYDRA LOGS

        if (!AuthUtil.getInstance().firebase.isEmpty() && AuthUtil.getInstance().isSignedIn) {
            //HYDRA SIGN IN EVENT
            getDefaultInstance(applicationContext)!!.pushUserSignin(
            AuthUtil.getInstance().customer.id,
            AuthUtil.getInstance().customer.attributes.firstName,
            AuthUtil.getInstance().customer.attributes.lastName,
            AuthUtil.getInstance().customer.attributes.email,
            AuthUtil.getInstance().customer.attributes.mobile,
            emptyMap<String, Any>())

            HydraFirebaseAPI.getInstance()
                .passPushToken(applicationContext, AuthUtil.getInstance().firebase)

            AppLogger.longError("HYDRA CAPILLARY FCM TOKEN " + AuthUtil.getInstance().firebase)
            AppLogger.longError("HYDRA CAPILLARY Customer Data " + Gson().toJson(AuthUtil.getInstance().customer))
        }
    }

    private fun setupSideBar() {
        binding.navigationLayout.apply {
            val screenWidth = ViewUtil.getScreenWidth(this@MainActivity)
            val drawerWidthMultiplier = ViewUtil.dpToPx(this@MainActivity, 129)
            val calcDrawerWidth = (screenWidth - drawerWidthMultiplier).toInt()
            layoutParams.width = calcDrawerWidth
        }
        val customer = AuthUtil.getInstance().customer
        val isSubs = CacheUtil.getInstance().mode == 2
        val isSignedIn = AuthUtil.getInstance().isSignedIn
        val isGuest =
            (isSignedIn && !customer.attributes.accountType.equals("full", ignoreCase = true))
        if (isSignedIn) {
            val customerNameLabel =
                CacheUtil.getInstance().getTranslations(Text.HI_LABEL, "Hi (x)!")
            binding.sidebar.customerName.text = customerNameLabel.replace(
                "(x)", if (isSignedIn) customer.attributes.firstName.trim() else ""
            )
            binding.sidebar.clNotification.visible()
            binding.sidebar.fmLoyalty.visible()
        } else {
            binding.sidebar.customerName.text =
                CacheUtil.getInstance().getTranslations(Text.HI_LOGGED_OUT, "Hi!")
            binding.sidebar.clNotification.gone()
            binding.sidebar.fmLoyalty.gone()
        }
        val containsLoyalty =
            CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes().contains(
                PaymentType.LOYALTY
            ) ||
                    CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                        .contains(PaymentType.LOYALTY)
        if(containsLoyalty && isSignedIn){
            binding.sidebar.fmLoyalty.visible()
        }else {
            binding.sidebar.fmLoyalty.gone()
        }
        binding.sidebar.fmAccount.visibility = if (isSignedIn) View.VISIBLE else View.GONE
        binding.sidebar.fmActiveSubscription.visibility =
            if (isSubs && isSignedIn) View.VISIBLE else View.GONE
        binding.sidebar.fmSignin.visibility =
            if (isSignedIn && !isGuest) View.GONE else View.VISIBLE
        val loggedInRules = if (isSignedIn) (if (isSubs) View.GONE else View.VISIBLE) else View.GONE
        binding.sidebar.fmHistory.visibility = loggedInRules
        binding.sidebar.fmFavorites.visibility = loggedInRules
        binding.sidebar.selectionLayout.visibility =
            if (BuildConfig.SUBSCRIPTION_ENABLED) View.VISIBLE else View.GONE
        binding.sidebar.fmSupport.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
        }
        binding.sidebar.fmStoreLocator.isVisible = isSignedIn
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
            eventName ?: "", eventAttributes ?: HashMap(), eventMetricName ?: "", eventMetricValue
        )
    }

    override fun setupViews() {
        mainPresenter.getCaloriesPage()
        binding.drawerLayout.apply {
            setScrimColor(Color.parseColor("#339C9C9C"))
            addDrawerListener(object : DrawerListener {
                override fun onDrawerSlide(p0: View, p1: Float) {
                    if (isDrawerOpen) {
                        Blurry.delete(binding.drawerMain)
                    }
                }

                override fun onDrawerOpened(p0: View) {
                    isDrawerOpen = true
                }

                override fun onDrawerClosed(p0: View) {
                    isDrawerOpen = false
                }

                override fun onDrawerStateChanged(p0: Int) {

                }

            })
        }
        showSocialMediaLinks()
        binding.sidebar.signin.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("sign_in", true)
            intent.putExtra("guest", false)
            intent.putExtra("curbside", false)
            intent.putExtra("checkout", false)
            authLauncher.launch(intent)
        }
        binding.sidebar.notification.ninjaTap(lifecycleScope) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigateToDeepLink("nav_notifications")
        }
        binding.sidebar.fmHistory.setOnClickListener { showHistory() }
        binding.sidebar.fmLoyalty.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this@MainActivity, LoyaltyActivity::class.java)
            startActivity(intent)
            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
        }
        binding.sidebar.fmFeedback.setOnClickListener {
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
                    intent.putExtra(
                        "title", CacheUtil.getInstance().getTranslations(
                            Text.FEEDBACK
                        )
                    )
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
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showNotifications()
        }
        binding.sidebar.fmAccount.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
//            val intent = Intent(this@MainActivity, AccountActivity::class.java)
//            startActivity(intent)
//            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigateToDeepLink("profile_fragment_nav")
        }
        binding.sidebar.fmStoreLocator.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigateToDeepLink("store_locator_nav")
        }
        binding.sidebar.fmFavorites.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigateToDeepLink("nav_favorites")
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

        binding.sidebar.fmPromotions.setOnClickListener { view: View? ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigateToDeepLink("nav_promotions")
        }
        binding.toolbarMain.apply {
            setSideButtonAction {
                Blurry.with(this@MainActivity).radius(25).sampling(2).onto(binding.drawerMain)
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
            setCartButtonAction {
                showCart()
            }
        }

        if (intent.getBooleanExtra("autoLogin", false)) {
            intent.removeExtra("autoLogin")
            binding.sidebar.signin.performClick()
        }
    }

    private fun showNotifications() {
        /*if (isDefaultHomeScreenDesign || currentDestination == R.id.nav_menu) {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuFragmentDirections.actionMenuToNotifications()
            )
        } else {
            NavigationUtils.navigateSafe(
                navController,
                NewMenuHomePageFragmentDirections.actionHomeToNotifications()
            )
        }*/
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
                    serverTypeStr,
                    object : TypeToken<ArrayList<com.herfy.model.ServerType>>() {}.type
                )
            )
            val env = envList.find { s ->
                s.url.equals(
                    EnvUtil.getEnvironmentUrl(), ignoreCase = true
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
            .requestIdToken(BuildUtil.googleInClient(this@MainActivity)).requestEmail().build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        googleSignInClient.signOut()
        SoloAuth.getInstance().logout()
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

    override fun setFaqs(content: String) {
        binding.sidebar.fmFaqs.apply {
            setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                navCmsPage(
                    title = CacheUtil.getInstance().getTranslations(Text.FAQS_LABEL, "FAQs"),
                    content = content
                )
            }
            visible()
        }
    }

    override fun setAboutUs(content: String) {
        binding.sidebar.fmAboutUs.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navCmsPage(
                title = CacheUtil.getInstance().getTranslations(Text.ABOUT_US_LABEL, "About Us"),
                content = content
            )
        }
    }

    override fun setTermsAndConditions(content: String) {
        binding.sidebar.fmTerms.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navCmsPage(
                title = CacheUtil.getInstance()
                    .getTranslations(Text.TERMS_LABEL, "Terms & Condition"), content = content
            )
        }
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
                    .requestIdToken(BuildUtil.googleInClient(this@MainActivity)).requestEmail()
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            googleSignInClient.signOut()
            SoloAuth.getInstance().logout()
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

    @OptIn(DelicateCoroutinesApi::class)
    fun setCartQuantity() {
        GlobalScope.launch(context = CoroutineContextProvider().Main){
            cacheUtilHelper.getCartItems {
                cacheUtilHelper.setCartQuantity(it)
                binding.toolbarMain.setCart(cacheUtilHelper.getCartQuantity())
            }
        }

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
        fragment?.cast<HomeFragment> {
            setCartQuantity()
        }
        fragment?.cast<MenuFragment> {
            it.setCartQuantity()
        }
        fragment?.cast<CartFragment> {
            it.cartExpired()
        }
        fragment?.cast<FavoritesFragment> {
            it.updateFavoriteQuantity()
        }
    }

    fun showNotifIcon() {/*if (!AuthUtil.getInstance().isSignedIn || !CacheUtil.getInstance().concept.isNotificationCenterEnabled) {
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
        goToMenu = false/*binding.mainToolbar.cartIcon.visibility = View.GONE
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
        binding.sidebar.brands.text = CacheUtil.getInstance().getTranslations(Text.BRANDS, "Brands")
        binding.sidebar.promotions.text =
            CacheUtil.getInstance().getTranslations(Text.PROMOTIONS, "Promotions")
        binding.sidebar.liveChat.text =
            CacheUtil.getInstance().getTranslations(Text.LIVE_CHAT, "Live Chat")
        binding.sidebar.signin.text = CacheUtil.getInstance()
            .getTranslations(Text.GET_STARTED_LABEL, "Get Started with Herfy")
        binding.scheduleOrderLabel.text = CacheUtil.getInstance().getTranslations(
            Text.TAP_HERE_TO_SCHEDULE_YOUR_ORDER, "Tap here to schedule your order"
        )
        binding.sidebar.history.text =
            CacheUtil.getInstance().getTranslations(Text.YOUR_ORDERS_LABEL, "Your Orders")
        binding.sidebar.loyalty.text =
            CacheUtil.getInstance().getTranslations(Text.LOYALTY_REWARDS_LABEL, "Loyalty & Rewards")
        binding.sidebar.feedback.text =
            CacheUtil.getInstance().getTranslations(Text.FEEDBACK, "Feedback")
        binding.sidebar.account.text =
            CacheUtil.getInstance().getTranslations(Text.MY_PROFILE_LABEL, "My Profile")
        binding.sidebar.favorites.text =
            CacheUtil.getInstance().getTranslations(Text.FAVOURITES_LABEL, "Favourites")
        binding.sidebar.activeSubscription.text = CacheUtil.getInstance().getTranslations(
            Text.ACTIVE_SUBSCRIPTION, "Active Subscription"
        )
        binding.sidebar.quickOrderLabel.text =
            CacheUtil.getInstance().getTranslations(Text.QUICK_ORDER, "Quick Order")
        binding.sidebar.subscriptionsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBSCRIPTIONS, "Subscriptions")
        binding.sidebar.notification.text =
            CacheUtil.getInstance().getTranslations(Text.NOTIFICATIONS, "Notifications")
        binding.sidebar.tvReachUs.text = CacheUtil.getInstance()
            .getTranslations(Text.SOCIAL_MEDIA_LABEL, "Follow Us On Social Media")
        binding.sidebar.support.text =
            CacheUtil.getInstance().getTranslations(Text.HELP_SUPPORT_LABEL, "Help & Support")
        binding.sidebar.faqs.text = CacheUtil.getInstance().getTranslations(Text.FAQS_LABEL, "FAQs")
        binding.sidebar.aboutUs.text =
            CacheUtil.getInstance().getTranslations(Text.ABOUT_US_LABEL, "About Us")
        binding.sidebar.terms.text =
            CacheUtil.getInstance().getTranslations(Text.TERMS_LABEL, "Terms & Condition")
        binding.sidebar.refer.text =
            CacheUtil.getInstance().getTranslations(Text.REFER_AND_EARN_LABEL, "Refer & Earn")
        binding.sidebar.storeLocator.text =
            CacheUtil.getInstance().getTranslations(Text.STORE_LOCATOR, "Store Locator")
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

    fun showCartFromHerfyHome() {
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            return
        }
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            currentDestination = R.id.nav_herfy_home
            NavigationUtils.navigateSafe(
                navController, HomeFragmentDirections.actionHomeToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )
        }, 500)
    }

    override fun showCart() {
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            return
        }
        when (currentDestination) {
            R.id.nav_herfy_home -> NavigationUtils.navigateSafe(
                navController, HomeFragmentDirections.actionHomeToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_home -> NavigationUtils.navigateSafe(
                navController, NewMenuHomePageFragmentDirections.actionHomeToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_menu_item_detail -> NavigationUtils.navigateSafe(
                navController, MenuItemDetailFragmentDirections.actionMenuItemToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_favorites -> NavigationUtils.navigateSafe(
                navController, FavoritesFragmentDirections.actionFavoritesToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_popular_now -> NavigationUtils.navigateSafe(
                navController, PopularNowFragmentDirections.actionPopularNowToCart(
                    CacheUtil.getInstance().preselectedBranch
                )
            )

            R.id.nav_digital_coupons -> NavigationUtils.navigateSafe(
                navController, DigitalCouponsFragmentDirections.actionDigitalCouponsToCart()
            )

            R.id.nav_digital_coupon -> NavigationUtils.navigateSafe(
                navController, DigitalCouponFragmentDirections.actionDigitalCouponToCart()
            )

            R.id.nav_promotions -> NavigationUtils.navigateSafe(
                navController, PromotionsFragmentDirections.actionPromotionsToCart()
            )
        }
    }

    override fun showFavorites() {}

    override fun setCartQuantity(quantity: Int) {
        binding.toolbarMain.setCart(quantity)
    }

    override fun showRatings(ratings: List<Rating>) {

        //HIDE this
//        runOnUiThread {
//            try {
//                val ratingsBottomSheetDialogFragment =
//                    RatingsBottomSheetDialogFragment.newInstance({ dialog, ratings ->
//                        dialog.dismiss()
//                        showLoadingDialog()
//                        mainPresenter!!.sendOrderFeedback(ratings)
//                    }, ratings)
//                ratingsBottomSheetDialogFragment.show(
//                    supportFragmentManager, RatingsBottomSheetDialogFragment::class.java.simpleName
//                )
//            } catch (ex: Exception) {
//                ex.printStackTrace()
//            }
//        }
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

    override fun showNewNotifIndicator(count: Int) {/*if (count > 0 && CacheUtil.getInstance().concept.isNotificationCenterEnabled && (currentDestination == R.id.nav_home || currentDestination == R.id.nav_menu) && binding.mainToolbar.notif.visibility == View.VISIBLE && Values.sDeepLink.isEmpty()) {
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

    fun showScheduleButton(onClick: View.OnClickListener) {/*if (currentDestination == R.id.nav_cart) {
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

    fun showScheduleBubble() {/*if (CacheUtil.getInstance().concept.allowAdvanceOrder && currentDestination == R.id.nav_cart && binding.mainToolbar.schedule.visibility == View.VISIBLE) {
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

    fun showOrderDetails(fromPayment: String, orderId: String) {
        navController.navigateToDeepLink("nav_new_order_tracker/${fromPayment}/${orderId}")
    }

    fun showHistory() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        navController.navigateToDeepLink("new_history_fragment_nav")
    }

    fun popUpsell() {
        if (currentDestination == R.id.nav_upsell) navController.popBackStack()
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

    private fun navCmsPage(title: String, content: String) {
        val intent: Intent
        if (URLUtil.isValidUrl(content)) {
            intent = Intent(this, BrowserActivity::class.java)
            intent.putExtra("url", content)
        } else {
            intent = Intent(this, CmsPageActivity::class.java)
            intent.putExtra("content", content)
        }
        intent.putExtra("title", title)
        startActivity(intent)
    }

    private fun signUpHydra() {
        val auth = AuthUtil.getInstance()
        if (!auth.isSignedIn) return

        if (BuildConfig.VERSION_NAME != "3.1.2") return

        val customer = auth.customer ?: return
        val phone =customer.attributes?.mobile ?: ""
        if (phone.isBlank()) return

        if (!auth.isHydraSignedUp(phone)) {
            HydraAPI.getDefaultInstance(this@MainActivity)?.pushUserSignup(
                cuid = customer.id ?: "",
                firstName = customer.attributes?.firstName,
                lastName  = customer.attributes?.lastName,
                email     = customer.attributes?.email,
                phone     = phone,
                customData = emptyMap()
            )
            auth.saveHydraSignUp(phone)
        }
    }
}