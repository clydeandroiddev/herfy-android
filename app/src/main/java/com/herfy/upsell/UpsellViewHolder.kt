package com.herfy.upsell

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.herfy.cart.CartContract
import com.herfy.cart.databinding.ItemUpsellBinding
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import java.util.Locale

class UpsellViewHolder(val binding: ItemUpsellBinding, private val upsellPresenter: CartContract.Presenter) :
    RecyclerView.ViewHolder(
      binding.root
    ){
    fun setUpsell(upsell: MenuItem) {
        binding.apply {
            contentMain.setOnClickListener { v: View? ->
                if (upsell.isMustBeCustomized) {
                    upsellPresenter.goToItemDetails(adapterPosition, upsell)
                } else {
                    upsell.attributes.quantity = 1
                    upsellPresenter.addToCart(upsell)
                }
            }

            plus.setOnClickListener {
                if (upsell.isMustBeCustomized) {
                    upsellPresenter.goToItemDetails(adapterPosition, upsell)
                } else {
                    upsell.attributes.quantity = 1
                    upsellPresenter.addToCart(upsell)
                }
            }
            var imageUrl = upsell.attributes.image
            if (imageUrl == null || imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
            }
            image.loadImageFromUrl(imageUrl = imageUrl, requestOptions = RequestOptions.centerCropTransform())
            name.text = upsell.attributes.name
            var orderStoreId: String? = null
            if (CacheUtil.getInstance().orderStore != null) {
                orderStoreId = CacheUtil.getInstance().orderStore.id
            }
            totalPrice.text = StringUtil.getFormattedPrice(upsell.attributes.getPrice(orderStoreId))
        }

    }
}