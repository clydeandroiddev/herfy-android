package com.herfy.utils.extensions

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.util.AppLogger

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
            .listener(object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    AppLogger.error(e.toString())
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            })
            .into(imageView)

    }

}

@BindingAdapter("selectedBackground", "unselectedBackground", "isSelected", requireAll = false)
fun View.updateBackgroundWith(selectedBackground : Drawable, unselectedBackground : Drawable? = null, isSelected : Boolean){
    if (isSelected){
        background = selectedBackground
    }else {
        unselectedBackground?.let {
            background = it
        }?: run {
            setBackgroundResource(0)
        }
    }
}

