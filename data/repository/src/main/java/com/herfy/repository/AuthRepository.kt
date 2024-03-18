package com.herfy.repository

import android.util.Log
import com.google.gson.Gson
import com.herfy.model.auth.CustomerResponse
import com.herfy.model.auth.GoogleResponse
import com.herfy.model.auth.GuestResponse
import com.herfy.model.auth.LoginResponse
import com.herfy.model.auth.ResendResponse
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.SoloAuth
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.Customer
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody
import com.skylinedynamics.solosdk.api.models.requestbodies.CustomerSMSVerificationBody
import com.skylinedynamics.solosdk.api.models.requestbodies.GuestBodyNew
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginGoogleBody
import com.skylinedynamics.solosdk.api.models.requestbodies.LoginSimpleBody
import com.skylinedynamics.solosdk.api.models.requestbodies.ResendSMSBody
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerMobileBody
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerNameBody
import com.skylinedynamics.solosdk.util.AppLogger
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedList
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

interface AuthRepository {

    suspend fun login(phoneNumber: String, result: (LoginResponse) -> Unit)

    suspend fun guestLogin(phoneNumber: String, result: (GuestResponse) -> Unit)

    suspend fun googleLogin(email: String, token: String, result: (GoogleResponse) -> Unit)

    suspend fun sendVerification(
        customerId: String, customerNumber: String, result: (ResendResponse) -> Unit
    )

    suspend fun verify(
        customerId: String, code: String, result: (CustomerResponse) -> Unit
    )

    suspend fun getCustomer(
        customerId: String, result: (CustomerResponse) -> Unit
    )

    suspend fun updateName(
        customerId: String, name: String, lastName: String, result: (CustomerResponse) -> Unit
    )

    suspend fun updateMobile(
        customerId: String, mobile: String, result: (CustomerResponse) -> Unit
    )
}

class AuthRepositoryImpl @Inject constructor(
) : AuthRepository {

    override suspend fun login(phoneNumber: String, result: (LoginResponse) -> Unit) {
        AuthUtil.getInstance().password = ""
        val phoneNumberTrue = phoneNumber.replace("+", "")
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
                                    verificationSend(
                                        customer.id, phoneNumberTrue
                                    )
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

    override suspend fun guestLogin(phoneNumber: String, result: (GuestResponse) -> Unit) {
        val guestBody = GuestBodyNew(phoneNumber.replace("+", ""))
        CustomersApiCall.getInstance().createGuest(guestBody, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                if (o is JSONObject) {
                    try {
                        val dataObj = o.getJSONObject("data")
                        val customerId = dataObj.getString("id")
                        result(
                            GuestResponse(
                                true, customerId, ""
                            )
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        result(
                            GuestResponse(
                                false,
                                "",
                                CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                            )
                        )
                    }
                } else {
                    result(
                        GuestResponse(
                            false,
                            "",
                            CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                        )
                    )
                }
            }

            override fun onError(o: Any) {
                if (o is JSONObject) {
                    result(
                        GuestResponse(
                            false,
                            "",
                            CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                        )
                    )
                } else {
                    result(
                        GuestResponse(
                            false,
                            "",
                            CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
                        )
                    )
                }
            }
        })
    }

    override suspend fun googleLogin(
        email: String, token: String, result: (GoogleResponse) -> Unit
    ) {
        AuthUtil.getInstance().password = ""
        AuthUtil.getInstance().provider = Values.PROVIDER_GOOGLE
        AuthUtil.getInstance().token = token
        AuthUtil.getInstance().secret = ""
        val loginGoogleBody = LoginGoogleBody(Values.PROVIDER_GOOGLE, token)
        CustomersApiCall.getInstance()
            .loginCustomerThroughGoogle(loginGoogleBody, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val data = o.getJSONObject("data")
                            val attributes = data.getJSONObject("attributes")
                            val status =
                                attributes.getString("status").lowercase(Locale.getDefault())
                                    .trim { it <= ' ' }
                            if (status == Values.STATUS_VERIFIED || status == Values.STATUS_UNVERIFIED) {
                                val customer = GsonHelper.gsonFinal().fromJson(
                                    data.toString(), Customer::class.java
                                )
                                val token = attributes.getString("token")
                                SoloAuth.getInstance().login(token)
                                if (status == Values.STATUS_VERIFIED) {
                                    if (customer.attributes.mobile != null && StringUtil.isValidPhoneNumber(
                                            customer.attributes.mobile
                                        )
                                    ) {
                                        result(GoogleResponse(true, false, false, customer, ""))
                                    } else {
                                        result(GoogleResponse(true, false, true, customer, ""))
                                    }
                                } else {
                                    if (customer.attributes.mobile != null && StringUtil.isValidPhoneNumber(
                                            customer.attributes.mobile
                                        )
                                    ) {
                                        result(GoogleResponse(true, true, false, customer, ""))
                                    } else {
                                        result(GoogleResponse(true, false, true, customer, ""))
                                    }
                                }
                            } else {
                                result(
                                    GoogleResponse(
                                        false,
                                        false,
                                        false,
                                        null,
                                        CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                                    )
                                )
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            result(
                                GoogleResponse(
                                    false,
                                    false,
                                    false,
                                    null,
                                    CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                                )
                            )
                        }
                    } else {
                        result(
                            GoogleResponse(
                                false,
                                false,
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                            )
                        )
                    }
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    result(
                        GoogleResponse(
                            false,
                            false,
                            false,
                            null,
                            CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR)
                        )
                    )
                }
            })
    }

    override suspend fun sendVerification(
        customerId: String, customerNumber: String, result: (ResendResponse) -> Unit
    ) {
        val resendSMSBody = ResendSMSBody(customerId, customerNumber)
        CustomersApiCall.getInstance().resendSMS(resendSMSBody, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                result(ResendResponse(true, ""))
            }

            override fun onError(o: Any) {
                result(ResendResponse(true, ""))
            }
        })
    }

    override suspend fun verify(
        customerId: String, code: String, result: (CustomerResponse) -> Unit
    ) {
        val customerSMSVerificationBody = CustomerSMSVerificationBody(code)
        CustomersApiCall.getInstance()
            .verifyCustomer(customerSMSVerificationBody, customerId, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        try {
                            val data = o.getJSONObject("data")
                            val attributes = data.getJSONObject("attributes")
                            val token = attributes.getString("token")
                            SoloAuth.getInstance().login(token)
                            val customer = Gson().fromJson(
                                data.toString(), Customer::class.java
                            )
                            result(
                                CustomerResponse(
                                    true,
                                    customer,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.VERIFYING_NUMBER_ERROR)
                                )
                            )
                        } catch (ex: JSONException) {
                            ex.printStackTrace()
                        }
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
                            )
                        )
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
                            result(
                                CustomerResponse(
                                    false, null, detail
                                )
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            result(
                                CustomerResponse(
                                    false,
                                    null,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.VERIFYING_NUMBER_ERROR)
                                )
                            )
                        }
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
                            )
                        )
                    }
                }
            })
    }

    override suspend fun getCustomer(
        customerId: String, result: (CustomerResponse) -> Unit
    ) {
        CustomersApiCall.getInstance().getCustomerById(customerId, object : ApiRequestListener {
            override fun onSuccess(o: Any) {
                if (o is JSONObject) {
                    try {
                        val data = o.getJSONObject("data")
                        val customer = Gson().fromJson(
                            data.toString(), Customer::class.java
                        )
                        AuthUtil.getInstance().customer = customer
                        if (CacheUtil.getInstance().orderType === OrderType.DELIVERY && CacheUtil.getInstance().orderDeliveryAddress != null) {
                            val createCustomerAddressBody = CreateCustomerAddressBody(
                                "",
                                CacheUtil.getInstance().orderDeliveryLabel,
                                "",
                                "",
                                "",
                                "",
                                "",
                                CacheUtil.getInstance().orderDeliveryAddress,
                                java.lang.Double.valueOf(CacheUtil.getInstance().orderDeliveryLatitude),
                                java.lang.Double.valueOf(CacheUtil.getInstance().orderDeliveryLongitude),
                                CacheUtil.getInstance().orderDeliveryTelephone,
                                CacheUtil.getInstance().orderDeliveryInstructions,
                                ""
                            )
                            CustomersApiCall.getInstance().addCustomerAddress(
                                createCustomerAddressBody,
                                customer.id,
                                "1",
                                object : ApiRequestListener {
                                    override fun onSuccess(o: Any) {
                                        CustomersApiCall.getInstance()
                                            .getCustomerAddresses(AuthUtil.getInstance().customer.id,
                                                "1",
                                                object : ApiRequestListener {
                                                    override fun onSuccess(o: Any) {
                                                        try {
                                                            val `object` = JSONObject(o.toString())
                                                            val array =
                                                                `object`.getJSONArray("data")
                                                            val addresses = LinkedList<Address>()
                                                            for (i in 0 until array.length()) {
                                                                addresses.add(
                                                                    Address.parseAddress(
                                                                        array.getJSONObject(
                                                                            i
                                                                        )
                                                                    )
                                                                )
                                                            }
                                                            if (addresses.size > 0) {
                                                                val address = addresses[0]
                                                                CacheUtil.getInstance().orderType =
                                                                    OrderType.DELIVERY
                                                                CacheUtil.getInstance().orderAddress =
                                                                    address
                                                                CacheUtil.getInstance().orderDeliveryAddress =
                                                                    null
                                                            }
                                                        } catch (e: JSONException) {
                                                            e.printStackTrace()
                                                        }
                                                    }

                                                    override fun onError(o: Any) {
                                                        AppLogger.printRetrofitError(o)
                                                    }
                                                })
                                    }

                                    override fun onError(o: Any) {
                                        AppLogger.printRetrofitError(o)
                                    }
                                })
                        }
                        var uniqueID = AuthUtil.getInstance().uniqueId
                        if (uniqueID.isEmpty()) {
                            uniqueID = UUID.randomUUID().toString()
                            AuthUtil.getInstance().uniqueId = uniqueID
                        }
                        UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().firebase,
                            uniqueID,
                            customer.attributes.mobile,
                            customer.id,
                            object : ApiRequestListener {
                                override fun onSuccess(`object`: Any) {
                                    try {
                                        val jsonObject = JSONObject(`object`.toString())
                                        val data = jsonObject.getJSONObject("data")
                                        AuthUtil.getInstance().deviceId = data.getString("id")
                                        if (!AuthUtil.getInstance().deviceId.isEmpty()) {
                                            UtilitiesApiCall.getInstance()
                                                .changeLanguage(AuthUtil.getInstance().deviceId,
                                                    LocaleUtil.getInstance().language,
                                                    object : ApiRequestListener {
                                                        override fun onSuccess(obj: Any) {}

                                                        override fun onError(error: Any) {
                                                            AppLogger.printRetrofitError(error)
                                                        }
                                                    })
                                        }
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onError(error: Any) {
                                    AppLogger.printRetrofitError(error)
                                }
                            })
                        result(
                            CustomerResponse(
                                true, customer, ""
                            )
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
                            )
                        )
                    }
                } else {
                    result(
                        CustomerResponse(
                            false,
                            null,
                            CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
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
                        result(
                            CustomerResponse(
                                false, null, detail
                            )
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
                            )
                        )
                    }
                } else {
                    result(
                        CustomerResponse(
                            false,
                            null,
                            CacheUtil.getInstance().getTranslations(Text.VERIFYING_NUMBER_ERROR)
                        )
                    )
                }
            }
        })
    }

    override suspend fun updateName(
        customerId: String, name: String, lastName: String, result: (CustomerResponse) -> Unit
    ) {
        val updateCustomerBody = UpdateCustomerNameBody(name, lastName)
        CustomersApiCall.getInstance()
            .updateCustomerName(updateCustomerBody, customerId, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        val customer = AuthUtil.getInstance().customer
                        CustomersApiCall.getInstance()
                            .getCustomerById(customer.id.toString(), object : ApiRequestListener {
                                override fun onSuccess(o: Any) {
                                    if (o is JSONObject) {
                                        try {
                                            val dataObj = o.getJSONObject("data")
                                            val cust = Gson().fromJson(
                                                dataObj.toString(), Customer::class.java
                                            )
                                            AuthUtil.getInstance().customer = cust
                                            result(CustomerResponse(true, cust, ""))
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            result(
                                                CustomerResponse(
                                                    false,
                                                    null,
                                                    CacheUtil.getInstance()
                                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                                )
                                            )
                                        }
                                    } else {
                                        result(
                                            CustomerResponse(
                                                false,
                                                null,
                                                CacheUtil.getInstance()
                                                    .getTranslations(Text.SAVING_PROFILE_ERROR)
                                            )
                                        )
                                    }
                                }

                                override fun onError(o: Any) {
                                    AppLogger.printRetrofitError(o)
                                    if (o is JSONObject) {
                                        try {
                                            val error = o["error"]
                                            var detail = CacheUtil.getInstance()
                                                .getTranslations(Text.AN_ERROR_OCCURRED)
                                            if (error is JSONObject) {
                                                detail = error.getString("detail")
                                            } else if (error is JSONArray) {
                                                val errorObject = error.getJSONObject(0)
                                                detail = errorObject.getString("detail")
                                            }
                                            result(
                                                CustomerResponse(
                                                    false, null, detail
                                                )
                                            )
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            result(
                                                CustomerResponse(
                                                    false,
                                                    null,
                                                    CacheUtil.getInstance()
                                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                                )
                                            )
                                        }
                                    } else {
                                        result(
                                            CustomerResponse(
                                                false,
                                                null,
                                                CacheUtil.getInstance()
                                                    .getTranslations(Text.SAVING_PROFILE_ERROR)
                                            )
                                        )
                                    }
                                }
                            })
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR)
                            )
                        )
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
                            result(
                                CustomerResponse(
                                    false, null, detail
                                )
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            result(
                                CustomerResponse(
                                    false,
                                    null,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                )
                            )
                        }
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR)
                            )
                        )
                    }
                }
            })
    }

    override suspend fun updateMobile(
        customerId: String, mobile: String, result: (CustomerResponse) -> Unit
    ) {
        val updateCustomerBody = UpdateCustomerMobileBody(
            mobile
        )
        CustomersApiCall.getInstance()
            .updateCustomerMobile(updateCustomerBody, customerId, object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    if (o is JSONObject) {
                        CustomersApiCall.getInstance()
                            .getCustomerById(customerId, object : ApiRequestListener {
                                override fun onSuccess(o: Any) {
                                    if (o is JSONObject) {
                                        try {
                                            val dataObj = o.getJSONObject("data")
                                            val cust = Gson().fromJson(
                                                dataObj.toString(), Customer::class.java
                                            )
                                            AuthUtil.getInstance().customer = cust
                                            result(CustomerResponse(true, cust, ""))
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            result(
                                                CustomerResponse(
                                                    false,
                                                    null,
                                                    CacheUtil.getInstance()
                                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                                )
                                            )
                                        }
                                    } else {
                                        result(
                                            CustomerResponse(
                                                false,
                                                null,
                                                CacheUtil.getInstance()
                                                    .getTranslations(Text.SAVING_PROFILE_ERROR)
                                            )
                                        )
                                    }
                                }

                                override fun onError(o: Any) {
                                    AppLogger.printRetrofitError(o)
                                    if (o is JSONObject) {
                                        try {
                                            val error = o["error"]
                                            var detail = CacheUtil.getInstance()
                                                .getTranslations(Text.AN_ERROR_OCCURRED)
                                            if (error is JSONObject) {
                                                detail = error.getString("detail")
                                            } else if (error is JSONArray) {
                                                val errorObject = error.getJSONObject(0)
                                                detail = errorObject.getString("detail")
                                            }
                                            result(
                                                CustomerResponse(
                                                    false, null, detail
                                                )
                                            )
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            result(
                                                CustomerResponse(
                                                    false,
                                                    null,
                                                    CacheUtil.getInstance()
                                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                                )
                                            )
                                        }
                                    } else {
                                        result(
                                            CustomerResponse(
                                                false,
                                                null,
                                                CacheUtil.getInstance()
                                                    .getTranslations(Text.SAVING_PROFILE_ERROR)
                                            )
                                        )
                                    }
                                }
                            })
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR)
                            )
                        )
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
                            result(
                                CustomerResponse(
                                    false, null, detail
                                )
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            result(
                                CustomerResponse(
                                    false,
                                    null,
                                    CacheUtil.getInstance()
                                        .getTranslations(Text.SAVING_PROFILE_ERROR)
                                )
                            )
                        }
                    } else {
                        result(
                            CustomerResponse(
                                false,
                                null,
                                CacheUtil.getInstance().getTranslations(Text.SAVING_PROFILE_ERROR)
                            )
                        )
                    }
                }
            })
    }

    fun verificationSend(
        customerId: String, customerNumber: String
    ) {
        val resendSMSBody = ResendSMSBody(customerId, customerNumber)
        CustomersApiCall.getInstance().resendSMS(resendSMSBody, object : ApiRequestListener {
            override fun onSuccess(o: Any) {}
            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
            }
        })
    }
}