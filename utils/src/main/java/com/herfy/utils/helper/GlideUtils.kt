package com.herfy.utils.helper

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

// Create this use either in java code or kotlin
object GlideUtils {

    fun loadImageUrl(image:ImageView, url:String,context:Context) {
        Glide.with(context).load(url)
            .skipMemoryCache(false).diskCacheStrategy(
                DiskCacheStrategy.ALL
            ).apply(RequestOptions.centerCropTransform()).into(image)
    }
}