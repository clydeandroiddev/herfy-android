package com.herfy.auth.views

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup.OnPositionChangedListener
import com.daimajia.androidanimations.library.Techniques
import com.herfy.R
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.auth.AuthContract
import com.herfy.auth.AuthPresenter
import com.herfy.base.*
import com.herfy.data.constants.AUTH_ENTER_MOBILE_OR_EMAIL_ADDRESS
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentSignInBinding
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries
import com.skylinedynamics.solosdk.api.models.objects.LoginOption
import com.herfy.util.*
import com.herfy.utils.helper.LocaleUtil

class SignInFragment : BaseFragment(), AuthContract.View {

    private var _listener: Listener? = null
    private val listener get() = _listener!!



    private var  _binding: FragmentSignInBinding? = null
    val binding: FragmentSignInBinding? get() =  _binding
    private var authPresenter: AuthContract.Presenter? = null
    private var firstAnimate = true
    private var forgotPasswordDialogFragment: ForgotPasswordDialogFragment? = null
    private lateinit var loginOption : LoginOption;
    private var isPasswordHidden = true

    private val allowedCountries = ArrayList<AllowedCountries>()
    private var selectedDialCode = ""
    private var mobileFormatIndex = -1
    var firstTime = true
    private var lastTimeClicked: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        authPresenter = AuthPresenter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginOption = CacheUtil.getInstance().concept.loginOption
        authPresenter!!.start()
    }

    override fun onResume() {
        super.onResume()
        if (firstAnimate) {
            firstAnimate = false
            binding?.apply {
                    usernameLabel.isVisible = true
                if (loginOption != LoginOption.EMAIL) {
                    phoneNumberContainer.isVisible = true
                    usernameContainer.isVisible = false
                } else {
                    usernameContainer.isVisible = true
                  phoneNumberContainer.isVisible = false
                }
            }


            with(listener) {
                binding?.apply {
                    animateSpringView(Techniques.SlideInUp, 300, usernameLabel, 8)
                    if (loginOption != LoginOption.EMAIL) animateSpringView(
                        Techniques.SlideInUp,
                        350,
                        phoneNumberContainer,
                        8
                    )
                    else animateSpringView(Techniques.SlideInUp, 350, usernameContainer, 8)

                    animateSpringView(Techniques.SlideInUp, 400, passwordLabel, 8)
                    animateSpringView(Techniques.SlideInUp, 450, passwordContainer, 8)
                    animateSpringView(Techniques.SlideInUp, 500, signIn, 8)
                    animateSpringView(Techniques.SlideInUp, 550, forgotPassword, 8)
                    animateSpringView(Techniques.SlideInUp, 800, language.languageContainer, 8)

                    val socialLoginOption = CacheUtil.getInstance().concept.socialLoginOption
                    if (socialLoginOption.isNotEmpty() && socialLoginOption != "apple") {
                        animateSpringView(Techniques.SlideInUp, 600, or, 8)
                        if (socialLoginOption.contains("solo-guest")) {
                            animateSpringView(Techniques.SlideInUp, 650, guest, 8)
                        } else guest.visibility = View.GONE
                        if (socialLoginOption.contains("google")) {
                            animateSpringView(Techniques.SlideInUp, 700, google, 8)
                        } else binding?.google?.visibility = View.GONE
                    } else {
                        binding?.apply {
                            or.visibility = View.GONE
                            google.visibility = View.GONE
                            guest.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(!AuthUtil.getInstance().isSignedIn) {
            if (binding?.usernameContainer?.visibility == View.VISIBLE) {
                CacheUtil.getInstance().signinUsername = binding?.username?.text.toString()
            } else {
                CacheUtil.getInstance().signinUsername = binding?.phoneNumber?.text.toString()
            }
        }
    }

    override fun setPresenter(presenter: AuthContract.Presenter) {
        authPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: java.util.HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        listener.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
    }

    override fun setCartExpiry() {}
    override fun setupViews() {

        val socialLoginOption = CacheUtil.getInstance().concept.socialLoginOption
        binding?.apply {
            if (socialLoginOption.isEmpty()) {
                or.visibility = View.GONE
                google.visibility = View.GONE
                guest.visibility = View.GONE
            } else {
                if (socialLoginOption == "apple") {
                   or.visibility = View.GONE
                } else or.visibility = View.VISIBLE
                if (socialLoginOption.contains("solo-guest")) {
                    guest.visibility = View.VISIBLE
                }
                if (socialLoginOption.contains("google")) {
                    google.visibility = View.VISIBLE
                }
            }

            if (loginOption != LoginOption.EMAIL) {
                phoneNumberContainer.visibility = View.VISIBLE
                usernameContainer.visibility = View.GONE
                phoneNumber.setText(CacheUtil.getInstance().signinUsername)
            } else {
                usernameContainer.visibility = View.VISIBLE
                phoneNumberContainer.visibility = View.GONE
                username.setText(CacheUtil.getInstance().signinUsername)
            }

            if (CacheUtil.getInstance().concept.allowedCountries.size == 1) {
                phoneNumberPrefix.background = null
                phoneNumberPrefix.isEnabled = false
            }

            allowedCountries.addAll(CacheUtil.getInstance().concept.allowedCountries)

            if (selectedDialCode.isEmpty()) selectedDialCode = allowedCountries[0].dialCode
            if (mobileFormatIndex == -1 && allowedCountries.size > 0) mobileFormatIndex = 0

            phoneNumberPrefix.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position != null) {
                            selectedDialCode = allowedCountries[position].dialCode
                            mobileFormatIndex = position
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            val adapter = AllowedCountriesAdapter(requireContext(), allowedCountries)
            phoneNumberPrefix.adapter = adapter

            phoneNumber.filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(
                    13 - selectedDialCode.replace(
                        "+",
                        ""
                    ).length
                )
            )
            val phoneNumberTextWatcher: TextWatcher = object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun afterTextChanged(editable: Editable) {
                    val phoneNumberEditText = binding?.phoneNumber

                    phoneNumberEditText?.removeTextChangedListener(this)
                    phoneNumberEditText?.setText(StringUtil.localize(phoneNumberEditText.text.toString()))
                    phoneNumberEditText?.setSelection(editable.length)
                    phoneNumberEditText?.addTextChangedListener(this)
                }
            }

            val passwordTextWatcher: TextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    linearLayout2.visibility = if (charSequence.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                   ivHidePassword.visibility = if (charSequence.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    val passwordInput = binding?.password
                    passwordInput?.removeTextChangedListener(this)
//                binding.password.setText(StringUtil.localize(binding.password.text.toString()))
                    passwordInput?.setText(passwordInput.text.toString())
                    passwordInput?.setSelection(editable.length)
                    passwordInput?.addTextChangedListener(this)
                }
            }
            phoneNumber.addTextChangedListener(phoneNumberTextWatcher)
            password.addTextChangedListener(passwordTextWatcher)
            password.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                activity?.let {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        binding?.signIn?.performClick()
                        val inputMethodManager =
                            it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(binding?.password?.windowToken, 0)
                        return@OnEditorActionListener true
                    }
                }
                false
            })

            binding?.ivHidePassword?.setOnClickListener {
                if (isPasswordHidden) {
                    isPasswordHidden = false
                    // show password
                    password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivHidePassword.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_eye
                        )
                    )
                } else {
                    isPasswordHidden = true
                    // hide password
                    ivHidePassword.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_eye_hidden
                        )
                    )
                    password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }

            binding?.signIn?.setOnClickListener {
                if (loginOption == LoginOption.MOBILE_NUMBER) {
                    listener.signIn(
                        StringUtil.toEnglish(
                            StringUtil.localize(
                                if (selectedDialCode.contains(
                                        "+"
                                    )
                                ) selectedDialCode else "+$selectedDialCode"
                            ) + phoneNumber.text.toString()
                        ), password.text.toString().trim { it <= ' ' }, mobileFormatIndex
                    )
                } else {
                    listener.signIn(
                        username.text.toString().trim { it <= ' ' },
                        password.text.toString().trim { it <= ' ' },
                        -1
                    )
                }
            }
            forgotPassword.setOnClickListener {
                forgotPasswordDialogFragment =
                    ForgotPasswordDialogFragment.newInstance(authPresenter)
                forgotPasswordDialogFragment?.show(
                    childFragmentManager,
                    ForgotPasswordDialogFragment::class.java.simpleName
                )
            }
            guest.setOnClickListener {
                firstAnimate = true
                listener.showGuest()
            }
//        binding.social.facebook.setOnClickListener { listener.signInFacebook() }
            google.setOnClickListener { listener.signInGoogle() }
            val languages = CacheUtil.getInstance().concept.languages.toList()
            binding?.language?.apply {
                french.isVisible =
                    !(languages.find { lang -> lang.equals(LocaleUtil.FR, true) }.isNullOrEmpty())
                english.isVisible = !(languages.find { lang -> lang.equals(LocaleUtil.EN_US, true) }
                    .isNullOrEmpty())
                arabic.isVisible = !(languages.find { lang -> lang.equals(LocaleUtil.AR_SA, true) }
                    .isNullOrEmpty())
            }

            binding?.language?.languageSegmented?.apply {
                val position = when {
                    LocaleUtil.getInstance().isArabic -> 0
                    LocaleUtil.getInstance().isFrench -> 1
                    else -> 2
                }
                setPosition(position, true)
                onPositionChangedListener = OnPositionChangedListener { position ->
                    if (firstTime) {
                        firstTime = false
                        return@OnPositionChangedListener
                    }

                    if (SystemClock.elapsedRealtime() - lastTimeClicked < 1000) {
                        return@OnPositionChangedListener
                    }
                    lastTimeClicked = SystemClock.elapsedRealtime()

                    when (getButton(position)) {


                        language.english -> if (!LocaleUtil.getInstance().isEnglish) listener.changeLanguage(
                            "en"
                        )

                        language.arabic -> if (!LocaleUtil.getInstance().isArabic) listener.changeLanguage(
                            "ar"
                        )

                        language.french -> if (!LocaleUtil.getInstance().isFrench) listener.changeLanguage(
                            "fr"
                        )

                        /* 0 -> if (LocaleUtil.getInstance().isEnglish) {
                                 listener.changeLanguage("ar")
                             }
                             1 -> if (!LocaleUtil.getInstance().isEnglish) {
                                 listener.changeLanguage("en")
                             }*/
                    }
                }
            }


             username.requestFocus()
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding?.apply {
            if (loginOption != LoginOption.EMAIL) {
                usernameLabel.text = CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER, "PHONE NUMBER")
            } else {
                usernameLabel.text = CacheUtil.getInstance().getTranslations(Text.USERNAME_CAPS, "USERNAME")
            }

            phoneNumber.hint = CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS, "ENTER PHONE NUMBER").toLowerCase().capitalize()
            username.hint = CacheUtil.getInstance().getTranslations(
                AUTH_ENTER_MOBILE_OR_EMAIL_ADDRESS, "enter_mobile_or_email_address").toLowerCase().capitalize()
            phoneNumberLabel.text = CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER)
            passwordLabel.text = CacheUtil.getInstance().getTranslations(Text.PASSWORD)
            password.hint = CacheUtil.getInstance().getTranslations(Text.PLEASE_TYPE_YOUR_PASSWORD_HERE, "Please type your password here").toLowerCase().capitalize()
            signIn.text = CacheUtil.getInstance().getTranslations(Text.SIGN_IN)
            forgotPassword.text = CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD)
            or.text = CacheUtil.getInstance().getTranslations(Text.OR)
            guest.text = CacheUtil.getInstance().getTranslations(Text.SIGN_IN_AS_GUEST, "SIGN IN AS GUEST")
            tvGoogle.text = CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_GOOGLE, "Sign in with Google")
        }
    }

    override fun signInEmail(email: String?, password: String?, mobileFormatIndex: Int) {}
    override fun forgotPassword(phoneNumber: String?, mobileFormatIndex: Int) {
        listener.showLoadingDialog()
        authPresenter!!.forgotPassword(phoneNumber,mobileFormatIndex)
    }

    override fun signInFacebook() {}
    override fun signInGoogle() {}
    override fun signUp(fullName: String, phoneNumber: String, email: String, password: String, index: Int) {}
    override fun showErrorEmail(error: String) {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _listener = when {
            context is Listener -> context
            parentFragment is Listener -> parentFragment as Listener
            else -> null
        }
    }

    interface Listener: BaseDialogListener, BaseNavigationListener, BaseLogListener, BaseViewManipulationListener {
        fun showError(error: String)
        fun showPhoneNumber(customerId: String, email: String, password: String)
        fun showCode(guest: Boolean, customerId: String, email: String, password: String, phoneNumber: String)
        fun signIn(username: String, password: String, mobileFormatIndex: Int)
        fun showGuest()
        fun signInFacebook()
        fun signInGoogle()
        fun changeLanguage(language: String)
    }

    override fun showErrorUsername(error: String) {
        binding?.apply {
            if (error.isEmpty()) {
                usernameError.visibility = View.GONE
            } else {
                usernameError.text = error
                usernameError.visibility = View.VISIBLE
            }
        }
        _listener?.dismissDialogs()
    }

    override fun showErrorPassword(error: String) {
        binding?.apply {
            if (error.isEmpty()) {
                passwordError.visibility = View.GONE
            } else {
                passwordError.text = error
                passwordError.visibility = View.VISIBLE
            }
            _listener?.dismissDialogs()
        }
    }

    override fun showMessage(message: String) {
        listener.showMessage(message)
    }

    override fun showError(error: String) {
        if (forgotPasswordDialogFragment != null) {
            forgotPasswordDialogFragment!!.showError("")
        }
        _listener?.showError(error)
    }

    override fun showSuccess(success: String) {
        if (forgotPasswordDialogFragment != null) {
            forgotPasswordDialogFragment!!.dismiss()
        }
        _listener?.dismissDialogs()
        _listener?.showAlertDialog("", success)
    }

    override fun showErrorFirstName(error: String) {}
    override fun showErrorPhoneNumber(error: String) {
        binding?.apply {
            if (error.isEmpty()) {
                phoneNumberError.visibility = View.GONE
            } else {
                phoneNumberError.text = error
                phoneNumberError.visibility = View.VISIBLE
            }
        }
    }

    override fun showGuest() {}
    override fun showPhoneNumber(customerId: String, email: String, password: String) {
        _listener?.showPhoneNumber(customerId, email, password)
    }

    override fun setupCodeBroadcastListener() {

    }

    override fun showCode(guest: Boolean, customerId: String, email: String, password: String, phoneNumber: String) {
        _listener?.showCode(guest, customerId, email, password, phoneNumber)
    }

    override fun showMain() {
        _listener?.showMain()
    }

    override fun changeLanguage(language: String) {}
    override fun logSigninEvent(customerId: String?) {
            FirebaseECommerceEventUtil.signInEvent(requireContext(), customerId)
    }

    override fun logRegistrationEvent(registrationMethod: String?) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding  = null // to prevent memory leaks
    }
    override fun onDestroy() {
        _listener = null
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(): SignInFragment {
            return SignInFragment()
        }
    }
}