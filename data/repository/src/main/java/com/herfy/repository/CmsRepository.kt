package com.herfy.repository

import com.herfy.utils.helper.AppLogger
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CmsApiCall
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage
import org.json.JSONObject
import javax.inject.Inject

interface CmsRepository {

    suspend fun getCatering(result: (String?) -> Unit)
}

class CmsRepositoryImpl @Inject constructor(
) : CmsRepository {

    override suspend fun getCatering(result: (String?) -> Unit) {
        CmsApiCall.getInstance().getPageByLabel("Catering", object : ApiRequestListener {

            override fun onSuccess(obj: Any?) {
                try {
                    val jsonObject = JSONObject(obj.toString())
                    val data = jsonObject.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val cateringPage = CmsPage().parse(data.getJSONObject(0))
                        val content = cateringPage.attributes.url
                        result(content)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(error: Any?) {
                AppLogger.printRetrofitError(error)
                result(null)
            }

        })
    }

}