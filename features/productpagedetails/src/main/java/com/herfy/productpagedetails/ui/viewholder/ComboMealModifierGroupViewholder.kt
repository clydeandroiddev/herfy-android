package com.herfy.productpagedetails.ui.viewholder

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig
import java.util.Locale

class ComboMealModifierGroupViewholder(val binding: ItemModifierGroupNewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun setModifierGroup(
        data: ModifierGroup,
        showModifierItemPrice: Boolean,
        shouldShowModifierGroupCheckbox: Boolean,
        shouldShowQuantity: HashMap<Int, Boolean>
    ) {
        binding.apply {
            val context = itemView.context
            val imageUrl: String = data.attributes.image
            var visible = false
            val visibilityConfig: VisibilityConfig =
                data.attributes.visibilityConfig
            if (visibilityConfig != null) {
                if (visibilityConfig.initialValue != null && !visibilityConfig.initialValue.equals(
                        "hidden",
                        ignoreCase = true
                    )
                ) {
                    visible = true
                    recyclerView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.GONE
                }
            } else {
                visible = true
                recyclerView.visibility = View.VISIBLE
            }

            name.text = data.attributes.name
            if (imageUrl != null && !imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) && !imageUrl.isEmpty() && !imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                modifierImage.loadImageFromUrl(
                    imageUrl,
                    requestOptions = RequestOptions.centerInsideTransform(),
                    strategy = DiskCacheStrategy.ALL
                )
            }
            btnMinimum.setVisible(shouldShowModifierGroupCheckbox && data.attributes.minimum > 1)
            checkBox.setVisible(shouldShowModifierGroupCheckbox)
            minimum.setText(CacheUtil.getInstance().getTranslations(Text.REQUIRED, "Required"))


            if (visible) {

                val minimum: Int = data.attributes.minimum
                val maximum: Int = data.attributes.maximum
                if (minimum > 0) {
                    if (maximum > minimum) {
                        select.text =
                            CacheUtil.getInstance()
                                .getTranslations(Text.SELECT_ATLEAST, "(Select at least (x))")
                                .replace(
                                    "(x)",
                                    StringUtil.localize(minimum.toString())
                                )
                        select.visibility = View.VISIBLE
                    } else if (maximum == minimum) {
                        select.text =
                            CacheUtil.getInstance().getTranslations(Text.SELECT_X, "(Select (x))")
                                .replace(
                                    "(x)",
                                    StringUtil.localize(maximum.toString())
                                )
                        select.visibility = View.VISIBLE
                    } else {
                        select.visibility = View.INVISIBLE
                    }
                } else if (minimum == 0) {
                    if (maximum > 0) {
                        select.text =
                            CacheUtil.getInstance()
                                .getTranslations(Text.SELECT_UP_TO_X, "(Select up to (x))").replace(
                                    "(x)",
                                   StringUtil.localize(maximum.toString())
                                )
                        select.visibility = View.VISIBLE
                    } else {
                        select.visibility = View.INVISIBLE
                    }
                } else {
                    select.visibility = View.INVISIBLE
                }

                recyclerView.layoutManager = LinearLayoutManager(context)
                /*val modifierItemsNewRecyclerViewAdapter = ComboMealModifierItemAdapter(

                )
                //modifierItemsNewRecyclerViewAdapter.setOnAction(onAction)
                recyclerView.adapter = modifierItemsNewRecyclerViewAdapter*/
            }
        }
    }
}