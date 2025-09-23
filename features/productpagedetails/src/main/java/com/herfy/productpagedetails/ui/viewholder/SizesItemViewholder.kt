package com.herfy.productpagedetails.ui.viewholder

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemNewSizesFixedBinding
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

class SizesItemViewholder(val binding : ItemNewSizesFixedBinding) : RecyclerView.ViewHolder(binding.root) {

    private var onAction : OnClickListener? = null

    fun setOnAction(action: OnClickListener?){
        onAction = action
    }

    interface OnClickListener{
        fun onSelectSize(size: ModifierItem, pos: Int)
    }

    fun setSize(data: ModifierItem, position : Int) {
        binding.apply {
            val context = itemView.context
            name.text = data.attributes.name
            val price = data.attributes.price
            txtSizesPrice.setVisible(price != 0.0)
            txtSizesPrice.text = StringUtil.getFormattedPrice(price)
            if(data.attributes.isSelected){
                card.strokeColor = ContextCompat.getColor(context, R.color.color_blue)
                card.strokeWidth = ViewUtil.dpToPx(context, 1)
                card.radius = context.resources.getDimensionPixelSize(com.herfy.commonstyles.R.dimen.radius_large).toFloat()
                name.setTextColor(ContextCompat.getColor(context, com.herfy.commonstyles.R.color.white))
                txtSizesPrice.setTextColor(ContextCompat.getColor(context, com.herfy.commonstyles.R.color.white))
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_blue))
                radioButton.setImageResource(R.drawable.ic_size_drawable_checked)
            }else {
                card.strokeColor = ContextCompat.getColor(context, R.color.color_light_blue_shade)
                card.strokeWidth = ViewUtil.dpToPx(context, 1)
                card.radius = context.resources.getDimensionPixelSize(com.herfy.commonstyles.R.dimen.radius_large).toFloat()
                name.setTextColor(ContextCompat.getColor(context, com.herfy.commonstyles.R.color.black))
                txtSizesPrice.setTextColor(ContextCompat.getColor(context,R.color.color_gray))
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_light_blue_shade))
                radioButton.setImageResource(R.drawable.modifier_item_radio_unselected)
            }

            container.setOnClickListener {
                if (!data.attributes.isSelected) {
                   onAction?.onSelectSize(data, position)
                }
            }
        }

    }
}