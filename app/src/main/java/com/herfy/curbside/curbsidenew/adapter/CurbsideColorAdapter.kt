package com.herfy.curbside.curbsidenew.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.commonstyles.R
import com.herfy.ordertype.databinding.ItemVehicleColorBinding
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor

class CurbsideColorAdapter(
    val curbsideList: ArrayList<CarColor> = arrayListOf(),
    val curbsideListener: CurbsideListener,
    var selectedId: String
) : RecyclerView.Adapter<CurbsideColorAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemVehicleColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemVehicleColorBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_vehicle_color,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = curbsideList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = curbsideList[position]
        holder.binding.apply {

            if (selectedId != item.id) {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_circle_unselected)
            } else {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver_selected)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_store_selected)
            }

            brandName.text = item.attributes.name

            if (item.attributes.label.toLowerCase() != "white") {
                curbsideColor.strokeColor =
                    curbsideColor.context.resources.getColor(android.R.color.transparent)
            } else {
                curbsideColor.strokeColor =
                    curbsideColor.context.resources.getColor(R.color.color_stroke)
            }

            curbsideColor.setCardBackgroundColor(
                Color.parseColor(
                    "#" + item.attributes.color.replace(
                        "#",
                        ""
                    )
                )
            )

            chooseCurbside.setOnClickListener {
                curbsideListener.onSelectedColor(item, position)
            }

            executePendingBindings()
        }
    }

    fun updateList(promoList: ArrayList<CarColor>) {
        this.curbsideList.clear()
        this.curbsideList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface CurbsideListener {
        fun onSelectedColor(car: CarColor, position: Int)
    }
}