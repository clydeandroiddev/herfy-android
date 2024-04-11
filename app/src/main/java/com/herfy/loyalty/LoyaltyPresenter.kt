package com.herfy.loyalty

import android.annotation.SuppressLint
import android.util.Pair
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier
import com.skylinedynamics.solosdk.api.models.objects.QrData
import com.skylinedynamics.solosdk.api.models.objects.Tier
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
    private var tiers: List<Tier> = ArrayList()
    private val disposables = CompositeDisposable()

    init {
        loyaltyView.setPresenter(this)
    }

    override fun start() {
        loyaltyView.setupViews()
        loyaltyView.setupFonts()
        loyaltyView.setupTranslations()
    }


    val tiersObservable: Single<List<Tier>>
        get() = Single.create { source: SingleEmitter<List<Tier>> ->
            CustomersApiCall.getInstance().getLoyaltyTiers(object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val tiersArray = `object`.getJSONArray("data")
                        val type = object : TypeToken<List<Tier?>?>() {}.type
                        val tiers =
                            GsonHelper.gsonFinal().fromJson<List<Tier>>(tiersArray.toString(), type)
                        // TODO: Should set Tier as Comparable in SDK
                        Collections.sort(
                            tiers,
                            Comparator { o1: Tier, o2: Tier -> o1.startPoint - o2.startPoint } as Comparator<Tier>)
                        source.onSuccess(tiers)
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
    val loyaltyObservable: Single<Loyalty>
        get() = Single.create { source: SingleEmitter<Loyalty> ->
            CustomersApiCall.getInstance().getCustomerLoyalty(
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
        Single.zip(
            tiersObservable, loyaltyObservable
        ) { tiers: List<Tier>, loyalty: Loyalty -> Pair(loyalty, tiers) }
            .doOnSubscribe { disposable: Disposable? ->
                disposables.add(
                    disposable!!
                )
            }.subscribe(
                { pair: Pair<Loyalty, List<Tier>> ->
                    val tiers = pair.second
                    val loyalty = pair.first
                    this.tiers = tiers
                    val totalPointsAccumulated = loyalty.attributes.currentBalance
                    var eligibleTiers = Observable.fromIterable(tiers)
                        .filter { tier: Tier -> totalPointsAccumulated <= tier.endPoint }
                        .toList().blockingGet()
                    var pointsToNextTier = 0
                    loyaltyView.showLoyalty(
                        loyalty,
                        AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                    )
                    loyaltyView.showTiers(loyalty)
                    var nextTier: Tier? = null
                    if (eligibleTiers.size <= 0 && tiers.size >= 1) {
                        eligibleTiers = ArrayList()
                        eligibleTiers.add(tiers[tiers.size - 1])
                    }
                    if (eligibleTiers.size >= 1) {
                        nextTier = eligibleTiers[0]
                    }
                    if (eligibleTiers.size > 1) {
                        nextTier = eligibleTiers[1]
                        pointsToNextTier = nextTier.startPoint - loyalty.attributes.currentBalance
                    }
                    var startPoints = 0
                    if (nextTier != null) {
                        startPoints = nextTier.startPoint
                    }
                    loyaltyView.showNextTier(
                        nextTier,
                        totalPointsAccumulated,
                        pointsToNextTier,
                        startPoints
                    )
                })
            { error: Throwable? ->
                val loyalty = Loyalty()
                val nextTier = Tier()
                tiers = ArrayList()
                loyaltyView.showLoyalty(
                    loyalty,
                    AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                )
                loyaltyView.showTiers(loyalty)
                loyaltyView.showNextTier(nextTier, 0, 100, 100)
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
        CustomersApiCall.getInstance().getLoyaltyTiers(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val tiersArray = `object`.getJSONArray("data")
                    val type = object : TypeToken<List<Tier?>?>() {}.type
                    val tiers =
                        GsonHelper.gsonFinal().fromJson<List<Tier>>(tiersArray.toString(), type)
                    Collections.sort(
                        tiers,
                        Comparator { o1: Tier, o2: Tier -> o1.startPoint - o2.startPoint } as Comparator<Tier>)
                    val loyalty = AuthUtil.getInstance().customerLoyalty
                    val totalPointsAccumulated = loyalty.attributes.pointsAccumulated
                    val eligibleTiers = Observable.fromIterable(tiers)
                        .filter { tier: Tier -> totalPointsAccumulated <= tier.endPoint }
                        .toList()
                        .blockingGet()
                    val nextTier =
                        if (eligibleTiers.isEmpty()) tiers[tiers.size - 1] else eligibleTiers[0]
                    val pointsToNextTier =
                        if (eligibleTiers.size > 1) nextTier.startPoint - totalPointsAccumulated else 0
                    val startPoints = nextTier.startPoint
                    loyaltyView.showNextTier(
                        nextTier,
                        totalPointsAccumulated,
                        pointsToNextTier,
                        startPoints
                    )
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
            }

            override fun onError(o: Any) {
                val loyaltyAttributes = LoyaltyAttributes()
                val loyaltyTier = LoyaltyTier()
                loyaltyTier.name = "Classic"
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                val currentTier = Tier()
                currentTier.name = "Classic"
                currentTier.startPoint = 0
                val nextTier = Tier()
                nextTier.name = "Silver"
                nextTier.startPoint = 100
                loyaltyAttributes.loyaltyTier = loyaltyTier
                loyaltyAttributes.currentBalance = 0
                tiers = ArrayList(Arrays.asList(currentTier))
                loyaltyView.showNextTier(nextTier, 0, 100, 100)
            }
        })
    }


    override fun getTiers(): List<Tier> {
        return tiers
    }

}
