package com.herfy.home.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromotionCategoryBinding
import com.herfy.model.PromotionCategory

class PromotionCategoryAdapter(
    private var categoryList : List<PromotionCategory>? = null,
    private val categoryListener : PromotionCategoryListener,
) : RecyclerView.Adapter<PromotionCategoryAdapter.ViewHolder>(){

    inner class ViewHolder(val binding : ItemPromotionCategoryBinding) : RecyclerView.ViewHolder(binding.root)


    interface PromotionCategoryListener{
        fun selectedCategory(category: PromotionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : ItemPromotionCategoryBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_promotion_category,
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = categoryList?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        categoryList?.let {
            val data = it[position]
            holder.binding.apply {
                category = data
                listener = categoryListener
                categoryPos = position
                executePendingBindings()
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCategoryList(category: PromotionCategory){
        categoryList?.forEach {
            if(category.name != it.name){
                it.isSelected = false
            }
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(promoCategories: List<PromotionCategory>) {
        categoryList = promoCategories
        notifyDataSetChanged()
    }
}