package com.herfy.menu.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.herfy.menu.R
import com.herfy.menu.databinding.AdapterMenuCategoryBinding
import com.herfy.menu.databinding.AdapterMenuItemBinding
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import java.util.Locale


/**
 * [MenuCategoryItemAdapter] adapter for displaying all the categories
 */
class MenuCategoryItemAdapter(
    private val onSelect: (MenuCategory, MenuItem) -> Unit,
    private val onFavorite: (MenuItem) -> Unit,
    private val onAdd: (MenuCategory, MenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemList: List<MenuAdapterItem> = mutableListOf()

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    /**
     * [MenuCategoryViewHolder] for binding the views
     * @param binding as [AdapterMenuCategoryBinding] the views to be binded
     */
    class MenuCategoryViewHolder(
        private val binding: AdapterMenuCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuCategoryAdapterItem) {
            binding.apply {

                var imageUrl = item.menuCategory.image
                if (imageUrl == null || imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    imageUrl = Values.DEFAULT_IMAGE_URL
                }

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage)

                tvName.text = item.menuCategory.getName(LocaleUtil.getInstance().language)

            }
        }
    }

    /**
     * [MenuItemViewHolder] for binding the views
     * @param binding as [AdapterMenuItemBinding] the views to be binded
     */
    class MenuItemViewHolder(
        private val binding: AdapterMenuItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val layoutMenuItem = binding.layoutMenuItem
        val btnFavorite = binding.btnFavorite
        val btnAdd = binding.btnAdd

        fun bind(item: MenuItemAdapterItem) {
            binding.apply {

                val menuItem = item.menuItem

                var imageUrl = menuItem.image
                if (imageUrl == null || imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    imageUrl = Values.DEFAULT_IMAGE_URL
                }

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage)

                val caloriesValue = menuItem.calories
                val calorieCount = menuItem.calorieCount
                tvCalories.text = if (calorieCount != 0) {
                    layoutCalories.visibility = View.VISIBLE
                    "$calorieCount"
                } else if (caloriesValue != null && caloriesValue.isNotEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                    layoutCalories.visibility = View.VISIBLE
                    caloriesValue
                } else {
                    layoutCalories.visibility = View.GONE
                    ""
                }

                binding.btnFavorite.apply {
                    visibility = if(AuthUtil.getInstance().isSignedIn) {
                        setImageResource(
                            if (CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                                R.drawable.ic_favorite_filled
                            } else {
                                R.drawable.ic_favorite_empty
                            }
                        )
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

                tvName.text = menuItem.getName(LocaleUtil.getInstance().language)
                tvDescription.text = menuItem.getDescription(LocaleUtil.getInstance().language)

                val price = menuItem.getPrice(CacheUtil.getOrderStoreId())
                tvPrice.text = String.format("SAR %.2f", price)

            }
        }
    }


    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MENU_CATEGORY -> {
                val binding = AdapterMenuCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MenuCategoryViewHolder(binding)
            }
            VIEW_TYPE_MENU_ITEM -> {
                val binding = AdapterMenuItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MenuItemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_MENU_CATEGORY -> {
                val item = itemList[position] as MenuCategoryAdapterItem
                val menuCategoryHolder = holder as MenuCategoryViewHolder
                menuCategoryHolder.bind(item)
            }
            VIEW_TYPE_MENU_ITEM -> {
                val item = itemList[position] as MenuItemAdapterItem
                val menuItemHolder = holder as MenuItemViewHolder
                menuItemHolder.apply {
                    layoutMenuItem.setOnClickListener {
                        onSelect(item.menuCategory, item.menuItem)
                    }
                    btnFavorite.apply {
                        setOnClickListener {
                            onFavorite(item.menuItem)
                            setImageResource(
                                if (CacheUtil.getInstance().favorites.contains(item.menuItem.id)) {
                                    R.drawable.ic_favorite_empty
                                } else {
                                    R.drawable.ic_favorite_filled
                                }
                            )
                        }
                    }
                    btnAdd.setOnClickListener {
                        onAdd(item.menuCategory, item.menuItem)
                    }
                    bind(item)
                }
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    /**
     * Returns the number of addresses
     */
    override fun getItemCount(): Int = itemList.size

    fun addData(items: List<MenuAdapterItem>) {
        val diffResult = DiffUtil.calculateDiff(Diff(itemList, items))
        this.itemList = items
        diffResult.dispatchUpdatesTo(this)
    }

    fun getMenuItemCategory(position: Int): MenuCategory? {
        if(position < itemList.size) {
            if(itemList[position] is MenuItemAdapterItem) {
                return (itemList[position] as MenuItemAdapterItem).menuCategory
            }
        }
        return null
    }

    class Diff(private val old: List<MenuAdapterItem>, private val newItem: List<MenuAdapterItem>) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPos: Int, newPosition: Int): Boolean {
            return when (val oldItem = old[oldItemPos]) {
                is MenuCategoryAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is MenuCategoryAdapterItem -> {
                            oldItem.menuCategory.id == newItem.menuCategory.id
                        }
                        else -> false
                    }
                }
                is MenuItemAdapterItem -> {
                    when (val newItem = newItem[newPosition]) {
                        is MenuItemAdapterItem -> {
                            oldItem.menuItem.id == newItem.menuItem.id
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

    sealed class MenuAdapterItem {
        abstract val viewType: Int
    }

    data class MenuCategoryAdapterItem(
        val menuCategory: MenuCategory
    ) : MenuAdapterItem() {
        override val viewType: Int = VIEW_TYPE_MENU_CATEGORY
    }

    data class MenuItemAdapterItem(
        val menuCategory: MenuCategory,
        val menuItem: MenuItem
    ) : MenuAdapterItem() {
        override val viewType: Int = VIEW_TYPE_MENU_ITEM
    }

    companion object {
        private const val VIEW_TYPE_MENU_CATEGORY = 0
        private const val VIEW_TYPE_MENU_ITEM = 1
    }
}
