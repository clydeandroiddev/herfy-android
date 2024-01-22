package com.herfy.util.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

fun ImageView.loadImageFromUrl(imageUrl:String) {
    Glide.with(this)
        .load(imageUrl)
        .apply(RequestOptions.centerInsideTransform())
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(this)
}