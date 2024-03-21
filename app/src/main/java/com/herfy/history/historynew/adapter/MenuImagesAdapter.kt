package com.herfy.history.historynew.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.history.databinding.ItemMenuImageBinding
import com.herfy.utils.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import java.util.Locale

class MenuImagesAdapter(
    val items: List<String> = arrayListOf(),
) : RecyclerView.Adapter<MenuImagesAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemMenuImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemMenuImageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.history.R.layout.item_menu_image,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            executePendingBindings()
            var imageUrl: String = item
            if (imageUrl.equals(
                    "null", ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
            }
            Glide.with(itemImage.context).load(imageUrl).apply(RequestOptions().override(80, 80))
                .skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.centerInsideTransform())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }).into(itemImage)
        }
    }
}