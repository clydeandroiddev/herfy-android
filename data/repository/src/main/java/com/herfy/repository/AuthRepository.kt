package com.herfy.repository

import com.google.gson.Gson
import com.herfy.model.LoginResponse
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Customer
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginSimpleBody
import com.skylinedynamics.solosdk.api.models.requestbodies.ResendSMSBody
import com.skylinedynamics.solosdk.util.AppLogger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

interface AuthRepository {

    suspend fun login(phoneNumber: String, result: (LoginResponse) -> Unit)

}

class AuthRepositoryImpl @Inject constructor(
) : AuthRepository {

    override suspend fun login(phoneNumber: String, result: (LoginResponse) -> Unit) {
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
                                result(LoginResponse(true, true, customer, ""))
                            } else {
                                if (customer.attributes.mobile != null && StringUtil.isValidPhoneNumber(
                                        customer.attributes.mobile
                                    )
                                ) {
                                    sendVerification(customer)
                                }
                                result(LoginResponse(true, true, customer, ""))
                            }
                        } else {
                            result(
                                LoginResponse(
                                    false,
                                    false,
                                    null,
                                    CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR)
                                )
                            )
                        }
                    } catch (e: JSONException) {
                        result(
                            LoginResponse(
                                false,
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR)
                            )
                        )
                    }
                } else {
                    result(
                        LoginResponse(
                            false,
                            false,
                            null,
                            CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR)
                        )
                    )
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
                        result(LoginResponse(false, false, null, detail))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        result(
                            LoginResponse(
                                false,
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR)
                            )
                        )
                    }
                } else {
                    result(
                        LoginResponse(
                            false,
                            false,
                            null,
                            CacheUtil.getInstance().getTranslations(Text.SIGN_IN_ERROR)
                        )
                    )
                }
            }
        })
    }

    private fun sendVerification(customer: Customer) {
        val resendSMSBody = ResendSMSBody(customer.id, customer.attributes.mobile)
        CustomersApiCall.getInstance().resendSMS(resendSMSBody, object : ApiRequestListener {
            override fun onSuccess(o: Any) {}
            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
            }
        })
    }
}