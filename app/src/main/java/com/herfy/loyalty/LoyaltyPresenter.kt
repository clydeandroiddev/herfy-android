package com.herfy.loyalty

import android.annotation.SuppressLint
import android.util.Pair
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.herfy.enum.HistorySortType
import com.herfy.loyalty.objects.DateItem
import com.herfy.loyalty.objects.ListItem
import com.herfy.loyalty.objects.LoyaltyTransactionItem
import com.herfy.model.SortingCategory
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.handler.ErrorHandler.getErrorMessage
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.TimeUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Customer
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransactionDetails
import com.skylinedynamics.solosdk.api.models.objects.QrData
import com.skylinedynamics.solosdk.api.models.objects.Tier
import com.skylinedynamics.solosdk.api.models.requestbodies.PosPointsRedemptionBody
import com.skylinedynamics.solosdk.util.GsonHelper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@SuppressLint("CheckResult")
class LoyaltyPresenter @Inject constructor(
    private val loyaltyView: LoyaltyContract.View,
    private val cacheUtil: CacheUtilHelper
) : LoyaltyContract.Presenter {
    var filteredTransactionList = ArrayList<LoyaltyTransaction>()
    var transactionsList = ArrayList<LoyaltyTransaction>()
    var tiers: List<Tier> = ArrayList()
    var sortingCategories = arrayListOf<SortingCategory>()
    private var newestToOldest = true
    private val disposables = CompositeDisposable()
    var consolidatedList = ArrayList<ListItem>()

    init {
        loyaltyView.setPresenter(this)
    }

    override fun start() {
        loyaltyView.setupViews()
        loyaltyView.setupFonts()
        loyaltyView.setupTranslations()
    }

    override fun selectCategory(category: SortingCategory) {
        if(!category.isSelected){
            category.isSelected = true
            loyaltyView.updateSortingCategory(category)
            filterTransactions(category)
        }
    }

    private fun filterTransactions(category: SortingCategory){
        if(category.sortType == HistorySortType.ALL){
            filteredTransactionList.clear()
            filteredTransactionList.addAll(transactionsList)
        }else {
            val selectedCategory = when(category.sortType) {
                HistorySortType.EARNED -> {
                    "collection"
                }
                HistorySortType.USED -> {
                    "redeemed"
                }
                HistorySortType.ALL -> {
                    ""
                }
            }
            filteredTransactionList.addAll(
                transactionsList.filter {
                        trans -> trans.type.equals(selectedCategory, ignoreCase = true)
                }
            )
        }
        consolidatedList.clear()
        val groupedHashMap = groupDataIntoHashMap(filteredTransactionList)
        val keys: List<String> = ArrayList(groupedHashMap.keys)
        Collections.sort(keys) { lhs: String?, rhs: String? ->
            try {
                val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                val lhsDate = simpleDateFormat.parse(lhs)
                val rhsDate = simpleDateFormat.parse(rhs)
                return@sort rhsDate.compareTo(lhsDate)

            } catch (e: ParseException) {
                e.printStackTrace()
            }
            0
        }
        val sortedHashMap: HashMap<String, List<LoyaltyTransaction>> = LinkedHashMap()
        for (key in keys) {
            sortedHashMap[key] = groupedHashMap[key]!!
        }
        for (date in sortedHashMap.keys) {
            val dateItem = DateItem()
            dateItem.date = date
            consolidatedList.add(dateItem)
            for (loyaltyTransaction in sortedHashMap[date]!!) {
                val loyaltyTransactionItem = LoyaltyTransactionItem()
                loyaltyTransactionItem.loyaltyTransaction = loyaltyTransaction
                consolidatedList.add(loyaltyTransactionItem)
            }
        }
        loyaltyView.updateTransactions()
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
                    if (o is Throwable) {
                        source.onError(o)
                    } else {
                        source.onError(
                            Throwable(
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
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

    override fun cleanup() {
        disposables.clear()
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
                }) { error: Throwable? ->
                val loyalty = Loyalty()
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
                loyalty.attributes = loyaltyAttributes
                loyaltyView.showLoyalty(
                    loyalty,
                    AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                )
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

    override fun getCustomerDetails() {
        val customer = AuthUtil.getInstance().customer
        CustomersApiCall.getInstance()
            .getCustomerById(customer.id.toString(), object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val dataObj = o.getJSONObject("data")
                            val customer = GsonHelper.gsonFinal()
                                .fromJson(dataObj.toString(), Customer::class.java)
                            if (customer.attributes.lastOrderId == CacheUtil.getInstance().lastOrderId) {
                                getLoyalty()
                            } else {
                                fetchAndShowNextTier()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    getLoyalty()
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

    override fun getLoyaltyHistoryTransaction() {
        CustomersApiCall.getInstance().getCustomerLoyaltyTransactions(
            AuthUtil.getInstance().customer.id,
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    try {
                        val `object` = JSONObject(o.toString())
                        val data = `object`.getJSONObject("data")
                        val attributes = data.getJSONObject("attributes")
                        val tier = attributes["tier"]
                        if (tier !is JSONObject) {
                            attributes.put("tier", LoyaltyTier().toJSON())
                            data.put("attributes", attributes)
                        }
                        val loyalty =
                            GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty::class.java)
                        loyaltyView.showLoyalty(
                            loyalty,
                            AuthUtil.getInstance().customer.attributes.firstName + " " + AuthUtil.getInstance().customer.attributes.lastName
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError(e)
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    loyaltyView.showLoyaltyError(
                        getErrorMessage(
                            o, CacheUtil.getInstance().getTranslations(
                                Text.AN_ERROR_OCCURRED
                            ), null
                        )
                    )
                }
            })
    }

    override fun getConsolidatedList(): ArrayList<ListItem> {
        return consolidatedList
    }

    override fun getLoyaltyHistoryTransaction2(page: Int) {
        getLoyaltyHistory(page).subscribeOn(Schedulers.io())
            .doOnSubscribe { disposable: Disposable? ->
                disposables.add(
                    disposable!!
                )
            }.subscribe(
                { transactionsList: List<LoyaltyTransaction>? ->
                    if (page == 1) {
                        this.transactionsList.clear()
                        consolidatedList.clear()
                    }
                    this.transactionsList.addAll(transactionsList!!)
                    sortTransactionList()
                }) { error: Throwable? ->
                loyaltyView.showLoyaltyError(
                    CacheUtil.getInstance().getTranslations(
                        Text.AN_ERROR_OCCURRED
                    )
                )
            }
    }

    fun getLoyaltyHistory(page: Int): Single<List<LoyaltyTransaction>> {
        return Single.create { source: SingleEmitter<List<LoyaltyTransaction>> ->
            CustomersApiCall.getInstance().getLoyaltyTransactions(
                AuthUtil.getInstance().customer.id,
                page,
                object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        try {
                            val `object` = JSONObject(o.toString())
                            val array = `object`.getJSONArray("data")
                            val list: MutableList<LoyaltyTransaction> = ArrayList()
                            for (i in 0 until array.length()) {
                                val object1 = array.getJSONObject(i)
                                val details = object1.opt("details")
                                if (!object1.has("details") || object1.isNull("details") || details !is JSONObject) {
                                    val loyaltyTransactionDetails = LoyaltyTransactionDetails()
                                    loyaltyTransactionDetails.amount = 0.0
                                    object1.put(
                                        "details",
                                        JSONObject(Gson().toJson(loyaltyTransactionDetails))
                                    )
                                }
                                val transaction = GsonHelper.gsonFinal()
                                    .fromJson(object1.toString(), LoyaltyTransaction::class.java)
                                list.add(transaction)
                            }
                            val filteredList: MutableList<LoyaltyTransaction> = ArrayList()
                            for (loyaltyTransaction in list) {
                                if (loyaltyTransaction.details != null && loyaltyTransaction.details.amount != null) {
                                    filteredList.add(loyaltyTransaction)
                                }
                            }
                            source.onSuccess(filteredList)
                            loyaltyView.showLoyaltyPointsHistory(newestToOldest, transactionsList)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onError(e)
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o)
                        if (o is JSONException) {
                            source.onError((o as Throwable))
                        } else if (o is JSONObject) {
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
                                source.onError(e)
                            }
                        } else {
                            source.onError(Throwable("API Error"))
                        }
                    }
                })
        }
    }

    override fun onBindLoyaltyHistoryViewAtPosition(
        position: Int,
        loyaltyPointsViewHolder: LoyaltyPointsViewHolder
    ) {
        val item = consolidatedList[position] as LoyaltyTransactionItem
        loyaltyPointsViewHolder.setLoyaltyHistory(item.loyaltyTransaction)
    }

    override fun getLoyaltyPointsHistoryCount(): Int {
        return transactionsList.size
    }

    private fun dummyResponse(): JsonObject {
        val json =
            """{
  "type" : "transactions",
  "data" : [
    {
      "status" : "successful",
      "points" : 49,
      "details" : [

      ],
      "id" : "9482e1b7-fb8f-463c-b7b6-f580469ae273",
      "expires_at" : "2021-10-29 06:55:35",
      "created-at" : "2021-09-29 06:55:35",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-09-29 06:55:35"
    },
    {
      "status" : "successful",
      "points" : 1,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 17.390000000000001,
        "order-id" : 1190911
      },
      "id" : "948f38fe-338c-41f2-b8c7-5dc40b842d6d",
      "expires_at" : "2021-11-04 10:09:34",
      "created-at" : "2021-10-05 10:09:34",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 10:09:34"
    },
    {
      "status" : "successful",
      "points" : 2,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 27.829999999999998,
        "order-id" : 1190912
      },
      "id" : "948f3d30-f926-497b-964f-24f2ee13ce60",
      "expires_at" : "2021-11-04 10:21:18",
      "created-at" : "2021-10-05 10:21:18",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 10:21:18"
    },
    {
      "status" : "successful",
      "points" : 10,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 103.48,
        "order-id" : 1190913
      },
      "id" : "948f3ea2-82c7-48cc-9646-92817668794e",
      "expires_at" : "2021-11-04 10:25:20",
      "created-at" : "2021-10-05 10:25:20",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 10:25:20"
    },
    {
      "status" : "successful",
      "points" : 29,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 295.64999999999998,
        "order-id" : 1190914
      },
      "id" : "948f4001-0947-47b4-ae2a-d7cf9d68bcc2",
      "expires_at" : "2021-11-04 10:29:10",
      "created-at" : "2021-10-05 10:29:10",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 10:29:10"
    },
    {
      "status" : "successful",
      "points" : 11,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 113.04000000000001,
        "order-id" : 1190915
      },
      "id" : "948f40d5-74f3-49bd-9f98-cbd8cd4c82fb",
      "expires_at" : "2021-11-04 10:31:29",
      "created-at" : "2021-10-05 10:31:29",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 10:31:29"
    },
    {
      "status" : "successful",
      "points" : 0,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 0.20000000000000001,
        "order-id" : 1190930
      },
      "id" : "948fbf97-8fe8-4cb5-be51-099e6545e0c8",
      "created-at" : "2021-10-05 16:25:56",
      "type" : "redemption",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 16:25:56"
    },
    {
      "status" : "successful",
      "points" : 10,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 103.48,
        "order-id" : 1190930
      },
      "id" : "948fc05e-cd16-472e-b750-c0d5d177a3af",
      "expires_at" : "2021-11-04 16:28:06",
      "created-at" : "2021-10-05 16:28:06",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-05 16:28:06"
    },
    {
      "status" : "successful",
      "points" : 869,
      "details" : {
        "date" : "2021-10-11",
        "amount" : 8695.6399999999994,
        "order-id" : 1191018
      },
      "id" : "949bc203-3bab-4a26-9cb5-bebf447cf8c4",
      "expires_at" : "2021-11-10 15:42:38",
      "created-at" : "2021-10-11 15:42:38",
      "type" : "collection",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-11 15:42:38"
    },
    {
      "status" : "successful",
      "points" : 0,
      "details" : {
        "date" : "2021-10-13",
        "amount" : 2,
        "order-id" : 1191058
      },
      "id" : "949fb81c-695f-4554-8d74-a994887b9610",
      "created-at" : "2021-10-13 14:58:15",
      "type" : "redemption",
      "customer-id" : "9482e1aa-043a-4550-9264-248c3237437a",
      "updated-at" : "2021-10-13 14:58:15"
    }
  ],
  "meta" : {
    "total-pages" : 16,
    "count" : 10,
    "current-page" : 1,
    "total" : 154,
    "per-page" : 10
  }
}
"""
        return GsonHelper.gsonFinal().fromJson(json, JsonObject::class.java)
    }

    override fun getTiers(): List<Tier> {
        return tiers
    }

    override fun sortHistory() {
        newestToOldest = !newestToOldest
        sortTransactionList()
        loyaltyView.showLoyaltyPointsHistory(newestToOldest, transactionsList)
    }

    private fun sortTransactionList() {
        consolidatedList.clear()
        val groupedHashMap = groupDataIntoHashMap(transactionsList)
        val keys: List<String> = ArrayList(groupedHashMap.keys)
        Collections.sort(keys) { lhs: String?, rhs: String? ->
            try {
                val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                val lhsDate = simpleDateFormat.parse(lhs)
                val rhsDate = simpleDateFormat.parse(rhs)
                if (newestToOldest) {
                    return@sort rhsDate.compareTo(lhsDate)
                } else {
                    return@sort lhsDate.compareTo(rhsDate)
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            0
        }
        val sortedHashMap: HashMap<String, List<LoyaltyTransaction>> = LinkedHashMap()
        for (key in keys) {
            sortedHashMap[key] = groupedHashMap[key]!!
        }
        for (date in sortedHashMap.keys) {
            val dateItem = DateItem()
            dateItem.date = date
            consolidatedList.add(dateItem)
            for (loyaltyTransaction in sortedHashMap[date]!!) {
                val loyaltyTransactionItem = LoyaltyTransactionItem()
                loyaltyTransactionItem.loyaltyTransaction = loyaltyTransaction
                consolidatedList.add(loyaltyTransactionItem)
            }
        }
    }

    private fun groupDataIntoHashMap(loyaltyTransactions: List<LoyaltyTransaction>): HashMap<String, MutableList<LoyaltyTransaction>> {
        val groupedHashMap = HashMap<String, MutableList<LoyaltyTransaction>>()
        for (loyaltyTransaction in loyaltyTransactions) {
            val hashMapKey = TimeUtil.formatDate(loyaltyTransaction.details.date, "yyyy-MM-dd")
            if (groupedHashMap.containsKey(hashMapKey)) {
                groupedHashMap[hashMapKey]!!.add(loyaltyTransaction)
            } else {
                val list: MutableList<LoyaltyTransaction> = ArrayList()
                list.add(loyaltyTransaction)
                groupedHashMap[hashMapKey] = list
            }
        }
        return groupedHashMap
    }

    override fun getSortingCategories() {
        sortingCategories.clear()
        sortingCategories.add(
            SortingCategory(
                cacheUtil.getTranslations(Text.ALL_LABEL, "All"),
                true,
                HistorySortType.ALL
            )
        )
        sortingCategories.add(
            SortingCategory(
                cacheUtil.getTranslations(Text.EARNED_LABEL, "Earned"),
                false,
                HistorySortType.EARNED
            )
        )
        sortingCategories.add(
            SortingCategory(
                cacheUtil.getTranslations(Text.USED_LABEL, "Used"),
                false,
                HistorySortType.USED
            )
        )
        loyaltyView.showSortingCategories(sortingCategories)
    }
}
