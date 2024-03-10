package com.herfy.repository

import com.google.gson.JsonObject
import com.herfy.database.AppDatabase
import com.herfy.model.ErrorMsgItem
import com.herfy.model.payment.OTPData
import com.herfy.model.payment.OTPResponse
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.handler.ErrorHandler.getErrorMessage
import com.herfy.utils.helper.ApiLogLevel
import com.herfy.utils.helper.ApiLogUtil
import com.herfy.utils.helper.ApiLogUtil.Companion.sendMessageLog
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.requestbodies.AuthorizeProviderBody
import com.skylinedynamics.solosdk.api.models.requestbodies.RedeemProviderBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

interface PaymentRepository {
    suspend fun sendOTP(number: String, result: (OTPResponse) -> Unit)

    suspend fun verifyOTP(otp: String, amount: String, cartBody : JsonObject,result: (OTPResponse) -> Unit)
}

class PaymentRepositoryImpl @Inject constructor(
    val appDatabase: AppDatabase, val cacheUtilHelper: CacheUtilHelper
) : PaymentRepository {
    override suspend fun sendOTP(number: String, result: (OTPResponse) -> Unit) {
        val customerId = AuthUtil.getInstance().customer.id.toInt()
        val body = AuthorizeProviderBody(customerId, number)
        CustomersApiCall.getInstance()
            .authorizeProvider(body, Values.MUKAFAA_PROVIDER, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        result(OTPResponse(true, null,null))
                        //otpView.setCountdown((3 * 60 * 1000).toLong())
                    } else {
                        result(
                            OTPResponse(
                                false,
                                null,
                                ErrorMsgItem(
                                    true,
                                    CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED),
                                    true
                                )
                            )
                        )

                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    if (o is JSONObject) {
                        try {
                            val `object` = o
                            ApiLogUtil.sendMessageLog(ApiLogLevel.ERROR, `object`.toString())
                            val error = `object`["error"]
                            val messages = `object`["messages"]
                            var detail: String? = getErrorMessage(
                                error, CacheUtil.getInstance().getTranslations(
                                    Text.AN_ERROR_OCCURRED
                                ), null
                            )
                            if (messages is JSONObject) {
                                detail = messages.getString("message")
                                val errorCode = messages.getString("errorCode")
                                if (errorCode == "210") {
                                    result(
                                        OTPResponse(
                                            false,
                                            null,
                                            ErrorMsgItem(
                                                true,
                                                detail,
                                                false
                                            )
                                        )
                                    )
                                    return
                                } else {
                                    result(
                                        OTPResponse(
                                            false,
                                            null,
                                            ErrorMsgItem(
                                                true,
                                                detail,
                                                true
                                            )
                                        )
                                    )
                                }
                            } else {
                                result(
                                    OTPResponse(
                                        false,
                                        null,
                                        ErrorMsgItem(
                                            true,
                                            detail ?: "",
                                            true
                                        )
                                    )
                                )
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            result(
                                OTPResponse(
                                    false,
                                    null,
                                    ErrorMsgItem(
                                        true,
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.AN_ERROR_OCCURRED),
                                        true
                                    )
                                )
                            )
                        }
                    } else {
                        result(
                            OTPResponse(
                                false,
                                null,
                                ErrorMsgItem(
                                    true,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.AN_ERROR_OCCURRED),
                                    true
                                )
                            )
                        )
                    }
                }
            })
    }

    override suspend fun verifyOTP(otp: String, amount: String, cartBody: JsonObject,result: (OTPResponse) -> Unit) {
        var valid = true

        if (otp.isEmpty()) {
            valid = false
            result(
                OTPResponse(
                    false,
                    null,
                    ErrorMsgItem(
                        true,
                        CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED),
                        false
                    )
                )
            )
        }

        if (valid) {
            val customerId = AuthUtil.getInstance().customer.id.toInt()
            val body = RedeemProviderBody(customerId, otp, amount, cartBody)
            CustomersApiCall.getInstance()
                .redeemProvider(body, Values.MUKAFAA_PROVIDER, object : ApiRequestListener {
                    override fun onSuccess(o: Any) {
                        if (o is JSONObject) {
                            try {
                                val transactionId = o.getString("transaction-id")
                                result(
                                    OTPResponse(
                                        true,
                                        OTPData(transactionId, amount),
                                       null
                                    )
                                )
                            } catch (e: JSONException) {
                                onError(e)
                            }
                        } else {
                            result(
                                OTPResponse(
                                    true,
                                    null,
                                    ErrorMsgItem(
                                        true,
                                        "",
                                        true
                                    )
                                )
                            )
                        }
                    }

                    override fun onError(o: Any) {
                        if (o is JSONObject) {
                            try {
                                val obj = o
                                sendMessageLog(ApiLogLevel.ERROR, obj.toString())
                                val error = obj["error"]
                                val messages = obj["messages"]
                                val detail = getErrorMessage(
                                    error, CacheUtil.getInstance().getTranslations(
                                        Text.AN_ERROR_OCCURRED
                                    ), null
                                )
                                if (error is JSONObject) {
                                    val errorCode = error.getString("code")
                                    if (errorCode == "1062") {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                                    false
                                                )
                                            )
                                        )
                                    } else {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    detail,
                                                    true
                                                )
                                            )
                                        )
                                    }
                                } else if (error is JSONArray) {
                                    val errorObject = error.getJSONObject(0)
                                    val errorCode = errorObject.getString("code")
                                    if (errorCode == "1062") {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                                    false
                                                )
                                            )
                                        )
                                    } else {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    detail,
                                                    true
                                                )
                                            )
                                        )
                                    }
                                } else if (messages is JSONObject) {
                                    val jsonMessages = messages
                                    val msg = jsonMessages.getString("message")
                                    val errorCode = jsonMessages.getString("errorCode")
                                    if (errorCode != "301") {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    msg,
                                                    true
                                                )
                                            )
                                        )
                                    } else {
                                        result(
                                            OTPResponse(
                                                true,
                                                null,
                                                ErrorMsgItem(
                                                    true,
                                                    CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                                    false
                                                )
                                            )
                                        )
                                    }
                                } else {
                                    result(
                                        OTPResponse(
                                            true,
                                            null,
                                            ErrorMsgItem(
                                                true,
                                                CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                                false
                                            )
                                        )
                                    )
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                result(
                                    OTPResponse(
                                        true,
                                        null,
                                        ErrorMsgItem(
                                            true,
                                            CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                            false
                                        )
                                    )
                                )
                            }
                        } else {
                            result(
                                OTPResponse(
                                    true,
                                    null,
                                    ErrorMsgItem(
                                        true,
                                        CacheUtil.getInstance().getTranslations(Text.INVALID_OTP),
                                        false
                                    )
                                )
                            )
                        }
                    }
                })
        }
    }


}