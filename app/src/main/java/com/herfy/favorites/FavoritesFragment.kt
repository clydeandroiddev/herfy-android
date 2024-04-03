package com.herfy.favorites

import android.content.Context.VIBRATOR_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.OnClick
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.favourites.databinding.FragmentNewFavoritesBinding
import com.herfy.favourites.ui.DialogClearCurrentBasketFragment
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.util.CacheHelper
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.*
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.Favorite
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.OrderType.Companion.convertOrderTypeToString
import com.skylinedynamics.solosdk.api.models.objects.OrderTypeMenus

class FavoritesFragment : BaseFragment(), FavoritesContract.View {

    private lateinit var binding: FragmentNewFavoritesBinding

    private lateinit var favoritesPresenter: FavoritesContract.Presenter

    private var adapter: FavoritesRecyclerViewAdapter? = null
    private val orderTypeViews = mutableListOf<OrderTypeView>()
    private val args: FavoritesFragmentArgs by navArgs()

    private val applicationAttributes by lazy {
        CacheUtil
            .getInstance()
            .application
            .attributes
    }
    private val orderTypeMenus by lazy {
        applicationAttributes.orderTypeMenus
    }
    private val allowedOrderMenus by lazy {
        applicationAttributes
            .allowedOrderTypes
    }

    private val currentOrderType by lazy {
        CacheUtil.getInstance()
            .orderType
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesPresenter = FavoritesPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoritesPresenter.start()
        val eventAttributes = HashMap<String, String>()
        eventAttributes["User ID"] = AuthUtil.getInstance().customer.id
        logEvent("MyFavoritesView", eventAttributes, "", 0.0)
        setUpNavigationObservers()
        getFavoritesByOrderType(
            currentOrderType,
            orderTypeMenus = orderTypeMenus
        )
    }

    private fun setUpNavigationObservers() {
        setFragmentResultListener(DialogClearCurrentBasketFragment.CURRENT_CART_KEY) { result, bundle ->
            val cartTotal = bundle.getInt(DialogClearCurrentBasketFragment.CART_TOTAL_KEY)
            binding.footer.setVisible(cartTotal >= 1)
            if (cartTotal == 0) {
                val currentSelectedOrderType = CacheUtil.getInstance().orderType
                getFavoritesByOrderType(currentSelectedOrderType,
                    orderTypeMenus = orderTypeMenus
                )

            }
            binding.txtCartFavouriteTotal.text = cartTotal.toString()
        }
    }
    override fun setPresenter(presenter: FavoritesContract.Presenter) {
        favoritesPresenter = presenter
    }

    private fun getFavoritesByOrderType(orderType: OrderType, orderTypeMenus: List<OrderTypeMenus>) {
        if (orderType != OrderType.NONE) {
            val orderTypeString = convertOrderTypeToString(orderType)
            if (orderTypeString.isNotEmpty()) {
                val selectedOrderTypeMenu =
                    orderTypeMenus.find { it.type.equals(orderTypeString, ignoreCase = true) }
                favoritesPresenter.getFavorites(1, selectedOrderTypeMenu?.id ?: CacheUtil
                    .getInstance()
                    .menuId)
            }
        } else {
            favoritesPresenter.getFavorites(1, orderTypeMenus.firstOrNull()?.id ?: CacheUtil.getInstance()
                .menuId)
        }
    }
    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {
        activity?.cast<BaseActivity> {
            it.setCartExpiry()
        }
    }

    override fun setupViews() {
        activity?.cast<MainActivity> {
            it.setCloseButton {
                findNavController().navigateUp()
            }
        }
        activity?.cast<NavigationActivity> {
        }
        binding.btnAddMoreItems
            .ninjaTap(viewLifecycleScope) {
                val request = NavDeepLinkRequest.Builder.fromUri(
                    Uri.parse("${Values.NAV_DEEP_LINK_BASE_URL}nav_menu")
                ).build()

                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(0, true)
                    .build()
                findNavController().navigate(request, options)
            }

        binding.icBack.ninjaTap(viewLifecycleScope) {
            if (args.isFromOrderType && CacheUtil
                    .getInstance().orderType != OrderType.NONE) {
                findNavController().popBackStack()
                findNavController().navigate(FavoritesFragmentDirections.actionFavoritesToHome())
            }else {
                findNavController().navigateUp()
            }
        }

        val cartItems = CacheUtil.getInstance().cart
        val quantityTotal =  cartItems.filter { it.isFavorite }
            .sumOf { it.attributes.quantity }


            binding.txtCartFavouriteTotal.text = quantityTotal.toString()
            binding.footer.setVisible(quantityTotal >= 1)

        adapter = FavoritesRecyclerViewAdapter(favoritesPresenter)
        adapter?.setOnAction(object : FavoritesAction {

            override fun onFavouriteAddToCart(favorite: Favorite) {
                if (CacheUtil
                        .getInstance().orderType != OrderType.NONE && args.isFromOrderType || CacheUtil.getInstance().cart
                        .count { it.isFavorite } >= 1
                ) {
                    MixpanelUtil.trackEvent(MixpanelUtil.ADD_TO_CART_FROM_FAVOURITES)
                    activity?.apply {
                        val menuItem = MenuUtil.getFavoriteMenuItem(favorite)
                        FacebookAppEventUtil.addToCartEvent(this, menuItem, MenuUtil.getMenuItemTotalPrice(menuItem))
                    }
                    findNavController().navigate(FavoritesFragmentDirections.actionFavoritesToMenuItem(favorite.id, isFavorite = true))
                } else {
                    CacheUtil
                        .getInstance().saveFavoriteToAddToCart(favorite)
                    findNavController().navigate(FavoritesFragmentDirections.actionFavoritesToOrderType(isFromFavorites = true))
                }
            }


        })
        binding.favouriteRecycleView.layoutManager = LinearLayoutManager(context)
        binding.favouriteRecycleView.adapter = adapter
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.btnAddMoreItems.text = CacheUtil.getInstance().getTranslations(Text.ADD_MORE_ITEMS,"Add more items")
        binding.txtViewBasket.text = CacheUtil.getInstance().getTranslations(Text.VIEW_BASKET, "View basket")
        binding.txtFavourites.text = CacheUtil.getInstance().getTranslations(Text.FAVOURITES,"Favourites")
    }

    override fun setIsLoading(isLoading: Boolean) {
        binding.favouriteRecycleView.setVisible(!isLoading)
        binding.newMenuItemSearchShimmer.setVisible(isLoading)
        binding.newMenuItemSearchShimmer.startShimmer()
    }

    override fun showLoadingDialog() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
    }
    override fun showFavorites(favorites: ArrayList<Favorite>) {
        val hasFavorite = CacheUtil.getInstance().cart
            .count { it.isFavorite }
        if (favorites.isEmpty() && hasFavorite >= 1) {
            val updatedCart: ArrayList<MenuItem> = ArrayList<MenuItem>().apply {
                CacheUtil.getInstance().cart.forEach { menuItem ->
                    menuItem.isFavorite = false
                    if (menuItem.isFavorite) {
                        add(menuItem)
                    }
                }

            }
                CacheUtil.getInstance().cart = updatedCart
                binding
                    .footer.setVisible(true)
            }

        activity?.let { activity ->
            val baseActivity = activity as BaseActivity
            baseActivity.dismissDialogs()
                    binding.orderTypeContainer.removeAllViews()
            System.out.println("Order type menus $allowedOrderMenus")
            val orderTypeTitles = allowedOrderMenus?.split(",") ?: emptyList()
            val translatedTitles = mapOrderTypeTitlesToTranslations(orderTypeTitles)
            val formattedOrderTypes = createFavoriteOrderTypeUIModels(translatedTitles, orderTypeMenus)
            val cacheUtil = CacheUtil.getInstance()
            formattedOrderTypes.forEach { orderType ->
                        val view = LayoutInflater.from(activity)
                            .inflate(R.layout.item_favourite_order_type, binding.orderTypeContainer, false)
                        val viewHolder = OrderTypeViewHolder(view = view, orderTypeUIModel = orderType

                        ) { selectedOrderType ->
                            handleOrderTypeClick(selectedOrderType, orderType)
                        }
                viewHolder.setInitialSelectedOrderType()
                        orderTypeViews.add(viewHolder)
                        viewHolder.bind()
                        binding.orderTypeContainer.addView(view)
                    }
                }



            favoritesPresenter?.setToSearch(false)
            binding.newMenuItemSearchShimmer.visibility = View.GONE
            binding.orderTypeShimmering.gone()
            adapter?.notifyDataSetChanged()

            val isFavoritesNotEmpty = favorites.isNotEmpty()
            binding.favouriteRecycleView.setVisible(isFavoritesNotEmpty)
            binding.emptyState.setVisible(isFavoritesNotEmpty.not())
            binding.emptyState.setTitle(CacheUtil.getInstance().getTranslations(Text.NO_FAVORITE_FOUND, "No favourites found."))

        }

    // Function to map order type titles to translated titles
   private fun mapOrderTypeTitlesToTranslations(orderTypeTitles: List<String>?): List<String> {
        return orderTypeTitles?.mapNotNull { orderTypeTitle ->
            val type = OrderType.fromString(orderTypeTitle)
            val translation = CacheUtil.getInstance()
            when (type) {
                OrderType.DELIVERY -> translation.getTranslations(Text.DELIVERY, "Delivery")
                OrderType.DINE_IN -> translation.getTranslations(Text.DINE_IN, "Dinein")
                OrderType.PICKUP -> translation.getTranslations(Text.PICKUP, "Pickup")
                else -> null
            }
        } ?: emptyList()
    }

    private fun createFavoriteOrderTypeUIModels(translatedTitles: List<String>, orderTypeMenus: List<OrderTypeMenus>): List<FavoriteOrderTypeUIModel> {
        val cacheUtil = CacheUtil.getInstance()
        val deliveryTranslated = cacheUtil.getTranslations(Text.DELIVERY, "Delivery")
        val pickUpTranslated = cacheUtil.getTranslations(Text.PICKUP, "Pickup")
        val dineInTranslated = cacheUtil.getTranslations(Text.DINE_IN, "Dine In")

        return translatedTitles.mapNotNull { title ->
            val translation = CacheUtil.getInstance()

            when (title) {
                deliveryTranslated -> {
                    val orderTypeMenu = orderTypeMenus.find { it.type == "deliver" }
                    orderTypeMenu?.let { FavoriteOrderTypeUIModel(it.id, title, orderTypeName = it.type) }
                }
                pickUpTranslated -> {
                    val orderTypeMenu = orderTypeMenus.find { it.type == "pickup" || it.type == "dinein" }
                    orderTypeMenu?.let { FavoriteOrderTypeUIModel(it.id, title, orderTypeName = it.type) }
                }
                dineInTranslated -> {
                    val orderTypeMenu = orderTypeMenus.find { it.type == "pickup" || it.type == "dinein" }
                    orderTypeMenu?.let { FavoriteOrderTypeUIModel(it.id, title, orderTypeName = "dinein") }
                }
                else -> null
            }
        }.sortedWith(compareBy(
            { it.title != deliveryTranslated },  // Ensure "Delivery" is first
            { it.title != pickUpTranslated },    // Ensure "Pickup" is second
            { it.title != dineInTranslated }     // Ensure "Dine In" is third
        ))
    }


    private fun handleOrderTypeClick(selectedOrderTypeTag: String, orderType: FavoriteOrderTypeUIModel) {
        val currentSelectedOrderType = CacheUtil.getInstance().orderType
        val hasFavoriteAddedInCart = CacheUtil.getInstance().cart.count { it.isFavorite } >= 1
            orderTypeViews.forEach { orderTypeView ->
            if (currentSelectedOrderType.name == OrderType.DELIVERY.name && selectedOrderTypeTag == OrderType.PICKUP.name.lowercase() && hasFavoriteAddedInCart) {
                showDialogClearCurrentBasketDialog(selectedOrderTypeTag)
                return
            } else if (currentSelectedOrderType.name == OrderType.PICKUP.name && selectedOrderTypeTag == OrderType.DINE_IN.name.replace("_","").lowercase() || selectedOrderTypeTag == "deliver" && hasFavoriteAddedInCart) {
                showDialogClearCurrentBasketDialog(selectedOrderTypeTag)
                return
            }else if (currentSelectedOrderType.name == OrderType.DELIVERY.name && selectedOrderTypeTag == OrderType.DINE_IN.name.replace("_","").lowercase()  && hasFavoriteAddedInCart) {
                showDialogClearCurrentBasketDialog(selectedOrderTypeTag)
                return
            }
            if (orderTypeView.tag == selectedOrderTypeTag) {
                if (CacheUtil.getInstance().cart.count { it.isFavorite } >= 1) {
                    showDialogClearCurrentBasketDialog(selectedOrderTypeTag)
                    return
                }else {
                    CacheUtil.getInstance().orderType = OrderType.fromString(selectedOrderTypeTag)
                }
                orderTypeView.setOrderTypeSelected()
            } else {
                orderTypeView.setOrderTypeUnselected()
            }
        }

        if (CacheUtil.getInstance().cart.isEmpty() || !CacheUtil.getInstance().cart.isEmpty()) {
            favoritesPresenter?.getFavouritesByOrderTypeId(
                orderType.id,
                orderType.orderTypeName
            )
        }
    }
    private fun showDialogClearCurrentBasketDialog(selectedOrderTypeTag: String) {
        val bundle = Bundle().apply {
            putString(DialogClearCurrentBasketFragment.SELECTED_ORDER_TYPE, selectedOrderTypeTag)
        }

        DialogClearCurrentBasketFragment.newInstance(bundle =bundle)
            .show(
                parentFragmentManager,
                DialogClearCurrentBasketFragment.TAG
            )
    }


    override fun setFavorite(favorite: Favorite, outOfStockLocations: ArrayList<String>, discount: Double, specialPrice: Double) {}
    override fun showMenuItem(categoryId: String, menuItemId: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
//        NavigationUtils.navigateSafe(findNavController(), FavoritesFragmentDirections.actionFavoritesToMenuItem(
//            menuCategoryId = categoryId,
//            menuItemId = menuItemId
//        ))
    }

    override fun addToCartFavorite(menuItem: MenuItem, view: ImageView) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            baseActivity.runOnUiThread {
                activity?.cast<MainActivity> { mainActivity ->
                    mainActivity.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
                activity?.cast<NavigationActivity> { menuItemActivity ->
                    menuItemActivity.setCartQuantity(CacheUtil.getInstance().cartQuantity)
                }
                val vibrator = baseActivity.getSystemService(VIBRATOR_SERVICE) as Vibrator
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

                Toast.makeText(
                    baseActivity,
                    CacheUtil.getInstance().getTranslations(Text.FAVORITE_ADDED_TO_CART),
                    Toast.LENGTH_SHORT
                ).show()

                NavigationUtils.navigateSafe(findNavController(), FavoritesFragmentDirections.actionFavoritesToOrderType(isFromFavorites = true, storeId = menuItem.id))
            }
        }

    }

    override fun showMessage(message: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.runOnUiThread {
                baseActivity.dismissDialogs()
//                binding.blocker.visibility = View.GONE
                binding.newMenuItemSearchShimmer.visibility = View.GONE
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun showError(error: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.runOnUiThread {
                baseActivity.dismissDialogs()
//                binding.blocker.visibility = View.GONE
                binding.newMenuItemSearchShimmer.visibility = View.GONE
                baseActivity.showAlertDialog("", error)
            }
        }
    }

    override fun showSearchFavorites(favorites: ArrayList<Favorite>) {
        if (favorites.size == 0) {
            binding.favouriteRecycleView.visibility = View.GONE
        } else {
            favoritesPresenter!!.setToSearch(true)
            adapter!!.notifyDataSetChanged()
            binding.favouriteRecycleView.visibility = View.VISIBLE
        }
    }

    override fun logFirebaseEvent(event: String) {
        activity?.let {
            if (Values.sDeepLinkUtm != null) {
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm)
            }
        }
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    @OnClick(R.id.blocker)
    fun blocker() {
        // Do not remove this
    }
}