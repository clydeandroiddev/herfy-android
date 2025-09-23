package com.herfy.account.account.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.R
import com.herfy.profile.databinding.ItemUserPaymentMethodsBinding
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard

class PaymentMethodsAdapter(
    val customerCards: ArrayList<CustomerCard> = arrayListOf(), val cardListener: CardListener
) : RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemUserPaymentMethodsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemUserPaymentMethodsBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.profile.R.layout.item_user_payment_methods,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = customerCards.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = customerCards[position]
        holder.binding.apply {
            executePendingBindings()
            chooseCurbside.setOnClickListener {
                cardListener.onSelectCard(item, position)
            }
            cardNumber.text = item.attributes.brand + " ****" + item.attributes.last4Digits
            val res: Int = com.herfy.common.R.drawable.ic_common_payment_type_card
            cardImage.setImageResource(res)
        }
    }

    fun updateList(promoList: ArrayList<CustomerCard>) {
        this.customerCards.clear()
        this.customerCards.addAll(promoList)
        notifyDataSetChanged()
    }

    interface CardListener {
        fun onSelectCard(card: CustomerCard, position: Int)
    }
}