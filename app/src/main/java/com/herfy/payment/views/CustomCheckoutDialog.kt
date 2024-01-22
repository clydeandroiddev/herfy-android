package com.herfy.payment.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.checkout.base.model.CardScheme
import com.checkout.base.model.Environment
import com.checkout.frames.api.PaymentFlowHandler
import com.checkout.frames.api.PaymentFormMediator
import com.checkout.frames.model.Margin
import com.checkout.frames.model.Shape
import com.checkout.frames.model.font.FontWeight
import com.checkout.frames.screen.paymentform.model.PaymentFormConfig
import com.checkout.frames.style.component.PayButtonComponentStyle
import com.checkout.frames.style.component.base.ContainerStyle
import com.checkout.frames.style.component.default.DefaultButtonStyle
import com.checkout.frames.style.component.default.DefaultLightStyle
import com.checkout.frames.style.screen.PaymentDetailsStyle
import com.checkout.frames.style.screen.PaymentFormStyle
import com.checkout.frames.utils.constants.PaymentButtonConstants
import com.checkout.threedsecure.model.ThreeDSRequest
import com.checkout.threedsecure.model.ThreeDSResult
import com.checkout.tokenization.model.TokenDetails
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseDialogFragment
import com.herfy.constants.Text
import com.herfy.util.checkout.PaymentFormConstants.backIconSize
import com.herfy.util.checkout.PaymentFormConstants.inputFieldColor
import com.herfy.util.checkout.PaymentFormConstants.inputFieldCornerRadius
import com.herfy.util.checkout.PaymentFormConstants.marginBottom
import com.herfy.util.AppLogger
import com.herfy.util.CacheUtil
import com.herfy.util.EnvUtil
import com.herfy.util.ViewUtil
import org.json.JSONObject


class CustomCheckoutDialog(private val context: Context) : BaseDialogFragment() {
    private var mPaymentForm: PaymentFormMediator? = null
    private var listener: OnActionSelectedListener? = null
    private val activity: Activity? = null
    private var madaOnly = false
    private lateinit var view : View
    private val mPaymentFlowHandler: PaymentFlowHandler = object : PaymentFlowHandler {
        override fun onSubmit() {
            listener!!.onFormSubmit()
        }

        override fun onSuccess(tokenDetails: TokenDetails) {
            AppLogger.info("onTokenGenerated", Gson().toJson(tokenDetails))
            var cardSupported = true
            if (madaOnly) {
                val madaBins: Set<String> = HashSet(listOf(*MADA_BINS))
                cardSupported = madaBins.contains(tokenDetails.bin)
            }
            if (!cardSupported) {
                (context as BaseActivity).showAlertDialog(
                    "",
                    CacheUtil.getInstance().getTranslations(
                        Text.SORRY_ERROR_PAYMENT_MADA,
                        "Sorry we can't process your payment request at the moment. Please try a mada card."
                    )
                )
            } else {
                //tokenizationResponse = response;
                listener!!.onTokenGenerated(tokenDetails)
            }
        }

        override fun onFailure(errorMessage: String) {
            AppLogger.info("onError", errorMessage)
            listener!!.onError(errorMessage)
        }

        override fun onBackPressed() {
            listener!!.onBackPressed()
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.apply {
                setLayout(width, height)
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                val typedValue = TypedValue()
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = typedValue.data
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = mPaymentForm!!.provideFragmentContent(this)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val integration = CacheUtil.getInstance().integration
        var integrationValue: String? = ""
        if (integration != null) {
            integrationValue = integration.value
        }

        val supportedCardTypes: MutableList<CardScheme> = ArrayList()
        if (CacheUtil.getInstance().supportCardTypes.isEmpty()) {
            madaOnly = false
            supportedCardTypes.add(CardScheme.VISA)
            supportedCardTypes.add(CardScheme.MASTERCARD)
            supportedCardTypes.add(CardScheme.MADA)
        } else {
            var hasMada = false
            for (supportedCardType in CacheUtil.getInstance().supportCardTypes) {
                if (supportedCardType.equals("visa", ignoreCase = true)) {
                    supportedCardTypes.add(CardScheme.VISA)
                } else if (supportedCardType.equals("mastercard", ignoreCase = true)) {
                    supportedCardTypes.add(CardScheme.MASTERCARD)
                } else if (supportedCardType.equals("mada", ignoreCase = true)) {
                    hasMada = true
                    supportedCardTypes.add(CardScheme.MADA)
                }
            }
            madaOnly = hasMada && supportedCardTypes.size == 1
        }



        context?.let { ctx ->

            val paymentFormStyle = PaymentFormStyle().copy(
                paymentDetailsStyle = PaymentDetailsStyle(
                    paymentDetailsHeaderStyle =provideHeaderStyle(ctx),
                    addressSummaryStyle = null,
                    payButtonStyle = providePayButtonStyle(ctx)
                )
            )


            val paymentFormConfig = PaymentFormConfig(
                publicKey = integrationValue!!,
                context = context,
                environment = if (EnvUtil.isProduction()) Environment.PRODUCTION else Environment.SANDBOX,
                style = paymentFormStyle,
                mPaymentFlowHandler,
                supportedCardTypes
            )
            mPaymentForm = PaymentFormMediator(paymentFormConfig)

        }

    }


    private val threeDSResultHandler: kotlin.Function1<ThreeDSResult, Unit> =
        { threeDSResult: ThreeDSResult ->
            if (threeDSResult is ThreeDSResult.Success) {
                val token: String = threeDSResult.token
                listener!!.on3DSSuccess()
            } else if (threeDSResult is ThreeDSResult.Error) {
                val errorMessage: String = threeDSResult.error.message ?: ""
                listener!!.on3DSFailure(errorMessage)
            } else {
                listener!!.on3DSFailure("Authentication Failure")
            }
        }

    fun handle3ds(url: JSONObject) {
        AppLogger.info("handle3ds went here?", url.toString())
        try {
            this.dialog?.window?.let {
                val threeDSRequest = ThreeDSRequest(
                    container = it.findViewById(android.R.id.content),  // Provide a ViewGroup container for 3DS WebView
                    url.getString("redirection"),  // Provide a 3D Secure URL
                    url.getString("success-url"),
                    url.getString("failure-url"),
                    threeDSResultHandler
                )
                mPaymentForm!!.handleThreeDS(threeDSRequest)
            }

        } catch (var3: Exception) {
            var3.printStackTrace()
        }
    }

    fun setOnActionSelectedListener(listener: OnActionSelectedListener?) {
        this.listener = listener
    }

    interface OnActionSelectedListener {
        fun onFormSubmit()
        fun onTokenGenerated(response: TokenDetails?)
        fun onError(fail: String?)
        fun on3DSSuccess()
        fun on3DSFailure(error: String?)
        fun onBackPressed()
    }

    companion object {
        val MADA_BINS = arrayOf(
            "446404",
            "440795",
            "440647",
            "421141",
            "474491",
            "588845",
            "968208",
            "457997",
            "457865",
            "468540",
            "468541",
            "468542",
            "468543",
            "417633",
            "446393",
            "636120",
            "968201",
            "410621",
            "409201",
            "403024",
            "458456",
            "462220",
            "968205",
            "455708",
            "484783",
            "588848",
            "455036",
            "968203",
            "486094",
            "486095",
            "486096",
            "504300",
            "440533",
            "489318",
            "489319",
            "445564",
            "968211",
            "410685",
            "406996",
            "432328",
            "428671",
            "428672",
            "428673",
            "968206",
            "446672",
            "543357",
            "434107",
            "407197",
            "407395",
            "42689700",
            "412565",
            "431361",
            "604906",
            "521076",
            "v588850",
            "968202",
            "529415",
            "535825",
            "543085",
            "524130",
            "554180",
            "549760",
            "968209",
            "524514",
            "529741",
            "537767",
            "535989",
            "536023",
            "513213",
            "520058",
            "558563",
            "605141",
            "968204",
            "422817",
            "422818",
            "422819",
            "410834",
            "428331",
            "442463",
            "483010",
            "483011",
            "483012",
            "589206",
            "968207",
            "406136",
            "419593",
            "439954",
            "407520",
            "530060",
            "531196",
            "420132",
            "242030",
            "22402030"
        )
    }


    private fun provideHeaderStyle(ctx : Context) = DefaultLightStyle.screenHeader(
        textId = R.string.cko_payment_details_title,
        imageId = R.drawable.ic_back_arrow,
        leadingIconSize = backIconSize,
        textColor = inputFieldColor,
    ).copy(
        containerStyle = ContainerStyle(color = Color.parseColor(String.format(
            "#FF%06X", 0xFFFFFF and ViewUtil.getColorMain(ctx))).toLong())
    )

    private fun providePayButtonStyle(ctx : Context): PayButtonComponentStyle {
        val buttonStyle = DefaultButtonStyle.lightSolid(
            textId = R.string.cko_pay,
            contentColor = inputFieldColor,
            containerColor = Color.parseColor(String.format(
                "#FF%06X", 0xFFFFFF and ViewUtil.getColorAccent(ctx))).toLong(),
            disabledContentColor = PaymentButtonConstants.disabledContentColor,
            disabledContainerColor = PaymentButtonConstants.disabledContainerColor,
            contentPadding = PaymentButtonConstants.contentPadding,
            fontWeight = FontWeight.Bold,
            margin = Margin(top = marginBottom),
            shape = Shape.RoundCorner,
            cornerRadius = inputFieldCornerRadius
        )
        return PayButtonComponentStyle(buttonStyle = buttonStyle)
    }
}