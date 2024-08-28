package com.herfy.utils.extensions

import android.app.Activity
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.herfy.utils.R

fun ImageView.loadImageFromUrl(imageUrl:String,
                               isSkipMemoryCache : Boolean = false,
                               strategy: DiskCacheStrategy = DiskCacheStrategy.ALL,
                               requestOptions: RequestOptions = RequestOptions.noTransformation(),
                               placeholder: Int = R.drawable.imagepicker_image_placeholder,
                               listener: RequestListener<Drawable>? = null
) {
    var isContextDestroyed = false
    this.context.cast<Activity> {
        isContextDestroyed = !it.isDestroyed
    }
    if(!isContextDestroyed) {
        try {
            Glide.with(this.context)
                .load(imageUrl)
                .listener(listener)
                .skipMemoryCache(isSkipMemoryCache)
                .apply(requestOptions)
                .diskCacheStrategy(strategy)
                .placeholder(placeholder)
                .into(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
