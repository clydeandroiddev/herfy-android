package com.herfy.utils.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

fun ImageView.loadImageFromUrl(imageUrl:String,
                               isSkipMemoryCache : Boolean,
                               strategy: DiskCacheStrategy = DiskCacheStrategy.ALL,
                               requestOptions: RequestOptions = RequestOptions.noTransformation()) {
    Glide.with(this)
        .load(imageUrl)
        .skipMemoryCache(isSkipMemoryCache)
        .apply(requestOptions)
        .diskCacheStrategy(strategy)
        .into(this)
}