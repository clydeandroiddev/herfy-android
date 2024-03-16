package com.herfy.productpagedetails.ui.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.productpagedetails.databinding.ItemModifierFrequentlyBoughtBinding
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem


class FrequentlyBoughtItemViewholder(val binding : ItemModifierFrequentlyBoughtBinding) : RecyclerView.ViewHolder(binding.root) {
    fun setItems(menuItem: MenuItem) {
        binding.apply {
            val imageUrl = menuItem.attributes.image
            modifierImage.loadImageFromUrl(
                imageUrl,
                requestOptions = RequestOptions.centerCropTransform(),
                strategy = DiskCacheStrategy.ALL
            )

            val menuItemAttributes = menuItem.attributes
            txtModifierItemName.text = menuItemAttributes.name
            modifierDescription.text = menuItem.getDescription(LocaleUtil.getInstance().language)
            txtmodifierPrice.text = StringUtil.getFormattedPrice(menuItemAttributes.price)
            txtCalories.text = menuItem.calories
        }
    }
}