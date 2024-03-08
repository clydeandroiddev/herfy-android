package com.herfy.loyalty

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.herfy.enum.HistorySortType
import com.herfy.loyalty.objects.DateItem
import com.herfy.loyalty.objects.ListItem
import com.herfy.loyalty.objects.LoyaltyTransactionItem
import com.herfy.model.SortingCategory
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.TimeUtil
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
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
        consolidatedList.clear()
        val groupedHashMap = groupDataIntoHashMap(transactionsList)
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
    }

    override fun cleanup() {
        disposables.clear()
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
                            historyView.showLoyaltyPointsHistory(transactionsList)
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
        val item = consolidatedList[position] as LoyaltyTransactionItem
        loyaltyPointsViewHolder.setLoyaltyHistory(item.loyaltyTransaction)
    }

    override fun getConsolidatedList(): ArrayList<ListItem> {
       return consolidatedList
    }

    override fun start() {
        historyView.setupViews()
        historyView.setupFonts()
        historyView.setupTranslations()
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
        historyView.updateTransactions()
    }


}