package com.herfy.loyalty

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityLoyaltyQrBinding
import com.herfy.util.QRHelper
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.TimeUtil
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransactionV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val POINTS = "points"
const val VALUE = "value"
const val CODE = "code"
const val EXPIRES_AT = "expires-at"
const val QR_DATA = "qr-data"


@AndroidEntryPoint
class LoyaltyQrActivity : BaseActivity(), LoyaltyContract.View {

    private lateinit var binding: ActivityLoyaltyQrBinding
    @Inject
    lateinit var loyaltyPresenter: LoyaltyContract.Presenter
    private var countDownTimer: CountDownTimer? = null

    private var points: Int = 0
    private var value: Double = 0.0
    private var code: String = ""
    private var expiresAt: Long = 0L
    private var qrData: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoyaltyQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadPreferences()
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
            code = b.getString(CODE, "")

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
            generateQRCode(qrData)
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            val cacheUtil = CacheUtil.getInstance()
            tvClaimYourDiscount.text =
                cacheUtil.getTranslations(Text.CLAIM_YOUR_DISCOUNT, "Claim your discount")
            tvClaimYourDiscountSub.text = cacheUtil.getTranslations(
                Text.WHEN_COME_TO_ORDER,
                "When you come in to order, tell this promo code to get your discount:"
            )
        }
    }

    override fun showLoyalty(loyalty: LoyaltyV2, name: String?) {
    }

    override fun showLoyaltyError(message: String?) {
    }

    override fun showLoyaltyPointsHistory(
        newestToOldest: Boolean, loyaltyTransactions: ArrayList<LoyaltyTransaction>?
    ) {
    }

    override fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransactionV2?) {
    }

    override fun showNextTier(
        tier: LoyaltyTierV2,
        totalAccumulatedPoints: Int,
        pointsUntilNextTier: Int,
        startingPointsOfNextTier: Int,
        isMaxTier: Boolean,
        loyalty: LoyaltyV2
    ) {
    }

    private fun showQRCode(d: Drawable) {
        Handler(Looper.getMainLooper()).post {
            binding.apply {
                d.let {
                    ivQrCode.setImageDrawable(it)
                }
                if (code.isNotEmpty()) {
                    setupSixDigitCode(code)
                }
                if (expiresAt != 0L) {
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
                qrData = Gson().toJson(qr)
                generateQRCode(qrData)
            }

        }
    }

    override fun showQrData(data: String?) {

    }

    override fun showTiers(loyalty: LoyaltyV2) {
        TODO("Not yet implemented")
    }

    private fun setupSixDigitCode(code: String) {
    }

    private fun setupExpiry(expiresAt: Long) {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
            countDownTimer = null
        }

        countDownTimer = object : CountDownTimer(expiresAt, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val lbl: String = getFormattedTimeForCountdown(millisUntilFinished) ?: ""

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