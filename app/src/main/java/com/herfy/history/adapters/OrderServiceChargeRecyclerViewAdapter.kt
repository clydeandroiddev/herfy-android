package com.herfy.history.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.herfy.R
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge
import com.herfy.util.StringUtil

class OrderServiceChargeRecyclerViewAdapter(private val charges: List<ServiceCharge>) :
	RecyclerView.Adapter<OrderServiceChargeRecyclerViewAdapter.ItemViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
		return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_service_charge, parent, false))
	}

	override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
		holder.serviceChargeLabel.text = charges[position].name
		holder.serviceCharge.text = StringUtil.getFormattedPrice(charges[position].amount)
	}

	override fun getItemCount(): Int = charges.size

	inner class ItemViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
		var serviceChargeLabel: TextView = itemView.findViewById(R.id.service_charge_label)
		var serviceCharge: TextView = itemView.findViewById(R.id.service_charge)
	}
}