package com.herfy.repository

import android.util.Log
import com.google.gson.Gson
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Customer
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginSimpleBody
import com.skylinedynamics.solosdk.util.AppLogger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

interface AuthRepository {

    suspend fun login(phoneNumber: String, result: (String) -> Unit)

}

class AuthRepositoryImpl @Inject constructor(
) : AuthRepository {

    override suspend fun login(phoneNumber: String, result: (String) -> Unit) {
        AuthUtil.getInstance().password = ""
        val loginBody = LoginSimpleBody(StringUtil.toEnglish(phoneNumber.replace("+", "")), "full")
        CustomersApiCall.getInstance().loginCustomer(loginBody, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                if (o is JSONObject) {
                    try {
                        val data = o.getJSONObject("data")
                        val attributes = data.getJSONObject("attributes")
                        val status = attributes.getString("status").lowercase(Locale.getDefault())
                            .trim { it <= ' ' }
                        if (status == Values.STATUS_VERIFIED || status == Values.STATUS_UNVERIFIED) {
                            val customer = Gson().fromJson(
                                data.toString(), Customer::class.java
                            )
                            if (status == Values.STATUS_VERIFIED) {
                                Log.e("==AAASD", "ADADADADShowCode1")
                            } else {
                                if (customer.attributes.mobile != null && StringUtil.isValidPhoneNumber(
                                        customer.attributes.mobile
                                    )
                                ) {
                                    Log.e("==AAASD", "ADADADADShowCode2")
                                } else {
                                    Log.e("==AAASD", "ADADADADShowCode3")
                                }
                            }
                        } else {
                            Log.e("==AAASD", "ADADADADERror1")
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("==AAASD", "ADADADADERror2")
                    }
                } else {
                    Log.e("==AAASD", "ADADADADERror3")
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                if (o is JSONObject) {
                    try {
                        val error = o["error"]
                        var detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                        if (error is JSONObject) {
                            detail = error.getString("detail")
                        } else if (error is JSONArray) {
                            val errorObject = error.getJSONObject(0)
                            detail = errorObject.getString("detail")
                        }
                        Log.e("==AAASD", "ADADADADERrordetail1" + detail)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("==AAASD", "ADADADADERrordetail2")
                    }
                } else {
                    Log.e("==AAASD", "ADADADADERrordetail3")
                }
            }
        })
    }
}