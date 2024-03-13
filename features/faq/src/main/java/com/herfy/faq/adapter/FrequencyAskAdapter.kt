

package com.herfy.faq.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import com.herfy.faq.R
import com.herfy.faq.databinding.ItemFrequentBinding
import com.herfy.utils.base.mvvm.BaseListAdapter
import com.herfy.utils.extensions.setVisible

class FrequencyAskAdapter(private val onFrequencyClicked: (FrequencyAskUIModel) -> Unit): BaseListAdapter<FrequencyAskUIModel,BaseListAdapter.BaseViewViewHolder<FrequencyAskUIModel>>(FrequentCallback()) {

    var tracker: SelectionTracker<FrequencyAskUIModel>? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewViewHolder<FrequencyAskUIModel> {
        return FrequentViewHolder(
            DataBindingUtil
                .inflate(LayoutInflater.from(parent.context),
                    R.layout.item_frequent,
                    parent,
                    false
                ),
            parent.context
        )
    }


    override fun onBindViewHolder(holder: BaseViewViewHolder<FrequencyAskUIModel>, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FrequentViewHolder(override val binding: ItemFrequentBinding,private val context: Context) :  BaseViewViewHolder<FrequencyAskUIModel>(binding) {
        private var frequencyAskUIModel: FrequencyAskUIModel? =null

        init {
            binding.icPlus.setOnClickListener {
                frequencyAskUIModel?.let { frequencyAskQuestion -> onFrequencyClicked(frequencyAskQuestion) }
            }
        }

        override fun bind(item: FrequencyAskUIModel) {
            this.frequencyAskUIModel = item
            val isSelected = tracker?.isSelected(item) ?: false
            binding.frequentlyBackground.isSelected = isSelected
            binding.txtFrequentTitle.text = item.frequencyTitle
            if (binding.frequentlyBackground.isSelected) {
                onFrequencyClicked(item)
            }
            binding.frequentlyBackground.background = if (isSelected) ContextCompat.getDrawable(context, R.drawable.bg_faq_state_selected) else ContextCompat.getDrawable(context,R.drawable.bg_unselected_state)
            binding.icPlus.setImageResource(if (isSelected) R.drawable.ic_common_minus  else R.drawable.ic_common_black_plus)
            binding.webViewParentLayout.setVisible(isSelected)
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.loadUrl(item.url)
        }
        fun getItemDetails(): ItemDetailsLookup.ItemDetails<FrequencyAskUIModel> =
            object : ItemDetailsLookup.ItemDetails<FrequencyAskUIModel>() {
                override fun getPosition(): Int =adapterPosition
                override fun getSelectionKey(): FrequencyAskUIModel? = getItem(adapterPosition)

                // enable single tap selection
                override fun inSelectionHotspot(e: MotionEvent) = true
            }
    }


     class FrequentCallback(): DiffUtil.ItemCallback<FrequencyAskUIModel>() {
        override fun areItemsTheSame(
            oldItem: FrequencyAskUIModel,
            newItem: FrequencyAskUIModel
        ): Boolean =
            oldItem == newItem


        override fun areContentsTheSame(
            oldItem: FrequencyAskUIModel,
            newItem: FrequencyAskUIModel
        ): Boolean =  oldItem == newItem

    }
}