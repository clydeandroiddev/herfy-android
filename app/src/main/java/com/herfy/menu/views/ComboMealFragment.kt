package com.herfy.menu.views

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.base.BaseActivity
import com.herfy.model.ShowComboMealItemDetails
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.FragmentComboMealBinding
import com.herfy.productpagedetails.ui.adapter.ComboItemDetailsModifierGroupAdapter
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
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComboMealFragment : BaseFragment<ComboViewModel, FragmentComboMealBinding>() {

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
    private var comboItemDetailsModifierGroupAdapter: ComboItemDetailsModifierGroupAdapter? = null
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

    override fun initViews() {
        super.initViews()
        binding.viewModel = this@ComboMealFragment.viewModel
        setupFrequentlyBoughtItems()
        setupSizeItems()
        setupComboMealItems()
        setupComboMealDetailsBottomSheet()
        binding.apply {
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

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
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
                    Text.TOTAL_IS_INCLUSIVE_OF_VAT,
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
                comboItemDetailsModifierGroupAdapter = ComboItemDetailsModifierGroupAdapter()
                comboItemDetailsModifierGroupAdapter?.setOnAction(this@ComboMealFragment.viewModel)
                context?.let { ctx ->
                    val linearLayoutManager =
                        LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    rvModifiers.apply {
                        adapter = comboItemDetailsModifierGroupAdapter
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
                add.setOnClickListener {
                    collapseComboItemDetails()
                }


            }

            mBottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.viewBgTransparent.gone()
                        this@ComboMealFragment.viewModel.currentSelectedModifierItem = null
                        binding.layoutBottomSheetComboDetail.btnAddDetails.setOnClickListener(null)
                        comboItemDetailsModifierGroupAdapter?.updateList(
                            arrayListOf(),
                            hashMapOf(),
                            hashMapOf()
                        )
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

    private fun setupSizeItems() {
        binding.apply {
            ivComboSizeIcon.setImageResource(R.drawable.ic_combo_size)
            activity?.let { act ->
                val itemWidth = (ViewUtil.getScreenWidth(act) / 3) - ViewUtil.dpToPx(act, 20)
                val linearLayoutManager = GridLayoutManager(act, 3)
                sizesAdapter = SizesItemAdapter(itemWidth = itemWidth)
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
                    val formattedPrice = StringUtil.getFormattedPrice(totalPrice)
                    binding.tvTotal.text = formattedPrice
                    binding.add.text =
                        cacheUtilHelper.getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                            .replace("(x)", formattedPrice.toString())
                }
            }
            showComboSizes.observe(this@ComboMealFragment) {
                binding.apply {
                    if (it.isNotEmpty()) {
                        sizesLayoutTextContainer.visible()
                        sizesRecyclerView.visible()
                        val hasSelectedSize = it.any { it.attributes.isSelected }
                        if (hasSelectedSize) {
                            checkBox.visible()
                        } else {
                            checkBox.gone()
                        }
                        sizesAdapter?.updateList(it)
                    } else {
                        sizesLayoutTextContainer.gone()
                        sizesRecyclerView.gone()
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
                comboItemDetailsModifierGroupAdapter?.apply {
                    viewModel.getComponentModifierItemTotalPrice(this.getUpdateModifierGroupList())
                    shouldShowCheckBox(it.first, it.second)
                    updateComboModifierItemsPerModifierGroup(it.second)
                }
            }
            showComponentModifierItemTotal.observe(this@ComboMealFragment) {
                binding.layoutBottomSheetComboDetail.tvTotalValue.text =
                    StringUtil.getFormattedPrice(it)
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
        }
    }

    private fun showToastMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFrequentlyBoughtItems() {
        viewModel.getFrequentBoughtItems()
        context?.let { ctx ->
            binding.apply {
                txtFrequntlyBought.gone()
                frequentBoughItemsAdapter =
                    FrequentlyBoughtItemAdapter(listener = this@ComboMealFragment.viewModel)
                frequentlyBoughRecycleview.apply {
                    layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    adapter = frequentBoughItemsAdapter
                }
            }
        }
    }

    private fun setupMenuItemDetails(data: MenuItem) {
        binding.apply {
            language = LocaleUtil.getInstance().language
            menuItem = data
            setFavorite(data)
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
                val showModifierItemPrice = viewModel.showModifierItemPricePer(this)
                val showModifierCheckBox = viewModel.showModifierItemCheckBoxPer(this)
                tvTitle.text = name.toTranslate()
                this@ComboMealFragment.viewModel.getComponentModifierItemTotalPrice(
                    modifierGroupsCopy
                )
                comboItemDetailsModifierGroupAdapter?.updateList(
                    modifierGroupsCopy,
                    showModifierItemPrice,
                    showModifierCheckBox
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
                        comboItemDetailsModifierGroupAdapter?.getUpdateModifierGroupList()
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