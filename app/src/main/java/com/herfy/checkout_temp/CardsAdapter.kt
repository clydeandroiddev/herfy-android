package com.herfy.checkout_temp

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.herfy.R
import com.herfy.checkout.databinding.ItemCardPaymentBinding
import com.herfy.checkout.databinding.ItemPaymentCreditDebitBinding
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard

class CardsAdapter(private val cards: List<CustomerCard>, private var selectedPos: Int = -1, private val callback: (CustomerCard, Int) -> Unit) : RecyclerView.Adapter<CardsAdapter.CardViewHolder>() {

    inner class CardViewHolder(val binding: ItemPaymentCreditDebitBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: CustomerCard, position: Int) {


            var res = R.drawable.ic_saved_card
            var cardLabel = "Visa ****"
            if (card.attributes.brand.equals("visa", true)) {
                res = R.drawable.ic_visa
                cardLabel = "Visa ****"
            } else if (card.attributes.brand.endsWith("mastercard", true)){
                res = R.drawable.ic_mastercard
                cardLabel = "Master ****"
            }


            if (selectedPos == position) {
                Log.e("Selected", "selectedPos bai  "+selectedPos+"---"+position)
                binding.rbCard.setImageResource(R.drawable.ic_payment_radio_selected)
                binding.cardsContainer.strokeColor = ContextCompat.getColor(binding.root.context,R.color.border_color_light)
                binding.cardsContainer.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.border_color_light))

            } else {
                Log.e("Selected22222", "selectedPos "+selectedPos+"---"+position)
                binding.rbCard.setImageResource(R.drawable.ic_payment_radio_unselected)
                binding.cardsContainer.strokeColor = ContextCompat.getColor(binding.root.context, R.color.card_stroke_color)
                binding.cardsContainer.setCardBackgroundColor(Color.WHITE)
            }

            binding.cardImage.setImageResource(res)
            binding.cardLabel.text = cardLabel+card.attributes.last4Digits

            binding.cardsContainer.setOnClickListener {
                selectedPos = position
                callback.invoke(card, selectedPos)
                notifyDataSetChanged()
            }


        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding : ItemPaymentCreditDebitBinding = ItemPaymentCreditDebitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
       return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = cards.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
       holder.bind(cards[position], position)
    }

    fun setSelectedPosition(newPosition: Int) {
        selectedPos = newPosition
        notifyDataSetChanged()
    }
}