package com.herfy.newmenu.viewholder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
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
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil.Companion.addToCartEvent
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.auth.views.SignInActivity
import com.herfy.common.view.SlantedTextView
import com.herfy.databinding.ItemMenuItemNewBinding
import com.herfy.main.MainActivity
import com.herfy.menu.MenuContract
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter
import com.herfy.util.StringUtil
import com.herfy.util.scroll.ScrollableViewPagerContentTouchEventDelegator
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.Favorite
import com.skylinedynamics.solosdk.api.models.objects.Ingredient
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.Store
import java.util.Locale.getDefault

class NewMenuViewHolder(val context: Context, val menuPresenter: MenuContract.Presenter, val binding: ItemMenuItemNewBinding) : RecyclerView.ViewHolder(binding.root), MenuContract.View, SharedPreferences.OnSharedPreferenceChangeListener {

    private var menuItem: MenuItem? = null
    private val touchDelegator = ScrollableViewPagerContentTouchEventDelegator(context)
    private val primaryColorStateList: ColorStateList
    private val disabledStateList: ColorStateList
    private val favoriteColor = Color.parseColor("#F22424")
    private val notFavoriteColor = Color.parseColor("#DEDEDE")
    private val soldOutColor: Int
    private val notSoldOutColor: Int
    private var isSignedIn = AuthUtil.getInstance().isSignedIn
    override fun setPresenter(presenter: MenuContract.Presenter) {}
    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }
    override fun setCartExpiry() {}
    override fun setupViews() {}
    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.colorPrimary))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    private fun setupEvents() {
        CacheUtil.getInstance().sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.addToBag.text = CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag")
    }

    override fun showStore(store: Store) {}
    override fun navigateToMenuItemDetails(menuCategory: MenuCategory?, menuItem: MenuItem?) {

    }

    override fun isFavorite(isFavorite: Boolean) {

    }

    override fun showStoreOpen(open: Boolean, message: String) {}
    override fun showFrequentBoughtItems(frequentBoughtItems: MutableList<MenuItem>?) {
        TODO("Not yet implemented")
    }

    override fun showAddress(address: String) {}
    override fun reloadMenu() {}
    override fun showMenuCategories(
        refresh: Boolean,
        isFromInitialLoad: Boolean,
        menuCategories: MutableList<MenuCategory>?
    ) {}
    override fun selectMenuCategory(position: Int, menuCategory: MenuCategory) {}
    override fun selectMenuItem(menuCategory: MenuCategory?, menuItem: MenuItem?) {}
    override fun setMenuCategory(menuCategory: MenuCategory, selected: Boolean) {}
    override fun setMenuItemDetails(menuItem: MenuItem?) {

    }

    override fun setIngredient(ingredient: Ingredient) {}
    override fun setModifierGroup(
        position: Int,
        modifierGroup: ModifierGroup,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean,
        modifierItemsRecyclerViewAdapterNewHashMap: java.util.HashMap<Int, ModifierItemsNewRecyclerViewAdapter>
    ) {}
    override fun setMenuItemQuantity(quantity: Int) {}
    override fun setMenuItemTotalPrice(totalPrice: Double, menuItemPrice: Double) {}
    override fun selectSize() {}
    override fun showOrderType() {}
    override fun addCartItem(
        menuItem: MenuItem,
        isShouldBackPressed: Boolean,
        menuAddedToCartPosition: Int
    ) {}
    override fun addCartItem(
        menuItem: MenuItem,
        view: ImageView,
        isShouldBackPressed: Boolean
    ) {}
    override fun updateCartItem() {}
    override fun showMessage(message: String) {
        /*((HomeActivity) context).dismissDialogs();
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();*/
    }

    override fun showError(error: String) {
        /*((HomeActivity) context).dismissDialogs();
        ((HomeActivity) context).showAlertDialog("", error);*/
    }
    override fun updateFavoriteByPosition(
        position: Int,
        menuItem: MenuItem?
    ) {}
    override fun showHideModifierItemPrice(show: Boolean, modifierGroupPosition: Int) {}
    override fun showModifierSelectionIndicator(
        hasAtleastOneSelectedItem: Boolean,
        modifierGroupPosition: Int,
        modifierGroupIndexVisibility: java.util.HashMap<String, Int>?,
        modifierItemPosition: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun showFavorites(fromAddRemove: Boolean, favorites: ArrayList<Favorite>) {}
    override fun setFavorite(favorite: Favorite, outOfStockLocations: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun setMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {
        binding.root.setOnLongClickListener { false }
        this.menuItem = menuItem
        setFavoriteIcon(menuItem)

        val price = menuItem.getPrice(CacheUtil.getOrderStoreId())

        binding.container.setOnClickListener {
            menuPresenter.selectMenuItem(menuCategory, menuItem)
            FirebaseECommerceEventUtil.selectItemEvent(context, menuItem, menuCategory, price)
        }
        binding.imageItem.setOnClickListener { menuPresenter.selectMenuItem(menuCategory, menuItem) }
        var imageUrl = menuItem.image
        imageUrl = if (imageUrl == null || imageUrl.equals("null", ignoreCase = true) || imageUrl.isEmpty() || imageUrl.lowercase(
                getDefault()
            )
                .contains("default-image.png")) {
            Values.DEFAULT_IMAGE_URL
        } else {
            StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl)
        }
        Glide.with(context)
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
        binding.itemPrice.text = StringUtil.getFormattedPrice(price)
        if (price > 0) {
            binding.itemPrice.visibility = View.VISIBLE
        } else {
            binding.itemPrice.visibility = View.INVISIBLE
        }
        binding.itemName.text = menuItem.getName(LocaleUtil.getInstance().language)
        binding.favoriteCard.visibility = if (isSignedIn && !menuItem.isCombo) View.VISIBLE else View.INVISIBLE
        setFavorite(menuItem)
        binding.favoriteContainer.setOnClickListener {
            menuPresenter.setMenuCategory(menuCategory)
            menuPresenter.menuItem = menuItem
            if (isSignedIn) {
                // TODO: remove possible memory leak
                (context as MainActivity).showLoadingDialog()
                if (CacheUtil.getInstance().favorites.contains(menuItem.id)) {
                    binding.favoriteIcon.setColorFilter(notFavoriteColor)
                    menuPresenter.addRemoveFavorite(false)
                } else {
                    binding.favoriteIcon.setColorFilter(favoriteColor)
                    menuPresenter.addRemoveFavorite(true)
                }
            } else {
                CacheUtil.getInstance().favoriteMenuCategory = menuPresenter.menuCategory
                CacheUtil.getInstance().favoriteMenuItem = menuPresenter.menuItem
                val intent = Intent(context, SignInActivity::class.java)
                intent.putExtra("favorite_home", true)
                context.startActivity(intent)
            }
        }
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            binding.addToBag.backgroundTintList = disabledStateList
        } else {
            binding.addToBag.setBackgroundResource(R.drawable.background_add_to_bag)
        }
        binding.addToBag.setOnClickListener { v: View? ->
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                FirebaseService.newNotification(
                    CacheUtil.getInstance().getTranslations(
                    Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), binding.addToBag.context)
                return@setOnClickListener
            }

            menuPresenter.addCartMenuItem(binding.imageItem, menuItem, price, 1)

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

    override fun setFavorite(menuItem: MenuItem) {
        if (isSignedIn && !menuItem.isCombo && CacheUtil.getInstance().favorites.contains(menuItem.id)) {
            binding.favoriteIcon.setColorFilter(favoriteColor)
        } else {
            binding.favoriteIcon.setColorFilter(notFavoriteColor)
        }
    }

    override fun showSearchedMenuItems(searchedMenuItems: ArrayList<MenuItem>) {}
    override fun hideSearchedMenuItems(menuCategory: MenuCategory) {}
    override fun setSizes(menuItem: MenuItem) {}
    override fun setModifierItem(modifierGroup: ModifierGroup, modifierItem: ModifierItem, position: Int) {}
    override fun setModifierItem(modifierGroupPosition: Int, modifierGroup: ModifierGroup, modifierItemPosition: Int, modifierItem: ModifierItem, showModifierItemPrice: Boolean) {}
    override fun showMenuItem(categoryId: String, menuItemId: String) {}
    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showBestSellers(menuCategories: ArrayList<MenuItem>) {}
    override fun showLatestOffers(latestOffers: List<Campaign>) {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {}
    override fun setCoupon(campaign: Campaign) {}
    override fun setBestSeller(menuItem: MenuItem, outOfStockLocations: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun setRecentOrder(menuItem: MenuItem, outOfStockLocations: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun logFirebaseEvent(event: String) {}
    override fun saveToCache(key: String?, value: String?) {

    }

    override fun getFromCache(key: String?): String {
        return ""
    }

    override fun showLoader() {

    }

    override fun updateComponents(
        componentPosition: Int,
        componentModifierItems: MutableList<ComponentModifierItem>?,
        previous: Int,
        selected: Int
    ) {

    }

    override fun updateComponentModifierGroups(
        componentPosition: Int,
        componentModifierItems: MutableList<ComponentModifierItem>?,
        selected: Int
    ) {

    }

    override fun updateOrderTypeContainer(isPreselected: Boolean) {
    }

    override fun logAddToWishlistEvent(menuItem: MenuItem) {
        FacebookAppEventUtil.addToWishlistEvent(context, menuItem)
        KochavaEventUtil.itemAddToWishList(menuItem)
    }

    override fun setOriginalMenuItemValue(value: String?) {

    }

    override fun showRecentOrders(menuItems: ArrayList<MenuItem>) {}

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals("Favorites", ignoreCase = true) && menuItem != null) {
            setFavoriteIcon(menuItem!!)
        }
    }

    fun setFavoriteIcon(menuItem: MenuItem) {
        binding.favoriteIcon.setColorFilter(if (CacheUtil.getInstance().favorites.contains(menuItem.id)) {
            favoriteColor
        } else {
            notFavoriteColor
        })
    }

    init {
        val primaryColor = fetchAccentColor()
        primaryColorStateList = ColorStateList.valueOf(primaryColor)
        disabledStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.disabled_tint))
        soldOutColor = ContextCompat.getColor(context, R.color.soldout)
        notSoldOutColor = ContextCompat.getColor(context, android.R.color.transparent)
        setupViews()
        setupEvents()
        setupFonts()
        setupTranslations()
    }

    override fun setMenuCategoryForAnalytics(
        menuItem: MenuItem?,
        menuCategory: MenuCategory?,
        price: Double,
        quantity: Int
    ) {
        addToCartEvent(
            context,
            menuItem!!, menuCategory, price, quantity
        )
    }

    override fun updateFloatingTotalPrice() {

    }

}