package com.herfy.menuitem.view

import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.herfy.R
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.analytics.kochava.KochavaEventType
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.databinding.FragmentMenuItemDetailBinding
import com.herfy.menu.adapters.ModifierGroupNewRecycleAdapterKotlin
import com.herfy.menuitem.viewmodel.MenuItemDetailViewModel
import com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet
import com.herfy.nutritiondetails.NutritionInfoParams
import com.herfy.productpagedetails.ui.adapter.SizesItemAdapter
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.showToast
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CurrencyUtils
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.helper.viewLifecycleScope
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MenuItemDetailFragment :
    BaseFragment<MenuItemDetailViewModel, FragmentMenuItemDetailBinding>() {

    override val viewModel: MenuItemDetailViewModel by viewModels()
    override val layout: Int = R.layout.fragment_menu_item_detail

    private val args by lazy { arguments }

    private var sizesItemAdapter: SizesItemAdapter? = null
    private var modifierGroupAdapter: ModifierGroupNewRecycleAdapterKotlin? = null

    private var _totalPrice = 0.0


    @Inject
    lateinit var cacheUtilHelper: CacheUtilHelper

    private val isPromo: Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_PROMO, false) ?: false
    }
    private val menuItemId: String by lazy {
        args?.getString(Values.KEY_MENU_ITEM_ID) ?: ""
    }
    private val isUpsell: Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_UPSELL, false) ?: false
    }
    private val isEdit: Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_EDIT, false) ?: false
    }
    private val cartMenuItemPos: Int by lazy {
        args?.getInt(Values.KEY_CART_MENU_ITEM_POSITION, -1) ?: -1
    }
    private val menuCategoryId: String by lazy {
        args?.getString(Values.KEY_MENU_CATEGORY_ID, "") ?: ""
    }
    private val isSoldOut: Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_SOLD_OUT, false) ?: false
    }

    private val isFromFavorite: Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_FAVORITE, false) ?: false
    }

    private var inProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isPromo) {
            if (menuItemId.isNotEmpty()) {
                viewModel.apply {
                    isFromCartEdit = false
                    if (menuCategoryId.isNotEmpty()) {
                        setMenuCategory(menuCategoryId)
                    }
                    getMenuItemFromDbBy(false, menuItemId, isUpsell)
                }
            } else if (cartMenuItemPos != -1) {
                viewModel.apply {
                    getMenuItemFromCartBy(cartMenuItemPos, isUpsell)
                }
            }
        } else {
            val menuItem: MenuItem? = if (isEdit) {
                if (CacheUtil.getInstance().couponCode != null && CacheUtil.getInstance().couponMenuItem != null) {
                    CacheUtil.getInstance().couponMenuItem
                } else if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotionMenuItem != null) {
                    CacheUtil.getInstance().campaignPromotionMenuItem
                } else {
                    null
                }
            } else {
                null
            }
            menuItem?.let {
                viewModel.apply {
                    isFromCartEdit = isEdit
                    setPromoMenuItem(it)
                }
            }
        }
    }


    override fun initViews() {
        super.initViews()
        //this@MenuItemDetailFragment.viewModel.isFromFavorite = this.isFromFavorite
        binding.viewModel = this@MenuItemDetailFragment.viewModel
        setupImageHeight()
        setupMenuItemModifiers()
        binding.apply {
            add.apply {
                if (isSoldOut) {
                    activity?.apply {
                        image.setColorFilter(
                            ContextCompat.getColor(
                                this, com.herfy.R.color.soldout
                            )
                        )
                    }
                    binding.tvSoldOut.visible()
                    isEnabled = false
                    alpha = 0.3f
                } else {
                    binding.tvSoldOut.gone()
                    isEnabled = true
                    alpha = 1.0f
                }
            }
            cbtMenuItem.apply {
                setNavButtonAction {
                    navigateUp()
                }
                setRightButtonAction {
                    showLoading()
                    if (CacheUtil.getInstance().favorites.contains(this@MenuItemDetailFragment.viewModel.menuItem?.id)) {
                        setRightButtonIcon(com.herfy.productpagedetails.R.drawable.ic_favorite_filled)
                    } else {
                        setRightButtonIcon(com.herfy.productpagedetails.R.drawable.ic_favorite_empty)
                    }
                    //this@MenuItemDetailFragment.viewModel.addRemoveFavorite()
                }
            }
            add.setOnClickListener {
                if (CacheUtil.getInstance().application.attributes.menuOnly) {
                    context?.let {
                        FirebaseService.newNotification(
                            CacheUtil.getInstance().getTranslations(
                                Text.MENU_ONLY_MESSAGE,
                                "Sorry, ordering is not available at the moment"
                            ), it
                        )
                    }
                    return@setOnClickListener
                }
                showLoading()
                if (!isPromo) {
                    if (cartMenuItemPos >= 0) {
                        //this@ComboMealFragment.viewModel.updateCartItem(false, cartMenuItemPos)
                    } else {
                        //this@ComboMealFragment.viewModel.getCartMenuItem()
                    }
                } else {
                    //this@ComboMealFragment.viewModel.updateCartItem(true, cartMenuItemPos)
                }
                /*context?.let {
                    FirebaseECommerceEventUtil.addToCartEvent(
                        it,
                        this@ComboMealFragment.viewModel.menuItem!!,
                        this@ComboMealFragment.viewModel.menuCategory!!,
                        _totalPrice,
                        Integer.parseInt(binding.quantity.text.toString())
                    )
                    FacebookAppEventUtil.addToCartEvent(
                        it, this@ComboMealFragment.viewModel.menuItem!!, _totalPrice
                    )
                    KochavaTrackerUtil.trackEventWithData(
                        EventType.ADD_TO_CART.eventName,
                        KochavaEventUtil.itemObject(
                            this@ComboMealFragment.viewModel.menuItem!!
                        )
                    )
                }*/
            }

            if (isPromo) {
                minus.invisible()
                plus.invisible()
            } else {
                minus.visible()
                plus.visible()
            }
            minus.setOnClickListener {
                //viewModel?.changeMenuItemQuantity(false)
            }
            plus.setOnClickListener {
                //viewModel?.changeMenuItemQuantity(true)
            }
        }
    }

    private fun setupMenuItemModifiers() {
        modifierGroupAdapter =
            ModifierGroupNewRecycleAdapterKotlin(
                onUserClickModifierItem = { params ->
                    modifierGroupAdapter?.updateModifierItem(
                        params,
                        modifierGroupList = viewModel.modifierGroupList()
                    )
                    viewModel.apply {
                        menuItem?.let {
                            getMenuItemTotalPrice(
                                it,
                                params.modifierGroupPosition
                            )
                        }
                    }
                },
                showModifierMaximumLimit = { message ->
                    requireContext().showToast(message)
                },
                onUserAddMoreModifierItemQuantity = { params->
                    modifierGroupAdapter?.updateModifierItemQuantity(
                        params,
                        modifierGroupList = viewModel.modifierGroupList()
                    )
                    viewModel.apply {
                        menuItem?.let { menuItem ->
                            getMenuItemTotalPrice(
                                menuItem,
                                params.modifierGroupPosition
                            )
                        }
                    }
                }
            )
    }

    /* Display sizes list
    * */
    private fun setupMenuItemSizeList(menuItem: MenuItem?, sizes: List<ModifierItem>) {
        menuItem?.let {
            var showSizes = true
            val visibilityConfig = menuItem.sizes.attributes.visibilityConfig
            if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                    "hidden",
                    ignoreCase = true
                ) && !menuItem.sizes.attributes.type.equals("drinks", ignoreCase = true)
            ) {
                showSizes = false
            }

            binding.apply {
                if (showSizes) {
                    if(sizes.size > 1){
                        binding.sizesLabel.visible()

                        binding.checkBox.setVisible(true)
                        binding.sizesLabel.text = menuItem.sizes.attributes.name
                        val imageUrl = menuItem.sizes.attributes.image
                        if (imageUrl != null && !imageUrl.equals(
                                "null",
                                ignoreCase = true
                            ) && !imageUrl.lowercase(
                                Locale.getDefault()
                            ).contains("default-image.png") && !imageUrl.lowercase(
                                Locale.getDefault()
                            ).contains("default_category.png")
                        ) {
                            binding.sizesImage.visible()
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .apply(RequestOptions.centerCropTransform())
                                .placeholder(R.drawable.imagepicker_image_placeholder)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.sizesImage)
                        } else {
                            binding.sizesImage.gone()
                        }

                        activity?.let { act ->
                            val itemWidth =
                                (ViewUtil.getScreenWidth(act) / sizes.size) - ViewUtil.dpToPx(act, 20)
                            val linearLayoutManager = GridLayoutManager(act, sizes.size)
                            sizesItemAdapter = SizesItemAdapter(items = sizes, itemWidth = itemWidth)
                            sizesItemAdapter?.setOnAction(this@MenuItemDetailFragment.viewModel)
                            sizesRecyclerView.apply {
                                adapter = sizesItemAdapter
                                layoutManager = linearLayoutManager
                                setHasFixedSize(true)
                                isNestedScrollingEnabled = false
                            }
                            sizesLabel.visibility = View.VISIBLE
                            sizesRecyclerView.visibility = View.VISIBLE
                            viewSizesSeparator.visible()
                            sizesLayoutTextContainer.visible()
                        }
                    }else {
                        sizesLayoutTextContainer.gone()
                        viewSizesSeparator.gone()
                        sizesRecyclerView.gone()
                    }
                } else {
                    sizesLayoutTextContainer.gone()
                    viewSizesSeparator.gone()
                    sizesRecyclerView.gone()
                }
            }


        }
    }

    private fun setupImageHeight() {
        binding.image.apply {
            val toolbarHeight = (Resources.getSystem().displayMetrics.widthPixels / 1.1).toInt()
            binding.mainToolbar.layoutParams.height = toolbarHeight
            binding.image.layoutParams.height = toolbarHeight
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            tvSoldOut.text = CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT")
            txtMoreInfo.text = CacheUtil.getInstance().getTranslations(Text.MORE_INFO, "More Info")
            notesLabel.text =
                cacheUtilHelper.getTranslations(Text.SPECIAL_INSTRUCTION, "Special Instruction")
            notesDescription.text = cacheUtilHelper.getTranslations(
                Text.NOTES_DESCRIPTION,
                "Please let us know if you are allergic to anything or if we need to avoid anything."
            )
            txtFrequntlyBought.text = cacheUtilHelper.getTranslations(
                Text.FREQUENTLY_BOUGHT_TOGETHER_LABEL, "Frequently bought together"
            )
            txtQuantity.text = cacheUtilHelper.getTranslations(Text.ITEM_QUANTITY, "Item quantity")
            if (cartMenuItemPos >= 0 || isEdit) {
                add.text = cacheUtilHelper.getTranslations(Text.UPDATE, "Update")
            } else {
                add.text =
                    cacheUtilHelper.getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                        .replace(
                            "(x)",
                            "${StringUtil.getFormattedPrice(this@MenuItemDetailFragment.viewModel.cartMenuItemsTotalPrice)}"
                        )
            }
            layoutBottomSheetComboDetail.apply {
                tvTotalLabel.text = cacheUtilHelper.getTranslations(
                    Text.TOTAL_IS_INCLUSIVE_OF_VAT, "Total is inclusive of VAT"
                )
                btnAddDetails.text = cacheUtilHelper.getTranslations(Text.ADD, "Add")
            }
            sizesLabel.text = cacheUtilHelper.getTranslations(
                Text.CHOOSE_YOUR_MEAL_SIZE_LABEL,
                "Choose your meal size"
            )
            if (CacheUtil.getInstance().concept.isCartItemsNotesEnabled) {
                notesLabel.visibility = View.VISIBLE
                notesContainer.visibility = View.VISIBLE
                notesLabel.text = CacheUtil.getInstance()
                    .getTranslations(Text.SPECIAL_INSTRUCTION, "Special Instruction")
                notesDescription.text = CacheUtil.getInstance()
                    .getTranslations(
                        Text.NOTES_DESCRIPTION,
                        "Please let us know if you are allergic to anything or if we need to avoid anything."
                    )

                etItemNotes.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val inputMethodManager =
                            requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(etItemNotes.windowToken, 0)
                        return@OnEditorActionListener true
                    }
                    false
                })

            } else {
                notesLabel.visibility = View.GONE
                notesContainer.visibility = View.GONE
            }
        }
    }

    override fun observe() {
        super.observe()
        viewModel.apply {
            showError.observeNonNull(this@MenuItemDetailFragment) {
                it.getContentIfNotHandled()?.let {
                    runOnMain {
                        dismissLoading()
                        showAlertDialog(
                            title = "",
                            message = it.message,
                            neutral = CacheUtil.getInstance().getTranslations(Text.OK, "Ok")
                        ) { dialog, _ ->
                            if (it.closePage) {
                                navigateUp()
                            }
                            dialog.dismiss()
                        }
                    }

                }
            }
            showMenuItemDetails.observeNonNull(this@MenuItemDetailFragment) {
                setupMenuItemDetails(it)
            }
            showMenuItemSizes.observeNonNull(this@MenuItemDetailFragment) {
                setupMenuItemSizeList(menuItem, it)
            }
            showMenuItemCalories.observe(this@MenuItemDetailFragment) {
                binding.apply {
                    if (it.isNotEmpty()) {
                        btnCalories.visible()
                        calories.text = it
                    } else {
                        btnCalories.gone()
                    }
                }
            }
            showMenuItemTotal.observeNonNull(this@MenuItemDetailFragment) { totalPrice ->
                if (totalPrice <= 0.0 && isPromo) {
                    binding.total.text = CacheUtil.getInstance().getTranslations(
                        Text.FREE
                    )
                } else {
                    _totalPrice = totalPrice
                    val formattedPrice = CurrencyUtils.updateCurrency(
                        totalPrice, withSpace = true, ignoreDecimalPlaces = false
                    )
                    binding.total.text = formattedPrice
                    if (cartMenuItemPos >= 0 || isEdit) {
                        binding.add.text = cacheUtilHelper.getTranslations(Text.UPDATE, "Update")
                    } else {
                        binding.add.text =
                            cacheUtilHelper.getTranslations(
                                Text.ADD_TO_BAG_AMOUNT,
                                "Add to bag - (x)"
                            )
                                .replace("(x)", formattedPrice.toString())
                    }

                }
            }
            showMenuItemQuantity.observeNonNull(this@MenuItemDetailFragment) { quantityValue ->
                binding.minus.isFocusable = quantityValue != 1
                binding.minus.isClickable = quantityValue != 1


                binding.quantity.text = StringUtil.localize(quantityValue.toString())
            }
            showModifierGroupAdapterItems.observeNonNull(this@MenuItemDetailFragment) { adapterItems ->
                binding.shimmerLayoutModifiers.setVisible(adapterItems.isEmpty())
                modifierGroupAdapter?.submitList(adapterItems)
                if(adapterItems.isNotEmpty()){
                    binding.rvModifiers.apply {
                        visible()
                        isNestedScrollingEnabled = false
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        adapter = modifierGroupAdapter
                    }
                }else {
                    binding.apply {
                        rvModifiers.gone()
                        shimmerLayoutModifiers.gone()
                    }
                }

            }
        }
    }

    private fun setupMenuItemDetails(data: MenuItem) {
        binding.apply {
            coordinatorLayout.visible()
            footer.visible()
            language = LocaleUtil.getInstance().language
            menuItem = data
            setFavorite(data)
            txtMoreInfo.isVisible = !data.customFieldObjectMap.nutritionInfoImageUrl.isNullOrEmpty()
            txtMoreInfo.ninjaTap(viewLifecycleScope) {
                val bundle = Bundle()
                bundle.putParcelable(
                    NutritionInfoFragmentBottomSheet.NAV_ARGS_INFO_KEY, NutritionInfoParams(
                        nutritionImageUrl = data.customFieldObjectMap.nutritionInfoImageUrl.orEmpty(),
                        nutritionStory = data.customFieldObjectMap.nutritionInfoStory.orEmpty(),
                        menuName = data.getName(LocaleUtil.getInstance().language)
                    )
                )
                NutritionInfoFragmentBottomSheet.newInstance(bundle)
                    .show(childFragmentManager, NutritionInfoFragmentBottomSheet.TAG)
            }
            setupKochava(data)
        }
    }

    private fun setupKochava(menuItem: MenuItem) {
        var categoryName = ""
        this@MenuItemDetailFragment.viewModel.menuCategory?.let {
            categoryName = it.getName(LocaleUtil.getInstance().language)
        }
        KochavaTrackerUtil.trackEventWithData(
            KochavaEventType.VIEW_PRODUCT.eventName,
            KochavaEventUtil.viewProductObject(
                menuItem,
                categoryName
            )
        )
    }

    private fun setFavorite(menuItem: MenuItem) {
        binding.cbtMenuItem.apply {
            if (!isPromo && AuthUtil.getInstance().isSignedIn && CacheUtil.getInstance().favorites.contains(
                    menuItem.id
                )
            ) {
                setRightButtonIcon(com.herfy.productpagedetails.R.drawable.ic_favorite_filled)
            } else {
                setRightButtonIcon(com.herfy.productpagedetails.R.drawable.ic_favorite_empty)
            }
        }

    }


    private fun showLoading() {
        binding.apply {
            inProgress = true
            isVisible = true
        }
    }

    private fun dismissLoading() {
        binding.apply {
            inProgress = false
            isVisible = false
        }
    }

}