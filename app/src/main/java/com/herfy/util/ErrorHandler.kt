package com.herfy.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object ErrorHandler {
    @JvmStatic
    fun getErrorMessage(o: Any, defaultErrorMsg: String, forceErrorMessage : String? = null) : String{
        forceErrorMessage?.let {
            return it
        }


        var errorMsg =  defaultErrorMsg
        try {
            if(o !is JSONObject){
                return defaultErrorMsg
            }

            val error = o["error"]
            if (error is JSONObject) {
                val key = if(error.has("code")) "error_code_" + error.getString("code") else ""

                if(key.isNotEmpty()){
                    errorMsg = CacheUtil.getInstance().getTranslations(key)
                }

                if(key.isNotEmpty() && errorMsg.equals(key)){
                    errorMsg = error.getString("detail")
                }
            } else if (error is JSONArray) {
                val errorObject = error.getJSONObject(0)
                val key = if(errorObject.has("code")) "error_code_" + errorObject.getString("code") else ""
                if(key.isNotEmpty()){
                    errorMsg = CacheUtil.getInstance().getTranslations(key)
                }

                if(key.isNotEmpty() && errorMsg.equals(key)){
                    errorMsg = errorObject.getString("detail")
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return errorMsg
    }

}