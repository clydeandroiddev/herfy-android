package com.herfy.newmenu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.utils.constants.Text
import com.herfy.databinding.ItemEmptyMenuBinding
import com.herfy.databinding.ItemNewMenuCategoryBinding
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.herfy.utils.helper.LocaleUtil
import java.util.LinkedList
import com.herfy.utils.helper.*

/**
 * [MenuCategoriesAdapter] adapter for displaying all the menu categories and items
 */
class MenuCategoriesAdapter(
    private var menuCategories: Array<MenuCategory>,
    private val onSelect: (MenuCategory, MenuItem) -> Unit,
    private val onAddRemoveFavorite: (Boolean, MenuCategory, MenuItem) -> Unit,
    private val onAddToCart: (ImageView, MenuItem, Double, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isCollapsed = false

    private val VIEW_TYPE_MENU_EMPTY = 1
    private val VIEW_TYPE_MENU_CATEGORY = 2

    private val menuItemsAdapters = mutableMapOf<String, MenuItemsAdapter>()
    private val sharedViewPool = RecyclerView.RecycledViewPool()

    class EmptyMenuItemViewHolder(
        private val binding: ItemEmptyMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(isCollapsed: Boolean) {
            if (isCollapsed) {
                binding.topView.gone()
            } else {
                binding.topView.visible()
            }
            binding.emptyMessage.text = CacheUtil.getInstance().getTranslations(
                Text.MENU_EMPTY_STATE_MESSAGE,
                "Thank you for visiting.\n" +
                        "We are currently closed.\n" +
                        "Please visit during the operating hours."
            )
        }

    }

    /**
     * [MenuCategoryViewHolder] for binding the views
     * @param binding as [ItemNewMenuCategoryBinding] the views to be binded
     */
    class MenuCategoryViewHolder(
        private val binding: ItemNewMenuCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * bind the [MenuCategory] to display
         */
        fun bind(
            menuCategory: MenuCategory,
            menuItemsAdapter: MenuItemsAdapter,
            sharedViewPool: RecyclerView.RecycledViewPool
        ) {

            binding.name.text = menuCategory.getName(LocaleUtil.getInstance().language)

            binding.menuItemsRecyclerView.apply {
                if (layoutManager == null) {
                    layoutManager = LinearLayoutManager(itemView.context)
                    setHasFixedSize(true)
                    setRecycledViewPool(sharedViewPool)
                }
                if (adapter !== menuItemsAdapter) {
                    adapter = menuItemsAdapter
                }
            }

        }
    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            VIEW_TYPE_MENU_EMPTY -> ItemEmptyMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
                return EmptyMenuItemViewHolder(it)
            }
            else ->  ItemNewMenuCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
                return MenuCategoryViewHolder(it)
            }
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is EmptyMenuItemViewHolder) {
            holder.bind(isCollapsed)
        } else if(holder is MenuCategoryViewHolder) {
            if (position < menuCategories.size) {
                val menuCategory = menuCategories[position]
                val menuItemsAdapter = menuItemsAdapters.getOrPut(menuCategory.id) {
                    MenuItemsAdapter(
                        menuCategory = menuCategory,
                        menuItems = buildFilteredMenuItems(menuCategory),
                        onSelect = onSelect,
                        onAddRemoveFavorite = onAddRemoveFavorite,
                        onAddToCart = onAddToCart
                    )
                }
                holder.bind(
                    menuCategory = menuCategory,
                    menuItemsAdapter = menuItemsAdapter,
                    sharedViewPool = sharedViewPool
                )
            }
        }
    }

    private fun buildFilteredMenuItems(menuCategory: MenuCategory): Array<MenuItem> {
        val isStoreSelected = CacheUtil.getInstance().orderStore != null
        val filteredMenuItems = LinkedList<MenuItem>()
        for (menuItem in menuCategory.menuItems) {
            if (menuItem.enabled == 1 && (!isStoreSelected || !menuItem.excludedLocations.contains(
                    CacheUtil.getInstance().orderStore.id
                ))
            ) {
                filteredMenuItems.add(menuItem)
            }
        }
        if (filteredMenuItems.isNotEmpty()) {
            filteredMenuItems.sortBy { it.displayOrder }
        }
        return filteredMenuItems.toTypedArray()
    }

    /**
     * Returns the number of menu categories
     */
    override fun getItemCount(): Int {
        return if(menuCategories.isNotEmpty()) {
            menuCategories.size
        } else {
            1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(menuCategories.isEmpty()) {
            VIEW_TYPE_MENU_EMPTY
        } else {
            VIEW_TYPE_MENU_CATEGORY
        }
    }

    fun updateMenuCategories(menuCategories: Array<MenuCategory>) {
        menuItemsAdapters.clear()
        this.menuCategories = menuCategories
        notifyDataSetChanged()
    }

    fun updateMenuItems(position: Int) {
        if (position in menuCategories.indices) {
            menuItemsAdapters[menuCategories[position].id]?.notifyDataSetChanged()
        }
    }

    fun setCollapsed(value: Boolean) {
        if(menuCategories.isEmpty()) {
            isCollapsed = value
            notifyDataSetChanged()
        }
    }

}