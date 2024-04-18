package com.herfy.productpagedetails.ui.viewholder

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemComboComponentItemBinding
import com.herfy.productpagedetails.ui.adapter.SelectedModifiersPerItemAdapter
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import java.util.Locale


class ComboMealModifierItemViewHolder(val binding: ItemComboComponentItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun setComboComponentItem(componentModifierItem: ComponentModifierItem) {
        binding.apply {
            val context = itemView.context
            var imageUrl: String? = componentModifierItem.image
            var hasModifiers = componentModifierItem.modifierGroups.size >= 1
            if (imageUrl == null || imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
            }

            image.loadImageFromUrl(
                imageUrl,
                strategy = DiskCacheStrategy.ALL,
                requestOptions = RequestOptions.centerInsideTransform(),
                isSkipMemoryCache = false
            )

            if(hasModifiers){
                val selectedLists = fetchSelectedModifierItemsPerItem(componentModifierItem.modifierGroups)
                if(selectedLists.isNotEmpty()){
                    val selectedModifiersPerItemAdapter = SelectedModifiersPerItemAdapter(selectedLists)
                    val flexLayoutManager = FlexboxLayoutManager(context)
                    flexLayoutManager.flexWrap = FlexWrap.WRAP
                    flexLayoutManager.flexDirection = FlexDirection.ROW
                    flexLayoutManager.alignItems = AlignItems.FLEX_START
                    rvSelectedModifiers.apply {
                        visible()
                        layoutManager = flexLayoutManager
                        adapter = selectedModifiersPerItemAdapter
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = false
                    }
                }else{
                    rvSelectedModifiers.gone()
                }
            }else {
                rvSelectedModifiers.gone()
            }

            if (hasModifiers) {
                ivRightIcon.visible()
                radioButton.gone()
                ivRightIcon.setImageResource(R.drawable.ic_arrow_right_tail_blue)
            } else {
                ivRightIcon.gone()
                radioButton.visible()
            }

            if (componentModifierItem.selected) {
                optionCardView.strokeWidth = 1
                optionCardView.strokeColor = context.getColor(R.color.background_color_blue)
                optionCardView.backgroundTintList =
                    ColorStateList.valueOf(context.getColor(R.color.card_selected_bg))
                if (hasModifiers) {
                    ivRightIcon.setImageResource(R.drawable.ic_edit_blue)
                } else {
                    radioButton.isChecked = true
                }
            } else {
                optionCardView.strokeColor = Color.WHITE
                optionCardView.backgroundTintList =
                    ColorStateList.valueOf(context.getColor(R.color.white))
                if (hasModifiers) {
                    ivRightIcon.setImageResource(R.drawable.ic_arrow_right_tail_blue)
                } else {
                    radioButton.isChecked = false
                }
            }
            txtModifierItemName.text = componentModifierItem.name.toTranslate()

            val componentModifierItemPrice =
                if (componentModifierItem.price != 0.0) componentModifierItem.price else componentModifierItem.attributes.price
            if (componentModifierItemPrice > 0) {
                txtmodifierPrice.visible()
                txtmodifierPrice.text = StringUtil.getFormattedPrice(componentModifierItemPrice)
            } else {
                txtmodifierPrice.invisible()
            }



        }
    }


    private fun fetchSelectedModifierItemsPerItem(modifierGroups : List<ModifierGroup>) : List<String>{
        val list = arrayListOf<String>()
        for (modifierGroup in modifierGroups) {
            val visibilityConfig = modifierGroup.attributes.visibilityConfig
            if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
                )
            ) {
                for (modifierItem in modifierGroup.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        modifierItem.attributes.translatedName.toTranslate()?.let {
                            list.add(it)
                        }
                    }
                }
            }
        }

        return list
    }

    interface onClickListener {
        fun onSelectComboComponentItem(item: ComponentModifierItem, menuItemComponentPos: Int, componentModifierItemPos : Int, previousSelectedIndex : Int)
    }
}