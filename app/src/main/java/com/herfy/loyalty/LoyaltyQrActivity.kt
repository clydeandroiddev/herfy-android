package com.herfy.loyalty

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.utils.constants.Text
import com.herfy.databinding.ActivityLoyaltyQrBinding
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.herfy.util.*
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.TimeUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

const val POINTS = "points"
const val VALUE = "value"
const val CODE = "code"
const val EXPIRES_AT = "expires-at"
const val QR_DATA = "qr-data"


class LoyaltyQrActivity : BaseActivity(), LoyaltyContract.View {

    private lateinit var binding: ActivityLoyaltyQrBinding
    private lateinit var loyaltyPresenter: LoyaltyContract.Presenter
    private var countDownTimer: CountDownTimer? = null

    private var points: Int = 0
    private var value: Double = 0.0
    private var code: Int = 0
    private var expiresAt: Long = 0L
    private var qrData: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoyaltyQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadPreferences()
        loyaltyPresenter = LoyaltyPresenter(this)
        loyaltyPresenter.start()
    }

    private fun generateQRCode(qrData: String) {
        dismissDialogs()
        GlobalScope.launch {
            val qrCode = QRHelper(this@LoyaltyQrActivity).generateQR(qrData)
            showQRCode(qrCode)
        }
    }

    private fun loadPreferences() {
        val bundle = intent.extras
        bundle?.let { b ->
            points = b.getInt(POINTS, 0)

            value = b.getDouble(VALUE, 0.0)
            code = b.getInt(CODE, 0)

            b.getString(EXPIRES_AT)?.let {
                expiresAt = it.toLong()
            }

            b.getString(QR_DATA)?.let {
                qrData = it
            }
        }
    }

    override fun setPresenter(presenter: LoyaltyContract.Presenter?) {
        presenter?.let {
            loyaltyPresenter = it
        }

    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }

    override fun setupViews() {
        showLoadingDialog()
        binding.apply {
            toolbar.apply {
                close.setOnClickListener { onBackPressed() }
            }

            btnRegenerate.setOnClickListener {
                if (points != 0) {
                    showLoadingDialog()
                    loyaltyPresenter.redeemPoints(points)
                }
            }

            btnRegenerate.paintFlags = btnRegenerate.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            generateQRCode(qrData)
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            val cacheUtil = CacheUtil.getInstance()
            toolbar.title.text = cacheUtil.getTranslations(Text.LOYALTY_QR_CODE, "LOYALTY QR CODE")
            tvClaimDiscountInStore.text = cacheUtil.getTranslations(
                Text.CLAIM_DISCOUNT_IN_STORE, "Claim your discount In-store:"
            )
            tvClaimDiscountInStoreSub.text = cacheUtil.getTranslations(
                Text.SCAN_QR_CODE, "Scan this QR Code to your nearest store to avail this discount."
            )
            tvClaimYourDiscount.text =
                cacheUtil.getTranslations(Text.CLAIM_YOUR_DISCOUNT, "Claim your discount")
            tvClaimYourDiscountSub.text = cacheUtil.getTranslations(
                Text.WHEN_COME_TO_ORDER,
                "When you come in to order, tell this promo code to get your discount:"
            )
            toolbar.close.text = cacheUtil.getTranslations(Text.CLOSE, "Close")
            tvExpiry.text =
                cacheUtil.getTranslations(Text.QR_CODE_WILL_EXPIRE, "This QR Code will expire in")
                    .replace("(x)", "")
            btnRegenerate.text =
                cacheUtil.getTranslations(Text.GENERATE_NEW_CODE, "Generate new code")
        }
    }

    override fun showLoyalty(loyalty: Loyalty?, name: String?) {
    }

    override fun showLoyaltyError(message: String?) {
    }

    override fun showLoyaltyPointsHistory(
        newestToOldest: Boolean, loyaltyTransactions: ArrayList<LoyaltyTransaction>?
    ) {
    }

    override fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransaction?) {
    }

    override fun showNextTier(
        tier: Tier?,
        totalAccumulatedPoints: Int,
        pointsUntilNextTier: Int,
        startingPointsOfNextTier: Int
    ) {
    }

    private fun showQRCode(d: Drawable) {
        Handler(Looper.getMainLooper()).post {
            binding.apply {
                d.let {
                    ivQrCode.setImageDrawable(it)
                }
                if (code != 0) {
                    setupSixDigitCode(code)
                }
                if (expiresAt != 0L) {
                    expiryLayout.visibility = View.VISIBLE
                    btnRegenerate.visibility = View.GONE
                    setupExpiry(expiresAt)
                }
            }
        }
    }

    override fun showQRPage(loyalty: LoyaltyPointsPosRedemption?) {
        loyalty?.let {
            points = it.points
            value = it.value.toDouble()
            code = it.code
            if (loyalty.expiresAt.isNotEmpty()) {
                expiresAt = TimeUtil.getTimeDifferenceInMillis(loyalty.expiresAt)
            }
            it.qrData?.let { qr ->
                binding.ivQrCode.alpha = 1f
                binding.tvPromoCode.alpha = 1f
                binding.tvPromoCode.setTextColor(ViewUtil.getColorMain(binding.tvPromoCode.context))
                qrData = Gson().toJson(qr)
                generateQRCode(qrData)
            }

        }
    }

    override fun showQrData(data: String?) {

    }

    private fun setupSixDigitCode(code: Int) {
        binding.tvPromoCode.text = code.toString()
    }

    private fun setupExpiry(expiresAt: Long) {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
            countDownTimer = null
        }

        countDownTimer = object : CountDownTimer(expiresAt, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val lbl: String = getFormattedTimeForCountdown(millisUntilFinished) ?: ""
                binding.tvExpiryTime.text = lbl
            }

            override fun onFinish() {
                showRegenerateCode()
            }
        }
        countDownTimer?.start()
    }

    private fun showRegenerateCode() {
        binding.apply {
            ivQrCode.alpha = 0.3f
            tvPromoCode.alpha = 0.6f
            tvPromoCode.setTextColor(ContextCompat.getColor(this@LoyaltyQrActivity, R.color.gray))
            expiryLayout.visibility = View.GONE
            btnRegenerate.visibility = View.VISIBLE
            countDownTimer = null
        }
    }

    fun getFormattedTimeForCountdown(millisUntilFinished: Long): String? {
        var millisUntilFinished = millisUntilFinished
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
        millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
        var countdownTime = String.format("%02d:%02d", minutes, seconds)
        return StringUtil.localize(countdownTime)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer = null
    }

}