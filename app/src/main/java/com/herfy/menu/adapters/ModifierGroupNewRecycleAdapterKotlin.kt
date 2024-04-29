package com.herfy.menu.adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.R
import com.herfy.menu.adapters.model.ModifierItemClickParams
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.ArabicUtils
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ModifierProcessor
import com.herfy.utils.helper.ModifierProcessorCallback
import com.herfy.utils.helper.StringUtil.localize
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemAttributes
import com.herfy.productpagedetails.R as productPageR


class ModifierGroupNewRecycleAdapterKotlin(
    val onUserClickModifierItem: (ModifierItemClickParams) -> Unit,
    private val showModifierMaximumLimit: (String) -> Unit,
    private val onUserAddMoreModifierItemQuantity: (ModifierItemClickParams) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemList: List<ModifierAdapterType> = mutableListOf()
    private val clickCountMap = HashMap<String, Int>()

    companion object {
        const val VIEW_TYPE_MODIFIER_GROUP = 0

        const val VIEW_TYPE_MODIFIER_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MODIFIER_GROUP -> {
                val binding = ItemModifierGroupNewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierGroupViewHolder(binding)
            }

            VIEW_TYPE_MODIFIER_ITEM -> {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_MODIFIER_GROUP -> {
                val item = itemList[position] as ModifierGroupAdapterItem
                val menuCategoryHolder = holder as ModifierGroupViewHolder
                menuCategoryHolder.bind(item)
            }

            VIEW_TYPE_MODIFIER_ITEM -> {
                val item = itemList[position] as ModifierItemAdapterItem
                val menuItemHolder = holder as ModifierItemViewHolder
                menuItemHolder.bind(item)

            }
        }
    }


    override fun getItemCount(): Int = itemList.size


    fun submitList(items: List<ModifierAdapterType>) {
        val diffResult = DiffUtil.calculateDiff(Diff(itemList, items))
        this.itemList = items
        diffResult.dispatchUpdatesTo(this)
    }


    class ModifierGroupViewHolder(private val binding: ItemModifierGroupNewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierGroupAdapterItem) {
            val modifierGroup = item.modifierGroup
            val isModifierGroupRequired = !item.hasSelectedItems && modifierGroup.attributes.minimum >= 1
            val imageUrl = item.modifierGroup.attributes.image
            val isImageUrl = imageUrl != null &&
                    imageUrl.isNotBlank() &&
                    !imageUrl.lowercase().contains("default-image.png") &&
                    !imageUrl.lowercase().contains("default_category.png")

            binding.modifierImage.setVisible(isImageUrl)
            Glide.with(itemView.context)
                .load(imageUrl)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(R.drawable.imagepicker_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.modifierImage)
            binding.name.text = item.modifierGroup.attributes.name
            binding.btnMinimum.setVisible(isModifierGroupRequired)
            binding.minimum.text =
                CacheUtil.getInstance().getTranslations(Text.REQUIRED, "Required")
            binding.checkBox.setVisible(modifierGroup.isHasSelectedModifierItems || item.hasSelectedItems)

        }
    }

    fun updateModifierItem(params: ModifierItemClickParams) {
        with(params) {
            val modifierGroupAttributes = modifierGroup.attributes
            val modifierGroupMaximum = modifierGroupAttributes.maximum
            val modifierGroupMinimum = modifierGroupAttributes.minimum
            if (modifierGroupMinimum == 0 && modifierGroupMaximum == 1) {
                deselectPreviouslySelectedItems(modifierPosition)
                selectNewItem(modifierPosition, isSelected, modifierGroupPosition, modifierGroup)

            } else if (modifierGroupMaximum > 1) {
                var selectedModifierItem = 0
                val modifierItem = modifierGroup.modifierItems[modifierPosition - 1]

                for ((index, listItem) in itemList.withIndex()) {
                    if (listItem is ModifierItemAdapterItem && listItem.modifierItem.attributes.isSelected) {
                        selectedModifierItem += listItem.modifierItem.attributes.quantity

                    }
                }
                if (isSelected && selectedModifierItem >= modifierGroupMaximum) {
                    showModifierMaximumLimit(
                        CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
                    )
                } else {
                    if (this.modifierItem.id == modifierItem.id) {
                        selectNewItem(
                            modifierPosition,
                            isSelected,
                            modifierGroupPosition,
                            modifierGroup
                        )
                    }
                }
            }
        }
    }

    fun updateModifierItemQuantity(modifierItemClickParams: ModifierItemClickParams) {
        with(modifierItemClickParams) {
            val modifierAttributes = modifierItem.attributes
            val quantity = modifierAttributes.quantity
            if (isAdd.not()) {
                if (quantity == 1) {
                    updateModifierItemQuantity(
                        selectedPosition = modifierPosition,
                        isSelected = false,
                        modifierGroupPosition
                    )
                }
            } else {
                updateModifierItemQuantity(
                    selectedPosition = modifierPosition,
                    isSelected = true,
                    modifierGroupPosition
                )
            }
        }
    }

    private fun updateModifierGroup(currentModifierGroupIndex: Int, modifierGroup: ModifierGroup) {
        val item = itemList[currentModifierGroupIndex]
        val modifierGroupItem = itemList.find {
            it is ModifierGroupAdapterItem
        }


        if (modifierGroupItem is ModifierGroupAdapterItem) {
            try {
                val hasSelectedItemModifiers = modifierGroup.modifierItems.any {
                    it.attributes.isSelected
                }
                modifierGroupItem.hasSelectedItems = hasSelectedItemModifiers
                notifyItemChanged(currentModifierGroupIndex)
                modifierGroupItem.modifierGroup = modifierGroup

            }catch (e :Exception) {
                println("Error message $e")
            }
        }

//        if (item is ModifierItemAdapterItem) {
//            val hasSelectedItemModifiers = modifierGroup.modifierItems.any {
//                it.attributes.isSelected
//            }
//            item.modifierGroup = modifierGroup
//            item.modifierGroup.isHasSelectedModifierItems = hasSelectedItemModifiers
//            notifyItemChanged(currentModifierGroupIndex)
//        }
    }


    private fun deselectPreviouslySelectedItems(selectedPosition: Int) {
        for ((index, listItem) in itemList.withIndex()) {
            if (listItem is ModifierItemAdapterItem && listItem.modifierItem.attributes.isSelected && index != selectedPosition) {
                listItem.modifierItem.attributes.isSelected = false
                notifyItemChanged(index)
            }
        }
    }

    private fun selectNewItem(
        selectedPosition: Int,
        isSelected: Boolean,
        selectedModifierGroupIndex: Int,
        modifierGroup: ModifierGroup
    ) {
        val item = itemList[selectedPosition]
        if (item is ModifierItemAdapterItem) {
            item.modifierItem.attributes.isSelected = isSelected
            notifyItemChanged(selectedPosition)
            updateModifierGroup(selectedModifierGroupIndex, item.modifierGroup)
        }
    }

    private fun updateModifierItemQuantity(
        selectedPosition: Int,
        isSelected: Boolean,
        selectedModifierGroupIndex: Int
    ) {
        val item = itemList[selectedPosition]
        if (item !is ModifierItemAdapterItem) return

        val modifierItem = item.modifierItem
        val attributes = modifierItem.attributes
        increaseQuantity(attributes)
        attributes.isSelected = isSelected
        notifyItemChanged(selectedPosition)
        updateModifierGroup(selectedModifierGroupIndex, modifierGroup = item.modifierGroup)
    }

    private fun increaseQuantity(attributes: ModifierItemAttributes) {
        var quantity = attributes.quantity
        if (!attributes.isSelected && quantity <= 1) quantity else quantity += 1
        attributes.quantity = quantity
    }

    inner class ModifierItemViewHolder(private val binding: ItemModifierOptionTypeBinding) :
        RecyclerView.ViewHolder(binding.root), ModifierProcessorCallback {

        private var modifierProcesser: ModifierProcessor? = null

        init {
            modifierProcesser = ModifierProcessor(this)
        }

        fun bind(item: ModifierItemAdapterItem) {
            val modifierItem = item.modifierItem
            val modifierGroup = item.modifierGroup
            modifierProcesser?.processModifier(item.modifierItem)
            val modifierGroupAttributes = item.modifierGroup.attributes
            val modifierGroupMaximum = modifierGroupAttributes.maximum
            val modifierGroupMinimum = modifierGroupAttributes.minimum
            val attributes = item.modifierItem.attributes
            val imageUrl = attributes.image
            val modifierItemAttributes = modifierItem.attributes
            val modifierMaximum = modifierItemAttributes.maximum
            val modifierMinimum = modifierItemAttributes.minimum
            val isModifierItemSelected = modifierItem.attributes.isSelected
            val modifierItemQuantity = modifierItem.attributes.quantity
            val shouldShowQuantityLayout = MenuUtil.shouldShowQuantity(
                modifierGroup,
                modifierItem
            ) && (binding.checkLayout.visibility == View.GONE
                    && binding.radioButton.visibility == View.GONE)
            val isQuantityShouldShow =
                isModifierItemSelected && shouldShowQuantityLayout
            val isUserCanAddMoreQuantity = modifierItemAttributes.quantity < modifierGroupMaximum

            val isImageUrl = imageUrl != null &&
                    imageUrl.isNotBlank() &&
                    !imageUrl.lowercase().contains("default-image.png") &&
                    !imageUrl.lowercase().contains("default_category.png")

            binding.quantityDownLayout.setVisible(isQuantityShouldShow)
            binding.quantity.text = localize("0" + modifierItem.attributes.quantity)
            binding.radioButton.setVisible(modifierGroupMaximum == 1 && modifierGroupMinimum == 1)
            binding.checkBox.setVisible(modifierGroupMaximum > 1 || modifierGroupMinimum == 0)
            binding.image.setVisible(isImageUrl)
            binding.quantityUp.isFocusable = isUserCanAddMoreQuantity
            binding.quantity.isClickable = isUserCanAddMoreQuantity
            binding.checkLayout
                .setOnClickListener {
                    if (shouldSelectModifier(
                            modifierMinimum,
                            modifierItemAttributes.isSelected
                        )
                    ) {
                        val params = ModifierItemClickParams(
                            isSelected = false,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            modifierGroup = modifierGroup
                        )
                        onUserClickModifierItem(params)
                    } else {
                        val params = ModifierItemClickParams(
                            isSelected = true,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            modifierGroup = modifierGroup
                        )
                        onUserClickModifierItem(params)
                    }
                }

            Glide.with(itemView.context)
                .load(imageUrl)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(R.drawable.imagepicker_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.image)

            binding.clickableLayout
                .setOnClickListener {
                    if (shouldSelectModifier(
                            modifierMinimum,
                            modifierItemAttributes.isSelected
                        )
                    ) {
                        val params = ModifierItemClickParams(
                            isSelected = false,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            modifierGroup = modifierGroup
                        )
                        onUserClickModifierItem(params)
                    } else {
                        val params = ModifierItemClickParams(
                            isSelected = true,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            modifierGroup = modifierGroup
                        )
                        onUserClickModifierItem(params)
                    }
                }
            updateView(isModifierItemSelected, shouldShowQuantityLayout, modifierItem)
            binding
                .quantityUp.setOnClickListener {
                    val isUserCanAddMoreModifierQuantity =
                        modifierItemQuantity < modifierGroupMaximum
                    if (isUserCanAddMoreModifierQuantity) {
                        val params = ModifierItemClickParams(
                            isSelected = true,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            isAdd = true,
                            modifierGroup = modifierGroup
                        )
                        onUserAddMoreModifierItemQuantity(params)
                    } else {
                        showModifierMaximumLimit(
                            CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
                        )
                    }
                }

        }

        private fun updateView(
            isModifierItemSelected: Boolean,
            shouldShowQuantityLayout: Boolean,
            modifierItem: ModifierItem
        ) {
            if (isModifierItemSelected) {
                binding.optionCardView.setBackgroundResource(productPageR.drawable.modifier_item_bg_selected_state)
                binding.radioButton.setImageResource(productPageR.drawable.modifier_radio_item_selected)
                binding.checkBox.setImageResource(productPageR.drawable.modifier_item_checked_selected)
            } else {
                binding.optionCardView.setBackgroundResource(productPageR.drawable.modifier_item_bg_unselected_state)
                binding.radioButton.setImageResource(productPageR.drawable.modifier_item_radio_unselected)
                binding.checkBox.setImageResource(productPageR.drawable.modifier_item_unselected)

            }
            binding.quantityUp.setVisible(shouldShowQuantityLayout)
            if (shouldShowQuantityLayout) {
                binding.quantityUp.isEnabled = true
                binding.quantityUp.setImageResource(productPageR.drawable.quantity_up_blue)
                binding.optionCardView.isEnabled = true
            } else {
                if (shouldShowQuantityLayout) {
                    binding.quantityUp.isEnabled = false
                    binding.quantityUp.setImageResource(productPageR.drawable.quantity_up_blue_gray)
                    binding.optionCardView.isEnabled = false
                }
            }
        }


        private fun shouldSelectModifier(
            modifierMinimum: Int,
            isSelected: Boolean
        ): Boolean {
            return modifierMinimum != 1 && isSelected
        }

        private fun setModifierNameAndPrice(name: String, formattedPrice: String) {
            val localeUtil = LocaleUtil.getInstance()
            if (localeUtil.isEnglish || localeUtil.isFrench || ArabicUtils.isArabic(name)) {
                binding.txtModifierItemName.text = Html.fromHtml(name, Html.FROM_HTML_MODE_COMPACT)
            } else {
                binding.txtModifierItemName.text = Html.fromHtml(name)
            }
            if (formattedPrice.isNotEmpty()) {
                binding.txtmodifierPrice.text = formattedPrice
            }
        }

        override fun onModifierProcessed(name: String, formattedPrice: String) {
            setModifierNameAndPrice(name = name, formattedPrice = formattedPrice)
        }
    }


    class Diff(
        private val old: List<ModifierAdapterType>,
        private val newItem: List<ModifierAdapterType>
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


    sealed class ModifierAdapterType {
        abstract val viewType: Int
    }

    data class ModifierGroupAdapterItem(
        var modifierGroup: ModifierGroup,
        val modifierGroupIndex: Int,
        var hasSelectedItems: Boolean = false,
    ) : ModifierAdapterType() {
        override val viewType: Int = VIEW_TYPE_MODIFIER_GROUP
    }

    data class ModifierItemAdapterItem(
        val modifierItem: ModifierItem,
        var modifierGroup: ModifierGroup,
        val modifierGroupPosition: Int,
    ) : ModifierAdapterType() {
        override val viewType: Int = VIEW_TYPE_MODIFIER_ITEM
    }
}