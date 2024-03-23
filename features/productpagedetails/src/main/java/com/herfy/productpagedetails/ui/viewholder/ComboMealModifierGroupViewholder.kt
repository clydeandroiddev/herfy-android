package com.herfy.productpagedetails.ui.viewholder

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.adapter.ComboMealModifierItemAdapter
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents

class ComboMealModifierGroupViewholder(
    val binding: ItemModifierGroupNewBinding,
    private val viewModel: ComboViewModel
) :
    RecyclerView.ViewHolder(binding.root) {

    private var listener: ComboMealModifierItemViewHolder.onClickListener? = null
    fun setListener(listener: ComboMealModifierItemViewHolder.onClickListener?) {
        this.listener = listener
    }

    fun setComboModifierGroup(
        data: MenuItemComponents,
        menuItemComponentPos: Int,
        comboMealModifierItemAdapters: HashMap<Int, ComboMealModifierItemAdapter>,
        checkBoxIndicator: Boolean,
        ) {
        binding.apply {
            val context = itemView.context

            name.text = data.name.getValue(LocaleUtil.getInstance().language)
            minimum.gone()
            if(checkBoxIndicator){
                minimum.gone()
                checkBox.visible()
            }else {
                checkBox.gone()
                minimum.visible()
                minimum.text = CacheUtil.getInstance().getTranslations(Text.REQUIRED, "Required")
            }
            select.visibility = View.VISIBLE
            select.text = CacheUtil.getInstance().getTranslations(Text.SELECT_X, "Select (x)")
                .replace("(x)", "one")
            val comboMealModifierItemAdapter =
                ComboMealModifierItemAdapter(menuItemComponentPos = menuItemComponentPos)
            comboMealModifierItemAdapter.setListener(listener)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = comboMealModifierItemAdapter
            }
            viewModel.getComponentModifierItemPer(menuItemComponentPos) {
                comboMealModifierItemAdapter.updateList(it.items)
            }
            comboMealModifierItemAdapters[menuItemComponentPos] = comboMealModifierItemAdapter
        }
    }
}
