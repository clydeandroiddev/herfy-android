package com.herfy.order.views.ordertype

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.ordertype.databinding.ItemAddressDeliveryNewBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import io.shipbook.shipbooksdk.Log

class DeliveryAddressAdapter(
    val addressList: ArrayList<Address> = arrayListOf(),
    val addressListener: AddressListener,
    var selectedId: String
) : RecyclerView.Adapter<DeliveryAddressAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemAddressDeliveryNewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemAddressDeliveryNewBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_address_delivery_new,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = addressList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = addressList[position]
        holder.binding.apply {
            if (selectedId != item.id) {
                deliveryMain.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_circle_unselected)
            } else {
                deliveryMain.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver_selected)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_store_selected)
            }
            editDelivery.setOnClickListener {
                addressListener.onEditAddress(item, position)
            }

            deliveryMain.setOnClickListener {
                addressListener.onSelectedAddress(item, position)
            }

            val labelValue = item.attributes.label
            address.text = item.attributes.line1
            val type = if (labelValue != null && !labelValue.isEmpty() && !labelValue.equals(
                    "null", ignoreCase = true
                )
            ) {
                labelValue
            } else {
                CacheUtil.getInstance().getTranslations(Text.OTHER, "Other")
            }
            addressType.text = type
            executePendingBindings()
        }
    }

    fun updateDeliveryList(promoList: ArrayList<Address>) {
        this.addressList.clear()
        this.addressList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface AddressListener {
        fun onSelectedAddress(address: Address, position: Int)
        fun onEditAddress(address: Address, position: Int)
    }
}