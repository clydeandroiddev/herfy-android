package com.herfy.productpagedetails.ui.view

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.extensions.toTranslate
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComboMealFragment : BaseFragment<ComboViewModel, FragmentComboMealBinding>() {

    @Inject
    lateinit var cacheUtilHelper: CacheUtilHelper

    private val args by lazy { arguments }

    override val viewModel: ComboViewModel by viewModels()
    override val layout: Int = R.layout.fragment_combo_meal

    private var mBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var sizesAdapter: SizesItemAdapter? = null
    private var frequentBoughItemsAdapter: FrequentlyBoughtItemAdapter? = null
    private var comboMealModifierGroupAdapter: ComboMealModifierGroupAdapter? = null
    private var comboItemDetailsModifierGroupAdapter : ComboItemDetailsModifierGroupAdapter? = null

    private val isPromo : Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_PROMO, false) ?: false
    }
    private val menuItemId : String by lazy {
        args?.getString(Values.KEY_MENU_ITEM_ID) ?: ""
    }
    private val isUpsell : Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_UPSELL, false) ?: false
    }
    private val isEdit : Boolean by lazy {
        args?.getBoolean(Values.KEY_IS_EDIT, false) ?: false
    }
    private val cartMenuItemPos : Int by lazy {
        args?.getInt(Values.KEY_CART_MENU_ITEM_POSITION, -1) ?: -1
    }
    private val menuCategoryId : String by lazy {
        args?.getString(Values.KEY_MENU_CATEGORY_ID, "") ?: ""
    }

    override fun initViews() {
        super.initViews()
        binding.viewModel = this@ComboMealFragment.viewModel
        setupFrequentlyBoughtItems()
        setupSizeItems()
        setupComboMealItems()
        setupComboMealDetailsBottomSheet()
        if(!isPromo){
            if(menuItemId.isNotEmpty()){
                viewModel.apply {
                    isFromCartEdit = false
                    if(menuCategoryId.isNotEmpty()){
                        setMenuCategory(menuCategoryId)
                    }
                    getMenuItemFromDbBy(false , menuItemId, isUpsell)
                }
            }else if(cartMenuItemPos != -1){
                viewModel.apply {
                    getMenuItemFromCartBy(cartMenuItemPos, isUpsell)
                }
            }
        }else {
            val menuItem : MenuItem? = if(isEdit){
                if (CacheUtil.getInstance().couponToAdd != null) {
                    CacheUtil.getInstance().couponToAdd.menuItem
                } else if (CacheUtil.getInstance().campaignToAdd != null) {
                    CacheUtil.getInstance().campaignToAdd.menuItem
                } else {
                    null
                }
            }else {
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
            notesLabel.text = cacheUtilHelper.getTranslations(Text.SPECIAL_INSTRUCTION, "Special Instruction")
            notesDescription.text = cacheUtilHelper.getTranslations(Text.SPECIAL_INSTRUCTION_DESCRIPTION, "Please let us know if you are allergic to anything or if we need to avoid anything")
            txtFrequntlyBought.text = cacheUtilHelper.getTranslations(Text.FREQUENTLY_BOUGHT_TOGETHER_LABEL, "Frequently bought together")
            txtQuantity.text = cacheUtilHelper.getTranslations(Text.ITEM_QUANTITY,"Item quantity")
            if (cartMenuItemPos >= 0 || isEdit) {
               add.text = cacheUtilHelper.getTranslations(Text.UPDATE)
            } else {
                add.text = cacheUtilHelper.getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                    .replace("(x)","${StringUtil.getFormattedPrice(this@ComboMealFragment.viewModel.cartMenuItemsTotalPrice)}")
            }
            layoutBottomSheetComboDetail.apply {
                tvTotalLabel.text = cacheUtilHelper.getTranslations(Text.TOTAL_IS_INCLUSIVE_OF_VAT, "Total is inclusive of VAT")
                btnAddDetails.text = cacheUtilHelper.getTranslations(Text.ADD, "Add")
            }
        }
    }

    private fun setupComboMealDetailsBottomSheet() {
        binding.apply {
            BottomSheetBehavior.from(layoutBottomSheetComboDetail.clBottomSheetDetailsMain).let { bsb->
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                mBottomSheetBehavior = bsb
            }

            layoutBottomSheetComboDetail.apply {
                icClose.setOnClickListener {
                    collapseComboItemDetails()
                }
                comboItemDetailsModifierGroupAdapter = ComboItemDetailsModifierGroupAdapter()
                comboItemDetailsModifierGroupAdapter?.setOnAction(this@ComboMealFragment.viewModel)
                context?.let {ctx ->
                    val linearLayoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    rvModifiers.apply {
                        adapter = comboItemDetailsModifierGroupAdapter
                        layoutManager = linearLayoutManager
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = false
                    }
                }
                add.setOnClickListener {
                    collapseComboItemDetails()
                }


            }
        }
    }

    private fun setupComboMealItems() {
        binding.apply {
            context?.let { ctx ->
                val linearLayoutManager =
                    LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                comboMealModifierGroupAdapter = ComboMealModifierGroupAdapter(comboViewModel = this@ComboMealFragment.viewModel)
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
            activity?.let { act ->
                val itemWidth = ViewUtil.getScreenWidth(act) / 3
                val linearLayoutManager =
                    LinearLayoutManager(act, LinearLayoutManager.HORIZONTAL, false)
                sizesAdapter = SizesItemAdapter(listener = this@ComboMealFragment.viewModel, itemWidth = itemWidth)
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


                if (quantityValue != 1) {
                    context?.let {
                        binding.minus.setColorFilter(
                            it.resources.getColor(R.color.background_color_blue, null),
                            PorterDuff.Mode.SRC_IN
                        )
                    }

                } else {
                    binding.minus.setColorFilter(
                        Color.parseColor("#BEBEBE"),
                        PorterDuff.Mode.SRC_IN
                    )
                }
                binding.quantity.text = StringUtil.localize(quantityValue.toString())
            }
            showMenuItemTotal.observe(this@ComboMealFragment){totalPrice ->
                if (totalPrice <= 0.0 && isPromo) {
                    binding.total.text = CacheUtil.getInstance().getTranslations(
                        Text.FREE
                    )
                } else {
                    binding.total.text = StringUtil.getFormattedPrice(totalPrice)
                }
            }
            showComboSizes.observe(this@ComboMealFragment){
                sizesAdapter?.updateList(it)
            }
            showMenuItemComponent.observe(this@ComboMealFragment){
                val comboMealModifierGroupCheckBoxMap = viewModel.showComboModifierGroupCheckBox(it)
                comboMealModifierGroupAdapter?.updateList(it, comboMealModifierGroupCheckBoxMap)
            }
            updateComponentModifierList.observe(this@ComboMealFragment){ indexes ->
                comboMealModifierGroupAdapter?.apply {
                    updateComboMealModifierItem(indexes.first, indexes.second)
                }
            }
            showComboMealItemDetails.observe(this@ComboMealFragment){data ->
                showComboComponentModifierItemDetails(data)
            }
        }
    }

    private fun setupFrequentlyBoughtItems() {
        viewModel.getFrequentBoughtItems()
        context?.let { ctx ->
            binding.apply {
                frequentBoughItemsAdapter = FrequentlyBoughtItemAdapter(listener = this@ComboMealFragment.viewModel)
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
        }
    }

    private fun expandComboItemDetails(){
        mBottomSheetBehavior?.let{
            it.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun collapseComboItemDetails(){
        mBottomSheetBehavior?.let{
            if(it.state == BottomSheetBehavior.STATE_EXPANDED){
                it.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun showComboComponentModifierItemDetails(data: ShowComboMealItemDetails) {
        binding.layoutBottomSheetComboDetail.apply {
            data.componentModifierItem.apply {
                val showModifierItemPrice = viewModel.showModifierItemPricePer(this)
                val showModifierCheckBox = viewModel.showModifierItemCheckBoxPer(this)
                tvTitle.text = name.toTranslate()
                comboItemDetailsModifierGroupAdapter?.updateList(modifierGroups,
                    showModifierItemPrice,
                    showModifierCheckBox)
            }
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}