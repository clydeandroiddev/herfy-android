package com.herfy.menu.adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.R
import com.herfy.menu.adapters.model.ModifierItemClickParams
import com.herfy.productpagedetails.databinding.ItemModifierGroupHeaderBinding
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.headerVisibility
import com.herfy.utils.extensions.itemVisibility
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.ArabicUtils
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ModifierProcessor
import com.herfy.utils.helper.ModifierProcessorCallback
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.StringUtil.localize
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemAttributes
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule
import com.herfy.productpagedetails.R as productPageR


class ModifierGroupNewRecycleAdapterKotlin(
    val onUserClickModifierItem: (ModifierItemClickParams) -> Unit,
    private val showModifierMaximumLimit: (String) -> Unit,
    private val onUserAddMoreModifierItemQuantity: (ModifierItemClickParams) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemList: List<ModifierAdapterType> = mutableListOf()


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
                val binding = ItemModifierGroupHeaderBinding.inflate(
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


    inner class ModifierGroupViewHolder(private val binding: ItemModifierGroupHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModifierGroupAdapterItem) {
            val modifierGroup = item.modifierGroup
            val isModifierGroupRequired =
                !item.hasSelectedItems && modifierGroup.attributes.minimum >= 1
            val imageUrl = item.modifierGroup.attributes.image
            val isImageUrl = imageUrl != null &&
                    imageUrl.isNotBlank() &&
                    !imageUrl.lowercase().contains("default-image.png") &&
                    !imageUrl.lowercase().contains("default_category.png")
            val visibilityConfig = modifierGroup.attributes.visibilityConfig

            var isVisibleByConfig = true
            if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
                )
            ) {
                isVisibleByConfig = false
            }
            binding.modifierGroupLayout.headerVisibility(isVisibleByConfig)
            val minimum = modifierGroup.attributes.minimum
            val maximum = modifierGroup.attributes.maximum
            when {
                minimum > 0 -> {
                    binding.select.text = when {
                        maximum > minimum -> CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_ATLEAST, "(Select at least (x))")

                        maximum == minimum -> CacheUtil.getInstance()
                            .getTranslations(Text.SELECT_X, "(Select (x))")

                        else -> ""
                    }.replace("(x)", StringUtil.localize(minimum.toString()))
                    binding.select.visibility =
                        if (binding.select.text.isNotBlank()) View.VISIBLE else View.INVISIBLE
                }

                minimum == 0 && maximum > 0 -> {
                    binding.select.text = CacheUtil.getInstance()
                        .getTranslations(Text.SELECT_UP_TO_X, "(Select up to (x))")
                        .replace("(x)", StringUtil.localize(maximum.toString()))
                    binding.select.visibility = View.VISIBLE
                }

                else -> binding.select.visibility = View.INVISIBLE
            }
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
            binding.checkBox.setVisible(modifierGroup.isHasSelectedModifierItems || item.hasSelectedItems && binding.btnMinimum.visibility == View.GONE)
        }

        fun setVisibility(isVisible: Boolean) {
            val param = binding.root.layoutParams as RecyclerView.LayoutParams
            if (isVisible) {
                param.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                binding.root.visibility = View.VISIBLE
            } else {
                binding.root.visibility = View.GONE
                param.height = 0
            }
            binding.root.layoutParams = param
        }
    }

    fun updateModifierItem(params: ModifierItemClickParams) {
        with(params) {
            val modifierGroupAttributes = modifierGroup.attributes
            val modifierGroupMaximum = modifierGroupAttributes.maximum
            val modifierGroupMinimum = modifierGroupAttributes.minimum
            if (modifierGroupMinimum == 0 && modifierGroupMaximum == 1) {
                deselectPreviouslySelectedItems(modifierPosition, params.modifierGroup)
                selectNewItem(modifierPosition, isSelected, modifierGroupPosition)
                updateVisibilityConfigBy(currentModifierGroup = params.modifierGroup)

            } else if (modifierGroupMaximum > 1) {
                val selectedModifierIndex =
                    modifierGroup.modifierItems.indexOfFirst { it.id == params.modifierItem.id }
                val modifierItem = modifierGroup.modifierItems[selectedModifierIndex]
                if (!params.isSelected) {
                    modifierItem.attributes.quantity = 1
                    notifyItemChanged(modifierPosition)
                }

                val selectedItemTotal = isCurrentSelectionEqualToMax(modifierGroup)
                isUserExceedTheModifierGroupMax(modifierGroup = params.modifierGroup)
                val isExceedingLimit =
                    isUserExceedTheModifierGroupMax(modifierGroup = params.modifierGroup)
                if (isSelected && isExceedingLimit) {
                    showModifierMaximumLimit(
                        CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
                    )
                } else {
                    if (this.modifierItem.id == modifierItem.id) {
                        selectNewItem(
                            modifierPosition,
                            isSelected,
                            modifierGroupPosition
                        )
                        if (selectedItemTotal) {
                            updateNonSelectedModifiersExceedingMaximum(
                                params = params,
                                isSelected = isSelected
                            )
                        }
                        updateVisibilityConfigBy(currentModifierGroup = params.modifierGroup)
                    }
                }
            }
        }
    }


    private fun updateVisibilityConfigBy(currentModifierGroup: ModifierGroup) {
        var visibilityConfig: VisibilityConfig? = null
        itemList.filterIsInstance<ModifierGroupAdapterItem>()
            .forEachIndexed { index, item ->
                val attributes = item.modifierGroup.attributes
                visibilityConfig = attributes.visibilityConfig
                if (visibilityConfig != null) {
                    val rules = visibilityConfig?.rules
                    if (rules != null && rules.isNotEmpty()) {
                        val rule: VisibilityConfigRule = rules[0]
                        if (rule.targetType != null && rule.targetType.equals(
                                "modifier-group",
                                ignoreCase = true
                            )
                        ) {

                            val eachModifierGroupAttributes = item.modifierGroup.attributes
                            visibilityConfig =
                                eachModifierGroupAttributes.visibilityConfig
                            if (visibilityConfig != null) {
                                val eachModifierRules = visibilityConfig?.rules
                                if (!eachModifierRules.isNullOrEmpty()) {
                                    val eachModifierRule: VisibilityConfigRule = rules[0]
                                    if (eachModifierRule.targetType != null && rule.targetType.equals(
                                            "modifier-group",
                                            ignoreCase = true
                                        )
                                    ) {

                                        item.listOfModifierGroups
                                            .forEachIndexed { eachModifierGroupIndex, modifierGroup ->
                                                if (item.modifierGroup.id != currentModifierGroup.id && eachModifierRule.targetId.equals(
                                                        modifierGroup.id
                                                    )
                                                ) {
                                                    val hasAnySelectedItems =
                                                        modifierGroup
                                                            .modifierItems.any { it.attributes.isSelected }
                                                    if (hasAnySelectedItems) {
                                                        item.modifierGroup.attributes
                                                            .visibilityConfig
                                                            .initialValue = ""
                                                    } else {
                                                        item.modifierGroup.attributes
                                                            .visibilityConfig.initialValue =
                                                            "hidden"
                                                    }
                                                    notifyItemChanged(0, eachModifierGroupIndex)
                                                }
                                            }

                                    }
                                }
                            }
                        }
                    }
                }
            }

        itemList.filterIsInstance<ModifierItemAdapterItem>().forEach { eachModifierItem ->

            val attributes = eachModifierItem.modifierGroup.attributes
            val modifierItemVisibilityConfig = attributes.visibilityConfig
            if (modifierItemVisibilityConfig != null) {
                val targetType = modifierItemVisibilityConfig?.rules?.firstOrNull {
                    it.targetType.equals(
                        "modifier-group",
                        ignoreCase = true
                    )
                }
                if (targetType != null) {
                    eachModifierItem.listOfModifierGroups.forEachIndexed { eachModifierGroupIndex, modifierGroup ->
                        if (eachModifierItem.modifierGroup.id != currentModifierGroup.id && targetType.targetId.equals(
                                modifierGroup.id
                            )
                        ) {
                            val hasAnySelectedItems =
                                modifierGroup.modifierItems.any { it.attributes.isSelected }
                            val visibilityInitialValue = if (hasAnySelectedItems) "" else "hidden"

                            if (visibilityInitialValue.isEmpty() || visibilityInitialValue == "hidden") {
                                eachModifierItem.modifierGroup.attributes.visibilityConfig.initialValue =
                                    visibilityInitialValue
                                notifyItemRangeChanged(0, modifierGroup.modifierItems.size)
                                return@forEach
                            }

                        }
                    }
                }
            }
        }

    }


    fun updateModifierItemQuantity(modifierItemClickParams: ModifierItemClickParams) {

        with(modifierItemClickParams) {
            val selectedItemTotal = isCurrentSelectionEqualToMax(modifierGroup)
            val modifierAttributes = modifierItem.attributes
            val quantity = modifierAttributes.quantity
            if (isAdd.not()) {
                if (quantity == 1) {
                    updateModifierItemQuantity(
                        selectedPosition = modifierPosition,
                        isSelected = false,
                        modifierGroupPosition,
                        false,
                        currentModifierQuantity = quantity,
                        params = modifierItemClickParams
                    )
                } else {
                    updateModifierItemQuantity(
                        selectedPosition = modifierPosition,
                        isSelected = modifierItemClickParams.isSelected,
                        modifierGroupPosition,
                        false,
                        currentModifierQuantity = quantity,
                        params = modifierItemClickParams
                    )
                }
            } else {
                val isUserAlreadyExceed =
                    isUserExceedTheModifierGroupMax(modifierGroup = modifierItemClickParams.modifierGroup)

                if (isUserAlreadyExceed) {
                    updateNonSelectedModifiersExceedingMaximum(
                        params = modifierItemClickParams,
                        isSelected = isSelected
                    )
                    showModifierMaxLimitMessage()
                    return
                }
                if (selectedItemTotal) {
                    updateNonSelectedModifiersExceedingMaximum(
                        params = modifierItemClickParams,
                        isSelected = isSelected
                    )
                }
                updateModifierItemQuantity(
                    selectedPosition = modifierPosition,
                    isSelected = true,
                    selectedModifierGroupIndex = modifierGroupPosition,
                    true,
                    currentModifierQuantity = quantity,
                    params = modifierItemClickParams
                )
            }
        }
    }

    private fun isUserExceedTheModifierGroupMax(modifierGroup: ModifierGroup): Boolean {
        val maximumModifierGroup = modifierGroup.attributes.maximum
        val selectedModifierItemByGroup = countModifierItemTotalBy(modifierGroup)
        return selectedModifierItemByGroup.any<ModifierGroup, Int> { (_, sum) -> sum >= maximumModifierGroup }
    }

    private fun updateNonSelectedModifiersExceedingMaximum(
        params: ModifierItemClickParams,
        isSelected: Boolean
    ) {
        itemList.filterIsInstance<ModifierItemAdapterItem>()
            .filter { isModifierItemExceedingMaximum(it, params) }
            .forEach { updateModifierItem(it, isSelected) }
    }

    private fun isModifierItemExceedingMaximum(
        listItem: ModifierItemAdapterItem,
        params: ModifierItemClickParams
    ): Boolean {
        val isSameGroup = listItem.modifierGroup.id == params.modifierGroup.id
        val isNotCurrentPosition = itemList.indexOf(listItem) != params.modifierPosition
        val isNotSelected = !listItem.modifierItem.attributes.isSelected
        val isMaximumGreaterThanOne = listItem.modifierItem.attributes.maximum > 1

        return isSameGroup && isNotCurrentPosition && isNotSelected && isMaximumGreaterThanOne
    }

    private fun updateModifierItem(item: ModifierItemAdapterItem, isSelected: Boolean) {
        item.modifierItem.attributes.setModifierItemQuantityExceedsMaximum(isSelected)
        println("Not max out ${item.modifierItem.attributes.name}  and $isSelected")
        notifyItemChanged(itemList.indexOf(item))
    }

    private fun isCurrentSelectionEqualToMax(modifierGroup: ModifierGroup): Boolean {
        val maximumAllowedSelection = modifierGroup.attributes.maximum
        val selectedItemsByGroup = countModifierItemTotalBy(modifierGroup)
        return selectedItemsByGroup.any { (_, sum) -> sum + 1 >= maximumAllowedSelection }
    }

    private fun countModifierItemTotalBy(modifierGroup: ModifierGroup): Map<ModifierGroup, Int> {
        val totalQuantityByGroup = itemList.filterIsInstance<ModifierItemAdapterItem>()
            .filter { it.modifierGroup.id == modifierGroup.id }
            .groupBy { it.modifierGroup }
            .mapValues { (_, items) ->
                items.sumOf { if (it.modifierItem.attributes.isSelected) it.modifierItem.attributes.quantity else 0 }
            }
        return totalQuantityByGroup
    }

    private fun updateModifierGroup(currentModifierGroupIndex: Int, modifierGroup: ModifierGroup) {
        val firstIndex = itemList.indexOfFirst { modifierAdapterItem ->
            modifierAdapterItem.viewType == VIEW_TYPE_MODIFIER_GROUP &&
                    (modifierAdapterItem as ModifierGroupAdapterItem).modifierGroup.id == modifierGroup.id
        }
        itemList.filterIsInstance<ModifierGroupAdapterItem>().forEach { eachModifierGroup ->
            if (eachModifierGroup.modifierGroupIndex == currentModifierGroupIndex) {
                val currentSelectedModifierGroup = eachModifierGroup.modifierGroup

                val hasSelectedItemModifiers = currentSelectedModifierGroup.modifierItems.any {
                    it.attributes.isSelected
                }
                eachModifierGroup.hasSelectedItems = hasSelectedItemModifiers
                notifyItemRangeChanged(firstIndex, currentModifierGroupIndex)
                eachModifierGroup.modifierGroup = currentSelectedModifierGroup
            }
        }
    }


    private fun deselectPreviouslySelectedItems(
        selectedPosition: Int,
        currentSelectedModifierGroup: ModifierGroup
    ) {
        itemList.filterIsInstance<ModifierItemAdapterItem>()
            .filterIndexed { index, listItem ->
                val attributes = listItem
                    .modifierItem.attributes
                index != selectedPosition &&
                        listItem.modifierItem.attributes.isSelected &&
                        listItem.modifierGroup.id == currentSelectedModifierGroup.id &&
                        attributes.minimum != 1 || attributes.maximum != 1
            }
            .forEach { listItem ->
                listItem.modifierItem.attributes.isSelected = false
                notifyItemChanged(itemList.indexOf(listItem))
            }
    }


    private fun showModifierMaxLimitMessage() {
        showModifierMaximumLimit(
            CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT)
        )
    }

    private fun selectNewItem(
        selectedPosition: Int,
        isSelected: Boolean,
        selectedModifierGroupIndex: Int
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
        selectedModifierGroupIndex: Int,
        isUserIncreaseModifierQuantity: Boolean,
        currentModifierQuantity: Int,
        params: ModifierItemClickParams
    ) {
        val item = itemList[selectedPosition]
        if (item !is ModifierItemAdapterItem) return

        val modifierItem = item.modifierItem
        val attributes = modifierItem.attributes
        if (isUserIncreaseModifierQuantity) {
            increaseQuantity(attributes)
        } else {
            if (currentModifierQuantity == 1) {
                updateQuantityAndNotify(
                    attributes,
                    isSelected,
                    selectedPosition,
                    selectedModifierGroupIndex,
                    item.modifierGroup
                )
                updateNonSelectedModifiersExceedingMaximum(isSelected = isSelected, params = params)
                return
            }
            decreaseModifierQuantity(attributes)
        }
        updateQuantityAndNotify(
            attributes,
            isSelected,
            selectedPosition,
            selectedModifierGroupIndex,
            item.modifierGroup
        )
    }

    private fun updateQuantityAndNotify(
        attributes: ModifierItemAttributes,
        isSelected: Boolean,
        selectedPosition: Int,
        selectedModifierGroupIndex: Int,
        modifierGroup: ModifierGroup
    ) {
        attributes.isSelected = isSelected
        notifyItemChanged(selectedPosition)
        updateModifierGroup(selectedModifierGroupIndex, modifierGroup)

    }

    private fun increaseQuantity(attributes: ModifierItemAttributes) {
        var quantity = attributes.quantity
        if (!attributes.isSelected && quantity <= 1) quantity else quantity += 1
        attributes.quantity = quantity
    }

    private fun decreaseModifierQuantity(attributes: ModifierItemAttributes) {
        attributes.quantity--
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
            )
            val isQuantityShouldShow =
                isModifierItemSelected && shouldShowQuantityLayout
            val isUserCanAddMoreQuantity = modifierItemAttributes.quantity < modifierGroupMaximum

            val isImageUrl = imageUrl != null &&
                    imageUrl.isNotBlank() &&
                    !imageUrl.lowercase().contains("default-image.png") &&
                    !imageUrl.lowercase().contains("default_category.png")
            updateView(
                isModifierItemSelected = isModifierItemSelected,
                shouldShowQuantityLayout = shouldShowQuantityLayout,
                hasExceededTheModifierMax = modifierItemAttributes.hasModifierItemExceedMaximum()
            )
            val visibilityConfig = modifierGroup.attributes.visibilityConfig

            var isVisibleByConfig = true
            if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
                )
            ) {
                isVisibleByConfig = false
            }
            itemView.itemVisibility(isVisibleByConfig)
            binding.quantityDownLayout.setVisible(isQuantityShouldShow)
            binding.quantity.text = localize("0" + modifierItem.attributes.quantity)
            binding.radioButton.setVisible(modifierGroupMaximum == 1 && modifierGroupMinimum == 1)
            binding.checkBox.setVisible(modifierGroupMaximum > 1 && modifierMaximum == 1 || modifierGroupMinimum == 0)
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
                        showModifierMaxLimitMessage()
                    }
                }
            binding
                .quantityDown.setOnClickListener {
                    val isModifierItemAboveSingleOrGroupMax =
                        modifierItemQuantity > 1 || modifierGroupMaximum != 1

                    if (isModifierItemAboveSingleOrGroupMax) {
                        val params = ModifierItemClickParams(
                            isSelected = true,
                            modifierGroupPosition = item.modifierGroupPosition,
                            group = item.modifierGroup,
                            modifierPosition = absoluteAdapterPosition,
                            modifierItem = modifierItem,
                            isAdd = false,
                            modifierGroup = modifierGroup
                        )
                        onUserAddMoreModifierItemQuantity(params)
                    }
                }
        }


        private fun updateView(
            isModifierItemSelected: Boolean,
            shouldShowQuantityLayout: Boolean,
            hasExceededTheModifierMax: Boolean,
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
            if (shouldShowQuantityLayout && hasExceededTheModifierMax.not()) {
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
            } else {
                binding.txtmodifierPrice.text = ""
            }
        }

        override fun onModifierProcessed(name: String, formattedPrice: String) {
            setModifierNameAndPrice(name = name, formattedPrice = formattedPrice)
        }

        fun setVisibility(isVisible: Boolean) {
            val param = binding.root.layoutParams as RecyclerView.LayoutParams
            if (isVisible) {
                param.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                binding.root.visibility = View.VISIBLE
            } else {
                binding.root.visibility = View.GONE
                param.height = 0
            }
            binding.root.layoutParams = param
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
        val listOfModifierGroups: List<ModifierGroup>
    ) : ModifierAdapterType() {
        override val viewType: Int = VIEW_TYPE_MODIFIER_GROUP
    }

    data class ModifierItemAdapterItem(
        val modifierItem: ModifierItem,
        var modifierGroup: ModifierGroup,
        val modifierGroupPosition: Int,
        val listOfModifierGroups: List<ModifierGroup>
    ) : ModifierAdapterType() {
        override val viewType: Int = VIEW_TYPE_MODIFIER_ITEM
    }
}