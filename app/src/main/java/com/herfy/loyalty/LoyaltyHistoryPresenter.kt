package com.herfy.loyalty

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.herfy.enum.HistorySortType
import com.herfy.loyalty.objects.ListItem
import com.herfy.model.SortingCategory
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransactionDetails
import com.skylinedynamics.solosdk.util.GsonHelper
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class LoyaltyHistoryPresenter @Inject constructor(
    private val historyView: LoyaltyHistoryContract.View,
    private val cacheUtilHelper: CacheUtilHelper
) : LoyaltyHistoryContract.Presenter {

    private var filteredTransactionList = ArrayList<LoyaltyTransaction>()
    private var transactionsList = ArrayList<LoyaltyTransaction>()
    private var consolidatedList = ArrayList<ListItem>()
    private var sortingCategories = arrayListOf<SortingCategory>()
    private val disposables = CompositeDisposable()
    init {
        historyView.setPresenter(this)
    }
    @SuppressLint("CheckResult")
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
                historyView.showLoyaltyError(
                    CacheUtil.getInstance().getTranslations(
                        Text.AN_ERROR_OCCURRED
                    )
                )
            }
    }

    private fun sortTransactionList() {
        if(transactionsList.size > 0){
            transactionsList.sortWith { lhs: LoyaltyTransaction, rhs: LoyaltyTransaction ->
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                val lhsDate = simpleDateFormat.parse(lhs.createdAt)
                val rhsDate = simpleDateFormat.parse(rhs.createdAt)
                return@sortWith rhsDate.compareTo(lhsDate)
            }
            filteredTransactionList.clear()
            filteredTransactionList.addAll(transactionsList)
        }
    }

    override fun cleanup() {
        disposables.clear()
    }

    private fun getLoyaltyHistory(page: Int): Single<List<LoyaltyTransaction>> {
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
                            historyView.showLoyaltyPointsHistory(filteredTransactionList)
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
                                historyView.showLoyaltyError(detail)
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

    override fun getLoyaltyPointsHistoryCount(): Int {
        return transactionsList.size
    }
    override fun onBindLoyaltyHistoryViewAtPosition(
        position: Int,
        loyaltyPointsViewHolder: LoyaltyPointsViewHolder
    ) {
        val item = filteredTransactionList[position]
        loyaltyPointsViewHolder.setLoyaltyHistory(item)
    }

    override fun transactionListCount(): Int = filteredTransactionList.size


    override fun start() {
        historyView.setupViews()
        historyView.setupFonts()
        historyView.setupTranslations()
    }

    override fun getSortingCategories() {
        sortingCategories.clear()
        sortingCategories.add(
            SortingCategory(
                cacheUtilHelper.getTranslations(Text.ALL_LABEL, "All"),
                true,
                HistorySortType.ALL
            )
        )
        sortingCategories.add(
            SortingCategory(
                cacheUtilHelper.getTranslations(Text.EARNED_LABEL, "Earned"),
                false,
                HistorySortType.EARNED
            )
        )
        sortingCategories.add(
            SortingCategory(
                cacheUtilHelper.getTranslations(Text.USED_LABEL, "Used"),
                false,
                HistorySortType.USED
            )
        )
        historyView.showSortingCategories(sortingCategories)
    }

    override fun selectCategory(category: SortingCategory) {
        if(!category.isSelected){
            category.isSelected = true
            historyView.updateSortingCategory(category)
            filterTransactions(category)
        }
    }

    private fun filterTransactions(category: SortingCategory){
        filteredTransactionList.clear()
        if(transactionsList.size > 0){
            if(category.sortType == HistorySortType.ALL){
                filteredTransactionList.addAll(transactionsList)
            }else {
                val selectedCategory = when(category.sortType) {
                    HistorySortType.EARNED -> {
                        "collection"
                    }
                    HistorySortType.USED -> {
                        "redemption"
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
        }
        historyView.updateTransactions()
    }


    private fun dummyResponse(): JsonObject {
        val json =
            """{
  "type" : "transactions",
  "data" : [
    {
      "status" : "successful",
      "points" : 49,
      "details" : {
        "date" : "2021-10-05",
        "amount" : 17.390000000000001,
        "order-id" : 1190911
      },
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

}