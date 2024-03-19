package com.herfy.productpagedetails.ui.view

import android.graphics.Color
import android.graphics.PorterDuff
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.FragmentComboMealBinding
import com.herfy.productpagedetails.ui.adapter.ComboMealModifierGroupAdapter
import com.herfy.productpagedetails.ui.adapter.FrequentlyBoughtItemAdapter
import com.herfy.productpagedetails.ui.adapter.SizesItemAdapter
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem



class ComboMealFragment : BaseFragment<ComboViewModel, FragmentComboMealBinding>() {

    private val args by lazy { arguments }

    override val viewModel: ComboViewModel by viewModels()
    override val layout: Int = R.layout.fragment_combo_meal

    private var sizesAdapter: SizesItemAdapter? = null
    private var frequentBoughItemsAdapter: FrequentlyBoughtItemAdapter? = null
    private var comboMealItemAdapter: ComboMealModifierGroupAdapter? = null

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
        setupFrequentlyBoughtItems()
        setupSizeItems()
        setupComboMealItems()
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

    private fun setupComboMealItems() {
        binding.apply {
            context?.let { ctx ->
                val linearLayoutManager =
                    LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                comboMealItemAdapter = ComboMealModifierGroupAdapter(viewModel = viewModel)
                rvModifierGroup.apply {
                    adapter = comboMealItemAdapter
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
                sizesAdapter = SizesItemAdapter(listener = viewModel, itemWidth = itemWidth)
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
            showMenuItemDetails.observeNonNull(this) {
                setupMenuItemDetails(it)
            }

            showMenuItemQuantity.observeNonNull(this) { quantityValue ->
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

            showMenuItemTotal.observe(this){totalPrice ->
                if (totalPrice <= 0.0 && isPromo) {
                    binding.total.text = CacheUtil.getInstance().getTranslations(
                        Text.FREE
                    )
                } else {
                    binding.total.text = StringUtil.getFormattedPrice(totalPrice)
                }
            }

            showComboSizes.observe(this){
                sizesAdapter?.updateList(it)
            }

            showMenuItemComponent.observe(this){
                comboMealItemAdapter?.updateList(it)
            }


        }
    }


    private fun setupFrequentlyBoughtItems() {
        viewModel.getFrequentBoughttItems()
        context?.let { ctx ->
            binding.apply {
                frequentBoughItemsAdapter = FrequentlyBoughtItemAdapter(listener = viewModel)
                frequentlyBoughRecycleview.apply {
                    layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    adapter = frequentBoughItemsAdapter
                }
            }
        }
    }

    private fun setupMenuItemDetails(data: MenuItem) {
        binding.apply {
            menuItem = data
        }
    }
}