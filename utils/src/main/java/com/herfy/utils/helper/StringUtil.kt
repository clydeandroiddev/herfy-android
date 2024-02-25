package com.herfy.utils.helper


import android.content.Context
import com.herfy.utils.R
import com.herfy.utils.constants.Values


object StringUtil {
    fun getCloudinaryUrl(width: Int, height: Int, imageUrl: String): String {
        //return imageUrl;
        return Values.CLOUDINARY_IMAGE_URL.replace("(w)", width.toString())
            .replace("(h)", height.toString()) + imageUrl
    }

    fun getCloudflareUrl(width: Int, height: Int, imageUrl: String): String {
        if (imageUrl.contains(Values.CDN_HOST)) {
            val index = imageUrl.lastIndexOf("/")
            val start = imageUrl.substring(0, index)
            val end = imageUrl.substring(index)
            return start + Values.CLOUDFLARE_IMAGE_URL.replace("(w)", width.toString())
                .replace("(h)", height.toString()) + end
        }
        return imageUrl
    }

    fun getCloudflareSliderUrl(width: Int, height: Int, imageUrl: String): String {
        if (imageUrl.contains(Values.CDN_HOST)) {
            val index = imageUrl.lastIndexOf("/")
            val start = imageUrl.substring(0, index)
            val end = imageUrl.substring(index)
            return start + Values.CLOUDFLARE_SLIDER_URL.replace("(w)", width.toString())
                .replace("(h)", height.toString()) + end
        }
        return imageUrl
    }

    fun getAppName(context: Context): String? {
        val appName = context.resources.getString(R.string.app_name)
        return if (CacheUtil.getInstance().country != null) {
            CacheUtil.getInstance().country.applicationName
        } else {
            appName
        }
    }

    @JvmStatic
    fun localize(number: String): String {
        return if (LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench) {
            number
        } else {
            number
                .replace("١", "1").replace("٢", "2")
                .replace("٣", "3").replace("٤", "4")
                .replace("٥", "5").replace("٦", "6")
                .replace("٧", "7").replace("٨", "8")
                .replace("٩", "9").replace("٠", "0")
                .replace(",", ".").replace("٫", ".")
        }
    }
}