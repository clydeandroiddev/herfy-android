package com.herfy.favorites

import android.content.Context.VIBRATOR_SERVICE
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.OnClick
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.favourites.databinding.FragmentNewFavoritesBinding
import com.herfy.favourites.ui.DialogClearCurrentBasketFragment
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.util.CacheHelper
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.*
import com.skylinedynamics.solosdk.api.models.objects.Favorite
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.OrderTypeMenus
import io.shipbook.shipbooksdk.Log

class FavoritesFragment : BaseFragment(), FavoritesContract.View {

    private lateinit var binding: FragmentNewFavoritesBinding

    private lateinit var favoritesPresenter: FavoritesContract.Presenter

    private var adapter: FavoritesRecyclerViewAdapter? = null

    private val args: FavoritesFragmentArgs by navArgs()

    private val orderTypeMenus by lazy {
        CacheUtil.getInstance().application.attributes.orderTypeMenus
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
//        binding.blocker.visibility = View.VISIBLE
        binding.newMenuItemSearchShimmer.visibility = View.VISIBLE
        setUpNavigationObservers()

        Log.d("FAFA","Menus bai ${CacheUtil.getInstance().application.attributes.orderTypeMenus}" )
        if (orderTypeMenus.isNotEmpty()) {
            favoritesPresenter.getFavorites(1, orderTypeMenus[0].id)
        }
    }

    private fun setUpNavigationObservers() {
        setFragmentResultListener(DialogClearCurrentBasketFragment.CURRENT_CART_KEY) { result, bundle ->
            val cartTotal = bundle.getInt(DialogClearCurrentBasketFragment.CART_TOTAL_KEY)
            Log.d("FAFA","Cart total "  + cartTotal)
            binding.footer.setVisible(cartTotal >= 1)
            if (cartTotal == 0) {
                CacheUtil.getInstance()
                    .orderType = OrderType.NONE
                if (orderTypeMenus.isNotEmpty()) {
                    favoritesPresenter.getFavorites(1, orderTypeMenus[0].id)
                }

            }
            binding.txtCartFavouriteTotal.text = cartTotal.toString()
        }
    }
    override fun setPresenter(presenter: FavoritesContract.Presenter) {
        favoritesPresenter = presenter
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
            findNavController().popBackStack()
        }
        if (args.isFromOrderType && CacheUtil.getInstance().cart.isNotEmpty()) {
            binding.txtCartFavouriteTotal.text = CacheUtil.getInstance()
                .cart
                .map { it.favourites }
                .count()
                .toString()
            binding.footer.setVisible(args.isFromOrderType)
        }
        adapter = FavoritesRecyclerViewAdapter(favoritesPresenter)
        adapter?.setOnAction(object : FavoritesAction {

            override fun onFavouriteAddToCart(favorite: Favorite) {
                Log.d("FAFA","Favourite clicked ${CacheUtil.getInstance().orderType}" )
                if (CacheUtil
                        .getInstance().orderType != OrderType.NONE
                ) {
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
        binding.txtFavourites.text = CacheUtil.getInstance().getTranslations(Text.FAVORITES,"Favourites")
    }

    override fun showLoadingDialog() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
    }
    override fun showFavorites(favorites: ArrayList<Favorite>) {
        val mockOrderTypes = CacheUtil.getInstance()
            .application
            .attributes.orderTypeMenus
        activity?.let { activity ->
            val baseActivity = activity as BaseActivity
            baseActivity.dismissDialogs()
            binding.orderTypeContainer.removeAllViews()
            val type = CacheUtil.getInstance().orderType
            // todo refactor this by extracting this to make this more readable
            if (args.isFromOrderType && type != OrderType.NONE) {
                    mockOrderTypes
                        .forEach { orderType->
                            val view = LayoutInflater.from(activity)
                                .inflate(
                                    R.layout.item_favourite_order_type,
                                    binding.orderTypeContainer,
                                    false
                                )
                            view.tag = orderType.id
                            val txtOrder = view.findViewById<MaterialTextView>(R.id.txtOrderType)
                            Log.d("FAFA","Test ${orderType.type.lowercase().contains(type.name.lowercase())}")
                            if (type.name.lowercase()
                                    .equals(orderType.type, ignoreCase = true) || orderType.type.lowercase()
                                    .contains(type.name.lowercase()) ||  type.name.replace("_", " ")
                            .equals(orderType.type, ignoreCase = true)
                            ) {
                                txtOrder.setTextColor(Color.WHITE)
                                txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
                            } else {
                                txtOrder.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.color_gray
                                    )
                                )
                                txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
                            }
                            val container =
                                view.findViewById<ConstraintLayout>(R.id.favouriteOrderContainer)
                            container.setOnClickListener { clickedView ->
                                for (i in 0 until binding.orderTypeContainer.childCount) {
                                    val childView = binding.orderTypeContainer.getChildAt(i)
                                    val txtOrderType: TextView =
                                        childView.findViewById(R.id.txtOrderType)
                                    if (childView.tag == clickedView.tag) {
                                        CacheUtil
                                            .getInstance().orderType = OrderType.fromFavouriteType(txtOrderType.text.toString())
                                        txtOrderType.setTextColor(Color.WHITE)
                                        if (CacheUtil.getInstance().orderType != OrderType.NONE && args.isFromOrderType) {
                                            DialogClearCurrentBasketFragment.newInstance()
                                                .show(parentFragmentManager, DialogClearCurrentBasketFragment.TAG)
                                        }
                                        txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
                                    } else {
                                        txtOrderType.setTextColor(
                                            ContextCompat.getColor(
                                                requireContext(),
                                                R.color.color_gray
                                            )
                                        )
                                        txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
                                    }
                                }
                                val txtOrderType =
                                    clickedView.findViewById<MaterialTextView>(R.id.txtOrderType)
                                // todo ask maam rio what's the endpoint for filtering it
                                favoritesPresenter?.getFavouritesByOrderTypeId(
                                    clickedView.tag as String,
                                    txtOrderType.text.toString()
                                )
                            }
                            val txtOrderType = view.findViewById<MaterialTextView>(R.id.txtOrderType)
                            txtOrderType.text = orderType.type
                            binding.orderTypeContainer.addView(view)
                        }

                } else {
                mockOrderTypes.forEach { orderType ->
                    val view = LayoutInflater.from(activity)
                        .inflate(
                            R.layout.item_favourite_order_type,
                            binding.orderTypeContainer,
                            false
                        )
                    view.tag = orderType.id
                    val txtOrder = view.findViewById<MaterialTextView>(R.id.txtOrderType)
                    Log.d("FAFA","Type bai" + CacheUtil.getInstance().orderType)
                    Log.d("FAFA","Test bai  ${orderType.type.lowercase().contains(type.name.lowercase())} and ${type.name.lowercase()
                        .equals(orderType.type, ignoreCase = true)}")
                    if (orderType.type.lowercase()
                            .equals(OrderType.DELIVERY.name, ignoreCase = true) && CacheUtil
                            .getInstance().orderType == OrderType.NONE
                    ) {
                        CacheUtil.getInstance().orderType = OrderType.DELIVERY
                        txtOrder.setTextColor(Color.WHITE)
                        txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)


                    }

                    else if (type.name.lowercase()
                            .equals(orderType.type, ignoreCase = true) || orderType.type.lowercase()
                            .contains(type.name.lowercase()) || type.name.replace("_", " ")
                            .equals(orderType.type, ignoreCase = true)
                    ) {
                        Log.d("FAFA","False bai")
                        txtOrder.setTextColor(Color.WHITE)
                        txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
                    }else {
                        CacheUtil
                            .getInstance()
                            .orderType
                        txtOrder.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.color_gray
                            )
                        )
                        txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
                    }
                    val container =
                        view.findViewById<ConstraintLayout>(R.id.favouriteOrderContainer)
                    container.setOnClickListener { clickedView ->
                        for (i in 0 until binding.orderTypeContainer.childCount) {
                            val childView = binding.orderTypeContainer.getChildAt(i)
                            val txtOrderType: TextView =
                                childView.findViewById(R.id.txtOrderType)
                            if (childView.tag == clickedView.tag) {
                                CacheUtil
                                    .getInstance().orderType = OrderType.fromFavouriteType(txtOrderType.text.toString())
                                txtOrderType.setTextColor(Color.WHITE)
                                if (CacheUtil.getInstance().orderType != OrderType.NONE && args.isFromOrderType && CacheUtil
                                        .getInstance()
                                        .cart.isNotEmpty()
                                ) {
                                    DialogClearCurrentBasketFragment.newInstance()
                                        .show(parentFragmentManager, DialogClearCurrentBasketFragment.TAG)
                                }
                                txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
                            } else {
                                txtOrderType.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.color_gray
                                    )
                                )
                                txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
                            }
                        }
                        val txtOrderType =
                            clickedView.findViewById<MaterialTextView>(R.id.txtOrderType)
                        // todo ask maam rio what's the endpoint for filtering it
                        favoritesPresenter?.getFavouritesByOrderTypeId(
                            clickedView.tag as String,
                            txtOrderType.text.toString()
                        )
                    }

                    val type = view.findViewById<MaterialTextView>(R.id.txtOrderType)
                    type.text = orderType.type
                    binding.orderTypeContainer.addView(view)
                }
            }
            }

            favoritesPresenter?.setToSearch(false)
            binding.newMenuItemSearchShimmer.visibility = View.GONE
            adapter?.notifyDataSetChanged()

            val isFavoritesNotEmpty = favorites.isNotEmpty()
            binding.favouriteRecycleView.setVisible(isFavoritesNotEmpty)
            binding.emptyState.setVisible(isFavoritesNotEmpty.not())
            binding.emptyState.setTitle(CacheUtil.getInstance().getTranslations(Text.NO_FAVORITE_FOUND, "No favourites found."))
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