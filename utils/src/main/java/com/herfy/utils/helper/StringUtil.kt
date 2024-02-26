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
    fun fetchEmojiFlag(countryCode: String): String? {
        val code = countryCode.uppercase()
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(code, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(code, 1) - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
    fun localize(number: String): String? {
        return if (LocaleUtil.getInstance().isEnglish) {
            number
        } else {
            number.replace("1", "١").replace("2", "٢").replace("3", "٣").replace("4", "٤")
                .replace("5", "٥").replace("6", "٦").replace("7", "٧").replace("8", "٨")
                .replace("9", "٩").replace("0", "٠").replace(".", ",")
        }
    }
    fun toEnglish(number: String): String? {
        return if (LocaleUtil.getInstance().isEnglish) {
            number
        } else {
            number.replace("١", "1").replace("٢", "2").replace("٣", "3").replace("٤", "4")
                .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8")
                .replace("٩", "9").replace("٠", "0").replace(",", ".")
        }
    }
}