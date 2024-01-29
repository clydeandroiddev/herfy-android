package com.herfy.util

import com.google.gson.GsonBuilder
import com.herfy.data.constants.Text
import org.json.JSONException
import retrofit2.Response

fun Response<*>.getErrorMessage(
    defaultError: String = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)
): String {
    return parseErrJsonResponse<SoloError>()?.detail
        ?: parseErrJsonResponse<List<SoloError>>()?.get(0)?.detail ?: defaultError
}

inline fun <reified T> Response<*>.parseErrJsonResponse(): T? {

    val response = errorBody()?.string()
    val gson = GsonBuilder().create()
    return try {
        val errorObject = gson.fromJson(response, T::class.java)
        errorObject
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

data class SoloError(
    val detail: String
)