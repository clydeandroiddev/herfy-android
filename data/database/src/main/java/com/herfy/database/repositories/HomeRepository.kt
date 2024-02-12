package com.herfy.database.repositories

import com.google.gson.reflect.TypeToken
import com.herfy.database.AppDatabase
import com.herfy.database.entities.MenuCategoryEntity.Companion.toDomain
import com.herfy.database.entities.MenuItemEntity
import com.herfy.model.HomeResponse
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CacheUtilHelper.Companion.get
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CmsApiCall
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Promotion
import com.skylinedynamics.solosdk.api.models.objects.Slide
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import javax.inject.Inject


interface HomeRepository {
    suspend fun getSliders(response: (HomeResponse) -> Unit)

    suspend fun getGuestDigitalCoupons(orderType: OrderType, response: (HomeResponse) -> Unit)

    suspend fun getSliderId(): String

    suspend fun getGetOrderType(): OrderType

    suspend fun getMenuCategories(response: (HomeResponse) -> Unit)

    suspend fun getMenuItemBy(categoryId : String, response: (HomeResponse) -> Unit)
    fun getOrderStore(): Store?

    suspend fun getMenuItemWith(menuItemId : String, response: (HomeResponse) -> Unit)

    suspend fun getCustomerLoyalty(id: String, response: (HomeResponse) -> Unit)

    suspend fun getLoyaltyTiers(response: (HomeResponse) -> Unit)
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

                    val digitalCoupons : ArrayList<Campaign> = GsonHelper.gsonFinal().fromJson(
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

    override suspend fun getCustomerLoyalty(id: String, response: (HomeResponse) -> Unit) {
        CustomersApiCall.getInstance().getCustomerLoyalty(id,
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val data = `object`.getJSONObject("data")
                        val loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty::class.java)
                        response(HomeResponse(true, loyalty, ""))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError(e)
                    }
                }

                override fun onError(o: Any) {
                    response(HomeResponse(false, null, o.toString()))
                }
            })
    }

    override suspend fun getLoyaltyTiers(response: (HomeResponse) -> Unit) {
        CustomersApiCall.getInstance().getLoyaltyTiers(object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                try {
                    val `object` = JSONObject(o.toString())
                    val tiersArray = `object`.getJSONArray("data")
                    val type = object :
                        TypeToken<List<Tier?>?>() {}.type
                    val tiers = GsonHelper.gsonFinal()
                        .fromJson<List<Tier>>(tiersArray.toString(), type)
                    // TODO: Should set Tier as Comparable in SDK
                    Collections.sort(tiers,
                        java.util.Comparator { o1: Tier, o2: Tier -> o1.startPoint - o2.startPoint } as java.util.Comparator<Tier>)
                    response(HomeResponse(true, tiers, ""))
                } catch (ex: JSONException) {
                    ex.printStackTrace()
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

    override suspend fun getMenuCategories(response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuCategoryDbs = appDatabase.menuCategoryEntityDao().getAllByApplication(application.id)
            val menuCategoryList = arrayListOf<MenuCategory>()
            menuCategoryDbs.forEach {
                menuCategoryList.add(it.toDomain())
            }
            response(HomeResponse(true, menuCategoryList, ""))
        }
    }

    override suspend fun getMenuItemBy(categoryId: String, response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuItemDb = appDatabase.menuItemEntityDao().findAllByMenuCategoryId(application.id, categoryId)
            response(HomeResponse(true, menuItemDb, ""))
        }
    }

    override fun getOrderStore(): Store? {
        return cacheUtilHelper.getOrderStore()
    }

    override suspend fun getMenuItemWith(menuItemId: String, response: (HomeResponse) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuItemEntity : MenuItemEntity? = appDatabase.menuItemEntityDao().findById(application.id, menuItemId)
            response(HomeResponse(true, menuItemEntity, ""))
        }
    }


}