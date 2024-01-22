package com.herfy.util

import com.skylinedynamics.solosdk.api.models.objects.device.Device
import com.skylinedynamics.solosdk.util.GsonHelper
import org.json.JSONException
import org.json.JSONObject

object DeviceUtil {

    @JvmStatic
    fun save(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val data = jsonObject.getJSONObject("data")
            val device: Device? = GsonHelper.gsonFinal().fromJson(
                data.toString(),
                Device::class.java
            )
            AuthUtil.getInstance().deviceId = device?.id
            AuthUtil.getInstance().device = device?.attributes?.deviceId
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

}