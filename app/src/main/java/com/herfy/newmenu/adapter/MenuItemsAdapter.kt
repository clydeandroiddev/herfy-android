package com.herfy.newmenu.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.R
import com.herfy.auth.views.SignInActivity
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.databinding.ItemMenuItemNewBinding
import com.herfy.ext.cast
import com.herfy.firebase.FirebaseService
import com.herfy.main.MainActivity
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.util.StringUtil
import com.herfy.views.SlantedTextView
import com.herfy.utils.helper.*
import com.herfy.utils.menu.MenuUtil

/**
 * [MenuItemsAdapter] adapter for displaying all the menu categories and items
 */
class MenuItemsAdapter(
    private val menuCategory: MenuCategory,
    private var menuItems: Array<MenuItem>,
    private val onSelect: (MenuCategory, MenuItem) -> Unit,
    private val onAddRemoveFavorite: (Boolean, MenuCategory, MenuItem) -> Unit,
    private val onAddToCart: (ImageView, MenuItem, Double, Int) -> Unit
) : RecyclerView.Adapter<MenuItemsAdapter.MenuItemViewHolder>() {

    /**
     * [MenuItemViewHolder] for binding the views
     * @param binding as [ItemMenuItemNewBinding] the views to be binded
     */
    class MenuItemViewHolder(
        private val binding: ItemMenuItemNewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val favoriteColor = Color.parseColor("#F22424")
        private val notFavoriteColor = Color.parseColor("#DEDEDE")

        /**
         * bind the [MenuItem] to display
         */
        fun bind(
            menuCategory: MenuCategory,
            menuItem: MenuItem,
            onSelect: (MenuCategory, MenuItem) -> Unit,
            onAddRemoveFavorite: (Boolean, MenuCategory, MenuItem) -> Unit,
            onAddToCart: (ImageView, MenuItem, Double, Int) -> Unit
        ) {

            val primaryColor = fetchAccentColor()
            val primaryColorStateList = ColorStateList.valueOf(primaryColor)
            val disabledStateList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.disabled_tint))
            val soldOutColor = ContextCompat.getColor(itemView.context, R.color.soldout)
            val notSoldOutColor = ContextCompat.getColor(itemView.context, R.color.transparent)

            binding.root.setOnLongClickListener { false }

            setFavoriteIcon(menuItem)

            val price = menuItem.getPrice(CacheUtil.getOrderStoreId())

            binding.container.setOnClickListener {
                onSelect(menuCategory, menuItem)
                FirebaseECommerceEventUtil.selectItemEvent(itemView.context, menuItem, menuCategory, price)
            }
            binding.imageItem.setOnClickListener {
                onSelect(menuCategory, menuItem)
            }
            var imageUrl = menuItem.image
            imageUrl = if (imageUrl == null || imageUrl.equals("null", ignoreCase = true) || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                Values.DEFAULT_IMAGE_URL
            } else {
                StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl)
            }
            Glide.with(itemView.context)
                .load(imageUrl)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.centerCropTransform())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?, isFirstResource: Boolean): Boolean {
                        binding.imageItem.visibility = View.INVISIBLE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        binding.imageItem.visibility = View.VISIBLE
                        return false
                    }
                }).into(binding.imageItem)
            val description = menuItem.getDescription(LocaleUtil.getInstance().language)
            binding.itemDescription.text = if (description != null && !description.isEmpty() && !description.equals("null", ignoreCase = true)) {
                description
            } else {
                ""
            }

            val caloriesValue = menuItem.calories
            val calorieCount = menuItem.calorieCount
            var calorieStr = ""
            if (caloriesValue != null && !caloriesValue.isEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                calorieStr = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
            } else if (calorieCount != 0) {
                calorieStr = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
            }
            if(calorieStr.isNotEmpty()){
                binding.cvCalories.visibility = View.VISIBLE
                binding.itemCalories.text = calorieStr
            }



            binding.itemPrice.text = StringUtil.getFormattedPrice(price)
            if (price > 0) {
                binding.itemPrice.visibility = View.VISIBLE
            } else {
                binding.itemPrice.visibility = View.INVISIBLE
            }
            binding.itemName.text = menuItem.getName(LocaleUtil.getInstance().language)
            binding.favoriteCard.visibility = if (AuthUtil.getInstance().isSignedIn && !menuItem.isCombo) View.VISIBLE else View.INVISIBLE

            setFavorite(menuItem)
            binding.favoriteContainer.setOnClickListener {
                if (AuthUtil.getInstance().isSignedIn) {
                    itemView.context?.cast<MainActivity> {
                        it.showLoadingDialog()
                    }
                    if (CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                        binding.favoriteIcon.setColorFilter(notFavoriteColor)
                        onAddRemoveFavorite(false, menuCategory, menuItem)
                    } else {
                        binding.favoriteIcon.setColorFilter(favoriteColor)
                        onAddRemoveFavorite(true, menuCategory, menuItem)
                    }
                } else {
                    CacheUtil.getInstance().favoriteMenuCategory = menuCategory
                    CacheUtil.getInstance().favoriteMenuItem = menuItem
                    itemView.context.apply {
                        val intent = Intent(this, SignInActivity::class.java)
                        intent.putExtra("favorite_home", true)
                        this.startActivity(intent)
                    }
                }
            }

            binding.addToBag.text = CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag")
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                binding.addToBag.backgroundTintList = disabledStateList
            } else {
                binding.addToBag.setBackgroundResource(R.drawable.background_add_to_bag)
            }
            binding.addToBag.setOnClickListener { v: View? ->
                if (CacheUtil.getInstance().application.attributes.menuOnly) {
                    FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(
                        Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), binding.addToBag.context)
                    return@setOnClickListener
                }
                onAddToCart(binding.imageItem, menuItem, price, 1)
            }
            if (LocaleUtil.getInstance().isArabic) {
                binding.stvSoldout.mode = SlantedTextView.MODE_LEFT
            } else binding.stvSoldout.mode = SlantedTextView.MODE_RIGHT

            if(menuItem.listPrice > 0.0) {
                val formattedOldPrice = StringUtil.getFormattedPrice(menuItem.listPrice)
                val oldPrice =  SpannableString(formattedOldPrice);
                oldPrice.setSpan(StrikethroughSpan(),0, formattedOldPrice.length,0)
                binding.oldPrice.text = oldPrice
                binding.oldPrice.visibility = View.VISIBLE
                binding.discountIndicator.visibility = View.VISIBLE
            } else {
                binding.oldPrice.visibility = View.GONE
                binding.discountIndicator.visibility = View.GONE
            }

            if (CacheUtil.getOrderStoreId() != null && menuItem.outOfStockLocations.contains(
                    CacheUtil.getOrderStoreId())) {
                binding.stvSoldout.text = CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT")
                binding.stvSoldout.visibility = View.VISIBLE
                binding.imageItem.setColorFilter(soldOutColor)
                binding.addToBag.backgroundTintList = disabledStateList
                binding.addToBag.isEnabled = false
            } else {
                binding.imageItem.setColorFilter(notSoldOutColor)
                binding.addToBag.backgroundTintList = primaryColorStateList
                binding.stvSoldout.visibility = View.GONE
                binding.addToBag.isEnabled = true
            }



        }

        private fun fetchAccentColor(): Int {
            val typedValue = TypedValue()
            val a = itemView.context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
            val color = a.getColor(0, 0)
            a.recycle()
            return color
        }


        private fun setFavoriteIcon(menuItem: MenuItem) {
            binding.favoriteIcon.setColorFilter(if (CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                favoriteColor
            } else {
                notFavoriteColor
            })
        }

        fun setFavorite(menuItem: MenuItem) {
            if (AuthUtil.getInstance().isSignedIn && !menuItem.isCombo && CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                binding.favoriteIcon.setColorFilter(favoriteColor)
            } else {
                binding.favoriteIcon.setColorFilter(notFavoriteColor)
            }
        }

    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {

        /**
         * inflates the current layout with view holder
         */
        ItemMenuItemNewBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            return MenuItemViewHolder(it)
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        if(position < menuItems.size) {
            holder.bind(
                menuCategory = menuCategory,
                menuItem = menuItems[position],
                onSelect = onSelect,
                onAddRemoveFavorite = onAddRemoveFavorite,
                onAddToCart = onAddToCart
            )
        }
    }

    /**
     * Returns the number of menu items
     */
    override fun getItemCount(): Int = menuItems.size

    fun updateMenuItems(menuItems: Array<MenuItem>) {
        this.menuItems = menuItems
        notifyDataSetChanged()
    }

}