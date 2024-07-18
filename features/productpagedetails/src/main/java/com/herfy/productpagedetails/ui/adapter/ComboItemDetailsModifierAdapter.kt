package com.herfy.productpagedetails.ui.adapter

import android.text.Html
import android.view.LayoutInflater
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
import com.herfy.model.modifiers.ModifierLineAdapterItem
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemComboModifierGroupHeaderBinding
import com.herfy.productpagedetails.databinding.ItemComboModifierItemBinding
import com.herfy.productpagedetails.databinding.ItemComboModifierSeperatorBinding
import com.herfy.productpagedetails.databinding.ItemModifierGroupHeaderBinding
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding
import com.herfy.productpagedetails.ui.viewholder.ModifierItemOptionViewHolder
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.comboHeaderVisibility
import com.herfy.utils.extensions.comboItemVisibility
import com.herfy.utils.extensions.comboLineVisibility
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.headerVisibility
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.itemVisibility
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ModifierProcessor
import com.herfy.utils.helper.ModifierProcessorCallback
import com.herfy.utils.helper.StringUtil.localize
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupAttributes
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import java.util.Locale

class ComboItemDetailsModifierAdapter(private var itemList: List<ModifierAdapterItem> = mutableListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var onAction: ComboItemDetailsModifierListener? = null
    fun setOnAction(onAction: ComboItemDetailsModifierListener) {
        this.onAction = onAction
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ModifierAdapterItem.VIEW_TYPE_MODIFIER_GROUP -> {
                val binding = ItemComboModifierGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierGroupItemViewHolder(binding)
            }

            ModifierAdapterItem.VIEW_TYPE_MODIFIER_ITEM -> {
                val binding = ItemComboModifierItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierItemViewHolder(binding)
            }

            ModifierAdapterItem.VIEW_TYPE_MODIFIER_LINE -> {
                val binding = ItemComboModifierSeperatorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ModifierLineViewHolder(binding)
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
                            onAction?.onCheckedChanged(
                                !item.modifierItem.attributes.isSelected,
                                modifierGroup = item.modifierGroup,
                                modifierItem = item.modifierItem
                            )
                        }
                        checkLayout.setOnClickListener {
                            onAction?.onCheckedChanged(
                                !item.modifierItem.attributes.isSelected,
                                modifierGroup = item.modifierGroup,
                                modifierItem = item.modifierItem
                            )
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
                        quantityMinus.setOnClickListener {
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

            ModifierAdapterItem.VIEW_TYPE_MODIFIER_LINE -> {
                val item = itemList[position] as ModifierLineAdapterItem
                holder.cast<ModifierLineViewHolder> {
                    it.bind(item)
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

                is ModifierLineAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is ModifierLineAdapterItem -> {
                            oldItem.modifierGroup.id == newItem.modifierGroup.id
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
        private val binding: ItemComboModifierGroupHeaderBinding
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
                itemView.comboHeaderVisibility(isVisibileByConfig)
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

                    text = when {
                        data.attributes.minimum > 0 && data.attributes.maximum > data.attributes.minimum ->{
                            var selectStr = cacheUtil.getTranslations(Text.SELECT_AT_LEAST_X_OPTION, "(Select at least (x) option)")
                            if(data.attributes.minimum > 1){
                                selectStr = cacheUtil.getTranslations(
                                    Text.SELECT_AT_LEAST_X_OPTIONS,
                                    "(Select at least (x) options)"
                                )
                            }
                            selectStr.replace("(x)", data.attributes.minimum.toString())
                        }


                        data.attributes.minimum > 0 && data.attributes.minimum == data.attributes.maximum -> {
                            var selectStr = cacheUtil.getTranslations(Text.SELECT_X_OPTION, "(Select (x) option)")
                            if(data.attributes.maximum > 1){
                                selectStr = cacheUtil.getTranslations(
                                    Text.SELECT_X_OPTIONS,
                                    "(Select (x) options)"
                                )
                            }
                            selectStr.replace("(x)", localize(data.attributes.maximum.toString()))
                        }

                        else -> {
                            var selectStr =  cacheUtil.getTranslations(
                                Text.SELECT_UP_TO_X_OPTION,
                                "(Select up to (x) option)"
                            )
                            if(data.attributes.maximum > 1){
                                selectStr = cacheUtil.getTranslations(
                                    Text.SELECT_UP_TO_X_OPTIONS,
                                    "(Select up to (x) options)"
                                )
                            }
                            selectStr.replace("(x)", localize(data.attributes.maximum.toString()))
                        }
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
        val binding: ItemComboModifierItemBinding
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
                itemView.comboItemVisibility(isVisibileByConfig)
                modifierProcessor.processModifier(item.modifierItem)

                val modifierGroupAttributes: ModifierGroupAttributes = modifierGroup.attributes
                val isModifierItemSelected: Boolean = modifierItem.attributes.isSelected
                val maximum = modifierGroupAttributes.maximum
                val minimum = modifierGroupAttributes.minimum
                val shouldShowQuantity = MenuUtil.shouldShowQuantity(modifierGroup, modifierItem)
                val isCheckBoxVisible = !shouldShowQuantity && (maximum > 1 || minimum == 0)
                val isRadioButtonVisible = !shouldShowQuantity && (maximum == 1 && minimum == 1)
                val isQuantityLayoutVisible = shouldShowQuantity
                val isQuantityDownLayout =  (modifierGroup.attributes.maximum > 1 &&
                        modifierItem.attributes.isSelected && modifierItem.attributes.maximum > 1)
                updateSideButtons(isCheckBoxVisible, isRadioButtonVisible, isQuantityLayoutVisible,isQuantityDownLayout,isModifierItemSelected, modifierGroup,modifierItem, item.isAboveMax)

                quantity.text = when (modifierItem.attributes.quantity > 9) {
                    true -> localize(modifierItem.attributes.quantity.toString())
                    else -> localize(("0" + modifierItem.attributes.quantity))
                }
                val imageUrl = modifierItem.attributes.image
                if (imageUrl != null && !imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) && imageUrl.isNotEmpty() && !imageUrl.lowercase(
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


            }
        }

        private fun updateSideButtons(
            isCheckBoxVisible: Boolean,
            isRadioButtonVisible: Boolean,
            isQuantityLayoutVisible: Boolean,
            isQuantityDownVisible : Boolean,
            isModifierItemSelected: Boolean,
            modifierGroup : ModifierGroup,
            modifierItem: ModifierItem,
            isAboveMax : Boolean
        ) {
            binding.apply {
                if(isRadioButtonVisible){
                    radioButton.visible()
                    checkBox.gone()
                    quantityUp.gone()
                    quantityDownLayout.gone()
                }else if(isCheckBoxVisible){
                    checkBox.visible()
                    radioButton.gone()
                    quantityUp.gone()
                    quantityDownLayout.gone()
                }else if(isQuantityLayoutVisible){
                    quantityDownLayout.gone()
                    radioButton.gone()
                    checkBox.gone()
                    quantityUp.visible()
                    quantityDownLayout.apply {
                        if(modifierItem.attributes.quantity > 1){
                            quantityMinus.visible()
                            quantityDown.gone()
                        }else {
                            quantityMinus.gone()
                            quantityDown.visible()
                        }
                    }
                }else {
                    radioButton.gone()
                    checkBox.gone()
                    quantityUp.gone()
                    quantityDownLayout.gone()
                }

                if (!isAboveMax) {
                    quantityUp.isEnabled = true
                    quantityUp.setImageResource(R.drawable.quantity_up_blue)
                    optionCardView.isEnabled = true
                } else {
                    quantityUp.isEnabled = false
                    quantityUp.setImageResource(R.drawable.quantity_up_blue_gray)
                    optionCardView.isEnabled = false
                }

                if (isModifierItemSelected) {
                    optionCardView.setBackgroundResource(R.drawable.modifier_item_bg_selected_state)
                    radioButton.apply {
                        if(isVisible()) setImageResource(R.drawable.modifier_radio_item_selected)
                    }
                    checkBox.apply {
                        if(isVisible()) setImageResource(R.drawable.modifier_item_checked_selected)
                    }
                } else {
                    optionCardView.setBackgroundResource(R.drawable.modifier_item_bg_unselected_state)
                    radioButton.apply {
                        if(isVisible()) setImageResource(R.drawable.modifier_item_radio_unselected)
                    }
                    checkBox.apply {
                        if(isVisible()) setImageResource(R.drawable.modifier_item_unselected)
                    }
                }

                val quantityUpIsClickable =
                    modifierItem.attributes.quantity < modifierGroup.attributes.maximum
                quantityUp.apply {
                    isFocusable = quantityUpIsClickable
                    isClickable = quantityUpIsClickable
                }

                val quantityDownIsClickable = modifierItem.attributes.quantity != 1
                quantityMinus.apply {
                    isFocusable = quantityDownIsClickable
                    isClickable = quantityDownIsClickable
                }
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
                if(formattedPrice.isNotEmpty()){
                    txtmodifierPrice.apply {
                        text = formattedPrice
                        visible()
                    }
                }else {
                    txtmodifierPrice.gone()
                }


            }
        }
    }

    class ModifierLineViewHolder(val binding : ItemComboModifierSeperatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item : ModifierLineAdapterItem){
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
            itemView.comboLineVisibility(isVisibileByConfig)
        }
    }

    fun updateSelectedModifierItem(
        hasModifierItemsSelected: Boolean,
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem,
        isAboveMax: Boolean,
        modifierVisibilityHashMap : HashMap<String, String>
    ) {
        itemList.filterIsInstance<ModifierGroupAdapterItem>().map { modifierAdapterItem ->
            if (modifierAdapterItem.modifierGroup.id == modifierGroup.id) {
                modifierAdapterItem.modifierGroup = modifierGroup
                modifierAdapterItem.hasSelectedModifierItems = hasModifierItemsSelected
            }
        }
        itemList.filterIsInstance<ModifierItemAdapterItem>().map { modifierAdapterItem ->
            if (modifierAdapterItem.modifierGroup.id == modifierGroup.id) {
                modifierAdapterItem.modifierGroup = modifierGroup
                if (modifierAdapterItem.modifierItem.id == modifierItem.id) {
                    modifierAdapterItem.modifierItem = modifierItem
                }
                modifierAdapterItem.isAboveMax = isAboveMax
            }
        }
        modifierVisibilityHashMap.forEach { (groupId, visiblityValue) ->
            var hasChanges = false
            itemList.filterIsInstance<ModifierGroupAdapterItem>().map { modifierGroupItem ->
                val currentVisibilityModifierValue = modifierGroupItem.modifierGroup.attributes.visibilityConfig?.initialValue
                if(modifierGroupItem.modifierGroup.id == groupId && currentVisibilityModifierValue != null && currentVisibilityModifierValue != visiblityValue) {
                    modifierGroupItem.modifierGroup.attributes.visibilityConfig.initialValue = visiblityValue
                    hasChanges = true
                }
            }
            if(hasChanges){
                itemList.filterIsInstance<ModifierItemAdapterItem>().map { modifierItemAdapterItem ->
                    if (modifierItemAdapterItem.modifierGroup.id == groupId) {
                        modifierItemAdapterItem.modifierGroup.attributes.visibilityConfig.initialValue = visiblityValue
                    }
                }
                itemList.filterIsInstance<ModifierGroupAdapterItem>().map { modifierLineItem ->
                    val currentVisibilityModifierValue = modifierLineItem.modifierGroup.attributes.visibilityConfig?.initialValue
                    if(modifierLineItem.modifierGroup.id == groupId && currentVisibilityModifierValue != null && currentVisibilityModifierValue != visiblityValue) {
                        modifierLineItem.modifierGroup.attributes.visibilityConfig.initialValue = visiblityValue
                    }
                }
            }

        }
        modifierVisibilityHashMap.entries.removeIf{e -> e.value == "hidden" }
        notifyDataSetChanged()
    }
    fun getUpdatedModifierList() : List<ModifierGroup>{
        return when(itemList.isNotEmpty()) {
            true -> {
                val modifierGroupLists = arrayListOf<ModifierGroup>()
                val modifierGroups = itemList.filter { modifierAdapterItem -> modifierAdapterItem.viewType == ModifierAdapterItem.VIEW_TYPE_MODIFIER_GROUP }
                modifierGroups.forEach {
                    it.cast<ModifierGroupAdapterItem> { modifierGroupAdapterItem ->
                        modifierGroupAdapterItem.modifierGroup.modifierItems = getModifierItemsPerModifierGroup(modifierGroupAdapterItem.modifierGroup.id)
                        modifierGroupLists.add(modifierGroupAdapterItem.modifierGroup)
                    }
                }
                modifierGroupLists
            }
            else -> arrayListOf()
        }
    }

    private fun getModifierItemsPerModifierGroup(id : String) : List<ModifierItem>{
       return itemList.filter { modifierAdapterItem -> modifierAdapterItem is ModifierItemAdapterItem && modifierAdapterItem.modifierGroup.id == id }.map { modifierAdapterItem -> (modifierAdapterItem as ModifierItemAdapterItem).modifierItem }.toList()
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