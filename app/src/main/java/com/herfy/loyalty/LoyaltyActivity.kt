package com.herfy.loyalty

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityLoyaltyBinding
import com.herfy.font.FontHandler
import com.herfy.loyalty.CustomerLoyaltyDialog
import com.herfy.loyalty.CustomerLoyaltyDialog.Companion.newInstance
import com.herfy.loyalty.RedeemLoyaltyDialog
import com.herfy.loyalty.RedeemLoyaltyDialog.Companion.newInstance
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.TimeUtil
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.Tier
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LoyaltyActivity  : BaseActivity(), LoyaltyContract.View, RedeemLoyaltyDialog.OnAction,
    CustomerLoyaltyDialog.OnAction {
        
        
    @Inject    
    private lateinit var loyaltyPresenter: LoyaltyContract.Presenter
    @Inject
    private lateinit var cacheUtilHelper: CacheUtilHelper
    
    private lateinit var binding : ActivityLoyaltyBinding
    
    private val POINTS = "points"
    private val VALUE = "value"
    private val CODE = "code"
    private val EXPIRES_AT = "expires-at"
    private val QR_DATA = "qr-data"
    
    
    private var redeemLoyaltyDialog: RedeemLoyaltyDialog? = null
    private var customerLoyaltyDialog: CustomerLoyaltyDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoyaltyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        
        if (AuthUtil.getInstance().customerLoyalty != null) {
            val loyalty = AuthUtil.getInstance().customerLoyalty
            showLoyalty(
                loyalty,
                AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
            )
        }
        if (AuthUtil.getInstance().customerLoyaltyTier != null) {
            val tier = AuthUtil.getInstance().customerLoyaltyTier
            showNextTier(
                tier,
                CacheUtil.getInstance().totalAccumulatedPoints,
                CacheUtil.getInstance().pointsUntilNextTier,
                CacheUtil.getInstance().startingPointsOfNextTier
            )
        }

    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(AnimUtil.outF(), AnimUtil.outS(), R.color.white)
            }else{
                overridePendingTransition(AnimUtil.outF(), AnimUtil.outS())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loyaltyPresenter.start()
        Handler().postDelayed({
            if (isNetworkAvailable) {
                if (AuthUtil.getInstance().customerLoyalty == null) {
                    checkLoading()
                }
                loyaltyPresenter.getLoyalty()
                loyaltyPresenter.getCustomerLoyalty()
            }
        }, 350)
    }

    private fun checkLoading() {
        try {
            if (redeemLoyaltyDialog != null || customerLoyaltyDialog != null) {
                if (redeemLoyaltyDialog != null) {
                    if (!redeemLoyaltyDialog!!.isVisible) {
                        showLoadingDialog()
                    }
                }
                if (customerLoyaltyDialog != null) {
                    if (!customerLoyaltyDialog!!.isVisible) {
                        showLoadingDialog()
                    }
                }
            } else {
                showLoadingDialog()
            }
        } catch (e: Exception) {
            showLoadingDialog()
            e.printStackTrace()
        }
    }


    override fun setPresenter(presenter: LoyaltyContract.Presenter) {
        loyaltyPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
    }

    override fun setupViews() {
        binding.apply {
            toolbar.apply {
                setNavButtonAction{
                    onBackPressedDispatcher.onBackPressed()
                }
                setRightButtonAction{
                    val intent = Intent(this@LoyaltyActivity, LoyaltyPointsHistory::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    override fun setupFonts() {
    }

    override fun setupTranslations() {
        binding.apply {
            toolbar.title = cacheUtilHelper.getTranslations(Text.REWARDS_LABEL, "Rewards")
            layoutCard.apply {

            }
        }
       /* tvCurrencyEquivalence!!.text = CacheUtil.getInstance()
            .getTranslations(Text.CURRENCY_EQUIVALENCE, "(x) Equivalence")
            .replace(
                "(x)",
                CacheUtil.getInstance()
                    .getTranslations(CacheUtil.getInstance().concept.currencyCode.uppercase(Locale.getDefault()))
            )
        btnLearnMore!!.text = CacheUtil.getInstance().getTranslations(
            Text.LEARN_ABOUT_THIS_POINTS_LABEL,
            "Learn about points"
        )
        tvPointsLabel!!.text =
            CacheUtil.getInstance().getTranslations(Text.PTS_LABEL, "PTS")
                .replace("(x)", "")
        btnHistory!!.text = CacheUtil.getInstance()
            .getTranslations(Text.POINTS_HISTORY, "POINTS HISTORY")
        tvSarLabel!!.text = CacheUtil.getInstance()
            .getTranslations(CacheUtil.getInstance().concept.currencyCode.lowercase(Locale.getDefault()))
        tvTotalPoints!!.text = CacheUtil.getInstance().getTranslations(
            Text.LOYALTY_TOTAL_POINTS,
            "Total number of points"
        )
        cardNumberLabel!!.text = CacheUtil.getInstance().getTranslations(
            Text.LOYALTY_MOBILE_NUMBER_LABEL,
            "MOBILE NUMBER"
        )
        btnRedeem!!.text = CacheUtil.getInstance()
            .getTranslations(Text.REDEEM_IN_STORE, "REDEEM IN STORE")*/
    }

    override fun showNextTier(
        tier: Tier,
        totalAccumulatedPoints: Int,
        pointsUntilNextTier: Int,
        startingPointsOfNextTier: Int
    ) {
       /* AuthUtil.getInstance().customerLoyaltyTier = tier
        CacheUtil.getInstance().totalAccumulatedPoints = totalAccumulatedPoints
        CacheUtil.getInstance().pointsUntilNextTier = pointsUntilNextTier
        CacheUtil.getInstance().startingPointsOfNextTier = startingPointsOfNextTier
        for (thisTier in loyaltyPresenter.tiers) {
            Glide.with(this).load(thisTier.imageUrl).preload()
        }
        if (pointsUntilNextTier <= 0) {
            if (tier == null) {
                progress!!.max = 100
                progress!!.progress = 0
                progressLabel!!.text = "You are now a Tier member"
            } else {
                progress!!.max = 100
                progress!!.progress = 100
                progressLabel!!.text = CacheUtil.getInstance().getTranslations(
                    Text.TOTAL_POINTS_MAX_OUT,
                    "You have maxed out on our tier levels"
                )
            }
        } else {
            progress!!.max = startingPointsOfNextTier
            progress!!.progress = totalAccumulatedPoints
            progressLabel!!.text = HtmlCompat.fromHtml(
                StringUtil.localize(
                    CacheUtil.getInstance().getTranslations(
                        Text.LOYALTY_POINTS_UNTIL_TIER, "(x) points till you reach (y) Tier"
                    ).replace("(x)", pointsUntilNextTier.toString()).replace(
                        "(y)", "<b>" + tier.name.substring(0, 1).uppercase(
                            Locale.getDefault()
                        ) + tier.name.substring(1).lowercase(Locale.getDefault())
                    )
                ) + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
        dismissDialogs()*/
    }

    override fun showQRPage(loyalty: LoyaltyPointsPosRedemption) {
        if (loyalty != null) {
            cancel()
            val intent = Intent(this@LoyaltyActivity, LoyaltyQrActivity::class.java)
            intent.putExtra(POINTS, loyalty.points)
            intent.putExtra(VALUE, loyalty.value)
            intent.putExtra(CODE, loyalty.code)
            loyalty.expiresAt
            if (!loyalty.expiresAt.isEmpty()) {
                val expiresAt = TimeUtil.getTimeDifferenceInMillis(loyalty.expiresAt).toString()
                intent.putExtra(EXPIRES_AT, expiresAt)
            }
            intent.putExtra(QR_DATA, Gson().toJson(loyalty.qrData))
            startActivity(intent)
        } else {
            showLoyaltyError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED))
        }
    }

    override fun showQrData(data: String) {
        /*qrImageToolbar!!.visibility = View.VISIBLE
        Log.e("ADADADADAD", "ADADAD1:$data")
        qrImageToolbar!!.setOnClickListener {
            customerLoyaltyDialog = newInstance(data)
            customerLoyaltyDialog!!.isCancelable = true
            customerLoyaltyDialog!!.show(
                supportFragmentManager,
                CustomerLoyaltyDialog::class.java.toString()
            )
        }*/
    }

    override fun showLoyalty(loyalty: Loyalty, name: String) {
        /*if (loyalty != null && loyalty.attributes.loyaltyTier != null && !TextUtils.isEmpty(loyalty.attributes.loyaltyTier.imageUrl)) {
            ivDefaultArt!!.visibility = View.GONE
            Glide.with(this).load(loyalty.attributes.loyaltyTier.imageUrl).preload()
            Glide.with(this).load(loyalty.attributes.loyaltyTier.imageUrl).diskCacheStrategy(
                DiskCacheStrategy.ALL
            ).apply(RequestOptions.centerInsideTransform())
                .addListener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        tierCard!!.setCardBackgroundColor(Color.parseColor("#5730BF"))
                        tier!!.setTextColor(
                            ContextCompat.getColor(
                                this@LoyaltyActivity,
                                R.color.white
                            )
                        )
                        cardNumberLabel!!.setTextColor(
                            ContextCompat.getColor(
                                this@LoyaltyActivity,
                                R.color.white
                            )
                        )
                        cardNumber!!.setTextColor(
                            ContextCompat.getColor(
                                this@LoyaltyActivity,
                                R.color.white
                            )
                        )
                        customerName!!.setTextColor(
                            ContextCompat.getColor(
                                this@LoyaltyActivity,
                                R.color.white
                            )
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Thread {

                            // allow image to load into target
                            SystemClock.sleep(200)
                            runOnUiThread {
                                card!!.buildDrawingCache()
                                val brightness = calculateBrightness(card!!.drawingCache, 1)
                                if (brightness > 255 / 2) {
                                    tier!!.setTextColor(
                                        ContextCompat.getColor(
                                            this@LoyaltyActivity,
                                            R.color.black
                                        )
                                    )
                                    cardNumberLabel!!.setTextColor(
                                        ContextCompat.getColor(
                                            this@LoyaltyActivity,
                                            R.color.black
                                        )
                                    )
                                    cardNumber!!.setTextColor(
                                        ContextCompat.getColor(
                                            this@LoyaltyActivity,
                                            R.color.black
                                        )
                                    )
                                    customerName!!.setTextColor(
                                        ContextCompat.getColor(
                                            this@LoyaltyActivity,
                                            R.color.black
                                        )
                                    )
                                }
                            }
                        }.start()
                        return false
                    }
                }).into(card!!)
        } else {
            ivDefaultArt!!.visibility = View.VISIBLE
            tierCard!!.setCardBackgroundColor(Color.parseColor("#5730BF"))
        }
        if (loyalty != null && loyalty.attributes.loyaltyTier == null) {
            if (loyaltyPresenter.tiers != null && loyaltyPresenter.tiers.size > 0) {
                tier!!.text = loyaltyPresenter.tiers[0].name
            }
        } else {
            tier!!.text = loyalty.attributes.loyaltyTier.name
        }
        val customer = AuthUtil.getInstance().customer
        cardNumber!!.text = customer.attributes.mobile
        customerName!!.text = name
        val formatter = DecimalFormat("#,###")
        tvSarValue!!.text =
            StringUtil.localize(formatter.format(loyalty.attributes.currentBalanceAmount))
        tvPoints!!.text =
            StringUtil.localize(formatter.format(loyalty.attributes.currentBalance.toLong()))
        if (loyalty.attributes.currentBalance > 0) {
            btnRedeem!!.visibility = View.VISIBLE
            btnRedeem!!.setOnClickListener {
                if (loyalty.attributes.currentBalanceAmount > 0) {
                    redeemLoyaltyDialog = newInstance(loyalty)
                    redeemLoyaltyDialog!!.isCancelable = true
                    redeemLoyaltyDialog!!.show(
                        supportFragmentManager,
                        RedeemLoyaltyDialog::class.java.toString()
                    )
                } else {
                    showAlertDialog(
                        "",
                        CacheUtil.getInstance().getTranslations(
                            "not_enough_balance_amount_message",
                            "Please make sure you have at least 1 (x) to redeem"
                        ).replace(
                            "(x)", CacheUtil.getInstance().getTranslations(
                                CacheUtil.getInstance().concept.currencyCode.lowercase(
                                    Locale.getDefault()
                                )
                            )
                        ),
                        CacheUtil.getInstance().getTranslations(Text.OK)
                    ) { dialog, which -> dialog.dismiss() }
                }
            }
        } else {
            btnRedeem!!.visibility = View.GONE
        }
        dismissDialogs()*/
    }

    fun calculateBrightness(bitmap: Bitmap, skipPixel: Int): Int {
        var R = 0
        var G = 0
        var B = 0
        val height = bitmap.height
        val width = bitmap.width
        var n = 0
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var i = 0
        while (i < pixels.size) {
            val color = pixels[i]
            R += Color.red(color)
            G += Color.green(color)
            B += Color.blue(color)
            n++
            i += skipPixel
        }
        return (R + B + G) / (n * 3)
    }

    override fun showLoyaltyError(message: String) {
        showAlertDialog(
            "",
            message,
            CacheUtil.getInstance().getTranslations(Text.OK)
        ) { dialog, which ->
            dialog.dismiss()
            onBackPressed()
        }
    }

    override fun showLoyaltyPointsHistory(
        newestToOldest: Boolean,
        loyaltyTransactions: ArrayList<LoyaltyTransaction>
    ) {
    }

    override fun setLoyaltyHistory(loyaltyTransaction: LoyaltyTransaction) {}
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1024) {
            onBackPressed()
        }
    }

    override fun navigate(points: Int) {
        showLoadingDialog()
        loyaltyPresenter.redeemPoints(points)
    }

    override fun cancel() {
        if (redeemLoyaltyDialog == null) {
            return
        }
        redeemLoyaltyDialog!!.dismiss()
        redeemLoyaltyDialog = null
    }

    override fun closeQR() {
        if (customerLoyaltyDialog == null) {
            return
        }
        customerLoyaltyDialog!!.dismiss()
        customerLoyaltyDialog = null
    }
}


