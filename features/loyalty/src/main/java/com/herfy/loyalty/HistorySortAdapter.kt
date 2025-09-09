package com.herfy.loyalty

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.loyalty.databinding.ItemHistorySortBinding
import com.herfy.model.SortingCategory

class HistorySortAdapter(private val sortCategoryList : ArrayList<SortingCategory> = arrayListOf(),
    private val categoryListener: CategoryListener)  : RecyclerView.Adapter<HistorySortAdapter.ViewHolder>() {
    inner class ViewHolder(val binding : ItemHistorySortBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : ItemHistorySortBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_history_sort,
            parent,
            false
        )
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int = sortCategoryList.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryData = sortCategoryList[position]
        holder.binding.apply {
            category = categoryData
            listener = categoryListener
            executePendingBindings()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCategoryList(category: SortingCategory){
        sortCategoryList.forEach {
            it.isSelected = (it.sortType == category.sortType)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(categories: ArrayList<SortingCategory>) {
        sortCategoryList.clear()
        sortCategoryList.addAll(categories)
        notifyDataSetChanged()
    }

    interface CategoryListener{
        fun selectCategory(category: SortingCategory)
    }


}