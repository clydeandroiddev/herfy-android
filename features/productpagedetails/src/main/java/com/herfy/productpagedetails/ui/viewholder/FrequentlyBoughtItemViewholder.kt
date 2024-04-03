package com.herfy.productpagedetails.ui.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.productpagedetails.databinding.ItemModifierFrequentlyBoughtBinding
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem


class FrequentlyBoughtItemViewholder(val binding : ItemModifierFrequentlyBoughtBinding, val listener : OnClickListener) : RecyclerView.ViewHolder(binding.root) {
    fun setItems(menuItem: MenuItem) {
        binding.apply {
            val imageUrl = menuItem.attributes.image
            modifierImage.loadImageFromUrl(
                imageUrl,
                requestOptions = RequestOptions.centerCropTransform(),
                strategy = DiskCacheStrategy.ALL
            )
            modifierImage.visible()

            val caloriesValue = menuItem.calories
            val calorieCount = menuItem.calorieCount
            if (caloriesValue != null && !caloriesValue.isEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                txtCalories.text = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
                txtCalories.visible()
            } else if (calorieCount != 0) {
                txtCalories.text = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
                txtCalories.visible()
            } else {
                txtCalories.gone()
            }


            val menuItemAttributes = menuItem.attributes
            txtModifierItemName.text = menuItemAttributes.name
            modifierDescription.text = menuItem.getDescription(LocaleUtil.getInstance().language)
            txtmodifierPrice.text = StringUtil.getFormattedPrice(menuItemAttributes.price)
        }
    }

    interface OnClickListener{
        fun onSelectFrequentlyBoughtItem(menuItem : MenuItem, position : Int)
    }
}