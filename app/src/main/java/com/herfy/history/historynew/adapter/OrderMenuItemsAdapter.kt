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
import com.google.gson.Gson
import com.herfy.history.databinding.ItemOrderMenuBinding
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.Translated
import io.shipbook.shipbooksdk.Log
import java.util.Locale

class OrderMenuItemsAdapter(
    val menuItems: ArrayList<MenuItem> = arrayListOf()
) : RecyclerView.Adapter<OrderMenuItemsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemOrderMenuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemOrderMenuBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.history.R.layout.item_order_menu,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = menuItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.binding.apply {
            itemName.text = menuItem.attributes.name
            var imageUrl: String = menuItem.attributes.image
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

            var first = true
            val stringBuilder = StringBuilder()
            for (modifierItem in menuItem.orderModifierItems) {
                if (!first) {
                    stringBuilder.append(", ")
                } else {
                    first = false
                }
                stringBuilder.append(modifierItem.attributes.name)
            }
            for (ingredient in menuItem.orderIngredients) {
                if (!first) {
                    stringBuilder.append(", ")
                } else {
                    first = false
                }
                stringBuilder.append(ingredient.attributes.name)
            }

            if (stringBuilder.isNotEmpty()) {
                itemModifiers.visibility = View.VISIBLE
                itemModifiers.text = stringBuilder
            } else {
                itemModifiers.visibility = View.INVISIBLE
            }

            itemQuantity.text = "x" + menuItem.attributes.quantity

            totalPrice.text = StringUtil.getNewFormattedPrice(
                (menuItem.attributes.price * menuItem.attributes.quantity), true
            )

            executePendingBindings()
        }
    }

    fun translated(language: String, name: Translated): String? {
        return if (language.equals("en-us", ignoreCase = true)) {
            name.english
        } else {
            if (language.equals("fr", ignoreCase = true)) name.french else name.arabic
        }
    }

    fun updateList(menuItems: ArrayList<MenuItem>) {
        this.menuItems.clear()
        this.menuItems.addAll(menuItems)
        notifyDataSetChanged()
    }
}
