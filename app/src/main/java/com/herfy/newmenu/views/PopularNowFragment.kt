package com.herfy.newmenu.views

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herfy.analytics.FacebookAppEventUtil
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.base.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.databinding.FragmentPopularNowBinding
import com.herfy.main.MainActivity
import com.herfy.menu.MenuContract
import com.herfy.menu.MenuPresenter
import com.herfy.newmenu.adapter.PopularMenuItemAdapter
import com.skylinedynamics.solosdk.api.models.objects.*
import com.herfy.util.*
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils

class PopularNowFragment : BaseFragment(), MenuContract.View {

    private lateinit var binding: FragmentPopularNowBinding

    private val args: PopularNowFragmentArgs by navArgs()

    private lateinit var menuPresenter: MenuContract.Presenter

    private var menuItems: ArrayList<MenuItem>? = null
    private var popularNowAdapter: PopularMenuItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = object : TypeToken<List<MenuItem>>() {}.type
        menuItems = Gson().fromJson(args.menuItems, type)
        menuPresenter = MenuPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPopularNowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuPresenter.start();
    }

    override fun showStore(store: Store) {}
    override fun showStoreOpen(open: Boolean, message: String) {}
    override fun showAddress(address: String) {}
    override fun reloadMenu() {}
    override fun showMenuCategories(
        refresh: Boolean,
        isFromInitialLoad: Boolean,
        menuCategories: MutableList<MenuCategory>?
    ) {}
    override fun selectMenuCategory(position: Int, menuCategory: MenuCategory) {}
    override fun selectMenuItem(menuCategory: MenuCategory?, menuItem: MenuItem?) {
        if(menuCategory != null && menuItem != null) {
            activity?.runOnUiThread {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ITEM)
                NavigationUtils.navigateSafe(
                    findNavController(),
                    PopularNowFragmentDirections.actionPopularNowToMenuItem(
                        menuCategory.id,
                        menuItem.id,
                        -1
                    )
                )
            }
        }
    }

    override fun setMenuCategory(menuCategory: MenuCategory, selected: Boolean) {}
    override fun setMenuItemDetails(menuItem: MenuItem) {}
    override fun setMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {}
    override fun setFavorite(menuItem: MenuItem) {}
    override fun showSearchedMenuItems(searchedMenuItems: ArrayList<MenuItem>) {}
    override fun hideSearchedMenuItems(menuCategory: MenuCategory) {}
    override fun setSizes(menuItem: MenuItem) {}
    override fun setModifierItem(
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem,
        position: Int
    ) {
    }

    override fun setModifierItem(
        modifierGroupPosition: Int,
        modifierGroup: ModifierGroup,
        modifierItemPosition: Int,
        modifierItem: ModifierItem,
        showModifierItemPrice: Boolean
    ) {
    }

    override fun setIngredient(ingredient: Ingredient) {}
    override fun setModifierGroup(
        position: Int,
        modifierGroup: ModifierGroup,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean
    ) {
    }

    override fun setMenuItemQuantity(quantity: Int) {}
    override fun setMenuItemTotalPrice(totalPrice: Double) {}
    override fun selectSize() {}
    override fun showOrderType() {}
    override fun addCartItem(menuItem: MenuItem) {
        try {
            activity?.cast<MainActivity> {
                it.runOnUiThread {
                    val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    500,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(100)
                        }
                    }
                    it.dismissDialogs()
                    Toast.makeText(
                        it,
                        CacheUtil.getInstance()
                            .getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                        Toast.LENGTH_SHORT
                    ).show()
                    it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addCartItem(menuItem: MenuItem, view: ImageView) {
        addCartItem(menuItem)
    }

    override fun updateCartItem() {}
    override fun showMessage(message: String) {}
    override fun showError(error: String) {}
    override fun showHideModifierItemPrice(show: Boolean, modifierGroupPosition: Int) {}
    override fun showModifierSelectionIndicator(
        hasAtleastOneSelectedItem: Boolean,
        modifierGroupPosition: Int
    ) {

    }

    override fun showFavorites(fromAddRemove: Boolean, favorites: ArrayList<Favorite>) {}
    override fun setFavorite(favorite: Favorite, outOfStockLocations: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun showMenuItem(categoryId: String, menuItemId: String) {
        activity?.runOnUiThread {
            NavigationUtils.navigateSafe(findNavController(), PopularNowFragmentDirections.actionPopularNowToMenuItem(categoryId, menuItemId, -1))
        }
    }
    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showBestSellers(menuCategories: ArrayList<MenuItem>) {}
    override fun showRecentOrders(menuItems: ArrayList<MenuItem>) {}
    override fun showLatestOffers(latestOffers: List<Campaign>) {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {}
    override fun setCoupon(campaign: Campaign) {}
    override fun setBestSeller(menuItem: MenuItem, outOfStockLocation: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun setRecentOrder(menuItem: MenuItem, outOfStockLocation: ArrayList<String>, discount:  Double, specialPrice: Double) {}
    override fun logFirebaseEvent(event: String) {}
    override fun saveToCache(key: String, value: String) {
        CacheHelper.save(requireContext(), key, value)
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    override fun showLoader() {}
    override fun updateComponents(
        componentPosition: Int,
        componentModifierItems: List<ComponentModifierItem>,
        previous: Int,
        selected: Int
    ) {
    }

    override fun updateComponentModifierGroups(
        componentPosition: Int,
        componentModifierItems: List<ComponentModifierItem>,
        selected: Int
    ) {
    }

    override fun updateOrderTypeContainer(isPreselected: Boolean) {
    }

    override fun logAddToWishlistEvent(menuItem: MenuItem) {
        FacebookAppEventUtil.addToWishlistEvent(requireContext(), menuItem)
    }

    override fun setOriginalMenuItemValue(value: String?) {

    }

    override fun setPresenter(presenter: MenuContract.Presenter) {}
    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }

    override fun setCartExpiry() {
        try {
            val activity = activity as MainActivity?
            activity?.setCartExpiry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setupViews() {
        try {
            val activity = activity as MainActivity?
            if (activity != null) {
                popularNowAdapter = PopularMenuItemAdapter(activity, menuItems, menuPresenter)
                val gridLayoutManager = GridLayoutManager(activity, 2)
                binding.itemsList.layoutManager = gridLayoutManager
                binding.itemsList.adapter = popularNowAdapter
                popularNowAdapter!!.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {

    }

    override fun setMenuCategoryForAnalytics(
        menuItem: MenuItem?,
        menuCategory: MenuCategory?,
        price: Double,
        quantity: Int
    ) {
        FirebaseECommerceEventUtil.addToCartEvent(
            context,
            menuItem!!, menuCategory, price, quantity
        )
    }

    override fun updateFloatingTotalPrice() {
        TODO("Not yet implemented")
    }

}