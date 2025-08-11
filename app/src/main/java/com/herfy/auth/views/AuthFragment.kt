package com.herfy.auth.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.capillary.hydra.android.sdk.HydraAPI
import com.capillary.hydra.android.sdk.HydraAPI.Companion.getDefaultInstance
import com.daimajia.androidanimations.library.Techniques
import com.facebook.BuildConfig
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.setApplicationId
import com.facebook.GraphRequest
import com.facebook.LoggingBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.herfy.R
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.auth.AuthContract
import com.herfy.auth.AuthFragmentPagerAdapter
import com.herfy.auth.AuthPresenter
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.cart.views.CartFragment
import com.herfy.databinding.FragmentAuthBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.newmenu.views.NewMenuHomePageFragment
import com.herfy.order.views.ordertype.OrderTypeNewFragment
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.herfy.verification.CodeBroadcastReceiver
import com.herfy.verification.views.VerifyCodeDialogFragment
import com.skylinedynamics.solosdk.api.SoloAuth
import org.json.JSONException
import java.util.Locale
import java.util.Map
import java.util.regex.Pattern

class AuthFragment : BaseFragment(), AuthContract.View, FacebookCallback<LoginResult>,
    SignInFragment.Listener {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
    }

    private lateinit var binding: FragmentAuthBinding

    private val args: AuthFragmentArgs by navArgs()

    private lateinit var authPresenter: AuthContract.Presenter

    private var adapter: AuthFragmentPagerAdapter? = null
    private var signIn = true
    private var orderHistory = false
    private var cart = false
    private var orderType = false
    private var favoriteHome = false
    private var favoriteMenuItem = false
    private var fromNormal = false
    private var curbside = false
    private var facebookCallbackManager = CallbackManager.Factory.create()
    private var googleSignInClient: GoogleSignInClient? = null
    private var verifyCodeDialogFragment: VerifyCodeDialogFragment? = null
    private var codeBroadcastReceiver: CodeBroadcastReceiver? = null

    private var googleLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result != null && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(
                    ApiException::class.java
                )
                if (account != null) {
                    showLoadingDialog()
                    authPresenter.signInGoogle(account.email, account.idToken)
                } else {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR))
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                showMessage(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR))
            }
        }
    }
    private var codeLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result != null && result.data != null) {
            try {
                val message = result.data!!.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                if (message != null) {
                    val otpLength = CacheUtil.getInstance().concept.otpLength.toString()
                    val p = Pattern.compile(String.format("\\b\\d{%s}\\b", otpLength))
                    val m = p.matcher(message)
                    var codeValue: String? = ""
                    while (m.find()) {
                        codeValue = m.group(0)
                    }
                    if (verifyCodeDialogFragment != null) {
                        verifyCodeDialogFragment!!.setCode(codeValue)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authPresenter = AuthPresenter(this)
        signIn = args.signIn

        if(BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dismissDialogs()
        checkBackground()
        authPresenter!!.start()
        LoginManager.getInstance().registerCallback(facebookCallbackManager, this)
        activity?.let {
            val googleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildUtil.googleInClient(it))
                    .requestEmail()
                    .build()
            googleSignInClient = GoogleSignIn.getClient(it, googleSignInOptions)
        }
    }

    private fun checkBackground() {
        binding.tabLayout.setBackgroundResource(R.color.page_background_color)
    }

    private fun navigateUp() {
        activity?.cast<MainActivity> {
            sendFragmentResult()
        }
        activity?.cast<NavigationActivity> {
            sendFragmentResult()
        }
        activity?.cast<AuthActivity> {
            it.setResult(Activity.RESULT_OK, Intent())
            it.finish()
        }
    }

    private fun sendFragmentResult() {
        setFragmentResult(
            ARGS_KEY_FRAGMENT_RESULT,
            bundleOf(
                NewMenuHomePageFragment.ARGS_KEY_HISTORY to args.history,
                NewMenuHomePageFragment.ARGS_KEY_ACCOUNT to args.account,
                OrderTypeNewFragment.ARGS_KEY_CURBSIDE to args.curbside,
                CartFragment.ARGS_KEY_CHECKOUT to args.checkout
            )
        )
        findNavController().navigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        codeBroadcastReceiver?.let { receiver ->
            activity?.let {
                it.unregisterReceiver(receiver)
            }
        }
        googleLauncher = null
        codeLauncher = null
    }

    override fun setPresenter(presenter: AuthContract.Presenter) {
        authPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {
        animateSpringView(Techniques.SlideInUp, 250, binding.tabLayout, 8)
        adapter = AuthFragmentPagerAdapter(childFragmentManager)
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.viewPager.currentItem = if (signIn) 0 else 1
        signIn = false
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                adapter!!.notifyDataSetChanged()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.close.setOnClickListener { v: View? -> navigateUp() }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {}
    override fun signInEmail(email: String, password: String, mobileFormatIndex: Int) {
        showLoadingDialog()
        authPresenter!!.signIn(email, password, mobileFormatIndex)
    }

    override fun forgotPassword(phoneNumber: String?, mobileFormatIndex: Int) {
        showLoadingDialog()
        authPresenter!!.forgotPassword(phoneNumber, mobileFormatIndex)
    }

    override fun signInFacebook() {
        showLoadingDialog()
        LoginManager.getInstance().logOut()
        showLoadingDialog()
        setApplicationId(getString(R.string.facebook_app_id))
        val permissions: Collection<String> = listOf("public_profile", "email")
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            facebookCallbackManager,
            permissions
        )
    }

    override fun signInGoogle() {
        showLoadingDialog()
        googleSignInClient!!.signOut()
        showLoadingDialog()
        val signInIntent = googleSignInClient!!.signInIntent
        googleLauncher!!.launch(signInIntent)
    }

    override fun signUp(
        fullName: String,
        phoneNumber: String,
        email: String,
        password: String,
        index: Int
    ) {
        showLoadingDialog()
        authPresenter!!.signUp(fullName, phoneNumber, email, password, index)
    }

    override fun showErrorEmail(error: String) {
        val baseFragment = adapter!!.getItem(binding.viewPager.currentItem) as BaseFragment
        if (baseFragment is SignInFragment) {
            baseFragment.showErrorEmail(error)
        } else if (baseFragment is RegisterFragment) {
            baseFragment.showErrorEmail(error)
        }
        if (!error.isEmpty()) {
            dismissDialogs()
        }
    }

    override fun showErrorPassword(error: String) {
        val baseFragment = adapter?.getItem(binding.viewPager.currentItem) as BaseFragment
        if (baseFragment is SignInFragment) {
            baseFragment.showErrorPassword(error)
        } else if (baseFragment is RegisterFragment) {
            baseFragment.showErrorPassword(error)
        }
        if (!error.isEmpty()) {
            dismissDialogs()
        }
    }

    override fun showErrorUsername(error: String) {
        val baseFragment = adapter!!.getItem(binding.viewPager.currentItem) as AuthContract.View
        baseFragment.showErrorUsername(error)
        if (!error.isEmpty()) dismissDialogs()
    }

    override fun showMessage(message: String) {
        dismissDialogs()
        activity?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showAlertDialog(title: String, success: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showAlertDialog(title, success)
        }
    }

    override fun showError(error: String) {
        showAlertDialog("", error)
    }

    override fun showSuccess(success: String) {
        showAlertDialog("", success)
    }

    override fun showErrorFirstName(error: String) {
        val baseFragment = adapter!!.getItem(binding.viewPager.currentItem) as BaseFragment
        if (baseFragment is RegisterFragment) {
            baseFragment.showErrorFirstName(error)
        }
        if (error.isNotEmpty()) {
            dismissDialogs()
        }
    }

    override fun showErrorPhoneNumber(error: String) {
        val baseFragment = adapter!!.getItem(binding.viewPager.currentItem) as BaseFragment
        if (baseFragment is RegisterFragment) {
            baseFragment.showErrorPhoneNumber(error)
        } else if (baseFragment is SignInFragment) {
            baseFragment.showErrorPhoneNumber(error)
        }
        if (error.isNotEmpty()) {
            dismissDialogs()
        }
    }

    override fun showGuest() {
        setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
            clearFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT)
            if (AuthUtil.getInstance().isSignedIn) {
                navigateUp()
            }
        }
        NavigationUtils.navigateSafe(
            findNavController(),
            AuthFragmentDirections.actionAuthToGuest(args.checkout)
        )
    }

    override fun showPhoneNumber(customerId: String, email: String, password: String) {
        dismissDialogs()
        codeBroadcastReceiver = CodeBroadcastReceiver()
        codeBroadcastReceiver?.setCodeBroadcastReceiverListener(object :
            CodeBroadcastReceiver.CodeBroadcastReceiverListener {
            override fun onReceived(intent: Intent) {
                try {
                    codeLauncher?.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            override fun onTimeout() {
                AppLogger.error("CodeBroadcastReceiver onTimeout")
            }
        })
        activity?.let {
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            it.registerReceiver(
                codeBroadcastReceiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null
            )
            val task = SmsRetriever.getClient(it).startSmsUserConsent(null)
            task.addOnSuccessListener { aVoid: Void? -> }
            task.addOnFailureListener { e: Exception -> AppLogger.error(e.message) }
        }
        val args = Bundle()
        args.putBoolean("guest", false)
        args.putBoolean("order_history", orderHistory)
        args.putBoolean("cart", cart)
        args.putBoolean("order_type", orderType)
        args.putBoolean("favorite_home", favoriteHome)
        args.putBoolean("favorite_menu_item", favoriteMenuItem)
        args.putString("customer_id", customerId)
        args.putString("email", email)
        args.putString("password", password)
        args.putString("phone_number", "")
        verifyCodeDialogFragment = VerifyCodeDialogFragment.newInstance()
        verifyCodeDialogFragment?.arguments = args
        verifyCodeDialogFragment?.show(
            childFragmentManager,
            VerifyCodeDialogFragment::class.java.simpleName
        )
        if (authPresenter.registrationMethod.isNotEmpty()) {
            logRegistrationEvent(authPresenter.registrationMethod)
        }
    }

    override fun setupCodeBroadcastListener() {
        codeBroadcastReceiver = CodeBroadcastReceiver()
        codeBroadcastReceiver!!.setCodeBroadcastReceiverListener(object :
            CodeBroadcastReceiver.CodeBroadcastReceiverListener {
            override fun onReceived(intent: Intent) {
                try {
                    codeLauncher?.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            override fun onTimeout() {
                AppLogger.error("CodeBroadcastReceiver onTimeout")
            }
        })
        activity?.let {
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            it.registerReceiver(
                codeBroadcastReceiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null
            )
            val task = SmsRetriever.getClient(it).startSmsUserConsent(null)
            task.addOnSuccessListener { aVoid: Void? -> }
            task.addOnFailureListener { e: Exception -> AppLogger.error(e.message) }
        }
    }

    override fun showCode(
        guest: Boolean,
        customerId: String,
        email: String,
        password: String,
        phoneNumber: String
    ) {
        dismissDialogs()
        setupCodeBroadcastListener()
        val args = Bundle()
        args.putBoolean("guest", guest)
        args.putBoolean("order_history", orderHistory)
        args.putBoolean("cart", cart)
        args.putBoolean("order_type", orderType)
        args.putBoolean("favorite_home", favoriteHome)
        args.putBoolean("favorite_menu_item", favoriteMenuItem)
        args.putString("customer_id", customerId)
        args.putString("email", email)
        args.putString("password", password)
        args.putString("phone_number", phoneNumber)
        verifyCodeDialogFragment = VerifyCodeDialogFragment.newInstance()
        verifyCodeDialogFragment?.arguments = args
        verifyCodeDialogFragment?.show(
            childFragmentManager,
            VerifyCodeDialogFragment::class.java.simpleName
        )
        if (authPresenter.registrationMethod.isNotEmpty()) {
            logRegistrationEvent(authPresenter.registrationMethod)
        }
    }

    override fun showMain() {
        //HYDRA SIGN IN EVENT
        getDefaultInstance(requireContext())!!.pushUserSignin(
            AuthUtil.getInstance().customer.id,
            AuthUtil.getInstance().customer.attributes.firstName,
            AuthUtil.getInstance().customer.attributes.lastName,
            AuthUtil.getInstance().customer.attributes.email,
            AuthUtil.getInstance().customer.attributes.mobile,
            emptyMap())

        verifyCodeDialogFragment?.let {
            it.dismiss()
        }
        CacheUtil.getInstance().signinUsername = ""
        dismissDialogs()
        navigateUp()
    }

    override fun logEventAttributesMetric(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(
                eventName,
                eventAttributes,
                eventMetricName,
                eventMetricValue
            )
        }
    }

    override fun animateSpringView(technique: Techniques, duration: Long, view: View, offset: Int) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.animateSpringView(technique, duration, view, offset)
        }
    }

    override fun changeLanguage(language: String) {
        showLoadingDialog()
        val eventAttributes = HashMap<String, String>()
        LocaleUtil.getInstance().language = language
        eventAttributes["ChangeLanguage"] = language.uppercase(Locale.getDefault())
        logEvent("ChangeLanguage", eventAttributes, "", 0.0)
        LocaleUtil.getInstance().setLocale(context)
        val config = Configuration()
        config.locale = Locale(language)
        resources.updateConfiguration(config, resources.displayMetrics)

        SoloAuth.getInstance()
            .setLanguage(LocaleUtil.getInstance().languageSymbol)
        authPresenter!!.changeLanguage()
        activity?.let {
            val intent = Intent(it, AuthActivity::class.java)
            startActivity(intent)
            it.finish()
        }
    }

    override fun logSigninEvent(customerId: String?) {
        if (isCanProceedFragmentTransaction(fragmentManager = parentFragmentManager)) {
           FirebaseECommerceEventUtil.signInEvent(requireContext(), customerId)
        }
    }

    private fun isCanProceedFragmentTransaction(fragmentManager: FragmentManager): Boolean {
        return !fragmentManager.isStateSaved
    }

    override fun logRegistrationEvent(registrationMethod: String?) {
        FacebookAppEventUtil.completeRegistrationEvent(requireContext(), registrationMethod!!)
    }

    override fun dismissDialogs() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
    }

    override fun showLoadingDialog() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
    }

    override fun onSuccess(loginResult: LoginResult) {
        val accessToken = loginResult.accessToken
        val token = accessToken.token
        val request =
            GraphRequest.newMeRequest(
                loginResult.accessToken
            ) { obj, response ->
                obj?.let {
                    if (it.has("email")) {
                        try {
                            showLoadingDialog()
                            authPresenter!!.signInFacebook(it.getString("email"), token)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            showMessage(
                                CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR)
                            )
                        }
                    } else {
                        showMessage(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR))
                    }
                }

            }
        val bundle = Bundle()
        bundle.putString("fields", "email")
        request.parameters = bundle
        request.executeAsync()
    }

    override fun onCancel() {
        dismissDialogs()
        showMessage(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR))
    }

    override fun onError(error: FacebookException) {
        dismissDialogs()
        showMessage(error.message!!)
    }

    override fun signIn(username: String, password: String, mobileFormatIndex: Int) {
        showLoadingDialog()
        authPresenter!!.signIn(username, password, mobileFormatIndex)
    }
}