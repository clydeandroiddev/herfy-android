package com.herfy.payment.views

import android.content.Intent
import android.os.Bundle
import com.google.gson.reflect.TypeToken
import com.moyasar.android.sdk.PaymentConfig
import com.moyasar.android.sdk.PaymentResult
import com.moyasar.android.sdk.PaymentSheet
import com.moyasar.android.sdk.PaymentSheetResultCallback
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.base.BaseActivity
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.util.GsonHelper
import com.herfy.utils.helper.*

class MoyasarActivity : BaseActivity(), PaymentSheetResultCallback {

    private lateinit var paymentSheet : PaymentSheet
    private var orderId = ""
    private var amount = 0
    private var menuItems = ArrayList<MenuItem>()

    private var moyasarApiKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moyasar)
        intent?.let {
            it.getStringExtra(EXTRA_ORDER_ID)?.let { id ->
                orderId = id
            }
            amount = (it.getDoubleExtra(EXTRA_AMOUNT, 0.0) * 100).toInt()
            menuItems = GsonHelper.gsonFinal().fromJson(it.getStringExtra(EXTRA_MENU_ITEMS), object : TypeToken<List<MenuItem>>() {}.type)

            it.getStringExtra(EXTRA_API_KEY)?.let { apiKey ->
                moyasarApiKey = apiKey
            }
        }

        val config = PaymentConfig(
            amount = amount,
            currency = CacheUtil.getInstance().concept.currencyCode,
            description = "Order Id $orderId",
            apiKey = moyasarApiKey,
            baseUrl = BuildConfig.MOYASAR_BASE_URL,
            metadata = mapOf(
                "reference" to orderId,
                "items" to menuItems.joinToString("\n") { x -> x.getName(LocaleUtil.getInstance().language) }
            ),
            manual = false,
            createSaveOnlyToken = false
        )
        paymentSheet = PaymentSheet(this, this, config)
        paymentSheet.present()

    }

    override fun onResult(result: PaymentResult) {
        val intent = Intent()
        when(result){
            is PaymentResult.Completed -> {
                intent.putExtra(EXTRA_MOYASAR_ID, result.payment.id)
                intent.putExtra(EXTRA_MOYASAR_STATUS, result.payment.status)
                setResult(RESULT_OK, intent)
                finish()
            }

            is PaymentResult.CompletedToken -> {
                intent.putExtra(EXTRA_MOYASAR_ID, result.token.id)
                intent.putExtra(EXTRA_MOYASAR_STATUS, result.token.status)
                setResult(RESULT_OK, intent)
                finish()
            }

            is PaymentResult.Failed -> {
                intent.putExtra(EXTRA_MOYASAR_ID, STATUS_FAILED)
                intent.putExtra(EXTRA_MOYASAR_STATUS, result.error.message)
                intent.putExtra(EXTRA_ORDER_ID, orderId)
                setResult(RESULT_OK, intent)
                finish()
            }

            is PaymentResult.Canceled -> {
                intent.putExtra(EXTRA_MOYASAR_ID, STATUS_CANCELLED)
                intent.putExtra(EXTRA_MOYASAR_STATUS, STATUS_CANCELLED)
                intent.putExtra(EXTRA_ORDER_ID, orderId)
                setResult(RESULT_CANCELED, intent)
                finish()
            }

            else -> {
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_MENU_ITEMS = "menu_items"
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_AMOUNT = "order_amount"
        const val EXTRA_MOYASAR_STATUS = "moyasar_status"
        const val EXTRA_MOYASAR_ID ="moyasar_id"
        const val EXTRA_API_KEY = "api_key"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILED = "failed"
    }
}