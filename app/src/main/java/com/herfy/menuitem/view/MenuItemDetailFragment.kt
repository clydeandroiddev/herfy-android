package com.herfy.menuitem.view

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.R2.attr.layoutManager
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.analytics.kochava.KochavaEventType
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.base.BaseActivity
import com.herfy.databinding.FragmentMenuItemDetailBinding
import com.herfy.menu.adapters.ModifierGroupNewRecycleAdapterKotlin
import com.herfy.menuitem.viewmodel.MenuItemDetailViewModel
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet
import com.herfy.nutritiondetails.NutritionInfoParams
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierAdapter
import com.herfy.productpagedetails.ui.adapter.ComboMealComponentAdapter
import com.herfy.productpagedetails.ui.adapter.SizesItemAdapter
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.OnLayoutLoadingCompleted
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.deepCopy
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.showToast
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.CurrencyUtils
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.SnackBarUtils
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.helper.viewLifecycleScope
import com.kochava.tracker.events.EventType
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
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

    private var mBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    private var sizesItemAdapter: SizesItemAdapter? = null
    private var modifierGroupAdapter: ModifierGroupNewRecycleAdapterKotlin? = null
    private var comboMealComponentAdapter: ComboMealComponentAdapter? = null
    private var comboItemDetailsModifierAdapter: ComboItemDetailsModifierAdapter? = null

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
        this@MenuItemDetailFragment.viewModel.isFromFavorite = this.isFromFavorite
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
        //
        binding.viewModel = this@MenuItemDetailFragment.viewModel
        setupImageHeight()
        binding.apply {
            shimmerLayoutModifiers.visible()
            add.apply {
                if (isSoldOut) {
                    activity?.apply {
                        image.setColorFilter(
                            ContextCompat.getColor(
                                this, R.color.soldout
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
                    this@MenuItemDetailFragment.viewModel.addRemoveFavorite()
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
                        this@MenuItemDetailFragment.viewModel.updateCartItem(false, cartMenuItemPos)
                    } else {
                        this@MenuItemDetailFragment.viewModel.getCartMenuItem()
                    }
                } else {
                    this@MenuItemDetailFragment.viewModel.updateCartItem(true, cartMenuItemPos)
                }
                context?.let {
                    FirebaseECommerceEventUtil.addToCartEvent(
                        it,
                        this@MenuItemDetailFragment.viewModel.menuItem!!,
                        this@MenuItemDetailFragment.viewModel.menuCategory!!,
                        _totalPrice,
                        Integer.parseInt(binding.quantity.text.toString())
                    )
                    FacebookAppEventUtil.addToCartEvent(
                        it, this@MenuItemDetailFragment.viewModel.menuItem!!, _totalPrice
                    )
                    KochavaTrackerUtil.trackEventWithData(
                        EventType.ADD_TO_CART.eventName,
                        KochavaEventUtil.itemObject(
                            this@MenuItemDetailFragment.viewModel.menuItem!!
                        )
                    )
                }
            }

            if (isPromo) {
                minus.invisible()
                plus.invisible()
            } else {
                minus.visible()
                plus.visible()
            }
            minus.setOnClickListener {
                viewModel?.changeMenuItemQuantity(false)
            }
            plus.setOnClickListener {
                viewModel?.changeMenuItemQuantity(true)
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
                        binding.checkBox.visible()
                        if(it.isCombo && it.combo.isNotEmpty()){
                            sizesImage.gone()
                        }else {
                            sizesImage.visible()
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
                                sizesImage.visible()
                                Glide.with(requireContext())
                                    .load(imageUrl)
                                    .apply(RequestOptions.centerCropTransform())
                                    .placeholder(R.drawable.imagepicker_image_placeholder)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(sizesImage)
                            } else {
                                sizesImage.gone()
                            }
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
                add.text = if(LocaleUtil.getInstance().isArabic) {
                    "${StringUtil.getFormattedPrice(this@MenuItemDetailFragment.viewModel.cartMenuItemsTotalPrice)} - ${cacheUtilHelper.getTranslations(Text.ADD_TO_BAG, "Add to bag")}"
                } else {
                    "${cacheUtilHelper.getTranslations(Text.ADD_TO_BAG, "Add to bag")} - ${StringUtil.getFormattedPrice(this@MenuItemDetailFragment.viewModel.cartMenuItemsTotalPrice)}"
                }
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
            showLoading.observe(this@MenuItemDetailFragment) {
                if (it) showLoading() else dismissLoading()
            }
            showToastMessage.observeNonNull(this@MenuItemDetailFragment){
                showToastMessage(it)
            }
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
                        binding.add.text = if(LocaleUtil.getInstance().isArabic) {
                            "$formattedPrice - ${cacheUtilHelper.getTranslations(Text.ADD_TO_BAG, "Add to bag")}"
                        } else {
                            "${cacheUtilHelper.getTranslations(Text.ADD_TO_BAG, "Add to bag")} - $formattedPrice"
                        }
                    }
                }
            }
            showMenuItemQuantity.observeNonNull(this@MenuItemDetailFragment) { quantityValue ->
                binding.minus.isFocusable = quantityValue != 1
                binding.minus.isClickable = quantityValue != 1
                binding.quantity.text = StringUtil.localize(quantityValue.toString())
            }
            showModifierGroupAdapterItems.observeNonNull(this@MenuItemDetailFragment) { adapterItems ->
                modifierGroupAdapter?.submitList(adapterItems)
                binding.shimmerLayoutModifiers.gone()
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
                    }
                }

            }
            showComboMenuItemComponents.observe(this@MenuItemDetailFragment) {
                binding.apply {
                    shimmerLayoutModifiers.gone()
                    if(it.isNotEmpty()){
                        rvModifiers.visible()
                    }else {
                        rvModifiers.gone()
                    }
                }

                comboMealComponentAdapter?.addData(it)
            }
            updateMenuItemSizeList.observeNonNull(this@MenuItemDetailFragment){ pair ->
                menuItem?.let {
                    sizesItemAdapter?.updateSelectedSize(pair.second, pair.first)
                    if(it.isCombo && it.combo.isNotEmpty()){
                        comboMealComponentAdapter?.addData(arrayListOf())
                    }
                }
            }
            updateComboComponents.observeNonNull(this@MenuItemDetailFragment){
                val viewModel = this@MenuItemDetailFragment.viewModel
                val componentModifierQuantity = viewModel.componentModifierCount(it.second.items)
                val isAboveMax = viewModel.isAboveMaxComponent(it.second, componentModifierQuantity)
                comboMealComponentAdapter?.apply {
                    updateSelectedComponent(
                        it.first, it.second, it.third, isAboveMax
                    )
                }
            }
            showComboMealItemDetails.observeNonNull(this@MenuItemDetailFragment) { data ->
                showComboComponentModifierItemDetails(data)
            }
            modifierAdapterItems.observe(this@MenuItemDetailFragment) {
                it.getContentIfNotHandled()?.let { modifierAdapterItems ->
                    comboItemDetailsModifierAdapter?.addData(modifierAdapterItems)
                }
            }
            showComponentModifierItemTotal.observeNonNull(this@MenuItemDetailFragment){
                val currentTotal = it + _totalPrice
                binding.layoutBottomSheetComboDetail.tvTotalValue.text =
                    CurrencyUtils.updateCurrency(
                        currentTotal, withSpace = true, ignoreDecimalPlaces = false
                    )
            }

            logFirebaseEvent.observe(this@MenuItemDetailFragment) { value ->
                context?.let { ctx ->
                    if (Values.sDeepLinkUtm != null) {
                        val firebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
                        firebaseAnalytics.logEvent(value, Values.sDeepLinkUtm)
                    }
                }

            }

            logEvent.observe(this@MenuItemDetailFragment) { attributes ->
                activity?.cast<BaseActivity> {
                    it.logEventAttributesMetric(
                        attributes.eventName,
                        attributes.eventAttributes,
                        attributes.eventMetrictName,
                        attributes.eventMetricValue
                    )
                }
            }

            logAddToWishlistEvent.observe(this@MenuItemDetailFragment) { menuItem ->
                context?.let {
                    FacebookAppEventUtil.addToWishlistEvent(it, menuItem)
                    KochavaEventUtil.itemAddToWishList(menuItem)
                }
            }

            setFavoriteItem.observe(this@MenuItemDetailFragment) {
                dismissLoading()
                setFavorite(it)
            }

            showMenuItemQuantity.observeNonNull(this@MenuItemDetailFragment) { quantityValue ->
                binding.minus.isFocusable = quantityValue != 1
                binding.minus.isClickable = quantityValue != 1


                binding.quantity.text = StringUtil.localize(quantityValue.toString())
            }
            addCartItem.observe(this@MenuItemDetailFragment) {
                activity?.let {
                    it.runOnUiThread {
                        val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    500, VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(100)
                        }
                        dismissLoading()
                        showToastMessage(
                            CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART)
                        )
                        navigateUp()
                    }
                }

            }
            updateCartItem.observe(this@MenuItemDetailFragment) {
                var message =
                    CacheUtil.getInstance().getTranslations(Text.ITEM_UPDATED, "Item updated")
                if (isPromo && !isEdit) {
                    message = CacheUtil.getInstance()
                        .getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart")
                }
                activity?.let {
                    navigateUp()
                    showToastMessage(message)
                    dismissLoading()
                }
            }
            updateComboItemDetailModifier.observe(this@MenuItemDetailFragment) {
                val modifierItemsQuantity =
                    this@MenuItemDetailFragment.viewModel.modifierItemQuantityCount(it.second.modifierItems)
                val isAboveMax =
                    this@MenuItemDetailFragment.viewModel.isAboveMax(it.second, modifierItemsQuantity)
                comboItemDetailsModifierAdapter?.apply {
                    this@MenuItemDetailFragment.viewModel.apply {
                        getComponentModifierItemTotalPrice(
                            getUpdatedModifierList()
                        )
                        updateModifierVisibilityBy(it.second) { mapOfModifierVisibilityConfig ->
                            updateSelectedModifierItem(
                                hasModifierItemsSelected = it.first,
                                modifierGroup = it.second,
                                modifierItem = it.third,
                                isAboveMax = isAboveMax,
                                modifierVisibilityHashMap = mapOfModifierVisibilityConfig
                            )
                        }
                    }
                }
            }
            showCartExpiry.observe(this@MenuItemDetailFragment) {
                activity?.cast<BaseActivity> {
                    it.runOnUiThread {
                        it.setCartExpiry()
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
            if (!isPromo && AuthUtil.getInstance().isSignedIn) {
                binding.cbtMenuItem.setRightButtonVisibility(true)
                setFavorite(data)
            } else {
                binding.cbtMenuItem.setRightButtonVisibility(false)
            }

            if(data.isCombo && data.combo.isNotEmpty()){
                setupComboMealItems()
                setupComboMealDetailsBottomSheet()
            }else {
                setupMenuItemModifiers()
            }
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

    private fun setupComboMealDetailsBottomSheet() {
        binding.apply {
            BottomSheetBehavior.from(layoutBottomSheetComboDetail.clBottomSheetDetailsMain)
                .let { bsb ->
                    bsb.state = BottomSheetBehavior.STATE_HIDDEN
                    mBottomSheetBehavior = bsb
                }

            layoutBottomSheetComboDetail.apply {
                icClose.setOnClickListener {
                    collapseComboItemDetails()
                }
                comboItemDetailsModifierAdapter = ComboItemDetailsModifierAdapter()
                comboItemDetailsModifierAdapter?.setOnAction(this@MenuItemDetailFragment.viewModel)
                context?.let { ctx ->
                    val linearLayoutManager =
                        LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    rvModifiers.apply {
                        adapter = comboItemDetailsModifierAdapter
                        layoutManager = linearLayoutManager
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = false
                        setOnScrollChangeListener { view, i, i2, i3, i4 ->
                            mBottomSheetBehavior?.let { behavior ->
                                if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                    behavior.isDraggable =
                                        linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0
                                } else {
                                    behavior.isDraggable = true
                                }
                            }

                        }
                    }
                }
            }

            mBottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.viewBgTransparent.gone()
                        this@MenuItemDetailFragment.viewModel.currentSelectedModifierItem = null
                        binding.layoutBottomSheetComboDetail.btnAddDetails.setOnClickListener(null)
                        comboItemDetailsModifierAdapter?.addData(arrayListOf())
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    private fun setupComboMealItems() {
        binding.apply {
            context?.let { ctx ->
                val linearLayoutManager =
                    LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                comboMealComponentAdapter = ComboMealComponentAdapter()
                comboMealComponentAdapter?.setOnAction(this@MenuItemDetailFragment.viewModel)
                rvModifiers.apply {
                    adapter = comboMealComponentAdapter
                    layoutManager = linearLayoutManager
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                }
                rvModifiers.OnLayoutLoadingCompleted {
                    this@MenuItemDetailFragment.viewModel.apply {
                        menuItem?.let {
                            getMenuItemTotalPrice(it, -1)
                        }
                    }
                }
            }
        }
    }

    private fun expandComboItemDetails() {
        binding.viewBgTransparent.visible()
        mBottomSheetBehavior?.let {
            it.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun collapseComboItemDetails() {
        mBottomSheetBehavior?.let {
            viewModel.currentSelectedModifierItem = null
            binding.viewBgTransparent.gone()
            binding.layoutBottomSheetComboDetail.btnAddDetails.setOnClickListener(null)
            if (it.state == BottomSheetBehavior.STATE_EXPANDED) {
                it.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun showComboComponentModifierItemDetails(data: ShowComboMealItemDetails) {
        binding.layoutBottomSheetComboDetail.apply {
            data.componentModifierItem.apply {
                val modifierGroupsCopy = modifierGroups.deepCopy(ModifierGroup::class.java)
                this@MenuItemDetailFragment.viewModel.currentSelectedModifierItem = this
                viewModel.getModifierAdapterItem(modifierGroupsCopy)
                tvTitle.text = name.toTranslate()
                this@MenuItemDetailFragment.viewModel.getComponentModifierItemTotalPrice(
                    modifierGroupsCopy
                )
                btnAddDetails.setOnClickListener {
                    viewModel.updateSelectedModifierItem(
                        data.isSelected,
                        data.menuItemComponent,
                        data.menuItemComponentIndex,
                        data.componentModifierItem,
                        comboItemDetailsModifierAdapter?.getUpdatedModifierList()
                    )
                    collapseComboItemDetails()
                }
            }
            expandComboItemDetails()
        }
    }

    private fun showToastMessage(message: String) {
        if (inProgress) {
            dismissLoading()
        }
        if (message == CacheUtil.getInstance()
                .getTranslations(Text.ADDED_TO_FAVORITES) || message == CacheUtil.getInstance()
                .getTranslations(Text.REMOVED_FROM_FAVORITES)
        ) {
            isFavorites(
                message == CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
            )
        } else {
            context?.let {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isFavorites(isFavorite: Boolean) {
        var text = CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
        if (!isFavorite) {
            text = CacheUtil.getInstance().getTranslations(Text.REMOVED_FROM_FAVORITES)
        }
        var background: Int = com.herfy.R.drawable.background_favorites
        if (!isFavorite) {
            background = com.herfy.R.drawable.background_unfavorite
        }

        var color: Int = binding.mainToolbar.context.resources.getColor(R.color.fav_color)
        if (!isFavorite) {
            color = binding.mainToolbar.context.resources.getColor(R.color.unfav_color)
        }

        SnackBarUtils.showCustomSnackBar(binding.mainLayout, text!!, isFavorite, background, color)
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