package com.herfy.order.views.ordertype.selectarea.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.ordertype.databinding.ItemCityBinding
import com.skylinedynamics.solosdk.api.models.objects.area.Area
import java.util.Locale.getDefault

class SelectAreaAdapter(
    val cityList: ArrayList<Area> = arrayListOf(),
    val listener: SelectCityListener,
    var selectedId: String
) : RecyclerView.Adapter<SelectAreaAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCityBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_city,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = cityList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cityList[position]
        holder.binding.apply {

            if (selectedId.lowercase(getDefault()) != item.name.lowercase(getDefault())) {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_circle_unselected)
            } else {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver_selected)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_store_selected)
            }

            brandName.text = item.name

            chooseCurbside.setOnClickListener {
                listener.onSelectedCity(item, position)
            }

            executePendingBindings()
        }
    }

    fun updateList(promoList: ArrayList<Area>) {
        this.cityList.clear()
        this.cityList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface SelectCityListener {
        fun onSelectedCity(city: Area, position: Int)
    }
}