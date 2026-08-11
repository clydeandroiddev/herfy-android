package com.herfy.menu.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.herfy.menu.R
import com.herfy.menu.databinding.CardMenuCategoryBinding
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import java.util.Locale
import kotlin.math.roundToInt

/**
 * [MenuCategoryAdapter] adapter for displaying all the menu items
 */
class MenuCategoryAdapter(
    val onSelect: (Int, MenuCategory) -> Unit
) : RecyclerView.Adapter<MenuCategoryAdapter.MenuCategoryViewHolder>() {

    companion object {
        private const val PAYLOAD_IMAGE_VISIBILITY = "payload_image_visibility"
    }

    private var selected = 0
    private var itemList = mutableListOf<MenuCategory>()
    private var hideCategoryImagesOnScroll = false

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
        fun bind(isSelected: Boolean, menuCategory: MenuCategory, hideImage: Boolean) {

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

                setImageVisibility(hideImage)

                tvName.apply {
                    text = menuCategory.getName(LocaleUtil.getInstance().language)
                    if(isSelected) {
                        setTypeface(
                            ResourcesCompat.getFont(itemView.context, com.herfy.commonstyles.R.font.inter_semi_bold),
                            Typeface.BOLD
                        )
                    } else {
                        setTypeface(
                            ResourcesCompat.getFont(itemView.context, com.herfy.commonstyles.R.font.inter_regular),
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

        fun setImageVisibility(hidden: Boolean) {
            binding.ivImage.visibility = if (hidden) View.GONE else View.VISIBLE

            val nameLayoutParams = binding.tvName.layoutParams as? MarginLayoutParams
            val titleTopMargin = if (hidden) dpToPx(6) else dpToPx(8)
            val titleBottomMargin = dpToPx(14)
            if (nameLayoutParams != null &&
                (nameLayoutParams.topMargin != titleTopMargin || nameLayoutParams.bottomMargin != titleBottomMargin)
            ) {
                nameLayoutParams.topMargin = titleTopMargin
                nameLayoutParams.bottomMargin = titleBottomMargin
                binding.tvName.layoutParams = nameLayoutParams
            }

            val itemLayoutParams = binding.layoutMenuCategory.layoutParams
            val itemHeight = if (hidden) dpToPx(74) else dpToPx(118)
            if (itemLayoutParams != null && itemLayoutParams.height != itemHeight) {
                itemLayoutParams.height = itemHeight
                binding.layoutMenuCategory.layoutParams = itemLayoutParams
            }
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * itemView.resources.displayMetrics.density).roundToInt()
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
        holder.bind(selected == position, menuCategory, hideCategoryImagesOnScroll)
    }

    override fun onBindViewHolder(
        holder: MenuCategoryViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_IMAGE_VISIBILITY)) {
            val menuCategory = itemList[position]
            holder.layoutMenuCategory.setOnClickListener {
                onSelect(position, menuCategory)
            }
            holder.setImageVisibility(hideCategoryImagesOnScroll)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    /**
     * Returns the number of addresses
     */
    override fun getItemCount(): Int = itemList.size

    fun addData(items: List<MenuCategory>) {
        this.itemList.clear()
        this.itemList.addAll(items)
        notifyDataSetChanged()
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

    fun setCategoryImagesHidden(hidden: Boolean) {
        if (hideCategoryImagesOnScroll == hidden) {
            return
        }
        hideCategoryImagesOnScroll = hidden
        if (itemList.isNotEmpty()) {
            notifyItemRangeChanged(0, itemList.size, PAYLOAD_IMAGE_VISIBILITY)
        }
    }

}