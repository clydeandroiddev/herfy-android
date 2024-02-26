package com.herfy.upsell

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.cart.CartContract
import com.herfy.cart.databinding.ItemUpsellBinding


class UpsellRecyclerViewAdapter(private val cartPresenter : CartContract.Presenter) :
    RecyclerView.Adapter<UpsellViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpsellViewHolder {
        val binding = ItemUpsellBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpsellViewHolder(binding, cartPresenter)
    }

    override fun onBindViewHolder(holder: UpsellViewHolder, position: Int) {
        cartPresenter.onBindUpsellViewAtPosition(position, holder)
    }

    override fun getItemCount(): Int {
        return cartPresenter.getUpsellCount()
    }
}
