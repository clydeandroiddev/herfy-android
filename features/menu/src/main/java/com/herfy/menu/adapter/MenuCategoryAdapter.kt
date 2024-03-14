package com.herfy.menu.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.herfy.menu.R
import com.herfy.menu.databinding.CardMenuCategoryBinding
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import java.util.Locale

/**
 * [MenuCategoryAdapter] adapter for displaying all the menu items
 */
class MenuCategoryAdapter(
    val onSelect: (Int, MenuCategory) -> Unit
) : RecyclerView.Adapter<MenuCategoryAdapter.MenuCategoryViewHolder>() {

    private var selected = 0
    private var itemList: List<MenuCategory> = mutableListOf()

    /**
     * [MenuCategoryViewHolder] for binding the views
     * @param binding as [CardMenuCategoryBinding] the views to be binded
     */
    class MenuCategoryViewHolder(
        private val binding: CardMenuCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val layoutMenuCategory = binding.layoutMenuCategory

        /**
         * bind the [MenuCategory] to display
         */
        fun bind(isSelected: Boolean, menuCategory: MenuCategory) {

            binding.apply {

                layoutMenuCategory.setBackgroundResource(
                    if(isSelected) {
                        R.drawable.background_menu_category_selected
                    } else {
                        R.drawable.background_menu_category_unselected
                    }
                )

                var imageUrl = menuCategory.image
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

                tvName.apply {
                    text = menuCategory.getName(LocaleUtil.getInstance().language)
                    if(isSelected) {
                        setTypeface(
                            ResourcesCompat.getFont(itemView.context, R.font.inter_semi_bold),
                            Typeface.BOLD
                        )
                    } else {
                        setTypeface(
                            ResourcesCompat.getFont(itemView.context, R.font.inter_regular),
                            Typeface.NORMAL
                        )
                    }
                }

                ivIndicator.visibility = if(isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            }

        }
    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuCategoryViewHolder {

        /**
         * inflates the current layout with view holder
         */
        CardMenuCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also {
                return MenuCategoryViewHolder(it)
            }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: MenuCategoryViewHolder, position: Int) {
        val menuCategory = itemList[position]
        holder.layoutMenuCategory.setOnClickListener {
            onSelect(position, menuCategory)
        }
        holder.bind(selected == position, menuCategory)
    }

    /**
     * Returns the number of addresses
     */
    override fun getItemCount(): Int = itemList.size

    fun addData(items: List<MenuCategory>) {
        val diffResult = DiffUtil.calculateDiff(Diff(itemList, items))
        this.itemList = items
        diffResult.dispatchUpdatesTo(this)
    }

    fun select(position: Int) {
        val previous = selected
        selected = position
        notifyItemChanged(previous)
        notifyItemChanged(selected)
    }

    fun getSelected() = selected

    fun getPosition(menuCategoryId: String): Int? {
        for((index, menuCategory) in itemList.withIndex()) {
            if(menuCategory.id == menuCategoryId) {
                return index
            }
        }
        return null
    }

    class Diff(private val old: List<MenuCategory>, private val newItem: List<MenuCategory>) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPos: Int, newPosition: Int): Boolean {
            val oldItem = old[oldItemPos]
            val newItem = newItem[newPosition]
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition] == newItem[newItemPosition]

        override fun getOldListSize(): Int = old.size

        override fun getNewListSize(): Int = newItem.size
    }

}