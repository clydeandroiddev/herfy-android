package com.herfy.utils.extensions

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.StringUtil

@BindingAdapter("app:imageUri")
fun loadImageWithUri(imageView: ImageView, imageUri: String?){
    if(!imageUri.isNullOrEmpty()){
        val imageUrl: String = StringUtil.getCloudflareSliderUrl(
            Values.SLIDER_IMAGE_WIDTH,
            Values.SLIDER_IMAGE_HEIGHT,
            imageUri
        )

        Glide.with(imageView.context).load(imageUrl)
            .centerCrop()
            .skipMemoryCache(false)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)

    }

}
