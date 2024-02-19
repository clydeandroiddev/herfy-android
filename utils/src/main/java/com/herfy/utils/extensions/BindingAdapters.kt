package com.herfy.utils.extensions

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

@BindingAdapter("app:sliderImageUri")
fun loadSliderImage(imageView: ImageView, sliderImageUri: String?){
    if(!sliderImageUri.isNullOrEmpty()){
        val imageUrl: String = StringUtil.getCloudflareSliderUrl(
            Values.SLIDER_IMAGE_WIDTH,
            Values.SLIDER_IMAGE_HEIGHT,
            sliderImageUri
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
@BindingAdapter("app:imageSrc")
fun loadImage(imageView: ImageView, imageSrc: String?){
    val imageURL : String =  if(imageSrc.isNullOrEmpty()) Values.DEFAULT_IMAGE_URL else imageSrc
    Glide.with(imageView.context).load(imageURL)
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

@BindingAdapter("app:selectedBackground", "app:unselectedBackground", "app:isSelected", requireAll = false)
fun updateBackgroundWith(view : View, selectedBackground : Drawable?, unselectedBackground : Drawable? = null, isSelected : Boolean){
    if (isSelected){
        selectedBackground?.let {
            view.background = it
        }
    }else {
        unselectedBackground?.let {
            view.background = it
        }?: run {
            view.setBackgroundResource(0)
        }
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("app:countValue")
fun TextView.setCount(value : Int){
    text = "+${value -1}"
}

@BindingAdapter("app:icon")
fun ImageView.setIconWith(resource : Int){
    if(resource != -1){
        visibility = View.VISIBLE
        setImageResource(resource)
    }else {
        visibility = View.GONE
    }

}

