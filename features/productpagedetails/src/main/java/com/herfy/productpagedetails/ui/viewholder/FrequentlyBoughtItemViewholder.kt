package com.herfy.productpagedetails.ui.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemModifierFrequentlyBoughtBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.PriceFormatter
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import java.util.Locale


class FrequentlyBoughtItemViewholder(val binding : ItemModifierFrequentlyBoughtBinding, val listener : OnClickListener) : RecyclerView.ViewHolder(binding.root) {
    fun setItems(menuItem: MenuItem, position: Int) {
        binding.apply {
            val menuItemAttributes = menuItem.attributes
            val imageUrl = menuItemAttributes.image

            val isAlreadyAddedToCart: Boolean = isFavoriteItemInCart(menuItem)
            btnAdd.visibility = if (isAlreadyAddedToCart) View.GONE else View.VISIBLE
            if (imageUrl != null && !imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) && imageUrl.isNotEmpty() && !imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                modifierImage.loadImageFromUrl(
                    imageUrl = imageUrl,
                    requestOptions = RequestOptions.centerCropTransform(),
                    strategy = DiskCacheStrategy.ALL)
                modifierImage.visible()
            } else {
                modifierImage.gone()
            }
            btnFavorite.setOnClickListener {
                listener.toggleFavoriteFrequentlyBoughtItem(
                    menuItem.isFavorite,
                    menuItem,
                    position
                )
            }

            val caloriesValue = menuItemAttributes.calories
            val calorieCount = menuItemAttributes.calorieCount
            if (calorieCount != 0) {
                layoutCalories.visibility = View.VISIBLE
                tvCalories.text = CacheUtil.getInstance().getTranslations(Text.CALORIES_KCAL)
                    .replace("(x)", calorieCount.toString())
            } else if (caloriesValue != null && caloriesValue.isNotEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                layoutCalories.visibility = View.VISIBLE
                tvCalories.text = CacheUtil.getInstance().getTranslations(Text.CALORIES_KCAL)
                    .replace("(x)", caloriesValue)
            } else {
                layoutCalories.visibility = View.GONE
                tvCalories.text = ""
            }
            if (AuthUtil.getInstance().isSignedIn) {
                if (menuItem.isFavorite) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_frequent_favourites)
                }
                btnFavorite.visibility = View.VISIBLE
            } else {
                btnFavorite.visibility = View.GONE
            }

            btnAdd.setOnClickListener {
                listener.addFrequentlyBoughtItemToCart(
                    menuItem.id,
                    position
                )
            }
            frequentlyParentLayout.setOnClickListener {
                listener.navigateToMenuItemDetails(
                    menuItem.id
                )
            }


            txtModifierItemName.text = menuItemAttributes.name
            modifierDescription.text = menuItemAttributes.description
            txtmodifierPrice.text = PriceFormatter.formatPriceWithLocale(
                menuItemAttributes.price,
                menuItemAttributes.name
            )
        }
    }

    private fun isFavoriteItemInCart(menuItem: MenuItem): Boolean {
        val cartItems = CacheUtil.getInstance().getCart()
        val frequentlyMenuAdded = CacheUtil.getInstance().getFrequentlyQuantityShouldShow()
        val isFrequentMenuAdded = frequentlyMenuAdded.first
        val frequentMenuId = frequentlyMenuAdded.second
        for (cartItem in cartItems) {
            if (isSameMenuItem(cartItem, menuItem) && cartItem.isAddedToCart) {
                return true
            }
            if (isFrequentMenuAdded && frequentMenuId.isNotEmpty() && cartItem.id.toString() == frequentMenuId) {
                return true
            }
        }
        return false
    }

    private fun isSameMenuItem(item1: MenuItem, item2: MenuItem): Boolean {
        return item1.id.toString() == item2.id
    }

    interface OnClickListener{
        fun addFrequentlyBoughtItemToCart(menuItemId: String, position: Int)
        fun toggleFavoriteFrequentlyBoughtItem(favorite: Boolean, menuItem: MenuItem, position: Int)
        fun navigateToMenuItemDetails(menuItemId: String)
    }
}