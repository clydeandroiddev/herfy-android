package com.herfy.productpagedetails.ui.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.model.modifiers.ModifierAdapterItem
import com.herfy.model.modifiers.ModifierGroupAdapterItem
import com.herfy.model.modifiers.ModifierItemAdapterItem
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemModifierGroupHeaderBinding
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding
import com.herfy.productpagedetails.ui.viewholder.ModifierItemOptionViewHolder
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.visibility
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ModifierProcessor
import com.herfy.utils.helper.ModifierProcessorCallback
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.StringUtil.localize
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupAttributes
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import java.util.Locale

class ComboItemDetailsModifierAdapter(private var itemList: List<ModifierAdapterItem> = mutableListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var onAction: ComboItemDetailsModifierListener? = null
    fun setOnAction(onAction : ComboItemDetailsModifierListener){
        this.onAction = onAction
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ModifierAdapterItem.VIEW_TYPE_MODIFIER_GROUP -> {
                val binding = ItemModifierGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierGroupItemViewHolder(binding)
            }

            ModifierAdapterItem.VIEW_TYPE_MODIFIER_ITEM -> {
                val binding = ItemModifierOptionTypeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierItemViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ModifierAdapterItem.VIEW_TYPE_MODIFIER_GROUP -> {
                val item = itemList[position] as ModifierGroupAdapterItem
                holder.cast<ModifierGroupItemViewHolder> {
                    it.bind(item)

                }

            }

            ModifierAdapterItem.VIEW_TYPE_MODIFIER_ITEM -> {
                val item = itemList[position] as ModifierItemAdapterItem
                holder.cast<ModifierItemViewHolder> {
                    it.bind(item)
                    it.binding.apply {
                        clickableLayout.setOnClickListener {
                            if(item.modifierItem.attributes.minimum != 1){
                                onAction?.onCheckedChanged(!item.modifierItem.attributes.isSelected,
                                    modifierGroup = item.modifierGroup,
                                    modifierItem = item.modifierItem)
                            }
                        }
                        checkLayout.setOnClickListener {
                            if(item.modifierItem.attributes.minimum != 1){
                                onAction?.onCheckedChanged(!item.modifierItem.attributes.isSelected,
                                    modifierGroup = item.modifierGroup,
                                    modifierItem = item.modifierItem)
                            }
                        }
                        quantityUp.setOnClickListener {
                            onAction?.onQuantityChanged(
                                true,
                                modifierGroup = item.modifierGroup,
                                modifierItem = item.modifierItem,
                                quantityDown.isVisible()
                            )
                        }
                        quantityDown.setOnClickListener {
                            onAction?.onQuantityChanged(
                                false,
                                modifierGroup = item.modifierGroup,
                                modifierItem = item.modifierItem,
                                quantityDown.isVisible()
                            )
                        }
                    }
                }
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemViewType(position: Int): Int = itemList[position].viewType

    fun addData(items: List<ModifierAdapterItem>) {
        val diffResult = DiffUtil.calculateDiff(Diff(itemList, items))
        this.itemList = items
        diffResult.dispatchUpdatesTo(this)
    }

    class Diff(
        private val old: List<ModifierAdapterItem>,
        private val newItem: List<ModifierAdapterItem>
    ) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPos: Int, newPosition: Int): Boolean {
            return when (val oldItem = old[oldItemPos]) {
                is ModifierGroupAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is ModifierGroupAdapterItem -> {
                            oldItem.modifierGroup.id == newItem.modifierGroup.id
                        }

                        else -> false
                    }
                }

                is ModifierItemAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is ModifierItemAdapterItem -> {
                            oldItem.modifierItem.id == newItem.modifierItem.id
                        }

                        else -> false
                    }
                }
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition] == newItem[newItemPosition]

        override fun getOldListSize(): Int = old.size

        override fun getNewListSize(): Int = newItem.size
    }


    //ViewHolder Classes
    class ModifierGroupItemViewHolder(
        private val binding: ItemModifierGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierGroupAdapterItem) {
            binding.apply {
                val data = item.modifierGroup
                val visibilityConfig = data.attributes.visibilityConfig
                var isVisibileByConfig = true
                if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                        "hidden",
                        ignoreCase = true
                    )
                ) {
                    isVisibileByConfig = false
                }
                itemView.visibility(isVisibileByConfig)
                name.text = data.attributes.name

                var imageUrl: String? = data.attributes.image
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
                val cacheUtil = CacheUtil.getInstance()
                select.apply {
                    visible()
                    text = when (data.attributes.maximum > 1) {
                        true -> cacheUtil.getTranslations(Text.SELECT_UP_TO_X, "Select up to (x)")
                            .replace("(x)", StringUtil.localize(data.attributes.maximum.toString()))

                        false -> cacheUtil.getTranslations(Text.SELECT_X, "Select (x)")
                            .replace("(x)", StringUtil.localize(data.attributes.maximum.toString()))
                    }
                }
                minimum.text = cacheUtil.getTranslations(Text.REQUIRED, "Required")
                if (item.hasSelectedModifierItems) {
                    checkBox.visible()
                    btnMinimum.gone()
                } else {
                    checkBox.gone()
                    if (data.attributes.minimum >= 1) {
                        btnMinimum.visible()
                    }
                }
            }
        }
    }

    class ModifierItemViewHolder(
        val binding: ItemModifierOptionTypeBinding
    ) : RecyclerView.ViewHolder(binding.root), ModifierProcessorCallback {
        private var modifierProcessor = ModifierProcessor(this)
        fun bind(item: ModifierItemAdapterItem) {
            binding.apply {
                val modifierGroup = item.modifierGroup
                val modifierItem = item.modifierItem
                val visibilityConfig = modifierGroup.attributes.visibilityConfig
                var isVisibileByConfig = true
                if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                        "hidden",
                        ignoreCase = true
                    )
                ) {
                    isVisibileByConfig = false
                }
                itemView.visibility(isVisibileByConfig)
                modifierProcessor.processModifier(item.modifierItem)

                val modifierGroupAttributes: ModifierGroupAttributes = modifierGroup.attributes
                val maximum = modifierGroupAttributes.maximum
                val minimum = modifierGroupAttributes.minimum
                val isModifierItemSelected: Boolean = modifierItem.attributes.isSelected
                radioButton.visibility =
                    if (maximum == 1 && minimum == 1) View.VISIBLE else View.GONE
                checkBox.visibility =
                    if (maximum > 1 || minimum == 0 && maximum == 1) View.VISIBLE else View.GONE

                if (isModifierItemSelected) {
                    optionCardView.setBackgroundResource(R.drawable.modifier_item_bg_selected_state)
                    radioButton.setImageResource(R.drawable.modifier_radio_item_selected)
                    checkBox.setImageResource(R.drawable.modifier_item_checked_selected)
                } else {
                    optionCardView.setBackgroundResource(R.drawable.modifier_item_bg_unselected_state)
                    radioButton.setImageResource(R.drawable.modifier_item_radio_unselected)
                    checkBox.setImageResource(R.drawable.modifier_item_unselected)
                }
                val isQuantityShouldShow = modifierGroup.attributes.maximum > 1 &&
                        modifierItem.attributes.isSelected && modifierItem.attributes.maximum > 1

                quantityDownLayout.visibility =
                    if (isQuantityShouldShow) View.VISIBLE else View.GONE

                val quantityUpIsClickable =
                    modifierItem.attributes.quantity < modifierGroup.attributes.maximum
                quantityUp.apply {
                    isFocusable = quantityUpIsClickable
                    isClickable = quantityUpIsClickable
                }
                quantity.text = localize(("0" + modifierItem.attributes.quantity))
                val imageUrl = modifierItem.attributes.image
                if (imageUrl != null && !imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) && !imageUrl.isEmpty() && !imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    Glide.with(itemView.context).load(imageUrl)
                        .apply(RequestOptions.centerCropTransform())
                        .placeholder(R.drawable.imagepicker_image_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).into(image)
                    image.visible()
                } else {
                    image.gone()
                }
                if (modifierItem.attributes.hasModifierItemExceedMaximum() && modifierItem.attributes.hasUserInteractedWithModifierItem()) {
                    val shouldShowQuantityLayout =
                        MenuUtil.shouldShowQuantity(modifierGroup, modifierItem)
                    if (shouldShowQuantityLayout) {
                        quantityUp.isEnabled = true
                        quantityUp.setImageResource(R.drawable.quantity_up_blue)
                        optionCardView.isEnabled = true
                    }
                } else {
                    val shouldShowQuantityLayout =
                        MenuUtil.shouldShowQuantity(modifierGroup, modifierItem)
                    if (shouldShowQuantityLayout) {
                        quantityUp.setEnabled(false)
                        quantityUp.setImageResource(R.drawable.quantity_up_blue_gray)
                        optionCardView.setEnabled(false)
                    }
                }
                val quantityDownIsClickable = modifierItem.attributes.quantity != 1
                quantityDown.apply {
                    isFocusable = quantityDownIsClickable
                    isClickable = quantityDownIsClickable
                }
            }
        }

        override fun onModifierProcessed(name: String, formattedPrice: String) {
            binding.apply {
                if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                    txtModifierItemName.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE)
                } else {
                    if (ModifierItemOptionViewHolder.isProbablyArabic(name)) {
                        txtModifierItemName.setText(
                            Html.fromHtml(name),
                            TextView.BufferType.SPANNABLE
                        )
                    } else {
                        txtModifierItemName.setText(
                            Html.fromHtml(name),
                            TextView.BufferType.SPANNABLE
                        )
                    }
                }
                txtmodifierPrice.text = formattedPrice
            }
        }
    }


    fun updateSelectedModifierItem(
        hasModifierItemsSelected: Boolean,
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem
    ) {
        val firstIndex =
            itemList.indexOfFirst { modifierAdapterItem -> modifierAdapterItem.viewType == ModifierAdapterItem.VIEW_TYPE_MODIFIER_GROUP && (modifierAdapterItem as ModifierGroupAdapterItem).modifierGroup.id == modifierGroup.id }

        itemList.map { modifierAdapterItem ->
            if (modifierAdapterItem is ModifierGroupAdapterItem && modifierAdapterItem.modifierGroup.id == modifierGroup.id) {
                modifierAdapterItem.modifierGroup = modifierGroup
                modifierAdapterItem.hasSelectedModifierItems = hasModifierItemsSelected
            }
        }
        itemList.map { modifierAdapterItem ->
            if (modifierAdapterItem is ModifierItemAdapterItem && modifierAdapterItem.modifierGroup.id == modifierGroup.id) {
                modifierAdapterItem.modifierGroup = modifierGroup
                if(modifierAdapterItem.modifierItem.id == modifierItem.id){
                    modifierAdapterItem.modifierItem = modifierItem
                }
            }
        }

        notifyItemRangeChanged(firstIndex, modifierGroup.modifierItems.size)
    }


    //Event
    interface ComboItemDetailsModifierListener {
        fun onCheckedChanged(
            isSelected: Boolean,
            modifierGroup: ModifierGroup,
            modifierItem: ModifierItem
        )

        fun onQuantityChanged(
            add: Boolean,
            modifierGroup: ModifierGroup,
            modifierItem: ModifierItem,
            isQuantityVisible: Boolean,
        )

    }


}