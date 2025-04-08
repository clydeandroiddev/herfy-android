package com.herfy.util.hmac

import com.herfy.BuildConfig
import com.herfy.utils.helper.CacheUtil
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class HmacHeaderInterceptor : Interceptor {

    companion object {
        private const val NO_HMAC = "noHmac"
        const val HEADER_SIGNATURE = "X-FO-Signature"
    }

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {

        val original = chain.request()

        val noHeaders = original.headers[NO_HMAC] == "true"

        val requestBuilder = request().newBuilder()
            .removeHeader(NO_HMAC)

        if(!noHeaders && isEndpointHmacProtected(original)) {

            var timestamp = ""
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(sdf.format(Date()))
                if (date != null) {
                    timestamp = (date.time / 1000L).toString()
                }
            } catch (e: ParseException) {
                throw RuntimeException(e)
            }

            val requestBody = bodyToString(request().body)

            val appKey = BuildConfig.SOLO_APP
            val hmac = HmacUtil.encrypt(requestBody)
            val signature = "$timestamp|$appKey|$hmac"

            requestBuilder.addHeader(
                HEADER_SIGNATURE,
                signature.trim().replace(" ", "")
            )

        }

        proceed(requestBuilder.build())

    }

    private fun isEndpointHmacProtected(request: Request): Boolean {
        var isProtected = false
        val method = request.method
        val path = request.url.toUrl().path
        val application = CacheUtil.getInstance().application
        val hmacEndpoints = application?.attributes?.hmacEndpoints ?: emptyList()
        for(endpoint in hmacEndpoints) {
            isProtected = method.equals(endpoint.method, ignoreCase = true) && path.equals(endpoint.path, ignoreCase = true)
            if(isProtected) {
                break
            }
        }
        return isProtected
    }

    private fun bodyToString(request: RequestBody?): String {
        try {
            val copy: RequestBody? = request
            val buffer = Buffer()
            if (copy != null) copy.writeTo(buffer)
            else return ""
            return buffer.readUtf8()
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

}