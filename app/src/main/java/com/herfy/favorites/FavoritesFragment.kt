package com.herfy.favorites

import android.app.Activity
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.OnClick
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.databinding.FragmentFavoritesBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.skylinedynamics.solosdk.api.models.objects.Favorite
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.herfy.util.CacheHelper
import com.herfy.utils.helper.*

class FavoritesFragment : BaseFragment(), FavoritesContract.View {

    private lateinit var binding: FragmentFavoritesBinding

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
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoritesPresenter.start()
        val eventAttributes = HashMap<String, String>()
        eventAttributes["User ID"] = AuthUtil.getInstance().customer.id
        logEvent("MyFavoritesView", eventAttributes, "", 0.0)
        binding.blocker.visibility = View.VISIBLE
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
            it.setCloseButton {
                findNavController().navigateUp()
            }
        }
        adapter = FavoritesRecyclerViewAdapter(favoritesPresenter)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isEmpty()) {
                    favoritesPresenter!!.setToSearch(false)
                    showFavorites(favoritesPresenter!!.favorites)
                } else {
                    favoritesPresenter!!.searchFavorites(editable.toString())
                }
            }
        })
        binding.search.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            activity?.let {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val inputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(binding.search.windowToken, 0)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.searchEmptyText.text = CacheUtil.getInstance()
            .getTranslations(Text.NO_FAVORITES_AVAILABLE)
        binding.search.hint = CacheUtil.getInstance().getTranslations(
            Text.SEARCH_THROUGH_YOUR_FAVORITES,
            "Search through your favorites"
        )
        binding.searchEmptyTextDesc.text = CacheUtil.getInstance().getTranslations(
            Text.NO_ITEM_FOUND_DESC,
            "Sorry, we were unable to find something that matches your search."
        )
    }

    override fun showLoadingDialog() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
    }

    override fun showFavorites(favorites: ArrayList<Favorite>) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        favoritesPresenter!!.setToSearch(false)
        binding.blocker.visibility = View.GONE
        binding.newMenuItemSearchShimmer.visibility = View.GONE
        adapter!!.notifyDataSetChanged()
        if (favorites.size > 0) {
            binding.searchEmptyLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.searchEmptyLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
    }

    override fun setFavorite(favorite: Favorite, outOfStockLocations: ArrayList<String>, discount: Double, specialPrice: Double) {}
    override fun showMenuItem(categoryId: String, menuItemId: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        NavigationUtils.navigateSafe(findNavController(), FavoritesFragmentDirections.actionFavoritesToMenuItem(
            menuCategoryId = categoryId,
            menuItemId = menuItemId
        ))
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
            }
        }

    }

    override fun showMessage(message: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.runOnUiThread {
                baseActivity.dismissDialogs()
                binding.blocker.visibility = View.GONE
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
                binding.blocker.visibility = View.GONE
                binding.newMenuItemSearchShimmer.visibility = View.GONE
                baseActivity.showAlertDialog("", error)
            }
        }
    }

    override fun showSearchFavorites(favorites: ArrayList<Favorite>) {
        if (favorites.size == 0) {
            binding.searchEmptyTextDesc.visibility = View.VISIBLE
            binding.searchEmptyLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            favoritesPresenter!!.setToSearch(true)
            adapter!!.notifyDataSetChanged()
            binding.searchEmptyLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
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