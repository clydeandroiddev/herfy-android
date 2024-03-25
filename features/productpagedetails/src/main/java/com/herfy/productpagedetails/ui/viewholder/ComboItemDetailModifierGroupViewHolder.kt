package com.herfy.productpagedetails.ui.viewholder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierItemAdapter
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import java.util.Locale

class ComboItemDetailModifierGroupViewHolder(val binding : ItemModifierGroupNewBinding) : RecyclerView.ViewHolder(binding.root) {


    private var onAction: BaseModifierItemViewHolder.OnAction? = null

    fun setOnAction(onAction: BaseModifierItemViewHolder.OnAction?) {
        this.onAction = onAction
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setModifierGroup(
        data: ModifierGroup,
        modifierGroupPosition: Int,
        comboItemModifierItemAdapters: HashMap<Int, ComboItemDetailsModifierItemAdapter>,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean,
    ) {
        binding.apply {
            val context = itemView.context

            val visibilityConfig = data.attributes.visibilityConfig
            if (visibilityConfig != null) {
                if (visibilityConfig.initialValue == null) {
                    name.visibility = View.VISIBLE
                    recyclerView.visibility = View.VISIBLE
                } else {
                    if (visibilityConfig.initialValue != null && !visibilityConfig.initialValue.equals(
                            "hidden",
                            ignoreCase = true
                        )
                    ) {
                        name.visibility = View.VISIBLE
                        recyclerView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.GONE
                        name.visibility = View.GONE
                    }
                }
            } else {
                name.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
            }
            name.text = data.attributes.name
            val modifierItems = ArrayList(data.modifierItems)
            val comboMealModifierItemsAdapter = ComboItemDetailsModifierItemAdapter(
                items = modifierItems,
                modifierGroup = data,
                modifierGroupPosition = modifierGroupPosition,
                showModifierItemPrice = showModifierItemPrice
            )
            comboMealModifierItemsAdapter.setOnAction(onAction)
            recyclerView.apply {
                adapter = comboMealModifierItemsAdapter
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
            comboMealModifierItemsAdapter.notifyDataSetChanged()
            comboItemModifierItemAdapters[modifierGroupPosition] = comboMealModifierItemsAdapter
            var imageUrl : String? = data.attributes.image
            if (imageUrl == null || imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
            }

            modifierImage.loadImageFromUrl(
                imageUrl,
                strategy = DiskCacheStrategy.ALL,
                requestOptions = RequestOptions.centerInsideTransform(),
                isSkipMemoryCache = false
            )
            //btnMinimum.visibility = if (showShowModifierCheckBox && data.attributes.minimum > 1) View.GONE else View.VISIBLE
            if(showShowModifierCheckBox) {
                checkBox.visible()
                btnMinimum.gone()
            }else{
                checkBox.gone()
                if(data.attributes.minimum >= 1){
                    btnMinimum.visible()
                }

            }
            minimum.text = CacheUtil.getInstance().getTranslations(Text.REQUIRED, "Required")
        }
    }


}