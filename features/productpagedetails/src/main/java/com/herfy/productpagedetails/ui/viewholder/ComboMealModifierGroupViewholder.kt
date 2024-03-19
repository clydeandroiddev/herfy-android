package com.herfy.productpagedetails.ui.viewholder

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.adapter.ComboMealModifierItemAdapter
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents

class ComboMealModifierGroupViewholder(val binding: ItemModifierGroupNewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun setComboModifierGroup(
        data: MenuItemComponents,
        menuItemComponentPos : Int,
        viewModel: ComboViewModel
    ) {
        binding.apply {
            val context = itemView.context

            name.text = data.name.getValue(LocaleUtil.getInstance().language)
            checkBox.gone()
            minimum.text = CacheUtil.getInstance().getTranslations(Text.REQUIRED, "Required")
            select.visibility = View.VISIBLE
            select.text = CacheUtil.getInstance().getTranslations(Text.SELECT_X, "Select (x)").replace("(x)", "one")
            val comboMealModifierItemAdapter = ComboMealModifierItemAdapter(listener = viewModel, menuItemComponentPos = menuItemComponentPos)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = comboMealModifierItemAdapter
            }
            viewModel.getMenuItemComponentPerRow(menuItemComponentPos){
                comboMealModifierItemAdapter.updateList(it.items)
            }
        }
    }
}
