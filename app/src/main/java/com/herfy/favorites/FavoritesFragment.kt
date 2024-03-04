package com.herfy.favorites

import android.content.Context.VIBRATOR_SERVICE
import android.graphics.Color
import android.net.Uri
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
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.OnClick
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.favourites.databinding.FragmentNewFavoritesBinding
import com.herfy.favourites.ui.model.FakeOrderTypeUIModel
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

class FavoritesFragment : BaseFragment(), FavoritesContract.View {

    private lateinit var binding: FragmentNewFavoritesBinding

    private lateinit var favoritesPresenter: FavoritesContract.Presenter

    private var adapter: FavoritesRecyclerViewAdapter? = null

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
        favoritesPresenter.getFavorites(1)
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
        binding.icBack.ninjaTap(viewLifecycleScope) {
            findNavController().popBackStack()
        }
        adapter = FavoritesRecyclerViewAdapter(favoritesPresenter)
        binding.favouriteRecycleView.layoutManager = LinearLayoutManager(context)
        binding.favouriteRecycleView.adapter = adapter
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
//        binding.searchEmptyText.text = CacheUtil.getInstance()
//            .getTranslations(Text.NO_FAVORITES_AVAILABLE)
//        binding.search.hint = CacheUtil.getInstance().getTranslations(
//            Text.SEARCH_THROUGH_YOUR_FAVORITES,
//            "Search through your favorites"
//        )
//        binding.searchEmptyTextDesc.text = CacheUtil.getInstance().getTranslations(
//            Text.NO_ITEM_FOUND_DESC,
//            "Sorry, we were unable to find something that matches your search."
//        )
        binding.txtFavourites.text = CacheUtil.getInstance().getTranslations(Text.FAVORITES,"Favourites")
    }

    override fun showLoadingDialog() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
    }
    override fun showFavorites(favorites: ArrayList<Favorite>) {
        val mockOrderTypes = listOf(
            FakeOrderTypeUIModel(id = 1, "Delivery"),
            FakeOrderTypeUIModel(id = 2, "Pickup"),
            FakeOrderTypeUIModel(id = 3, "Dine In")
        )
        activity?.let { activity ->
            val baseActivity = activity as BaseActivity
            baseActivity.dismissDialogs()
            binding.orderTypeContainer.removeAllViews()

            mockOrderTypes.forEach { orderType ->
                val view = LayoutInflater.from(activity)
                    .inflate(R.layout.item_favourite_order_type, binding.orderTypeContainer, false)
                view.tag = orderType.id

                val container = view.findViewById<ConstraintLayout>(R.id.favouriteOrderContainer)
                container.tag = orderType.id
                container.setOnClickListener { clickedView ->
                    for (i in 0 until binding.orderTypeContainer.childCount) {
                        val childView = binding.orderTypeContainer.getChildAt(i)
                        val txtOrderType: TextView = childView.findViewById(R.id.txtOrderType)
                        if (childView.tag == clickedView.tag) {
                            txtOrderType.setTextColor(Color.WHITE)
                            txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
                        } else {
                            txtOrderType.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_gray))
                            txtOrderType.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
                        }
                    }
                    val txtOrderType = clickedView.findViewById<MaterialTextView>(R.id.txtOrderType)
                    // todo ask maam rio what's the endpoint for filtering it
                    favoritesPresenter?.getFavouritesByOrderTypeId(clickedView.tag as Long, txtOrderType.text.toString())
                }
                val txtOrderType = view.findViewById<MaterialTextView>(R.id.txtOrderType)
                txtOrderType.text = orderType.name
                binding.orderTypeContainer.addView(view)
            }

            favoritesPresenter?.setToSearch(false)
            binding.newMenuItemSearchShimmer.visibility = View.GONE
            adapter?.notifyDataSetChanged()

            val isFavoritesNotEmpty = favorites.isNotEmpty()
            binding.favouriteRecycleView.setVisible(isFavoritesNotEmpty)
            binding.emptyState.setVisible(isFavoritesNotEmpty.not())
            binding.emptyState.setTitle(CacheUtil.getInstance().getTranslations(Text.NO_FAVORITE_FOUND, "No favourites found."))
        }
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

                val request = NavDeepLinkRequest.Builder.fromUri(
                    Uri.parse(
                        "${Values.NAV_DEEP_LINK_BASE_URL}nav_order_type/${1}" // todo hardcoded for now ask maam rio where to get the store id
                    )
                ).build()
                NavigationUtils.navigateSafe(findNavController(), request)
                Toast.makeText(
                    baseActivity,
                    CacheUtil.getInstance().getTranslations(Text.FAVORITE_ADDED_TO_CART),
                    Toast.LENGTH_SHORT
                ).show()
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