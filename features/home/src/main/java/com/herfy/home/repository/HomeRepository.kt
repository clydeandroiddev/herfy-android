package com.herfy.home.repository

import com.google.gson.reflect.TypeToken
import com.herfy.data.AppDatabase
import com.herfy.home.data.HomeResponse
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CacheUtilHelper.Companion.get
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CmsApiCall
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Promotion
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject


interface HomeRepository {
    suspend fun getSliders(response: (HomeResponse) -> Unit)

    suspend fun getGuestDigitalCoupons(orderType: OrderType, response: (HomeResponse) -> Unit)

    suspend fun getSliderId(): String

    suspend fun getGetOrderType(): OrderType
}

class HomeRepositoryImpl @Inject constructor(
    val appDatabase: AppDatabase,
    val cacheUtilHelper: CacheUtilHelper
) : HomeRepository {
    override suspend fun getSliders(response: (HomeResponse) -> Unit) {
        CmsApiCall.getInstance()
            .getSlider(getSliderId(), object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    val slides = arrayListOf<Slide>()
                    try {
                        val `object` = JSONObject(o.toString())
                        val dataArray = `object`.getJSONArray("data")
                        for (i in 0 until dataArray.length()) {
                            val slideObject = dataArray.getJSONObject(i)
                            val slide = Slide.parse(slideObject)
                            slides.add(slide)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    response(HomeResponse(true, slides, ""))
                }

                override fun onError(o: Any) {
                    response(HomeResponse(false, null, o.toString()))
                }
            })
    }

    override suspend fun getGuestDigitalCoupons(
        orderType: OrderType,
        response: (HomeResponse) -> Unit
    ) {
        CouponsApiCall.getInstance().getGuestCampaigns(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {

                    val `object` = JSONObject(o.toString())

                    val digitalCoupons: ArrayList<Campaign> = GsonHelper.gsonFinal().fromJson(
                        `object`.getJSONArray("data").toString(),
                        object : TypeToken<ArrayList<Campaign>>() {}.type
                    )
                    if (`object`.has("included")) {
                        val promotions = GsonHelper.gsonFinal().fromJson<ArrayList<Promotion>>(
                            `object`.getJSONArray("included").toString(),
                            object :
                                TypeToken<ArrayList<Promotion>>() {}.type
                        )
                        val promotionsHashMap = HashMap<String, Promotion>()
                        for (promotion in promotions) {
                            promotionsHashMap[promotion.id] = promotion
                        }
                        for (digitalCoupon in digitalCoupons) {
                            digitalCoupon.promotion =
                                promotionsHashMap[digitalCoupon.attributes.promotionId]
                        }
                        val filteredDigitalCoupons: MutableList<Campaign> = ArrayList()
                        try {
                            for (digitalCoupon in digitalCoupons) {
                                if (digitalCoupon.promotion != null) {
                                    if (digitalCoupon.promotion.attributes != null) {
                                        if (digitalCoupon.promotion.attributes.type.equals(
                                                "delivery",
                                                ignoreCase = true
                                            )
                                        ) {
                                            if (orderType === OrderType.DELIVERY) {
                                                filteredDigitalCoupons.add(digitalCoupon)
                                            }
                                        } else {
                                            filteredDigitalCoupons.add(digitalCoupon)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        digitalCoupons.clear()
                        digitalCoupons.addAll(filteredDigitalCoupons)
                    }
                    response(HomeResponse(true, digitalCoupons, ""))
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                    onError("")
                }
            }

            override fun onError(o: Any) {
                response(HomeResponse(false, null, o.toString()))
            }
        })
    }

    override suspend fun getSliderId(): String {
        val pref = cacheUtilHelper.pref()
        return pref[CacheUtilHelper.SLIDER_ID, ""] ?: ""
    }

    override suspend fun getGetOrderType(): OrderType {
        val pref = cacheUtilHelper.pref()
        return when (pref[CacheUtilHelper.ORDER_TYPE, 0]) {
            1 -> OrderType.PICKUP;
            2 -> OrderType.DINE_IN;
            3 -> OrderType.DELIVERY;
            4 -> OrderType.CURBSIDE;
            else -> OrderType.NONE;
        }
    }


}