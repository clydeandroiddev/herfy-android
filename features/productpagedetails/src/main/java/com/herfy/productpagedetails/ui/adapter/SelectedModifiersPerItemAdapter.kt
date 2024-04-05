package com.herfy.productpagedetails.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemComboSelectedModifierBinding

class SelectedModifiersPerItemAdapter(val items : List<String>) : RecyclerView.Adapter<SelectedModifiersPerItemAdapter.ViewHolder>(){
    inner class ViewHolder(val binding : ItemComboSelectedModifierBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        ItemComboSelectedModifierBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            return ViewHolder(this)
        }
    }
    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.binding.apply {
           modifierItem = items[position]
           executePendingBindings()
       }
    }
}