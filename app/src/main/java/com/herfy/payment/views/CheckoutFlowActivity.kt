package com.herfy.payment.views

import android.os.Bundle
import android.widget.FrameLayout
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.localisation.Locale
import com.checkout.components.interfaces.model.ComponentName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.interfaces.uicustomisation.BorderRadius
import com.checkout.components.interfaces.uicustomisation.designtoken.ColorTokens
import com.checkout.components.interfaces.uicustomisation.designtoken.DesignTokens
import com.checkout.components.interfaces.uicustomisation.font.Font
import com.checkout.components.interfaces.uicustomisation.font.FontFamily
import com.checkout.components.interfaces.uicustomisation.font.FontName
import com.checkout.components.interfaces.uicustomisation.font.FontStyle
import com.checkout.components.interfaces.uicustomisation.font.FontWeight
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import com.herfy.utils.helper.LocaleUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutFlowActivity : BaseActivity() {

    private var checkoutComponents: CheckoutComponents? = null
    private var flowViewContainer: FrameLayout? = null

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var paymentSessionId: String
    private lateinit var paymentSessionToken: String
    private lateinit var paymentSessionSecret: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_flow_checkout) // Reuse the dialog layout
        flowViewContainer = findViewById(R.id.flow_container)

        // Retrieve session info from Intent extras
        paymentSessionId = intent.getStringExtra(EXTRA_PAYMENT_SESSION_ID).toString()
        paymentSessionToken = intent.getStringExtra(EXTRA_PAYMENT_SESSION_TOKEN).toString()
        paymentSessionSecret = intent.getStringExtra(EXTRA_PAYMENT_SESSION_SECRET).toString()

        uiScope.launch {
            try {
                initializeCheckout()
            } catch (t: Throwable) {
                showError()
            }
        }
    }

    private suspend fun initializeCheckout() = withContext(Dispatchers.IO) {
        val integration = CacheUtil.getInstance().integration?.value ?: ""
        val environment =
            if (EnvUtil.isProduction()) Environment.PRODUCTION else Environment.SANDBOX
        val hexColor = CacheUtil.getInstance().colorMain
        val cleanHex = hexColor.replace("#", "")
        val colorInt = ("FF$cleanHex").toLong(16)

        val customFontFamily = FontFamily.Custom(
            normalFont = R.font.cairo_regular,
            boldFont = R.font.cairo_bold,
            semiBold = R.font.cairo_semibold,
        )

        val designTokens = DesignTokens(
            colorTokens = ColorTokens(colorAction = colorInt),
            borderFormRadius = BorderRadius(all = 16),
            fonts = mapOf(
                FontName.Subheading to Font(
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = customFontFamily,
                ),
                FontName.Input to Font(
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.Normal,
                    fontFamily = customFontFamily,
                ),
                FontName.Button to Font(
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = customFontFamily,
                ),
                FontName.Label to Font(
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.Normal,
                    fontFamily = customFontFamily,
                ),
            ),
            borderButtonRadius = BorderRadius(all = 16)
        )

        val configuration = CheckoutComponentConfiguration(
            context = this@CheckoutFlowActivity,
            publicKey = integration,
            environment = environment,
            locale = if (LocaleUtil.getInstance().isArabic) Locale.Ar else Locale.En,
            paymentSession = PaymentSessionResponse(
                id = paymentSessionId,
                paymentSessionToken = paymentSessionToken,
                paymentSessionSecret = paymentSessionSecret
            ),
            appearance = designTokens,
            componentCallback = ComponentCallback(
                onSuccess = { _, _ ->
                    showSuccess()
                },
                onError = { _, error ->
                    // Any error coming from Google Play will not be handled
                    if (error.details.type?.value?.lowercase()?.contains("google") != true) {
                        if (error.code.toString().lowercase() != "component_invalid") {
                            showError()
                        }
                    }
                }
            )
        )

        try {
            checkoutComponents = CheckoutComponentsFactory(config = configuration).create()
        } catch (err: CheckoutError) {
            throw err
        }

        withContext(Dispatchers.Main) {
            renderFlow()
        }
    }

    private fun renderFlow() {
        val container = flowViewContainer ?: return
        container.removeAllViews()

        try {
            val flowComponent = checkoutComponents?.create(ComponentName.Flow) ?: run {
                showError()
                return
            }

            val providedView = flowComponent.provideView(container)
            container.addView(providedView)

        } catch (ex: Exception) {
            showError()
        }
    }

    private fun showError() {
        intent.putExtra(STATUS_SUCCESS, false)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showSuccess() {
        intent.putExtra(STATUS_SUCCESS, true)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val EXTRA_PAYMENT_SESSION_ID = "payment_session_id"
        const val EXTRA_PAYMENT_SESSION_TOKEN = "payment_session_token"
        const val EXTRA_PAYMENT_SESSION_SECRET = "payment_session_secret"
        const val STATUS_SUCCESS = "success"
    }
}