package com.herfy.menu.views

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.base.BaseActivity
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet
import com.herfy.nutritiondetails.NutritionInfoParams
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.FragmentComboMealBinding
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierAdapter
import com.herfy.productpagedetails.ui.adapter.ComboMealModifierGroupAdapter
import com.herfy.productpagedetails.ui.adapter.FrequentlyBoughtItemAdapter
import com.herfy.productpagedetails.ui.adapter.SizesItemAdapter
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.deepCopy
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.extensions.toTranslate
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
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ComboMealFragment : BaseFragment<ComboViewModel, FragmentComboMealBinding>() {


    private var selectedFrequentBoughtItemPos: Int = -1

    @Inject
    lateinit var cacheUtilHelper: CacheUtilHelper

    private val args by lazy { arguments }

    override val viewModel: ComboViewModel by viewModels()
    override val layout: Int = R.layout.fragment_combo_meal

    private var inProgress = false
    private var mBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var sizesAdapter: SizesItemAdapter? = null
    private var frequentBoughItemsAdapter: FrequentlyBoughtItemAdapter? = null
    private var comboMealModifierGroupAdapter: ComboMealModifierGroupAdapter? = null
    private var comboItemDetailsModifierAdapter: ComboItemDetailsModifierAdapter? = null
    private var _totalPrice = 0.0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViews() {
        super.initViews()
        binding.viewModel = this@ComboMealFragment.viewModel
        setupImageHeight()
        setupFrequentlyBoughtItems()
        setupComboMealItems()
        setupComboMealDetailsBottomSheet()
        binding.apply {
            add.apply {
                if (isSoldOut) {
                    activity?.apply {
                        image.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                com.herfy.R.color.soldout
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
            cbtComboMeal.apply {
                setNavButtonAction {
                    navigateUp()
                }
                setRightButtonAction {
                    showLoading()
                    if (CacheUtil.getInstance().favorites.contains(this@ComboMealFragment.viewModel.menuItem?.id)) {
                        setRightButtonIcon(R.drawable.ic_favorite_filled)
                    } else {
                        setRightButtonIcon(R.drawable.ic_favorite_empty)
                    }
                    this@ComboMealFragment.viewModel.addRemoveFavorite()
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
                        this@ComboMealFragment.viewModel.updateCartItem(false, cartMenuItemPos)
                    } else {
                        this@ComboMealFragment.viewModel.getCartMenuItem()
                    }
                } else {
                    this@ComboMealFragment.viewModel.updateCartItem(true, cartMenuItemPos)
                }
                context?.let {
                    FirebaseECommerceEventUtil.addToCartEvent(
                        it,
                        this@ComboMealFragment.viewModel.menuItem!!,
                        this@ComboMealFragment.viewModel.menuCategory!!,
                        _totalPrice,
                        Integer.parseInt(binding.quantity.text.toString())
                    );
                    FacebookAppEventUtil.addToCartEvent(
                        it,
                        this@ComboMealFragment.viewModel.menuItem!!,
                        _totalPrice
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
                if (CacheUtil.getInstance().couponToAdd != null) {
                    CacheUtil.getInstance().couponToAdd.menuItem
                } else if (CacheUtil.getInstance().campaignToAdd != null) {
                    CacheUtil.getInstance().campaignToAdd.menuItem
                } else {
                    null
                }
            } else {
                if (CacheUtil.getInstance().couponCode != null &&
                    CacheUtil.getInstance().couponMenuItem != null
                ) {
                    CacheUtil.getInstance().couponMenuItem
                } else if (CacheUtil.getInstance().campaign != null &&
                    CacheUtil.getInstance().campaignPromotionMenuItem != null
                ) {
                    CacheUtil.getInstance().campaignPromotionMenuItem
                } else {
                    null
                }
            }
            menuItem?.let {
                viewModel.apply {
                    isFromCartEdit = isEdit
                    setPromoMenuItem(it)
                }
            }
        }

    }

    private fun setupImageHeight() {
        binding.image.apply {
           /* val ratio = Values.MENU_ITEM_IMAGE_ASPECT_RATIO_WIDTH / Values.MENU_ITEM_IMAGE_ASPECT_RATIO_HEIGHT
            val imageWidth = Resources.getSystem().displayMetrics.widthPixels * Values.POPUP_WIDTH_PERCENT
            val imageHeight = imageWidth / ratio
            layoutParams.height = imageHeight.toInt()*/
            val toolbarHeight = (Resources.getSystem().displayMetrics.widthPixels / 1.1).toInt()
            binding.mainToolbar.layoutParams.height = toolbarHeight
            binding.image.layoutParams.height = toolbarHeight
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            tvSoldOut.text =
                CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT")
            txtMoreInfo.text = CacheUtil.getInstance()
                .getTranslations(Text.MORE_INFO, "More Info")
            notesLabel.text =
                cacheUtilHelper.getTranslations(Text.SPECIAL_INSTRUCTION, "Special Instruction")
            notesDescription.text = cacheUtilHelper.getTranslations(
                Text.SPECIAL_INSTRUCTION_DESCRIPTION,
                "Please let us know if you are allergic to anything or if we need to avoid anything"
            )
            txtFrequntlyBought.text = cacheUtilHelper.getTranslations(
                Text.FREQUENTLY_BOUGHT_TOGETHER_LABEL,
                "Frequently bought together"
            )
            txtQuantity.text = cacheUtilHelper.getTranslations(Text.ITEM_QUANTITY, "Item quantity")
            if (cartMenuItemPos >= 0 || isEdit) {
                add.text = cacheUtilHelper.getTranslations(Text.UPDATE)
            } else {
                add.text =
                    cacheUtilHelper.getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                        .replace(
                            "(x)",
                            "${StringUtil.getFormattedPrice(this@ComboMealFragment.viewModel.cartMenuItemsTotalPrice)}"
                        )
            }
            layoutBottomSheetComboDetail.apply {
                tvTotalLabel.text = cacheUtilHelper.getTranslations(
                    Text.TOTAL_INCLUSIVE_VAT,
                    "Total is inclusive of VAT"
                )
                btnAddDetails.text = cacheUtilHelper.getTranslations(Text.ADD, "Add")
            }
            sizesLabel.text = cacheUtilHelper.getTranslations(Text.COMBO_SIZES_LABEL, "Combo Sizes")
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
                comboItemDetailsModifierAdapter?.setOnAction(this@ComboMealFragment.viewModel)
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
                        this@ComboMealFragment.viewModel.currentSelectedModifierItem = null
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
                comboMealModifierGroupAdapter =
                    ComboMealModifierGroupAdapter(comboViewModel = this@ComboMealFragment.viewModel)
                rvModifierGroup.apply {
                    adapter = comboMealModifierGroupAdapter
                    layoutManager = linearLayoutManager
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                }
            }
        }
    }

    private fun setupSizeItems(sizes: List<ModifierItem>) {
        binding.apply {
            ivComboSizeIcon.setImageResource(R.drawable.ic_combo_size)
            activity?.let { act ->
                val itemWidth =
                    (ViewUtil.getScreenWidth(act) / sizes.size) - ViewUtil.dpToPx(act, 20)
                val linearLayoutManager = GridLayoutManager(act, sizes.size)
                sizesAdapter = SizesItemAdapter(items = sizes, itemWidth = itemWidth)
                sizesAdapter?.setOnAction(this@ComboMealFragment.viewModel)
                sizesRecyclerView.apply {
                    adapter = sizesAdapter
                    layoutManager = linearLayoutManager
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                }

            }
        }
    }

    override fun observe() {
        super.observe()
        viewModel.apply {
            showError.observeNonNull(this@ComboMealFragment) {
                it.getContentIfNotHandled()?.let {
                    runOnMain {
                        dismissLoading()
                        showAlertDialog(
                            title = "", message = it.message,
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
            showMenuItemDetails.observeNonNull(this@ComboMealFragment) {
                setupMenuItemDetails(it)
            }
            showMenuItemQuantity.observeNonNull(this@ComboMealFragment) { quantityValue ->
                binding.minus.isFocusable = quantityValue != 1
                binding.minus.isClickable = quantityValue != 1


                binding.quantity.text = StringUtil.localize(quantityValue.toString())
            }
            showMenuItemTotal.observe(this@ComboMealFragment) { totalPrice ->
                if (totalPrice <= 0.0 && isPromo) {
                    binding.tvTotal.text = CacheUtil.getInstance().getTranslations(
                        Text.FREE
                    )
                } else {
                    _totalPrice = totalPrice
                    val formattedPrice = CurrencyUtils.updateCurrency(
                        totalPrice,
                        withSpace = true,
                        ignoreDecimalPlaces = false
                    )
                    binding.tvTotal.text = formattedPrice
                    binding.add.text =
                        cacheUtilHelper.getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                            .replace("(x)", formattedPrice.toString())
                }
            }
            showComboSizes.observe(this@ComboMealFragment) {
                binding.apply {
                    if (it.size > 1) {
                        sizesLayoutTextContainer.visible()
                        sizesRecyclerView.visible()
                        viewSizesSeparator.visible()
                        val hasSelectedSize = it.any { it.attributes.isSelected }
                        if (hasSelectedSize) {
                            checkBox.visible()
                        } else {
                            checkBox.gone()
                        }
                        setupSizeItems(it)
                    } else {
                        sizesLayoutTextContainer.gone()
                        sizesRecyclerView.gone()
                        viewSizesSeparator.gone()
                    }
                }


            }
            showMenuItemComponent.observe(this@ComboMealFragment) {
                val comboMealModifierGroupCheckBoxMap = viewModel.showComboModifierGroupCheckBox(it)
                comboMealModifierGroupAdapter?.updateList(it, comboMealModifierGroupCheckBoxMap)
            }
            updateComponentModifierList.observe(this@ComboMealFragment) { indexes ->
                comboMealModifierGroupAdapter?.apply {
                    updateComboMealModifierItem(indexes.first, indexes.second)
                }
            }
            showComboMealItemDetails.observe(this@ComboMealFragment) { data ->
                showComboComponentModifierItemDetails(data)
            }
            showMenuItemCalories.observe(this@ComboMealFragment) {
                binding.apply {
                    if (it.isNotEmpty()) {
                        btnCalories.visible()
                        calories.text = it
                    } else {
                        btnCalories.gone()
                    }
                }
            }
            updateComboSizeList.observe(this@ComboMealFragment) {
                sizesAdapter?.updateSelectedSize(it.second, it.first)
            }
            updateComboItemDetailModifier.observe(this@ComboMealFragment) {
                val modifierItemsQuantity =
                    this@ComboMealFragment.viewModel.modifierItemQuantityCount(it.second.modifierItems)
                val isAboveMax =
                    this@ComboMealFragment.viewModel.isAboveMax(it.second, modifierItemsQuantity)
                comboItemDetailsModifierAdapter?.apply {
                    this@ComboMealFragment.viewModel.apply {
                        getComponentModifierItemTotalPrice(
                            getUpdatedModifierList()
                        )
                        updateModifierVisibilityBy(it.second){mapOfModifierVisibilityConfig ->
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
            showComponentModifierItemTotal.observe(this@ComboMealFragment) {
                val currentTotal = it + _totalPrice
                binding.layoutBottomSheetComboDetail.tvTotalValue.text =
                    CurrencyUtils.updateCurrency(
                        currentTotal    ,
                        withSpace = true,
                        ignoreDecimalPlaces = false
                    )
            }
            showToastMessage.observe(this@ComboMealFragment) {
                showToastMessage(it)
            }
            addCartItem.observe(this@ComboMealFragment) {
                activity?.let {
                    it.runOnUiThread {
                        val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                        dismissLoading()
                        showToastMessage(
                            CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART)
                        )
                        navigateUp()
                    }
                }

            }
            updateCartItem.observe(this@ComboMealFragment) {
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
            showCartExpiry.observe(this@ComboMealFragment) {
                activity?.cast<BaseActivity> {
                    it.runOnUiThread {
                        it.setCartExpiry()
                    }
                }
            }

            logFirebaseEvent.observe(this@ComboMealFragment) { value ->
                context?.let { ctx ->
                    if (Values.sDeepLinkUtm != null) {
                        val firebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
                        firebaseAnalytics.logEvent(value, Values.sDeepLinkUtm)
                    }
                }

            }
            logEvent.observe(this@ComboMealFragment) { attributes ->
                activity?.cast<BaseActivity> {
                    it.logEventAttributesMetric(
                        attributes.eventName,
                        attributes.eventAttributes,
                        attributes.eventMetrictName,
                        attributes.eventMetricValue
                    )
                }
            }
            logAddToWishlistEvent.observe(this@ComboMealFragment) { menuItem ->
                context?.let {
                    FacebookAppEventUtil.addToWishlistEvent(it, menuItem)
                }
            }
            setFavoriteItem.observe(this@ComboMealFragment) {
                dismissLoading()
                setFavorite(it)
            }
            showFrequentBoughtItems.observe(this@ComboMealFragment) {
                it.getContentIfNotHandled()?.let { list ->
                    binding.apply {
                        if (list.size > 0) {
                            txtFrequntlyBought.visible()
                            frequentlyBoughRecycleview.visible()
                            frequentBoughItemsAdapter?.updateList(list)
                        } else {
                            txtFrequntlyBought.gone()
                            frequentlyBoughRecycleview.gone()
                        }
                    }
                }

            }

            addFrequentlyBoughtItem.observe(this@ComboMealFragment) { pair ->
                activity?.let {
                    it.runOnUiThread {
                        val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                        frequentBoughItemsAdapter?.updateAddToCartItem(pair.second, pair.first)
                        dismissLoading()
                        showToastMessage(
                            CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART)
                        )
                    }
                }
            }
            navigateToMenuItemDetails.observe(this@ComboMealFragment) {
                it.getContentIfNotHandled()?.let { triple ->
                    val isCombo = triple.second.isCombo
                    val navController = findNavController()
                    if (isCombo) {
                        navController.navigate(
                            ComboMealFragmentDirections.actionComboToCombo(
                                triple.first.id,
                                triple.second.id,
                                isFrequentlyBoughtItem = true
                            )
                        )
                    } else {
                        navController.navigate(
                            ComboMealFragmentDirections.actionComboToMenuItem(
                                triple.second.id, triple.first.id,
                                isFrequentlyBoughtItem = true
                            )
                        )
                    }
                    selectedFrequentBoughtItemPos = triple.third
                }
            }
            updateFavoriteByPosition.observe(this@ComboMealFragment) { pair ->
                frequentBoughItemsAdapter?.updateFavorite(pair.first, pair.second)
            }
            showLoading.observe(this@ComboMealFragment) {
                if (it) showLoading() else dismissLoading()
            }
            modifierAdapterItems.observe(this@ComboMealFragment) {
                it.getContentIfNotHandled()?.let { modifierAdapterItems ->
                    comboItemDetailsModifierAdapter?.addData(modifierAdapterItems)
                }
            }
        }
    }

    private fun showToastMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFrequentlyBoughtItems() {
        if (!isPromo && menuItemId.isNotEmpty()) {
            if (viewModel.frequentBoughtItemList.isEmpty()) {
                // TODO: Remove this comment when client requests to re-enable frequently bought items feature.
//                viewModel.getFrequentBoughtItems(1, menuItemId)
            }
            context?.let { ctx ->
                binding.apply {
                    txtFrequntlyBought.gone()
                    frequentBoughItemsAdapter =
                        FrequentlyBoughtItemAdapter(listener = this@ComboMealFragment.viewModel)
                    frequentlyBoughRecycleview.apply {
                        layoutManager =
                            LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                        adapter = frequentBoughItemsAdapter
                    }
                }
            }

            setFragmentResultListener(Values.FREQUENTLY_BOUGHT_ITEM_LISTENER) { key, bundle ->
                val isAddedToFavorite = bundle.getBoolean(Values.KEY_IS_ADDED_TO_FAVORITES, false)
                val isAddedToCart = bundle.getBoolean(Values.KEY_IS_ADDED_TO_CART, false)

                if (selectedFrequentBoughtItemPos != -1 && (isAddedToCart || isAddedToFavorite)) {
                    viewModel.updateFrequentlyBoughtItems(
                        selectedFrequentBoughtItemPos,
                        isAddedToCart
                    )
                    selectedFrequentBoughtItemPos = -1
                }
            }
        }

    }

    private fun setupMenuItemDetails(data: MenuItem) {
        binding.apply {
            language = LocaleUtil.getInstance().language
            menuItem = data
            setFavorite(data)
            txtMoreInfo.isVisible =
                !data.customFieldObjectMap.nutritionInfoImageUrl.isNullOrEmpty()
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
        }
    }

    private fun setFavorite(menuItem: MenuItem) {
        binding.cbtComboMeal.apply {
            if (!isPromo && AuthUtil.getInstance().isSignedIn && CacheUtil.getInstance().favorites.contains(
                    menuItem.id
                )
            ) {
                setRightButtonIcon(R.drawable.ic_favorite_filled)
            } else {
                setRightButtonIcon(R.drawable.ic_favorite_empty)
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
                this@ComboMealFragment.viewModel.currentSelectedModifierItem = this
                viewModel.getModifierAdapterItem(modifierGroupsCopy)
                tvTitle.text = name.toTranslate()
                this@ComboMealFragment.viewModel.getComponentModifierItemTotalPrice(
                    modifierGroupsCopy
                )
                btnAddDetails.setOnClickListener {
                    if (data.previousComponentModifierIndex != data.componentModifierItemIndex) {
                        viewModel.updatePreviousComponentModifierItem(
                            data.menuItemComponentIndex,
                            data.previousComponentModifierIndex
                        )
                    }
                    viewModel.updateSelectedModifierItem(
                        data.menuItemComponentIndex,
                        data.componentModifierItemIndex,
                        this,
                        comboItemDetailsModifierAdapter?.getUpdatedModifierList()
                    )
                    comboMealModifierGroupAdapter?.updateComboMealModifierItem(
                        data.menuItemComponentIndex,
                        data.componentModifierItemIndex
                    )
                    collapseComboItemDetails()
                }
            }
            expandComboItemDetails()
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