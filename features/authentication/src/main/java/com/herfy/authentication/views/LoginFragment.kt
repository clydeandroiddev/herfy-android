package com.herfy.authentication.views

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.herfy.analytics.kochava.KochavaEventType
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.state.LoginViewState
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.loadSliderImage
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.collectViewModelState
import com.skylinedynamics.solosdk.api.models.objects.Customer
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class LoginFragment : BaseFragment<LoginViewModel, FragmentLoginBinding>() {

    private lateinit var addNumberDialog: AddNumberDialog

    override val viewModel: LoginViewModel by viewModels()

    override val layout: Int = R.layout.fragment_login

    private var keyboardShown = false

    private var googleName = ""

    private var addIsShown = false

    private var isMaxTen = false

    private var googleSignInClient: GoogleSignInClient? = null

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
                    googleName = account.displayName.orEmpty()
                    viewModel.loginViaGoogle(account.email.orEmpty(), account.idToken.orEmpty())
                } else {
                    showAlertDialog(
                        title = "",
                        message = CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                    )
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                showAlertDialog(
                    title = "", message = CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        googleLauncher = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            val googleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildUtil.googleInClient(it)).requestEmail().build()
            googleSignInClient = GoogleSignIn.getClient(it, googleSignInOptions)
        }
    }

    override fun initViews() {
        super.initViews()
        viewModel.getBannerSlider()
        binding.apply {

            binding.root.viewTreeObserver.addOnGlobalLayoutListener {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - r.bottom
                val isShown = keypadHeight > screenHeight * 0.15
                if (isShown != keyboardShown && !addIsShown) {
                    keyboardShown = isShown
                    adjustViewHeight(isShown)
                }
            }

            loginButton.setOnClickListener {
                hideKeyboard(binding.root)
                var phoneNumber = phoneNumberEdit.text.toString()
                if (isMaxTen && phoneNumber.startsWith("0")) {
                    phoneNumber = phoneNumber.removePrefix(phoneNumber.substring(0, 1))
                }
                viewModel.login(
                    mobileNumberDialCode.text.toString(), phoneNumber
                )
            }

            closeButton.setOnClickListener {
                hideKeyboard(binding.root)
                requireActivity().finish()
            }

            loginGoogle.setOnClickListener {
                hideKeyboard(binding.root)
                googleSignInClient!!.signOut()
                showProgressDialog(
                    false, CacheUtil.getInstance().getTranslations(Text.LOADING, "Loading")
                )
                val signInIntent = googleSignInClient!!.signInIntent
                googleLauncher!!.launch(signInIntent)
            }

            loginGuest.setOnClickListener {
                hideKeyboard(binding.root)
                requireActivity().finish()
            }

            phoneNumberEdit.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick()
                    true
                } else false
            }

            phoneNumberEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s != null) {
                        if (s.toString().startsWith("0")) {
                            setPhoneNumberLength(10)
                            isMaxTen = true
                        } else {
                            isMaxTen = false
                            setPhoneNumberLength(9)
                        }
                    }
                }
            })

            arguments?.let {
                if (it.getBoolean("fromCheckout", false)) {
                    loginGuest.isVisible = false
                }
            }
        }
    }

    fun setPhoneNumberLength(length: Int) {
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(length)
        binding.phoneNumberEdit.filters = fArray
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            mobileNumberDialCode.text = "+966"

            titleLabel.text = CacheUtil.getInstance().getTranslations(
                Text.JOIN_NOW, "Join now and start ordering, earning points and much more"
            )
            mobileNumberLabel.text = CacheUtil.getInstance().getTranslations(
                Text.MOBILE_NUMBER, "Mobile Number"
            )
            loginButton.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_LABEL, "Continue"
            )
            orLabel.text = CacheUtil.getInstance().getTranslations(
                Text.OR_NEW, "Or"
            )
            loginGoogle.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_WITH_GOOGLE, "Continue with Google"
            )
            loginGuest.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_AS_GUEST, "Continue as Guest"
            )
        }
    }

    private fun proceed(customer: Customer) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val status =
                customer.attributes.status.lowercase(Locale.getDefault()).trim { it <= ' ' }
            findNavController().navigate(
                LoginFragmentDirections.actionLoginToOtp(
                    phoneNumber = customer.attributes.mobile ?: "",
                    customerId = customer.id,
                    isVerified = status == Values.STATUS_VERIFIED,
                    isGuest = false,
                    givenName = googleName
                )
            )
        }, 100)
    }

    private fun adjustViewHeight(keyboardVisible: Boolean) {
        if (keyboardVisible) {
            binding.spacing.layoutParams.height =
                resources.getDimensionPixelSize(R.dimen.view_keyboard_shown)
        } else {
            binding.spacing.layoutParams.height =
                resources.getDimensionPixelSize(R.dimen.view_keyboard_regular)
        }
        binding.spacing.requestLayout()
    }

    override fun observe() {
        super.observe()

        viewModel.bannerList.observeNonNull(this) { slides ->
            slides?.firstOrNull()?.let { slide ->
                slide.imageUri?.let {
                    if (it.isNotEmpty()) {
                        loadSliderImage(binding.signupPic, it)
                    }
                }
            }
        }

        viewModel.state.collectViewModelState(viewLifecycleOwner) {
            when (it) {
                is LoginViewState.UserLogin -> {
                    proceed(it.customer)
                }

                is LoginViewState.GoogleGoOTP -> {
                    proceed(it.customer)
                }

                is LoginViewState.GoogleSuccess -> {
                    KochavaTrackerUtil.trackEventWithData(
                        KochavaEventType.LOGIN.eventName,
                        KochavaEventUtil.customer(it.customer.id)
                    )
                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()
                }

                is LoginViewState.ShowLoading -> {
                    if (it.loading) {
                        showProgressDialog(
                            false, CacheUtil.getInstance().getTranslations(Text.LOADING, "Loading")
                        )
                    } else {
                        dismissProgressDialog()
                    }
                }

                is LoginViewState.ShowError -> {
                    showAlertDialog(
                        title = "", message = it.error
                    )
                }

                is LoginViewState.GuestSuccess -> {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginToOtp(
                            phoneNumber = it.phoneNumber,
                            customerId = it.customerId,
                            isVerified = false,
                            isGuest = true,
                            givenName = googleName
                        )
                    )
                }

                is LoginViewState.GoogleSuccessFromAddNumber -> {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginToOtp(
                            phoneNumber = it.mobile,
                            customerId = it.customer.id,
                            isVerified = false,
                            isGuest = false,
                            givenName = googleName
                        )
                    )
                }

                is LoginViewState.ShowAddNumber -> {
                    showAddNumber(false, it.customer)
                }
            }
        }
    }

    private fun showAddNumber(fromGuest: Boolean, customer: Customer?) {
        dismissAddNumberDialog()
        addNumberDialog = AddNumberDialog.newInstance(object : AddNumberDialog.OnSubmit {
            override fun onSubmit(phoneNumber: String) {
                dismissAddNumberDialog()
                if (fromGuest) {
                    viewModel.guestLogin(
                        phoneNumber
                    )
                } else {
                    customer?.let {
                        viewModel.updateMobile(phoneNumber, it)
                    }
                }
            }

            override fun exit() {
                dismissAddNumberDialog()
            }

            override fun onError(error: String) {
                showAlertDialog(
                    title = "", message = error
                )
            }

        })
        addIsShown = true
        addNumberDialog.show(
            requireActivity().supportFragmentManager, AddNumberDialog::class.java.simpleName
        )
    }

    private fun dismissAddNumberDialog() {
        try {
            addIsShown = false
            addNumberDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}