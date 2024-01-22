package com.herfy.util

import com.google.gson.JsonObject
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.LogsApiCall

enum class ApiLogLevel {
    INFO,
    WARNING,
    ERROR
}

class ApiLogUtil {

    companion object {

        private fun getDefaultBody(level: ApiLogLevel): JsonObject {
            val body = JsonObject()
            body.addProperty("level", level.name.lowercase())
            if(AuthUtil.getInstance().isSignedIn && !AuthUtil.getInstance().customer.id.isNullOrEmpty()) {
                body.addProperty("customer-id", AuthUtil.getInstance().customer.id.toLongOrNull())
            }
            if(!AuthUtil.getInstance().device.isNullOrEmpty()) {
                body.addProperty("device-id", AuthUtil.getInstance().device)
            }
            return body
        }

        @JvmStatic
        fun sendMessageLog(level: ApiLogLevel, message: String) {
            val body = getDefaultBody(level)
            body.addProperty("message", message)
            sendLog(body)
        }

        @JvmStatic
        fun sendOrderLog(level: ApiLogLevel, orderId: String?, message: String, cause: String) {
            orderId?.let {
                val body = getDefaultBody(level)
                body.addProperty("order-id", it.toLongOrNull())
                body.addProperty("message", message)
                if(level == ApiLogLevel.ERROR) {
                    body.addProperty("cause", message)
                }
                sendLog(body)
            }
        }

        private fun sendLog(body: JsonObject) {
            LogsApiCall.getInstance().sendLog(body, object : ApiRequestListener {
                override fun onSuccess(obj: Any?) {

                }

                override fun onError(error: Any?) {
                    AppLogger.printRetrofitError(error)
                }

            })
        }

    }

}