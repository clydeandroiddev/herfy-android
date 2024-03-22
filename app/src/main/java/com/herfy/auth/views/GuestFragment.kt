package com.herfy.auth.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.daimajia.androidanimations.library.Techniques
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.herfy.BuildConfig
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.auth.AuthContract
import com.herfy.auth.AuthPresenter
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.cart.views.CartFragment
import com.herfy.utils.constants.Text
import com.herfy.databinding.FragmentGuestBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries
import com.herfy.util.*
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.verification.CodeBroadcastReceiver
import com.herfy.verification.CodeBroadcastReceiver.CodeBroadcastReceiverListener
import com.herfy.verification.views.CodeDialogFragment
import com.herfy.verification.views.VerifyCodeDialogFragment
import java.util.regex.Pattern

class GuestFragment : BaseFragment(), AuthContract.View {

    private lateinit var binding: FragmentGuestBinding

    private val args: GuestFragmentArgs by navArgs()

    private lateinit var authPresenter: AuthContract.Presenter

    private var orderHistory = false
    private var cart = false
    private var orderType = false
    private var favoriteHome = false
    private var favoriteMenuItem = false
    private var curbside = false

    private var googleSignInClient: GoogleSignInClient? = null
    private var codeDialogFragment: CodeDialogFragment? = null
    private var verifyCodeDialogFragment: VerifyCodeDialogFragment? = null
    private var codeBroadcastReceiver: CodeBroadcastReceiver? = null
    private val allowedCountries = ArrayList<AllowedCountries>()
    private var selectedDialCode = ""
    private var mobileFormatIndex = -1;

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
                    activity?.let {
                        val baseActivity = it as BaseActivity
                        baseActivity.showLoadingDialog()
                    }
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
        /*val bundle = intent.extras
        if (bundle != null) {
            if (bundle.containsKey("curbside")) {
                curbside = bundle.getBoolean("curbside")
            }
            if (bundle.containsKey("order_history")) {
                orderHistory = bundle.getBoolean("order_history")
            }
            if (bundle.containsKey("cart")) {
                cart = bundle.getBoolean("cart")
            }
            if (bundle.containsKey("order_type")) {
                orderType = bundle.getBoolean("order_type")
            }
            if (bundle.containsKey("favorite_home")) {
                favoriteHome = bundle.getBoolean("favorite_home")
            }
            if (bundle.containsKey("favorite_menu_item")) {
                favoriteMenuItem = bundle.getBoolean("favorite_menu_item")
            }
        }*/
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
        binding = FragmentGuestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPresenter!!.start()
        activity?.let {
            val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildUtil.googleInClient(it))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(it, googleSignInOptions)
        }
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
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.animateSpringView(Techniques.SlideInUp, 300, binding.back, 8)
            baseActivity.animateSpringView(Techniques.SlideInUp, 300, binding.tvTitle, 8)
            baseActivity.animateSpringView(Techniques.SlideInUp, 350, binding.firstNameLabel, 8)
            baseActivity.animateSpringView(
                Techniques.SlideInUp,
                400,
                binding.firstNameContainer,
                8
            )
            baseActivity.animateSpringView(
                Techniques.SlideInUp,
                450,
                binding.phoneNumberLabel,
                8
            )
            baseActivity.animateSpringView(
                Techniques.SlideInUp,
                500,
                binding.phoneNumberContainer,
                8
            )
            baseActivity.animateSpringView(Techniques.SlideInUp, 550, binding.guest, 8)
            val socialLoginOption = CacheUtil.getInstance().concept.socialLoginOption
            if (socialLoginOption.isNotEmpty() && socialLoginOption != "apple") {
                baseActivity.animateSpringView(Techniques.SlideInUp, 600, binding.or, 8)
                if (socialLoginOption.contains("google")) {
                    baseActivity.animateSpringView(Techniques.SlideInUp, 650, binding.google, 8)
                } else binding.google.visibility = View.GONE
            } else {
                binding.or.visibility = View.GONE
                binding.google.visibility = View.GONE
            }
        }

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }

        allowedCountries.addAll(CacheUtil.getInstance().concept.allowedCountries)

        if (selectedDialCode.isEmpty()) selectedDialCode = allowedCountries[0].dialCode
        if (mobileFormatIndex == -1 && allowedCountries.size > 0) mobileFormatIndex = 0

        binding.phoneNumberPrefix.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if(position < allowedCountries.size) {
                    selectedDialCode = allowedCountries[position].dialCode
                    mobileFormatIndex = position
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val adapter = AllowedCountriesAdapter(requireContext(), allowedCountries)
        binding.phoneNumberPrefix.adapter = adapter

        binding.phoneNumber.filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(13 - selectedDialCode.replace("+", "").length))
        val phoneNumberTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                binding.phoneNumber.removeTextChangedListener(this)
                binding.phoneNumber.setText(StringUtil.localize(binding.phoneNumber.text.toString()))
                binding.phoneNumber.setSelection(editable.length)
                binding.phoneNumber.addTextChangedListener(this)
            }
        }
        binding.phoneNumber.addTextChangedListener(phoneNumberTextWatcher)
        binding.phoneNumber.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            activity?.let {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.guest.performClick()
                    val inputMethodManager =
                        it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(
                        binding.phoneNumber.windowToken,
                        0
                    )
                    return@OnEditorActionListener true
                }
            }
            false
        })
        binding.guest.setOnClickListener {
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.showLoadingDialog()
            }
            authPresenter!!.guest(
                binding.firstName!!.text.toString().trim { it <= ' ' },
                StringUtil.toEnglish(
                    selectedDialCode + binding.phoneNumber.text.toString()
                        .trim { it <= ' ' }),
                mobileFormatIndex
            )
        }

        binding.google.setOnClickListener {
            googleSignInClient!!.signOut()
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.showLoadingDialog()
            }
            val signInIntent = googleSignInClient!!.signInIntent
            googleLauncher!!.launch(signInIntent)
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.tvTitle.text = CacheUtil.getInstance()
            .getTranslations(Text.LOG_IN_AS_GUEST_CAPS)
        binding.back.text = CacheUtil.getInstance().getTranslations(Text.BACK)
        binding.firstName.hint = CacheUtil.getInstance().getTranslations(
            Text.ENTER_YOUR_FIRSTNAME_AND_LASTNAME,
            "Enter your first name and last name"
        )
        binding.firstNameLabel.text = CacheUtil.getInstance().getTranslations(Text.FULL_NAME)
        binding.phoneNumberLabel.text =
            CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER)
        binding.guest.text = CacheUtil.getInstance()
            .getTranslations(Text.SIGN_IN_AS_GUEST, "SIGN IN AS GUEST")
        binding.or.text = CacheUtil.getInstance().getTranslations(Text.OR)
        binding.tvGoogle.text = CacheUtil.getInstance()
            .getTranslations(Text.SIGN_IN_WITH_GOOGLE)
        //        facebook.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_FACEBOOK));
    }

    override fun showGuest() {}
    override fun showErrorPassword(error: String) {}
    override fun showErrorUsername(error: String) {}
    override fun showMessage(message: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showError(error: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showAlertDialog("", error)
        }
    }

    override fun showSuccess(success: String) {}
    override fun showErrorFirstName(error: String) {
        if (error.isEmpty()) {
            binding.firstNameError.visibility = View.GONE
        } else {
            binding.firstNameError.text = error
            binding.firstNameError.visibility = View.VISIBLE
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.dismissDialogs()
            }
        }
    }

    override fun showErrorPhoneNumber(error: String) {
        if (error.isEmpty()) {
            binding.phoneNumberError.visibility = View.GONE
        } else {
            binding.phoneNumberError.text = error
            binding.phoneNumberError.visibility = View.VISIBLE
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.dismissDialogs()
            }
        }
    }

    override fun signInEmail(email: String?, password: String?, mobileFormatIndex: Int) {}
    override fun forgotPassword(phoneNumber: String, mobileFormatIndex: Int) {}
    override fun signInFacebook() {}
    override fun signInGoogle() {}
    override fun signUp(fullName: String, phoneNumber: String, email: String, password: String, index: Int) {}
    override fun showErrorEmail(error: String) {}
    override fun showPhoneNumber(customerId: String, email: String, password: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        codeDialogFragment?.let {
            it.dismiss()
        }
        codeBroadcastReceiver = CodeBroadcastReceiver()
        codeBroadcastReceiver!!.setCodeBroadcastReceiverListener(object :
            CodeBroadcastReceiverListener {
            override fun onReceived(intent: Intent) {
                if (intent != null) {
                    try {
                        codeLauncher!!.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onTimeout() {
                AppLogger.error("CodeBroadcastReceiver onTimeout")
            }
        })
        activity?.let {
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            it.registerReceiver(codeBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
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
        codeDialogFragment = CodeDialogFragment.newInstance()
        codeDialogFragment?.arguments = args
        codeDialogFragment?.show(childFragmentManager, CodeDialogFragment::class.java.simpleName)
    }

    override fun setupCodeBroadcastListener() {

    }

    override fun showCode(
        guest: Boolean,
        customerId: String,
        email: String,
        password: String,
        phoneNumber: String
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        if (codeDialogFragment == null || codeDialogFragment!!.dialog == null || !codeDialogFragment!!.dialog!!
                .isShowing
        ) {
            codeBroadcastReceiver = CodeBroadcastReceiver()
            codeBroadcastReceiver!!.setCodeBroadcastReceiverListener(object :
                CodeBroadcastReceiverListener {
                override fun onReceived(intent: Intent) {
                    if (intent != null) {
                        try {
                            codeLauncher!!.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onTimeout() {
                    AppLogger.error("CodeBroadcastReceiver onTimeout")
                }
            })
            activity?.let {
                val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                it.registerReceiver(codeBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
                val task = SmsRetriever.getClient(it).startSmsUserConsent(null)
                task.addOnSuccessListener { aVoid: Void? -> }
                task.addOnFailureListener { e: Exception -> AppLogger.error(e.message) }
            }
        } else {
            codeDialogFragment!!.dismiss()
        }
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
        verifyCodeDialogFragment?.setArguments(args)
        verifyCodeDialogFragment?.show(
            childFragmentManager,
            VerifyCodeDialogFragment::class.java.simpleName
        )
        if(authPresenter.registrationMethod.isNotEmpty()) {
            logRegistrationEvent(authPresenter.registrationMethod)
        }
    }

    override fun showMain() {
        codeDialogFragment?.let {
            it.dismiss()
        }
        verifyCodeDialogFragment?.let {
            it.dismiss()
        }
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            Toast.makeText(
                it,
                CacheUtil.getInstance().getTranslations(Text.SIGNED_IN),
                Toast.LENGTH_SHORT
            ).show()
        }
        navigateUp()
        /*var intent = Intent(this, MainActivity::class.java)
        if (curbside) {
            setResult(RESULT_OK, Intent())
            finish()
        } else if (!orderType) {
            intent.putExtra("cart", cart)
            if (orderHistory) {
                intent = Intent(this, OrdersActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            } else if (favoriteHome) {
                intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            } else if (favoriteMenuItem) {
                //intent = new Intent(this, MenuItemActivity.class);
                intent = Intent(this, MainActivity::class.java)
                intent.putExtra("menu", true)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            } else {
                intent.putExtra("home", true)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            intent = Intent(this, OrderTypeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        intent.putExtra("sign_in", true)
        startActivity(intent)
        finish()*/
    }

    private fun navigateUp() {
        activity?.cast<MainActivity> {
            sendFragmentResult()
        }
        activity?.cast<NavigationActivity> {
            sendFragmentResult()
        }
        activity?.cast<AuthActivity> {
            it.finish()
        }
    }

    private fun sendFragmentResult() {
        setFragmentResult(
            AuthFragment.ARGS_KEY_FRAGMENT_RESULT,
            bundleOf(
                CartFragment.ARGS_KEY_CHECKOUT to args.checkout
            )
        )
        findNavController().navigateUp()
    }

    override fun changeLanguage(language: String) {}
    override fun logSigninEvent(customerId: String?) {}
    override fun logRegistrationEvent(registrationMethod: String?) {
        FacebookAppEventUtil.completeRegistrationEvent(requireContext(), registrationMethod!!)
    }

}