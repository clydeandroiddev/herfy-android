package com.herfy.utils.helper


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
}