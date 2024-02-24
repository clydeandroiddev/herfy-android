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
                /*if (upsell.isMustBeCustomized) {
                    upsellPresenter.goToItemDetails(adapterPosition, upsell)
                } else {
                    if (upsell.attributes.quantity != 0) {
                        upsellPresenter.changeToZero(adapterPosition)
                    } else {
                        upsellPresenter.changeQuantity(adapterPosition, true)
                    }
                }*/
            }
           /* if (upsell.isMustBeCustomized) {
                quantityLayout!!.visibility = View.GONE
                container!!.strokeColor =
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
            } else {
                if (upsell.attributes.quantity != 0) {
                    quantityLayout!!.visibility = View.VISIBLE
                    container!!.strokeColor = Color.parseColor(CacheUtil.getInstance().colorMain)
                } else {
                    quantityLayout!!.visibility = View.GONE
                    container!!.strokeColor =
                        ContextCompat.getColor(itemView.context, android.R.color.transparent)
                }
            }*/
            plus.setOnClickListener {
                /*upsellPresenter.changeQuantity(
                    adapterPosition, true
                )*/
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