package com.herfy.authentication.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.state.LoginViewState
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
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

        binding.apply {
            loginButton.setOnClickListener {
                hideKeyboard(binding.root)
                viewModel.login(
                    mobileNumberDialCode.text.toString(), binding.phoneNumberEdit.text.toString()
                )
            }

            closeButton.setOnClickListener {
                requireActivity().finish()
            }

            loginGoogle.setOnClickListener {
                googleSignInClient!!.signOut()
                showProgressDialog(
                    false, CacheUtil.getInstance().getTranslations(Text.LOADING, "Loading")
                )
                val signInIntent = googleSignInClient!!.signInIntent
                googleLauncher!!.launch(signInIntent)
            }

            loginGuest.setOnClickListener {
                requireActivity().finish()
                //showAddNumber(true, null)
            }

            phoneNumberEdit.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick()
                    true
                } else false
            }
        }
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
        val status = customer.attributes.status.lowercase(Locale.getDefault()).trim { it <= ' ' }
        findNavController().navigate(
            LoginFragmentDirections.actionLoginToOtp(
                phoneNumber = customer.attributes.mobile ?: "",
                customerId = customer.id,
                isVerified = status == Values.STATUS_VERIFIED,
                isGuest = false
            )
        )
    }

    override fun observe() {
        super.observe()

        viewModel.state.collectViewModelState(viewLifecycleOwner) {
            when (it) {
                is LoginViewState.UserLogin -> {
                    proceed(it.customer)
                }

                is LoginViewState.GoogleGoOTP -> {
                    proceed(it.customer)
                }

                is LoginViewState.GoogleSuccess -> {
                    requireContext().applicationContext.sendBroadcast(
                        Intent(
                            requireContext().getString(R.string.exit_to_home)
                        )
                    )
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
                        title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"),
                        message = it.error
                    )
                }

                is LoginViewState.GuestSuccess -> {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginToOtp(
                            phoneNumber = it.phoneNumber,
                            customerId = it.customerId,
                            isVerified = false,
                            isGuest = true
                        )
                    )
                }

                is LoginViewState.GoogleSuccessFromAddNumber -> {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginToOtp(
                            phoneNumber = it.mobile,
                            customerId = it.customer.id,
                            isVerified = false,
                            isGuest = false
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
        addNumberDialog.show(
            requireActivity().supportFragmentManager, AddNumberDialog::class.java.simpleName
        )
    }

    private fun dismissAddNumberDialog() {
        try {
            addNumberDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}