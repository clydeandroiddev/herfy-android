package com.herfy.utils.handler

import com.herfy.utils.helper.CacheUtil
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

    @JvmStatic
    fun getErrorCode(o: Any, code : (String) -> Unit){
        var codeValue = ""
        try {
            if(o !is JSONObject){
                return code(codeValue)
            }

            val error = o["error"]
            if (error is JSONObject) {
                val key = if(error.has("code")) error.getString("code") else ""

                if(key.isNotEmpty()){
                    codeValue = CacheUtil.getInstance().getTranslations(key)
                }

            } else if (error is JSONArray) {
                val errorObject = error.getJSONObject(0)
                val key = if(errorObject.has("code")) errorObject.getString("code") else ""
                if(key.isNotEmpty()){
                    codeValue = CacheUtil.getInstance().getTranslations(key)
                }

            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        code(codeValue)
    }

}