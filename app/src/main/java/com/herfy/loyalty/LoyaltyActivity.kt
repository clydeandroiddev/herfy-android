package com.herfy.loyalty

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityLoyaltyBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.OnLayoutLoadingCompleted
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.TimeUtil
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoyaltyActivity : BaseActivity(), LoyaltyContract.View, RedeemLoyaltyDialog.OnAction,
    CustomerLoyaltyDialog.OnAction {


    @Inject
    lateinit var loyaltyPresenter: LoyaltyContract.Presenter

    @Inject
    lateinit var cacheUtilHelper: CacheUtilHelper

    private lateinit var binding: ActivityLoyaltyBinding


    private val POINTS = "points"
    private val VALUE = "value"
    private val CODE = "code"
    private val EXPIRES_AT = "expires-at"
    private val QR_DATA = "qr-data"

    private val rewardsTierAdapter : RewardTierAdapter by lazy {
        RewardTierAdapter()
    }

    private var redeemLoyaltyDialog: RedeemLoyaltyDialog? = null
    private var customerLoyaltyDialog: CustomerLoyaltyDialog? = null

    private val loyaltyQrActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isNetworkAvailable) {
                        if (AuthUtil.getInstance().customerLoyalty == null) {
                            checkLoading()
                        }
                        loyaltyPresenter.getLoyalty()
                        loyaltyPresenter.getCustomerLoyalty()
                    }
                }, 350)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoyaltyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setupLoyaltyImageHeight()
        loyaltyPresenter.start()

        if (AuthUtil.getInstance().customerLoyalty != null) {
            val loyalty = AuthUtil.getInstance().customerLoyalty

            if (AuthUtil.getInstance().customerLoyaltyTier != null) {
                val tier = AuthUtil.getInstance().customerLoyaltyTier
                showNextTier(
                    tier,
                    CacheUtil.getInstance().totalAccumulatedPoints,
                    CacheUtil.getInstance().pointsUntilNextTier,
                    CacheUtil.getInstance().startingPointsOfNextTier,
                    CacheUtil.getInstance().pointsUntilNextTier == 0,
                    loyalty
                )
            }
            showLoyalty(
                loyalty,
                AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
            )
            val tiers = CacheUtil.getInstance().loyaltyTiers
            loyaltyPresenter.setTiers(tiers)
            showTiers(loyalty)
        }
        getLoyalty()

    }

    private fun setupLoyaltyImageHeight() {
        binding.layoutCard.apply{
            fmShimmerLoyalty.apply {
                val imageHeight = (Resources.getSystem().displayMetrics.widthPixels / Values.LOYALTY_CARD_ASPECT_RATIO)
                layoutParams.height = imageHeight.toInt()
            }
            clCardMain.apply {
                val imageHeight = (Resources.getSystem().displayMetrics.widthPixels / Values.LOYALTY_CARD_ASPECT_RATIO)
                layoutParams.height = imageHeight.toInt()
            }
        }
    }

    private fun getLoyalty() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isNetworkAvailable) {
                if (AuthUtil.getInstance().customerLoyalty == null) {
                    binding.layoutCard.fmShimmerLoyalty.visible()
                    checkLoading()
                }
                loyaltyPresenter.getLoyalty()
            }
        }, 350)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,AnimUtil.outF(), AnimUtil.outS(),getColor(R.color.white))
            } else {
                overridePendingTransition(AnimUtil.outF(), AnimUtil.outS())
            }
            finish()
        }
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
        setupRewardTiers()
        binding.apply {
            binding.layoutCard.apply {
                progress.isEnabled = false
            }

            toolbar.apply {
                setNavButtonAction {
                    onBackPressedDispatcher.onBackPressed()
                }
                setRightButtonAction {
                    val intent = Intent(this@LoyaltyActivity, LoyaltyPointsHistory::class.java)
                    loyaltyQrActivityLauncher.launch(intent)
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                getLoyalty()
            }
        }

    }

    private fun setupRewardTiers() {
        binding.apply {
            layoutRewardsTier.apply {
                rvRewardTier.apply {
                    adapter = rewardsTierAdapter
                    setHasFixedSize(true)
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
                tvAshabHerfy.text = cacheUtilHelper.getTranslations(Text.ASHAB_HERFY, "Ashab Herfy")
                tvTotalPointsLabel.text =
                    cacheUtilHelper.getTranslations(Text.TOTAL_POINTS, "Total Points")
                tvSarValueLabel.text =
                    cacheUtilHelper.getTranslations(Text.SAR_VALUE_LABEL, "SR VALUE")
            }
            layoutRewardsTier.apply {
                tvRewardTierLabel.text =
                    cacheUtilHelper.getTranslations(Text.REWARD_TIER_LABEL, "Reward Tier")
            }
            layoutScanPoints.apply {
                tvScanPoints.text =
                    cacheUtilHelper.getTranslations(Text.SCAN_POINTS_LABEL, "Scan to earn points")
                tvScanPointsDescription.text = cacheUtilHelper.getTranslations(
                    Text.SCAN_POINTS_DESCRIPTION,
                    "Scan your card to earn points on order in the restaurant."
                )
            }
        }
    }

    override fun showNextTier(
        tier: LoyaltyTierV2,
        totalAccumulatedPoints: Int,
        pointsUntilNextTier: Int,
        startingPointsOfNextTier: Int,
        isMaxTier: Boolean,
        loyalty: LoyaltyV2
    ) {
        binding.apply {
            layoutCard.apply {
                AuthUtil.getInstance().customerLoyaltyTier = tier
                CacheUtil.getInstance().totalAccumulatedPoints = totalAccumulatedPoints
                CacheUtil.getInstance().pointsUntilNextTier = pointsUntilNextTier
                CacheUtil.getInstance().startingPointsOfNextTier = startingPointsOfNextTier
                if (pointsUntilNextTier <= 0) {
                    val tierName = loyalty?.attributes?.loyaltyTier?.label?.english?: ""
                    if (isMaxTier) {
                        progress.max = 100
                        progress.progress = 100
                        tvTierProgressDesc.text = CacheUtil.getInstance().getTranslations(
                            Text.CONGRATS_YOUVE_REACHED_X, "Congrats! You’ve reached (x)"
                        ).replace("(x)",tierName)
                    }
                } else {
                    var loyaltyTier = tier?.label?.english
                    if(!loyaltyTier.isNullOrEmpty() && LocaleUtil.getInstance().isEnglish) {
                        loyaltyTier = loyaltyTier.lowercase().replaceFirstChar(Char::titlecase)
                    }
                    progress.max = startingPointsOfNextTier
                    progress.progress = totalAccumulatedPoints
                    tvTierProgressDesc.text = cacheUtilHelper.getTranslations(Text.POINTS_UNTIL_NEXT_TIER, "(x) points until (y)").replace("(x)", pointsUntilNextTier.toString()).replace("(y)", loyaltyTier?:"")
                }

            }
        }
        dismissDialogs()
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
    }


    override fun showTiers(loyalty: LoyaltyV2) {
        binding.apply {
            val tiers = loyaltyPresenter.tiers //loyalty.attributes.tiers
            if(tiers != null && tiers.size > 0){
                rewardsTierAdapter.updateList(tiers)
                layoutRewardsTier.apply {
                    rvRewardTier.OnLayoutLoadingCompleted {
                        placeholderRewardTier.shimmerRewardTier.gone()
                        clRewardTier.visible()
                    }
                }

            }else {
                fmRewardsTier.gone()
            }
        }

    }

    override fun showLoyalty(loyalty: LoyaltyV2, name: String) {
        binding.apply {
            swipeRefreshLayout.isRefreshing = false
            loyalty?.let { loyalty ->
                if(loyalty.attributes.loyaltyTier != null){
                    layoutCard.apply {
                        var loyaltyTier: String = when {
                            loyalty.attributes.loyaltyTier == null
                                    && loyaltyPresenter.tiers != null &&
                                    loyaltyPresenter.tiers.size > 0 -> {
                                loyaltyPresenter.tiers[0]?.label?.english?:""
                            }
                            else -> {
                                loyalty.attributes?.loyaltyTier?.label?.english?: ""
                            }
                        }
                        if(LocaleUtil.getInstance().isEnglish) {
                            loyaltyTier = loyaltyTier.lowercase().replaceFirstChar(Char::titlecase)
                        }
                        var tierBackground = com.herfy.home.R.drawable.background_bronze_tier
                        if (loyaltyTier.equals(
                                "Silver",
                                ignoreCase = true
                            ) || loyaltyTier.equals("فضي")
                        ) {
                            tierBackground = com.herfy.home.R.drawable.background_silver_tier
                        } else if (loyaltyTier.equals(
                                "Gold",
                                ignoreCase = true
                            ) || loyaltyTier.equals("ذهبي")
                        ) {
                            tierBackground = com.herfy.home.R.drawable.background_gold_tier
                        } else if (loyaltyTier.equals(
                                "Diamond",
                                ignoreCase = true
                            ) || loyaltyTier.equals("البلاتين")
                        ) {
                            tierBackground = com.herfy.home.R.drawable.background_diamond_tier
                        }
                        tvTier.text = loyaltyTier
                        tvTier.setBackgroundResource(tierBackground)
                        val currentBalanceValue = String.format("%.2f", loyalty.attributes.currentBalanceAmount)
                        tvSarValue.text =
                            StringUtil.localize(currentBalanceValue)
                        tvTotalPoints.text =
                            StringUtil.localize(loyalty.attributes.currentBalance.toLong().toString())
                        tvProgressTier.text = loyaltyTier

                        val tierImageUrl = loyalty.attributes?.loyaltyTier?.imageUrl ?: ""
                        if(tierImageUrl.isNotEmpty()){
                            if(!isFinishing && !isDestroyed){
                                Glide.with(this@LoyaltyActivity)
                                    .load(tierImageUrl)
                                    .skipMemoryCache(false)
                                    .apply(RequestOptions.noTransformation())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(object : DrawableImageViewTarget(binding.layoutCard.ivLoyaltyCard) {
                                        override fun setResource(resource: Drawable?) {
                                            super.setResource(resource)
                                            binding.layoutCard.apply {
                                                if (resource == null) {
                                                    ivLoyaltyCard.setImageResource(com.herfy.home.R.drawable.background_loyalty_card)
                                                }
                                                fmShimmerLoyalty.gone()
                                                clCardMain.visible()
                                            }

                                        }
                                    })
                            }
                        }else {
                            binding.layoutCard.apply {
                                ivLoyaltyCard.setImageResource(com.herfy.home.R.drawable.background_loyalty_card)
                                fmShimmerLoyalty.gone()
                                clCardMain.visible()
                            }
                        }
                    }
                }else{
                    Handler(Looper.getMainLooper()).postDelayed({
                        showLoyaltyError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED))
                    },350)

                }

            }

        }
        dismissDialogs()
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


