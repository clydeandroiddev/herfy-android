package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.herfy.model.combo.ComboComponentAdapterItem
import com.herfy.model.combo.ComponentAdapterItem
import com.herfy.model.combo.ComponentModifierAdapterItem
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemComboComponentItemBinding
import com.herfy.productpagedetails.databinding.ItemModifierGroupHeaderBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil

import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.StringUtil.localize
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import java.util.Locale

class ComboMealComponentAdapter(private var itemList: List<ComboComponentAdapterItem> = mutableListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var onAction: ComboComponentListener? = null
    fun setOnAction(onAction: ComboComponentListener) {
        this.onAction = onAction
    }

    //Event
    interface ComboComponentListener {
        fun onComponentCheckedChanged(
            isSelected: Boolean,
            menuItemComponent: MenuItemComponents,
            componentModifierItem: ComponentModifierItem,
            menuItemComponentPos: Int,
            componentModifierItemPos: Int
        )

        fun onComponentQuantityChanged(
            add: Boolean,
            menuItemComponent: MenuItemComponents,
            componentModifierItem: ComponentModifierItem,
            isQuantityVisible: Boolean,
            menuItemComponentPos: Int,
            componentModifierItemPos: Int
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ComboComponentAdapterItem.VIEW_TYPE_MENU_ITEM_COMPONENT -> {
                val binding = ItemModifierGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ComponentViewHolder(binding)
            }

            ComboComponentAdapterItem.VIEW_TYPE_COMPONENT_MODIFIER_ITEM -> {
                val binding = ItemComboComponentItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ComponentModifierItemViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemViewType(position: Int): Int = itemList[position].viewType

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ComboComponentAdapterItem.VIEW_TYPE_MENU_ITEM_COMPONENT -> {
                val item = itemList[position] as ComponentAdapterItem
                holder.cast<ComponentViewHolder> { it.bind(item) }
            }

            ComboComponentAdapterItem.VIEW_TYPE_COMPONENT_MODIFIER_ITEM -> {
                val item = itemList[position] as ComponentModifierAdapterItem
                holder.cast<ComponentModifierItemViewHolder> {
                    it.bind(item)

                    it.binding.apply {
                        clickableLayout.setOnClickListener {
                            onAction?.onComponentCheckedChanged(
                                !item.componentModifierItem.selected,
                                menuItemComponent = item.menuItemComponents,
                                componentModifierItem = item.componentModifierItem,
                                item.menuItemComponentPos,
                                item.componentModifierItemPos
                            )
                        }
                        checkLayout.setOnClickListener {
                            onAction?.onComponentCheckedChanged(
                                !item.componentModifierItem.selected,
                                menuItemComponent = item.menuItemComponents,
                                componentModifierItem = item.componentModifierItem,
                                item.menuItemComponentPos,
                                item.componentModifierItemPos
                            )
                        }
                        quantityUp.setOnClickListener {
                            onAction?.onComponentQuantityChanged(
                                true,
                                menuItemComponent = item.menuItemComponents,
                                componentModifierItem = item.componentModifierItem,
                                quantityDown.isVisible(),
                                item.menuItemComponentPos,
                                item.componentModifierItemPos
                            )
                        }
                        quantityDown.setOnClickListener {
                            onAction?.onComponentQuantityChanged(
                                false,
                                menuItemComponent = item.menuItemComponents,
                                componentModifierItem = item.componentModifierItem,
                                quantityDown.isVisible(),
                                item.menuItemComponentPos,
                                item.componentModifierItemPos
                            )
                        }
                    }
                }
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    fun addData(items: List<ComboComponentAdapterItem>) {
        val diffResult = DiffUtil.calculateDiff(Diff(itemList, items))
        this.itemList = items
        diffResult.dispatchUpdatesTo(this)
    }

    class Diff(
        private val old: List<ComboComponentAdapterItem>,
        private val newItem: List<ComboComponentAdapterItem>
    ) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPos: Int, newPosition: Int): Boolean {
            return when (val oldItem = old[oldItemPos]) {
                is ComponentAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is ComponentAdapterItem -> {
                            oldItem.menuItemComponents.id == newItem.menuItemComponents.id
                        }

                        else -> false
                    }
                }

                is ComponentModifierAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is ComponentModifierAdapterItem -> {
                            oldItem.componentModifierItem.id == newItem.componentModifierItem.id
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

    class ComponentViewHolder(
        private val binding: ItemModifierGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val cacheUtil = CacheUtil.getInstance()

        fun bind(item: ComponentAdapterItem) {
            binding.apply {

                val data = item.menuItemComponents
                modifierImage.gone()
                name.text = data.name.toTranslate()

                select.apply {
                    visible()
                    text = when {
                        data.minimum > 0 && data.maximum > data.minimum -> cacheUtil.getTranslations(
                            Text.SELECT_AT_LEAST_X_OPTIONS,
                            "(Select at least (x) options)"
                        ).replace("(x)", localize(data.minimum.toString()))

                        data.minimum > 0 && data.minimum == data.maximum -> cacheUtil.getTranslations(
                            Text.SELECT_X_OPTIONS,
                            "(Select (x) options)"
                        ).replace("(x)", localize(data.maximum.toString()))

                        else -> cacheUtil.getTranslations(
                            Text.SELECT_UP_TO_X_OPTIONS,
                            "(Select up to (x) options)"
                        ).replace("(x)", localize(data.maximum.toString()))
                    }
                }
                minimum.text = cacheUtil.getTranslations(Text.REQUIRED, "Required")
                if (item.hasSelectedModifierItems) {
                    checkBox.visible()
                    btnMinimum.gone()
                } else {
                    checkBox.gone()
                    if (data.minimum >= 1) {
                        btnMinimum.visible()
                    } else {
                        btnMinimum.gone()
                    }
                }

            }
        }
    }

    class ComponentModifierItemViewHolder(
        val binding: ItemComboComponentItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ComponentModifierAdapterItem) {
            binding.apply {
                val menuItemComponent = item.menuItemComponents
                val componentModifierItem = item.componentModifierItem

                val context = itemView.context
                var imageUrl: String? = componentModifierItem.image

                val hasModifiers = componentModifierItem.modifierGroups.size >= 1
                var hasSelectedModifiers = false

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

                if (hasModifiers) {
                    val selectedLists =
                        fetchSelectedModifierItemsPerItem(componentModifierItem.modifierGroups)
                    hasSelectedModifiers = selectedLists.isNotEmpty()
                    if (hasSelectedModifiers) {
                        val selectedModifiersPerItemAdapter =
                            SelectedModifiersPerItemAdapter(selectedLists)
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
                    } else {
                        rvSelectedModifiers.gone()
                    }
                } else {
                    rvSelectedModifiers.gone()
                }

                txtModifierItemName.text = componentModifierItem.name.toTranslate()
                txtmodifierPrice.visible()
                val componentModifierItemPrice =
                    if (componentModifierItem.price != 0.0) componentModifierItem.price else componentModifierItem.attributes.price
                if (componentModifierItemPrice > 0) {
                    txtmodifierPrice.text = StringUtil.getFormattedPrice(componentModifierItemPrice)
                } else {
                    txtmodifierPrice.text =
                        CacheUtil.getInstance().getTranslations(Text.FREE, "Free");
                }

                quantity.text = when (componentModifierItem.quantity > 9) {
                    true -> localize(componentModifierItem.quantity.toString())
                    else -> localize(("0" + componentModifierItem.quantity))
                }

                updateSideButtons(
                    componentModifierItem,
                    menuItemComponent,
                    item.isAboveMax,
                    hasSelectedModifiers
                )
            }
        }


        private fun updateSideButtons(
            componentModifierItem: ComponentModifierItem,
            menuItemComponent: MenuItemComponents,
            isAboveMax: Boolean,
            hasSelectedModifiers: Boolean
        ) {
            val hasModifiers = componentModifierItem.modifierGroups.size >= 1
            val maximum = menuItemComponent.maximum
            val minimum = menuItemComponent.minimum
            val isComponentSelected = componentModifierItem.selected
            val shouldShowQuantity =
                MenuUtil.shouldShowComboItemQuantity(menuItemComponent, componentModifierItem)
            val isCheckBoxVisible =
                !hasModifiers && !shouldShowQuantity && (maximum > 1 || minimum == 0)
            val isRadioButtonVisible =
                !hasModifiers && !shouldShowQuantity && (maximum == 1 && minimum == 1)
            val isQuantityDownLayout = (menuItemComponent.maximum > 1 &&
                    componentModifierItem.selected && componentModifierItem.maximum > 1)
            binding.apply {
                val context = itemView.context

                if (hasModifiers || isRadioButtonVisible || isCheckBoxVisible) {
                    checkLayout.visible()
                    if (hasModifiers) {
                        ivRightIcon.visible()
                        ivRightIcon.setImageResource(R.drawable.ic_arrow_right_tail_blue)
                        radioButton.gone()
                        checkBox.gone()
                    } else if (isRadioButtonVisible) {
                        radioButton.visible()
                        checkBox.gone()
                        ivRightIcon.gone()
                    } else if (isCheckBoxVisible) {
                        checkBox.visible()
                        radioButton.gone()
                        ivRightIcon.gone()
                    }
                } else {
                    checkLayout.gone()
                }

                if (shouldShowQuantity) {
                    quantityContainer.visible()
                    quantityDownLayout.gone()
                    quantityUp.visible()
                    quantityDownLayout.apply {
                        if (isQuantityDownLayout) visible() else gone()
                    }
                } else {
                    quantityContainer.gone()
                    quantityUp.gone()
                    quantityDownLayout.gone()
                }

                if (isComponentSelected) {
                    optionCardView.strokeWidth = 1
                    optionCardView.strokeColor = context.getColor(R.color.background_color_blue)
                    optionCardView.backgroundTintList =
                        ColorStateList.valueOf(context.getColor(R.color.card_selected_bg))
                    if (hasModifiers && hasSelectedModifiers) {
                        ivRightIcon.setImageResource(R.drawable.ic_edit_blue)
                    } else if (radioButton.isVisible()) {
                        radioButton.isChecked = true
                    } else if (checkBox.isVisible()) {
                        checkBox.setImageResource(R.drawable.modifier_item_checked_selected)
                    }
                } else {
                    optionCardView.strokeColor = Color.WHITE
                    optionCardView.backgroundTintList =
                        ColorStateList.valueOf(context.getColor(R.color.white))
                    if (hasModifiers) {
                        ivRightIcon.setImageResource(R.drawable.ic_arrow_right_tail_blue)
                    } else if (radioButton.isVisible()) {
                        radioButton.isChecked = false
                    } else if (checkBox.isVisible()) {
                        checkBox.setImageResource(R.drawable.modifier_item_unselected)
                    }
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

                val quantityUpIsClickable =
                    componentModifierItem.quantity < menuItemComponent.maximum
                quantityUp.apply {
                    isFocusable = quantityUpIsClickable
                    isClickable = quantityUpIsClickable
                }

                val quantityDownIsClickable = componentModifierItem.quantity != 1
                quantityDown.apply {
                    isFocusable = quantityDownIsClickable
                    isClickable = quantityDownIsClickable
                }
            }


        }


        private fun fetchSelectedModifierItemsPerItem(modifierGroups: List<ModifierGroup>): List<String> {
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
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateSelectedComponent(
        hasSelectedModifiers: Boolean,
        menuItemComponent: MenuItemComponents,
        componentModifierItem: ComponentModifierItem,
        isAboveMax: Boolean
    ) {
        itemList.filterIsInstance<ComponentAdapterItem>().map { componentAdapterItem ->
            if (componentAdapterItem.menuItemComponents.id == menuItemComponent.id) {
                componentAdapterItem.menuItemComponents = menuItemComponent
                componentAdapterItem.hasSelectedModifierItems = hasSelectedModifiers
            }
        }

        itemList.filterIsInstance<ComponentModifierAdapterItem>()
            .map { componentModifierAdapterItem ->
                if (componentModifierAdapterItem.menuItemComponents.id == menuItemComponent.id) {
                    componentModifierAdapterItem.menuItemComponents = menuItemComponent
                    if (componentModifierAdapterItem.componentModifierItem.id == componentModifierItem.id) {
                        componentModifierAdapterItem.componentModifierItem = componentModifierItem
                    }
                    componentModifierAdapterItem.isAboveMax = isAboveMax
                }
            }

        notifyDataSetChanged()
    }

}