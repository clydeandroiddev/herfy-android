package com.herfy.loyalty

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyProfile
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2
import com.skylinedynamics.solosdk.api.models.objects.QrData
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.api.models.objects.TranslatedV2
import com.skylinedynamics.solosdk.api.models.requestbodies.PosPointsRedemptionBody
import com.skylinedynamics.solosdk.util.GsonHelper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.Collections
import javax.inject.Inject

@SuppressLint("CheckResult")
class LoyaltyPresenter @Inject constructor(
    private val loyaltyView: LoyaltyContract.View
) : LoyaltyContract.Presenter {
    private var tiers: List<LoyaltyTierV2> = ArrayList()
    private val disposables = CompositeDisposable()



    init {
        loyaltyView.setPresenter(this)
    }

    override fun start() {
        loyaltyView.setupViews()
        loyaltyView.setupFonts()
        loyaltyView.setupTranslations()
    }


    val tiersObservable: Single<List<LoyaltyTierV2>>
        get() = Single.create { source: SingleEmitter<List<LoyaltyTierV2>> ->
            CustomersApiCall.getInstance().getLoyaltyTiersV2(object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val tiersArray = `object`.getJSONArray("data")
                        val type = object : TypeToken<List<LoyaltyTierV2?>?>() {}.type
                        val tiers = GsonHelper.gsonFinal().fromJson<List<LoyaltyTierV2>?>(tiersArray.toString(), type)

                        Collections.sort(
                            tiers,
                            Comparator { o1: LoyaltyTierV2, o2: LoyaltyTierV2 -> o1.startingPoints - o2.startingPoints } as Comparator<LoyaltyTierV2>)
                        source.onSuccess(tiers?: arrayListOf())
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                    }
                }

                override fun onError(o: Any) {
                    try{
                        if (o is Throwable) {
                            source.onError(o)
                        } else {
                            source.onError(
                                Throwable(
                                    CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED, "An error occurred."),

                                )
                            )
                        }
                    }catch (e : Exception){
                        e.printStackTrace()
                    }

                }
            })
        }
    val loyaltyObservable: Single<LoyaltyV2>
        get() = Single.create { source: SingleEmitter<LoyaltyV2> ->
            CustomersApiCall.getInstance().getLoyaltyCustomerV2(
                AuthUtil.getInstance().customer.id,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val `object` = JSONObject(o.toString())
                            val data = `object`.getJSONObject("data")
                            if (data.getJSONObject("loyalty_profile")
                                    .optJSONObject("current_tier") == null || data.getJSONObject("loyalty_profile")["current_tier"] is JSONObject
                            ) {
                                val loyalty = GsonHelper.gsonFinal()
                                    .fromJson(data.toString(), LoyaltyV2::class.java)
                                AuthUtil.getInstance().customerLoyalty = loyalty
                                source.onSuccess(loyalty)
                            } else {
                                source.onError(
                                    Throwable(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED)
                                    )
                                )
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onError(e)
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        if (o is JSONObject) {
                            try {
                                val error = o["error"]
                                var detail =
                                    CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                                if (error is JSONObject) {
                                    detail = error.getString("detail")
                                } else if (error is JSONArray) {
                                    val errorObject = error.getJSONObject(0)
                                    detail = errorObject.getString("detail")
                                }
                                source.onError(Throwable(detail))
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                source.onError(
                                    Throwable(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED)
                                    )
                                )
                            }
                        } else {
                            if (o is Throwable) {
                                source.onError(o)
                            } else {
                                source.onError(
                                    Throwable(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED)
                                    )
                                )
                            }
                        }
                    }
                })
        }

    override fun redeemPoints(points: Int) {
        val customerId = AuthUtil.getInstance().customer.id
        val body = PosPointsRedemptionBody(points)
        CustomersApiCall.getInstance()
            .redeemLoyaltyPointsForPos(customerId, body, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    var data: LoyaltyPointsPosRedemption? = null
                    try {
                        data = Gson().fromJson(o.toString(), LoyaltyPointsPosRedemption::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    loyaltyView.showQRPage(data)
                }

                override fun onError(o: Any) {
                    AppLogger.error(o.toString())
                    loyaltyView.showQRPage(null)
                }
            })
    }

    override fun getLoyalty() {
        // Ensures that there will always be a tier when loyalty is shown
      /*  CustomersApiCall.getInstance().getCustomerLoyalty(
            AuthUtil.getInstance().customer.id,
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val data = `object`.getJSONObject("data")
                        if (data.getJSONObject("attributes")
                                .optJSONObject("tier") == null || data.getJSONObject("attributes")["tier"] is JSONObject
                        ) {
                            val loyalty = GsonHelper.gsonFinal()
                                .fromJson(data.toString(), Loyalty::class.java)
                            AuthUtil.getInstance().customerLoyalty = loyalty
                            loyaltyView.showLoyalty(
                                loyalty,
                                AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                            )
                            loyaltyView.showTiers(loyalty)
                            //source.onSuccess(loyalty)
                        } else {
                           loyaltyView.showLoyaltyError( CacheUtil.getInstance()
                               .getTranslations(Text.AN_ERROR_OCCURRED))
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError(e)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    if (o is JSONObject) {
                        try {
                            val error = o["error"]
                            var detail =
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            if (error is JSONObject) {
                                detail = error.getString("detail")
                            } else if (error is JSONArray) {
                                val errorObject = error.getJSONObject(0)
                                detail = errorObject.getString("detail")
                            }
                            loyaltyView.showLoyaltyError(detail)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            loyaltyView.showLoyaltyError( CacheUtil.getInstance()
                                .getTranslations(Text.AN_ERROR_OCCURRED))
                        }
                    } else {
                        loyaltyView.showLoyaltyError( CacheUtil.getInstance()
                            .getTranslations(Text.AN_ERROR_OCCURRED))
                    }
                }
            })*/
        Single.zip(
            loyaltyObservable,tiersObservable
        ) { loyalty: LoyaltyV2,tiers: List<LoyaltyTierV2>  -> Pair(loyalty, tiers) }
            .doOnSubscribe { disposable: Disposable? ->
                disposables.add(
                    disposable!!
                )
            }.subscribe(
                { pair: Pair<LoyaltyV2, List<LoyaltyTierV2>> ->
                    val tiers = pair.second
                    val loyalty = pair.first
                    this.tiers = tiers
                    val totalPointsAccumulated = loyalty.attributes.currentBalance
                    var eligibleTiers = Observable.fromIterable(tiers)
                        .filter { tier: LoyaltyTierV2 -> totalPointsAccumulated <= tier.endingPoints }
                        .toList().blockingGet()
                    var pointsToNextTier = 0
                    if (loyalty.attributes.loyaltyTier == null){
                        val tier = tiers[0]
                        val loyaltyTier = LoyaltyTierV2()
                        val translated = TranslatedV2()
                        translated.english = tier.label.english
                        translated.arabic = tier.label.arabic
                        translated.french = tier.label.french
                        loyaltyTier.label = translated
                        loyaltyTier.id = tier.id
                        loyaltyTier.imageUrl = tier.imageUrl
                        loyalty.attributes.loyaltyTier = loyaltyTier
                    }
                    AuthUtil.getInstance().customerLoyalty = loyalty
                    loyaltyView.showLoyalty(
                        loyalty,
                        AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                    )
                    loyaltyView.showTiers(loyalty)
                    var nextTier: LoyaltyTierV2? = null
                    var isMaxTier = false
                    if (eligibleTiers.size <= 0 && tiers.size >= 1) {
                        eligibleTiers = ArrayList()
                        eligibleTiers.add(tiers[tiers.size - 1])
                    }
                    if (eligibleTiers.size >= 1) {
                        nextTier = eligibleTiers[0]
                    }
                    if (eligibleTiers.size > 1) {
                        nextTier = eligibleTiers[1]
                        //pointsToNextTier = nextTier.startingPoints - loyalty.attributes.currentBalance
                        pointsToNextTier = loyalty.attributes.pointsRequiredForNextTier
                    }
                    var startPoints = 0
                    if (nextTier != null) {
                        startPoints = nextTier.startingPoints
                        if(loyalty.attributes.loyaltyTier != null){
                            isMaxTier = nextTier.label.english == loyalty.attributes.loyaltyTier.label.english
                        }
                        if(isMaxTier){
                            pointsToNextTier = 0
                        }
                    }
                    loyaltyView.showNextTier(
                        nextTier,
                        totalPointsAccumulated,
                        pointsToNextTier,
                        startPoints,
                        isMaxTier,
                        loyalty
                    )
                })
            { error: Throwable? ->
                var loyalty = LoyaltyV2()
                if(AuthUtil.getInstance().customerLoyalty != null){
                   loyalty = AuthUtil.getInstance().customerLoyalty
                }
                val nextTier = LoyaltyTierV2()
                tiers = ArrayList()
                loyaltyView.showLoyalty(
                    loyalty,
                    AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                )
                loyaltyView.showTiers(loyalty)
                loyaltyView.showNextTier(nextTier, 0, 100, 100, false, loyalty)
            }
    }

    override fun getCustomerLoyalty() {
        val customerId = AuthUtil.getInstance().customer.id
        CustomersApiCall.getInstance().getLoyaltyCustomer(customerId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val data = `object`.getJSONObject("data")
                    val qrData = Gson().fromJson(
                        data.getJSONObject("qr-data").toString(),
                        QrData::class.java
                    )
                    loyaltyView.showQrData(Gson().toJson(qrData))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(o: Any) {
                AppLogger.error(o.toString())
            }
        })
    }


    private fun fetchAndShowNextTier() {
        CustomersApiCall.getInstance().getLoyaltyTiersV2(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val tiersArray = `object`.getJSONArray("data")
                    val type = object : TypeToken<List<LoyaltyTierV2?>?>() {}.type
                    val tiers =
                        GsonHelper.gsonFinal().fromJson<List<LoyaltyTierV2>>(tiersArray.toString(), type)
                    Collections.sort(
                        tiers,
                        Comparator { o1: LoyaltyTierV2, o2: LoyaltyTierV2 -> o1.startingPoints - o2.startingPoints } as Comparator<LoyaltyTierV2>)
                    val loyalty = AuthUtil.getInstance().customerLoyalty
                    val totalPointsAccumulated = loyalty.attributes.currentBalance
                    val eligibleTiers = Observable.fromIterable(tiers)
                        .filter { tier: LoyaltyTierV2 -> totalPointsAccumulated <= tier.endingPoints }
                        .toList()
                        .blockingGet()
                    val nextTier =
                        if (eligibleTiers.isEmpty()) tiers[tiers.size - 1] else eligibleTiers[0]
                    //val pointsToNextTier = if (eligibleTiers.size > 1) nextTier.startingPoints - totalPointsAccumulated else 0
                    val pointsToNextTier = if (eligibleTiers.size > 1) loyalty.attributes.pointsRequiredForNextTier else 0
                    val startPoints = nextTier.startingPoints
                    loyaltyView.showNextTier(
                        nextTier,
                        totalPointsAccumulated,
                        pointsToNextTier,
                        startPoints,
                        false,
                        loyalty
                    )
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
            }

            override fun onError(o: Any) {
                val loyaltyAttributes = LoyaltyProfile()
                val loyaltyTier = LoyaltyTierV2()
                loyaltyTier.label.english = "Classic"
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                val currentTier = LoyaltyTierV2()
                currentTier.label.english = "Classic"
                currentTier.startingPoints = 0
                val nextTier = Tier()
                nextTier.name = "Silver"
                nextTier.startPoint = 100
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                tiers = ArrayList(Arrays.asList(currentTier))
                //loyaltyView.showNextTier(nextTier, 0, 100, 100, false, loyalty)
            }
        })
    }

    override fun setTiers(tiers: List<LoyaltyTierV2>){
        this.tiers = tiers
    }
    override fun getTiers(): List<LoyaltyTierV2> {
        return tiers
    }

}
