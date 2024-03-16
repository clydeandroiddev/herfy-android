package com.herfy.productpagedetails.ui.view

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.FragmentComboMealBinding
import com.herfy.productpagedetails.ui.adapter.ComboMealModifierGroupAdapter
import com.herfy.productpagedetails.ui.adapter.FrequentlyBoughtItemAdapter
import com.herfy.productpagedetails.ui.adapter.SizesItemAdapter
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.herfy.utils.base.mvvm.BaseFragment

class ComboMealFragment : BaseFragment<ComboViewModel, FragmentComboMealBinding>() {

    private val args by lazy { arguments }

    override val viewModel: ComboViewModel by viewModels()
    override val layout: Int = R.layout.fragment_combo_meal

    private var sizesAdapter: SizesItemAdapter? = null
    private var frequentBoughItemsAdapter: FrequentlyBoughtItemAdapter? = null
    private var comboMealItemAdapter : ComboMealModifierGroupAdapter? = null

    override fun initViews() {
        super.initViews()
        setupFrequentlyBoughtItems()
    }

    override fun observe() {
        super.observe()
    }


    private fun setupFrequentlyBoughtItems(){
        viewModel.getFrequentBoughttItems()
        context?.let {ctx ->
            binding.apply {
                frequentlyBoughRecycleview.apply {
                    layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                    adapter
                }
            }
        }

    }
}