package com.herfy.account.account.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.ordertype.R
import com.herfy.profile.databinding.ItemUserAddressesBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address

class UsersAddressesAdapter(
    val customerAddresses: ArrayList<Address> = arrayListOf(),
    val addressUserListner: AddressUserListener
) : RecyclerView.Adapter<UsersAddressesAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemUserAddressesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemUserAddressesBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.profile.R.layout.item_user_addresses,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = customerAddresses.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = customerAddresses[position]
        holder.binding.apply {
            executePendingBindings()
            editItem.setOnClickListener {
                addressUserListner.onEditAddress(item, position)
            }
            deleteItem.setOnClickListener {
                addressUserListner.onDeleteAddress(item, position)
            }

            val labelValue = item.attributes.label
            val type = if (labelValue != null && !labelValue.isEmpty() && !labelValue.equals(
                    "null", ignoreCase = true
                )
            ) {
                labelValue
            } else {
                CacheUtil.getInstance().getTranslations(Text.OTHER, "Other")
            }
            if (type.equals("home", ignoreCase = true)) {
                cardImage.setImageResource(R.drawable.ic_home_label)
            } else if (type.equals("work", ignoreCase = true)) {
                cardImage.setImageResource(R.drawable.ic_label_work)
            } else {
                cardImage.setImageResource(R.drawable.ic_address_user_location)
            }

            addressType.text = type
            address.text = item.attributes.line1
            if (!item.attributes.instructions.isNullOrEmpty()) {
                noteToRider.text = CacheUtil.getInstance()
                    .getTranslations(Text.NOTE_TO_RIDER_LABEL, "Note to rider: (x)")
                    .replace("(x)", item.attributes.instructions)
                noteToRider.isVisible = true
            } else {
                noteToRider.isVisible = false
            }
        }
    }

    fun updateList(address: ArrayList<Address>) {
        this.customerAddresses.clear()
        this.customerAddresses.addAll(address)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        this.customerAddresses.removeAt(position)
        notifyDataSetChanged()
    }

    interface AddressUserListener {
        fun onEditAddress(address: Address, position: Int)
        fun onDeleteAddress(address: Address, position: Int)
    }
}