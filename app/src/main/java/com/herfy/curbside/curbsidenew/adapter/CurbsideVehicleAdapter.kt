package com.herfy.curbside.curbsidenew.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.ordertype.databinding.ItemCurbsideNewBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class CurbsideVehicleAdapter(
    val curbsideList: ArrayList<CustomerCar> = arrayListOf(),
    val curbsideListener: CurbsideListener,
    var selectedId: String
) : RecyclerView.Adapter<CurbsideVehicleAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCurbsideNewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCurbsideNewBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_curbside_new,
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

            curbsidePlateNumber.text = item.attributes.plateNumber

            chooseCurbside.setOnClickListener {
                curbsideListener.onSelectedCar(item, position)
            }
            curbsideAdded.text =
                CacheUtil.getInstance().getTranslations(Text.ADDED_CURBSIDE, "Added: (x)")
                    .replace("(x)", item.attributes.createdAt)

            executePendingBindings()
        }
    }

    fun updateList(promoList: ArrayList<CustomerCar>) {
        this.curbsideList.clear()
        this.curbsideList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface CurbsideListener {
        fun onSelectedCar(car: CustomerCar, position: Int)
    }
}